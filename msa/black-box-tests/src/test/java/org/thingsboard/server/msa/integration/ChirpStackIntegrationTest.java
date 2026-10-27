// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.msa.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.testng.SkipException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.event.EventType;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.msa.ContainerTestSuite;
import org.thingsboard.server.msa.TestProperties;
import org.thingsboard.server.msa.WsClient;
import org.thingsboard.server.msa.integration.chirpstack.ChirpStackTestClient;
import org.thingsboard.server.msa.integration.chirpstack.ChirpStackTestClient.ProvisionedDevice;
import org.thingsboard.server.msa.integration.chirpstack.LoRaWanUplinkFrameBuilder;
import org.thingsboard.server.msa.mapper.WsTelemetryResponse;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.thingsboard.server.common.data.DataConstants.CLIENT_SCOPE;
import static org.thingsboard.server.common.data.DataConstants.SHARED_SCOPE;
import static org.thingsboard.server.common.data.integration.IntegrationType.CHIRPSTACK;

/**
 * End-to-end ChirpStack integration test: provisions a real ChirpStack v4 server (run by the
 * test container suite), publishes a genuine LoRaWAN uplink to ChirpStack's gateway MQTT topic,
 * and verifies that ChirpStack decrypts it and forwards it through the ThingsBoard CHIRPSTACK
 * HTTP integration into telemetry/attributes.
 */
@Slf4j
public class ChirpStackIntegrationTest extends AbstractIntegrationTest {

    private static final String ROUTING_KEY = "routing-key-chirpstack";
    private static final String SECRET_KEY = "secret-key-chirpstack";

    // Application payload [0x2a, 0x3f] -> base64 "Kj8=", decoded as temperature 42 / humidity 63.
    private static final byte[] UPLINK_PAYLOAD = {0x2a, 0x3f};
    private static final int F_PORT = 1;

    // Downlink converter: reads the shared-attribute body and produces a DownlinkData whose
    // metadata (DevEUI, fPort) routes ChirpStackIntegration to POST {applicationServerUrl}/api/
    // devices/{devEui}/queue. The 'data' field is the base64 payload ChirpStack will enqueue.
    private static final String DOWNLINK_CONVERTER_ENCODER =
            "var result = {\n" +
            "    contentType: \"TEXT\",\n" +
            "    data: msg.downlinkData,\n" +
            "    metadata: {\n" +
            "        DevEUI: msg.devEui,\n" +
            "        fPort: \"1\"\n" +
            "    }\n" +
            "};\n" +
            "return result;";

    private ChirpStackTestClient chirpStackClient;
    private ProvisionedDevice provisionedDevice;
    private WsClient wsClient;

    @BeforeClass
    public void beforeChirpStackClass() {
        if (!ContainerTestSuite.getInstance().isActive()) {
            throw new SkipException("ChirpStack tests need the docker-compose test environment");
        }
        chirpStackClient = new ChirpStackTestClient(TestProperties.getChirpStackRestUrl());
    }

    @AfterMethod
    public void tearDown() throws Exception {
        if (wsClient != null) {
            wsClient.closeBlocking();
            wsClient = null;
        }
        if (provisionedDevice != null) {
            chirpStackClient.deleteApplication(provisionedDevice.getApplicationId());
            provisionedDevice = null;
        }
    }

    @Test
    public void checkTelemetryUploadedWithLocalIntegration() throws Exception {
        provisionedDevice = chirpStackClient.provision(ROUTING_KEY);
        createTbChirpStackIntegration();

        wsClient = subscribeToWebSocket(device.getId(), "LATEST_TELEMETRY", CmdsType.TS_SUB_CMDS);

        sendUplink();

        WsTelemetryResponse actualLatestTelemetry = wsClient.getLastMessage();
        assertThat(actualLatestTelemetry.getDataValuesByKey("data").get(1)).isEqualTo("Kj8=");
        assertThat(actualLatestTelemetry.getDataValuesByKey("temperature").get(1)).isEqualTo("42");
        assertThat(actualLatestTelemetry.getDataValuesByKey("humidity").get(1)).isEqualTo("63");
        assertThat(actualLatestTelemetry.getDataValuesByKey("snr").get(1))
                .isEqualTo(String.valueOf(LoRaWanUplinkFrameBuilder.SNR));
    }

    @Test
    public void checkAttributesUploadedWithLocalIntegration() throws Exception {
        provisionedDevice = chirpStackClient.provision(ROUTING_KEY);
        createTbChirpStackIntegration();

        wsClient = subscribeToWebSocket(device.getId(), "CLIENT_SCOPE", CmdsType.ATTR_SUB_CMDS);

        sendUplink();

        WsTelemetryResponse actualLatestAttr = wsClient.getLastMessage();
        assertThat(actualLatestAttr.getData()).hasSize(3);
        assertThat(actualLatestAttr.getLatestValues().keySet()).containsOnlyOnceElementsOf(Arrays.asList("rssi", "eui", "fPort"));
        wsClient.closeBlocking();

        assertThat(actualLatestAttr.getDataValuesByKey("rssi").get(1)).isEqualTo(String.valueOf(LoRaWanUplinkFrameBuilder.RSSI));
        assertThat(actualLatestAttr.getDataValuesByKey("eui").get(1)).isEqualTo(HexFormat.of().formatHex(provisionedDevice.getDevEui()));
        assertThat(actualLatestAttr.getDataValuesByKey("fPort").get(1)).isEqualTo(String.valueOf(F_PORT));
    }

