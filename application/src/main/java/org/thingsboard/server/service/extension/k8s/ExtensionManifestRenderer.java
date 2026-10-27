// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.extension.k8s;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.fabric8.kubernetes.api.model.EnvFromSource;
import io.fabric8.kubernetes.api.model.EnvFromSourceBuilder;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.LocalObjectReference;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.fabric8.kubernetes.api.model.ResourceRequirementsBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.SecretEnvSourceBuilder;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.networking.v1.NetworkPolicy;
import io.fabric8.kubernetes.api.model.networking.v1.NetworkPolicyBuilder;
import io.fabric8.kubernetes.api.model.networking.v1.NetworkPolicyEgressRule;
import io.fabric8.kubernetes.api.model.networking.v1.NetworkPolicyEgressRuleBuilder;
import io.fabric8.kubernetes.api.model.networking.v1.NetworkPolicyIngressRuleBuilder;
import io.fabric8.kubernetes.api.model.networking.v1.NetworkPolicyPeer;
import io.fabric8.kubernetes.api.model.networking.v1.NetworkPolicyPeerBuilder;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.extension.Extension;
import org.thingsboard.server.common.data.extension.ExtensionContainerSpec;
import org.thingsboard.server.common.data.extension.ExtensionState;
import org.thingsboard.server.common.data.extension.RegistryAuth;
import org.thingsboard.server.common.data.extension.ResourceSpec;
import org.thingsboard.server.service.extension.spi.ExtensionEnv;
import org.thingsboard.server.service.extension.spi.ExtensionResourceNames;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ExtensionManifestRenderer {

    // Name of the extension's container port. The Service targetPort and the NetworkPolicy ingress port
    // refer to it by name so that a port change reaches old and new pods alike during a rollout.
    static final String CONTAINER_PORT_NAME = "http";

    public static final String EGRESS_OPEN = "open";
    public static final String EGRESS_BASELINE = "baseline";
    public static final String EGRESS_RESTRICTED = "restricted";
    // IPv4 and IPv6 link-local metadata/credential endpoints are credential-theft targets; block by default.
    // 169.254.0.0/16 covers IMDS + ECS/EKS creds; fd00:ec2::254/128 is the AWS IMDS IPv6 address on
    // dual-stack clusters (an IPv4-only block leaves that reachable).
    public static final String DEFAULT_BLOCKED_EGRESS_CIDRS = "169.254.0.0/16,fd00:ec2::254/128";
    // Default ingress source for tenant pods: only the hosted catch-all router may reach them, so
    // one tenant pod cannot open a direct connection to another tenant's pod in the shared namespace.
    public static final String DEFAULT_FROM_POD_SELECTOR = "app.kubernetes.io/name=tb-ext-router";
    // Default cluster DNS peer for the always-allowed port-53 egress rule. These are the conventional
    // CoreDNS labels (kubeadm, EKS, GKE all keep k8s-app=kube-dns); clusters that label DNS differently
    // (e.g. OpenShift) override them with extensions.k8s.network-policy.dns-namespace-selector /
    // dns-pod-selector. Set both to empty to fall back to an unscoped DNS rule — which reopens an
    // any-destination channel on port 53 and is NOT recommended.
    public static final String DEFAULT_DNS_NAMESPACE_SELECTOR = "kubernetes.io/metadata.name=kube-system";
    public static final String DEFAULT_DNS_POD_SELECTOR = "k8s-app=kube-dns";
    /**
     * Cluster IP range of the kube-dns Service, added to the port-53 egress rule as an ipBlock peer
     * ALONGSIDE the pod/namespace selectors above.
     *
     * <p>A pod resolves names through the kube-dns <em>Service</em> ClusterIP (e.g. 10.100.0.10), not a
     * CoreDNS pod IP. Most CNIs evaluate egress after kube-proxy has rewritten that address, so a pod
     * selector matches and DNS works. The AWS VPC CNI evaluates it <em>before</em> DNAT, against the
     * virtual ClusterIP, which no pod or namespace selector can ever match — so the selector-only rule
     * denies DNS outright and the pod fails to resolve anything:
     *
     * <pre>recv() failed (111: Connection refused) while resolving, resolver: 10.100.0.10:53</pre>
     *
     * <p>Empty by default because the range is cluster-specific and there is no value that is right
     * everywhere (EKS commonly 10.100.0.0/16, kubeadm 10.96.0.0/12). It cannot be discovered here either:
     * the reconciler's Role is scoped to the extensions namespace and deliberately cannot read Services in
     * kube-system. Only load-bearing under egress-policy=restricted — under 'baseline' the wide rule
     * already covers port 53 to any destination.
     *
     * <p>Found on the EKS public-cloud run, 2026-08-11.
     */
    public static final String DEFAULT_DNS_SERVICE_CIDRS = "";

    /**
     * Size of the writable scratch mounted at {@code /tmp}, applied BOTH as the emptyDir sizeLimit and as
     * the container's ephemeral-storage request/limit. A read-only root filesystem still needs somewhere
     * to write, but an uncapped emptyDir lets a tenant fill the node's disk — evicting co-located pods,
     * including other tenants' extensions — without consuming any CPU or memory it is billed for.
     * Matches the tmpfs size the Docker backend already imposes on the same path.
     */
    static final String SCRATCH_SIZE = "64Mi";

    /**
     * Startup budget for a tenant container: {@code failureThreshold x periodSeconds} = 5 minutes before
     * the kubelet gives up and restarts it. Liveness does not run until the startup probe succeeds once,
     * so a slow-booting image is no longer killed mid-warm-up.
     */
    static final int STARTUP_PERIOD_SECONDS = 5;
    static final int STARTUP_FAILURE_THRESHOLD = 60;
    /**
     * Probe timeout. The Kubernetes default is 1s, which turns an occasionally slow health endpoint into
     * a needless restart loop.
     */
    static final int PROBE_TIMEOUT_SECONDS = 3;

    private final String namespace;
    private final String tbInternalUrl;
    private final String healthPath;
    private final String basePath;
    private final boolean networkPolicyEnabled;
    private final String npFromNamespaceLabel;
    private final String npFromPodLabel;
    private final String egressPolicy;
    private final List<String> blockedEgressCidrs;
    private final List<String> allowedEgressCidrs;
    private final String dnsNamespaceLabel;
    private final String dnsPodLabel;
    private final List<String> dnsServiceCidrs;
    private final boolean alwaysPull;

    public ExtensionManifestRenderer(String namespace, String tbInternalUrl, String healthPath, String basePath) {
        this(namespace, tbInternalUrl, healthPath, basePath, false, "");
    }

    public ExtensionManifestRenderer(String namespace, String tbInternalUrl, String healthPath, String basePath,
                                     boolean networkPolicyEnabled, String npFromNamespaceLabel) {
        this(namespace, tbInternalUrl, healthPath, basePath, networkPolicyEnabled, npFromNamespaceLabel, "",
                EGRESS_BASELINE, DEFAULT_BLOCKED_EGRESS_CIDRS, "");
    }

    public ExtensionManifestRenderer(String namespace, String tbInternalUrl, String healthPath, String basePath,
                                     boolean networkPolicyEnabled, String npFromNamespaceLabel, String npFromPodLabel,
                                     String egressPolicy, String blockedEgressCidrs, String allowedEgressCidrs) {
        this(namespace, tbInternalUrl, healthPath, basePath, networkPolicyEnabled, npFromNamespaceLabel, npFromPodLabel,
                egressPolicy, blockedEgressCidrs, allowedEgressCidrs, true);
    }

    public ExtensionManifestRenderer(String namespace, String tbInternalUrl, String healthPath, String basePath,
                                     boolean networkPolicyEnabled, String npFromNamespaceLabel, String npFromPodLabel,
                                     String egressPolicy, String blockedEgressCidrs, String allowedEgressCidrs,
                                     boolean alwaysPull) {
        this(namespace, tbInternalUrl, healthPath, basePath, networkPolicyEnabled, npFromNamespaceLabel, npFromPodLabel,
                egressPolicy, blockedEgressCidrs, allowedEgressCidrs, alwaysPull,
                DEFAULT_DNS_NAMESPACE_SELECTOR, DEFAULT_DNS_POD_SELECTOR);
    }

    public ExtensionManifestRenderer(String namespace, String tbInternalUrl, String healthPath, String basePath,
                                     boolean networkPolicyEnabled, String npFromNamespaceLabel, String npFromPodLabel,
                                     String egressPolicy, String blockedEgressCidrs, String allowedEgressCidrs,
                                     boolean alwaysPull, String dnsNamespaceSelector, String dnsPodSelector) {
        this(namespace, tbInternalUrl, healthPath, basePath, networkPolicyEnabled, npFromNamespaceLabel, npFromPodLabel,
                egressPolicy, blockedEgressCidrs, allowedEgressCidrs, alwaysPull,
                dnsNamespaceSelector, dnsPodSelector, DEFAULT_DNS_SERVICE_CIDRS);
    }

    public ExtensionManifestRenderer(String namespace, String tbInternalUrl, String healthPath, String basePath,
                                     boolean networkPolicyEnabled, String npFromNamespaceLabel, String npFromPodLabel,
                                     String egressPolicy, String blockedEgressCidrs, String allowedEgressCidrs,
                                     boolean alwaysPull, String dnsNamespaceSelector, String dnsPodSelector,
                                     String dnsServiceCidrs) {
        this.alwaysPull = alwaysPull;
        this.namespace = namespace;
        this.tbInternalUrl = tbInternalUrl;
        this.healthPath = StringUtils.isNotBlank(healthPath) ? healthPath : "/api/health";
        this.basePath = StringUtils.isNotBlank(basePath) ? basePath : ExtensionEnv.DEFAULT_BASE_PATH;
        this.networkPolicyEnabled = networkPolicyEnabled;
        this.npFromNamespaceLabel = requireLabelSelector(npFromNamespaceLabel,
                "extensions.k8s.network-policy.from-namespace-selector");
        this.npFromPodLabel = requireLabelSelector(npFromPodLabel,
                "extensions.k8s.network-policy.from-pod-selector");
        this.egressPolicy = requireEgressPolicy(egressPolicy);
        this.blockedEgressCidrs = parseCsv(blockedEgressCidrs);
        this.allowedEgressCidrs = parseCsv(allowedEgressCidrs);
        this.dnsNamespaceLabel = requireLabelSelector(dnsNamespaceSelector,
                "extensions.k8s.network-policy.dns-namespace-selector");
        this.dnsPodLabel = requireLabelSelector(dnsPodSelector,
                "extensions.k8s.network-policy.dns-pod-selector");
        this.dnsServiceCidrs = parseCsv(dnsServiceCidrs);
    }

    public boolean isNetworkPolicyEnabled() {
        return networkPolicyEnabled;
    }

    // Operator-level inputs injected into the rendered objects (env URLs, health path, network policy)
    // but absent from the user spec. Folded into the spec-hash so changing any of them re-converges
    // existing workloads instead of leaving them on stale values.
    public String renderContext() {
        return String.join("\n",
                tbInternalUrl, healthPath, basePath,
                Boolean.toString(networkPolicyEnabled),
                npFromNamespaceLabel, npFromPodLabel, egressPolicy,
                String.join(",", blockedEgressCidrs),
                String.join(",", allowedEgressCidrs),
                Boolean.toString(alwaysPull),
                dnsNamespaceLabel, dnsPodLabel,
                String.join(",", dnsServiceCidrs));
    }

    /**
     * Convenience overload that stamps no spec-hash. Only for callers that never write the result back
     * (tests, inspection): an object rendered without the hash annotation can never be recognised as
     * converged, so every apply would rewrite it.
     */
    public NetworkPolicy renderNetworkPolicy(Extension extension) {
        return renderNetworkPolicy(extension, null);
    }

    // Targets the container port BY NAME, not the Service port (which a namespace-wide rule would
    // mis-target after kube-proxy DNAT) and not the number either: apply() writes this policy and the
    // Service before the Deployment rolls, so a numeric port would move to the new value while the old
    // pods — kept serving by maxUnavailable=0 — still listen on the old one, and nothing would reach them
    // for the length of the rollout. The name resolves per pod, so old and new pods are both selected.
    public NetworkPolicy renderNetworkPolicy(Extension extension, String specHash) {
        if (!networkPolicyEnabled) {
            return null;
        }
        var ingress = new NetworkPolicyIngressRuleBuilder()
                .addNewPort().withProtocol("TCP").withPort(new IntOrString(CONTAINER_PORT_NAME)).endPort();
        NetworkPolicyPeer from = peer(npFromPodLabel, npFromNamespaceLabel);
        if (from != null) {
            ingress.withFrom(from);
        }
        boolean egressRestricted = !EGRESS_OPEN.equals(egressPolicy);
        List<String> policyTypes = new ArrayList<>();
        policyTypes.add("Ingress");
        if (egressRestricted) {
            policyTypes.add("Egress");
        }
        var spec = new NetworkPolicyBuilder()
                .withNewMetadata()
                .withName(ExtensionResourceNames.networkPolicy(extension.getSlug()))
                .withNamespace(namespace)
                .withLabels(ExtensionResourceNames.baseLabels(extension))
                .withAnnotations(specHashAnnotation(specHash))
                .endMetadata()
                .withNewSpec()
                .withNewPodSelector()
                .addToMatchLabels(ExtensionResourceNames.LABEL_APP_INSTANCE,
                        ExtensionResourceNames.instanceLabel(extension.getSlug()))
                .endPodSelector()
                .withPolicyTypes(policyTypes)
                .withIngress(ingress.build());
        if (egressRestricted) {
            spec = spec.withEgress(egressRules());
        }
        return spec.endSpec().build();
    }

    /**
     * Builds one NetworkPolicy peer out of an optional pod selector and an optional namespace selector.
     *
     * <p>Both may be set: a single peer carrying both means "pods matching the pod selector IN namespaces
     * matching the namespace selector" (logical AND) — e.g. the router pod in its namespace. A pod
     * selector alone matches that label only within the policy's own (shared) namespace, which is what
     * isolates one tenant pod from another.
     *
     * <p>Returns null when neither selector is set. For the ingress source that means "allow all
     * sources"; for the DNS egress destination it means the unscoped rule.
     *
     * <p>Both selectors are already validated as {@code key=value} by {@link #requireLabelSelector}.
     */
    private static NetworkPolicyPeer peer(String podLabel, String nsLabel) {
        boolean hasPod = hasSelector(podLabel);
        boolean hasNs = hasSelector(nsLabel);
        if (!hasPod && !hasNs) {
            return null;
        }
        NetworkPolicyPeerBuilder peer = new NetworkPolicyPeerBuilder();
        if (hasPod) {
            String[] kv = podLabel.split("=", 2);
            peer.withNewPodSelector().addToMatchLabels(kv[0].trim(), kv[1].trim()).endPodSelector();
        }
        if (hasNs) {
            String[] kv = nsLabel.split("=", 2);
            peer.withNewNamespaceSelector().addToMatchLabels(kv[0].trim(), kv[1].trim()).endNamespaceSelector();
        }
        return peer.build();
    }

    private static boolean hasSelector(String labelSelector) {
        return StringUtils.isNotBlank(labelSelector);
    }

    /**
     * Accepts a blank selector (meaning "not configured") or a single {@code key=value} pair, and refuses
     * anything else at startup.
     *
     * <p>Silently ignoring a malformed value is not a safe default here: an unusable pod selector makes
     * {@code peer(...)} return null, the rendered ingress rule then carries no {@code from} at all, and a
     * NetworkPolicy ingress rule with an empty {@code from} matches EVERY source — which removes exactly
     * the control that keeps one tenant's pod from connecting straight to another's in the shared
     * namespace. A YAML-style {@code key: value}, a set-based expression or a bare key are all easy to
     * write and would each land in that state without a word in the log.
     */
    private static String requireLabelSelector(String labelSelector, String property) {
        if (StringUtils.isBlank(labelSelector)) {
            return "";
        }
        String trimmed = labelSelector.trim();
        String[] kv = trimmed.split("=", 2);
        if (kv.length != 2 || kv[0].trim().isEmpty() || kv[1].trim().isEmpty()) {
            throw new IllegalArgumentException(property + " must be a single 'labelKey=labelValue' pair, but was '"
                    + trimmed + "'. Leave it empty to apply no source restriction.");
        }
        return trimmed;
    }

    /**
     * Refuses an unknown egress policy instead of falling back. A typo such as {@code restrictd} used to
     * render the {@code baseline} rule set — allow-all-except-the-blocked-CIDRs — for an operator who
     * asked for deny-all-except-the-allowlist, with nothing to indicate the request had been ignored.
     */
    private static String requireEgressPolicy(String egressPolicy) {
        if (StringUtils.isBlank(egressPolicy)) {
            return EGRESS_BASELINE;
        }
        String normalized = egressPolicy.trim().toLowerCase();
        if (!EGRESS_OPEN.equals(normalized) && !EGRESS_BASELINE.equals(normalized)
                && !EGRESS_RESTRICTED.equals(normalized)) {
            throw new IllegalArgumentException("extensions.k8s.network-policy.egress-policy must be one of '"
                    + EGRESS_OPEN + "', '" + EGRESS_BASELINE + "', '" + EGRESS_RESTRICTED + "', but was '"
                    + egressPolicy.trim() + "'.");
        }
        return normalized;
    }

    private List<NetworkPolicyEgressRule> egressRules() {
        List<NetworkPolicyEgressRule> rules = new ArrayList<>();
        // DNS is always allowed so the pod can resolve names — but scoped to the cluster DNS pods. An
        // egress rule with no 'to' matches EVERY destination, and egress rules are OR-ed, so an unscoped
        // one hands any tenant an unrestricted bidirectional channel to anything listening on port 53,
        // straight through the deny-by-default the 'restricted' mode is supposed to provide. Scoping it
        // is a no-op under 'baseline' (the wide rule already covers port 53), so a selector that does not
        // match this cluster's DNS cannot break name resolution there — only under 'restricted', where
        // the operator is configuring peers deliberately anyway.
        NetworkPolicyEgressRuleBuilder dns = new NetworkPolicyEgressRuleBuilder()
                .addNewPort().withProtocol("UDP").withPort(new IntOrString(53)).endPort()
                .addNewPort().withProtocol("TCP").withPort(new IntOrString(53)).endPort();
        // Everything below is skipped when the DNS rule is unscoped (both selectors empty): that rule
        // already matches every destination on port 53, so adding a narrower peer to it changes nothing.
        NetworkPolicyPeer dnsPeer = peer(dnsPodLabel, dnsNamespaceLabel);
        if (dnsPeer != null) {
            dns.withTo(dnsPeer);
            // Peers within one rule are OR-ed, so the kube-dns Service CIDR sits next to the selector
            // rather than replacing it: CNIs that match egress after DNAT keep using the selector, and the
            // AWS VPC CNI — which matches the ClusterIP before DNAT and can never satisfy a pod selector —
            // is served by this ipBlock. Without it, egress-policy=restricted denies DNS outright there.
            for (String cidr : dnsServiceCidrs) {
                dns.addNewTo().withNewIpBlock().withCidr(cidr).endIpBlock().endTo();
            }
        }
        rules.add(dns.build());
        if (EGRESS_RESTRICTED.equals(egressPolicy)) {
            // Deny-by-default: only the explicitly allow-listed CIDRs (e.g. TB core) are reachable.
            for (String cidr : allowedEgressCidrs) {
                rules.add(new NetworkPolicyEgressRuleBuilder()
                        .addNewTo().withNewIpBlock().withCidr(cidr).endIpBlock().endTo()
                        .build());
            }
        } else {
            // baseline: allow all egress EXCEPT the blocked CIDRs (cloud metadata, optional pod/service CIDRs).
            // Split by address family: a NetworkPolicy ipBlock 'except' must sit inside its 'cidr', so an IPv6
            // block cannot be an exception of 0.0.0.0/0. Emit a matching '::/0' rule so IPv6 metadata is blocked
            // too on dual-stack clusters. When no IPv6 block is configured, leave v6 egress denied by default
            // rather than opening it wide.
            List<String> blockedV4 = new ArrayList<>();
            List<String> blockedV6 = new ArrayList<>();
            for (String cidr : blockedEgressCidrs) {
                (cidr.contains(":") ? blockedV6 : blockedV4).add(cidr);
            }
            rules.add(baselineEgressRule("0.0.0.0/0", blockedV4));
            if (!blockedV6.isEmpty()) {
                rules.add(baselineEgressRule("::/0", blockedV6));
            }
        }
        return rules;
    }

    // "allow all in this family EXCEPT the blocked CIDRs" as one egress rule. 'except' is omitted when empty,
    // since a NetworkPolicy ipBlock with an empty except is pointless noise.
    private static NetworkPolicyEgressRule baselineEgressRule(String cidr, List<String> except) {
        var ipBlock = new NetworkPolicyEgressRuleBuilder().addNewTo().withNewIpBlock().withCidr(cidr);
        if (!except.isEmpty()) {
            ipBlock.withExcept(except);
        }
        return ipBlock.endIpBlock().endTo().build();
    }

    // User-supplied env may carry secrets, so it lives in a Secret (not a plaintext ConfigMap).
    public Secret renderEnvSecret(Extension extension, String specHash) {
        Map<String, String> data = new LinkedHashMap<>();
        ExtensionContainerSpec spec = extension.getContainerSpec();
        if (spec != null && spec.getEnv() != null) {
            data.putAll(spec.getEnv());
        }
        return new SecretBuilder()
                .withNewMetadata()
                .withName(ExtensionResourceNames.envSecret(extension.getSlug()))
                .withNamespace(namespace)
                .withLabels(ExtensionResourceNames.baseLabels(extension))
                .withAnnotations(Map.of(ExtensionResourceNames.ANNOTATION_SPEC_HASH, specHash))
                .endMetadata()
                .withType("Opaque")
                .withStringData(data)
                .build();
    }

    // dockerconfigjson Secret built from the resolved registry credentials. The password arrives here
    // already resolved from the tenant's ThingsBoard Secret (see ExtensionReconcileService); plaintext
    // lives only in this namespaced Secret, never on the extension entity.
    public Secret renderPullSecret(Extension extension, String specHash) {
        RegistryAuth auth = extension.getContainerSpec().getRegistryAuth();
        String registry = ExtensionResourceNames.resolveRegistry(auth, extension.getContainerSpec().getImage());
        String basic = Base64.getEncoder().encodeToString(
                (auth.getUsername() + ":" + auth.getPassword()).getBytes(StandardCharsets.UTF_8));
        ObjectNode entry = JacksonUtil.newObjectNode();
        entry.put("username", auth.getUsername());
        entry.put("password", auth.getPassword());
        entry.put("auth", basic);
        ObjectNode auths = JacksonUtil.newObjectNode();
        auths.set(registry, entry);
        ObjectNode root = JacksonUtil.newObjectNode();
        root.set("auths", auths);
        return new SecretBuilder()
                .withNewMetadata()
                .withName(ExtensionResourceNames.pullSecret(extension.getSlug()))
                .withNamespace(namespace)
                .withLabels(ExtensionResourceNames.baseLabels(extension))
                .withAnnotations(Map.of(ExtensionResourceNames.ANNOTATION_SPEC_HASH, specHash))
                .endMetadata()
                .withType("kubernetes.io/dockerconfigjson")
                .withStringData(Map.of(".dockerconfigjson", root.toString()))
                .build();
    }

    public Deployment renderDeployment(Extension extension, String specHash, Long restartedAt) {
        ExtensionContainerSpec spec = extension.getContainerSpec();
        Map<String, String> labels = ExtensionResourceNames.baseLabels(extension);

        Map<String, String> annotations = new LinkedHashMap<>();
        annotations.put(ExtensionResourceNames.ANNOTATION_SPEC_HASH, specHash);
        if (restartedAt != null) {
            annotations.put(ExtensionResourceNames.ANNOTATION_RESTARTED_AT, restartedAt.toString());
        }
        Map<String, String> templateAnnotations = new LinkedHashMap<>(annotations);

        EnvFromSource envFrom = new EnvFromSourceBuilder()
                .withSecretRef(new SecretEnvSourceBuilder()
                        .withName(ExtensionResourceNames.envSecret(extension.getSlug()))
                        .build())
                .build();

        int replicas = extension.getState() != null
                && extension.getState() == ExtensionState.STOPPED
                ? 0 : spec.getReplicas();
        // 'IfNotPresent' lets a node-cached layer start without consulting this pod's imagePullSecrets,
        // so any tenant that names an image:tag another tenant already pulled runs it without holding
        // registry credentials. 'Always' makes every pod start re-authenticate against the registry.
        String imagePullPolicy = alwaysPull || "latest".equalsIgnoreCase(spec.getTag()) ? "Always" : "IfNotPresent";

        DeploymentBuilder builder = new DeploymentBuilder()
                .withNewMetadata()
                .withName(ExtensionResourceNames.deployment(extension.getSlug()))
                .withNamespace(namespace)
                .withLabels(labels)
                .withAnnotations(annotations)
                .endMetadata()
                .withNewSpec()
                .withReplicas(replicas)
                .withNewSelector()
                .withMatchLabels(Map.of(ExtensionResourceNames.LABEL_APP_INSTANCE,
                        ExtensionResourceNames.instanceLabel(extension.getSlug())))
                .endSelector()
                .withNewStrategy()
                .withType("RollingUpdate")
                .withNewRollingUpdate()
                .withNewMaxSurge(1)
                .withNewMaxUnavailable(0)
                .endRollingUpdate()
                .endStrategy()
                .withNewTemplate()
                .withNewMetadata()
                .withLabels(labels)
                .withAnnotations(templateAnnotations)
                .endMetadata()
                .withNewSpec()
                // Untrusted workload: do not hand it the namespace's ServiceAccount token.
                .withAutomountServiceAccountToken(false)
                // Do not leak other tenants' Service names/ClusterIPs through *_SERVICE_HOST env vars.
                .withEnableServiceLinks(false)
                .withImagePullSecrets(imagePullSecrets(extension))
                .withNewSecurityContext()
                // runAsNonRoot enforces non-root; the image's own USER (UID) is respected — not pinned.
                .withRunAsNonRoot(true)
                .withNewSeccompProfile().withType("RuntimeDefault").endSeccompProfile()
                .endSecurityContext()
                .addNewContainer()
                .withName(ExtensionResourceNames.CONTAINER_NAME)
                .withImage(spec.getImage() + ":" + spec.getTag())
                .withImagePullPolicy(imagePullPolicy)
                .addNewPort().withContainerPort(spec.getPort()).withName(CONTAINER_PORT_NAME).endPort()
                .withEnvFrom(envFrom)
                .withEnv(builtInEnv(extension))
                .withResources(buildResources(spec.getResources()))
                .addNewVolumeMount().withName("tmp").withMountPath("/tmp").endVolumeMount()
                .withNewSecurityContext()
                .withAllowPrivilegeEscalation(false)
                .withReadOnlyRootFilesystem(true)
                .withNewCapabilities().withDrop("ALL").endCapabilities()
                .endSecurityContext()
                // A startupProbe holds liveness off until the container has answered once. Without it a slow
                // image (a Spring Boot service needing ~80s to warm up) is killed by liveness at ~55s
                // — initialDelay 15s + 3 x 20s at the Kubernetes default failureThreshold — and
                // CrashLoopBackOffs forever even though the image is correct. STARTUP_FAILURE_THRESHOLD x
                // STARTUP_PERIOD_SECONDS is the whole budget a container gets to come up.
                .withNewStartupProbe()
                .withNewHttpGet().withPath(healthPath).withPort(new IntOrString(spec.getPort())).endHttpGet()
                .withPeriodSeconds(STARTUP_PERIOD_SECONDS)
                .withFailureThreshold(STARTUP_FAILURE_THRESHOLD)
                .withTimeoutSeconds(PROBE_TIMEOUT_SECONDS)
                .endStartupProbe()
                .withNewReadinessProbe()
                .withNewHttpGet().withPath(healthPath).withPort(new IntOrString(spec.getPort())).endHttpGet()
                .withInitialDelaySeconds(5).withPeriodSeconds(10)
                .withTimeoutSeconds(PROBE_TIMEOUT_SECONDS)
                .endReadinessProbe()
                .withNewLivenessProbe()
                .withNewHttpGet().withPath(healthPath).withPort(new IntOrString(spec.getPort())).endHttpGet()
                .withInitialDelaySeconds(15).withPeriodSeconds(20)
                .withTimeoutSeconds(PROBE_TIMEOUT_SECONDS)
                .endLivenessProbe()
                .endContainer()
                .addNewVolume().withName("tmp").withNewEmptyDir().withSizeLimit(new Quantity(SCRATCH_SIZE)).endEmptyDir().endVolume()
                .endSpec()
                .endTemplate()
                .endSpec();
        return builder.build();
    }

    /** See {@link #renderNetworkPolicy(Extension)} — convenience overload that stamps no spec-hash. */
    public Service renderService(Extension extension) {
        return renderService(extension, null);
    }

    public Service renderService(Extension extension, String specHash) {
        ExtensionContainerSpec spec = extension.getContainerSpec();
        return new ServiceBuilder()
                .withNewMetadata()
                .withName(ExtensionResourceNames.service(extension.getSlug()))
                .withNamespace(namespace)
                .withLabels(ExtensionResourceNames.baseLabels(extension))
                .withAnnotations(specHashAnnotation(specHash))
                .endMetadata()
                .withNewSpec()
                .withType("ClusterIP")
                .addToSelector(ExtensionResourceNames.LABEL_APP_INSTANCE,
                        ExtensionResourceNames.instanceLabel(extension.getSlug()))
                .addNewPort()
                .withName(CONTAINER_PORT_NAME)
                .withPort(ExtensionResourceNames.SERVICE_PORT)
                // by name, see renderNetworkPolicy: a port change must not cut traffic during the rollout
                .withTargetPort(new IntOrString(CONTAINER_PORT_NAME))
                .withProtocol("TCP")
                .endPort()
                .endSpec()
                .build();
    }

    // Stamped on every rendered object so a re-apply can tell "already at the desired spec" from "needs a
    // write" without diffing the object itself. An empty map (not null) when no hash is passed: fabric8
    // treats null as "leave whatever is there", which would keep a stale annotation on an update.
    private static Map<String, String> specHashAnnotation(String specHash) {
        return specHash == null ? Map.of() : Map.of(ExtensionResourceNames.ANNOTATION_SPEC_HASH, specHash);
    }

    private List<LocalObjectReference> imagePullSecrets(Extension extension) {
        if (extension.getContainerSpec().getRegistryAuth() == null) {
            return List.of();
        }
        return List.of(new LocalObjectReference(ExtensionResourceNames.pullSecret(extension.getSlug())));
    }

    private List<EnvVar> builtInEnv(Extension extension) {
        List<EnvVar> env = new ArrayList<>();
        ExtensionEnv.builtIns(extension, basePath, tbInternalUrl).forEach((k, v) -> env.add(envVar(k, v)));
        return env;
    }

    private static EnvVar envVar(String name, String value) {
        return new EnvVarBuilder().withName(name).withValue(value).build();
    }

    private static List<String> parseCsv(String raw) {
        List<String> out = new ArrayList<>();
        if (raw != null) {
            for (String part : raw.split(",")) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty()) {
                    out.add(trimmed);
                }
            }
        }
        return out;
    }

    // The defaults and the "blank limit reuses the effective request" rule live in ResourceSpec,
    // shared with ExtensionDataValidator so the two can never drift apart.
    private static ResourceRequirements buildResources(ResourceSpec resources) {
        ResourceSpec spec = resources != null ? resources : new ResourceSpec();
        Map<String, Quantity> requests = new LinkedHashMap<>();
        requests.put("cpu", new Quantity(spec.effectiveCpuRequest()));
        requests.put("memory", new Quantity(spec.effectiveMemoryRequest()));
        requests.put("ephemeral-storage", new Quantity(SCRATCH_SIZE));
        Map<String, Quantity> limits = new LinkedHashMap<>();
        limits.put("cpu", new Quantity(spec.effectiveCpuLimit()));
        limits.put("memory", new Quantity(spec.effectiveMemoryLimit()));
        // Without this the pod can write to /tmp until the node's disk fills: ephemeral storage is not
        // covered by the tenant CPU/memory quota, nor by the namespace ResourceQuota, so it is a way to
        // hurt every workload on the node while touching no quota dimension at all. The Docker backend
        // caps the same scratch space with a tmpfs size, so this also keeps the substrates consistent.
        limits.put("ephemeral-storage", new Quantity(SCRATCH_SIZE));
        return new ResourceRequirementsBuilder().withRequests(requests).withLimits(limits).build();
    }

}
