// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.extension.k8s;

import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.networking.v1.NetworkPolicy;
import io.fabric8.kubernetes.api.model.networking.v1.NetworkPolicyBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.extension.Extension;
import org.thingsboard.server.common.data.extension.ExtensionContainerSpec;
import org.thingsboard.server.common.data.extension.ExtensionState;
import org.thingsboard.server.common.data.id.ExtensionId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.service.extension.spi.DeploymentHealth;
import org.thingsboard.server.service.extension.spi.ExtensionReconciler;
import org.thingsboard.server.service.extension.spi.ExtensionResourceNames;

import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Exercises {@link DefaultExtensionK8sReconciler} against an in-memory CRUD-emulating Kubernetes API
 * (fabric8 mock). Covers apply (objects + per-extension NetworkPolicy on the actual container port),
 * idempotency, describe, restart, and teardown — the wiring the pure unit tests can't reach.
 */
@EnableKubernetesMockClient(crud = true)
class DefaultExtensionK8sReconcilerTest {

    static KubernetesClient client;

    private static final String NS = "tb-extensions";

    private DefaultExtensionK8sReconciler reconciler() {
        ExtensionManifestRenderer renderer = new ExtensionManifestRenderer(
                NS, "http://tb-node:8080", "/api/health", "/api/extension/route", true, "");
        DefaultExtensionK8sReconciler r = new DefaultExtensionK8sReconciler(client, NS, renderer);
        // the mock API server runs no controllers, so no pod ever terminates on its own
        r.setPodTerminationWaitMs(50);
        return r;
    }

    private Extension extension(int port) {
        Extension e = new Extension(new ExtensionId(UUID.randomUUID()));
        e.setTenantId(TenantId.fromUUID(UUID.randomUUID()));
        e.setName("Acme Billing");
        e.setSlug("acme-billing-9f3a");
        e.setState(ExtensionState.PROVISIONING);
        ExtensionContainerSpec spec = new ExtensionContainerSpec();
        spec.setImage("registry.example.com/acme/billing-ext");
        spec.setTag("1.4.0");
        spec.setReplicas(2);
        spec.setPort(port);
        e.setContainerSpec(spec);
        return e;
    }

    @Test
    void applyCreatesAllObjectsWithNetworkPolicyOnContainerPort() {
        Extension ext = extension(9000);
        reconciler().apply(ext);

        assertThat(client.apps().deployments().inNamespace(NS).withName("tb-ext-acme-billing-9f3a").get()).isNotNull();
        assertThat(client.services().inNamespace(NS).withName("tb-ext-acme-billing-9f3a").get()).isNotNull();
        assertThat(client.secrets().inNamespace(NS).withName("tb-ext-acme-billing-9f3a-env").get()).isNotNull();

        NetworkPolicy np = client.network().v1().networkPolicies().inNamespace(NS).withName("tb-ext-acme-billing-9f3a").get();
        assertThat(np).isNotNull();
        assertThat(np.getSpec().getIngress().get(0).getPorts().get(0).getPort().getStrVal()).isEqualTo("http");
    }

    // The periodic resync re-applies every owned extension. Before the spec-hash annotation reached the
    // Service and the NetworkPolicy, both were rewritten on every one of those passes — an API-server
    // round trip (and full admission chain) per extension per pass, changing nothing. The trailing
    // port change proves the assertion has teeth: a real spec change still rewrites both.
    @Test
    void reapplyOfAnUnchangedSpecWritesNeitherServiceNorNetworkPolicy() {
        Extension ext = extension(9000);
        DefaultExtensionK8sReconciler r = reconciler();
        r.apply(ext);
        String serviceVersion = serviceResourceVersion();
        String policyVersion = networkPolicyResourceVersion();

        r.apply(ext);

        assertThat(serviceResourceVersion()).isEqualTo(serviceVersion);
        assertThat(networkPolicyResourceVersion()).isEqualTo(policyVersion);

        ext.getContainerSpec().setPort(9100);
        r.apply(ext);

        assertThat(serviceResourceVersion()).isNotEqualTo(serviceVersion);
        assertThat(networkPolicyResourceVersion()).isNotEqualTo(policyVersion);
    }

    private String serviceResourceVersion() {
        return client.services().inNamespace(NS).withName("tb-ext-acme-billing-9f3a").get()
                .getMetadata().getResourceVersion();
    }

    private String networkPolicyResourceVersion() {
        return client.network().v1().networkPolicies().inNamespace(NS).withName("tb-ext-acme-billing-9f3a").get()
                .getMetadata().getResourceVersion();
    }

