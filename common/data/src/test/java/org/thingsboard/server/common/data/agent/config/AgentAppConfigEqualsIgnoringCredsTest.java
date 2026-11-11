// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.agent.config;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.agent.AgentApplicationType;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentAppConfigEqualsIgnoringCredsTest {

    // ==================== GATEWAY — modern env schema ====================

    @Test
    void gateway_modern_onlyCredValuesDiffer_equal() {
        AgentAppConfig a = gatewayConfig(Map.of("TB_GW_HOST", "h", "TB_GW_SECURITY_TYPE", "accessToken", "TB_GW_ACCESS_TOKEN", "token-a"));
        AgentAppConfig b = gatewayConfig(Map.of("TB_GW_HOST", "h", "TB_GW_SECURITY_TYPE", "accessToken", "TB_GW_ACCESS_TOKEN", "token-b"));

        assertTrue(AgentAppConfig.equalsIgnoringCreds(AgentApplicationType.GATEWAY, a, b));
    }

    @Test
    void gateway_modern_hostDiffers_notEqual() {
        AgentAppConfig a = gatewayConfig(Map.of("TB_GW_HOST", "h1", "TB_GW_ACCESS_TOKEN", "token"));
        AgentAppConfig b = gatewayConfig(Map.of("TB_GW_HOST", "h2", "TB_GW_ACCESS_TOKEN", "token"));

        assertFalse(AgentAppConfig.equalsIgnoringCreds(AgentApplicationType.GATEWAY, a, b));
    }

    // ==================== GATEWAY — legacy env schema (pre-3.6 images) ====================

    @Test
    void gateway_legacy_onlyAccessTokenDiffers_equal() {
        AgentAppConfig a = gatewayConfig(Map.of("host", "h", "port", "1883", "accessToken", "token-a"));
        AgentAppConfig b = gatewayConfig(Map.of("host", "h", "port", "1883", "accessToken", "token-b"));

        assertTrue(AgentAppConfig.equalsIgnoringCreds(AgentApplicationType.GATEWAY, a, b));
    }

    @Test
    void gateway_legacy_onlyBasicCredsDiffer_equal() {
        AgentAppConfig a = gatewayConfig(Map.of("host", "h", "clientId", "c1", "username", "u1", "password", "p1"));
        AgentAppConfig b = gatewayConfig(Map.of("host", "h", "clientId", "c2", "username", "u2", "password", "p2"));

        assertTrue(AgentAppConfig.equalsIgnoringCreds(AgentApplicationType.GATEWAY, a, b));
    }

    @Test
    void gateway_legacy_hostDiffers_notEqual() {
        AgentAppConfig a = gatewayConfig(Map.of("host", "h1", "accessToken", "token"));
        AgentAppConfig b = gatewayConfig(Map.of("host", "h2", "accessToken", "token"));

        assertFalse(AgentAppConfig.equalsIgnoringCreds(AgentApplicationType.GATEWAY, a, b));
    }

    @Test
    void gateway_credRotationAcrossSchemas_equal() {
        // switching which schema carries the credential (e.g. profile edit) still counts as a cred-only change
        AgentAppConfig a = gatewayConfig(Map.of("host", "h", "accessToken", "token-a"));
        AgentAppConfig b = gatewayConfig(Map.of("host", "h", "TB_GW_SECURITY_TYPE", "accessToken", "TB_GW_ACCESS_TOKEN", "token-b"));

        assertTrue(AgentAppConfig.equalsIgnoringCreds(AgentApplicationType.GATEWAY, a, b));
    }

    // ==================== EDGE — credential keys come from the app type ====================

    @Test
    void edge_onlyRoutingCredsDiffer_equal() {
        AgentAppConfig a = edgeConfig(Map.of("CLOUD_RPC_HOST", "tb.cloud",
                "CLOUD_ROUTING_KEY", "rk-a", "CLOUD_ROUTING_SECRET", "secret-a"));
        AgentAppConfig b = edgeConfig(Map.of("CLOUD_RPC_HOST", "tb.cloud",
                "CLOUD_ROUTING_KEY", "rk-b", "CLOUD_ROUTING_SECRET", "secret-b"));

        assertTrue(AgentAppConfig.equalsIgnoringCreds(AgentApplicationType.EDGE, a, b));
    }

    @Test
    void edge_rpcHostDiffers_notEqual() {
        AgentAppConfig a = edgeConfig(Map.of("CLOUD_RPC_HOST", "tb.cloud", "CLOUD_ROUTING_KEY", "rk"));
        AgentAppConfig b = edgeConfig(Map.of("CLOUD_RPC_HOST", "other.cloud", "CLOUD_ROUTING_KEY", "rk"));

        assertFalse(AgentAppConfig.equalsIgnoringCreds(AgentApplicationType.EDGE, a, b));
    }

    // ==================== Arguments are never ignored ====================

    @Test
    void argumentsDiffer_notEqual() {
        AgentAppConfig a = gatewayConfig(Map.of("TB_GW_HOST", "h", "TB_GW_ACCESS_TOKEN", "token"));
        AgentAppConfig b = gatewayConfig(Map.of("TB_GW_HOST", "h", "TB_GW_ACCESS_TOKEN", "token"));
        a.setArguments(List.of(argument("device_uuid", "cloud_endpoint")));
        b.setArguments(List.of(argument("device_uuid", "other_key")));

        assertFalse(AgentAppConfig.equalsIgnoringCreds(AgentApplicationType.GATEWAY, a, b));
    }

    @Test
    void argumentsAddedOnOneSide_notEqual() {
        AgentAppConfig a = gatewayConfig(Map.of("TB_GW_HOST", "h", "TB_GW_ACCESS_TOKEN", "token"));
        AgentAppConfig b = gatewayConfig(Map.of("TB_GW_HOST", "h", "TB_GW_ACCESS_TOKEN", "token"));
        b.setArguments(List.of(argument("device_uuid", "cloud_endpoint")));

        assertFalse(AgentAppConfig.equalsIgnoringCreds(AgentApplicationType.GATEWAY, a, b));
    }

    @Test
    void nullAndEmptyArgumentLists_equal() {
        AgentAppConfig a = gatewayConfig(Map.of("TB_GW_HOST", "h", "TB_GW_ACCESS_TOKEN", "token-a"));
        AgentAppConfig b = gatewayConfig(Map.of("TB_GW_HOST", "h", "TB_GW_ACCESS_TOKEN", "token-b"));
        b.setArguments(List.of());

        assertTrue(AgentAppConfig.equalsIgnoringCreds(AgentApplicationType.GATEWAY, a, b));
    }

    // ==================== null app type — no key is ignored ====================

    @Test
    void nullAppType_credValuesDiffer_notEqual() {
        AgentAppConfig a = gatewayConfig(Map.of("TB_GW_HOST", "h", "TB_GW_ACCESS_TOKEN", "token-a"));
        AgentAppConfig b = gatewayConfig(Map.of("TB_GW_HOST", "h", "TB_GW_ACCESS_TOKEN", "token-b"));

        assertFalse(AgentAppConfig.equalsIgnoringCreds(null, a, b));
    }

    @Test
    void nullAppType_identicalComposes_equal() {
        AgentAppConfig a = gatewayConfig(Map.of("TB_GW_HOST", "h", "TB_GW_ACCESS_TOKEN", "token"));
        AgentAppConfig b = gatewayConfig(Map.of("TB_GW_HOST", "h", "TB_GW_ACCESS_TOKEN", "token"));

        assertTrue(AgentAppConfig.equalsIgnoringCreds(null, a, b));
    }

    // ==================== Helpers ====================

    private AgentAppArgument argument(String name, String key) {
        AgentAppArgument argument = new AgentAppArgument();
        argument.setName(name);
        argument.setSourceType(AgentAppArgumentSource.RELATED_ENTITY);
        argument.setValueType(AgentAppArgumentValueType.ATTRIBUTE);
        argument.setKey(key);
        return argument;
    }

    private DockerComposeConfig edgeConfig(Map<String, String> env) {
        return composeConfig("thingsboard/tb-edge-pe:3.8.0", "mytbedge", env);
    }

    private DockerComposeConfig gatewayConfig(Map<String, String> env) {
        return composeConfig("thingsboard/tb-gateway:3.6.3", "tb-gateway", env);
    }

    private DockerComposeConfig composeConfig(String image, String serviceName, Map<String, String> env) {
        ObjectNode envNode = JsonNodeFactory.instance.objectNode();
        env.forEach(envNode::put);

        ObjectNode service = JsonNodeFactory.instance.objectNode();
        service.put("image", image);
        service.set("environment", envNode);

        ObjectNode services = JsonNodeFactory.instance.objectNode();
        services.set(serviceName, service);

        ObjectNode compose = JsonNodeFactory.instance.objectNode();
        compose.set("services", services);

        DockerComposeConfig config = new DockerComposeConfig();
        config.setCompose(compose);
        return config;
    }
}
