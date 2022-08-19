/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.server.transport.mqtt.telemetry.timeseries;

import com.fasterxml.jackson.core.type.TypeReference;
import io.netty.handler.codec.mqtt.MqttQoS;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.transport.mqtt.AbstractMqttIntegrationTest;
import org.thingsboard.server.transport.mqtt.MqttTestCallback;
import org.thingsboard.server.transport.mqtt.MqttTestClient;
import org.thingsboard.server.transport.mqtt.MqttTestConfigProperties;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.common.data.device.profile.MqttTopics.DEVICE_ATTRIBUTES_TOPIC;
import static org.thingsboard.server.common.data.device.profile.MqttTopics.DEVICE_TELEMETRY_SHORT_JSON_TOPIC;
import static org.thingsboard.server.common.data.device.profile.MqttTopics.DEVICE_TELEMETRY_SHORT_TOPIC;
import static org.thingsboard.server.common.data.device.profile.MqttTopics.DEVICE_TELEMETRY_TOPIC;
import static org.thingsboard.server.common.data.device.profile.MqttTopics.GATEWAY_CONNECT_TOPIC;
import static org.thingsboard.server.common.data.device.profile.MqttTopics.GATEWAY_TELEMETRY_TOPIC;

@Slf4j
public abstract class AbstractMqttTimeseriesIntegrationTest extends AbstractMqttIntegrationTest {

    protected static final String PAYLOAD_VALUES_STR = "{\"key1\":\"value1\", \"key2\":true, \"key3\": 3.0, \"key4\": 4," +
            " \"key5\": {\"someNumber\": 42, \"someArray\": [1,2,3], \"someNestedObject\": {\"key\": \"value\"}}}";

    protected static final String MALFORMED_JSON_PAYLOAD = "{\"key1\":, \"key2\":true, \"key3\": 3.0, \"key4\": 4," +
            " \"key5\": {\"someNumber\": 42, \"someArray\": [1,2,3], \"someNestedObject\": {\"key\": \"value\"}}}";

    @Before
    public void beforeTest() throws Exception {
        MqttTestConfigProperties configProperties = MqttTestConfigProperties.builder()
                .deviceName("Test Post Telemetry device")
                .gatewayName("Test Post Telemetry gateway")
                .build();
        processBeforeTest(configProperties);
    }

    @Test
    public void testPushTelemetry() throws Exception {
        List<String> expectedKeys = Arrays.asList("key1", "key2", "key3", "key4", "key5");
        processJsonPayloadTelemetryTest(DEVICE_TELEMETRY_TOPIC, expectedKeys, PAYLOAD_VALUES_STR.getBytes(), false);
    }

    @Test
    public void testPushTelemetryWithTs() throws Exception {
        String payloadStr = "{\"ts\": 10000, \"values\": " + PAYLOAD_VALUES_STR + "}";
        List<String> expectedKeys = Arrays.asList("key1", "key2", "key3", "key4", "key5");
        processJsonPayloadTelemetryTest(DEVICE_TELEMETRY_TOPIC, expectedKeys, payloadStr.getBytes(), true);
    }

    @Test
    public void testPushTelemetryOnShortTopic() throws Exception {
        List<String> expectedKeys = Arrays.asList("key1", "key2", "key3", "key4", "key5");
        processJsonPayloadTelemetryTest(DEVICE_TELEMETRY_SHORT_TOPIC, expectedKeys, PAYLOAD_VALUES_STR.getBytes(), false);
    }

    @Test
    public void testPushTelemetryOnShortJsonTopic() throws Exception {
        List<String> expectedKeys = Arrays.asList("key1", "key2", "key3", "key4", "key5");
        processJsonPayloadTelemetryTest(DEVICE_TELEMETRY_SHORT_JSON_TOPIC, expectedKeys, PAYLOAD_VALUES_STR.getBytes(), false);
    }

    @Test
    public void testPushTelemetryGateway() throws Exception {
        List<String> expectedKeys = Arrays.asList("key1", "key2", "key3", "key4", "key5");
        String deviceName1 = "Device A";
        String deviceName2 = "Device B";
        String payload = getGatewayTelemetryJsonPayload(deviceName1, deviceName2, "10000", "20000");
        processGatewayTelemetryTest(GATEWAY_TELEMETRY_TOPIC, expectedKeys, payload.getBytes(), deviceName1, deviceName2);
    }

