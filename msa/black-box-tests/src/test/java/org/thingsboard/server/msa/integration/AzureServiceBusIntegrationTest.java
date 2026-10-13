// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.msa.integration;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Sets;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.event.EventType;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.msa.WsClient;
import org.thingsboard.server.msa.mapper.WsTelemetryResponse;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.thingsboard.server.common.data.DataConstants.DEVICE;
import static org.thingsboard.server.common.data.DataConstants.SHARED_SCOPE;
import static org.thingsboard.server.common.data.integration.IntegrationType.AZURE_SERVICE_BUS;

@Slf4j
public class AzureServiceBusIntegrationTest extends AbstractIntegrationTest {
    private static final String ROUTING_KEY = "routing-key-azure-service-bus";
    private static final String SECRET_KEY = "secret-key-azure-service-bus";
    protected static final String HUMIDITY_KEY = "humidity";
    protected static final String INFO_KEY = "info";
    private static final String CONNECTION_STRING = System.getProperty("blackBoxTests.azureServiceBusConnectionString", "");
    private static final String TOPIC_NAME = System.getProperty("blackBoxTests.azureServiceBusTopicName", "");
    private static final String SUBSCRIPTION_NAME = System.getProperty("blackBoxTests.azureServiceBusSubName", "");
    private static final String DOWNLINK_CONNECTION_STRING = System.getProperty("blackBoxTests.azureServiceBusDownlinkConnectionString", "");
    private static final String DOWNLINK_TOPIC_NAME = System.getProperty("blackBoxTests.azureServiceBusDownlinkTopicName", "");
    private static final String DOWNLINK_TOPIC_SUB_NAME = System.getProperty("blackBoxTests.azureServiceBusDownlinkSubName", "");
    private static final JsonNode INTEGRATION_CONFIG = JacksonUtil.toJsonNode("{\"clientConfiguration\":{" +
            "\"connectionString\":\"" + CONNECTION_STRING + "\"," +
            "\"topicName\":\"" + TOPIC_NAME + "\"," +
            "\"subName\":\"" + SUBSCRIPTION_NAME + "\"," +
            "\"downlinkConnectionString\":\"" + DOWNLINK_CONNECTION_STRING + "\"," +
            "\"downlinkTopicName\":\"" + DOWNLINK_TOPIC_NAME + "\"}," +
            "\"metadata\":{}}");

    private static final JsonNode CONVERTER_CONFIG = JacksonUtil.newObjectNode().put("decoder", "var strArray = decodeToString(payload);\n" +
            "var payloadArray = strArray.replace(/\\\"/g, \"\").replace(/\\s/g, \"\").replace(/\\\\n/g, \"\").split(',');\n" +
            "var telemetryPayload = {};\n" +
            "for (var i = 2; i < 6; i = i + 2) {\n" +
            "    var telemetryKey = payloadArray[i];\n" +
            "    var telemetryValue = parseFloat(payloadArray[i + 1]);\n" +
            "    telemetryPayload[telemetryKey] = telemetryValue;\n" +
            "}\n" +
            "telemetryPayload[payloadArray[6]] = payloadArray[7];\n" +
            "// Result object with device attributes/telemetry data\n" +
            "var result = {\n" +
            "    deviceName: payloadArray[0],\n" +
            "    deviceType: payloadArray[1],\n" +
            "    telemetry: telemetryPayload,\n" +
            "    attributes: {}\n" +
            "  };\n" +
            "function decodeToString(payload) {\n" +
            "   return String.fromCharCode.apply(String, payload);\n" +
            "}\n" +
            "return result;");

    private final JsonNode DOWNLINK_CONVERTER_CONFIG = JacksonUtil.newObjectNode()
            .put("encoder", "var data = {};\n" +
                    "data.booleanKey = msg.booleanKey;\n" +
                    "data.stringKey = msg.stringKey;\n" +
                    "data.stringKey2 = msg.stringKey2;\n" +
                    "data.stringKey3 = msg.stringKey3;\n" +
                    "data.stringKey4 = msg.stringKey4;\n" +
                    "data.doubleKey = msg.doubleKey;\n" +
                    "data.longKey = msg.longKey;\n" +
                    "\n" +
                    "data.devSerialNumber = metadata['ss_serialNumber'];\n" +
                    "var result = {\n" +
                    "    contentType: \"JSON\",\n" +
                    "    data: JSON.stringify(data),\n" +
                    "    metadata: {\n" +
                    "        deviceId: 'myDevice'\n" +
                    "    }\n" +
                    "};\n" +
                    "return result;");

    private final BlockingQueue<String> messageList = new ArrayBlockingQueue<>(100);

    @BeforeClass
    public static void beforeClass() {
        if (Boolean.parseBoolean(System.getProperty("blackBoxTests.integrations.skip", "true"))) {
            throw new SkipException("AzureServiceBusIntegrationTest is skipped");
        }
    }

