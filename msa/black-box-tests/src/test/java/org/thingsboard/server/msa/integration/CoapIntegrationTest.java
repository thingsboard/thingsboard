// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.msa.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Sets;
import jakarta.xml.bind.DatatypeConverter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.elements.exception.ConnectorException;
import org.testcontainers.containers.ContainerState;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.msa.WsClient;
import org.thingsboard.server.msa.mapper.WsTelemetryResponse;

import java.io.IOException;

import static ch.qos.logback.core.encoder.ByteArrayUtil.hexStringToByteArray;
import static org.thingsboard.server.msa.TestProperties.getRemoteCoapHost;
import static org.thingsboard.server.msa.TestProperties.getRemoteCoapPort;

@Slf4j
public class CoapIntegrationTest extends AbstractIntegrationTest {
    private static final String ROUTING_KEY = "routing-key-coap";
    private static final String SECRET_KEY = "secret-key-coap";
    private static final String TOKEN = ROUTING_KEY;
    private static final String CONFIG_INTEGRATION = "{\"clientConfiguration\":{" +
            "\"baseUrl\":\"coap://localhost\"," +
            "\"dtlsBaseUrl\":\"coaps://localhost\"," +
            "\"securityMode\":\"NO_SECURE\"," +
            "\"coapEndpoint\":\"coap://localhost/i/" + TOKEN + "\"," +
            "\"dtlsCoapEndpoint\":\"coaps://localhost/i/" + TOKEN + "\"},\"metadata\":{}}";
    private static final String JSON_CONVERTER_CONFIG = "var data = decodeToJson(payload);\n" +
            "var deviceName =  '" + "DEVICE_NAME" + "';\n" +
            "var deviceType = 'DEFAULT';\n" +
            "var data = decodeToJson(payload);\n" +
            "var result = {\n" +
            "   deviceName: deviceName,\n" +
            "   deviceType: deviceType,\n" +
            "   attributes: {},\n" +
            "   telemetry: {\n" +
            "       temperature: data.temperature,\n" +
            "       humidity: data.humidity\n" +
            "   }\n" +
            "};\n" +
            "\n" +
            "/** Helper functions **/\n" +
            "function decodeToString(payload) {\n" +
            "   return String.fromCharCode.apply(String, payload);\n" +
            "}\n" +
            "function decodeToJson(payload) {\n" +
            "   // covert payload to string.\n" +
            "   var str = decodeToString(payload);\n" +
            "   var data = JSON.parse(str);\n" +
            "   return data;\n" +
            "}\n" +
            "return result;";

    private static final String TEXT_CONVERTER_CONFIG = "var strArray = decodeToString(payload);\n" +
            "var payloadArray = strArray.replace(/\\\"/g, \"\").replace(/\\s/g, \"\").split(',');\n" +
            "var telemetryKey = payloadArray[2];\n" +
            "var telemetryValue = payloadArray[3]; \n" +
            "var telemetryPayload = {};\n" +
            "telemetryPayload[telemetryKey] = telemetryValue;\n" +
            "var result = {\n" +
            "    deviceName: payloadArray[0],\n" +
            "    deviceType: payloadArray[1],\n" +
            "    telemetry: telemetryPayload,\n" +
            "    attributes: {}\n" +
            "  };\n" +
            "function decodeToString(payload) {\n" +
            "   return String.fromCharCode.apply(String, payload);\n" +
            "}\n" +
            "return result;";

    private static final String BINARY_CONVERTER_CONFIG = "var payloadStr = decodeToString(payload);\n" +
            "\n" +
            "var deviceName = payloadStr.substring(0,12);\n" +
            "var deviceType = payloadStr.substring(12,19);\n" +
            "\n" +
            "var result = {\n" +
            "   deviceName: deviceName,\n" +
            "   deviceType: deviceType,\n" +
            "   attributes: {},\n" +
            "   telemetry: {\n" +
            "       temperature: parseFloat(payloadStr.substring(19,25))\n" +
            "   }\n" +
            "};\n" +
            "\n" +
            "function decodeToString(payload) {\n" +
            "   return String.fromCharCode.apply(String, payload);\n" +
            "}\n" +
            "\n" +
            "return result;";

    @AfterMethod
    public void afterMethod() {
        if (containerTestSuite.isActive()) {
            ContainerState tcpIntegrationContainer = containerTestSuite.getTestContainer().getContainerByServiceName("tb-coap-integration_1").get();
            tcpIntegrationContainer.getDockerClient().restartContainerCmd(tcpIntegrationContainer.getContainerId()).exec();
        }
    }