    @Test
    void applyIsIdempotentOnUnchangedSpec() {
        Extension ext = extension(8090);
        DefaultExtensionK8sReconciler r = reconciler();
        r.apply(ext);
        String hash1 = ExtensionResourceNames.annotation(
                client.apps().deployments().inNamespace(NS).withName("tb-ext-acme-billing-9f3a").get().getMetadata().getAnnotations(),
                ExtensionResourceNames.ANNOTATION_SPEC_HASH);
        r.apply(ext); // second apply: spec hash unchanged -> no-op, must not throw
        String hash2 = ExtensionResourceNames.annotation(
                client.apps().deployments().inNamespace(NS).withName("tb-ext-acme-billing-9f3a").get().getMetadata().getAnnotations(),
                ExtensionResourceNames.ANNOTATION_SPEC_HASH);
        assertThat(hash1).isEqualTo(hash2);
    }

    @Test
    void describeReflectsDesiredReplicas() {
        Extension ext = extension(8090);
        DefaultExtensionK8sReconciler r = reconciler();
        r.apply(ext);
        DeploymentHealth health = r.describe(ext);
        assertThat(health.exists()).isTrue();
        assertThat(health.desiredReplicas()).isEqualTo(2);
    }

    // The guard was only wired into the apply path, so a tenant whose slug collided with an
    // operator-owned Deployment (no managed-by label) could still scale, restart or delete it by name.
    private void unmanagedDeployment(String name) {
        client.apps().deployments().inNamespace(NS).resource(new DeploymentBuilder()
                .withNewMetadata().withName(name).withNamespace(NS)
                .withLabels(Map.of("app.kubernetes.io/name", "tb-ext-router"))
                .endMetadata()
                .withNewSpec().withReplicas(3)
                .withNewSelector().withMatchLabels(Map.of("app.kubernetes.io/name", "tb-ext-router")).endSelector()
                .withNewTemplate().withNewMetadata()
                .withLabels(Map.of("app.kubernetes.io/name", "tb-ext-router")).endMetadata()
                .withNewSpec().addNewContainer().withName("nginx").withImage("nginx:1.27").endContainer()
                .endSpec().endTemplate()
                .endSpec().build()).create();
    }

    @Test
    void scaleRefusesToTouchUnmanagedDeployment() {
        Extension ext = extension(8090);
        ext.setSlug("operator-owned-scale"); // the mock API server is shared across tests
        unmanagedDeployment(ExtensionResourceNames.deployment(ext.getSlug()));

        assertThatThrownBy(() -> reconciler().scaleTo(ext, 0)).isInstanceOf(IllegalStateException.class);
        assertThat(client.apps().deployments().inNamespace(NS)
                .withName(ExtensionResourceNames.deployment(ext.getSlug())).get()
                .getSpec().getReplicas()).isEqualTo(3);
    }

    @Test
    void restartRefusesToTouchUnmanagedDeployment() {
        Extension ext = extension(8090);
        ext.setSlug("operator-owned-restart");
        unmanagedDeployment(ExtensionResourceNames.deployment(ext.getSlug()));

        assertThatThrownBy(() -> reconciler().restart(ext)).isInstanceOf(IllegalStateException.class);
        assertThat(ExtensionResourceNames.annotation(client.apps().deployments().inNamespace(NS)
                .withName(ExtensionResourceNames.deployment(ext.getSlug())).get().getMetadata().getAnnotations(),
                ExtensionResourceNames.ANNOTATION_RESTARTED_AT)).isNull();
    }

    // scale/restart must treat a foreign-owner Deployment (previous slug holder, possibly another
    // tenant's) as absent, like the read paths do.
    @Test
    void scaleTreatsForeignOwnerDeploymentAsAbsent() {
        Extension previous = extension(8090);
        previous.setSlug("foreign-owner-scale");
        reconciler().apply(previous);
        Extension current = extension(8090);
        current.setSlug("foreign-owner-scale"); // same slug, different extension id + tenant

        reconciler().scaleTo(current, 0);

        assertThat(client.apps().deployments().inNamespace(NS)
                .withName(ExtensionResourceNames.deployment("foreign-owner-scale")).get()
                .getSpec().getReplicas()).isEqualTo(2);
    }

    @Test
    void restartTreatsForeignOwnerDeploymentAsAbsent() {
        Extension previous = extension(8090);
        previous.setSlug("foreign-owner-restart");
        reconciler().apply(previous);
        Extension current = extension(8090);
        current.setSlug("foreign-owner-restart");

        reconciler().restart(current);

        assertThat(ExtensionResourceNames.annotation(client.apps().deployments().inNamespace(NS)
                .withName(ExtensionResourceNames.deployment("foreign-owner-restart")).get().getMetadata().getAnnotations(),
                ExtensionResourceNames.ANNOTATION_RESTARTED_AT)).isNull();
    }

