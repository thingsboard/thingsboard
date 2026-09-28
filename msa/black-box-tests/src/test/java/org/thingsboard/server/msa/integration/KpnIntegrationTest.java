// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.msa.integration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.hash.Hashing;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.awaitility.Awaitility;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.debug.DebugSettings;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.msa.WsClient;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static org.thingsboard.server.common.data.DataConstants.DEVICE_NAME;
import static org.thingsboard.server.msa.prototypes.ConverterPrototypes.uplinkConverterPrototype;


@Slf4j
public class KpnIntegrationTest extends AbstractIntegrationTest {

    private static final String ROUTING_KEY = "routing-key-kpn-integration";

    private final JsonNode UPLINK_CONVERTER_CODE = JacksonUtil
            .newObjectNode().put("decoder", "function decodeToString(payload) {\n" +
                    "   return String.fromCharCode.apply(String, payload);\n" +
                    "}\n" +
                    "function decodeToJson(payload) {\n" +
                    "   var str = decodeToString(payload);\n" +
                    "   var data = JSON.parse(str);\n" +
                    "   return data;\n" +
                    "}\n" +
                    "return decodeToJson(payload);");

    private final JsonNode INTEGRATION_CONFIG = JacksonUtil.fromString("{\"baseUrl\": \"http://127.0.0.1:8080\",\n" +
            "\"destinationSharedSecret\": \"\"," +
            "\"enableSecurity\": false," +
            "\"headersFilter\": {" +
            "}," +
            "\"allowDownlink\": false," +
            "\"apiId\": null," +
            "\"apiKey\": null," +
            "\"metadata\": {}}", ObjectNode.class);

    private final JsonNode INTEGRATION_CONFIG_SECURITY_ENABLED = JacksonUtil.fromString("{\"baseUrl\": \"http://127.0.0.1:8080\"," +
            "\"destinationSharedSecret\": \"pnZxe#jVUBalFmNb(3%cNbRJATTe8(gj\"," +
            "\"enableSecurity\": true," +
            "\"headersFilter\": {" +
            "\"X-Things-Secret\": \"K{WY7UqQFu1*/*U916JbfcR3h%m;.PV.\"" +
            "}," +
            "\"allowDownlink\": false," +
            "\"apiId\": null," +
            "\"apiKey\": null," +
            "\"metadata\": {}}", ObjectNode.class);

    private final String MESSAGE_TEMPLATE = "{\"deviceType\":\"DEFAULT\",\"telemetry\":[{\"temperature\":\"42\"}]}";

    private WsClient wsClient;

    @AfterMethod
    public void tearDown() throws Exception {
        if (wsClient != null) {
            wsClient.closeBlocking();
        }
    }

    @Test
    public void shouldUploadTelemetryWhenSecurityIsDisabled() throws Exception {
        createIntegration(INTEGRATION_CONFIG);
        wsClient = subscribeToWebSocket(device.getId(), "LATEST_TELEMETRY", CmdsType.TS_SUB_CMDS);

        ObjectNode payloadForUplink = JacksonUtil.fromString(MESSAGE_TEMPLATE, ObjectNode.class);
        payloadForUplink.put(DEVICE_NAME, device.getName());

        testRestClient.postUplinkPayloadForHttpBasedIntegration(integration.getRoutingKey(), payloadForUplink, integration.getType());

        Awaitility
                .await()
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .until(() -> wsClient.getLastMessage().getDataValuesByKey(TELEMETRY_KEY).get(1).equals(TELEMETRY_VALUE));
    }

    @Test
    public void shouldUploadTelemetryWhenSecurityIsEnabled() throws Exception {
        createIntegration(INTEGRATION_CONFIG_SECURITY_ENABLED);
        wsClient = subscribeToWebSocket(device.getId(), "LATEST_TELEMETRY", CmdsType.TS_SUB_CMDS);


        ObjectNode payloadForUplink = JacksonUtil.fromString(MESSAGE_TEMPLATE, ObjectNode.class);
        payloadForUplink.put(DEVICE_NAME, device.getName());
        Map<String, Object> headers = JacksonUtil.fromString(INTEGRATION_CONFIG_SECURITY_ENABLED.get("headersFilter").toString(), new TypeReference<>() {
        });

        byte[] destinationSharedSecret = INTEGRATION_CONFIG_SECURITY_ENABLED.get("destinationSharedSecret").asText().getBytes(StandardCharsets.UTF_8);
        byte[] dataForHash = ArrayUtils.addAll(JacksonUtil.toString(payloadForUplink).getBytes(), destinationSharedSecret);
        String hashed = Hashing.sha256().hashBytes(dataForHash).toString();
        headers.put("things-message-token", hashed);

        testRestClient.postUplinkPayloadForHttpBasedIntegration(integration.getRoutingKey(), payloadForUplink, integration.getType(), headers);

        Awaitility
                .await()
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .until(() -> wsClient.getLastMessage().getDataValuesByKey(TELEMETRY_KEY).get(1).equals(TELEMETRY_VALUE));
    }

    @Test
    public void shouldRejectUploadTelemetryWhenSecurityIsEnabledAndTokenIsWrong() throws Exception {
        createIntegration(INTEGRATION_CONFIG_SECURITY_ENABLED);
        ObjectNode payloadForUplink = JacksonUtil.fromString(MESSAGE_TEMPLATE, ObjectNode.class);
        payloadForUplink.put(DEVICE_NAME, device.getName());
        Map<String, Object> headers = JacksonUtil.fromString(INTEGRATION_CONFIG_SECURITY_ENABLED.get("headersFilter").toString(), new TypeReference<>() {});
        headers.put("things-message-token", "wrong-token");
        testRestClient.postUplinkPayloadForHttpBasedIntegrationForExpectedErrorStatusCode(integration.getRoutingKey(), payloadForUplink, headers, integration.getType(), HTTP_FORBIDDEN);
    }

    @Test
    public void shouldRejectUploadTelemetryWhenSecurityIsEnabledAndTokenIsAbsent() throws Exception {
        createIntegration(INTEGRATION_CONFIG_SECURITY_ENABLED);
        ObjectNode payloadForUplink = JacksonUtil.fromString(MESSAGE_TEMPLATE, ObjectNode.class);
        payloadForUplink.put(DEVICE_NAME, device.getName());
        testRestClient.postUplinkPayloadForHttpBasedIntegrationForExpectedErrorStatusCode(integration.getRoutingKey(), payloadForUplink, new HashMap<>(), integration.getType(), HTTP_FORBIDDEN);
    }

    private Integration createIntegration(JsonNode config) {
        Integration integration = new Integration();
        integration.setConfiguration(config);

        uplinkConverter = testRestClient.postConverter(uplinkConverterPrototype(UPLINK_CONVERTER_CODE));
        integration.setDefaultConverterId(uplinkConverter.getId());

        integration.setName("kpn_" + RandomStringUtils.randomAlphanumeric(7));
        integration.setType(IntegrationType.KPN);
        integration.setEnabled(true);
        integration.setRemote(false);
        integration.setDebugSettings(DebugSettings.all());
        integration.setAllowCreateDevicesOrAssets(true);
        integration.setRoutingKey(ROUTING_KEY);
        integration.setSecret("secret-key-kpn-integration");

        integration = testRestClient.postIntegration(integration);

        IntegrationId integrationId = integration.getId();
        TenantId tenantId = integration.getTenantId();

        waitUntilIntegrationStarted(integrationId, tenantId);
        return this.integration = integration;
    }

    @Override
    protected String getDevicePrototypeSufix() {
        return "kpn_";
    }

}
