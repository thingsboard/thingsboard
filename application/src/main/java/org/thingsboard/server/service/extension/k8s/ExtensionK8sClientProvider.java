// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.extension.k8s;

import io.fabric8.kubernetes.api.model.LimitRange;
import io.fabric8.kubernetes.api.model.LimitRangeBuilder;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ResourceQuota;
import io.fabric8.kubernetes.api.model.ResourceQuotaBuilder;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.extension.ExtensionMode;
import org.thingsboard.server.common.data.extension.ResourceSpec;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@TbCoreComponent
@ConditionalOnExpression(ExtensionMode.WHEN_KUBERNETES)
@Slf4j
public class ExtensionK8sClientProvider {

    private static final String QUOTA_NAME = "tb-ext-quota";
    private static final String LIMIT_RANGE_NAME = "tb-ext-limits";

    @Getter
    private final String namespace;
    private final boolean autoCreateNamespace;
    private final String clusterMode;
    private final String kubeconfigPath;
    private final String masterUrl;
    private final String token;
    private final String caCert;
    private final String caCertPath;
    private final boolean insecureSkipTlsVerify;
    private final int connectTimeoutMs;
    private final int requestTimeoutMs;
    private final String infraMode;

    @Value("${extensions.k8s.resources.namespace-quota.enabled:true}")
    private boolean namespaceQuotaEnabled;
    @Value("${extensions.k8s.resources.namespace-quota.cpu:32}")
    private String quotaCpu;
    @Value("${extensions.k8s.resources.namespace-quota.memory:64Gi}")
    private String quotaMemory;
    @Value("${extensions.k8s.resources.namespace-quota.pods:100}")
    private String quotaPods;
    @Value("${extensions.k8s.resources.max-cpu:4}")
    private String maxCpu;
    @Value("${extensions.k8s.resources.max-memory:8Gi}")
    private String maxMemory;

    @Getter
    private KubernetesClient client;

    public ExtensionK8sClientProvider(
            @Value("${extensions.k8s.namespace.name:tb-extensions}") String namespace,
            @Value("${extensions.k8s.namespace.auto-create:false}") boolean autoCreateNamespace,
            @Value("${extensions.k8s.cluster.mode:in-cluster}") String clusterMode,
            @Value("${extensions.k8s.cluster.kubeconfig-path:}") String kubeconfigPath,
            @Value("${extensions.k8s.cluster.master-url:}") String masterUrl,
            @Value("${extensions.k8s.cluster.token:}") String token,
            @Value("${extensions.k8s.cluster.ca-cert:}") String caCert,
            @Value("${extensions.k8s.cluster.ca-cert-path:}") String caCertPath,
            @Value("${extensions.k8s.cluster.insecure-skip-tls-verify:false}") boolean insecureSkipTlsVerify,
            @Value("${extensions.k8s.cluster.connect-timeout-ms:10000}") int connectTimeoutMs,
            @Value("${extensions.k8s.cluster.request-timeout-ms:10000}") int requestTimeoutMs,
            @Value("${extensions.mode:disabled}") String infraMode) {
        this.namespace = namespace;
        this.autoCreateNamespace = autoCreateNamespace;
        this.clusterMode = clusterMode;
        this.kubeconfigPath = kubeconfigPath;
        this.masterUrl = masterUrl;
        this.token = token;
        this.caCert = caCert;
        this.caCertPath = caCertPath;
        this.insecureSkipTlsVerify = insecureSkipTlsVerify;
        this.connectTimeoutMs = connectTimeoutMs;
        this.requestTimeoutMs = requestTimeoutMs;
        this.infraMode = infraMode;
    }

    @PostConstruct
    void init() {
        this.client = buildClient();
        if (autoCreateNamespace) {
            ensureNamespace();
        }
        // The ResourceQuota/LimitRange backstop is independent of who creates the namespace: apply it even
        // when the operator pre-creates the namespace (auto-create=false, the default), otherwise the
        // documented cluster-level DoS backstop is silently skipped. Best-effort and idempotent.
        ensureResourceGuards();
        if (ExtensionMode.fromKey(infraMode) == ExtensionMode.PRIVATE_CLOUD && !"in-cluster".equalsIgnoreCase(clusterMode)) {
            log.warn("extensions.mode=private-cloud normally runs extensions in the same cluster as TB core "
                    + "(extensions.k8s.cluster.mode=in-cluster), but cluster.mode={}. Allowed as an advanced "
                    + "override — ensure tb-core can reach that cluster's API server.", clusterMode);
        }
        log.info("Extension Kubernetes client initialized (cluster.mode={}, namespace='{}', connectTimeout={}ms, "
                + "requestTimeout={}ms)", clusterMode, namespace, connectTimeoutMs, requestTimeoutMs);
    }

