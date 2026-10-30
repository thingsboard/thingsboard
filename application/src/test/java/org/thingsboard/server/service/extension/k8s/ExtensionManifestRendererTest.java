// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.extension.k8s;

import com.fasterxml.jackson.databind.JsonNode;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.networking.v1.NetworkPolicy;
import org.junit.jupiter.api.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.extension.Extension;
import org.thingsboard.server.common.data.extension.ExtensionContainerSpec;
import org.thingsboard.server.common.data.extension.ExtensionState;
import org.thingsboard.server.common.data.extension.RegistryAuth;
import org.thingsboard.server.common.data.extension.ResourceSize;
import org.thingsboard.server.common.data.extension.ResourceSpec;
import org.thingsboard.server.common.data.id.ExtensionId;
import org.thingsboard.server.common.data.id.TenantId;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExtensionManifestRendererTest {

    private static final String NS = "tb-extensions";
    private final ExtensionManifestRenderer renderer =
            new ExtensionManifestRenderer(NS, "http://tb-node:8080", "/api/health", "/api/extension/route");

    private Extension extension(String tag, String registry, ExtensionState state) {
        Extension e = new Extension(new ExtensionId(UUID.randomUUID()));
        e.setTenantId(TenantId.fromUUID(UUID.randomUUID()));
        e.setName("Acme Billing");
        e.setSlug("acme-billing-9f3a");
        e.setState(state);
        ExtensionContainerSpec spec = new ExtensionContainerSpec();
        spec.setImage("registry.example.com/acme/billing-ext");
        spec.setTag(tag);
        if (registry != null) {
            RegistryAuth auth = new RegistryAuth();
            auth.setRegistry(registry);
            auth.setUsername("robot$acme");
            auth.setPassword("s3cr3t"); // already resolved by the reconcile service before rendering
            spec.setRegistryAuth(auth);
        }
        spec.setReplicas(2);
        spec.setPort(9000);
        spec.setEnv(Map.of("FEATURE_X", "on"));
        ResourceSpec res = new ResourceSpec();
        res.setSize(ResourceSize.SMALL); // cpuRequest 250m, cpuLimit 500m, memory 256Mi
        spec.setResources(res);
        e.setContainerSpec(spec);
        return e;
    }

    private Container container(Deployment d) {
        return d.getSpec().getTemplate().getSpec().getContainers().get(0);
    }

    @Test
    void deploymentCoreFields() {
        Deployment d = renderer.renderDeployment(extension("1.4.0", "acme-registry", ExtensionState.PROVISIONING), "abc123", null);
        assertThat(d.getMetadata().getName()).isEqualTo("tb-ext-acme-billing-9f3a");
        assertThat(d.getMetadata().getNamespace()).isEqualTo(NS);
        assertThat(d.getMetadata().getAnnotations()).containsEntry("tb.thingsboard.io/spec-hash", "abc123");
        assertThat(d.getSpec().getReplicas()).isEqualTo(2);

        Container c = container(d);
        assertThat(c.getName()).isEqualTo("extension");
        assertThat(c.getImage()).isEqualTo("registry.example.com/acme/billing-ext:1.4.0");
        assertThat(c.getImagePullPolicy()).isEqualTo("Always");
        assertThat(c.getPorts().get(0).getContainerPort()).isEqualTo(9000);
        assertThat(c.getEnvFrom().get(0).getSecretRef().getName()).isEqualTo("tb-ext-acme-billing-9f3a-env");
        assertThat(c.getReadinessProbe().getHttpGet().getPath()).isEqualTo("/api/health");
        assertThat(c.getLivenessProbe().getHttpGet().getPath()).isEqualTo("/api/health");
        assertThat(c.getSecurityContext().getAllowPrivilegeEscalation()).isFalse();
    }

    // A slow-starting image (a Spring Boot service needing ~80s to warm up) used to be killed by liveness
    // at ~55s and CrashLoopBackOff forever. The startup probe holds liveness off until it answers once.
    @Test
    void startupProbeGivesASlowImageTimeToComeUp() {
        Deployment d = renderer.renderDeployment(extension("1.4.0", null, ExtensionState.PROVISIONING), "h", null);
        Container c = container(d);

        assertThat(c.getStartupProbe()).as("startupProbe").isNotNull();
        assertThat(c.getStartupProbe().getHttpGet().getPath()).isEqualTo("/api/health");
        long budgetSeconds = (long) c.getStartupProbe().getPeriodSeconds() * c.getStartupProbe().getFailureThreshold();
        assertThat(budgetSeconds).as("startup budget in seconds").isGreaterThanOrEqualTo(120);
        // the Kubernetes default of 1s turns an occasionally slow health endpoint into a restart loop
        assertThat(c.getReadinessProbe().getTimeoutSeconds()).isGreaterThan(1);
        assertThat(c.getLivenessProbe().getTimeoutSeconds()).isGreaterThan(1);
    }

    @Test
    void builtInEnvInjected() {
        Deployment d = renderer.renderDeployment(extension("1.4.0", null, ExtensionState.PROVISIONING), "h", null);
        Map<String, String> env = container(d).getEnv().stream()
                .collect(Collectors.toMap(EnvVar::getName, EnvVar::getValue));
        assertThat(env).containsKeys("TB_EXT_SLUG", "TB_EXT_ID", "TB_EXT_TENANT_ID", "TB_EXT_BASE_PATH", "THINGSBOARD_URL");
        assertThat(env.get("TB_EXT_SLUG")).isEqualTo("acme-billing-9f3a");
        assertThat(env.get("TB_EXT_BASE_PATH")).isEqualTo("/api/extension/route/acme-billing-9f3a");
        assertThat(env.get("THINGSBOARD_URL")).isEqualTo("http://tb-node:8080");
    }

    @Test
    void pullSecretReferencedOnlyWhenRegistryAuthSet() {
        Deployment d = renderer.renderDeployment(extension("1.4.0", null, ExtensionState.PROVISIONING), "h", null);
        assertThat(d.getSpec().getTemplate().getSpec().getImagePullSecrets()).isEmpty();
        Deployment withAuth = renderer.renderDeployment(extension("1.4.0", "registry.example.com", ExtensionState.PROVISIONING), "h", null);
        assertThat(withAuth.getSpec().getTemplate().getSpec().getImagePullSecrets().get(0).getName())
                .isEqualTo("tb-ext-acme-billing-9f3a-pull");
    }

    @Test
    void renderPullSecretBuildsDockerConfigJson() {
        Secret s = renderer.renderPullSecret(extension("1.4.0", "registry.example.com", ExtensionState.PROVISIONING), "h7");
        assertThat(s.getMetadata().getName()).isEqualTo("tb-ext-acme-billing-9f3a-pull");
        assertThat(s.getType()).isEqualTo("kubernetes.io/dockerconfigjson");
        JsonNode entry = JacksonUtil.fromString(s.getStringData().get(".dockerconfigjson"), JsonNode.class)
                .get("auths").get("registry.example.com");
        assertThat(entry.get("username").asText()).isEqualTo("robot$acme");
        assertThat(entry.get("password").asText()).isEqualTo("s3cr3t");
        assertThat(entry.get("auth").asText())
                .isEqualTo(Base64.getEncoder().encodeToString("robot$acme:s3cr3t".getBytes(StandardCharsets.UTF_8)));
    }

    // A malformed selector used to be ignored, which made peer(...) return null and left the ingress
    // rule with no `from` — and an ingress rule with an empty `from` matches EVERY source, removing the
    // control that keeps one tenant's pod from reaching another's in the shared namespace. YAML-style
    // `key: value`, a set-based expression and a bare key are all easy to write, so refuse at startup.
    // An egress rule with no `to` matches EVERY destination, and egress rules are OR-ed — so an unscoped
    // DNS rule hands a tenant an unrestricted bidirectional channel to anything listening on port 53,
    // straight through the deny-by-default that `restricted` is meant to provide.
    @Test
    void dnsEgressIsScopedToClusterDns() {
        ExtensionManifestRenderer r = new ExtensionManifestRenderer(NS, "http://tb-node:8080", "/api/health",
                "/api/extension/route", true, "", ExtensionManifestRenderer.DEFAULT_FROM_POD_SELECTOR,
                ExtensionManifestRenderer.EGRESS_RESTRICTED,
                ExtensionManifestRenderer.DEFAULT_BLOCKED_EGRESS_CIDRS, "10.100.0.0/16");

        var egress = r.renderNetworkPolicy(extension("1.4.0", null, ExtensionState.PROVISIONING))
                .getSpec().getEgress();
        var dnsRule = egress.stream()
                .filter(rule -> rule.getPorts().stream().anyMatch(p -> p.getPort().getIntVal() == 53))
                .findFirst().orElseThrow();

        assertThat(dnsRule.getTo()).as("an empty 'to' matches every destination").isNotEmpty();
        assertThat(dnsRule.getTo().get(0).getPodSelector().getMatchLabels()).containsEntry("k8s-app", "kube-dns");
        assertThat(dnsRule.getTo().get(0).getNamespaceSelector().getMatchLabels())
                .containsEntry("kubernetes.io/metadata.name", "kube-system");
    }

    // A pod resolves through the kube-dns SERVICE ClusterIP, not a CoreDNS pod IP. The AWS VPC CNI matches
    // egress against that ClusterIP before kube-proxy rewrites it, and no pod/namespace selector can match
    // a virtual address — so under 'restricted' the selector-only rule denies DNS and every lookup fails
    // with "recv() failed (111: Connection refused) while resolving". The service CIDR must therefore be
    // an ADDITIONAL peer on the same rule (peers are OR-ed), never a replacement for the selector, which
    // is what CNIs matching after DNAT rely on. Observed live on EKS, 2026-08-11.
    @Test
    void dnsEgressAlsoAllowsTheKubeDnsServiceCidrForPreDnatCnis() {
        ExtensionManifestRenderer r = new ExtensionManifestRenderer(NS, "http://tb-node:8080", "/api/health",
                "/api/extension/route", true, "", ExtensionManifestRenderer.DEFAULT_FROM_POD_SELECTOR,
                ExtensionManifestRenderer.EGRESS_RESTRICTED,
                ExtensionManifestRenderer.DEFAULT_BLOCKED_EGRESS_CIDRS, "10.100.0.0/16", true,
                ExtensionManifestRenderer.DEFAULT_DNS_NAMESPACE_SELECTOR,
                ExtensionManifestRenderer.DEFAULT_DNS_POD_SELECTOR, "10.100.0.0/16");

        var dnsRule = r.renderNetworkPolicy(extension("1.4.0", null, ExtensionState.PROVISIONING))
                .getSpec().getEgress().stream()
                .filter(rule -> rule.getPorts().stream().anyMatch(p -> p.getPort().getIntVal() == 53))
                .findFirst().orElseThrow();

        assertThat(dnsRule.getTo()).as("selector peer must survive alongside the ipBlock").hasSize(2);
        assertThat(dnsRule.getTo().get(0).getPodSelector().getMatchLabels()).containsEntry("k8s-app", "kube-dns");
        assertThat(dnsRule.getTo().get(1).getIpBlock().getCidr()).isEqualTo("10.100.0.0/16");
    }

    // Default is empty, so no ipBlock appears and existing clusters render exactly as before.
    @Test
    void dnsServiceCidrIsAbsentByDefault() {
        ExtensionManifestRenderer r = new ExtensionManifestRenderer(NS, "http://tb-node:8080", "/api/health",
                "/api/extension/route", true, "", ExtensionManifestRenderer.DEFAULT_FROM_POD_SELECTOR,
                ExtensionManifestRenderer.EGRESS_RESTRICTED,
                ExtensionManifestRenderer.DEFAULT_BLOCKED_EGRESS_CIDRS, "10.100.0.0/16");

        var dnsRule = r.renderNetworkPolicy(extension("1.4.0", null, ExtensionState.PROVISIONING))
                .getSpec().getEgress().stream()
                .filter(rule -> rule.getPorts().stream().anyMatch(p -> p.getPort().getIntVal() == 53))
                .findFirst().orElseThrow();

        assertThat(dnsRule.getTo()).hasSize(1);
        assertThat(dnsRule.getTo().get(0).getIpBlock()).isNull();
    }

    // With both selectors blank the DNS rule is already unscoped — it matches every destination on port 53 —
    // so narrowing peers must not be appended: that would leave the rule exactly as permissive while
    // implying otherwise to anyone reading the rendered policy.
    @Test
    void dnsServiceCidrIsNotAddedToAnAlreadyUnscopedDnsRule() {
        ExtensionManifestRenderer r = new ExtensionManifestRenderer(NS, "http://tb-node:8080", "/api/health",
                "/api/extension/route", true, "", ExtensionManifestRenderer.DEFAULT_FROM_POD_SELECTOR,
                ExtensionManifestRenderer.EGRESS_RESTRICTED,
                ExtensionManifestRenderer.DEFAULT_BLOCKED_EGRESS_CIDRS, "10.100.0.0/16", true,
                "", "", "10.100.0.0/16");

        var dnsRule = r.renderNetworkPolicy(extension("1.4.0", null, ExtensionState.PROVISIONING))
                .getSpec().getEgress().stream()
                .filter(rule -> rule.getPorts().stream().anyMatch(p -> p.getPort().getIntVal() == 53))
                .findFirst().orElseThrow();

        assertThat(dnsRule.getTo()).isNullOrEmpty();
    }

    // The CIDR is an operator input that changes the rendered policy, so it belongs in the spec hash —
    // otherwise setting it would leave already-deployed extensions on the old, DNS-denying policy.
    @Test
    void dnsServiceCidrIsPartOfTheRenderContext() {
        ExtensionManifestRenderer without = new ExtensionManifestRenderer(NS, "http://tb-node:8080", "/api/health",
                "/api/extension/route", true, "", ExtensionManifestRenderer.DEFAULT_FROM_POD_SELECTOR,
                ExtensionManifestRenderer.EGRESS_RESTRICTED,
                ExtensionManifestRenderer.DEFAULT_BLOCKED_EGRESS_CIDRS, "10.100.0.0/16");
        ExtensionManifestRenderer with = new ExtensionManifestRenderer(NS, "http://tb-node:8080", "/api/health",
                "/api/extension/route", true, "", ExtensionManifestRenderer.DEFAULT_FROM_POD_SELECTOR,
                ExtensionManifestRenderer.EGRESS_RESTRICTED,
                ExtensionManifestRenderer.DEFAULT_BLOCKED_EGRESS_CIDRS, "10.100.0.0/16", true,
                ExtensionManifestRenderer.DEFAULT_DNS_NAMESPACE_SELECTOR,
                ExtensionManifestRenderer.DEFAULT_DNS_POD_SELECTOR, "10.100.0.0/16");

        assertThat(with.renderContext()).isNotEqualTo(without.renderContext());
    }

    @Test
    void malformedIngressSelectorsAreRejectedAtStartup() {
        assertThatThrownBy(() -> rendererWithPodSelector("app.kubernetes.io/name: tb-ext-router"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("from-pod-selector")
                .hasMessageContaining("labelKey=labelValue");
        assertThatThrownBy(() -> rendererWithPodSelector("app in (proxy)"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> rendererWithPodSelector("bareKey"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> rendererWithPodSelector("=novalue"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void blankIngressSelectorStaysAllowedAndMeansNoRestriction() {
        NetworkPolicy policy = rendererWithPodSelector("")
                .renderNetworkPolicy(extension("1.4.0", null, ExtensionState.PROVISIONING));
        assertThat(policy.getSpec().getIngress().get(0).getFrom()).isNullOrEmpty();
    }

    // A typo used to render the permissive baseline rule set for an operator who asked for restricted.
    @Test
    void unknownEgressPolicyIsRejectedAtStartup() {
        assertThatThrownBy(() -> new ExtensionManifestRenderer(NS, "http://tb-node:8080", "/api/health",
                "/api/extension/route", true, "", ExtensionManifestRenderer.DEFAULT_FROM_POD_SELECTOR,
                "restrictd", ExtensionManifestRenderer.DEFAULT_BLOCKED_EGRESS_CIDRS, ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("egress-policy");
    }

    private ExtensionManifestRenderer rendererWithPodSelector(String podSelector) {
        return new ExtensionManifestRenderer(NS, "http://tb-node:8080", "/api/health", "/api/extension/route",
                true, "", podSelector, ExtensionManifestRenderer.EGRESS_BASELINE,
                ExtensionManifestRenderer.DEFAULT_BLOCKED_EGRESS_CIDRS, "");
    }

    // A read-only root filesystem still needs somewhere to write, but an uncapped emptyDir lets a tenant
    // fill the node's disk and evict co-located pods — including other tenants' extensions — while
    // consuming no CPU or memory it is billed for. Ephemeral storage is covered by neither the tenant
    // quota nor the namespace ResourceQuota, so the bound has to live on the pod itself.
    @Test
    void scratchVolumeAndEphemeralStorageAreBounded() {
        Deployment d = renderer.renderDeployment(extension("1.4.0", null, ExtensionState.PROVISIONING), "h", null);

        var tmp = d.getSpec().getTemplate().getSpec().getVolumes().stream()
                .filter(v -> "tmp".equals(v.getName())).findFirst().orElseThrow();
        assertThat(tmp.getEmptyDir().getSizeLimit().toString()).isEqualTo(ExtensionManifestRenderer.SCRATCH_SIZE);

        var res = container(d).getResources();
        assertThat(res.getLimits().get("ephemeral-storage").toString())
                .isEqualTo(ExtensionManifestRenderer.SCRATCH_SIZE);
        assertThat(res.getRequests().get("ephemeral-storage").toString())
                .isEqualTo(ExtensionManifestRenderer.SCRATCH_SIZE);
    }

    @Test
    void latestTagUsesAlwaysPullPolicy() {
        Deployment d = renderer.renderDeployment(extension("latest", null, ExtensionState.PROVISIONING), "h", null);
        assertThat(container(d).getImagePullPolicy()).isEqualTo("Always");
    }

    // A node-cached layer starts without consulting the pod's imagePullSecrets, so a pinned tag must
    // still re-authenticate: otherwise a tenant runs an image another tenant pulled with credentials
    // it does not hold. Only an operator opting out per extensions.image.always-pull=false gets the
    // node cache back.
    @Test
    void pinnedTagStillUsesAlwaysPullPolicy() {
        Deployment d = renderer.renderDeployment(extension("1.4.0", null, ExtensionState.PROVISIONING), "h", null);
        assertThat(container(d).getImagePullPolicy()).isEqualTo("Always");
    }

    @Test
    void alwaysPullDisabledFallsBackToIfNotPresentForPinnedTag() {
        ExtensionManifestRenderer r = new ExtensionManifestRenderer(NS, "http://tb-node:8080", "/api/health",
                "/api/extension/route", false, "", "", ExtensionManifestRenderer.EGRESS_BASELINE,
                ExtensionManifestRenderer.DEFAULT_BLOCKED_EGRESS_CIDRS, "", false);
        Deployment d = r.renderDeployment(extension("1.4.0", null, ExtensionState.PROVISIONING), "h", null);
        assertThat(container(d).getImagePullPolicy()).isEqualTo("IfNotPresent");
        // A mutable tag is pulled regardless of the flag.
        Deployment latest = r.renderDeployment(extension("latest", null, ExtensionState.PROVISIONING), "h", null);
        assertThat(container(latest).getImagePullPolicy()).isEqualTo("Always");
    }

    // The flag lands in the pod template, so flipping it must re-converge existing workloads.
    @Test
    void alwaysPullFeedsRenderContext() {
        ExtensionManifestRenderer off = new ExtensionManifestRenderer(NS, "http://tb-node:8080", "/api/health",
                "/api/extension/route", false, "", "", ExtensionManifestRenderer.EGRESS_BASELINE,
                ExtensionManifestRenderer.DEFAULT_BLOCKED_EGRESS_CIDRS, "", false);
        ExtensionManifestRenderer on = new ExtensionManifestRenderer(NS, "http://tb-node:8080", "/api/health",
                "/api/extension/route", false, "", "", ExtensionManifestRenderer.EGRESS_BASELINE,
                ExtensionManifestRenderer.DEFAULT_BLOCKED_EGRESS_CIDRS, "", true);
        assertThat(off.renderContext()).isNotEqualTo(on.renderContext());
    }

    @Test
    void stoppedRendersZeroReplicas() {
        Deployment d = renderer.renderDeployment(extension("1.4.0", null, ExtensionState.STOPPED), "h", null);
        assertThat(d.getSpec().getReplicas()).isZero();
    }

    @Test
    void resourceLimitsComeFromSizeTier() {
        Container c = container(renderer.renderDeployment(extension("1.4.0", null, ExtensionState.PROVISIONING), "h", null));
        var limits = c.getResources().getLimits();
        assertThat(limits.get("cpu").toString()).isEqualTo("500m"); // SMALL cpuLimit (burst above the 250m request)
        assertThat(limits.get("memory").toString()).isEqualTo("256Mi"); // SMALL memory (request == limit)
    }

    @Test
    void serviceExposes8090ToContainerPort() {
        Service s = renderer.renderService(extension("1.4.0", null, ExtensionState.PROVISIONING));
        assertThat(s.getMetadata().getName()).isEqualTo("tb-ext-acme-billing-9f3a");
        assertThat(s.getSpec().getPorts().get(0).getPort()).isEqualTo(8090);
        // by NAME, not number: a port change must keep routing to the old pods during the rollout
        assertThat(s.getSpec().getPorts().get(0).getTargetPort().getStrVal()).isEqualTo("http");
        Deployment d = renderer.renderDeployment(extension("1.4.0", null, ExtensionState.PROVISIONING), "h", null);
        var containerPort = d.getSpec().getTemplate().getSpec().getContainers().get(0).getPorts().get(0);
        assertThat(containerPort.getName()).isEqualTo("http");
        assertThat(containerPort.getContainerPort()).isEqualTo(9000);
    }

    @Test
    void envSecretHoldsUserEnv() {
        Secret secret = renderer.renderEnvSecret(extension("1.4.0", null, ExtensionState.PROVISIONING), "h");
        assertThat(secret.getMetadata().getName()).isEqualTo("tb-ext-acme-billing-9f3a-env");
        assertThat(secret.getStringData()).containsEntry("FEATURE_X", "on");
    }

    @Test
    void readOnlyRootFilesystemHardenedWithTmpVolume() {
        Deployment d = renderer.renderDeployment(extension("1.4.0", null, ExtensionState.PROVISIONING), "h", null);
        Container c = container(d);
        assertThat(c.getSecurityContext().getReadOnlyRootFilesystem()).isTrue();
        assertThat(c.getSecurityContext().getCapabilities().getDrop()).contains("ALL");
        assertThat(c.getVolumeMounts()).anyMatch(vm -> "/tmp".equals(vm.getMountPath()));
        assertThat(d.getSpec().getTemplate().getSpec().getVolumes()).anyMatch(v -> v.getEmptyDir() != null);
    }

    @Test
    void resourceLimitsAlwaysSetWithTheDefaultTierWhenOmitted() {
        Extension ext = extension("1.4.0", null, ExtensionState.PROVISIONING);
        ext.getContainerSpec().setResources(null);
        var res = container(renderer.renderDeployment(ext, "h", null)).getResources();
        // null resources -> ResourceSize.DEFAULT; assert against the tier, not its current numbers
        assertThat(res.getRequests().get("cpu").toString()).isEqualTo(ResourceSize.DEFAULT.getCpuRequest());
        assertThat(res.getRequests().get("memory").toString()).isEqualTo(ResourceSize.DEFAULT.getMemoryRequest());
        // limits ALWAYS present — cpu bursts to the tier limit, memory is pinned equal to the request
        assertThat(res.getLimits().get("cpu").toString()).isEqualTo(ResourceSize.DEFAULT.getCpuLimit());
        assertThat(res.getLimits().get("memory").toString()).isEqualTo(ResourceSize.DEFAULT.getMemoryLimit());
    }

    @Test
    void networkPolicyDisabledByDefault() {
        assertThat(renderer.renderNetworkPolicy(extension("1.4.0", null, ExtensionState.PROVISIONING))).isNull();
    }

    @Test
    void networkPolicyUsesContainerPortNotServicePort() {
        // Regression guard: a namespace-wide policy pinned to 8090 would blackhole non-default ports.
        ExtensionManifestRenderer np = new ExtensionManifestRenderer(
                NS, "http://tb-node:8080", "/api/health", "/api/extension/route", true, "");
        Extension ext = extension("1.4.0", null, ExtensionState.PROVISIONING); // container port 9000
        NetworkPolicy policy = np.renderNetworkPolicy(ext);
        assertThat(policy).isNotNull();
        assertThat(policy.getMetadata().getName()).isEqualTo("tb-ext-acme-billing-9f3a");
        assertThat(policy.getSpec().getPodSelector().getMatchLabels())
                .containsEntry("app.kubernetes.io/instance", "tb-ext-acme-billing-9f3a");
        var port = policy.getSpec().getIngress().get(0).getPorts().get(0).getPort();
        // the pod's container port by name (resolves per pod, so old and new pods stay selected across a
        // port change), NOT the Service's 8090
        assertThat(port.getStrVal()).isEqualTo("http");
        assertThat(policy.getSpec().getIngress().get(0).getFrom()).isNullOrEmpty(); // no source restriction configured
    }

    @Test
    void networkPolicyScopesSourceWhenConfigured() {
        ExtensionManifestRenderer np = new ExtensionManifestRenderer(
                NS, "http://tb-node:8080", "/api/health", "/api/extension/route", true, "kubernetes.io/metadata.name=ingress-nginx");
        NetworkPolicy policy = np.renderNetworkPolicy(extension("1.4.0", null, ExtensionState.PROVISIONING));
        var from = policy.getSpec().getIngress().get(0).getFrom();
        assertThat(from).hasSize(1);
        assertThat(from.get(0).getNamespaceSelector().getMatchLabels())
                .containsEntry("kubernetes.io/metadata.name", "ingress-nginx");
    }

    @Test
    void doesNotAutomountServiceAccountToken() {
        Deployment d = renderer.renderDeployment(extension("1.4.0", null, ExtensionState.PROVISIONING), "h", null);
        assertThat(d.getSpec().getTemplate().getSpec().getAutomountServiceAccountToken()).isFalse();
    }

    @Test
    void doesNotExposeKubernetesServiceLinks() {
        Deployment d = renderer.renderDeployment(extension("1.4.0", null, ExtensionState.PROVISIONING), "h", null);
        assertThat(d.getSpec().getTemplate().getSpec().getEnableServiceLinks()).isFalse();
    }

    @Test
    void podSecurityContextEnforcesNonRootAndSeccompWithoutPinningUid() {
        Deployment d = renderer.renderDeployment(extension("1.4.0", null, ExtensionState.PROVISIONING), "h", null);
        var sc = d.getSpec().getTemplate().getSpec().getSecurityContext();
        assertThat(sc.getRunAsNonRoot()).isTrue();
        assertThat(sc.getRunAsUser()).isNull(); // not pinned — the image's own non-root USER is respected
        assertThat(sc.getSeccompProfile().getType()).isEqualTo("RuntimeDefault");
    }

    @Test
    void egressBaselineBlocksCloudMetadata() {
        ExtensionManifestRenderer np = new ExtensionManifestRenderer(
                NS, "http://tb-node:8080", "/api/health", "/api/extension/route", true, "");
        NetworkPolicy policy = np.renderNetworkPolicy(extension("1.4.0", null, ExtensionState.PROVISIONING));
        assertThat(policy.getSpec().getPolicyTypes()).contains("Egress");
        boolean blocksMetadata = policy.getSpec().getEgress().stream()
                .flatMap(rule -> rule.getTo().stream())
                .filter(peer -> peer.getIpBlock() != null)
                .anyMatch(peer -> peer.getIpBlock().getExcept().contains("169.254.0.0/16"));
        assertThat(blocksMetadata).isTrue();
    }

    @Test
    void egressBaselineBlocksIpv6Metadata() {
        // Default blocked CIDRs include the AWS IMDS IPv6 address; it must land in a separate '::/0' rule,
        // because a NetworkPolicy ipBlock 'except' has to sit inside its 'cidr'.
        ExtensionManifestRenderer np = new ExtensionManifestRenderer(
                NS, "http://tb-node:8080", "/api/health", "/api/extension/route", true, "");
        NetworkPolicy policy = np.renderNetworkPolicy(extension("1.4.0", null, ExtensionState.PROVISIONING));
        boolean blocksV6Metadata = policy.getSpec().getEgress().stream()
                .flatMap(rule -> rule.getTo().stream())
                .filter(peer -> peer.getIpBlock() != null && "::/0".equals(peer.getIpBlock().getCidr()))
                .anyMatch(peer -> peer.getIpBlock().getExcept().contains("fd00:ec2::254/128"));
        assertThat(blocksV6Metadata).isTrue();
        // The IPv4 metadata block must not leak into the IPv6 rule's except (would be an invalid manifest).
        boolean v6RuleHasNoV4Except = policy.getSpec().getEgress().stream()
                .flatMap(rule -> rule.getTo().stream())
                .filter(peer -> peer.getIpBlock() != null && "::/0".equals(peer.getIpBlock().getCidr()))
                .noneMatch(peer -> peer.getIpBlock().getExcept().contains("169.254.0.0/16"));
        assertThat(v6RuleHasNoV4Except).isTrue();
    }

    @Test
    void egressOpenAddsNoEgressRules() {
        ExtensionManifestRenderer np = new ExtensionManifestRenderer(
                NS, "http://tb-node:8080", "/api/health", "/api/extension/route", true, "", "", "open", "169.254.169.254/32", "");
        NetworkPolicy policy = np.renderNetworkPolicy(extension("1.4.0", null, ExtensionState.PROVISIONING));
        assertThat(policy.getSpec().getPolicyTypes()).containsExactly("Ingress");
        assertThat(policy.getSpec().getEgress()).isNullOrEmpty();
    }

    @Test
    void networkPolicyScopesSourceToPodWhenConfigured() {
        // Pod selector alone -> only that pod label within the shared namespace may reach the extension.
        ExtensionManifestRenderer np = new ExtensionManifestRenderer(
                NS, "http://tb-node:8080", "/api/health", "/api/extension/route", true, "",
                "app.kubernetes.io/name=tb-ext-router", "baseline", "169.254.169.254/32", "");
        NetworkPolicy policy = np.renderNetworkPolicy(extension("1.4.0", null, ExtensionState.PROVISIONING));
        var from = policy.getSpec().getIngress().get(0).getFrom();
        assertThat(from).hasSize(1);
        assertThat(from.get(0).getPodSelector().getMatchLabels())
                .containsEntry("app.kubernetes.io/name", "tb-ext-router");
        assertThat(from.get(0).getNamespaceSelector()).isNull(); // same-namespace scope, not cross-namespace
    }

    @Test
    void networkPolicyCombinesPodAndNamespaceSelectorsInOnePeer() {
        // Both set -> a single peer carrying both = AND (that pod in that namespace).
        ExtensionManifestRenderer np = new ExtensionManifestRenderer(
                NS, "http://tb-node:8080", "/api/health", "/api/extension/route", true,
                "kubernetes.io/metadata.name=ingress-nginx",
                "app.kubernetes.io/name=tb-ext-router", "baseline", "169.254.169.254/32", "");
        NetworkPolicy policy = np.renderNetworkPolicy(extension("1.4.0", null, ExtensionState.PROVISIONING));
        var from = policy.getSpec().getIngress().get(0).getFrom();
        assertThat(from).hasSize(1);
        assertThat(from.get(0).getPodSelector().getMatchLabels())
                .containsEntry("app.kubernetes.io/name", "tb-ext-router");
        assertThat(from.get(0).getNamespaceSelector().getMatchLabels())
                .containsEntry("kubernetes.io/metadata.name", "ingress-nginx");
    }

}
