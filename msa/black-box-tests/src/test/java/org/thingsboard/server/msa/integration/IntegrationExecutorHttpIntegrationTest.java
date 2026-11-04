// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.msa.integration;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.awaitility.Awaitility;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.msa.WsClient;
import org.thingsboard.server.msa.mapper.WsTelemetryResponse;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.thingsboard.server.common.data.integration.IntegrationType.HTTP;
import static org.thingsboard.server.msa.prototypes.HttpIntegrationConfigPrototypes.defaultConfig;

@Slf4j
public class IntegrationExecutorHttpIntegrationTest extends AbstractIntegrationTest {

    private static final String EXECUTOR_BASE_URL = "https://haproxy";

    private final JsonNode CUSTOM_CONVERTER_CONFIGURATION = JacksonUtil
            .newObjectNode().put("decoder", """
                    var data = decodeToJson(payload);
                    var deviceName = data.deviceName;
                    var deviceType = data.deviceType;
                    var result = {
                       deviceName: deviceName,
                       deviceType: deviceType,
                       attributes: {
                           model: data.model
                       },
                       telemetry: {
                           temperature: data.temperature
                       }
                    };
                    function decodeToString(payload) {
                       return String.fromCharCode.apply(String, payload);
                    }
                    function decodeToJson(payload) {
                       var str = decodeToString(payload);
                       var data = JSON.parse(str);
                       return data;
                    }
                    return result;""");

    private final JsonNode ASSET_CONVERTER_CONFIGURATION = JacksonUtil
            .newObjectNode().put("decoder", """
                    var data = decodeToJson(payload);
                    var result = {
                       assetName: data.assetName,
                       assetType: data.assetType,
                       isAsset: true,
                       telemetry: {
                           temperature: data.temperature
                       }
                    };
                    function decodeToString(payload) {
                       return String.fromCharCode.apply(String, payload);
                    }
                    function decodeToJson(payload) {
                       var str = decodeToString(payload);
                       var data = JSON.parse(str);
                       return data;
                    }
                    return result;""");

    private WsClient wsClient;

    @AfterMethod
    public void tearDown() throws Exception {
        if (wsClient != null) {
            wsClient.closeBlocking();
        }
    }

    @Test
    public void testTelemetryUploadViaIntegrationExecutor() throws Exception {
        String routingKey = "ie-routing-" + RandomStringUtils.secure().nextAlphanumeric(10);
        String secretKey = "ie-secret-" + RandomStringUtils.secure().nextAlphanumeric(10);

        createIntegration(HTTP, defaultConfig(EXECUTOR_BASE_URL), CUSTOM_CONVERTER_CONFIGURATION, routingKey, secretKey, false);

        wsClient = subscribeToWebSocket(device.getId(), "LATEST_TELEMETRY", CmdsType.TS_SUB_CMDS);
        integrationExecutorHttpClient.postUplinkPayloadForHttpIntegration(integration.getRoutingKey(), createPayloadForUplink(device, TELEMETRY_VALUE));

        WsTelemetryResponse actualLatestTelemetry = wsClient.getLastMessage();
        assertThat(actualLatestTelemetry.getDataValuesByKey(TELEMETRY_KEY).get(1)).isEqualTo(TELEMETRY_VALUE);
    }

    @Test
    public void testDeviceAutoCreation() throws Exception {
        String routingKey = "ie-routing-" + RandomStringUtils.secure().nextAlphanumeric(10);
        String secretKey = "ie-secret-" + RandomStringUtils.secure().nextAlphanumeric(10);

        createIntegration(HTTP, defaultConfig(EXECUTOR_BASE_URL), CUSTOM_CONVERTER_CONFIGURATION, routingKey, secretKey, false);

        String newDeviceName = "auto_created_" + RandomStringUtils.secure().nextAlphanumeric(7);
        Device tempDevice = new Device();
        tempDevice.setName(newDeviceName);
        tempDevice.setType("DEFAULT");

        JsonNode uplinkPayload = createPayloadForUplink(tempDevice, TELEMETRY_VALUE);
        integrationExecutorHttpClient.postUplinkPayloadForHttpIntegration(integration.getRoutingKey(), uplinkPayload);

        Awaitility
                .await()
                .alias("Wait for auto-created device")
                .atMost(20, TimeUnit.SECONDS)
                .until(() -> {
                    PageData<Device> devices = testRestClient.getDevices(new PageLink(100, 0, newDeviceName));
                    return !devices.getData().isEmpty();
                });

        Device autoCreatedDevice = testRestClient.getDevices(new PageLink(100, 0, newDeviceName)).getData().getFirst();
        try {
            wsClient = subscribeToWebSocket(autoCreatedDevice.getId(), "LATEST_TELEMETRY", CmdsType.TS_SUB_CMDS);
            String temperatureValue2 = "58";
            integrationExecutorHttpClient.postUplinkPayloadForHttpIntegration(integration.getRoutingKey(), createPayloadForUplink(autoCreatedDevice, temperatureValue2));

            Awaitility
                    .await()
                    .atMost(10, TimeUnit.SECONDS)
                    .until(() -> {
                        WsTelemetryResponse msg = wsClient.getMessage();
                        return msg != null && temperatureValue2.equals(msg.getDataValuesByKey(TELEMETRY_KEY).get(1));
                    });
        } finally {
            testRestClient.deleteDevice(autoCreatedDevice.getId());
        }
    }