    @Test
    public void checkTelemetryUploadedWithJsonConverter() throws Exception {
        JsonNode configConverter = JacksonUtil.newObjectNode().put("decoder",
                JSON_CONVERTER_CONFIG.replaceAll("DEVICE_NAME", device.getName()));
        integration = createIntegration(
                IntegrationType.COAP, CONFIG_INTEGRATION, configConverter, ROUTING_KEY, SECRET_KEY, true);

        WsClient wsClient = subscribeToWebSocket(device.getId(), "LATEST_TELEMETRY", CmdsType.TS_SUB_CMDS);

        sendMessageToIntegration(createPayloadForUplink().toString().getBytes(), MediaTypeRegistry.APPLICATION_JSON);

        WsTelemetryResponse actualLatestTelemetry = wsClient.getLastMessage();
        log.info("Received telemetry: {}", actualLatestTelemetry);
        wsClient.closeBlocking();

        Assert.assertEquals(1, actualLatestTelemetry.getData().size());
        Assert.assertEquals(Sets.newHashSet(TELEMETRY_KEY),
                actualLatestTelemetry.getLatestValues().keySet());

        Assert.assertTrue(verify(actualLatestTelemetry, TELEMETRY_KEY, TELEMETRY_VALUE));
    }

    @Test
    public void checkTelemetryUploadedWithTextConverter() throws Exception {
        JsonNode configConverter = JacksonUtil.newObjectNode().put("decoder", TEXT_CONVERTER_CONFIG);
        integration = createIntegration(
                IntegrationType.COAP, CONFIG_INTEGRATION, configConverter, ROUTING_KEY, SECRET_KEY, true);

        WsClient wsClient = subscribeToWebSocket(device.getId(), "LATEST_TELEMETRY", CmdsType.TS_SUB_CMDS);

        String textPayload = device.getName() + ",default,temperature,25.7";
        sendMessageToIntegration(textPayload.getBytes(), MediaTypeRegistry.TEXT_PLAIN);

        WsTelemetryResponse actualLatestTelemetry = wsClient.getLastMessage();
        log.info("Received telemetry: {}", actualLatestTelemetry);
        wsClient.closeBlocking();

        Assert.assertEquals(1, actualLatestTelemetry.getData().size());
        Assert.assertEquals(Sets.newHashSet(TELEMETRY_KEY),
                actualLatestTelemetry.getLatestValues().keySet());

        Assert.assertTrue(verify(actualLatestTelemetry, TELEMETRY_KEY, "25.7"));
    }

    @Test
    public void checkTelemetryUploadedWithBinaryConverter() throws Exception {
        JsonNode configConverter = JacksonUtil.newObjectNode().put("decoder", BINARY_CONVERTER_CONFIG);
        integration = createIntegration(
                IntegrationType.COAP, CONFIG_INTEGRATION, configConverter, ROUTING_KEY, SECRET_KEY, true);

        WsClient wsClient = subscribeToWebSocket(device.getId(), "LATEST_TELEMETRY", CmdsType.TS_SUB_CMDS);

        String hexString = DatatypeConverter.printHexBinary((device.getName() + "default25.7").getBytes());
        byte[] bytes = hexStringToByteArray(hexString);
        sendMessageToIntegration(bytes, MediaTypeRegistry.APPLICATION_OCTET_STREAM);

        WsTelemetryResponse actualLatestTelemetry = wsClient.getLastMessage();
        log.info("Received telemetry: {}", actualLatestTelemetry);
        wsClient.closeBlocking();

        Assert.assertEquals(1, actualLatestTelemetry.getData().size());
        Assert.assertEquals(Sets.newHashSet(TELEMETRY_KEY),
                actualLatestTelemetry.getLatestValues().keySet());

        Assert.assertTrue(verify(actualLatestTelemetry, TELEMETRY_KEY, "25.7"));
    }

    private void sendMessageToIntegration(byte[] payload, int format) throws ConnectorException, IOException {
        CoapClient coapClient = new CoapClient("coap", getRemoteCoapHost(), getRemoteCoapPort(), "i", TOKEN);
        coapClient.post(payload, format);
    }

    @Override
    protected String getDevicePrototypeSufix() {
        return "coap_";
    }
}