    private KubernetesClient buildClient() {
        try {
            Config base = switch (clusterMode == null ? "in-cluster" : clusterMode) {
                case "kubeconfig" -> Config.fromKubeconfig(Files.readString(Path.of(kubeconfigPath)));
                case "endpoint" -> {
                    // autoConfigure=false: the default builder pre-fills from the ambient env (SA token,
                    // ~/.kube/config) and a blank token does NOT clear it — use only operator-supplied values.
                    ConfigBuilder config = new ConfigBuilder()
                            .withAutoConfigure(false)
                            .withMasterUrl(masterUrl)
                            .withOauthToken(StringUtils.isNotBlank(token) ? token : null);
                    if (insecureSkipTlsVerify) {
                        config.withTrustCerts(true);
                        log.warn("extensions.k8s.cluster.insecure-skip-tls-verify=true — Kubernetes API server "
                                + "certificate verification is DISABLED (MITM-exposed). Provide ca-cert/ca-cert-path instead.");
                    } else if (StringUtils.isNotBlank(caCert)) {
                        config.withCaCertData(caCert);
                    } else if (StringUtils.isNotBlank(caCertPath)) {
                        config.withCaCertFile(caCertPath);
                    }
                    yield config.build();
                }
                // Same config the no-arg KubernetesClientBuilder would resolve; spelled out so the timeouts
                // below are applied to it too, instead of only to the two explicit modes.
                default -> Config.autoConfigure(null);
            };
            // Every call this client makes runs on a caller thread that is holding something scarce: a
            // Tomcat HTTP worker for /logs and /status, or an ext-reconcile worker for an apply. The
            // library defaults (10 s) are sane but invisible and untunable, so an operator facing a slow
            // API server has no knob and cannot tell whether a timeout exists at all. Set them explicitly.
            Config tuned = new ConfigBuilder(base)
                    .withConnectionTimeout(connectTimeoutMs)
                    .withRequestTimeout(requestTimeoutMs)
                    .build();
            return new KubernetesClientBuilder().withConfig(tuned).build();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to build Kubernetes client (mode=" + clusterMode + "): " + e.getMessage(), e);
        }
    }

    private void ensureNamespace() {
        try {
            if (client.namespaces().withName(namespace).get() == null) {
                Namespace ns = new NamespaceBuilder().withNewMetadata().withName(namespace).endMetadata().build();
                client.namespaces().resource(ns).create();
                log.info("Created extensions namespace '{}'", namespace);
            }
        } catch (Exception e) {
            log.warn("Could not ensure namespace '{}': {}", namespace, e.getMessage());
        }
    }

    // Cluster-level backstop for tenant-supplied resources / extension counts (defense-in-depth on top
    // of in-app validation). Converge, do not create-once: an operator who lowers the configured quota
    // must see it take effect on an existing namespace. Failures are logged at ERROR, not WARN — the
    // shipped reference RBAC did not grant these verbs, so a swallowed 403 left the platform running
    // with no namespace backstop while the docs said one was in place.
    private void ensureResourceGuards() {
        if (!namespaceQuotaEnabled) {
            return;
        }
        try {
            boolean quotaExists = client.resourceQuotas().inNamespace(namespace).withName(QUOTA_NAME).get() != null;
            {
                Map<String, Quantity> hard = new LinkedHashMap<>();
                hard.put("requests.cpu", new Quantity(quotaCpu));
                hard.put("requests.memory", new Quantity(quotaMemory));
                hard.put("limits.cpu", new Quantity(quotaCpu));
                hard.put("limits.memory", new Quantity(quotaMemory));
                hard.put("pods", new Quantity(quotaPods));
                ResourceQuota quota = new ResourceQuotaBuilder()
                        .withNewMetadata().withName(QUOTA_NAME).withNamespace(namespace).endMetadata()
                        .withNewSpec().withHard(hard).endSpec()
                        .build();
                var quotaResource = client.resourceQuotas().inNamespace(namespace).resource(quota);
                if (quotaExists) {
                    quotaResource.update();
                } else {
                    quotaResource.create();
                }
                log.info("Applied ResourceQuota '{}' in namespace '{}'", QUOTA_NAME, namespace);
            }
        } catch (Exception e) {
            log.error("Could not ensure ResourceQuota in namespace '{}' — extension pods run with NO namespace "
                    + "quota backstop. Grant the reconciler role get/create/update/patch on resourcequotas, "
                    + "apply the quota manually, or set extensions.k8s.resources.namespace-quota.enabled=false: {}",
                    namespace, e.getMessage());
        }
        try {
            boolean limitRangeExists = client.limitRanges().inNamespace(namespace).withName(LIMIT_RANGE_NAME).get() != null;
            {
                Map<String, Quantity> max = new LinkedHashMap<>();
                max.put("cpu", new Quantity(maxCpu));
                max.put("memory", new Quantity(maxMemory));
                Map<String, Quantity> defaultRequest = new LinkedHashMap<>();
                defaultRequest.put("cpu", new Quantity(ResourceSpec.DEFAULT_CPU_REQUEST));
                defaultRequest.put("memory", new Quantity(ResourceSpec.DEFAULT_MEMORY_REQUEST));
                LimitRange limitRange = new LimitRangeBuilder()
                        .withNewMetadata().withName(LIMIT_RANGE_NAME).withNamespace(namespace).endMetadata()
                        .withNewSpec()
                        .addNewLimit()
                        .withType("Container")
                        .withMax(max)
                        .withDefault(max)
                        .withDefaultRequest(defaultRequest)
                        .endLimit()
                        .endSpec()
                        .build();
                var limitRangeResource = client.limitRanges().inNamespace(namespace).resource(limitRange);
                if (limitRangeExists) {
                    limitRangeResource.update();
                } else {
                    limitRangeResource.create();
                }
                log.info("Applied LimitRange '{}' in namespace '{}'", LIMIT_RANGE_NAME, namespace);
            }
        } catch (Exception e) {
            log.error("Could not ensure LimitRange in namespace '{}' — extension pods run with NO namespace "
                    + "LimitRange backstop. Grant the reconciler role get/create/update/patch on limitranges, "
                    + "apply it manually, or set extensions.k8s.resources.namespace-quota.enabled=false: {}",
                    namespace, e.getMessage());
        }
    }

    @PreDestroy
    void close() {
        if (client != null) {
            client.close();
        }
    }

}