    @Test
    public void testIntegrationUpdateOnExecutor() throws Exception {
        String routingKey = "ie-routing-" + RandomStringUtils.secure().nextAlphanumeric(10);
        String secretKey = "ie-secret-" + RandomStringUtils.secure().nextAlphanumeric(10);

        createIntegration(HTTP, defaultConfig(EXECUTOR_BASE_URL), CUSTOM_CONVERTER_CONFIGURATION, routingKey, secretKey, false);

        wsClient = subscribeToWebSocket(device.getId(), "LATEST_TELEMETRY", CmdsType.TS_SUB_CMDS);
        integrationExecutorHttpClient.postUplinkPayloadForHttpIntegration(integration.getRoutingKey(), createPayloadForUplink(device, TELEMETRY_VALUE));

        WsTelemetryResponse actualLatestTelemetry = wsClient.getLastMessage();
        assertThat(actualLatestTelemetry.getDataValuesByKey(TELEMETRY_KEY).get(1)).isEqualTo(TELEMETRY_VALUE);

        integration.setName("updated_" + integration.getName());
        integration = testRestClient.postIntegration(integration);
        waitForIntegrationEvent(integration, "UPDATED", 1);

        String temperatureValue2 = "58";
        integrationExecutorHttpClient.postUplinkPayloadForHttpIntegration(integration.getRoutingKey(), createPayloadForUplink(device, temperatureValue2));

        Awaitility
                .await()
                .atMost(10, TimeUnit.SECONDS)
                .until(() -> wsClient.getMessage().getDataValuesByKey(TELEMETRY_KEY).get(1).equals(temperatureValue2));
    }

    @Test
    public void testAssetAutoCreationViaIntegrationExecutor() throws Exception {
        String routingKey = "ie-routing-" + RandomStringUtils.secure().nextAlphanumeric(10);
        String secretKey = "ie-secret-" + RandomStringUtils.secure().nextAlphanumeric(10);

        createIntegration(HTTP, defaultConfig(EXECUTOR_BASE_URL), ASSET_CONVERTER_CONFIGURATION, routingKey, secretKey, false);

        String assetName = "auto_asset_" + RandomStringUtils.secure().nextAlphanumeric(7);
        String payload = "{\"assetName\":\"" + assetName + "\",\"assetType\":\"default\",\"temperature\":\"" + TELEMETRY_VALUE + "\"}";
        integrationExecutorHttpClient.postUplinkPayloadForHttpIntegration(integration.getRoutingKey(), JacksonUtil.toJsonNode(payload));

        Awaitility
                .await()
                .alias("Wait for auto-created asset")
                .atMost(20, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .until(() -> {
                    PageData<Asset> assets = testRestClient.getTenantAssets(new PageLink(100, 0, assetName));
                    return !assets.getData().isEmpty();
                });

        Asset autoCreatedAsset = testRestClient.getTenantAssets(new PageLink(100, 0, assetName)).getData().getFirst();
        try {
            Awaitility
                    .await()
                    .alias("Wait for telemetry on auto-created asset")
                    .atMost(10, TimeUnit.SECONDS)
                    .pollInterval(500, TimeUnit.MILLISECONDS)
                    .until(() -> {
                        List<TsKvEntry> ts = testRestClient.getLatestTimeseries(autoCreatedAsset.getId(), List.of(TELEMETRY_KEY));
                        return !ts.isEmpty() && TELEMETRY_VALUE.equals(ts.getFirst().getValue().toString());
                    });
        } finally {
            testRestClient.deleteAsset(autoCreatedAsset.getId());
        }
    }

    @Override
    protected String getDevicePrototypeSufix() {
        return "ie_http_";
    }

}