    @Test
    public void testGatewayConnect() throws Exception {
        String payload = "{\"device\":\"Device A\"}";
        MqttTestClient client = new MqttTestClient();
        client.connectAndWait(gatewayAccessToken);
        client.publish(GATEWAY_CONNECT_TOPIC, payload.getBytes());

        String deviceName = "Device A";

        Device device = doExecuteWithRetriesAndInterval(() -> doGet("/api/tenant/devices?deviceName=" + deviceName, Device.class),
            20,
        100);

        assertNotNull(device);
        client.disconnect();
    }

    protected void processJsonPayloadTelemetryTest(String topic, List<String> expectedKeys, byte[] payload, boolean withTs) throws Exception {
        processTelemetryTest(topic, expectedKeys, payload, withTs, false);
    }

    protected void processTelemetryTest(String topic, List<String> expectedKeys, byte[] payload, boolean withTs, boolean presenceFieldsTest) throws Exception {
        MqttTestClient client = new MqttTestClient();
        client.connectAndWait(accessToken);
        client.publishAndWait(topic, payload);
        client.disconnect();

        DeviceId deviceId = savedDevice.getId();

        List<String> actualKeys = getActualKeysList(deviceId, expectedKeys);
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
        long start = System.currentTimeMillis();
        long end = System.currentTimeMillis() + 5000;
        Map<String, List<Map<String, Object>>> values = null;
        while (start <= end) {
            values = doGetAsyncTyped(getTelemetryValuesUrl, new TypeReference<>() {});
            boolean valid = values.size() == expectedKeys.size();
            if (valid) {
                for (String key : expectedKeys) {
                    List<Map<String, Object>> tsValues = values.get(key);
                    if (tsValues != null && tsValues.size() > 0) {
                        Object ts = tsValues.get(0).get("ts");
                        if (ts == null) {
                            valid = false;
                            break;
                        }
                    } else {
                        valid = false;
                        break;
                    }
                }
            }
            if (valid) {
                break;
            }
            Thread.sleep(100);
            start += 100;
        }
        assertNotNull(values);

        if (withTs) {
            assertTs(values, expectedKeys, 10000, 0);
        }
        if (presenceFieldsTest) {
            assertExplicitProtoFieldValues(values);
        } else {
            assertValues(values, 0);
        }
    }

    protected void processGatewayTelemetryTest(String topic, List<String> expectedKeys, byte[] payload, String firstDeviceName, String secondDeviceName) throws Exception {
        MqttTestClient client = new MqttTestClient();
        client.connectAndWait(gatewayAccessToken);
        client.publishAndWait(topic, payload);
        client.disconnect();

        Device firstDevice = doExecuteWithRetriesAndInterval(() -> doGet("/api/tenant/devices?deviceName=" + firstDeviceName, Device.class),
                20,
                100);

        assertNotNull(firstDevice);

        Device secondDevice = doExecuteWithRetriesAndInterval(() -> doGet("/api/tenant/devices?deviceName=" + secondDeviceName, Device.class),
                20,
                100);

        assertNotNull(secondDevice);

        List<String> firstDeviceActualKeys = getActualKeysList(firstDevice.getId(), expectedKeys);
        Set<String> firstDeviceActualKeySet = new HashSet<>(firstDeviceActualKeys);

        List<String> secondDeviceActualKeys = getActualKeysList(secondDevice.getId(), expectedKeys);
        Set<String> secondDeviceActualKeySet = new HashSet<>(secondDeviceActualKeys);

        Set<String> expectedKeySet = new HashSet<>(expectedKeys);

        assertEquals(expectedKeySet, firstDeviceActualKeySet);
        assertEquals(expectedKeySet, secondDeviceActualKeySet);

        String getTelemetryValuesUrlFirstDevice = getTelemetryValuesUrl(firstDevice.getId(), firstDeviceActualKeySet);
        String getTelemetryValuesUrlSecondDevice = getTelemetryValuesUrl(firstDevice.getId(), secondDeviceActualKeySet);

        Map<String, List<Map<String, Object>>> firstDeviceValues = doGetAsyncTyped(getTelemetryValuesUrlFirstDevice, new TypeReference<>() {});
        Map<String, List<Map<String, Object>>> secondDeviceValues = doGetAsyncTyped(getTelemetryValuesUrlSecondDevice, new TypeReference<>() {});

        assertGatewayDeviceData(firstDeviceValues, expectedKeys);
        assertGatewayDeviceData(secondDeviceValues, expectedKeys);
    }

