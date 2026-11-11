// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.msa.integration;

import com.azure.messaging.eventhubs.EventData;
import com.azure.messaging.eventhubs.EventDataBatch;
import com.azure.messaging.eventhubs.EventHubClientBuilder;
import com.azure.messaging.eventhubs.EventHubProducerClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.msa.WsClient;
import org.thingsboard.server.msa.mapper.WsTelemetryResponse;

import static org.thingsboard.server.common.data.integration.IntegrationType.AZURE_EVENT_HUB;

@Slf4j
public class AzureEventHubIntegrationTest extends AbstractIntegrationTest {
    private static final String ROUTING_KEY = "routing-key-azure-event";
    private static final String SECRET_KEY = "secret-key-azure-event";
    private static final String CONNECTION_STRING = System.getProperty("blackBoxTests.azureEventHubConnectionString", "");
    private static final String STORAGE_CONNECTION_STRING = System.getProperty("blackBoxTests.azureEventHubStorageConnectionString", "");
    private static final String CONTAINER_NAME = System.getProperty("blackBoxTests.azureEventHubContainerName", "");
    private static final String CONFIG_INTEGRATION = "{\"clientConfiguration\":{" +
            "\"connectTimeoutSec\":10," +
            "\"connectionString\":\"" + CONNECTION_STRING + "\"," +
            "\"storageConnectionString\":\"" + STORAGE_CONNECTION_STRING + "\"," +
            "\"containerName\":\"" + CONTAINER_NAME + "\"," +
            "\"enablePersistentCheckpoints\": false," +
            "\"consumerGroup\":\"\"," +
            "\"iotHubName\":\"\"}," +
            "\"metadata\":{}}";
    private static final String CONFIG_CONVERTER = "var payloadStr = decodeToString(payload);\n" +
            "var data = JSON.parse(payloadStr);\n" +
            "var deviceName =  '" + "DEVICE_NAME" + "';\n" +
            "var deviceType = 'DEFAULT';\n" +
            "var result = {\n" +
            "   deviceName: deviceName,\n" +
            "   deviceType: deviceType,\n" +
            "   telemetry: {\n" +
            "       temperature: data.temperature,\n" +
            "   }\n" +
            "};\n" +
            "\n" +
            "function decodeToString(payload) {\n" +
            "   return String.fromCharCode.apply(String, payload);\n" +
            "}\n" +
            "\n" +
            "function decodeToJson(payload) {\n" +
            "   var str = decodeToString(payload);\n" +
            "\n" +
            "   var data = JSON.parse(str);\n" +
            "   return data;\n" +
            "}\n" +
            "return result;";

    @BeforeClass
    public static void beforeClass() {
        if (Boolean.parseBoolean(System.getProperty("blackBoxTests.integrations.skip", "true"))) {
            throw new SkipException("AzureventHubIntegrationTest is skipped");
        }
    }

    @Test
    public void telemetryUploadWithLocalIntegration() throws Exception {
        JsonNode configConverter = JacksonUtil.newObjectNode().put("decoder",
                CONFIG_CONVERTER.replaceAll("DEVICE_NAME", device.getName()));
        integration = createIntegration(AZURE_EVENT_HUB, CONFIG_INTEGRATION, configConverter, ROUTING_KEY, SECRET_KEY, false);

        Thread.sleep(10000); // await for initialization finish

        WsClient wsClient = subscribeToWebSocket(device.getId(), "LATEST_TELEMETRY", CmdsType.TS_SUB_CMDS);

        sendMessageToHub();

        WsTelemetryResponse actualLatestTelemetry = wsClient.getLastMessage();
        log.info("Received telemetry: {}", actualLatestTelemetry);
        wsClient.closeBlocking();

        Assert.assertEquals(1, actualLatestTelemetry.getData().size());
        Assert.assertEquals(Sets.newHashSet(TELEMETRY_KEY),
                actualLatestTelemetry.getLatestValues().keySet());

        Assert.assertTrue(verify(actualLatestTelemetry, TELEMETRY_KEY, TELEMETRY_VALUE));
    }

    void sendMessageToHub() {
        String payload = createPayloadForUplink().toString();

        EventHubProducerClient producer = new EventHubClientBuilder()
                .connectionString(CONNECTION_STRING)
                .buildProducerClient();

        EventData data = new EventData(payload);

        EventDataBatch eventDataBatch = producer.createBatch();
        eventDataBatch.tryAdd(data);
        producer.send(eventDataBatch);
        producer.close();
    }

    @Override
    protected String getDevicePrototypeSufix() {
        return "azure_event_";
    }
}
