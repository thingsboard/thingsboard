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
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.gen.transport.TransportApiProtos;
import org.thingsboard.server.mqtt.telemetry.AbstractMqttTelemetryIntegrationTest;
import org.thingsboard.server.transport.mqtt.MqttTopics;

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
public abstract class AbstractMqttTimseriesIntegrationTest extends AbstractMqttTelemetryIntegrationTest {

    @Before
    public void beforeTest() throws Exception {
        processBeforeTest("Test Post Telemetry device", "Test Post Telemetry gateway");
    }

    @Test
    public void testPushMqttTelemetryV1Json() throws Exception {
        List<String> expectedKeys = Arrays.asList("key1", "key2", "key3", "key4", "key5");
        processTelemetryTest(MqttTopics.DEVICE_TELEMETRY_TOPIC_V1_JSON, expectedKeys, PAYLOAD_VALUES_STR_V_1.getBytes(), false);
    }

    @Test
    public void testPushMqttTelemetryV1JsonWithTs() throws Exception {
        String payloadStr = "{\"ts\": 10000, \"values\": " + PAYLOAD_VALUES_STR_V_1 + "}";
        List<String> expectedKeys = Arrays.asList("key1", "key2", "key3", "key4", "key5");
        processTelemetryTest(MqttTopics.DEVICE_TELEMETRY_TOPIC_V1_JSON, expectedKeys, payloadStr.getBytes(), true);
    }

    @Test
    public void testPushMqttTelemetryV2Json() throws Exception {
        List<String> expectedKeys = Arrays.asList("key6", "key7", "key8", "key9", "key10");
        processTelemetryTest(MqttTopics.DEVICE_TELEMETRY_TOPIC_V2_JSON, expectedKeys, PAYLOAD_VALUES_STR_V_2.getBytes(), false);
    }

    @Test
    public void testPushMqttTelemetryV2JsonWithTs() throws Exception {
        String payloadStr = "{\"ts\": 10000, \"values\": " + PAYLOAD_VALUES_STR_V_2 + "}";
        List<String> expectedKeys = Arrays.asList("key6", "key7", "key8", "key9", "key10");
        processTelemetryTest(MqttTopics.DEVICE_TELEMETRY_TOPIC_V2_JSON, expectedKeys, payloadStr.getBytes(), true);
    }

    @Test
    public void testPushMqttTelemetryV2Proto() throws Exception {
        List<String> expectedKeys = Arrays.asList("key11", "key12", "key13", "key14", "key15");
        TransportApiProtos.TsKvListProto tsKvListProto = getTsKvListProto(expectedKeys, 0);
        processTelemetryTest(MqttTopics.DEVICE_TELEMETRY_TOPIC_V2_PROTO, expectedKeys, tsKvListProto.toByteArray(), false);
    }

    @Test
    public void testPushMqttTelemetryV2ProtoWithTs() throws Exception {
        List<String> expectedKeys = Arrays.asList("key16", "key17", "key18", "key19", "key20");
        TransportApiProtos.TsKvListProto tsKvListProto = getTsKvListProto(expectedKeys, 10000);
        processTelemetryTest(MqttTopics.DEVICE_TELEMETRY_TOPIC_V2_PROTO, expectedKeys, tsKvListProto.toByteArray(), true);
    }

    @Test
    public void testPushMqttTelemetryV1GatewayJson() throws Exception {
        List<String> expectedKeys = Arrays.asList("key1", "key2", "key3", "key4", "key5");
        String deviceName1 = "Device D";
        String deviceName2 = "Device E";
        String payload = getGatewayTelemetryJsonPayload(deviceName1, deviceName2, "10000", "20000");
        processGatewayTelemetryTest(MqttTopics.GATEWAY_TELEMETRY_TOPIC_V1_JSON, expectedKeys, payload.getBytes(), deviceName1, deviceName2);
    }

    @Test
    public void testPushMqttTelemetryV2GatewayJson() throws Exception {
        List<String> expectedKeys = Arrays.asList("key1", "key2", "key3", "key4", "key5");
        String deviceName1 = "Device F";
        String deviceName2 = "Device G";
        String payload = getGatewayTelemetryJsonPayload(deviceName1, deviceName2, "10000", "20000");
        processGatewayTelemetryTest(MqttTopics.GATEWAY_TELEMETRY_TOPIC_V2_JSON, expectedKeys, payload.getBytes(), deviceName1, deviceName2);
    }

