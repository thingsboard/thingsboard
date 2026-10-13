// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.msa.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.msa.WsClient;
import org.thingsboard.server.msa.mapper.WsTelemetryResponse;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.thingsboard.server.common.data.DataConstants.CLIENT_SCOPE;
import static org.thingsboard.server.common.data.integration.IntegrationType.CHIRPSTACK;

@Slf4j
public class ChirpStackIntegrationTest extends AbstractIntegrationTest {

    private static final String ROUTING_KEY = "routing-key-chirpstack";
    private static final String SECRET_KEY = "secret-key-chirpstack";

    private WsClient wsClient;

    @AfterMethod
    public void tearDown() throws Exception {
        if (wsClient != null) {
            wsClient.closeBlocking();
        }
    }

    @Test
    public void checkTelemetryUploadedWithLocalIntegration() throws Exception {
        JsonNode configConverter = JacksonUtil.toJsonNode(JSON_CONVERTER_CONFIG.replaceAll("DEVICE_NAME", device.getName()));
        JsonNode integrationConfig = createChirpStackJsonConfig();
        createIntegration(CHIRPSTACK, integrationConfig, configConverter, null, ROUTING_KEY, SECRET_KEY, false, 2);

        wsClient = subscribeToWebSocket(device.getId(), "LATEST_TELEMETRY", CmdsType.TS_SUB_CMDS);

        ObjectNode payloadMsg = createPayloadMsg();

        testRestClient.postUplinkPayloadForHttpBasedIntegration(integration.getRoutingKey(), payloadMsg, CHIRPSTACK);

        WsTelemetryResponse actualLatestTelemetry = wsClient.getLastMessage();
        assertThat(actualLatestTelemetry.getDataValuesByKey("data").get(1)).isEqualTo("Kj8=");
        assertThat(actualLatestTelemetry.getDataValuesByKey("temperature").get(1)).isEqualTo("42");
        assertThat(actualLatestTelemetry.getDataValuesByKey("humidity").get(1)).isEqualTo("63");
        assertThat(actualLatestTelemetry.getDataValuesByKey("snr").get(1)).isEqualTo("11.5");
    }

    @Test
    public void checkAttributesUploadedWithLocalIntegration() {
        JsonNode configConverter = JacksonUtil.toJsonNode(JSON_CONVERTER_CONFIG.replaceAll("DEVICE_NAME", device.getName()));
        JsonNode integrationConfig = createChirpStackJsonConfig();
        createIntegration(CHIRPSTACK, integrationConfig, configConverter, null, ROUTING_KEY, SECRET_KEY, false, 2);

        ObjectNode payloadMsg = createPayloadMsg();

        testRestClient.postUplinkPayloadForHttpBasedIntegration(integration.getRoutingKey(), payloadMsg, CHIRPSTACK);

        await()
                .pollInterval(1, TimeUnit.SECONDS)
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    List<JsonNode> attributes = testRestClient.getEntityAttributeByScopeAndKey(device.getId(), CLIENT_SCOPE, "rssi,eui,fPort");
                    Map<String, JsonNode> attributeMap = attributes.stream()
                            .collect(Collectors.toMap(
                                    node -> node.get("key").asText(),
                                    node -> node
                            ));

                    assertThat(attributeMap.get("rssi").get("value").asInt()).isEqualTo(-130);
                    assertThat(attributeMap.get("eui").get("value").asText()).isEqualTo("BE7A123456789");
                    assertThat(attributeMap.get("fPort").get("value").asInt()).isEqualTo(80);
                });
    }

    @Override
    protected String getDevicePrototypeSufix() {
        return "chirpstack_";
    }

    private ObjectNode createPayloadMsg() {
        ObjectNode payloadMsg = JacksonUtil.newObjectNode();

        String isoTime = OffsetDateTime.now(ZoneOffset.UTC)
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        payloadMsg.put("time", isoTime);
        payloadMsg.put("data", "Kj8=");

        ObjectNode deviceInfo = payloadMsg.putObject("deviceInfo");
        deviceInfo.put("devEui", "BE7A123456789");
        payloadMsg.put("fPort", 80);

        ArrayNode rxInfo = payloadMsg.putArray("rxInfo");
        ObjectNode rxInfoEntry = rxInfo.addObject();
        rxInfoEntry.put("rssi", -130);
        rxInfoEntry.put("snr", 11.5);

        return payloadMsg;
    }

    private JsonNode createChirpStackJsonConfig() {
        ObjectNode rootNode = JacksonUtil.newObjectNode();
        rootNode.putObject("metadata");

        ObjectNode clientConfigNode = rootNode.putObject("clientConfiguration");
        clientConfigNode.put("baseUrl", HTTPS_URL);
        clientConfigNode.put("applicationServerUrl", "test_server_url");
        clientConfigNode.put("applicationServerApiToken", "test_api_token");

        return rootNode;
    }

}
