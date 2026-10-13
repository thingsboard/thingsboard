// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.msa.integration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import io.restassured.path.json.JsonPath;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.event.EventType;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.msa.WsClient;
import org.thingsboard.server.msa.mapper.WsTelemetryResponse;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.thingsboard.server.common.data.DataConstants.CLIENT_SCOPE;
import static org.thingsboard.server.common.data.DataConstants.DEVICE;
import static org.thingsboard.server.common.data.DataConstants.SHARED_SCOPE;
import static org.thingsboard.server.common.data.integration.IntegrationType.HTTP;
import static org.thingsboard.server.msa.prototypes.HttpIntegrationConfigPrototypes.defaultConfig;
import static org.thingsboard.server.msa.prototypes.HttpIntegrationConfigPrototypes.defaultConfigWithSecurityEnabled;
import static org.thingsboard.server.msa.prototypes.HttpIntegrationConfigPrototypes.defaultConfigWithSecurityHeader;
import static org.thingsboard.server.msa.prototypes.HttpIntegrationConfigPrototypes.defaultConfigWithSecurityHeader2;


@Slf4j
public class HttpIntegrationTest extends AbstractIntegrationTest {
    private static final String ROUTING_KEY = "routing-key-123456";
    private static final String SECRET_KEY = "secret-key-123456";

    private final JsonNode CUSTOM_CONVERTER_CONFIGURATION = JacksonUtil
            .newObjectNode().put("decoder", "var data = decodeToJson(payload);\n" +
                    "var deviceName = data.deviceName;\n" +
                    "var deviceType = data.deviceType;\n" +
                    "var result = {\n" +
                    "   deviceName: deviceName,\n" +
                    "   deviceType: deviceType,\n" +
                    "   updateOnlyKeys: [\"humidity\"],\n" +
                    "   attributes: {\n" +
                    "       model: data.model,\n" +
                    "       serialNumber: data.param2,\n" +
                    "       humidity: data.humidity\n" +
                    "   },\n" +
                    "   telemetry: {\n" +
                    "       temperature: data.temperature\n" +
                    "   }\n" +
                    "};\n" +
                    "function decodeToString(payload) {\n" +
                    "   return String.fromCharCode.apply(String, payload);\n" +
                    "}\n" +
                    "function decodeToJson(payload) {\n" +
                    "   var str = decodeToString(payload);\n" +
                    "   var data = JSON.parse(str);\n" +
                    "   return data;\n" +
                    "}\n" +
                    "return result;");
    private final JsonNode DOWNLINK_CONVERTER_CONFIGURATION = JacksonUtil
            .newObjectNode().put("encoder", "var data = {};\n" +
                    "data.booleanKey = msg.booleanKey;\n" +
                    "data.stringKey = msg.stringKey;\n" +
                    "data.doubleKey = msg.doubleKey;\n" +
                    "data.longKey = msg.longKey;\n" +
                    "\n" +
                    "data.devSerialNumber = metadata['ss_serialNumber'];\n" +
                    "var result = {\n" +
                    "    contentType: \"JSON\",\n" +
                    "    data: JSON.stringify(data),\n" +
                    "    metadata: {\n" +
                    "            topic: metadata['deviceType']+'/'+metadata['deviceName']+'/upload'\n" +
                    "    }\n" +
                    "};\n" +
                    "return result;");

    private WsClient wsClient;

    @AfterMethod
    public void tearDown() throws Exception {
        if (wsClient != null) {
            wsClient.closeBlocking();
        }
    }

    @DataProvider(name = "integrationConfigs")
    public Object[][] integrationConfigs() {
        return new Object[][]{new Object[]{defaultConfig(HTTPS_URL)},
                new Object[]{defaultConfigWithSecurityEnabled(HTTPS_URL)},
                new Object[]{defaultConfigWithSecurityHeader(HTTPS_URL)}
        };
    }