    @Test
    public void testPushMqttTelemetryV2GatewayProto() throws Exception {
        TransportApiProtos.GatewayTelemetryMsg.Builder gatewayTelemetryMsgProtoBuilder = TransportApiProtos.GatewayTelemetryMsg.newBuilder();
        List<String> expectedKeys = Arrays.asList("key1", "key2", "key3", "key4", "key5");
        String deviceName1 = "Device H";
        String deviceName2 = "Device I";
        TransportApiProtos.TelemetryMsg deviceATelemetryMsgProto = getDeviceTelemetryMsgProto(deviceName1, expectedKeys, 10000, 20000);
        TransportApiProtos.TelemetryMsg deviceBTelemetryMsgProto = getDeviceTelemetryMsgProto(deviceName2, expectedKeys, 10000, 20000);
        gatewayTelemetryMsgProtoBuilder.addAllMsg(Arrays.asList(deviceATelemetryMsgProto, deviceBTelemetryMsgProto));
        TransportApiProtos.GatewayTelemetryMsg gatewayTelemetryMsg = gatewayTelemetryMsgProtoBuilder.build();
        processGatewayTelemetryTest(MqttTopics.GATEWAY_TELEMETRY_TOPIC_V2_PROTO, expectedKeys, gatewayTelemetryMsg.toByteArray(), deviceName1, deviceName2);
    }

    @Test
    public void testGatewayConnectJsonV1() throws Exception {
        String payload = "{\"device\":\"Device A\"}";
        MqttAsyncClient client = getMqttAsyncClient(gatewayAccessToken);
        Thread.sleep(3000);
        publishMqttMsg(client, payload.getBytes(), MqttTopics.GATEWAY_CONNECT_TOPIC_V1_JSON);

        Thread.sleep(2000);

        String deviceName = "Device A";
        Device device = doGet("/api/tenant/devices?deviceName=" + deviceName, Device.class);
        assertNotNull(device);
    }

    @Test
    public void testGatewayConnectJsonV2() throws Exception {
        String payload = "{\"device\":\"Device B\"}";
        MqttAsyncClient client = getMqttAsyncClient(gatewayAccessToken);
        Thread.sleep(3000);
        publishMqttMsg(client, payload.getBytes(), MqttTopics.GATEWAY_CONNECT_TOPIC_V2_JSON);

        Thread.sleep(2000);

        String deviceName = "Device B";
        Device device = doGet("/api/tenant/devices?deviceName=" + deviceName, Device.class);
        assertNotNull(device);
    }

    @Test
    public void testGatewayConnectProto() throws Exception {
        TransportApiProtos.ConnectMsg connectMsgProto = getConnectProto("Device C");
        MqttAsyncClient client = getMqttAsyncClient(gatewayAccessToken);
        Thread.sleep(3000);
        publishMqttMsg(client, connectMsgProto.toByteArray(), MqttTopics.GATEWAY_CONNECT_TOPIC_V2_PROTO);

        Thread.sleep(2000);

        String deviceName = "Device C";
        Device device = doGet("/api/tenant/devices?deviceName=" + deviceName, Device.class);
        assertNotNull(device);
    }

