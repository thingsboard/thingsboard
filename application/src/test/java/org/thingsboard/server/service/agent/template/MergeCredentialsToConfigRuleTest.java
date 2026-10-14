// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.agent.template;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.agent.AppConfigMergeCtx;
import org.thingsboard.server.common.data.agent.config.DockerComposeConfig;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.dao.agent.config.MergeCredentialsToConfigRule;
import org.thingsboard.server.dao.device.DeviceCredentialsService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.edge.EdgeService;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MergeCredentialsToConfigRuleTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TenantId TENANT_ID = TenantId.fromUUID(UUID.randomUUID());

    @Mock
    private EdgeService edgeService;
    @Mock
    private DeviceCredentialsService deviceCredentialsService;
    @Mock
    private DeviceService deviceService;

    private MergeCredentialsToConfigRule rule;

    @BeforeEach
    void setUp() {
        rule = new MergeCredentialsToConfigRule(edgeService, deviceCredentialsService, deviceService);
        lenient().when(deviceService.findDeviceById(any(TenantId.class), any(DeviceId.class))).thenReturn(new Device());
    }

    // ==================== supports() tests ====================

    @Test
    void supports_shouldReturnFalse_whenCtxIsNull() {
        AgentApplication app = createEdgeApp(null);
        assertFalse(rule.supports(app,null));
    }

    @Test
    void supports_shouldReturnFalse_whenRelatedEntityIdIsNull() {
        AgentApplication app = createEdgeApp(null);
        AppConfigMergeCtx ctx = AppConfigMergeCtx.builder().build();
        assertFalse(rule.supports(app,ctx));
    }

    @Test
    void supports_shouldReturnFalse_whenAppTypeIsGeneric() {
        AgentApplication app = createApp(AgentApplicationType.GENERIC, null);
        AppConfigMergeCtx ctx = AppConfigMergeCtx.builder().relatedEntityId(new EdgeId(UUID.randomUUID())).build();
        assertFalse(rule.supports(app,ctx));
    }

    @Test
    void supports_shouldReturnFalse_whenAppTypeIsNull() {
        AgentApplication app = createApp(null, null);
        AppConfigMergeCtx ctx = AppConfigMergeCtx.builder().relatedEntityId(new EdgeId(UUID.randomUUID())).build();
        assertFalse(rule.supports(app,ctx));
    }

    @Test
    void supports_shouldReturnTrue_whenEdgeAppWithRelatedEntityId() {
        AgentApplication app = createEdgeApp(null);
        AppConfigMergeCtx ctx = AppConfigMergeCtx.builder().relatedEntityId(new EdgeId(UUID.randomUUID())).build();
        assertTrue(rule.supports(app,ctx));
    }

    @Test
    void supports_shouldReturnTrue_whenGatewayAppWithRelatedEntityId() {
        AgentApplication app = createApp(AgentApplicationType.GATEWAY, null);
        AppConfigMergeCtx ctx = AppConfigMergeCtx.builder().relatedEntityId(new DeviceId(UUID.randomUUID())).build();
        assertTrue(rule.supports(app,ctx));
    }

    // ==================== apply() - Edge ====================

    @Test
    void apply_shouldInjectEdgeCredentials() {
        UUID edgeUuid = UUID.randomUUID();
        EdgeId edgeId = new EdgeId(edgeUuid);

        Edge edge = new Edge();
        edge.setTenantId(TENANT_ID);
        edge.setId(edgeId);
        edge.setRoutingKey("test-routing-key");
        edge.setSecret("test-secret");
        when(edgeService.findEdgeById(TENANT_ID, edgeId)).thenReturn(edge);

        JsonNode compose = createEdgeCompose("placeholder-key", "placeholder-secret", "7070");
        AgentApplication app = createEdgeApp(compose);

        AppConfigMergeCtx ctx = AppConfigMergeCtx.builder().relatedEntityId(new EdgeId(edgeUuid)).build();
        rule.apply(app, ctx);

        JsonNode env = getServiceEnvironment(app, "mytbedge");
        assertEquals("test-routing-key", env.get("CLOUD_ROUTING_KEY").asText());
        assertEquals("test-secret", env.get("CLOUD_ROUTING_SECRET").asText());
        assertEquals("7070", env.get("CLOUD_RPC_PORT").asText());
    }

    @Test
    void apply_shouldSkip_whenEdgeNotFound() {
        UUID edgeUuid = UUID.randomUUID();
        EdgeId edgeId = new EdgeId(edgeUuid);
        when(edgeService.findEdgeById(TENANT_ID, edgeId)).thenReturn(null);

        JsonNode compose = createEdgeCompose("original-key", "original-secret", "7070");
        AgentApplication app = createEdgeApp(compose);

        AppConfigMergeCtx ctx = AppConfigMergeCtx.builder().relatedEntityId(new EdgeId(edgeUuid)).build();
        rule.apply(app, ctx);

        JsonNode env = getServiceEnvironment(app, "mytbedge");
        assertEquals("original-key", env.get("CLOUD_ROUTING_KEY").asText());
    }

    // ==================== apply() - Gateway (ACCESS_TOKEN) ====================

    @Test
    void apply_shouldInjectGatewayAccessTokenCredentials() {
        UUID deviceUuid = UUID.randomUUID();
        DeviceId deviceId = new DeviceId(deviceUuid);

        DeviceCredentials credentials = new DeviceCredentials();
        credentials.setCredentialsType(DeviceCredentialsType.ACCESS_TOKEN);
        credentials.setCredentialsId("my-access-token");
        when(deviceCredentialsService.findDeviceCredentialsByDeviceId(TENANT_ID, deviceId)).thenReturn(credentials);

        JsonNode compose = createGatewayAccessTokenCompose("placeholder-token", "placeholder-type");
        AgentApplication app = createGatewayApp(compose);

        AppConfigMergeCtx ctx = AppConfigMergeCtx.builder().relatedEntityId(new DeviceId(deviceUuid)).build();
        rule.apply(app, ctx);

        JsonNode env = getServiceEnvironment(app, "mygateway");
        assertEquals("accessToken", env.get("TB_GW_SECURITY_TYPE").asText());
        assertEquals("my-access-token", env.get("TB_GW_ACCESS_TOKEN").asText());
    }

    // ==================== apply() - Gateway (MQTT_BASIC) ====================

    @Test
    void apply_shouldInjectGatewayMqttBasicCredentials() {
        UUID deviceUuid = UUID.randomUUID();
        DeviceId deviceId = new DeviceId(deviceUuid);

        DeviceCredentials credentials = new DeviceCredentials();
        credentials.setCredentialsType(DeviceCredentialsType.MQTT_BASIC);
        credentials.setCredentialsValue("{\"clientId\":\"myClient\",\"userName\":\"myUser\",\"password\":\"myPass\"}");
        when(deviceCredentialsService.findDeviceCredentialsByDeviceId(TENANT_ID, deviceId)).thenReturn(credentials);

        JsonNode compose = createGatewayMqttBasicCompose();
        AgentApplication app = createGatewayApp(compose);

        AppConfigMergeCtx ctx = AppConfigMergeCtx.builder().relatedEntityId(new DeviceId(deviceUuid)).build();
        rule.apply(app, ctx);

        JsonNode env = getServiceEnvironment(app, "mygateway");
        assertEquals("usernamePassword", env.get("TB_GW_SECURITY_TYPE").asText());
        assertEquals("myClient", env.get("TB_GW_CLIENT_ID").asText());
        assertEquals("myUser", env.get("TB_GW_USERNAME").asText());
        assertEquals("myPass", env.get("TB_GW_PASSWORD").asText());
    }

    @Test
    void apply_shouldInjectGatewayMqttBasicCredentials_partialFields() {
        UUID deviceUuid = UUID.randomUUID();
        DeviceId deviceId = new DeviceId(deviceUuid);

        DeviceCredentials credentials = new DeviceCredentials();
        credentials.setCredentialsType(DeviceCredentialsType.MQTT_BASIC);
        credentials.setCredentialsValue("{\"userName\":\"onlyUser\"}");
        when(deviceCredentialsService.findDeviceCredentialsByDeviceId(TENANT_ID, deviceId)).thenReturn(credentials);

        JsonNode compose = createGatewayMqttBasicCompose();
        AgentApplication app = createGatewayApp(compose);

        AppConfigMergeCtx ctx = AppConfigMergeCtx.builder().relatedEntityId(new DeviceId(deviceUuid)).build();
        rule.apply(app, ctx);

        JsonNode env = getServiceEnvironment(app, "mygateway");
        assertEquals("usernamePassword", env.get("TB_GW_SECURITY_TYPE").asText());
        assertNull(env.get("TB_GW_CLIENT_ID")); // removed since field was empty
        assertEquals("onlyUser", env.get("TB_GW_USERNAME").asText());
        assertNull(env.get("TB_GW_PASSWORD")); // removed
    }

    // ==================== apply() - Gateway (legacy env schema, pre-3.6 images) ====================

    @Test
    void apply_shouldInjectLegacyAccessTokenCredentials_whenComposeUsesLegacyEnvNames() {
        UUID deviceUuid = UUID.randomUUID();
        DeviceId deviceId = new DeviceId(deviceUuid);

        DeviceCredentials credentials = new DeviceCredentials();
        credentials.setCredentialsType(DeviceCredentialsType.ACCESS_TOKEN);
        credentials.setCredentialsId("my-access-token");
        when(deviceCredentialsService.findDeviceCredentialsByDeviceId(TENANT_ID, deviceId)).thenReturn(credentials);

        JsonNode compose = createGatewayLegacyCompose();
        AgentApplication app = createGatewayApp(compose);

        AppConfigMergeCtx ctx = AppConfigMergeCtx.builder().relatedEntityId(new DeviceId(deviceUuid)).build();
        rule.apply(app, ctx);

        JsonNode env = getServiceEnvironment(app, "mygateway");
        assertEquals("my-access-token", env.get("accessToken").asText());
        assertNull(env.get("TB_GW_ACCESS_TOKEN"));
        assertNull(env.get("TB_GW_SECURITY_TYPE")); // legacy schema has no security-type variable
    }

    @Test
    void apply_shouldInjectLegacyMqttBasicCredentials_whenComposeUsesLegacyEnvNames() {
        UUID deviceUuid = UUID.randomUUID();
        DeviceId deviceId = new DeviceId(deviceUuid);

        DeviceCredentials credentials = new DeviceCredentials();
        credentials.setCredentialsType(DeviceCredentialsType.MQTT_BASIC);
        credentials.setCredentialsValue("{\"clientId\":\"myClient\",\"userName\":\"myUser\",\"password\":\"myPass\"}");
        when(deviceCredentialsService.findDeviceCredentialsByDeviceId(TENANT_ID, deviceId)).thenReturn(credentials);

        JsonNode compose = createGatewayLegacyCompose();
        AgentApplication app = createGatewayApp(compose);

        AppConfigMergeCtx ctx = AppConfigMergeCtx.builder().relatedEntityId(new DeviceId(deviceUuid)).build();
        rule.apply(app, ctx);

        JsonNode env = getServiceEnvironment(app, "mygateway");
        assertEquals("myClient", env.get("clientId").asText());
        assertEquals("myUser", env.get("username").asText());
        assertEquals("myPass", env.get("password").asText());
        assertNull(env.get("accessToken")); // stale legacy token removed on credential type switch
        assertNull(env.get("TB_GW_SECURITY_TYPE"));
    }

    @Test
    void apply_shouldUseModernEnvNames_whenComposeContainsBothSchemas() {
        UUID deviceUuid = UUID.randomUUID();
        DeviceId deviceId = new DeviceId(deviceUuid);

        DeviceCredentials credentials = new DeviceCredentials();
        credentials.setCredentialsType(DeviceCredentialsType.ACCESS_TOKEN);
        credentials.setCredentialsId("my-access-token");
        when(deviceCredentialsService.findDeviceCredentialsByDeviceId(TENANT_ID, deviceId)).thenReturn(credentials);

        JsonNode compose = createGatewayAccessTokenCompose("placeholder-token", "placeholder-type");
        ObjectNode env0 = (ObjectNode) compose.get("services").get("mygateway").get("environment");
        env0.put("host", "legacy-host");
        env0.put("accessToken", "legacy-token");
        AgentApplication app = createGatewayApp(compose);

        AppConfigMergeCtx ctx = AppConfigMergeCtx.builder().relatedEntityId(new DeviceId(deviceUuid)).build();
        rule.apply(app, ctx);

        JsonNode env = getServiceEnvironment(app, "mygateway");
        assertEquals("accessToken", env.get("TB_GW_SECURITY_TYPE").asText());
        assertEquals("my-access-token", env.get("TB_GW_ACCESS_TOKEN").asText());
        // GatewayEnvSchema.detect() returns MODERN and only the detected schema's credential keys are
        // rewritten, so a legacy key of the other schema is left as-is.
        assertEquals("legacy-token", env.get("accessToken").asText());
    }

    @Test
    void apply_shouldSkip_whenEdgeBelongsToAnotherTenant() {
        UUID edgeUuid = UUID.randomUUID();
        EdgeId edgeId = new EdgeId(edgeUuid);

        Edge foreignEdge = new Edge();
        foreignEdge.setTenantId(TenantId.fromUUID(UUID.randomUUID()));
        foreignEdge.setId(edgeId);
        foreignEdge.setRoutingKey("foreign-key");
        foreignEdge.setSecret("foreign-secret");
        when(edgeService.findEdgeById(TENANT_ID, edgeId)).thenReturn(foreignEdge);

        JsonNode compose = createEdgeCompose("original-key", "original-secret", "7070");
        AgentApplication app = createEdgeApp(compose);

        rule.apply(app, AppConfigMergeCtx.builder().relatedEntityId(new EdgeId(edgeUuid)).build());

        JsonNode env = getServiceEnvironment(app, "mytbedge");
        assertEquals("original-key", env.get("CLOUD_ROUTING_KEY").asText());
        assertEquals("original-secret", env.get("CLOUD_ROUTING_SECRET").asText());
    }

    @Test
    void apply_shouldSkip_whenDeviceIsNotInTenant() {
        UUID deviceUuid = UUID.randomUUID();
        DeviceId deviceId = new DeviceId(deviceUuid);
        when(deviceService.findDeviceById(TENANT_ID, deviceId)).thenReturn(null);

        JsonNode compose = createGatewayAccessTokenCompose("original-token", "accessToken");
        AgentApplication app = createGatewayApp(compose);

        rule.apply(app, AppConfigMergeCtx.builder().relatedEntityId(new DeviceId(deviceUuid)).build());

        JsonNode env = getServiceEnvironment(app, "mygateway");
        assertEquals("original-token", env.get("TB_GW_ACCESS_TOKEN").asText());
    }

    @Test
    void apply_shouldSkip_whenDeviceCredentialsNotFound() {
        UUID deviceUuid = UUID.randomUUID();
        DeviceId deviceId = new DeviceId(deviceUuid);
        when(deviceCredentialsService.findDeviceCredentialsByDeviceId(TENANT_ID, deviceId)).thenReturn(null);

        JsonNode compose = createGatewayAccessTokenCompose("original-token", "accessToken");
        AgentApplication app = createGatewayApp(compose);

        AppConfigMergeCtx ctx = AppConfigMergeCtx.builder().relatedEntityId(new DeviceId(deviceUuid)).build();
        rule.apply(app, ctx);

        JsonNode env = getServiceEnvironment(app, "mygateway");
        assertEquals("original-token", env.get("TB_GW_ACCESS_TOKEN").asText());
    }

    // ==================== apply() - null/missing compose ====================

    @Test
    void apply_shouldSkip_whenComposeIsNull() {
        UUID edgeUuid = UUID.randomUUID();
        AgentApplication app = createEdgeApp(null);
        app.setConfig(new DockerComposeConfig());

        AppConfigMergeCtx ctx = AppConfigMergeCtx.builder().relatedEntityId(new EdgeId(edgeUuid)).build();
        // Should not throw
        rule.apply(app, ctx);

        verifyNoInteractions(edgeService, deviceCredentialsService);
        assertNull(((DockerComposeConfig) app.getConfig()).getCompose());
    }

    @Test
    void apply_shouldSkip_whenConfigIsNotDockerCompose() {
        UUID edgeUuid = UUID.randomUUID();
        AgentApplication app = new AgentApplication();
        app.setAppType(AgentApplicationType.EDGE);
        app.setTenantId(TENANT_ID);
        // config is null (not DockerComposeConfig)

        AppConfigMergeCtx ctx = AppConfigMergeCtx.builder().relatedEntityId(new EdgeId(edgeUuid)).build();
        rule.apply(app, ctx);

        verifyNoInteractions(edgeService, deviceCredentialsService);
        assertNull(app.getConfig());
    }

    @Test
    void apply_shouldInsertCredentials_whenEnvVarKeyNotPresent() {
        UUID edgeUuid = UUID.randomUUID();
        EdgeId edgeId = new EdgeId(edgeUuid);

        Edge edge = new Edge();
        edge.setTenantId(TENANT_ID);
        edge.setId(edgeId);
        edge.setRoutingKey("test-key");
        edge.setSecret("test-secret");
        when(edgeService.findEdgeById(TENANT_ID, edgeId)).thenReturn(edge);

        // Compose with no CLOUD_ROUTING_KEY/SECRET env vars declared
        ObjectNode service = MAPPER.createObjectNode();
        service.put("image", "thingsboard/tb-edge-pe:3.8.0");
        ObjectNode env = MAPPER.createObjectNode();
        env.put("SOME_OTHER_VAR", "value");
        service.set("environment", env);
        ObjectNode services = MAPPER.createObjectNode();
        services.set("mytbedge", service);
        ObjectNode compose = MAPPER.createObjectNode();
        compose.set("services", services);

        AgentApplication app = createEdgeApp(compose);
        AppConfigMergeCtx ctx = AppConfigMergeCtx.builder().relatedEntityId(new EdgeId(edgeUuid)).build();
        rule.apply(app, ctx);

        // upsert: existing keys preserved, missing credential keys inserted
        JsonNode resultEnv = getServiceEnvironment(app, "mytbedge");
        assertEquals("value", resultEnv.get("SOME_OTHER_VAR").asText());
        assertEquals("test-key", resultEnv.get("CLOUD_ROUTING_KEY").asText());
        assertEquals("test-secret", resultEnv.get("CLOUD_ROUTING_SECRET").asText());
    }

    // ==================== Helpers ====================

    private AgentApplication createEdgeApp(JsonNode compose) {
        return createAppWithCompose(AgentApplicationType.EDGE, compose);
    }

    private AgentApplication createGatewayApp(JsonNode compose) {
        return createAppWithCompose(AgentApplicationType.GATEWAY, compose);
    }

    private AgentApplication createApp(AgentApplicationType type, JsonNode compose) {
        return createAppWithCompose(type, compose);
    }

    private AgentApplication createAppWithCompose(AgentApplicationType type, JsonNode compose) {
        AgentApplication app = new AgentApplication();
        app.setAppType(type);
        app.setTenantId(TENANT_ID);
        if (compose != null) {
            DockerComposeConfig config = new DockerComposeConfig();
            config.setCompose(compose);
            app.setConfig(config);
        }
        return app;
    }

    private JsonNode createEdgeCompose(String routingKey, String secret, String rpcPort) {
        ObjectNode env = MAPPER.createObjectNode();
        env.put("CLOUD_ROUTING_KEY", routingKey);
        env.put("CLOUD_ROUTING_SECRET", secret);
        env.put("CLOUD_RPC_PORT", rpcPort);

        ObjectNode service = MAPPER.createObjectNode();
        service.put("image", "thingsboard/tb-edge-pe:3.8.0");
        service.set("environment", env);

        ObjectNode services = MAPPER.createObjectNode();
        services.set("mytbedge", service);

        ObjectNode compose = MAPPER.createObjectNode();
        compose.set("services", services);
        return compose;
    }

    private JsonNode createGatewayAccessTokenCompose(String accessToken, String securityType) {
        ObjectNode env = MAPPER.createObjectNode();
        env.put("TB_GW_SECURITY_TYPE", securityType);
        env.put("TB_GW_ACCESS_TOKEN", accessToken);

        ObjectNode service = MAPPER.createObjectNode();
        service.put("image", "thingsboard/tb-gateway:3.7");
        service.set("environment", env);

        ObjectNode services = MAPPER.createObjectNode();
        services.set("mygateway", service);

        ObjectNode compose = MAPPER.createObjectNode();
        compose.set("services", services);
        return compose;
    }

    private JsonNode createGatewayMqttBasicCompose() {
        ObjectNode env = MAPPER.createObjectNode();
        env.put("TB_GW_SECURITY_TYPE", "placeholder");
        env.put("TB_GW_CLIENT_ID", "placeholder");
        env.put("TB_GW_USERNAME", "placeholder");
        env.put("TB_GW_PASSWORD", "placeholder");

        ObjectNode service = MAPPER.createObjectNode();
        service.put("image", "thingsboard/tb-gateway:3.7");
        service.set("environment", env);

        ObjectNode services = MAPPER.createObjectNode();
        services.set("mygateway", service);

        ObjectNode compose = MAPPER.createObjectNode();
        compose.set("services", services);
        return compose;
    }

    private JsonNode createGatewayLegacyCompose() {
        ObjectNode env = MAPPER.createObjectNode();
        env.put("host", "host.docker.internal");
        env.put("port", "1883");
        env.put("accessToken", "placeholder-token");

        ObjectNode service = MAPPER.createObjectNode();
        service.put("image", "thingsboard/tb-gateway:3.6");
        service.set("environment", env);

        ObjectNode services = MAPPER.createObjectNode();
        services.set("mygateway", service);

        ObjectNode compose = MAPPER.createObjectNode();
        compose.set("services", services);
        return compose;
    }

    private JsonNode getServiceEnvironment(AgentApplication app, String serviceName) {
        DockerComposeConfig config = (DockerComposeConfig) app.getConfig();
        return config.getCompose().get("services").get(serviceName).get("environment");
    }
}