    @Test(dataProvider = "integrationConfigs")
    public void checkTelemetryUploadedWithLocalIntegration(JsonNode config) throws Exception {
        createIntegration(HTTP, config, CUSTOM_CONVERTER_CONFIGURATION, ROUTING_KEY, SECRET_KEY, false);

        wsClient = subscribeToWebSocket(device.getId(), "LATEST_TELEMETRY", CmdsType.TS_SUB_CMDS);
        if (!config.get("headersFilter").isEmpty()) {
            Map<String, Object> securityHeaders = JacksonUtil.fromString(config.get("headersFilter").toString(), new TypeReference<>() {});
            testRestClient.postUplinkPayloadForHttpIntegration(integration.getRoutingKey(), createPayloadForUplink(device, TELEMETRY_VALUE), securityHeaders);
        } else {
            testRestClient.postUplinkPayloadForHttpIntegration(integration.getRoutingKey(), createPayloadForUplink(device, TELEMETRY_VALUE));
        }

        WsTelemetryResponse actualLatestTelemetry = wsClient.getLastMessage();
        assertThat(actualLatestTelemetry.getDataValuesByKey(TELEMETRY_KEY).get(1)).isEqualTo(TELEMETRY_VALUE);
    }

    @Test
    public void checkOnUpdateAttributeKeysUploadedAndNotUpdatedWithLocalIntegration() throws Exception {
        JsonNode config = defaultConfig(HTTPS_URL);
        createIntegration(HTTP, config, CUSTOM_CONVERTER_CONFIGURATION, ROUTING_KEY, SECRET_KEY, false);

        for (int i = 0; i < 4; i++) {
            // Loop is required to update all nodes in cluster mode.
            // Cluster mode has a corner case when values for keys in updateOnlyKeys can be updated on different nodes.
            testRestClient.postUplinkPayloadForHttpIntegration(integration.getRoutingKey(), createPayloadForUplink(device, ATTRIBUTE_KEY, TELEMETRY_VALUE));
        }

        Thread.sleep(3000); // Wait for all attributes updated in cluster mode

        List<JsonNode> attributes = testRestClient.getEntityAttributeByScopeAndKey(device.getId(), CLIENT_SCOPE, ATTRIBUTE_KEY);
        Assert.assertFalse(attributes.isEmpty());
        Assert.assertEquals(attributes.get(0).get("value").asText(), TELEMETRY_VALUE);

        long firstUpdateTs = attributes.get(0).get("lastUpdateTs").asLong();

        testRestClient.postUplinkPayloadForHttpIntegration(integration.getRoutingKey(), createPayloadForUplink(device, ATTRIBUTE_KEY, TELEMETRY_VALUE));

        Awaitility
                .await()
                .atMost(10, TimeUnit.SECONDS)
                .until(() -> {
                    List<JsonNode> attrs = testRestClient.getEntityAttributeByScopeAndKey(device.getId(), CLIENT_SCOPE, ATTRIBUTE_KEY);
                    Assert.assertFalse(attrs.isEmpty());
                    Assert.assertEquals(attrs.get(0).get("lastUpdateTs").asLong(), firstUpdateTs);
                    Assert.assertEquals(attrs.get(0).get("value").asText(), TELEMETRY_VALUE);
                    return true;
                });

        testRestClient.postUplinkPayloadForHttpIntegration(integration.getRoutingKey(), createPayloadForUplink(device, ATTRIBUTE_KEY, "35"));
        Awaitility
                .await()
                .atMost(10, TimeUnit.SECONDS)
                .until(() -> {
                    List<JsonNode> attrs = testRestClient.getEntityAttributeByScopeAndKey(device.getId(), CLIENT_SCOPE, ATTRIBUTE_KEY);
                    Assert.assertFalse(attrs.isEmpty());
                    Assert.assertNotEquals(attrs.get(0).get("lastUpdateTs").asLong(), firstUpdateTs);
                    Assert.assertEquals(attrs.get(0).get("value").asText(), "35");
                    return true;
                });
    }

