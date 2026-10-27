// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.extension.k8s;

import io.fabric8.kubernetes.api.model.ContainerStateTerminated;
import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentCondition;
import io.fabric8.kubernetes.api.model.apps.DeploymentStatus;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.extension.Extension;
import org.thingsboard.server.common.data.extension.ExtensionMode;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.extension.spi.DeploymentHealth;
import org.thingsboard.server.service.extension.spi.ExtensionReconciler;
import org.thingsboard.server.service.extension.spi.ExtensionResourceNames;
import org.thingsboard.server.service.extension.spi.ExtensionSpecHasher;
import org.thingsboard.server.service.extension.spi.ReconcilerExceptions;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

@Service
@TbCoreComponent
@ConditionalOnExpression(ExtensionMode.WHEN_KUBERNETES)
@Slf4j
public class DefaultExtensionK8sReconciler implements ExtensionReconciler {

    private final ExtensionK8sClientProvider clientProvider;
    private final ExtensionManifestRenderer renderer;
    private final String egressPolicy;

    // Optional server-side salt. When set, the spec-hash is a keyed HMAC so the value stamped as a
    // Deployment annotation is not an offline-brute-forceable fingerprint of resolved secret values.
    @Value("${extensions.spec-hash-salt:}")
    private String specHashSalt = "";

    // Ceiling on how many pods one log read fans out over. The tail is applied PER POD, so without a cap
    // a single click on a 20-replica extension is 20 sequential log GETs against the API server, all on
    // one HTTP thread. Field injection (not a constructor arg) so the test-seam constructor keeps the
    // default, matching how specHashSalt above is wired. 0 disables the cap.
    @Value("${extensions.logs.max-pods:5}")
    private int maxLogPods = DEFAULT_MAX_LOG_PODS;

    static final int DEFAULT_MAX_LOG_PODS = 5;

    // A pod section costs a header before any of its log can follow, so opening one that leaves no room
    // for content spends the rest of the budget on a name. Below this much headroom the tail stops.
    static final int MIN_LOG_CHARS_PER_POD = 512;

    static final String TRUNCATION_MARK = "\n(truncated)";

    // How long a teardown waits for the pods of a deleted Deployment to disappear before it deletes their
    // NetworkPolicy. Longer than the default 30 s termination grace period; a pod stuck beyond that (a
    // finalizer, a node gone) keeps its policy until the orphan sweep. Field injection: test-seam ctor.
    @Value("${extensions.k8s.pod-termination-wait-ms:45000}")
    private long podTerminationWaitMs = DEFAULT_POD_TERMINATION_WAIT_MS;

    static final long DEFAULT_POD_TERMINATION_WAIT_MS = 45_000;
    static final long POD_TERMINATION_POLL_MS = 1_000;
    // How often a precondition-guarded delete re-reads an object that changed under it before giving up.
    static final int DELETE_PRECONDITION_ATTEMPTS = 3;

    // test seam: the mock API server runs no controllers, so no pod ever terminates on its own there
    void setPodTerminationWaitMs(long millis) {
        this.podTerminationWaitMs = millis;
    }

    private String namespace;
    private KubernetesClient client;

    @Autowired
    public DefaultExtensionK8sReconciler(
            ExtensionK8sClientProvider clientProvider,
            @Value("${extensions.k8s.tb-internal-url:http://tb-node:8080}") String tbInternalUrl,
            @Value("${extensions.k8s.health-path:/api/health}") String healthPath,
            @Value("${extensions.routing.base-path:/api/extension/route}") String basePath,
            @Value("${extensions.k8s.network-policy.enabled:true}") boolean networkPolicyEnabled,
            @Value("${extensions.k8s.network-policy.from-namespace-selector:}") String npFromNamespaceSelector,
            // These fall back to the renderer's own constants rather than repeating the literals: the
            // blocked-CIDR default was spelled out here and silently lost the IPv6 metadata address that
            // thingsboard.yml and the renderer both carry, so a deployment whose effective config lacks
            // the property would have left AWS IMDS reachable over fd00:ec2::254 on a dual-stack cluster.
            // An annotation value must be a constant expression, which these static finals are.
            @Value("${extensions.k8s.network-policy.from-pod-selector:"
                    + ExtensionManifestRenderer.DEFAULT_FROM_POD_SELECTOR + "}") String npFromPodSelector,
            @Value("${extensions.k8s.network-policy.egress-policy:"
                    + ExtensionManifestRenderer.EGRESS_BASELINE + "}") String egressPolicy,
            @Value("${extensions.k8s.network-policy.blocked-egress-cidrs:"
                    + ExtensionManifestRenderer.DEFAULT_BLOCKED_EGRESS_CIDRS + "}") String blockedEgressCidrs,
            @Value("${extensions.k8s.network-policy.allowed-egress-cidrs:}") String allowedEgressCidrs,
            @Value("${extensions.image.always-pull:true}") boolean alwaysPull,
            @Value("${extensions.k8s.network-policy.dns-namespace-selector:"
                    + ExtensionManifestRenderer.DEFAULT_DNS_NAMESPACE_SELECTOR + "}") String dnsNamespaceSelector,
            @Value("${extensions.k8s.network-policy.dns-pod-selector:"
                    + ExtensionManifestRenderer.DEFAULT_DNS_POD_SELECTOR + "}") String dnsPodSelector,
            @Value("${extensions.k8s.network-policy.dns-service-cidrs:"
                    + ExtensionManifestRenderer.DEFAULT_DNS_SERVICE_CIDRS + "}") String dnsServiceCidrs) {
        this.clientProvider = clientProvider;
        this.egressPolicy = egressPolicy;
        this.renderer = new ExtensionManifestRenderer(clientProvider.getNamespace(), tbInternalUrl, healthPath, basePath,
                networkPolicyEnabled, npFromNamespaceSelector, npFromPodSelector, egressPolicy, blockedEgressCidrs, allowedEgressCidrs,
                alwaysPull, dnsNamespaceSelector, dnsPodSelector, dnsServiceCidrs);
    }

