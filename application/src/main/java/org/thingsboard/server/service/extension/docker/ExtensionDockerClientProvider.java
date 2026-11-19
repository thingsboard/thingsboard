// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.extension.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Network;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.transport.DockerHttpClient;
import com.github.dockerjava.zerodep.ZerodepDockerHttpClient;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.extension.ExtensionMode;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.time.Duration;

/**
 * Builds and owns the {@link DockerClient} used by the docker-compose deployment backend, and ensures
 * the shared user-defined bridge network that every extension container joins (so the router resolves
 * {@code tb-ext-{slug}} by Docker's embedded DNS). The TB core process reaches the daemon through the
 * configured {@code extensions.docker.host} — a mounted {@code /var/run/docker.sock} for a co-located
 * daemon, or {@code tcp://host:2376} (+ TLS) for a remote one.
 */
@Component
@TbCoreComponent
@ConditionalOnExpression(ExtensionMode.WHEN_DOCKER)
@Slf4j
public class ExtensionDockerClientProvider {

    private final String dockerHost;
    private final boolean tlsVerify;
    private final String certPath;
    private final String apiVersion;
    private final int maxConnections;
    private final int connectTimeoutSec;
    private final int responseTimeoutSec;

    @Getter
    private final String networkName;
    private final boolean autoCreateNetwork;
    private final boolean internalNetwork;

    @Getter
    private DockerClient client;

    // What the live network enforces, as opposed to what was requested: the requested flag only ever
    // reaches a network TB creates itself.
    @Getter
    private boolean effectiveInternalNetwork;

    // Explicitly marked: the test-seam constructor below makes this a multi-constructor bean, and without
    // the marker Spring falls back to looking for a no-arg one and the context fails to start.
    @Autowired
    public ExtensionDockerClientProvider(
            @Value("${extensions.docker.host:unix:///var/run/docker.sock}") String dockerHost,
            @Value("${extensions.docker.tls-verify:false}") boolean tlsVerify,
            @Value("${extensions.docker.cert-path:}") String certPath,
            @Value("${extensions.docker.api-version:}") String apiVersion,
            @Value("${extensions.docker.max-connections:100}") int maxConnections,
            @Value("${extensions.docker.connect-timeout-seconds:10}") int connectTimeoutSec,
            @Value("${extensions.docker.response-timeout-seconds:45}") int responseTimeoutSec,
            @Value("${extensions.docker.network.name:tb-extensions}") String networkName,
            @Value("${extensions.docker.network.auto-create:false}") boolean autoCreateNetwork,
            @Value("${extensions.docker.network.internal:false}") boolean internalNetwork) {
        this.dockerHost = dockerHost;
        this.tlsVerify = tlsVerify;
        this.certPath = certPath;
        this.apiVersion = apiVersion;
        this.maxConnections = maxConnections;
        this.connectTimeoutSec = connectTimeoutSec;
        this.responseTimeoutSec = responseTimeoutSec;
        this.networkName = networkName;
        this.autoCreateNetwork = autoCreateNetwork;
        this.internalNetwork = internalNetwork;
    }

    // Test seam: inject a (mock) daemon client and exercise the network handling without building a real
    // one against extensions.docker.host.
    ExtensionDockerClientProvider(DockerClient client, String networkName, boolean autoCreateNetwork, boolean internalNetwork) {
        this("unix:///var/run/docker.sock", false, "", "", 100, 10, 45, networkName, autoCreateNetwork, internalNetwork);
        this.client = client;
    }

    @PostConstruct
    void init() {
        this.client = buildClient();
        verifyConnectivity();
        ensureNetwork();
        warnAboutEgress();
        log.info("Extension Docker client initialized (host='{}', network='{}', internal={} [requested {}])",
                dockerHost, networkName, effectiveInternalNetwork, internalNetwork);
    }

    private DockerClient buildClient() {
        try {
            DefaultDockerClientConfig.Builder configBuilder = DefaultDockerClientConfig.createDefaultConfigBuilder()
                    .withDockerHost(dockerHost)
                    .withDockerTlsVerify(tlsVerify);
            if (StringUtils.isNotBlank(certPath)) {
                configBuilder.withDockerCertPath(certPath);
            }
            if (StringUtils.isNotBlank(apiVersion)) {
                configBuilder.withApiVersion(apiVersion);
            }
            DefaultDockerClientConfig config = configBuilder.build();
            DockerHttpClient httpClient = new ZerodepDockerHttpClient.Builder()
                    .dockerHost(config.getDockerHost())
                    .sslConfig(config.getSSLConfig())
                    .maxConnections(maxConnections)
                    .connectionTimeout(Duration.ofSeconds(connectTimeoutSec))
                    .responseTimeout(Duration.ofSeconds(responseTimeoutSec))
                    .build();
            return DockerClientImpl.getInstance(config, httpClient);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to build Docker client (host=" + dockerHost + "): " + e.getMessage(), e);
        }
    }