    // Delete skips rather than throwing: teardown must still clear the objects we do own, and the orphan
    // sweep would otherwise retry against the same collision forever.
    @Test
    void deleteSkipsUnmanagedDeploymentButStillRemovesOwnObjects() {
        Extension ext = extension(8090);
        ext.setSlug("acme-billing-del");
        reconciler().apply(ext);
        String otherSlug = "operator-owned-cache";
        unmanagedDeployment(ExtensionResourceNames.deployment(otherSlug));

        reconciler().deleteBySlug(otherSlug);
        assertThat(client.apps().deployments().inNamespace(NS)
                .withName(ExtensionResourceNames.deployment(otherSlug)).get()).isNotNull();

        reconciler().deleteBySlug(ext.getSlug());
        assertThat(client.apps().deployments().inNamespace(NS)
                .withName(ExtensionResourceNames.deployment(ext.getSlug())).get()).isNull();
        assertThat(client.secrets().inNamespace(NS)
                .withName(ExtensionResourceNames.envSecret(ext.getSlug())).get()).isNull();
    }

    // Turning the feature off must remove the policies it created, the same way apply() removes a pull
    // Secret once registryAuth is gone. Otherwise every existing extension keeps enforcing its old
    // ingress rule — which admits only the previously configured router selector — so an operator who
    // disables network policies to unblock traffic finds those extensions unreachable instead, and
    // nothing ever cleans up: the orphan sweep only touches slugs whose DB row is gone.
    @Test
    void disablingNetworkPoliciesRemovesTheOneAlreadyApplied() {
        Extension ext = extension(9000);
        ext.setSlug("acme-nopolicy-5e6f");
        String name = ExtensionResourceNames.networkPolicy(ext.getSlug());

        reconciler().apply(ext);
        assertThat(client.network().v1().networkPolicies().inNamespace(NS).withName(name).get()).isNotNull();

        reconcilerWithoutNetworkPolicies().apply(ext);

        assertThat(client.network().v1().networkPolicies().inNamespace(NS).withName(name).get()).isNull();
        // the workload itself must survive the flag flip
        assertThat(client.apps().deployments().inNamespace(NS)
                .withName(ExtensionResourceNames.deployment(ext.getSlug())).get()).isNotNull();
    }

    private DefaultExtensionK8sReconciler reconcilerWithoutNetworkPolicies() {
        ExtensionManifestRenderer renderer = new ExtensionManifestRenderer(
                NS, "http://tb-node:8080", "/api/health", "/api/extension/route", false, "");
        return new DefaultExtensionK8sReconciler(client, NS, renderer);
    }

    // Scaling must be a scale, not a redeploy: the pod-template hash has to stay put so Kubernetes only
    // changes spec.replicas and leaves the running pods alone.
    @Test
    void changingReplicasScalesWithoutTouchingThePodTemplate() {
        Extension ext = extension(8090);
        ext.setSlug("acme-scaling-3d4e");
        DefaultExtensionK8sReconciler r = reconciler();
        r.apply(ext);
        String name = ExtensionResourceNames.deployment(ext.getSlug());
        String templateHashBefore = ExtensionResourceNames.annotation(
                client.apps().deployments().inNamespace(NS).withName(name).get()
                        .getSpec().getTemplate().getMetadata().getAnnotations(),
                ExtensionResourceNames.ANNOTATION_SPEC_HASH);

        ext.getContainerSpec().setReplicas(3);
        r.apply(ext);

        var updated = client.apps().deployments().inNamespace(NS).withName(name).get();
        assertThat(updated.getSpec().getReplicas()).isEqualTo(3);
        assertThat(ExtensionResourceNames.annotation(
                updated.getSpec().getTemplate().getMetadata().getAnnotations(),
                ExtensionResourceNames.ANNOTATION_SPEC_HASH)).isEqualTo(templateHashBefore);
    }