    @Test
    public void checkDownlinkEnqueuedInChirpStack() throws Exception {
        provisionedDevice = chirpStackClient.provision(ROUTING_KEY);
        createTbChirpStackIntegrationWithDownlink();

        // Re-root the rule chain so ATTRIBUTES_UPDATED messages flow into TbIntegrationDownlinkNode.
        RuleChainId ruleChainId = createRootRuleChainWithIntegrationDownlinkNode(integration.getId());

        String devEuiHex = HexFormat.of().formatHex(provisionedDevice.getDevEui());
        String downlinkDataBase64 = "AQID"; // [0x01, 0x02, 0x03]

        // Trigger the downlink: saving a shared attribute fires ATTRIBUTES_UPDATED whose JSON
        // body becomes the converter's `msg`. The converter reads devEui+downlinkData from it.
        ObjectNode sharedAttributes = JacksonUtil.newObjectNode();
        sharedAttributes.put("devEui", devEuiHex);
        sharedAttributes.put("downlinkData", downlinkDataBase64);
        testRestClient.saveEntityAttributes(EntityType.DEVICE.name(), device.getId().toString(), SHARED_SCOPE, sharedAttributes);

        // Wait until the integration's downlink node actually saw the message — surfaces converter
        // and HTTP errors as a debug event rather than just a queue that stays empty.
        RuleChainMetaData ruleChainMetadata = testRestClient.getRuleChainMetadata(ruleChainId);
        RuleNode integrationNode = ruleChainMetadata.getNodes().stream()
                .filter(n -> n.getType().equals("org.thingsboard.rule.engine.integration.TbIntegrationDownlinkNode"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("TbIntegrationDownlinkNode not found in rule chain"));
        waitTillRuleNodeReceiveMsg(integrationNode.getId(), EventType.DEBUG_RULE_NODE, integration.getTenantId(), "ATTRIBUTES_UPDATED");

        // Verify ChirpStack actually accepted the queue item with our payload.
        await()
                .pollInterval(1, TimeUnit.SECONDS)
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    JsonNode queue = chirpStackClient.getDeviceQueue(devEuiHex);
                    JsonNode result = queue.get("result");
                    assertThat(result).as("ChirpStack queue response should contain 'result'").isNotNull();
                    boolean matched = false;
                    for (JsonNode item : result) {
                        JsonNode data = item.get("data");
                        JsonNode fPort = item.get("fPort");
                        if (data != null && downlinkDataBase64.equals(data.asText())
                                && fPort != null && fPort.asInt() == F_PORT) {
                            matched = true;
                            break;
                        }
                    }
                    assertThat(matched)
                            .as("ChirpStack downlink queue should contain enqueued item with data=%s fPort=%d", downlinkDataBase64, F_PORT)
                            .isTrue();
                });
    }

    @Override
    protected String getDevicePrototypeSufix() {
        return "chirpstack_";
    }

    private void createTbChirpStackIntegration() {
        JsonNode configConverter = JacksonUtil.toJsonNode(JSON_CONVERTER_CONFIG.replaceAll("DEVICE_NAME", device.getName()));
        createIntegration(CHIRPSTACK, createChirpStackJsonConfig(), configConverter, null, ROUTING_KEY, SECRET_KEY, false, 2);
    }

    private void createTbChirpStackIntegrationWithDownlink() {
        JsonNode configConverter = JacksonUtil.toJsonNode(JSON_CONVERTER_CONFIG.replaceAll("DEVICE_NAME", device.getName()));
        JsonNode downlinkConverter = JacksonUtil.newObjectNode().put("encoder", DOWNLINK_CONVERTER_ENCODER);
        createIntegration(CHIRPSTACK, createChirpStackJsonConfig(), configConverter, downlinkConverter, ROUTING_KEY, SECRET_KEY, false, 2);
    }

    // Builds a real LoRaWAN uplink frame and publishes it to ChirpStack's gateway MQTT event topic.
    private void sendUplink() {
        String json = LoRaWanUplinkFrameBuilder.buildUplinkFrameJson(
                provisionedDevice.getDevAddr(),
                provisionedDevice.getNwkSKey(),
                provisionedDevice.getAppSKey(),
                0, F_PORT, UPLINK_PAYLOAD,
                provisionedDevice.getGatewayId());
        String topic = "eu868/gateway/" + provisionedDevice.getGatewayId() + "/event/up";
        try {
            MqttClient client = new MqttClient(TestProperties.getMqttBrokerUrl(), MqttClient.generateClientId(), new MemoryPersistence());
            client.connect();
            try {
                client.publish(topic, new MqttMessage(json.getBytes(StandardCharsets.UTF_8)));
            } finally {
                client.disconnect();
                client.close();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to publish ChirpStack uplink frame", e);
        }
    }

    private JsonNode createChirpStackJsonConfig() {
        ObjectNode rootNode = JacksonUtil.newObjectNode();
        rootNode.putObject("metadata");

        ObjectNode clientConfigNode = rootNode.putObject("clientConfiguration");
        clientConfigNode.put("baseUrl", HTTPS_URL);
        // Real in-network ChirpStack REST URL + the seeded admin JWT so the TB integration can
        // actually enqueue downlinks. The uplink tests don't read these (uplink path is one-way),
        // but having them populated lets the downlink test reuse the same config.
        clientConfigNode.put("applicationServerUrl", ChirpStackTestClient.getInNetworkRestUrl());
        clientConfigNode.put("applicationServerAPIToken", chirpStackClient.getToken());
        clientConfigNode.put("useAPI4Plus", true);

        return rootNode;
    }

}
