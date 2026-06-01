// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.msa.integration;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.awaitility.Awaitility;
import org.testng.SkipException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.debug.DebugSettings;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.msa.WsClient;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

import static org.thingsboard.server.msa.prototypes.ConverterPrototypes.uplinkConverterPrototype;

/**
 * Live integration test that exercises ThingsBoard's TTN integration against a real
 * The Things Stack Community application. The required cloud credentials are read from
 * system properties (-DblackBoxTests.ttn*=...). If any of them is unset, the test is
 * skipped — so the class is safe to keep in CI without leaking secrets.
 */
@Slf4j
public class TTNIntegrationTest extends AbstractIntegrationTest {

    private static final String ROUTING_KEY = "routing-key-ttn-integration";
    private static final String SECRET_KEY = "secret-key-ttn-integration";

    private static final String CONFIG_TEMPLATE = "{\n" +
            "  \"clientConfiguration\": {\n" +
            "    \"host\": \"%s\",\n" +
            "    \"customHost\": true,\n" +
            "    \"port\": 8883,\n" +
            "    \"ssl\": true,\n" +
            "    \"cleanSession\": true,\n" +
            "    \"connectTimeoutSec\": 10,\n" +
            "    \"protocolVersion\": \"MQTT_3_1_1\",\n" +
            "    \"maxBytesInMessage\": 32368,\n" +
            "    \"credentials\": {\n" +
            "      \"type\": \"basic\",\n" +
            "      \"username\": \"%s\",\n" +
            "      \"password\": \"%s\"\n" +
            "    }\n" +
            "  },\n" +
            "  \"topicFilters\": [ { \"filter\": \"v3/+/devices/+/up\", \"qos\": 0 } ],\n" +
            "  \"downlinkTopicPattern\": \"v3/%s/devices/${devId}/down/push\",\n" +
            "  \"metadata\": {}\n" +
            "}";

    private static final String DEVICE_NAME_PLACEHOLDER = "DEVICE_NAME";

    // TTN delivers an envelope JSON; we ignore the inbound payload entirely and pin every
    // uplink to the pre-created test device so the test does not depend on the TTN-side
    // device-id matching anything on the TB side.
    private static final String UPLINK_CONVERTER_CODE =
            "function decodeToString(payload) { return String.fromCharCode.apply(String, payload); }\n" +
            "function decodeToJson(payload) { return JSON.parse(decodeToString(payload)); }\n" +
            "var data = decodeToJson(payload);\n" +
            "var result = {\n" +
            "   deviceName: '" + DEVICE_NAME_PLACEHOLDER + "',\n" +
            "   deviceType: 'DEFAULT',\n" +
            "   telemetry: { temperature: '42' }\n" +
            "};\n" +
            "return result;";

    private JsonNode uplinkConverterCode;
    private WsClient wsClient;

    @BeforeMethod
    public void setUp() {
        uplinkConverterCode = JacksonUtil.newObjectNode()
                .put("decoder", UPLINK_CONVERTER_CODE.replace(DEVICE_NAME_PLACEHOLDER, device.getName()));
    }

    @AfterMethod
    public void tearDown() throws Exception {
        if (wsClient != null) {
            wsClient.closeBlocking();
        }
    }

    @Test
    public void shouldReceiveSimulatedUplinkFromTheThingsStack() throws Exception {
        String region = requireProperty("blackBoxTests.ttnRegion");
        String username = requireProperty("blackBoxTests.ttnUsername");
        String password = requireProperty("blackBoxTests.ttnPassword");
        String appId = requireProperty("blackBoxTests.ttnAppId");
        String deviceId = requireProperty("blackBoxTests.ttnDeviceId");

        createTtnIntegration(region, username, password);

        wsClient = subscribeToWebSocket(device.getId(), "LATEST_TELEMETRY", CmdsType.TS_SUB_CMDS);

        sendSimulatedUplink(region, password, appId, deviceId);

        Awaitility
                .await()
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .until(() -> wsClient.getLastMessage().getDataValuesByKey(TELEMETRY_KEY).get(1).equals(TELEMETRY_VALUE));
    }

    private Integration createTtnIntegration(String region, String username, String password) {
        String host = region + ".cloud.thethings.network";
        JsonNode config = JacksonUtil.toJsonNode(String.format(CONFIG_TEMPLATE, host, username, password, username));

        Integration integration = new Integration();
        integration.setConfiguration(config);

        uplinkConverter = testRestClient.postConverter(uplinkConverterPrototype(uplinkConverterCode));
        integration.setDefaultConverterId(uplinkConverter.getId());

        integration.setName("ttn_" + RandomStringUtils.randomAlphanumeric(7));
        integration.setType(IntegrationType.TTN);
        integration.setEnabled(true);
        integration.setRemote(false);
        integration.setDebugSettings(DebugSettings.all());
        integration.setAllowCreateDevicesOrAssets(true);
        integration.setRoutingKey(ROUTING_KEY);
        integration.setSecret(SECRET_KEY);

        integration = testRestClient.postIntegration(integration);
        waitUntilIntegrationStarted(integration.getId(), integration.getTenantId());
        return this.integration = integration;
    }

    private void sendSimulatedUplink(String region, String apiKey, String appId, String deviceId) throws Exception {
        String url = String.format("https://%s.cloud.thethings.network/api/v3/as/applications/%s/devices/%s/up/simulate",
                region, appId, deviceId);
        String frmPayload = Base64.getEncoder().encodeToString("{\"temperature\":42}".getBytes(StandardCharsets.UTF_8));
        String body = "{\"uplink_message\":{"
                + "\"f_port\":1,"
                + "\"frm_payload\":\"" + frmPayload + "\","
                + "\"settings\":{"
                + "\"data_rate\":{\"lora\":{\"bandwidth\":125000,\"spreading_factor\":7}},"
                + "\"frequency\":\"868000000\""
                + "}}}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() / 100 != 2) {
            throw new RuntimeException("TTN simulate-uplink call failed: " + response.statusCode() + " - " + response.body());
        }
    }

    private static String requireProperty(String key) {
        String value = System.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new SkipException("Required system property '" + key + "' is not set — skipping TTN live test.");
        }
        return value;
    }

    @Override
    protected String getDevicePrototypeSufix() {
        return "ttn_";
    }

}