    private List<String> getActualKeysList(DeviceId deviceId, List<String> expectedKeys) throws Exception {
        long start = System.currentTimeMillis();
        long end = System.currentTimeMillis() + 3000;

        List<String> actualKeys = null;
        while (start <= end) {
            actualKeys = doGetAsyncTyped("/api/plugins/telemetry/DEVICE/" + deviceId + "/keys/timeseries", new TypeReference<>() {});
            if (actualKeys.size() == expectedKeys.size()) {
                break;
            }
            Thread.sleep(100);
            start += 100;
        }
        return actualKeys;
    }

    protected String getGatewayTelemetryJsonPayload(String deviceA, String deviceB, String firstTsValue, String secondTsValue) {
        String payload = "[{\"ts\": " + firstTsValue + ", \"values\": " + PAYLOAD_VALUES_STR + "}, " +
                "{\"ts\": " + secondTsValue + ", \"values\": " + PAYLOAD_VALUES_STR + "}]";
        return "{\"" + deviceA + "\": " + payload + ",  \"" + deviceB + "\": " + payload + "}";
    }

    private String getTelemetryValuesUrl(DeviceId deviceId, Set<String> actualKeySet) {
        return "/api/plugins/telemetry/DEVICE/" + deviceId + "/values/timeseries?startTs=0&endTs=25000&keys=" + String.join(",", actualKeySet);
    }

    private void assertGatewayDeviceData(Map<String, List<Map<String, Object>>> deviceValues, List<String> expectedKeys) {

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

    private void assertValues(Map<String, List<Map<String, Object>>> deviceValues, int arrayIndex) {
        for (Map.Entry<String, List<Map<String, Object>>> entry : deviceValues.entrySet()) {
            String key = entry.getKey();
            List<Map<String, Object>> tsKv = entry.getValue();
            String value = (String) tsKv.get(arrayIndex).get("value");
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

    private void assertExplicitProtoFieldValues(Map<String, List<Map<String, Object>>> deviceValues) {
        for (Map.Entry<String, List<Map<String, Object>>> entry : deviceValues.entrySet()) {
            String key = entry.getKey();
            List<Map<String, Object>> tsKv = entry.getValue();
            String value = (String) tsKv.get(0).get("value");
            switch (key) {
                case "key1":
                    assertEquals("", value);
                    break;
                case "key2":
                    assertEquals("false", value);
                    break;
                case "key3":
                    assertEquals("0.0", value);
                    break;
                case "key4":
                    assertEquals("0", value);
                    break;
                case "key5":
                    assertEquals("{\"someArray\":[1,2,3],\"someNestedObject\":{\"key\":\"value\"}}", value);
                    break;
            }
        }
    }

    private void assertTs(Map<String, List<Map<String, Object>>> deviceValues, List<String> expectedKeys, int ts, int arrayIndex) {
        assertEquals(ts, deviceValues.get(expectedKeys.get(0)).get(arrayIndex).get("ts"));
        assertEquals(ts, deviceValues.get(expectedKeys.get(1)).get(arrayIndex).get("ts"));
        assertEquals(ts, deviceValues.get(expectedKeys.get(2)).get(arrayIndex).get("ts"));
        assertEquals(ts, deviceValues.get(expectedKeys.get(3)).get(arrayIndex).get("ts"));
        assertEquals(ts, deviceValues.get(expectedKeys.get(4)).get(arrayIndex).get("ts"));
    }

    //    @Test - Unstable
    public void testMqttQoSLevel() throws Exception {
        MqttTestClient client = new MqttTestClient();
        MqttTestCallback callback = new MqttTestCallback();
        client.setCallback(callback);
        client.connectAndWait(accessToken);
        client.subscribe(DEVICE_ATTRIBUTES_TOPIC, MqttQoS.AT_MOST_ONCE);
        String payload = "{\"key\":\"uniqueValue\"}";
//        TODO 3.1: we need to acknowledge subscription only after it is processed by device actor and not when the message is pushed to queue.
//        MqttClient -> SUB REQUEST -> Transport -> Kafka -> Device Actor (subscribed)
//        MqttClient <- SUB_ACK <- Transport
        Thread.sleep(5000);
        doPostAsync("/api/plugins/telemetry/" + savedDevice.getId() + "/SHARED_SCOPE", payload, String.class, status().isOk());
        callback.getSubscribeLatch().await(10, TimeUnit.SECONDS);
        assertEquals(payload.getBytes(), callback.getPayloadBytes());
        assertEquals(MqttQoS.AT_MOST_ONCE.value(), callback.getQoS());
    }

}