    DefaultExtensionK8sReconciler(KubernetesClient client, String namespace, ExtensionManifestRenderer renderer) {
        this.clientProvider = null;
        this.client = client;
        this.namespace = namespace;
        this.renderer = renderer;
        this.egressPolicy = ExtensionManifestRenderer.EGRESS_BASELINE;
    }

    @PostConstruct
    void init() {
        this.client = clientProvider.getClient();
        this.namespace = clientProvider.getNamespace();
        log.info("Extension K8s reconciler initialized for namespace '{}'", namespace);
        // 'baseline' blocks only the link-local metadata CIDRs, so a tenant pod still reaches every other
        // service in the cluster (tb-core's REST API, Kafka, Redis, Postgres, the API server). That is fine
        // for a trusted single-tenant install and NOT fine for untrusted code; say so once at startup rather
        // than leaving it to the docs.
        if (!ExtensionManifestRenderer.EGRESS_RESTRICTED.equals(egressPolicy)) {
            log.warn("Extension egress policy is '{}': tenant pods can reach cluster-internal services "
                    + "(tb-core, Kafka, Redis, Postgres, the Kubernetes API). For untrusted tenant code set "
                    + "extensions.k8s.network-policy.egress-policy=restricted with allowed-egress-cidrs, or run "
                    + "extensions in a separate cluster (extensions.mode=public-cloud).", egressPolicy);
        }
    }