    // The orphan sweep is the only net for a teardown that dies partway through. deleteBySlug removes the
    // Deployment first (so the workload stops while its NetworkPolicy still constrains it), which means a
    // failure afterwards leaves Secrets/Service/NetworkPolicy with no Deployment. Discovering orphans by
    // Deployment alone would hide those forever — and the env/pull Secrets hold resolved env values and
    // registry credentials in plaintext.
    @Test
    void listManagedSlugsFindsLeftoversAfterThePartialTeardownOfADeployment() {
        Extension ext = extension(8090);
        ext.setSlug("acme-partial-1a2b");
        DefaultExtensionK8sReconciler r = reconciler();
        r.apply(ext);

        // simulate teardown dying right after the Deployment delete succeeded
        client.apps().deployments().inNamespace(NS)
                .withName(ExtensionResourceNames.deployment(ext.getSlug())).delete();
        assertThat(client.secrets().inNamespace(NS)
                .withName(ExtensionResourceNames.envSecret(ext.getSlug())).get()).isNotNull();

        assertThat(r.listManagedSlugs()).contains(ext.getSlug());

        // and the sweep's follow-up delete actually clears what was left behind
        r.deleteBySlug(ext.getSlug());
        assertThat(client.secrets().inNamespace(NS)
                .withName(ExtensionResourceNames.envSecret(ext.getSlug())).get()).isNull();
        assertThat(client.services().inNamespace(NS)
                .withName(ExtensionResourceNames.service(ext.getSlug())).get()).isNull();
        assertThat(client.network().v1().networkPolicies().inNamespace(NS)
                .withName(ExtensionResourceNames.networkPolicy(ext.getSlug())).get()).isNull();
        assertThat(r.listManagedSlugs()).doesNotContain(ext.getSlug());
    }

    // The list endpoint resolves a whole page through this, so it must answer for every slug it is given
    // from the label-filtered listings rather than per-extension lookups, and ignore slugs it was not
    // asked about (the namespace is shared by every tenant).
    @Test
    void describeAllAnswersForEveryRequestedSlugAndIgnoresOthers() {
        DefaultExtensionK8sReconciler r = reconciler();
        Extension first = extension(8090);
        Extension second = extension(8091);
        second.setSlug("acme-invoices-1b7c");
        Extension notRequested = extension(8092);
        notRequested.setSlug("acme-reports-77aa");
        r.apply(first);
        r.apply(second);
        r.apply(notRequested);

        var health = r.describeAll(java.util.List.of(first, second));

        assertThat(health).containsOnlyKeys(first.getSlug(), second.getSlug());
        assertThat(health.get(first.getSlug()).exists()).isTrue();
        assertThat(health.get(first.getSlug()).desiredReplicas()).isEqualTo(2);
        assertThat(health.get(second.getSlug()).desiredReplicas()).isEqualTo(2);
    }

    // A slug with no workload is simply absent, which the caller reads as "no live snapshot" and falls
    // back to the DB row instead of failing the page.
    @Test
    void describeAllOmitsSlugsWithNoWorkload() {
        DefaultExtensionK8sReconciler r = reconciler();
        Extension missing = extension(8093);
        missing.setSlug("acme-nothing-0000");

        assertThat(r.describeAll(java.util.List.of(missing))).isEmpty();
        assertThat(r.describeAll(java.util.List.of())).isEmpty();
    }

    @Test
    void restartBumpsTemplateAnnotation() {
        Extension ext = extension(8090);
        DefaultExtensionK8sReconciler r = reconciler();
        r.apply(ext);
        r.restart(ext);
        Deployment d = client.apps().deployments().inNamespace(NS).withName("tb-ext-acme-billing-9f3a").get();
        assertThat(d.getSpec().getTemplate().getMetadata().getAnnotations())
                .containsKey(ExtensionResourceNames.ANNOTATION_RESTARTED_AT);
    }

    // The Deployment delete returns as soon as the API server accepts it, and the pods then run for their
    // termination grace period. Deleting the NetworkPolicy in the same breath left tenant code with no
    // IMDS block and no router-only ingress for that window. The policy must outlive the pods: while any
    // pod of the slug remains it stays, and only once they are gone does it go.
    @Test
    void networkPolicyOutlivesTerminatingPods() {
        Extension ext = extension(8090);
        ext.setSlug("np-outlives-pods");
        DefaultExtensionK8sReconciler r = reconciler();
        r.apply(ext);
        podOf(ext, "tb-ext-np-outlives-pods-abc12"); // a pod still terminating after the Deployment delete

        r.delete(ext);

        String name = ExtensionResourceNames.networkPolicy(ext.getSlug());
        assertThat(client.apps().deployments().inNamespace(NS).withName(name).get()).isNull();
        assertThat(client.network().v1().networkPolicies().inNamespace(NS).withName(name).get())
                .as("policy kept while the pod is alive").isNotNull();

        client.pods().inNamespace(NS).withName("tb-ext-np-outlives-pods-abc12").delete();
        r.deleteBySlug(ext.getSlug()); // the orphan sweep's follow-up
        assertThat(client.network().v1().networkPolicies().inNamespace(NS).withName(name).get()).isNull();
    }

