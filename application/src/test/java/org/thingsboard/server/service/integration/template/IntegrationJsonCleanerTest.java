// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.integration.template;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.integration.IntegrationType;

import static org.assertj.core.api.Assertions.assertThat;

class IntegrationJsonCleanerTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final IntegrationJsonCleaner cleaner = new IntegrationJsonCleaner();

    @Test
    void clean_strips_id_tenantId_routingKey_secret_converters_createdTime_externalId_debugSettings()
            throws Exception {
        ObjectNode src = (ObjectNode) mapper.readTree("""
            {
              "id": { "entityType": "INTEGRATION", "id": "uuid" },
              "tenantId": { "entityType": "TENANT", "id": "tuid" },
              "createdTime": 12345,
              "name": "My Integration",
              "type": "LORIOT",
              "enabled": false,
              "debugMode": false,
              "defaultConverterId": { "entityType": "CONVERTER", "id": "cup" },
              "downlinkConverterId": { "entityType": "CONVERTER", "id": "cdn" },
              "routingKey": "rk-123",
              "secret": "sec",
              "version": 7,
              "externalId": { "entityType": "INTEGRATION", "id": "ext" },
              "debugSettings": { "failuresEnabled": false, "allEnabled": false, "allEnabledUntil": 0 },
              "configuration": { "server": "eu1" },
              "allowCreateDevicesOrAssets": true
            }
            """);

        JsonNode cleaned = cleaner.clean(IntegrationType.LORIOT, src);

        assertThat(cleaned.has("id")).isFalse();
        assertThat(cleaned.has("tenantId")).isFalse();
        assertThat(cleaned.has("createdTime")).isFalse();
        assertThat(cleaned.has("defaultConverterId")).isFalse();
        assertThat(cleaned.has("downlinkConverterId")).isFalse();
        assertThat(cleaned.has("routingKey")).isFalse();
        assertThat(cleaned.has("secret")).isFalse();
        assertThat(cleaned.get("version").asInt()).isEqualTo(7);
        assertThat(cleaned.has("externalId")).isFalse();
        assertThat(cleaned.has("debugSettings")).isFalse();
        assertThat(cleaned.get("name").asText()).isEqualTo("My Integration");
        assertThat(cleaned.get("type").asText()).isEqualTo("LORIOT");
        assertThat(cleaned.get("enabled").asBoolean()).isTrue();          // reset
        assertThat(cleaned.get("debugMode").asBoolean()).isTrue();         // reset
        assertThat(cleaned.get("allowCreateDevicesOrAssets").asBoolean()).isTrue();
        assertThat(cleaned.get("configuration").get("server").asText()).isEqualTo("eu1");
    }

    @Test
    void clean_rewrites_top_level_baseUrl_and_httpEndpoint_with_routingKey_placeholder()
            throws Exception {
        ObjectNode src = (ObjectNode) mapper.readTree("""
            {
              "name": "Acme LORIOT",
              "type": "LORIOT",
              "configuration": {
                "baseUrl": "http://localhost:8081",
                "httpEndpoint": "http://localhost:8081/api/v1/integrations/loriot/abb0028e-26ac-aa0e-2901-eca3d94d53d7",
                "server": "eu1"
              }
            }
            """);

        JsonNode cleaned = cleaner.clean(IntegrationType.LORIOT, src);

        JsonNode cfg = cleaned.get("configuration");
        assertThat(cfg.get("baseUrl").asText()).isEqualTo("${baseUrl}");
        assertThat(cfg.get("httpEndpoint").asText())
                .isEqualTo("${baseUrl}/api/v1/integrations/loriot/${routingKey}");
        assertThat(cfg.get("server").asText()).isEqualTo("eu1");
    }

    @Test
    void clean_rewrites_nested_clientConfiguration_baseUrl_and_httpEndpoint()
            throws Exception {
        ObjectNode src = (ObjectNode) mapper.readTree("""
            {
              "name": "Acme CHIRPSTACK",
              "type": "CHIRPSTACK",
              "configuration": {
                "metadata": {},
                "clientConfiguration": {
                  "baseUrl": "https://thingsboard.cloud",
                  "httpEndpoint": "https://thingsboard.cloud/api/v1/integrations/chirpstack/11bd221c-3134-391e-e214-45f9a58a1694",
                  "applicationServerUrl": "https://chirpstack.example.com",
                  "useAPI4Plus": true
                }
              }
            }
            """);

        JsonNode cleaned = cleaner.clean(IntegrationType.CHIRPSTACK, src);

        JsonNode client = cleaned.get("configuration").get("clientConfiguration");
        assertThat(client.get("baseUrl").asText()).isEqualTo("${baseUrl}");
        assertThat(client.get("httpEndpoint").asText())
                .isEqualTo("${baseUrl}/api/v1/integrations/chirpstack/${routingKey}");
        assertThat(client.get("applicationServerUrl").asText())
                .isEqualTo("https://chirpstack.example.com");
        assertThat(client.get("useAPI4Plus").asBoolean()).isTrue();
    }

    @Test
    void clean_no_op_when_baseUrl_and_httpEndpoint_absent() throws Exception {
        ObjectNode src = (ObjectNode) mapper.readTree("""
            {
              "name": "Acme MQTT",
              "type": "MQTT",
              "configuration": {
                "clientConfiguration": {
                  "host": "broker.example.com",
                  "port": 1883
                }
              }
            }
            """);

        JsonNode cleaned = cleaner.clean(IntegrationType.MQTT, src);
        JsonNode client = cleaned.get("configuration").get("clientConfiguration");
        assertThat(client.has("baseUrl")).isFalse();
        assertThat(client.has("httpEndpoint")).isFalse();
        assertThat(client.get("host").asText()).isEqualTo("broker.example.com");
    }
}