    @Override
    public void apply(Extension extension) {
        String slug = extension.getSlug();
        String desiredHash = ExtensionSpecHasher.hash(extension, specHashSalt, renderer.renderContext());

        createOrUpdate(client.secrets().inNamespace(namespace), ExtensionResourceNames.envSecret(slug), existing ->
                converged(existing, desiredHash, extension)
                        ? null // already at the desired hash, and ours
                        : renderer.renderEnvSecret(extension, desiredHash));

        // Private-registry pull credentials (resolved upstream from a TB Secret). Rendered only when the
        // spec carries registryAuth; otherwise any previously created pull Secret is removed.
        String pullSecretName = ExtensionResourceNames.pullSecret(slug);
        if (extension.getContainerSpec().getRegistryAuth() != null) {
            createOrUpdate(client.secrets().inNamespace(namespace), pullSecretName, existing ->
                    converged(existing, desiredHash, extension)
                            ? null
                            : renderer.renderPullSecret(extension, desiredHash));
        } else {
            // deleteIf(isManaged), not a bare delete(): the name derives from the tenant's slug, so an
            // unguarded delete here would let a slug collision destroy an operator-owned Secret — the very
            // thing every other path in this class refuses to do. This branch runs on EVERY apply of an
            // extension without registryAuth, so it is the most reachable of them all.
            deleteIf(client.secrets().inNamespace(namespace), pullSecretName, DefaultExtensionK8sReconciler::isManaged);
        }

        // Converged check, same as the Secret above: the Service is derived from the container port and
        // the slug, both covered by the hash, so without it the periodic resync issued a pointless UPDATE
        // per extension per pass — one API-server round trip, admission chain included, for nothing.
        createOrUpdate(client.services().inNamespace(namespace), ExtensionResourceNames.service(slug), existing -> {
            if (converged(existing, desiredHash, extension)) {
                return null;
            }
            var desired = renderer.renderService(extension, desiredHash);
            if (existing != null) {
                // carry over the server-assigned immutable fields a PUT must not change
                desired.getSpec().setClusterIP(existing.getSpec().getClusterIP());
                desired.getSpec().setClusterIPs(existing.getSpec().getClusterIPs());
            }
            return desired;
        });

        // Rendered only while network policies are enabled; otherwise any previously created one is
        // removed — the same shape as the pull Secret above. Skipping the delete would leave every
        // existing extension enforcing its old policy after the flag is turned off: the stale ingress
        // rule still admits only the previously configured router selector, so an operator who disables
        // the feature to unblock traffic would find those extensions unreachable instead, with neither
        // re-apply nor resync ever removing the policy (the orphan sweep only touches slugs with no
        // live row).
        String networkPolicyName = ExtensionResourceNames.networkPolicy(slug);
        if (renderer.isNetworkPolicyEnabled()) {
            // Every input of the policy is hashed — the container port from the spec, the selectors and
            // the egress rules through renderContext() — so a converged one needs no rewrite either.
            createOrUpdate(client.network().v1().networkPolicies().inNamespace(namespace),
                    networkPolicyName, existing -> converged(existing, desiredHash, extension)
                            ? null
                            : renderer.renderNetworkPolicy(extension, desiredHash));
        } else {
            // Same ownership guard as the pull Secret above: a NetworkPolicy name is the bare tb-ext-{slug},
            // which is exactly the shape of an operator-owned object's name.
            deleteIf(client.network().v1().networkPolicies().inNamespace(namespace), networkPolicyName, DefaultExtensionK8sReconciler::isManaged);
        }

        createOrUpdate(client.apps().deployments().inNamespace(namespace), ExtensionResourceNames.deployment(slug), existing -> {
            Deployment desired = renderer.renderDeployment(extension, desiredHash, restartedAt(existing));
            if (existing == null) {
                log.info("Creating Deployment for extension {} (hash {})", slug, desiredHash);
                return desired;
            }
            String observedHash = ExtensionResourceNames.annotation(existing.getMetadata().getAnnotations(),
                    ExtensionResourceNames.ANNOTATION_SPEC_HASH);
            int desiredReplicas = desired.getSpec().getReplicas();
            int liveReplicas = existing.getSpec() != null && existing.getSpec().getReplicas() != null ? existing.getSpec().getReplicas() : -1;
            // Ownership is part of "converged": the spec-hash covers the container spec only, so a
            // Deployment left behind by a different extension that once held this slug can carry the same
            // hash. Writing the desired object restamps the owner labels the read paths rely on.
            if (desiredHash.equals(observedHash) && desiredReplicas == liveReplicas
                    && ExtensionResourceNames.ownedBy(existing.getMetadata().getLabels(), extension)) {
                log.debug("apply no-op for extension {} — already at hash {}", slug, desiredHash);
                return null;
            }
            log.info("Updating Deployment for extension {} ({} -> {})", slug, observedHash, desiredHash);
            return desired;
        });
    }

    // Converges one object kind to its rendered desired state. Explicit create-or-update instead of
    // server-side apply: SSA buys nothing here (we render the full desired object) and the fabric8
    // CRUD mock cannot emulate it. The callback receives the live object (null when absent) and
    // returns the object to write, or null to skip a no-op update.
    private <T extends HasMetadata, R extends Resource<T>> void createOrUpdate(
            NonNamespaceOperation<T, ?, R> op, String name, UnaryOperator<T> desiredFromExisting) {
        T existing = op.withName(name).get();
        T desired = desiredFromExisting.apply(existing);
        if (desired == null) {
            return;
        }
        if (existing == null) {
            op.resource(desired).create();
        } else {
            requireManaged(existing, name);
            op.resource(desired).update();
        }
    }

    // An existing object needs no rewrite only when it is at the desired spec-hash AND is ours. See
    // ExtensionResourceNames.ownedBy: a slug is reusable the moment its row is deleted, so a stale object
    // of the previous owner can still answer to this name.
    private static boolean converged(HasMetadata existing, String desiredHash, Extension extension) {
        return existing != null
                && desiredHash.equals(ExtensionResourceNames.annotation(
                        existing.getMetadata().getAnnotations(), ExtensionResourceNames.ANNOTATION_SPEC_HASH))
                && ExtensionResourceNames.ownedBy(existing.getMetadata().getLabels(), extension);
    }

    // Defence in depth against a slug whose object name collides with an operator-owned object (e.g.
    // the shared tb-ext-router): never touch an object that is not labelled as managed by ThingsBoard.
    // Reserved slugs are already rejected at creation; this guards every path that resolves an object
    // by its derived name — writing it (apply), mutating it (scale/restart) or destroying it (delete).
    private static boolean isManaged(HasMetadata existing) {
        Map<String, String> labels = existing.getMetadata() != null ? existing.getMetadata().getLabels() : null;
        return labels != null
                && ExtensionResourceNames.MANAGED_BY.equals(labels.get(ExtensionResourceNames.LABEL_MANAGED_BY));
    }

    private static void requireManaged(HasMetadata existing, String name) {
        if (!isManaged(existing)) {
            throw new IllegalStateException("Refusing to overwrite object '" + name + "' that is not managed by "
                    + "ThingsBoard (missing " + ExtensionResourceNames.LABEL_MANAGED_BY + "=" + ExtensionResourceNames.MANAGED_BY + " label)");
        }
    }