    @Test
    public void checkTelemetryUploadedAfterIntegrationConfigUpdated() throws Exception {
        JsonNode config = defaultConfigWithSecurityHeader(HTTPS_URL);
        createIntegration(HTTP, config, CUSTOM_CONVERTER_CONFIGURATION, ROUTING_KEY, SECRET_KEY, false);

        wsClient = subscribeToWebSocket(device.getId(), "LATEST_TELEMETRY", CmdsType.TS_SUB_CMDS);

        Map<String, Object> securityHeaders = JacksonUtil.fromString(config.get("headersFilter").toString(), new TypeReference<>() {});
        testRestClient.postUplinkPayloadForHttpIntegration(integration.getRoutingKey(), createPayloadForUplink(device, TELEMETRY_VALUE), securityHeaders);

        WsTelemetryResponse actualLatestTelemetry = wsClient.getLastMessage();
        assertThat(actualLatestTelemetry.getDataValuesByKey(TELEMETRY_KEY).get(1)).isEqualTo(TELEMETRY_VALUE);

        //update integration with new security header
        JsonNode config2 = defaultConfigWithSecurityHeader2(HTTPS_URL);
        integration.setConfiguration(config2);
        integration = testRestClient.postIntegration(integration);
        waitForIntegrationEvent(integration, "UPDATED", 1);

        String temperatureValue2 = "58";
        Map<String, Object> securityHeaders2 = JacksonUtil.fromString(config2.get("headersFilter").toString(), new TypeReference<>() {});
        testRestClient.postUplinkPayloadForHttpIntegration(integration.getRoutingKey(), createPayloadForUplink(device, temperatureValue2), securityHeaders2);

        Awaitility
                .await()
                .atMost(10, TimeUnit.SECONDS)
                .until(() -> wsClient.getMessage().getDataValuesByKey(TELEMETRY_KEY).get(1).equals(temperatureValue2));

        //update integration with new security disabled
        integration.setConfiguration(defaultConfig(HTTPS_URL));
        integration = testRestClient.postIntegration(integration);
        waitForIntegrationEvent(integration, "UPDATED", 2);

        String temperatureValue3 = "35";
        testRestClient.postUplinkPayloadForHttpIntegration(integration.getRoutingKey(), createPayloadForUplink(device, temperatureValue3));

        Awaitility
                .await()
                .atMost(10, TimeUnit.SECONDS)
                .until(() -> wsClient.getMessage().getDataValuesByKey(TELEMETRY_KEY).get(1).equals(temperatureValue3));
    }

