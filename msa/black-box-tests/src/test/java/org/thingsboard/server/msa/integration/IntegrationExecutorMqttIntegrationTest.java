// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.msa.integration;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.awaitility.Awaitility;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.msa.TestProperties;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.thingsboard.server.common.data.integration.IntegrationType.MQTT;
import static org.thingsboard.server.msa.prototypes.MQTTIntegrationPrototypes.defaultConfig;

@Slf4j
public class IntegrationExecutorMqttIntegrationTest extends AbstractIntegrationTest {

    private static final String SERVICE_NAME = "broker";
    private static final int SERVICE_PORT = 1883;
    private static final String TOPIC = "tb/mqtt/device";

    private static final String CONFIG_CONVERTER = """
            var payloadStr = decodeToString(payload);
            var data = JSON.parse(payloadStr);
            var deviceName = 'DEVICE_NAME';
            var deviceType = 'DEFAULT';
            var result = {
               deviceName: deviceName,
               deviceType: deviceType,
               telemetry: {
                   temperature: data.temperature,
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
            return result;""";

    private static final String ASSET_CONVERTER = """
            var payloadStr = decodeToString(payload);
            var data = JSON.parse(payloadStr);
            var result = {
               assetName: data.assetName,
               assetType: data.assetType,
               isAsset: true,
               telemetry: {
                   temperature: data.temperature,
               }
            };

            function decodeToString(payload) {
               return String.fromCharCode.apply(String, payload);
            }
            return result;""";

    private static final String AUTO_CREATE_CONVERTER = """
            var payloadStr = decodeToString(payload);
            var data = JSON.parse(payloadStr);
            var deviceName = data.deviceName;
            var deviceType = data.deviceType;
            var result = {
               deviceName: deviceName,
               deviceType: deviceType,
               telemetry: {
                   temperature: data.temperature,
               }
            };
            
            function decodeToString(payload) {
               return String.fromCharCode.apply(String, payload);
            }
            return result;""";

    private JsonNode configConverter;

    @BeforeMethod
    public void setUp() {
        configConverter = JacksonUtil.newObjectNode().put("decoder", CONFIG_CONVERTER.replaceAll("DEVICE_NAME", device.getName()));
    }