    // Guard for scale/restart: absent -> null, unmanaged (operator-owned) -> throw, managed but
    // foreign (previous slug owner, possibly another tenant) -> treated as absent, like the read paths.
    private Deployment managedDeployment(Extension extension, String action) {
        String name = ExtensionResourceNames.deployment(extension.getSlug());
        Deployment existing = client.apps().deployments().inNamespace(namespace).withName(name).get();
        if (existing == null) {
            log.info("{} skipped: Deployment for {} is absent", action, extension.getSlug());
            return null;
        }
        requireManaged(existing, name);
        if (!ExtensionResourceNames.ownedBy(existing.getMetadata().getLabels(), extension)) {
            log.warn("Refusing {} on Deployment '{}': it does not belong to extension {} ({})",
                    action, name, extension.getId(), extension.getTenantId());
            return null;
        }
        return existing;
    }

    @Override
    public void scaleTo(Extension extension, int replicas) {
        String name = ExtensionResourceNames.deployment(extension.getSlug());
        if (managedDeployment(extension, "scale") == null) {
            return;
        }
        client.apps().deployments().inNamespace(namespace).withName(name).scale(replicas);
        log.info("Scaled extension {} to {} replicas", extension.getSlug(), replicas);
    }

    @Override
    public void restart(Extension extension) {
        String name = ExtensionResourceNames.deployment(extension.getSlug());
        Deployment existing = managedDeployment(extension, "restart");
        if (existing == null) {
            return;
        }
        String now = Long.toString(System.currentTimeMillis());
        if (existing.getMetadata().getAnnotations() == null) {
            existing.getMetadata().setAnnotations(new HashMap<>());
        }
        existing.getMetadata().getAnnotations().put(ExtensionResourceNames.ANNOTATION_RESTARTED_AT, now);
        if (existing.getSpec().getTemplate().getMetadata().getAnnotations() == null) {
            existing.getSpec().getTemplate().getMetadata().setAnnotations(new HashMap<>());
        }
        existing.getSpec().getTemplate().getMetadata().getAnnotations().put(ExtensionResourceNames.ANNOTATION_RESTARTED_AT, now);
        client.apps().deployments().inNamespace(namespace).resource(existing).update();
        log.info("Restart annotation bumped for extension {} ({})", extension.getSlug(), now);
    }

    // Owner-checked teardown: every object must carry THIS extension's tenant/extension-id labels, not
    // just the managed-by marker. The slug is reusable the moment the row is deleted and teardown retries
    // for seconds, so a name-only delete could remove the workload a fast re-create with the same name
    // has already built for a new extension (another tenant's, even). The orphan sweep has no owner to
    // compare against and keeps using deleteBySlug.
    @Override
    public void delete(Extension extension) {
        deleteObjects(extension.getSlug(), obj -> ExtensionResourceNames.ownedBy(obj.getMetadata().getLabels(), extension));
    }

    @Override
    public void deleteBySlug(String slug) {
        deleteObjects(slug, DefaultExtensionK8sReconciler::isManaged);
    }

    // Deployment first, then WAIT for its pods to be gone, and only then the NetworkPolicy. delete() on a
    // Deployment returns as soon as the API server accepts it (background propagation); the pods then
    // have their termination grace period (30 s by default) in which they still run tenant code. If the
    // policy went in the same breath, nothing would select those pods for that window: no IMDS block, no
    // router-only ingress. If the pods do not go away within the bound, the policy is left in place for
    // the orphan sweep, which lists NetworkPolicies and calls back into here.
    private void deleteObjects(String slug, Predicate<HasMetadata> ours) {
        deleteIf(client.apps().deployments().inNamespace(namespace), ExtensionResourceNames.deployment(slug), ours);
        deleteIf(client.secrets().inNamespace(namespace), ExtensionResourceNames.envSecret(slug), ours);
        deleteIf(client.secrets().inNamespace(namespace), ExtensionResourceNames.pullSecret(slug), ours);
        deleteIf(client.services().inNamespace(namespace), ExtensionResourceNames.service(slug), ours);
        // The pod check is NOT conditional on having deleted the Deployment in this call: a retry, or the
        // orphan sweep's follow-up, arrives after an earlier call already removed it, and the pod that
        // made the earlier call leave the policy behind may still be terminating. Cheap when there is
        // nothing to wait for — one LIST that comes back empty.
        var policies = client.network().v1().networkPolicies().inNamespace(namespace);
        if (policies.withName(ExtensionResourceNames.networkPolicy(slug)).get() == null) {
            log.info("Deleted Deployment + Secrets + Service for extension {} (no NetworkPolicy present)", slug);
            return;
        }
        if (!awaitPodsGone(slug)) {
            log.warn("Pods of extension {} are still present after {} ms — leaving its NetworkPolicy in "
                    + "place for the orphan sweep", slug, podTerminationWaitMs);
            return;
        }
        deleteIf(policies, ExtensionResourceNames.networkPolicy(slug), ours);
        log.info("Deleted Deployment + Secrets + Service + NetworkPolicy for extension {}", slug);
    }

