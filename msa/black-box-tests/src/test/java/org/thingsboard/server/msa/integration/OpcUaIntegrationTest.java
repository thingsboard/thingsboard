// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.msa.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.msa.WsClient;
import org.thingsboard.server.msa.mapper.WsTelemetryResponse;

@Slf4j
public class OpcUaIntegrationTest extends AbstractIntegrationTest {
    private static final String ROUTING_KEY = "routing-key-opc-ua";
    private static final String SECRET_KEY = "secret-key-opc-ua";
    private static final String CONFIG_INTEGRATION = "{\"clientConfiguration\":{" +
            "\"applicationName\":\"\"," +
            "\"applicationUri\":\"\"," +
            "\"host\":\"opcplc\"," +
            "\"port\":50000," +
            "\"scanPeriodInSeconds\":10," +
            "\"timeoutInMillis\":5000," +
            "\"security\":\"None\"," +
            "\"identity\":{\"type\":\"anonymous\"}," +
            "\"mapping\":[{" +
            "\"deviceNodePattern\":\"Objects\\\\.Boiler \\\\#\\\\d+\"," +
            "\"mappingType\":\"FQN\"," +
            "\"subscriptionTags\":[{" +
            "\"key\":\"BoilerStatus\"," +
            "\"path\":\"BoilerStatus\"," +
            "\"required\":false}]}]," +
            "\"keystore\":{" +
            "\"location\":\"\"," +
            "\"type\":\"\"," +
            "\"fileContent\":\"\"," +
            "\"password\":\"secret\"," +
            "\"alias\":\"opc-ua-extension\"," +
            "\"keyPassword\":\"secret\"}}," +
            "\"metadata\":{}}";
    private static final String CONFIG_CONVERTER = "var data = decodeToJson(payload);\n" +
            "var deviceName =  '" + "DEVICE_NAME" + "';\n" +
            "var deviceType = 'DEFAULT';\n" +
            "\n" +
            "var result = {\n" +
            "   deviceName: deviceName,\n" +
            "   deviceType: deviceType,\n" +
            "   telemetry: {\n" +
            "   }\n" +
            "};\n" +
            "\n" +
            "var boilerStatus = data.BoilerStatus;\n" +
            "\n" +
            "\n" +
            "if (data.BoilerStatus) {\n" +
            "    result.telemetry.boilerStatus = boilerStatus;\n" +
            "}\n" +
            "\n" +
            "function decodeToString(payload) {\n" +
            "   return String.fromCharCode.apply(String, payload);\n" +
            "}\n" +
            "\n" +
            "function decodeToJson(payload) {\n" +
            "   var str = decodeToString(payload);\n" +
            "   var data = JSON.parse(str);\n" +
            "   return data;\n" +
            "}\n" +
            "\n" +
            "return result;";

    @Test
    public void telemetryUploadWithLocalIntegration() throws Exception {
        JsonNode configConverter = JacksonUtil.newObjectNode().put("decoder",
                CONFIG_CONVERTER.replaceAll("DEVICE_NAME", device.getName()));
        integration = createIntegration(
                IntegrationType.OPC_UA, CONFIG_INTEGRATION, configConverter, ROUTING_KEY, SECRET_KEY, false);
        WsClient wsClient = subscribeToWebSocket(device.getId(), "LATEST_TELEMETRY", CmdsType.TS_SUB_CMDS);

        WsTelemetryResponse actualLatestTelemetry = wsClient.getLastMessage();
        log.info("Received telemetry: {}", actualLatestTelemetry);
        wsClient.closeBlocking();

        Assert.assertEquals(1, actualLatestTelemetry.getData().size());
        Assert.assertEquals(Sets.newHashSet("boilerStatus"), actualLatestTelemetry.getLatestValues().keySet());
    }

    @Override
    protected String getDevicePrototypeSufix() {
        return "opc_ua_";
    }
}