    @Test
    public void testTelemetryUploadViaIntegrationExecutor() throws Exception {
        String routingKey = "ie-mqtt-routing-" + RandomStringUtils.secure().nextAlphanumeric(10);
        String secretKey = "ie-mqtt-secret-" + RandomStringUtils.secure().nextAlphanumeric(10);

        createIntegration(MQTT, defaultConfig(SERVICE_NAME, SERVICE_PORT, TOPIC), configConverter, routingKey, secretKey, false);

        publishToBroker(createPayloadForUplink().toString().getBytes());

        Awaitility
                .await()
                .alias("Wait for telemetry via MQTT integration executor")
                .atMost(30, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .until(() -> !testRestClient.getTimeseriesKeys(device.getId()).isEmpty());

        List<TsKvEntry> latestTimeseries = testRestClient.getLatestTimeseries(device.getId(), List.of(TELEMETRY_KEY));
        Assert.assertFalse(latestTimeseries.isEmpty());
        Assert.assertEquals(latestTimeseries.getFirst().getKey(), TELEMETRY_KEY);
        Assert.assertEquals(latestTimeseries.getFirst().getValue().toString(), TELEMETRY_VALUE);
    }

    @Test
    public void testIntegrationUpdateOnExecutor() throws Exception {
        String routingKey = "ie-mqtt-routing-" + RandomStringUtils.secure().nextAlphanumeric(10);
        String secretKey = "ie-mqtt-secret-" + RandomStringUtils.secure().nextAlphanumeric(10);

        createIntegration(MQTT, defaultConfig(SERVICE_NAME, SERVICE_PORT, TOPIC), configConverter, routingKey, secretKey, false);

        publishToBroker(createPayloadForUplink().toString().getBytes());

        Awaitility
                .await()
                .alias("Wait for telemetry via MQTT integration executor")
                .atMost(30, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .until(() -> !testRestClient.getTimeseriesKeys(device.getId()).isEmpty());

        List<TsKvEntry> latestTimeseries = testRestClient.getLatestTimeseries(device.getId(), List.of(TELEMETRY_KEY));
        Assert.assertEquals(latestTimeseries.getFirst().getValue().toString(), TELEMETRY_VALUE);

        integration.setName("updated_" + integration.getName());
        integration = testRestClient.postIntegration(integration);
        waitForIntegrationEvent(integration, "UPDATED", 1);

        String temperatureValue2 = "58";
        publishToBroker(("{\"temperature\":\"" + temperatureValue2 + "\"}").getBytes());

        Awaitility
                .await()
                .alias("Wait for updated telemetry")
                .atMost(30, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .until(() -> {
                    List<TsKvEntry> ts = testRestClient.getLatestTimeseries(device.getId(), List.of(TELEMETRY_KEY));
                    return !ts.isEmpty() && temperatureValue2.equals(ts.getFirst().getValue().toString());
                });
    }

    @Test
    public void testDeviceAutoCreation() throws Exception {
        String routingKey = "ie-mqtt-routing-" + RandomStringUtils.secure().nextAlphanumeric(10);
        String secretKey = "ie-mqtt-secret-" + RandomStringUtils.secure().nextAlphanumeric(10);

        JsonNode autoCreateConverterConfig = JacksonUtil.newObjectNode().put("decoder", AUTO_CREATE_CONVERTER);
        createIntegration(MQTT, defaultConfig(SERVICE_NAME, SERVICE_PORT, TOPIC), autoCreateConverterConfig, routingKey, secretKey, false);

        String newDeviceName = "auto_mqtt_" + RandomStringUtils.secure().nextAlphanumeric(7);
        String payload = "{\"deviceName\":\"" + newDeviceName + "\",\"deviceType\":\"DEFAULT\",\"temperature\":\"" + TELEMETRY_VALUE + "\"}";
        publishToBroker(payload.getBytes());

        Awaitility
                .await()
                .alias("Wait for auto-created device via MQTT")
                .atMost(30, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .until(() -> {
                    PageData<Device> devices = testRestClient.getDevices(new PageLink(100, 0, newDeviceName));
                    return !devices.getData().isEmpty();
                });

        Device autoCreatedDevice = testRestClient.getDevices(new PageLink(100, 0, newDeviceName)).getData().getFirst();
        try {
            Awaitility
                    .await()
                    .alias("Wait for telemetry on auto-created device")
                    .atMost(10, TimeUnit.SECONDS)
                    .pollInterval(500, TimeUnit.MILLISECONDS)
                    .until(() -> {
                        List<TsKvEntry> ts = testRestClient.getLatestTimeseries(autoCreatedDevice.getId(), List.of(TELEMETRY_KEY));
                        return !ts.isEmpty() && TELEMETRY_VALUE.equals(ts.getFirst().getValue().toString());
                    });
        } finally {
            testRestClient.deleteDevice(autoCreatedDevice.getId());
        }
    }

    @Test
    public void testAssetAutoCreationViaIntegrationExecutor() throws Exception {
        String routingKey = "ie-mqtt-routing-" + RandomStringUtils.secure().nextAlphanumeric(10);
        String secretKey = "ie-mqtt-secret-" + RandomStringUtils.secure().nextAlphanumeric(10);

        JsonNode assetConverterConfig = JacksonUtil.newObjectNode().put("decoder", ASSET_CONVERTER);
        createIntegration(MQTT, defaultConfig(SERVICE_NAME, SERVICE_PORT, TOPIC), assetConverterConfig, routingKey, secretKey, false);

        String assetName = "auto_mqtt_asset_" + RandomStringUtils.secure().nextAlphanumeric(7);
        String payload = "{\"assetName\":\"" + assetName + "\",\"assetType\":\"default\",\"temperature\":\"" + TELEMETRY_VALUE + "\"}";
        publishToBroker(payload.getBytes());

        Awaitility
                .await()
                .alias("Wait for auto-created asset via MQTT")
                .atMost(30, TimeUnit.SECONDS)
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

    private void publishToBroker(byte[] payload) throws Exception {
        MqttConnectOptions connOpts = new MqttConnectOptions();
        connOpts.setCleanSession(true);

        MqttClient client = new MqttClient(TestProperties.getMqttBrokerUrl(), StringUtils.randomAlphanumeric(10), new MemoryPersistence());
        client.connect(connOpts);
        MqttMessage message = new MqttMessage(payload);
        message.setQos(0);
        client.publish(TOPIC, message);
        client.disconnect();
        client.close();
    }

    @Override
    protected String getDevicePrototypeSufix() {
        return "ie_mqtt_";
    }

}