    // True once no pod of this instance is left (or none was there to begin with); false on timeout or
    // when the pods cannot be listed — an unknown pod set must keep its policy, not lose it.
    private boolean awaitPodsGone(String slug) {
        long deadline = System.currentTimeMillis() + podTerminationWaitMs;
        while (true) {
            List<Pod> pods;
            try {
                pods = client.pods().inNamespace(namespace)
                        .withLabel(ExtensionResourceNames.LABEL_MANAGED_BY, ExtensionResourceNames.MANAGED_BY)
                        .withLabel(ExtensionResourceNames.LABEL_APP_INSTANCE, ExtensionResourceNames.instanceLabel(slug))
                        .list().getItems();
            } catch (KubernetesClientException e) {
                log.warn("Could not list the pods of extension {} — keeping its NetworkPolicy: {}", slug, e.getMessage());
                return false;
            }
            if (pods.isEmpty()) {
                return true;
            }
            if (System.currentTimeMillis() >= deadline) {
                return false;
            }
            try {
                Thread.sleep(Math.min(POD_TERMINATION_POLL_MS, Math.max(1, deadline - System.currentTimeMillis())));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
    }

    // Skips a foreign object rather than throwing: teardown must still remove the objects we do own, and
    // it runs from the orphan sweep too, where an abort would retry against the same collision forever.
    // Returns true when an object was actually deleted.
    //
    // The delete is bound to the very object that passed the owner check: it carries the inspected
    // resourceVersion as a precondition, so if anything replaced or re-labelled the object under the same
    // name between the GET and the DELETE (a new owner of the slug applying its workload — the per-id
    // lock cannot serialize two different extensions), the API server answers 409 and nothing is removed.
    // The check is then simply repeated against the current object; a foreign one is refused as above.
    private <T extends HasMetadata, R extends Resource<T>> boolean deleteIf(
            NonNamespaceOperation<T, ?, R> op, String name, Predicate<HasMetadata> ours) {
        for (int attempt = 1; ; attempt++) {
            T existing = op.withName(name).get();
            if (existing == null) {
                return false;
            }
            if (!ours.test(existing)) {
                log.warn("Refusing to delete object '{}' in namespace '{}': it does not belong to this extension "
                                + "(missing {}={} or a different owner)", name, namespace,
                        ExtensionResourceNames.LABEL_MANAGED_BY, ExtensionResourceNames.MANAGED_BY);
                return false;
            }
            try {
                op.resource(existing).lockResourceVersion().delete();
                return true;
            } catch (KubernetesClientException e) {
                if (e.getCode() != 409 || attempt >= DELETE_PRECONDITION_ATTEMPTS) {
                    throw e;
                }
                log.info("Object '{}' changed between the owner check and the delete — re-checking", name);
            }
        }
    }

    // Unions every managed kind, not just Deployments. deleteBySlug removes the Deployment first so the
    // workload stops while its NetworkPolicy still constrains it, which means a teardown that fails
    // afterwards leaves Secrets/Service/NetworkPolicy behind with no Deployment. Discovering orphans by
    // Deployment alone would make those invisible to the sweep forever — and the env/pull Secrets hold
    // the extension's resolved env values and registry credentials in plaintext.
    @Override
    public Set<String> listManagedSlugs() {
        Set<String> slugs = new HashSet<>();
        collectManagedSlugs(client.apps().deployments().inNamespace(namespace), slugs);
        collectManagedSlugs(client.secrets().inNamespace(namespace), slugs);
        collectManagedSlugs(client.services().inNamespace(namespace), slugs);
        collectManagedSlugs(client.network().v1().networkPolicies().inNamespace(namespace), slugs);
        return slugs;
    }

    private <T extends HasMetadata, L extends KubernetesResourceList<T>, R extends Resource<T>> void collectManagedSlugs(
            NonNamespaceOperation<T, L, R> op, Set<String> into) {
        for (T item : op.withLabel(ExtensionResourceNames.LABEL_MANAGED_BY, ExtensionResourceNames.MANAGED_BY)
                .list().getItems()) {
            String slug = item.getMetadata() != null && item.getMetadata().getLabels() != null
                    ? item.getMetadata().getLabels().get(ExtensionResourceNames.LABEL_EXTENSION_SLUG) : null;
            if (slug != null) {
                into.add(slug);
            }
        }
    }

    @Override
    public DeploymentHealth describe(Extension extension) {
        Deployment d = client.apps().deployments().inNamespace(namespace)
                .withName(ExtensionResourceNames.deployment(extension.getSlug())).get();
        if (d == null) {
            return DeploymentHealth.missing();
        }
        if (!ExtensionResourceNames.ownedBy(d.getMetadata().getLabels(), extension)) {
            log.warn("Refusing describe on Deployment '{}': it does not belong to extension {} ({})",
                    d.getMetadata().getName(), extension.getId(), extension.getTenantId());
            return DeploymentHealth.missing();
        }
        return health(d, ownedPods(extension));
    }

    // The instance label derives from the slug, and a slug outlives the extension that held it: the row
    // is deleted synchronously while teardown runs asynchronously, and the orphan sweep skips a slug that
    // a new row already owns. Selecting on the instance label alone therefore hands the new extension the
    // PREVIOUS tenant's pods — and their logs. Match the owner labels (managed-by + tenant + extension id)
    // that every rendered pod template carries, and let the API server do the filtering.
    private List<Pod> ownedPods(Extension extension) {
        return client.pods().inNamespace(namespace)
                .withLabel(ExtensionResourceNames.LABEL_APP_INSTANCE, ExtensionResourceNames.instanceLabel(extension.getSlug()))
                .withLabels(ExtensionResourceNames.ownerSelector(extension))
                .list().getItems();
    }

    @Override
    public String tailLogs(Extension extension, int lines) {
        List<Pod> pods;
        try {
            pods = ownedPods(extension);
        } catch (Exception e) {
            // The pod LIST is the one call here with no catch; unsanitized it surfaces to the tenant as a
            // raw 500 whose message names the API server.
            throw new IllegalStateException("Could not list the pods of extension " + extension.getSlug()
                    + ": " + ReconcilerExceptions.sanitizeMessage(e), e);
        }
        if (pods.isEmpty()) {
            return "No pods found for extension " + extension.getSlug() + ".";
        }
        // The tail is per pod, so a 20-replica extension multiplies whatever `lines` allows — and the
        // tenant controls how long each line is. Two independent bounds apply. First, the pod fan-out is
        // capped, so the number of API-server round-trips per request does not scale with replica count.
        int podLimit = maxLogPods > 0 ? Math.min(pods.size(), maxLogPods) : pods.size();
        List<String> podNames = pods.subList(0, podLimit).stream()
                .map(p -> p.getMetadata().getName())
                .collect(Collectors.toList());
        // limitBytes makes the budget bind on the WIRE, not after the fact: tailingLines caps the number
        // of lines but nothing caps their length, so without it the client materializes the whole pod log
        // on the heap and only then truncates. One authenticated request could therefore allocate
        // hundreds of megabytes regardless of the ceiling. Bytes >= chars for UTF-8, so a byte budget of
        // `room` yields at most `room` chars — the conservative direction.
        return assembleLogTail(podNames, pods.size(), (podName, room) ->
                client.pods().inNamespace(namespace).withName(podName)
                        .inContainer(ExtensionResourceNames.CONTAINER_NAME)
                        .limitBytes(room)
                        .tailingLines(lines)
                        .getLog());
    }

    // Reads one pod's log with a character budget. Separating it from the assembly below keeps the budget
    // arithmetic testable at its boundaries without a Kubernetes client.
    @FunctionalInterface
    interface PodLogReader {
        String read(String podName, int room) throws Exception;
    }

    // The second bound on a log response: the character budget. Stop once it is spent, and stop BEFORE
    // fetching the next pod's log, so the transient allocation stays one pod rather than the whole fleet.
    static String assembleLogTail(List<String> podNames, int totalPods, PodLogReader reader) {
        StringBuilder sb = new StringBuilder();
        for (String podName : podNames) {
            String header = "==== " + podName + " ====\n";
            // Reserve the header BEFORE writing it. Checking only what is already in the buffer lets the
            // header itself cross the ceiling and leaves a negative budget for the body: the read would
            // then ask the API server for a negative number of bytes and the copy below would throw, so
            // the section would carry an error message instead of a log.
            if (sb.length() + header.length() + MIN_LOG_CHARS_PER_POD > MAX_LOG_RESPONSE_CHARS) {
                sb.append("\n(truncated: log output exceeded ").append(MAX_LOG_RESPONSE_CHARS)
                        .append(" characters; remaining pods omitted)");
                break;
            }
            sb.append(header);
            int room = MAX_LOG_RESPONSE_CHARS - sb.length();   // > 0 by the check above
            try {
                String podLog = reader.read(podName, room);
                if (podLog.length() > room) {
                    sb.append(podLog, 0, room).append(TRUNCATION_MARK);
                } else {
                    sb.append(podLog);
                }
            } catch (Exception e) {
                // Never the raw message: a fabric8 failure carries the API-server URL and pod path.
                sb.append("(could not read logs: ").append(ReconcilerExceptions.sanitizeMessage(e)).append(")");
            }
            sb.append('\n');
        }
        if (podNames.size() < totalPods) {
            sb.append("\n(showing ").append(podNames.size()).append(" of ").append(totalPods)
                    .append(" pods; raise extensions.logs.max-pods to see more)\n");
        }
        return capped(sb);
    }

    // Everything written after a pod's budget is spent — the truncation notice, a client error message,
    // the "showing N of M pods" footer — lands outside that budget, so the assembled text can still pass
    // the ceiling by a few hundred characters. The contract is a hard ceiling, so cut to it last.
    private static String capped(StringBuilder sb) {
        if (sb.length() <= MAX_LOG_RESPONSE_CHARS) {
            return sb.toString();
        }
        return sb.substring(0, MAX_LOG_RESPONSE_CHARS - TRUNCATION_MARK.length()) + TRUNCATION_MARK;
    }

    // Two label-filtered list calls for the whole set, instead of the GET + LIST that describe() issues
    // per extension. The pods are grouped by their instance label, so the cost is independent of how
    // many slugs are asked about.
    @Override
    public Map<String, DeploymentHealth> describeAll(Collection<Extension> extensions) {
        if (extensions == null || extensions.isEmpty()) {
            return Map.of();
        }
        Map<String, Extension> bySlug = extensions.stream().filter(e -> e.getSlug() != null)
                .collect(Collectors.toMap(Extension::getSlug, Function.identity(), (a, b) -> a));
        List<Deployment> deployments = client.apps().deployments().inNamespace(namespace)
                .withLabel(ExtensionResourceNames.LABEL_MANAGED_BY, ExtensionResourceNames.MANAGED_BY)
                .list().getItems();
        List<Pod> pods = client.pods().inNamespace(namespace)
                .withLabel(ExtensionResourceNames.LABEL_MANAGED_BY, ExtensionResourceNames.MANAGED_BY)
                .list().getItems();

        Map<String, List<Pod>> podsByInstance = pods.stream()
                .filter(p -> p.getMetadata() != null && p.getMetadata().getLabels() != null)
                .collect(Collectors.groupingBy(p -> String.valueOf(
                        p.getMetadata().getLabels().get(ExtensionResourceNames.LABEL_APP_INSTANCE))));

        Map<String, DeploymentHealth> result = new HashMap<>();
        for (Deployment d : deployments) {
            String slug = d.getMetadata() != null && d.getMetadata().getLabels() != null
                    ? d.getMetadata().getLabels().get(ExtensionResourceNames.LABEL_EXTENSION_SLUG) : null;
            Extension owner = slug != null ? bySlug.get(slug) : null;
            // Same owner check describe() makes: a Deployment still answering to this slug may belong to
            // the extension that held it before, whose status must not be reported as this one's.
            if (owner == null || !ExtensionResourceNames.ownedBy(d.getMetadata().getLabels(), owner)) {
                continue;
            }
            List<Pod> ownedPods = podsByInstance.getOrDefault(ExtensionResourceNames.instanceLabel(slug), List.of())
                    .stream()
                    .filter(p -> ExtensionResourceNames.ownedBy(p.getMetadata().getLabels(), owner))
                    .toList();
            result.put(slug, health(d, ownedPods));
        }
        return result;
    }

    private static DeploymentHealth health(Deployment d, List<Pod> pods) {
        var status = d.getStatus();
        int desired = d.getSpec() != null && d.getSpec().getReplicas() != null ? d.getSpec().getReplicas() : 0;
        int ready = status != null && status.getReadyReplicas() != null ? status.getReadyReplicas() : 0;
        int updated = status != null && status.getUpdatedReplicas() != null ? status.getUpdatedReplicas() : 0;
        Map<String, Integer> phases = new HashMap<>();
        String errorReason = null;
        for (Pod p : pods) {
            String phase = p.getStatus() != null ? p.getStatus().getPhase() : "Unknown";
            phases.merge(phase, 1, Integer::sum);
            String reason = podErrorReason(p);
            if (reason != null) {
                errorReason = reason;
            }
        }
        if (errorReason == null) {
            // Some failures never produce a Pod at all — a ResourceQuota rejection or an admission
            // webhook denial surfaces only as a Deployment condition. Without this the extension sits in
            // PROVISIONING forever with an empty lastError and nothing to show the tenant.
            errorReason = deploymentConditionError(status);
        }
        return new DeploymentHealth(true, desired, ready, updated, phases, errorReason);
    }

    // ReplicaFailure=True: the ReplicaSet could not create pods (quota, admission, PSA).
    // Progressing=False: the rollout gave up (usually ProgressDeadlineExceeded).
    private static String deploymentConditionError(DeploymentStatus status) {
        if (status == null || status.getConditions() == null) {
            return null;
        }
        for (DeploymentCondition condition : status.getConditions()) {
            String type = condition.getType();
            String conditionStatus = condition.getStatus();
            boolean failing = ("ReplicaFailure".equals(type) && "True".equals(conditionStatus))
                    || ("Progressing".equals(type) && "False".equals(conditionStatus));
            if (failing) {
                String reason = StringUtils.isNotBlank(condition.getReason()) ? condition.getReason() : type;
                return StringUtils.isNotBlank(condition.getMessage()) ? reason + ": " + condition.getMessage() : reason;
            }
        }
        return null;
    }

    // Why a pod is failing, or null while it is healthy or merely still coming up. The crash-loop check
    // runs FIRST because it is the only one that holds in both phases of a loop — see crashLoopReason.
    private static String podErrorReason(Pod pod) {
        if (pod.getStatus() == null || pod.getStatus().getContainerStatuses() == null) {
            return null;
        }
        String waiting = null;
        for (ContainerStatus cs : pod.getStatus().getContainerStatuses()) {
            String looping = crashLoopReason(cs);
            if (looping != null) {
                return looping;
            }
            if (waiting == null && cs.getState() != null && cs.getState().getWaiting() != null
                    && isTerminalWaitingReason(cs.getState().getWaiting().getReason())) {
                waiting = cs.getState().getWaiting().getReason();
            }
        }
        return waiting;
    }

    // A crash-looping container carries `waiting: CrashLoopBackOff` only BETWEEN attempts. While it is
    // actually running — the few seconds before it dies again — that waiting state is gone and the pod
    // reads clean, so a poll landing in that window found no error and the extension flipped
    // Failed -> Active -> Failed at the poll rate. What is true in BOTH phases is that the container is
    // not ready and its previous attempt terminated abnormally, so the reason is keyed off that
    // termination instead and stops blinking. Readiness is the guard that keeps a single restart which
    // then recovered from being reported for the rest of the pod's life: once the container is ready
    // again, its restart count no longer says anything about now.
    private static String crashLoopReason(ContainerStatus cs) {
        if (Boolean.TRUE.equals(cs.getReady()) || cs.getRestartCount() == null || cs.getRestartCount() <= 0) {
            return null;
        }
        ContainerStateTerminated last = cs.getLastState() != null ? cs.getLastState().getTerminated() : null;
        if (last == null || (last.getExitCode() != null && last.getExitCode() == 0)) {
            return null;
        }
        // The termination reason is the useful half of the message (OOMKilled above all); a bare 'Error'
        // adds nothing the back-off name does not already say.
        String detail = last.getReason();
        return StringUtils.isNotBlank(detail) && !"Error".equalsIgnoreCase(detail)
                ? "CrashLoopBackOff: " + detail
                : "CrashLoopBackOff";
    }

    // Any waiting reason that names an error or a back-off is terminal enough to report. An allowlist of
    // three (ImagePullBackOff / ErrImagePull / CrashLoopBackOff) missed the realistic ones —
    // CreateContainerConfigError (e.g. a non-numeric USER under runAsNonRoot, which the README calls out
    // as an image requirement), InvalidImageName, CreateContainerError — and left the extension showing
    // 'Provisioning' with no error at all. Benign transient reasons (ContainerCreating, PodInitializing)
    // match none of these shapes.

    private static boolean isTerminalWaitingReason(String reason) {
        if (reason == null) {
            return false;
        }
        // 'Err...' covers ErrImagePull / ErrImageNeverPull, which end in neither 'Error' nor 'BackOff'.
        return reason.startsWith("Err")
                || reason.endsWith("Error")
                || reason.endsWith("BackOff")
                || "InvalidImageName".equals(reason);
    }

    private static Long restartedAt(Deployment existing) {
        if (existing == null) {
            return null;
        }
        String raw = ExtensionResourceNames.annotation(existing.getMetadata().getAnnotations(), ExtensionResourceNames.ANNOTATION_RESTARTED_AT);
        try {
            return raw != null ? Long.parseLong(raw) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public boolean isRetryable(Throwable t) {
        return isRetryableK8s(t);
    }

    // Transient k8s/API-server failures are retried in-task with a short linear backoff; the
    // periodic resync is the durable net for anything that still slips through. Static so the
    // classification can be unit-tested and reused without a live client.
    // <=0: no HTTP response (connect/handshake failure); 408/429: timeout/throttling; 5xx: server side.
    // 409 is added on top of the shared rule: on the Kubernetes API it is the canonical
    // optimistic-concurrency signal ("the object has been modified"), which every read-modify-write here
    // can hit simply because the Deployment controller writes status during a rollout. Treating it as
    // terminal flipped healthy extensions to FAILED after a Restart clicked mid-rollout. The retry
    // re-reads the object first, so the next attempt works from the current resourceVersion.
    public static boolean isRetryableK8s(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof KubernetesClientException k8sException && k8sException.getCode() == 409) {
                return true;
            }
        }
        return ReconcilerExceptions.isRetryable(e, KubernetesClientException.class, KubernetesClientException::getCode);
    }

}
