/**
 * Copyright © 2016-2020 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.mqtt.telemetry.timeseries;

import io.netty.handler.codec.mqtt.MqttQoS;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.TransportPayloadType;
import org.thingsboard.server.common.data.device.profile.MqttTopics;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.mqtt.AbstractMqttIntegrationTest;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
public abstract class AbstractMqttTimeseriesIntegrationTest extends AbstractMqttIntegrationTest {

    protected static final String PAYLOAD_VALUES_STR = "{\"key1\":\"value1\", \"key2\":true, \"key3\": 3.0, \"key4\": 4," +
            " \"key5\": {\"someNumber\": 42, \"someArray\": [1,2,3], \"someNestedObject\": {\"key\": \"value\"}}}";

    @Before
    public void beforeTest() throws Exception {
        processBeforeTest("Test Post Telemetry device", "Test Post Telemetry gateway", null, null, null);
    }

    @After
    public void afterTest() throws Exception {
        processAfterTest();
    }

    @Test
    public void testPushMqttTelemetry() throws Exception {
        List<String> expectedKeys = Arrays.asList("key1", "key2", "key3", "key4", "key5");
        processTelemetryTest(MqttTopics.DEVICE_TELEMETRY_TOPIC, expectedKeys, PAYLOAD_VALUES_STR.getBytes(), false);
    }

    @Test
    public void testPushMqttTelemetryWithTs() throws Exception {
        String payloadStr = "{\"ts\": 10000, \"values\": " + PAYLOAD_VALUES_STR + "}";
        List<String> expectedKeys = Arrays.asList("key1", "key2", "key3", "key4", "key5");
        processTelemetryTest(MqttTopics.DEVICE_TELEMETRY_TOPIC, expectedKeys, payloadStr.getBytes(), true);
    }

    @Test
    public void testPushMqttTelemetryGateway() throws Exception {
        List<String> expectedKeys = Arrays.asList("key1", "key2", "key3", "key4", "key5");
        String deviceName1 = "Device A";
        String deviceName2 = "Device B";
        String payload = getGatewayTelemetryJsonPayload(deviceName1, deviceName2, "10000", "20000");
        processGatewayTelemetryTest(MqttTopics.GATEWAY_TELEMETRY_TOPIC, expectedKeys, payload.getBytes(), deviceName1, deviceName2);
    }

    @Test
    public void testGatewayConnect() throws Exception {
        String payload = "{\"device\":\"Device A\"}";
        MqttAsyncClient client = getMqttAsyncClient(gatewayAccessToken);
        publishMqttMsg(client, payload.getBytes(), MqttTopics.GATEWAY_CONNECT_TOPIC);

        String deviceName = "Device A";

        Device device = doExecuteWithRetriesAndInterval(() -> doGet("/api/tenant/devices?deviceName=" + deviceName, Device.class),
            20,
        100);

        assertNotNull(device);
    }

    protected void processTelemetryTest(String topic, List<String> expectedKeys, byte[] payload, boolean withTs) throws Exception {
        MqttAsyncClient client = getMqttAsyncClient(accessToken);
        publishMqttMsg(client, payload, topic);

        String deviceId = savedDevice.getId().getId().toString();

        long start = System.currentTimeMillis();
        long end = System.currentTimeMillis() + 2000;

        List<String> actualKeys = null;
        while (start <= end) {
            actualKeys = doGetAsync("/api/plugins/telemetry/DEVICE/" + deviceId + "/keys/timeseries", List.class);
            if (actualKeys.size() == expectedKeys.size()) {
                break;
            }
            Thread.sleep(100);
            start += 100;
        }
        assertNotNull(actualKeys);

        Set<String> actualKeySet = new HashSet<>(actualKeys);
        Set<String> expectedKeySet = new HashSet<>(expectedKeys);

        assertEquals(expectedKeySet, actualKeySet);

        String getTelemetryValuesUrl;
        if (withTs) {
            getTelemetryValuesUrl = "/api/plugins/telemetry/DEVICE/" + deviceId + "/values/timeseries?startTs=0&endTs=15000&keys=" + String.join(",", actualKeySet);
        } else {
            getTelemetryValuesUrl = "/api/plugins/telemetry/DEVICE/" + deviceId + "/values/timeseries?keys=" + String.join(",", actualKeySet);
        }
        Map<String, List<Map<String, String>>> values = doGetAsync(getTelemetryValuesUrl, Map.class);

        if (withTs) {
            assertTs(values, expectedKeys, 10000, 0);
        }
        assertValues(values, 0);
    }

    protected void processGatewayTelemetryTest(String topic, List<String> expectedKeys, byte[] payload, String firstDeviceName, String secondDeviceName) throws Exception {
        MqttAsyncClient client = getMqttAsyncClient(gatewayAccessToken);

        publishMqttMsg(client, payload, topic);

        Device firstDevice = doExecuteWithRetriesAndInterval(() -> doGet("/api/tenant/devices?deviceName=" + firstDeviceName, Device.class),
                20,
                100);

        assertNotNull(firstDevice);

        Device secondDevice = doExecuteWithRetriesAndInterval(() -> doGet("/api/tenant/devices?deviceName=" + secondDeviceName, Device.class),
                20,
                100);

        assertNotNull(secondDevice);

        Thread.sleep(2000);

        List<String> firstDeviceActualKeys = doGetAsync("/api/plugins/telemetry/DEVICE/" + firstDevice.getId() + "/keys/timeseries", List.class);
        Set<String> firstDeviceActualKeySet = new HashSet<>(firstDeviceActualKeys);

        List<String> secondDeviceActualKeys = doGetAsync("/api/plugins/telemetry/DEVICE/" + secondDevice.getId() + "/keys/timeseries", List.class);
        Set<String> secondDeviceActualKeySet = new HashSet<>(secondDeviceActualKeys);

        Set<String> expectedKeySet = new HashSet<>(expectedKeys);

        assertEquals(expectedKeySet, firstDeviceActualKeySet);
        assertEquals(expectedKeySet, secondDeviceActualKeySet);

        String getTelemetryValuesUrlFirstDevice = getTelemetryValuesUrl(firstDevice.getId(), firstDeviceActualKeySet);
        String getTelemetryValuesUrlSecondDevice = getTelemetryValuesUrl(firstDevice.getId(), secondDeviceActualKeySet);

        Map<String, List<Map<String, String>>> firstDeviceValues = doGetAsync(getTelemetryValuesUrlFirstDevice, Map.class);
        Map<String, List<Map<String, String>>> secondDeviceValues = doGetAsync(getTelemetryValuesUrlSecondDevice, Map.class);

        assertGatewayDeviceData(firstDeviceValues, expectedKeys);
        assertGatewayDeviceData(secondDeviceValues, expectedKeys);
    }

    protected String getGatewayTelemetryJsonPayload(String deviceA, String deviceB, String firstTsValue, String secondTsValue) {
        String payload = "[{\"ts\": " + firstTsValue + ", \"values\": " + PAYLOAD_VALUES_STR + "}, " +
                "{\"ts\": " + secondTsValue + ", \"values\": " + PAYLOAD_VALUES_STR + "}]";
        return "{\"" + deviceA + "\": " + payload + ",  \"" + deviceB + "\": " + payload + "}";
    }

    private String getTelemetryValuesUrl(DeviceId deviceId, Set<String> actualKeySet) {
        return "/api/plugins/telemetry/DEVICE/" + deviceId + "/values/timeseries?startTs=0&endTs=25000&keys=" + String.join(",", actualKeySet);
    }

    private void assertGatewayDeviceData(Map<String, List<Map<String, String>>> deviceValues, List<String> expectedKeys) {

        assertEquals(2, deviceValues.get(expectedKeys.get(0)).size());
        assertEquals(2, deviceValues.get(expectedKeys.get(1)).size());
        assertEquals(2, deviceValues.get(expectedKeys.get(2)).size());
        assertEquals(2, deviceValues.get(expectedKeys.get(3)).size());
        assertEquals(2, deviceValues.get(expectedKeys.get(4)).size());

        assertTs(deviceValues, expectedKeys, 20000, 0);
        assertTs(deviceValues, expectedKeys, 10000, 1);

        assertValues(deviceValues, 0);
        assertValues(deviceValues, 1);

    }

    private void assertValues(Map<String, List<Map<String, String>>> deviceValues, int arrayIndex) {
        for (Map.Entry<String, List<Map<String, String>>> entry : deviceValues.entrySet()) {
            String key = entry.getKey();
            List<Map<String, String>> tsKv = entry.getValue();
            String value = tsKv.get(arrayIndex).get("value");
            switch (key) {
                case "key1":
                    assertEquals("value1", value);
                    break;
                case "key2":
                    assertEquals("true", value);
                    break;
                case "key3":
                    assertEquals("3.0", value);
                    break;
                case "key4":
                    assertEquals("4", value);
                    break;
                case "key5":
                    assertEquals("{\"someNumber\":42,\"someArray\":[1,2,3],\"someNestedObject\":{\"key\":\"value\"}}", value);
                    break;
            }
        }
    }

    private void assertTs(Map<String, List<Map<String, String>>> deviceValues, List<String> expectedKeys, int ts, int arrayIndex) {
        assertEquals(ts, deviceValues.get(expectedKeys.get(0)).get(arrayIndex).get("ts"));
        assertEquals(ts, deviceValues.get(expectedKeys.get(1)).get(arrayIndex).get("ts"));
        assertEquals(ts, deviceValues.get(expectedKeys.get(2)).get(arrayIndex).get("ts"));
        assertEquals(ts, deviceValues.get(expectedKeys.get(3)).get(arrayIndex).get("ts"));
        assertEquals(ts, deviceValues.get(expectedKeys.get(4)).get(arrayIndex).get("ts"));
    }

    //    @Test - Unstable
    public void testMqttQoSLevel() throws Exception {
        String clientId = MqttAsyncClient.generateClientId();
        MqttAsyncClient client = new MqttAsyncClient(MQTT_URL, clientId, new MemoryPersistence());

        MqttConnectOptions options = new MqttConnectOptions();
        options.setUserName(accessToken);
        CountDownLatch latch = new CountDownLatch(1);
        TestMqttCallback callback = new TestMqttCallback(client, latch);
        client.setCallback(callback);
        client.connect(options).waitForCompletion(5000);
        client.subscribe("v1/devices/me/attributes", MqttQoS.AT_MOST_ONCE.value());
        String payload = "{\"key\":\"uniqueValue\"}";
//        TODO 3.1: we need to acknowledge subscription only after it is processed by device actor and not when the message is pushed to queue.
//        MqttClient -> SUB REQUEST -> Transport -> Kafka -> Device Actor (subscribed)
//        MqttClient <- SUB_ACK <- Transport
        Thread.sleep(5000);
        doPostAsync("/api/plugins/telemetry/" + savedDevice.getId() + "/SHARED_SCOPE", payload, String.class, status().isOk());
        latch.await(10, TimeUnit.SECONDS);
        assertEquals(payload, callback.getPayload());
        assertEquals(MqttQoS.AT_MOST_ONCE.value(), callback.getQoS());
    }

    private static class TestMqttCallback implements MqttCallback {

        private final MqttAsyncClient client;
        private final CountDownLatch latch;
        private volatile Integer qoS;
        private volatile String payload;

        String getPayload() {
            return payload;
        }

        TestMqttCallback(MqttAsyncClient client, CountDownLatch latch) {
            this.client = client;
            this.latch = latch;
        }

        int getQoS() {
            return qoS;
        }

        @Override
        public void connectionLost(Throwable throwable) {
            log.error("Client connection lost", throwable);
        }

        @Override
        public void messageArrived(String requestTopic, MqttMessage mqttMessage) {
            payload = new String(mqttMessage.getPayload());
            qoS = mqttMessage.getQos();
            latch.countDown();
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

        }
    }


}