    // A retry, or the orphan sweep's follow-up, arrives after an earlier teardown already removed the
    // Deployment. The pod that made that earlier call leave the policy behind may STILL be terminating:
    // the pod check must not be skipped just because there was no Deployment left to delete this time.
    @Test
    void repeatedTeardownKeepsTheNetworkPolicyWhileAPodRemains() {
        Extension ext = extension(8090);
        ext.setSlug("np-repeat-teardown");
        DefaultExtensionK8sReconciler r = reconciler();
        r.apply(ext);
        podOf(ext, "tb-ext-np-repeat-teardown-1");
        String name = ExtensionResourceNames.networkPolicy(ext.getSlug());

        r.delete(ext);        // Deployment gone, pod still there -> policy kept
        r.deleteBySlug(ext.getSlug()); // the follow-up, pod STILL there
        r.delete(ext);        // and an ordinary retry

        assertThat(client.apps().deployments().inNamespace(NS).withName(name).get()).isNull();
        assertThat(client.network().v1().networkPolicies().inNamespace(NS).withName(name).get())
                .as("policy kept for as long as the pod exists").isNotNull();

        client.pods().inNamespace(NS).withName("tb-ext-np-repeat-teardown-1").delete();
        r.deleteBySlug(ext.getSlug());
        assertThat(client.network().v1().networkPolicies().inNamespace(NS).withName(name).get()).isNull();
    }

    // Teardown retries for seconds after the row is gone while the slug is reusable at once. An extension
    // re-created under the same name meanwhile owns a workload with the same object names; the old
    // extension's teardown must not remove it.
    @Test
    void deleteLeavesTheWorkloadOfANewOwnerOfTheSlugAlone() {
        Extension previous = extension(8090);
        previous.setSlug("reused-slug-delete");
        Extension current = extension(8090);
        current.setSlug("reused-slug-delete"); // same slug, different extension id + tenant
        DefaultExtensionK8sReconciler r = reconciler();
        r.apply(current);

        r.delete(previous);

        String name = ExtensionResourceNames.deployment(current.getSlug());
        assertThat(client.apps().deployments().inNamespace(NS).withName(name).get()).isNotNull();
        assertThat(client.network().v1().networkPolicies().inNamespace(NS).withName(name).get()).isNotNull();
        assertThat(client.secrets().inNamespace(NS).withName(ExtensionResourceNames.envSecret(current.getSlug())).get()).isNotNull();
    }

    @Test
    void deleteRemovesAllObjectsIncludingNetworkPolicy() {
        Extension ext = extension(9000);
        DefaultExtensionK8sReconciler r = reconciler();
        r.apply(ext);
        r.delete(ext);
        assertThat(client.apps().deployments().inNamespace(NS).withName("tb-ext-acme-billing-9f3a").get()).isNull();
        assertThat(client.services().inNamespace(NS).withName("tb-ext-acme-billing-9f3a").get()).isNull();
        assertThat(client.secrets().inNamespace(NS).withName("tb-ext-acme-billing-9f3a-env").get()).isNull();
        assertThat(client.network().v1().networkPolicies().inNamespace(NS).withName("tb-ext-acme-billing-9f3a").get()).isNull();
    }

    // the orphan sweep (R4-5) relies on these two: managed deployments are found by label,
    // and a slug-only delete removes the full object set without an Extension row
    @Test
    void listManagedSlugsFindsAppliedDeploymentsByLabel() {
        Extension ext = extension(8090);
        DefaultExtensionK8sReconciler r = reconciler();
        r.apply(ext);

        assertThat(r.listManagedSlugs()).contains("acme-billing-9f3a");
    }

    @Test
    void deleteBySlugRemovesAllObjectsWithoutAnExtensionRow() {
        Extension ext = extension(8090);
        DefaultExtensionK8sReconciler r = reconciler();
        r.apply(ext);

        r.deleteBySlug("acme-billing-9f3a");

        assertThat(r.listManagedSlugs()).doesNotContain("acme-billing-9f3a");
        assertThat(client.apps().deployments().inNamespace(NS).withName("tb-ext-acme-billing-9f3a").get()).isNull();
        assertThat(client.services().inNamespace(NS).withName("tb-ext-acme-billing-9f3a").get()).isNull();
        assertThat(client.secrets().inNamespace(NS).withName("tb-ext-acme-billing-9f3a-env").get()).isNull();
        assertThat(client.network().v1().networkPolicies().inNamespace(NS).withName("tb-ext-acme-billing-9f3a").get()).isNull();
    }