    // Fail fast with a clear message if the daemon is unreachable at startup — otherwise the first
    // tenant deploy would be the thing that surfaces a misconfigured socket/host.
    private void verifyConnectivity() {
        try {
            client.pingCmd().exec();
        } catch (Exception e) {
            throw new IllegalStateException("Docker daemon not reachable at '" + dockerHost
                    + "'. Check extensions.docker.host and that the socket/endpoint is mounted and permitted: "
                    + e.getMessage(), e);
        }
    }

    // Best-effort: a failure here must not abort startup — the reconciler logs clearly if the network
    // is still missing on the first deploy. The one exception is a network that exists but does not
    // enforce the isolation the operator asked for; see below.
    void ensureNetwork() {
        try {
            Network existing = client.listNetworksCmd().withNameFilter(networkName).exec().stream()
                    .filter(n -> networkName.equals(n.getName()))
                    .findFirst().orElse(null);
            if (existing != null) {
                // 'internal' is fixed at creation: Docker cannot flip it on a live network, and this method
                // used to return here without ever looking, so internal=true against an operator-created
                // (or previously non-internal) network was silently a no-op — while warnAboutEgress went
                // quiet because it trusted the requested flag. Egress to the host and 169.254.169.254 then
                // stayed open with nothing said. Fail loudly instead: the operator asked for isolation and
                // does not have it, and the fix (recreate the network, or drop the flag) needs a decision.
                effectiveInternalNetwork = Boolean.TRUE.equals(existing.getInternal());
                if (internalNetwork && !effectiveInternalNetwork) {
                    throw new IllegalStateException("Docker network '" + networkName + "' already exists and is NOT "
                            + "internal, but extensions.docker.network.internal=true. Docker cannot change that flag "
                            + "on an existing network: recreate it (docker network rm " + networkName
                            + " && docker network create --internal " + networkName + ", then reattach your router), "
                            + "or set extensions.docker.network.internal=false and block egress on the bridge yourself.");
                }
                return;
            }
            if (autoCreateNetwork) {
                client.createNetworkCmd().withName(networkName).withDriver("bridge")
                        // An internal network has no route off the bridge, so tenant containers cannot reach the
                        // host, the cloud metadata endpoint or the internet — the closest Docker equivalent of the
                        // K8s 'restricted' egress policy. Off by default because it also cuts outbound calls the
                        // extension itself may need; only applies to a network TB creates.
                        .withInternal(internalNetwork)
                        .withCheckDuplicate(true).exec();
                effectiveInternalNetwork = internalNetwork;
                log.info("Created extensions Docker network '{}' (internal={})", networkName, internalNetwork);
            } else {
                // Whatever the operator creates by hand is unknown to us and, by default, not internal.
                effectiveInternalNetwork = false;
                log.warn("Docker network '{}' does not exist and auto-create is off. Create it (docker network "
                        + "create {}{}) and attach your router to it, or set extensions.docker.network.auto-create=true.",
                        networkName, internalNetwork ? "--internal " : "", networkName);
            }
        } catch (IllegalStateException e) {
            throw e; // a confirmed isolation mismatch: never downgrade it to a log line
        } catch (Exception e) {
            // Could not read the network's state, so assume the weaker one and let warnAboutEgress speak.
            effectiveInternalNetwork = false;
            log.warn("Could not ensure Docker network '{}': {}", networkName, e.getMessage());
        }
    }

    // The Docker backend cannot express per-container egress rules the way a NetworkPolicy can, so on a
    // plain bridge a tenant container routes anywhere the host can: the cloud metadata endpoint
    // (169.254.169.254 — instance-profile credentials), the host itself, tb-core's internal ports and every
    // other tenant's container. The K8s backend blocks the link-local ranges by default; here the operator
    // has to. Say so out loud once at startup instead of leaving it to the docs.
    void warnAboutEgress() {
        // Keyed on what the live network actually enforces, not on what was requested — the requested flag
        // applies to a TB-created network only, and a warning that trusts it stays silent in exactly the
        // case the operator most needs to hear about.
        if (effectiveInternalNetwork) {
            return;
        }
        log.warn("Extension containers on Docker network '{}' have unrestricted egress: they can reach the cloud "
                + "metadata endpoint (169.254.169.254), the Docker host and every other container on that network. "
                + "On a multi-tenant or cloud-hosted install either set extensions.docker.network.internal=true "
                + "(TB-created networks only), block 169.254.0.0/16 and fd00:ec2::254/128 on the bridge with "
                + "iptables/nftables, or use the Kubernetes backend.", networkName);
    }

    @PreDestroy
    void close() {
        if (client != null) {
            try {
                client.close();
            } catch (Exception e) {
                log.debug("Error closing Docker client: {}", e.getMessage());
            }
        }
    }

}
