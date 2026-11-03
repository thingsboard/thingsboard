// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.msa.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.awaitility.Awaitility;
import org.testng.SkipException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EventInfo;
import org.thingsboard.server.common.data.debug.DebugSettings;
import org.thingsboard.server.common.data.event.EventType;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.msa.WsClient;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.thingsboard.server.common.data.DataConstants.SHARED_SCOPE;
import static org.thingsboard.server.msa.prototypes.ConverterPrototypes.downlinkConverterPrototype;
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

    private static final String INTEGRATION_DOWNLINK_NODE_TYPE = "org.thingsboard.rule.engine.integration.TbIntegrationDownlinkNode";
    private static final int DOWNLINK_F_PORT = 1;
    private static final int TTN_QUEUE_TIMEOUT_SECONDS = 60;

    // Downlink converter: turns the shared-attribute body ({devId, downlinkData}) into the JSON
    // The Things Stack expects on the v3 'down/push' MQTT topic, and exposes devId as metadata so
    // BasicMqttIntegration can resolve the ${devId} placeholder in downlinkTopicPattern.
    private static final String DOWNLINK_CONVERTER_ENCODER =
            "var result = {\n" +
            "    contentType: \"JSON\",\n" +
            "    data: JSON.stringify({\n" +
            "        downlinks: [ { f_port: " + DOWNLINK_F_PORT + ", frm_payload: msg.downlinkData, priority: \"NORMAL\" } ]\n" +
            "    }),\n" +
            "    metadata: {\n" +
            "        devId: msg.devId\n" +
            "    }\n" +
            "};\n" +
            "return result;";

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
    // Coordinates for clearing the shared The Things Stack downlink queue in tearDown — set by the
    // downlink test so a leftover queued downlink does not bleed into other runs.
    private String[] downlinkQueueCoordinates;
    private String region;
    private String username;
    private String password;
    private String appId;
    private String deviceId;

    @BeforeClass
    public void beforeTtnClass() {
        region = requireProperty("blackBoxTests.ttnRegion");
        username = requireProperty("blackBoxTests.ttnUsername");
        password = requireProperty("blackBoxTests.ttnPassword");
        appId = requireProperty("blackBoxTests.ttnAppId");
        deviceId = requireProperty("blackBoxTests.ttnDeviceId");
    }

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
        if (downlinkQueueCoordinates != null) {
            try {
                clearDownlinkQueue(downlinkQueueCoordinates[0], downlinkQueueCoordinates[1], downlinkQueueCoordinates[2], downlinkQueueCoordinates[3]);
            } catch (Exception e) {
                log.warn("Failed to clear The Things Stack downlink queue", e);
            } finally {
                downlinkQueueCoordinates = null;
            }
        }
    }

    @Test
    public void shouldReceiveSimulatedUplinkFromTheThingsStack() throws Exception {
        createTtnIntegration(region, username, password);

        wsClient = subscribeToWebSocket(device.getId(), "LATEST_TELEMETRY", CmdsType.TS_SUB_CMDS);

        sendSimulatedUplink(region, password, appId, deviceId);

        Awaitility
                .await()
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .until(() -> wsClient.getLastMessage().getDataValuesByKey(TELEMETRY_KEY).get(1).equals(TELEMETRY_VALUE));
    }

    @Test
    public void shouldEnqueueDownlinkInTheThingsStack() throws Exception {
        JsonNode downlinkConverter = JacksonUtil.newObjectNode().put("encoder", DOWNLINK_CONVERTER_ENCODER);
        createTtnIntegration(region, username, password, downlinkConverter);

        // Re-root the rule chain so ATTRIBUTES_UPDATED messages flow into TbIntegrationDownlinkNode.
        RuleChainId ruleChainId = createRootRuleChainWithIntegrationDownlinkNode(integration.getId());

        // Start from a clean queue so the assertion is deterministic, and make sure tearDown clears it.
        downlinkQueueCoordinates = new String[]{region, password, appId, deviceId};
        clearDownlinkQueue(region, password, appId, deviceId);

        String downlinkPayloadBase64 = Base64.getEncoder().encodeToString(new byte[]{0x01, 0x02, 0x03});

        // Saving a shared attribute fires ATTRIBUTES_UPDATED whose JSON body becomes the converter's
        // `msg`; the converter reads devId + downlinkData from it.
        ObjectNode sharedAttributes = JacksonUtil.newObjectNode();
        sharedAttributes.put("devId", deviceId);
        sharedAttributes.put("downlinkData", downlinkPayloadBase64);
        testRestClient.saveEntityAttributes(EntityType.DEVICE.name(), device.getId().toString(), SHARED_SCOPE, sharedAttributes);

        // Wait until the integration's downlink node actually saw the message — surfaces converter
        // and MQTT-publish errors as a debug event rather than just a queue that stays empty.
        RuleChainMetaData ruleChainMetadata = testRestClient.getRuleChainMetadata(ruleChainId);
        RuleNode integrationNode = ruleChainMetadata.getNodes().stream()
                .filter(n -> n.getType().equals(INTEGRATION_DOWNLINK_NODE_TYPE))
                .findFirst()
                .orElseThrow(() -> new AssertionError("TbIntegrationDownlinkNode not found in rule chain"));
        waitTillRuleNodeReceiveMsg(integrationNode.getId(), EventType.DEBUG_RULE_NODE, integration.getTenantId(), "ATTRIBUTES_UPDATED");

        // The integration persists the 'Downlink' debug event after it published to The Things Stack MQTT broker.
        // Waiting for it first tells a downlink lost inside ThingsBoard apart from one The Things Stack did not queue.
        EventInfo downlinkEvent = waitForIntegrationDownlinkDebugEvent(integration);

        // Verify The Things Stack actually accepted the downlink into the device's queue. The queue lives in an
        // external SaaS, so the poll is longer than for local brokers and the last response is kept for the failure message.
        AtomicReference<String> lastQueueResponse = new AtomicReference<>();
        Awaitility
                .await()
                .alias("The Things Stack downlink queue after integration debug event " + describeDebugEvents(List.of(downlinkEvent)))
                .pollInterval(1, TimeUnit.SECONDS)
                .atMost(TTN_QUEUE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    JsonNode queue = getDownlinkQueue(region, password, appId, deviceId);
                    lastQueueResponse.set(String.valueOf(queue));
                    JsonNode downlinks = queue.get("downlinks");
                    assertThat(downlinks)
                            .as("The Things Stack downlink queue response should contain 'downlinks', last response: %s", lastQueueResponse.get())
                            .isNotNull();
                    boolean matched = false;
                    for (JsonNode item : downlinks) {
                        JsonNode frmPayload = item.get("frm_payload");
                        JsonNode fPort = item.get("f_port");
                        if (frmPayload != null && downlinkPayloadBase64.equals(frmPayload.asText())
                                && fPort != null && fPort.asInt() == DOWNLINK_F_PORT) {
                            matched = true;
                            break;
                        }
                    }
                    assertThat(matched)
                            .as("The Things Stack downlink queue should contain enqueued item with frm_payload=%s f_port=%d, last response: %s",
                                    downlinkPayloadBase64, DOWNLINK_F_PORT, lastQueueResponse.get())
                            .isTrue();
                });
    }

    private Integration createTtnIntegration(String region, String username, String password) {
        return createTtnIntegration(region, username, password, null);
    }

    private Integration createTtnIntegration(String region, String username, String password, JsonNode downlinkConverterConfig) {
        String host = region + ".cloud.thethings.network";
        JsonNode config = JacksonUtil.toJsonNode(String.format(CONFIG_TEMPLATE, host, username, password, username));

        Integration integration = new Integration();
        integration.setConfiguration(config);

        uplinkConverter = testRestClient.postConverter(uplinkConverterPrototype(uplinkConverterCode));
        integration.setDefaultConverterId(uplinkConverter.getId());

        if (downlinkConverterConfig != null) {
            integration.setDownlinkConverterId(testRestClient.postConverter(downlinkConverterPrototype(downlinkConverterConfig)).getId());
        }

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
        sendTtnRequest(request);
    }

    // Reads the device's downlink queue from the Application Server REST API.
    private JsonNode getDownlinkQueue(String region, String apiKey, String appId, String deviceId) throws Exception {
        String url = String.format("https://%s.cloud.thethings.network/api/v3/as/applications/%s/devices/%s/down",
                region, appId, deviceId);
        HttpResponse<String> response = sendTtnRequest(HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + apiKey)
                .GET()
                .build());
        return JacksonUtil.toJsonNode(response.body());
    }

    // Replaces the device's downlink queue with an empty one.
    private void clearDownlinkQueue(String region, String apiKey, String appId, String deviceId) throws Exception {
        String url = String.format("https://%s.cloud.thethings.network/api/v3/as/applications/%s/devices/%s/down/replace",
                region, appId, deviceId);
        sendTtnRequest(HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"downlinks\":[]}"))
                .build());
    }

    private HttpResponse<String> sendTtnRequest(HttpRequest request) throws Exception {
        HttpResponse<String> response;
        try {
            response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new SkipException("The Things Stack is unreachable (" + request.method() + " " + request.uri() + "): " + e);
        }
        if (response.statusCode() == 429 || response.statusCode() / 100 == 5) {
            throw new SkipException("The Things Stack is unavailable: " + request.method() + " " + request.uri()
                    + " -> " + response.statusCode() + " - " + response.body());
        }
        if (response.statusCode() / 100 != 2) {
            throw new RuntimeException("TTN API call failed: " + request.method() + " " + request.uri()
                    + " -> " + response.statusCode() + " - " + response.body());
        }
        return response;
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