    // A log read fans out over the extension's pods, one API-server round-trip each, all on the caller's
    // thread. The cap is what stops that cost from scaling with replica count, so a scaled-out extension
    // must be read partially and say so, rather than reading every pod. (The mock API server does not
    // serve the log subresource, so each read reports its own failure inline — which is exactly the
    // per-pod error handling this asserts is reached for the capped set and no further.)
    @Test
    void logTailStopsAtTheConfiguredPodCap() {
        Extension ext = extension(8090);
        // Own slug: the mock API server is shared by every test in this class, so pods labelled for the
        // common slug would show up in the other tests' describe() calls.
        ext.setSlug("log-fanout-7c21");
        int pods = DefaultExtensionK8sReconciler.DEFAULT_MAX_LOG_PODS + 3;
        for (int i = 0; i < pods; i++) {
            podOf(ext, "tb-ext-log-fanout-7c21-" + i);
        }

        String logs = reconciler().tailLogs(ext, 200);

        long readPods = logs.lines().filter(line -> line.startsWith("==== ")).count();
        assertThat(readPods).isEqualTo(DefaultExtensionK8sReconciler.DEFAULT_MAX_LOG_PODS);
        assertThat(logs).contains("showing " + DefaultExtensionK8sReconciler.DEFAULT_MAX_LOG_PODS + " of " + pods + " pods");
    }

    // A per-pod read failure is reported inline, but never with the raw fabric8 message: that carries the
    // API-server URL and the pod path, and the text goes straight to the tenant.
    @Test
    void logReadErrorsDoNotLeakTheApiServerAddress() {
        String logs = DefaultExtensionK8sReconciler.assembleLogTail(List.of("tb-ext-redact-1"), 1, (pod, room) -> {
            throw new KubernetesClientException("Failure executing: GET at: "
                    + "https://10.96.0.1:443/api/v1/namespaces/tb-extensions/pods/tb-ext-redact-1/log. Message: Forbidden.");
        });

        assertThat(logs).contains("could not read logs").contains("Forbidden");
        assertThat(logs).doesNotContain("https://").doesNotContain("10.96.0.1");
    }

    @Test
    void logTailReportsWhenThereAreNoPods() {
        Extension ext = extension(8090);
        ext.setSlug("never-deployed-0000");

        assertThat(reconciler().tailLogs(ext, 200)).contains("No pods found");
    }

    // A pod header is written before its log, so the budget has to be reserved for it. Checking only the
    // buffer length lets the header cross the ceiling and leaves a negative budget: the next read would
    // ask the API server for a negative number of bytes and the copy into the buffer would throw.
    @Test
    void logTailNeverAsksForANonPositiveBudget() {
        List<String> podNames = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            podNames.add("tb-ext-budget-boundary-" + i);
        }
        // Each pod fills a fortieth of the ceiling, so the last sections land on the boundary.
        String filler = "x".repeat(ExtensionReconciler.MAX_LOG_RESPONSE_CHARS / 40);
        List<Integer> budgets = new ArrayList<>();

        String logs = DefaultExtensionK8sReconciler.assembleLogTail(podNames, podNames.size(), (pod, room) -> {
            budgets.add(room);
            return filler;
        });

