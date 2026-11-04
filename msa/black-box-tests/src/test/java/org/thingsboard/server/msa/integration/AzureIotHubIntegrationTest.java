// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.msa.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Sets;
import com.microsoft.azure.sdk.iot.service.DeliveryAcknowledgement;
import com.microsoft.azure.sdk.iot.service.IotHubServiceClientProtocol;
import com.microsoft.azure.sdk.iot.service.Message;
import com.microsoft.azure.sdk.iot.service.ServiceClient;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.msa.WsClient;
import org.thingsboard.server.msa.mapper.WsTelemetryResponse;

import java.util.UUID;

import static org.thingsboard.server.common.data.integration.IntegrationType.AZURE_IOT_HUB;

@Slf4j
public class AzureIotHubIntegrationTest extends AbstractIntegrationTest {
    private static final String ROUTING_KEY = "routing-key-azure-iot";
    private static final String SECRET_KEY = "secret-key-azure-iot";
    private static final String HOST_NAME = System.getProperty("blackBoxTests.azureIotHubHostName", "");
    private static final String SAS_KEY = System.getProperty("blackBoxTests.azureIotHubSasKey", "");
    private static final String DEVICE_ID = System.getProperty("blackBoxTests.azureIotHubDeviceId", "");
    private static final String CONFIG_INTEGRATION = "{\"clientConfiguration\":{" +
            "\"host\":\"" + HOST_NAME + ".azure-devices.net\"," +
            "\"port\":8883," +
            "\"cleanSession\":true," +
            "\"ssl\":true," +
            "\"maxBytesInMessage\":32368," +
            "\"connectTimeoutSec\":10," +
            "\"clientId\":\"" + DEVICE_ID + "\"," +
            "\"credentials\":{" +
            "\"type\":\"sas\"," +
            "\"sasKey\":\"" + SAS_KEY + "\"}}," +
            "\"topicFilters\":[{" +
            "\"filter\":\"devices/" + DEVICE_ID + "/messages/devicebound/#\"," +
            "\"qos\":0}]," +
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
            throw new SkipException("AzurIotHubIntegrationTest is skipped");
        }
    }

    @Test
    public void telemetryUploadWithLocalIntegration() throws Exception {
        JsonNode configConverter = JacksonUtil.newObjectNode().put("decoder",
                CONFIG_CONVERTER.replaceAll("DEVICE_NAME", device.getName()));
        createIntegration(AZURE_IOT_HUB, CONFIG_INTEGRATION, configConverter, ROUTING_KEY, SECRET_KEY, false);

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

    void sendMessageToHub() throws Exception {
        ServiceClient serviceClient = initServiceClient();
        String payload = createPayloadForUplink().toString();

        Message message = new Message(payload);
        message.setDeliveryAcknowledgement(DeliveryAcknowledgement.Full);
        message.setMessageId(UUID.randomUUID().toString());
        message.getProperties().put("content-type", "JSON");
        serviceClient.send(DEVICE_ID, message);
        serviceClient.close();
    }

    private ServiceClient initServiceClient() throws Exception {
        //Event Hub-compatible endpoint
        String connectionString = System.getProperty("blackBoxTests.azureIotHubConnectionString");

        ServiceClient serviceClient = ServiceClient.createFromConnectionString(connectionString, IotHubServiceClientProtocol.AMQPS);
        serviceClient.open();
        return serviceClient;
    }

    @Override
    protected String getDevicePrototypeSufix() {
        return "azure_iot_";
    }
}