    @Test
    public void checkTelemetryUploadedAfterRemoteIntegrationConfigUpdated() throws Exception {
        JsonNode config = defaultConfigWithSecurityHeader(HTTPS_URL);
        createIntegration(HTTP, config, CUSTOM_CONVERTER_CONFIGURATION, ROUTING_KEY, SECRET_KEY, true);

        wsClient = subscribeToWebSocket(device.getId(), "LATEST_TELEMETRY", CmdsType.TS_SUB_CMDS);

        Map<String, Object> securityHeaders = JacksonUtil.fromString(config.get("headersFilter").toString(), new TypeReference<>() {});
        remoteHttpClient.postUplinkPayloadForHttpIntegration(integration.getRoutingKey(), createPayloadForUplink(device, TELEMETRY_VALUE), securityHeaders);

        WsTelemetryResponse actualLatestTelemetry = wsClient.getLastMessage();
        assertThat(actualLatestTelemetry.getDataValuesByKey(TELEMETRY_KEY).get(1)).isEqualTo(TELEMETRY_VALUE);

        //update integration with created config
        integration.setName("New name");
        integration = testRestClient.postIntegration(integration);
        waitForIntegrationEvent(integration, "UPDATED", 1);

        String temperatureValue = "12";
        remoteHttpClient.postUplinkPayloadForHttpIntegration(integration.getRoutingKey(), createPayloadForUplink(device, temperatureValue), securityHeaders);

        Awaitility
                .await()
                .atMost(10, TimeUnit.SECONDS)
                .until(() -> wsClient.getMessage().getDataValuesByKey(TELEMETRY_KEY).get(1).equals(temperatureValue));

        //update integration with new security header
        JsonNode config2 = defaultConfigWithSecurityHeader2(HTTPS_URL);
        integration.setConfiguration(config2);
        integration = testRestClient.postIntegration(integration);
        waitForIntegrationEvent(integration, "UPDATED", 2);

        String temperatureValue2 = "58";
        Map<String, Object> securityHeaders2 = JacksonUtil.fromString(config2.get("headersFilter").toString(), new TypeReference<>() {});
        remoteHttpClient.postUplinkPayloadForHttpIntegration(integration.getRoutingKey(), createPayloadForUplink(device, temperatureValue2), securityHeaders2);

        Awaitility
                .await()
                .atMost(10, TimeUnit.SECONDS)
                .until(() -> wsClient.getMessage().getDataValuesByKey(TELEMETRY_KEY).get(1).equals(temperatureValue2));

        //update integration with new security disabled
        integration.setConfiguration(defaultConfig(HTTPS_URL));
        integration = testRestClient.postIntegration(integration);
        waitForIntegrationEvent(integration, "UPDATED", 3);

        String temperatureValue3 = "35";
        remoteHttpClient.postUplinkPayloadForHttpIntegration(integration.getRoutingKey(), createPayloadForUplink(device, temperatureValue3));

        Awaitility
                .await()
                .atMost(10, TimeUnit.SECONDS)
                .until(() -> wsClient.getMessage().getDataValuesByKey(TELEMETRY_KEY).get(1).equals(temperatureValue3));
    }

    @Test
    public void checkDownlinkMessageWasSent() throws Exception {
        createIntegration(HTTP, defaultConfig(HTTPS_URL), CUSTOM_CONVERTER_CONFIGURATION, DOWNLINK_CONVERTER_CONFIGURATION, ROUTING_KEY, SECRET_KEY, false);

        testRestClient.postUplinkPayloadForHttpIntegration(integration.getRoutingKey(), createPayloadForUplink(device, TELEMETRY_VALUE));

        //create rule chain
        RuleChainId ruleChainId = createRootRuleChainWithIntegrationDownlinkNode(integration.getId());

        JsonNode attributes = JacksonUtil.toJsonNode(createPayload().toString());
        testRestClient.saveEntityAttributes(DEVICE, device.getId().toString(), SHARED_SCOPE, attributes);

        RuleChainMetaData ruleChainMetadata = testRestClient.getRuleChainMetadata(ruleChainId);
        RuleNode integrationNode = ruleChainMetadata.getNodes().stream().filter(ruleNode -> ruleNode.getType().equals("org.thingsboard.rule.engine.integration.TbIntegrationDownlinkNode")).findFirst().get();
        waitTillRuleNodeReceiveMsg(integrationNode.getId(), EventType.DEBUG_RULE_NODE, integration.getTenantId(), "ATTRIBUTES_UPDATED");

        //check downlink
        String temperatureValue2 = "12";
        JsonPath uplinkResponse2 = testRestClient.postUplinkPayloadForHttpIntegration(integration.getRoutingKey(), createPayloadForUplink(device, temperatureValue2))
                .extract().jsonPath();

        assertThat(uplinkResponse2.getString("booleanKey")).isEqualTo(attributes.get("booleanKey").asText());
        assertThat(uplinkResponse2.getString("stringKey")).isEqualTo(attributes.get("stringKey").asText());
        assertThat(uplinkResponse2.getString("doubleKey")).isEqualTo(attributes.get("doubleKey").asText());
        assertThat(uplinkResponse2.getString("longKey")).isEqualTo(attributes.get("longKey").asText());
    }

    @Override
    protected String getDevicePrototypeSufix() {
        return "http_";
    }

}
