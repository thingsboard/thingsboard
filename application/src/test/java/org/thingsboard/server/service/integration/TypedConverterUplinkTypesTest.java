// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.After;
import org.junit.Test;
import org.springframework.test.context.TestPropertySource;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.converter.ConverterType;
import org.thingsboard.server.common.data.debug.DebugSettings;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * An uplink through a typed (version 2) LoRaWAN converter and the default root rule chain:
 * identifiers the network server sends as strings are saved as strings, decoder output is still cast.
 */
@TestPropertySource(properties = {
        "js.evaluator=local",
        "service.integrations.supported=ALL"
})
@DaoSqlTest
public class TypedConverterUplinkTypesTest extends AbstractIntegrationTest {

    private static final String EUI = "1000000000000001";
    private static final String DEV_ADDR = "01020304";

    private static final String DECODER = """
            var data = JSON.parse(String.fromCharCode.apply(String, payload));
            return {attributes: {}, telemetry: {temperature: data.temperature}};
            """;

    @After
    public void afterTest() throws Exception {
        if (integration != null) {
            disableIntegration();
            removeIntegration(integration);
        }
    }

    @Override
    protected JsonNode createIntegrationClientConfiguration() {
        return JacksonUtil.newObjectNode();
    }

    @Test
    public void givenChirpStackUplink_whenSaved_thenStringIdentifiersStayStrings() throws Exception {
        loginTenantAdmin();
        createTypedConverter();
        createIntegration("Typed ChirpStack integration", IntegrationType.CHIRPSTACK);
        enableIntegration();
        waitUntilIntegrationStarted(tenantId, integration.getId());

        doPost("/api/v1/integrations/chirpstack/{routingKey}", chirpStackUplink(), integration.getRoutingKey()).andExpect(status().isOk());

        String deviceName = "Device " + EUI;
        await().atMost(TIMEOUT, TimeUnit.SECONDS).until(() ->
                doGet("/api/tenant/devices?deviceName={name}", deviceName).andReturn().getResponse().getStatus() == 200);
        Device device = doGet("/api/tenant/devices?deviceName={name}", Device.class, deviceName);

        String attributesUrl = "/api/plugins/telemetry/DEVICE/" + device.getUuidId() + "/values/attributes/CLIENT_SCOPE?keys=eui,devAddr";
        Map<String, JsonNode> attributes = new HashMap<>();
        await().atMost(TIMEOUT, TimeUnit.SECONDS).until(() -> {
            attributes.clear();
            doGetAsync(attributesUrl, JsonNode.class).forEach(kv -> attributes.put(kv.get("key").asText(), kv.get("value")));
            return attributes.size() == 2;
        });
        assertThat(attributes.get("eui").isTextual()).as("eui is a string: %s", attributes.get("eui")).isTrue();
        assertThat(attributes.get("eui").asText()).isEqualTo(EUI);
        assertThat(attributes.get("devAddr").isTextual()).as("devAddr is a string: %s", attributes.get("devAddr")).isTrue();
        assertThat(attributes.get("devAddr").asText()).isEqualTo(DEV_ADDR);

        String telemetryUrl = "/api/plugins/telemetry/DEVICE/" + device.getUuidId() + "/values/timeseries?keys=fCnt,temperature&useStrictDataTypes=true";
        Map<String, JsonNode> telemetry = new HashMap<>();
        await().atMost(TIMEOUT, TimeUnit.SECONDS).until(() -> {
            telemetry.clear();
            JsonNode latest = doGetAsync(telemetryUrl, JsonNode.class);
            for (String key : List.of("fCnt", "temperature")) {
                if (latest.has(key) && !latest.get(key).isEmpty()) {
                    telemetry.put(key, latest.get(key).get(0).get("value"));
                }
            }
            return telemetry.size() == 2;
        });
        assertThat(telemetry.get("fCnt").isIntegralNumber()).as("fCnt is a number: %s", telemetry.get("fCnt")).isTrue();
        assertThat(telemetry.get("fCnt").asLong()).isEqualTo(10L);
        // The decoder returns "22.5" as a string; the usual type cast still applies to it.
        assertThat(telemetry.get("temperature").isNumber()).as("temperature is a number: %s", telemetry.get("temperature")).isTrue();
        assertThat(telemetry.get("temperature").asDouble()).isEqualTo(22.5);
    }

    private void createTypedConverter() {
        ObjectNode configuration = JacksonUtil.newObjectNode();
        configuration.put("type", "DEVICE");
        configuration.put("name", "Device $eui");
        configuration.put("profile", "default");
        configuration.set("attributes", JacksonUtil.toJsonNode("[\"eui\", \"devAddr\"]"));
        configuration.set("telemetry", JacksonUtil.toJsonNode("[\"fCnt\"]"));
        configuration.put("scriptLang", "JS");
        configuration.put("decoder", DECODER);

        Converter converter = new Converter();
        converter.setTenantId(tenantId);
        converter.setName("Typed ChirpStack converter");
        converter.setType(ConverterType.UPLINK);
        converter.setIntegrationType(IntegrationType.CHIRPSTACK);
        converter.setConverterVersion(2);
        converter.setConfiguration(configuration);
        converter.setDebugSettings(DebugSettings.all());
        uplinkConverter = doPost("/api/converter", converter, Converter.class);
    }

    private static JsonNode chirpStackUplink() {
        return JacksonUtil.toJsonNode("""
                {
                  "deduplicationId": "3ac7e3c4-4401-4b8d-9386-a5c902f9202d",
                  "time": "2026-11-17T09:34:15.775023242+00:00",
                  "deviceInfo": {
                    "tenantId": "52f14cd4-c6f1-4fbd-8f87-4025e1d49242",
                    "tenantName": "ChirpStack",
                    "applicationId": "17c82e96-be03-4f38-aef3-f83d48582d97",
                    "applicationName": "Test application",
                    "deviceProfileId": "14855bf7-d10d-4aee-b618-ebfcb64dc7ad",
                    "deviceProfileName": "default",
                    "deviceName": "Chirpstack",
                    "devEui": "%s"
                  },
                  "devAddr": "%s",
                  "dr": 1,
                  "fCnt": 10,
                  "fPort": 80,
                  "object": {"temperature": "22.5"},
                  "rxInfo": [{"uplinkId": 1, "rssi": -22, "snr": 10.5}],
                  "txInfo": {"frequency": 867100000, "modulation": {"lora": {"bandwidth": 125000, "spreadingFactor": 11, "codeRate": "CR_4_5"}}}
                }
                """.formatted(EUI, DEV_ADDR));
    }

}
