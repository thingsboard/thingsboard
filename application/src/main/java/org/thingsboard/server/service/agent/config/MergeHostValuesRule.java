// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.agent.config;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.agent.AppConfigMergeCtx;
import org.thingsboard.server.common.data.agent.HasAgentAppConfig;
import org.thingsboard.server.common.data.agent.config.AgentAppConfig;
import org.thingsboard.server.common.data.agent.config.DockerComposeConfig;
import org.thingsboard.server.common.data.agent.config.DockerComposeUtils;
import org.thingsboard.server.common.data.agent.config.GatewayEnvSchema;
import org.thingsboard.server.dao.agent.config.AppConfigMergeRule;
import org.thingsboard.server.dao.device.DeviceConnectivityService;
import org.thingsboard.server.dao.util.DeviceConnectivityUtil;

import java.net.URI;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class MergeHostValuesRule implements AppConfigMergeRule {

    private static final String CLOUD_RPC_SSL_ENABLED = "CLOUD_RPC_SSL_ENABLED";
    private static final String CLOUD_RPC_HOST = "CLOUD_RPC_HOST";
    private static final String CLOUD_RPC_PORT = "CLOUD_RPC_PORT";
    private static final String GATEWAY_DEFAULT_PORT = "1883";

    private final DeviceConnectivityService deviceConnectivityService;

    @Value("${edges.rpc.port:7070}")
    private int edgeRpcPort;

    @Value("${edges.rpc.ssl.enabled}")
    private boolean grpcSslEnabled;

    @Override
    public boolean supports(HasAgentAppConfig data, AppConfigMergeCtx ctx) {
        if (ctx == null || !ctx.isSetHostValues()) {
            return false;
        }
        AgentApplicationType appType = AppConfigMergeRule.resolveAppType(data);
        return appType == AgentApplicationType.EDGE || appType == AgentApplicationType.GATEWAY;
    }

    @Override
    public void apply(HasAgentAppConfig data, AppConfigMergeCtx ctx) {
        AgentAppConfig config = data.getConfig();
        if (!(config instanceof DockerComposeConfig composeConfig)) {
            log.trace("Skipping host values merge: config is not DockerComposeConfig");
            return;
        }
        JsonNode compose = composeConfig.getCompose();
        if (compose == null || compose.isNull()) {
            log.trace("Skipping host values merge: compose is null");
            return;
        }
        mergeHostValues(data, ctx, compose);
    }

    private void mergeHostValues(HasAgentAppConfig data, AppConfigMergeCtx ctx, JsonNode compose) {
        String baseUrl = ctx.getBaseUrl();
        if (StringUtils.isBlank(baseUrl)) {
            log.warn("Skipping host values merge: baseUrl is blank (general.baseUrl unset and no request)");
            return;
        }

        AgentApplicationType appType = AppConfigMergeRule.resolveAppType(data);
        if (appType == AgentApplicationType.EDGE) {
            applyEdgeHost(compose, baseUrl);
        } else if (appType == AgentApplicationType.GATEWAY) {
            applyGatewayHost(compose, baseUrl);
        }
    }

    private void applyEdgeHost(JsonNode compose, String baseUrl) {
        String host = extractHost(baseUrl);
        String resolvedHost = DeviceConnectivityUtil.isLocalhost(host) ? DeviceConnectivityUtil.HOST_DOCKER_INTERNAL : host;
        Map<String, String> envVars = Map.of(
                CLOUD_RPC_SSL_ENABLED, String.valueOf(grpcSslEnabled),
                CLOUD_RPC_HOST, resolvedHost,
                CLOUD_RPC_PORT, String.valueOf(edgeRpcPort)
        );
        DockerComposeUtils.upsertEnvVariables(compose, AgentApplicationType.EDGE.getMainImagePattern(), envVars);
    }

    private void applyGatewayHost(JsonNode compose, String baseUrl) {
        String host = deviceConnectivityService.resolveGatewayHost(baseUrl);
        if (StringUtils.isBlank(host)) {
            log.warn("Skipping gateway host merge: resolveGatewayHost returned blank for baseUrl [{}]", baseUrl);
            return;
        }
        String port = deviceConnectivityService.resolveGatewayPort();
        if (StringUtils.isBlank(port)) {
            log.debug("Gateway MQTT port is not configured in connectivity settings, using default [{}]", GATEWAY_DEFAULT_PORT);
            port = GATEWAY_DEFAULT_PORT;
        }
        var imagePattern = AgentApplicationType.GATEWAY.getMainImagePattern();
        GatewayEnvSchema schema = GatewayEnvSchema.detect(compose, imagePattern);
        Map<String, String> envVars = Map.of(
                schema.getHostKey(), host,
                schema.getPortKey(), port
        );
        DockerComposeUtils.setEnvVariables(compose, imagePattern, envVars);
    }

    private static String extractHost(String baseUrl) {
        try {
            String host = URI.create(baseUrl).getHost();
            return StringUtils.isNotBlank(host) ? host : baseUrl;
        } catch (IllegalArgumentException e) {
            log.warn("Failed to parse baseUrl [{}] as URI, falling back to raw value", baseUrl, e);
            return baseUrl;
        }
    }
}