    private void processTelemetryTest(String topic, List<String> expectedKeys, byte[] payload, boolean withTs) throws Exception {
        MqttAsyncClient client = getMqttAsyncClient(accessToken);
        Thread.sleep(3000);
        publishMqttMsg(client, payload, topic);

        String deviceId = savedDevice.getId().getId().toString();

        Thread.sleep(2000);
        List<String> actualKeys = doGetAsync("/api/plugins/telemetry/DEVICE/" + deviceId + "/keys/timeseries", List.class);
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
            assertTsValues(values, actualKeys, 10000, 0);
        }
        assertValues(values, actualKeys, 0);
    }

    private void processGatewayTelemetryTest(String topic, List<String> expectedKeys, byte[] payload, String firstDeviceName, String secondDeviceName) throws Exception {
        MqttAsyncClient client = getMqttAsyncClient(gatewayAccessToken);
        Thread.sleep(3000);

        publishMqttMsg(client, payload, topic);

        Thread.sleep(2000);

        Device firstDevice = doGet("/api/tenant/devices?deviceName=" + firstDeviceName, Device.class);
        assertNotNull(firstDevice);
        Device secondDevice = doGet("/api/tenant/devices?deviceName=" + secondDeviceName, Device.class);
        assertNotNull(secondDevice);

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

        assertGatewayDeviceData(firstDeviceValues, firstDeviceActualKeys);
        assertGatewayDeviceData(secondDeviceValues, secondDeviceActualKeys);


    }

    private String getTelemetryValuesUrl(DeviceId deviceId, Set<String> actualKeySet) {
        return "/api/plugins/telemetry/DEVICE/" + deviceId + "/values/timeseries?startTs=0&endTs=25000&keys=" + String.join(",", actualKeySet);
    }

    private String getGatewayTelemetryJsonPayload(String deviceA, String deviceB, String firstTsValue, String secondTsValue) {
        String payload = "[{\"ts\": " + firstTsValue + ", \"values\": " + PAYLOAD_VALUES_STR_V_1 + "}, " +
                "{\"ts\": " + secondTsValue + ", \"values\": " + PAYLOAD_VALUES_STR_V_1 + "}]";
        return "{\"" + deviceA + "\": " + payload + ",  \"" + deviceB + "\": " + payload + "}";
    }

    private void assertGatewayDeviceData(Map<String, List<Map<String, String>>> deviceValues, List<String> deviceActualKeys) {

        assertEquals(2, deviceValues.get(deviceActualKeys.get(0)).size());
        assertEquals(2, deviceValues.get(deviceActualKeys.get(1)).size());
        assertEquals(2, deviceValues.get(deviceActualKeys.get(2)).size());
        assertEquals(2, deviceValues.get(deviceActualKeys.get(3)).size());
        assertEquals(2, deviceValues.get(deviceActualKeys.get(4)).size());

        assertTsValues(deviceValues, deviceActualKeys, 20000, 0);
        assertTsValues(deviceValues, deviceActualKeys, 10000, 1);

        assertValues(deviceValues, deviceActualKeys, 0);
        assertValues(deviceValues, deviceActualKeys, 1);

    }

    private void assertValues(Map<String, List<Map<String, String>>> deviceValues, List<String> deviceActualKeys, int arrayIndex) {
        assertEquals("value1", deviceValues.get(deviceActualKeys.get(0)).get(arrayIndex).get("value"));
        assertEquals("true", deviceValues.get(deviceActualKeys.get(1)).get(arrayIndex).get("value"));
        assertEquals("3.0", deviceValues.get(deviceActualKeys.get(2)).get(arrayIndex).get("value"));
        assertEquals("4", deviceValues.get(deviceActualKeys.get(3)).get(arrayIndex).get("value"));
        assertEquals("{\"someNumber\":42,\"someArray\":[1,2,3],\"someNestedObject\":{\"key\":\"value\"}}", deviceValues.get(deviceActualKeys.get(4)).get(arrayIndex).get("value"));
    }

    private void assertTsValues(Map<String, List<Map<String, String>>> deviceValues, List<String> deviceActualKeys, int ts, int arrayIndex) {
        assertEquals(ts, deviceValues.get(deviceActualKeys.get(0)).get(arrayIndex).get("ts"));
        assertEquals(ts, deviceValues.get(deviceActualKeys.get(1)).get(arrayIndex).get("ts"));
        assertEquals(ts, deviceValues.get(deviceActualKeys.get(2)).get(arrayIndex).get("ts"));
        assertEquals(ts, deviceValues.get(deviceActualKeys.get(3)).get(arrayIndex).get("ts"));
        assertEquals(ts, deviceValues.get(deviceActualKeys.get(4)).get(arrayIndex).get("ts"));
    }


    private TransportApiProtos.ConnectMsg getConnectProto(String deviceName) {
        TransportApiProtos.ConnectMsg.Builder builder = TransportApiProtos.ConnectMsg.newBuilder();
        builder.setDeviceName(deviceName);
        return builder.build();
    }

    private TransportApiProtos.TelemetryMsg getDeviceTelemetryMsgProto(String deviceName, List<String> expectedKeys, long firstTs, long secondTs) {
        TransportApiProtos.TelemetryMsg.Builder deviceTelemetryMsgBuilder = TransportApiProtos.TelemetryMsg.newBuilder();
        TransportApiProtos.TsKvListProto tsKvListProto1 = getTsKvListProto(expectedKeys, firstTs);
        TransportApiProtos.TsKvListProto tsKvListProto2 = getTsKvListProto(expectedKeys, secondTs);
        TransportApiProtos.TsKvListProtoArray.Builder tsKvListProtoArrayBuilder = TransportApiProtos.TsKvListProtoArray.newBuilder();
        tsKvListProtoArrayBuilder.addAllTsKv(Arrays.asList(tsKvListProto1, tsKvListProto2));
        deviceTelemetryMsgBuilder.setDeviceName(deviceName);
        deviceTelemetryMsgBuilder.setValues(tsKvListProtoArrayBuilder);
        return deviceTelemetryMsgBuilder.build();
    }

    private TransportApiProtos.TsKvListProto getTsKvListProto(List<String> expectedKeys, long ts) {
        List<TransportApiProtos.KeyValueProto> kvProtos = getKvProtos(expectedKeys);
        TransportApiProtos.TsKvListProto.Builder builder = TransportApiProtos.TsKvListProto.newBuilder();
        builder.addAllKv(kvProtos);
        builder.setTs(ts);
        return builder.build();
    }

    //    @Test - Unstable
    public void testMqttQoSLevel() throws Exception {
        String clientId = MqttAsyncClient.generateClientId();
        MqttAsyncClient client = new MqttAsyncClient(MQTT_URL, clientId);

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
