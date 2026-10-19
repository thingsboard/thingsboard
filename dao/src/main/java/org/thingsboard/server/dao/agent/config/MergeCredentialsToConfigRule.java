// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.agent.config;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.agent.AppConfigMergeCtx;
import org.thingsboard.server.common.data.agent.HasAgentAppConfig;
import org.thingsboard.server.common.data.agent.config.AgentAppConfig;
import org.thingsboard.server.common.data.agent.config.DockerComposeConfig;
import org.thingsboard.server.common.data.agent.config.DockerComposeUtils;
import org.thingsboard.server.common.data.agent.config.GatewayEnvSchema;
import org.thingsboard.server.common.data.device.credentials.BasicMqttCredentials;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.dao.device.DeviceCredentialsService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.edge.EdgeService;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Merge rule that injects entity credentials into the compose configuration.
 * Must run after {@link MergeTemplateComposeRule} (which has {@code @Order(Ordered.HIGHEST_PRECEDENCE)})
 * because it operates on the already-populated compose config.
 * <p>
 * For EDGE applications: injects CLOUD_ROUTING_KEY and CLOUD_ROUTING_SECRET from the selected Edge entity.
 * For GATEWAY applications: injects credentials based on type — access token for token auth,
 * or client id/username/password for MQTT_BASIC auth. The env variable names are taken from the
 * {@link GatewayEnvSchema} detected from the compose, so legacy gateway lines (pre-3.6 images that
 * only read the unprefixed {@code host}/{@code accessToken}/… names) are supported alongside the
 * modern {@code TB_GW_*} style.
 * Host/port env vars (CLOUD_RPC_HOST/PORT for EDGE, TB_GW_HOST/PORT for GATEWAY) are injected separately
 * by the application-module {@code MergeHostValuesRule} when the caller opts in via {@link AppConfigMergeCtx#isSetHostValues()}.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class MergeCredentialsToConfigRule implements AppConfigMergeRule {

    private static final String CLOUD_ROUTING_KEY = "CLOUD_ROUTING_KEY";
    private static final String CLOUD_ROUTING_SECRET = "CLOUD_ROUTING_SECRET";

    private final EdgeService edgeService;
    private final DeviceCredentialsService deviceCredentialsService;
    private final DeviceService deviceService;

    @Override
    public boolean isAppliedOnSave() {
        return true;
    }

    @Override
    public boolean supports(HasAgentAppConfig data, AppConfigMergeCtx ctx) {
        return ctx != null
                && ctx.getRelatedEntityId() != null
                && data instanceof AgentApplication agentApp
                && agentApp.getAppType() != null
                && (agentApp.getAppType() == AgentApplicationType.EDGE || agentApp.getAppType() == AgentApplicationType.GATEWAY);
    }

    @Override
    public void apply(HasAgentAppConfig data, AppConfigMergeCtx ctx) {
        if (!(data instanceof AgentApplication agentApp)) {
            log.warn("MergeCredentialsToConfigRule called with non-AgentApplication data [{}], skipping", data.getClass().getSimpleName());
            return;
        }
        AgentAppConfig config = agentApp.getConfig();
        if (!(config instanceof DockerComposeConfig composeConfig)) {
            log.trace("Skipping credentials merge: config is not DockerComposeConfig");
            return;
        }

        JsonNode compose = composeConfig.getCompose();
        if (compose == null || compose.isNull()) {
            log.trace("Skipping credentials merge: compose is null");
            return;
        }

        AgentApplicationType appType = agentApp.getAppType();
        TenantId tenantId = agentApp.getTenantId();

        if (appType == AgentApplicationType.EDGE) {
            applyEdgeCredentials(compose, tenantId, ctx);
        } else if (appType == AgentApplicationType.GATEWAY) {
            applyGatewayCredentials(compose, tenantId, ctx);
        }
    }

    private void applyEdgeCredentials(JsonNode compose, TenantId tenantId, AppConfigMergeCtx ctx) {
        EntityId rId = ctx.getRelatedEntityId();
        if (rId.getEntityType() != EntityType.EDGE) {
            throw new IllegalArgumentException("Related entity id with EDGE entity type is expected, but found: " + rId.getEntityType());
        }
        EdgeId edgeId = (EdgeId) rId;
        Edge edge = edgeService.findEdgeById(tenantId, edgeId);
        if (edge == null || !tenantId.equals(edge.getTenantId())) {
            log.warn("Edge not found in tenant [{}] for id [{}], skipping credentials merge", tenantId, edgeId);
            return;
        }
        Map<String, String> envVars = new LinkedHashMap<>();
        if (StringUtils.isNotEmpty(edge.getRoutingKey())) {
            envVars.put(CLOUD_ROUTING_KEY, edge.getRoutingKey());
        }
        if (StringUtils.isNotEmpty(edge.getSecret())) {
            envVars.put(CLOUD_ROUTING_SECRET, edge.getSecret());
        }
        DockerComposeUtils.upsertEnvVariables(compose, AgentApplicationType.EDGE.getMainImagePattern(), envVars);
    }

    private void applyGatewayCredentials(JsonNode compose, TenantId tenantId, AppConfigMergeCtx ctx) {
        EntityId rId = ctx.getRelatedEntityId();
        if (rId.getEntityType() != EntityType.DEVICE) {
            throw new IllegalArgumentException("Related entity id with DEVICE type is expected, but found: " + rId.getEntityType());
        }
        DeviceId deviceId = (DeviceId) rId;
        if (deviceService.findDeviceById(tenantId, deviceId) == null) {
            log.warn("Device not found in tenant [{}] for id [{}], skipping credentials merge", tenantId, deviceId);
            return;
        }
        DeviceCredentials credentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, deviceId);
        if (credentials == null) {
            log.warn("Device credentials not found for device [{}], skipping credentials merge", deviceId);
            return;
        }
        var imagePattern = AgentApplicationType.GATEWAY.getMainImagePattern();
        GatewayEnvSchema schema = GatewayEnvSchema.detect(compose, imagePattern);
        Map<String, String> envVars = new LinkedHashMap<>();
        switch (credentials.getCredentialsType()) {
            case ACCESS_TOKEN:
                putSecurityType(envVars, schema, "accessToken");
                envVars.put(schema.getAccessTokenKey(), credentials.getCredentialsId());
                break;
            case MQTT_BASIC:
                putSecurityType(envVars, schema, "usernamePassword");
                BasicMqttCredentials mqttCredentials = JacksonUtil.fromString(
                        credentials.getCredentialsValue(), BasicMqttCredentials.class);
                if (mqttCredentials != null) {
                    if (StringUtils.isNotEmpty(mqttCredentials.getClientId())) {
                        envVars.put(schema.getClientIdKey(), mqttCredentials.getClientId());
                    }
                    if (StringUtils.isNotEmpty(mqttCredentials.getUserName())) {
                        envVars.put(schema.getUsernameKey(), mqttCredentials.getUserName());
                    }
                    if (StringUtils.isNotEmpty(mqttCredentials.getPassword())) {
                        envVars.put(schema.getPasswordKey(), mqttCredentials.getPassword());
                    }
                }
                break;
            default:
                log.warn("Unsupported credentials type [{}] for gateway auto-fill", credentials.getCredentialsType());
                return;
        }
        List<String> staleKeys = schema.credentialKeys().stream()
                .filter(k -> !envVars.containsKey(k))
                .toList();
        DockerComposeUtils.removeEnvVariables(compose, imagePattern, staleKeys);
        DockerComposeUtils.upsertEnvVariables(compose, imagePattern, envVars);
    }

    // legacy gateway images have no security-type variable; they infer the auth type from the credential keys
    private static void putSecurityType(Map<String, String> envVars, GatewayEnvSchema schema, String securityType) {
        if (schema.getSecurityTypeKey() != null) {
            envVars.put(schema.getSecurityTypeKey(), securityType);
        }
    }
}