        assertThat(budgets).isNotEmpty().allMatch(room -> room > 0);
        assertThat(logs.length()).isLessThanOrEqualTo(ExtensionReconciler.MAX_LOG_RESPONSE_CHARS);
    }

    @Test
    void logTailStaysWithinTheCeilingWhenOnePodFillsIt() {
        String huge = "y".repeat(ExtensionReconciler.MAX_LOG_RESPONSE_CHARS * 2);

        String logs = DefaultExtensionK8sReconciler.assembleLogTail(
                List.of("tb-ext-huge-0"), 1, (pod, room) -> huge);

        assertThat(logs.length()).isLessThanOrEqualTo(ExtensionReconciler.MAX_LOG_RESPONSE_CHARS);
        assertThat(logs).endsWith(DefaultExtensionK8sReconciler.TRUNCATION_MARK);
    }

    // The client error message is written outside the per-pod budget, and the tenant influences how long
    // it is, so the ceiling has to hold for the assembled text and not only for the log bodies.
    @Test
    void logTailKeepsAnErrorMessageWithinTheCeiling() {
        String logs = DefaultExtensionK8sReconciler.assembleLogTail(
                List.of("tb-ext-error-0"), 1, (pod, room) -> {
                    throw new IllegalStateException("e".repeat(ExtensionReconciler.MAX_LOG_RESPONSE_CHARS * 2));
                });

        assertThat(logs.length()).isLessThanOrEqualTo(ExtensionReconciler.MAX_LOG_RESPONSE_CHARS);
    }

    // Same for the "showing N of M pods" footer, which is appended after the loop has spent the budget.
    @Test
    void logTailKeepsTheFooterWithinTheCeiling() {
        String huge = "z".repeat(ExtensionReconciler.MAX_LOG_RESPONSE_CHARS);

        String logs = DefaultExtensionK8sReconciler.assembleLogTail(
                List.of("tb-ext-footer-0", "tb-ext-footer-1"), 9, (pod, room) -> huge);

        assertThat(logs.length()).isLessThanOrEqualTo(ExtensionReconciler.MAX_LOG_RESPONSE_CHARS);
    }

    // A pod exactly as the renderer stamps it: the instance label plus the owner labels (managed-by,
    // tenant id, extension id) that identify whose workload it is.
    private void podOf(Extension owner, String name) {
        Map<String, String> labels = new HashMap<>(ExtensionResourceNames.baseLabels(owner));
        client.pods().inNamespace(NS).resource(new PodBuilder()
                .withNewMetadata()
                .withName(name)
                .withNamespace(NS)
                .withLabels(labels)
                .endMetadata()
                .build()).create();
    }

    // A slug is released the moment its row is deleted, while teardown runs asynchronously — and the
    // orphan sweep then skips the slug because a live row owns it again. So the previous owner's pods can
    // still answer to this slug's instance label, and reading them would hand one tenant another tenant's
    // logs. Only the owner labels tell the two apart.
    @Test
    void logTailIgnoresPodsLeftByAPreviousOwnerOfTheSlug() {
        Extension previous = extension(8090);
        previous.setSlug("reused-slug-logs");
        podOf(previous, "tb-ext-reused-slug-logs-old");

        Extension current = extension(8090);
        current.setSlug("reused-slug-logs"); // same slug, different tenant and extension id

        assertThat(reconciler().tailLogs(current, 200)).contains("No pods found");
    }

    @Test
    void describeIgnoresADeploymentLeftByAPreviousOwnerOfTheSlug() {
        Extension previous = extension(8090);
        previous.setSlug("reused-slug-describe");
        reconciler().apply(previous);

        Extension current = extension(8090);
        current.setSlug("reused-slug-describe");

        assertThat(reconciler().describe(current).exists()).isFalse();
        assertThat(reconciler().describeAll(java.util.List.of(current))).isEmpty();
    }

    // The spec-hash covers the container spec only, so the previous owner's Deployment can carry exactly
    // the hash this extension wants — and treating that as converged would leave the other tenant's
    // workload serving this extension's traffic under its own owner labels forever.
    @Test
    void applyRewritesADeploymentLeftByAPreviousOwnerEvenAtTheSameHash() {
        Extension previous = extension(8090);
        previous.setSlug("reused-slug-apply");
        reconciler().apply(previous);

        Extension current = extension(8090); // identical spec => identical hash
        current.setSlug("reused-slug-apply");
        reconciler().apply(current);

        Deployment d = client.apps().deployments().inNamespace(NS)
                .withName(ExtensionResourceNames.deployment(current.getSlug())).get();
        assertThat(d.getMetadata().getLabels())
                .containsEntry(ExtensionResourceNames.LABEL_EXTENSION_ID, current.getId().getId().toString())
                .containsEntry(ExtensionResourceNames.LABEL_TENANT_ID, current.getTenantId().getId().toString());
        assertThat(reconciler().describe(current).exists()).isTrue();
    }

    // Both of these delete an object whose name derives from the tenant's slug. They used to call delete()
    // straight, bypassing the ownership guard every other path in the class applies — so a slug collision
    // could destroy an operator-owned object. The pull-Secret branch runs on EVERY apply of an extension
    // without registryAuth, which makes it the most reachable of the two.
    @Test
    void applyDoesNotDeleteAnUnmanagedPullSecret() {
        Extension ext = extension(8090);
        ext.setSlug("operator-owned-pull");
        String name = ExtensionResourceNames.pullSecret(ext.getSlug());
        client.secrets().inNamespace(NS).resource(new SecretBuilder()
                .withNewMetadata().withName(name).withNamespace(NS)
                .withLabels(Map.of("app.kubernetes.io/name", "tb-ext-router"))
                .endMetadata().build()).create();

        reconciler().apply(ext); // no registryAuth -> the "remove any stale pull Secret" branch

        assertThat(client.secrets().inNamespace(NS).withName(name).get()).isNotNull();
    }

    @Test
    void applyDoesNotDeleteAnUnmanagedNetworkPolicy() {
        Extension ext = extension(8090);
        ext.setSlug("operator-owned-np");
        String name = ExtensionResourceNames.networkPolicy(ext.getSlug());
        client.network().v1().networkPolicies().inNamespace(NS).resource(new NetworkPolicyBuilder()
                .withNewMetadata().withName(name).withNamespace(NS)
                .withLabels(Map.of("app.kubernetes.io/name", "tb-ext-router"))
                .endMetadata().build()).create();

        reconcilerWithoutNetworkPolicies().apply(ext); // policies off -> the "remove any stale policy" branch

        assertThat(client.network().v1().networkPolicies().inNamespace(NS).withName(name).get()).isNotNull();
    }

    // A crash-looping container spends most of its time in `waiting: CrashLoopBackOff`, but between
    // attempts it really is running — and while it is, the waiting state is gone. Reading the reason off
    // the waiting state alone made a poll landing in that window see a healthy pod, so the extension
    // blinked Failed -> Active -> Failed at the poll rate. The reason must survive the running window.
    @Test
    void crashLoopIsReportedWhileTheContainerIsBetweenRestarts() {
        Extension ext = extension(8090);
        DefaultExtensionK8sReconciler r = reconciler();
        r.apply(ext);
        crashLoopingPod(ext, "running-window", false, 3, 137, "OOMKilled");

        assertThat(r.describe(ext).errorReason()).isEqualTo("CrashLoopBackOff: OOMKilled");
    }

    // The mirror image: readiness is what stops a container that restarted once and then recovered from
    // being reported as failing for the rest of the pod's life.
    @Test
    void aRestartThatRecoveredIsNotReportedAsCrashLooping() {
        Extension ext = extension(8090);
        ext.setSlug("recovered-1a2b");
        DefaultExtensionK8sReconciler r = reconciler();
        r.apply(ext);
        crashLoopingPod(ext, "recovered", true, 3, 137, "OOMKilled");

        assertThat(r.describe(ext).errorReason()).isNull();
    }

    private void crashLoopingPod(Extension ext, String name, boolean ready, int restarts, int exitCode, String reason) {
        Map<String, String> labels = new HashMap<>(ExtensionResourceNames.baseLabels(ext));
        client.pods().inNamespace(NS).resource(new PodBuilder()
                .withNewMetadata().withName(name).withNamespace(NS).withLabels(labels).endMetadata()
                .withNewStatus()
                .withPhase("Running")
                .addNewContainerStatus()
                .withName(ExtensionResourceNames.CONTAINER_NAME)
                .withReady(ready)
                .withRestartCount(restarts)
                .withNewState().withNewRunning().endRunning().endState()
                .withNewLastState().withNewTerminated()
                .withExitCode(exitCode).withReason(reason)
                .endTerminated().endLastState()
                .endContainerStatus()
                .endStatus()
                .build()).create();
    }

    // The reconcile brain delegates terminal-vs-transient classification to this backend: 5xx /
    // timeout / throttling / no-response (connect failure) / IO errors retry; 4xx fail fast.
    @Test
    void retryClassification() {
        assertThat(DefaultExtensionK8sReconciler.isRetryableK8s(new KubernetesClientException("x", 500, null))).isTrue();
        assertThat(DefaultExtensionK8sReconciler.isRetryableK8s(new KubernetesClientException("x", 429, null))).isTrue();
        assertThat(DefaultExtensionK8sReconciler.isRetryableK8s(new KubernetesClientException("x", 0, null))).isTrue();
        assertThat(DefaultExtensionK8sReconciler.isRetryableK8s(new RuntimeException(new SocketTimeoutException("t")))).isTrue();
        assertThat(DefaultExtensionK8sReconciler.isRetryableK8s(new KubernetesClientException("x", 400, null))).isFalse();
        assertThat(DefaultExtensionK8sReconciler.isRetryableK8s(new KubernetesClientException("x", 403, null))).isFalse();
        assertThat(DefaultExtensionK8sReconciler.isRetryableK8s(new IllegalStateException("x"))).isFalse();
        // 409 is the API server's optimistic-concurrency signal, which any read-modify-write here hits
        // whenever the Deployment controller writes status mid-rollout. Treating it as terminal flipped
        // healthy extensions to FAILED after a Restart; the retry re-reads and works from the new version.
        assertThat(DefaultExtensionK8sReconciler.isRetryableK8s(new KubernetesClientException("conflict", 409, null))).isTrue();
    }

}