    @Test
    public void telemetryUploadWithLocalIntegration() throws Exception {
        createIntegration(AZURE_SERVICE_BUS, INTEGRATION_CONFIG, CONVERTER_CONFIG, ROUTING_KEY, SECRET_KEY, false);

        WsClient wsClient = subscribeToWebSocket(device.getId(), "LATEST_TELEMETRY", CmdsType.TS_SUB_CMDS);

        String temp = "27.7";
        String humidity = "67";
        String info = "漢字special$_українськаלום";
        sendMessageToServiceBusTopic(device, temp, humidity, info);

        WsTelemetryResponse actualLatestTelemetry = wsClient.getLastMessage();
        log.info("Received telemetry: {}", actualLatestTelemetry);
        wsClient.closeBlocking();

        Assert.assertEquals(3, actualLatestTelemetry.getData().size());
        Assert.assertEquals(actualLatestTelemetry.getLatestValues().keySet(), Sets.newHashSet(TELEMETRY_KEY, HUMIDITY_KEY, INFO_KEY));

        Assert.assertTrue(verify(actualLatestTelemetry, TELEMETRY_KEY, temp));
        Assert.assertTrue(verify(actualLatestTelemetry, HUMIDITY_KEY, humidity));
        Assert.assertTrue(verify(actualLatestTelemetry, INFO_KEY, info));
    }

    @Test
    public void checkDownlinkMessageWasSent() throws Exception {
        createIntegration(AZURE_SERVICE_BUS, INTEGRATION_CONFIG, CONVERTER_CONFIG, DOWNLINK_CONVERTER_CONFIG, ROUTING_KEY, SECRET_KEY, false);

        //subscribe for service bus topic
        try (ServiceBusProcessorClient serviceBusProcessorClient = getDownlinkProcessorClient()) {
            serviceBusProcessorClient.start();

            //add downlink node
            RuleChainId ruleChainId = createRootRuleChainWithIntegrationDownlinkNode(integration.getId());

            //create 4 attributes (stringKey, booleanKey, doubleKey, longKey)
            JsonNode attributes = JacksonUtil.toJsonNode(createPayload().toString());
            testRestClient.saveEntityAttributes(DEVICE, device.getId().toString(), SHARED_SCOPE, attributes);

            RuleChainMetaData ruleChainMetadata = testRestClient.getRuleChainMetadata(ruleChainId);
            RuleNode downlinkNode = ruleChainMetadata.getNodes().stream().filter(ruleNode -> ruleNode.getType().equals("org.thingsboard.rule.engine.integration.TbIntegrationDownlinkNode")).findFirst().get();
            waitTillRuleNodeReceiveMsg(downlinkNode.getId(), EventType.DEBUG_RULE_NODE, integration.getTenantId(), "ATTRIBUTES_UPDATED");

            //check downlink message
            Awaitility
                    .await()
                    .alias("Get message from azure service bus topic")
                    .atMost(20, TimeUnit.SECONDS)
                    .until(() -> messageList.size() > 0);

            JsonNode actual = JacksonUtil.toJsonNode(messageList.poll());

            assertThat(actual.get("stringKey")).isEqualTo(attributes.get("stringKey"));
            assertThat(actual.get("stringKey2")).isEqualTo(attributes.get("stringKey2"));
            assertThat(actual.get("stringKey3")).isEqualTo(attributes.get("stringKey3"));
            assertThat(actual.get("stringKey4")).isEqualTo(attributes.get("stringKey4"));
            assertThat(actual.get("booleanKey")).isEqualTo(attributes.get("booleanKey"));
            assertThat(actual.get("doubleKey")).isEqualTo(attributes.get("doubleKey"));
            assertThat(actual.get("longKey")).isEqualTo(attributes.get("longKey"));
        }
    }

    private ServiceBusProcessorClient getDownlinkProcessorClient() {
        return new ServiceBusClientBuilder()
                .connectionString(DOWNLINK_CONNECTION_STRING)
                .processor()
                .topicName(DOWNLINK_TOPIC_NAME)
                .subscriptionName(DOWNLINK_TOPIC_SUB_NAME)
                .processMessage(this::processMessage)
                .processError(error -> log.error("It was trouble when receiving: " + error.getException().getMessage()))
                .buildProcessorClient();
    }

    private void processMessage(ServiceBusReceivedMessageContext context) {
        ServiceBusReceivedMessage message = context.getMessage();
        messageList.add(new String(message.getBody().toBytes()));
    }

    void sendMessageToServiceBusTopic(Device device, String temp, String humidity, String info) {
        try (ServiceBusSenderClient serviceBusSenderClient = new ServiceBusClientBuilder()
                .connectionString(CONNECTION_STRING)
                .sender()
                .topicName(TOPIC_NAME)
                .buildClient()) {
            serviceBusSenderClient.sendMessage(new ServiceBusMessage(String.format("%s,default,temperature,%s,humidity,%s,info,%s", device.getName(), temp, humidity, info)));
        }
    }

    @Override
    protected String getDevicePrototypeSufix() {
        return "azure_service_bus_";
    }

    public JsonObject createPayload() {
        JsonObject values = new JsonObject();
        values.addProperty("stringKey", "漢字");
        values.addProperty("stringKey2", "special$");
        values.addProperty("stringKey3", "українська");
        values.addProperty("stringKey4", "שלום");
        values.addProperty("booleanKey", true);
        values.addProperty("doubleKey", 42.6);
        values.addProperty("longKey", 73L);

        return values;
    }
}
