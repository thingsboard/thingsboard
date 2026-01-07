/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.transport.mqtt.mqttv3.telemetry.timeseries;

import com.fasterxml.jackson.core.type.TypeReference;
import io.netty.handler.codec.mqtt.MqttQoS;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.msg.gateway.metrics.GatewayMetadata;
import org.thingsboard.server.transport.mqtt.AbstractMqttIntegrationTest;
import org.thingsboard.server.transport.mqtt.MqttTestConfigProperties;
import org.thingsboard.server.transport.mqtt.gateway.GatewayMetricsService;
import org.thingsboard.server.transport.mqtt.gateway.metrics.GatewayMetricsState;
import org.thingsboard.server.transport.mqtt.mqttv3.MqttTestCallback;
import org.thingsboard.server.transport.mqtt.mqttv3.MqttTestClient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.common.data.device.profile.MqttTopics.DEVICE_ATTRIBUTES_TOPIC;
import static org.thingsboard.server.common.data.device.profile.MqttTopics.DEVICE_TELEMETRY_SHORT_JSON_TOPIC;
import static org.thingsboard.server.common.data.device.profile.MqttTopics.DEVICE_TELEMETRY_SHORT_TOPIC;
import static org.thingsboard.server.common.data.device.profile.MqttTopics.DEVICE_TELEMETRY_TOPIC;
import static org.thingsboard.server.common.data.device.profile.MqttTopics.GATEWAY_CONNECT_TOPIC;
import static org.thingsboard.server.common.data.device.profile.MqttTopics.GATEWAY_TELEMETRY_TOPIC;
import static org.thingsboard.server.transport.mqtt.gateway.GatewayMetricsService.GATEWAY_METRICS;

@Slf4j
public abstract class AbstractMqttTimeseriesIntegrationTest extends AbstractMqttIntegrationTest {

    protected static final String PAYLOAD_VALUES_STR = "{\"key1\":\"value1\", \"key2\":true, \"key3\": 3.0, \"key4\": 4," +
            " \"key5\": {\"someNumber\": 42, \"someArray\": [1,2,3], \"someNestedObject\": {\"key\": \"value\"}}}";

    protected static final String MALFORMED_JSON_PAYLOAD = "{\"key1\":, \"key2\":true, \"key3\": 3.0, \"key4\": 4," +
            " \"key5\": {\"someNumber\": 42, \"someArray\": [1,2,3], \"someNestedObject\": {\"key\": \"value\"}}}";

    @SpyBean
    GatewayMetricsService gatewayMetricsService;

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
    public void testAckIsReceivedOnFailedPublishMessage() throws Exception {
        String devicePayload = "[{\"ts\": 10000, \"values\": " + PAYLOAD_VALUES_STR + "}]";
        String payloadA = "{\"Device A\": " + devicePayload + "}";

        String deviceBPayload = "[{\"ts\": 10000, \"values\": " + PAYLOAD_VALUES_STR + "}]";
        String payloadB = "{\"Device B\": " + deviceBPayload + "}";

        testAckIsReceivedOnFailedPublishMessage("Device A", payloadA.getBytes(), "Device B", payloadB.getBytes());
    }

    protected void testAckIsReceivedOnFailedPublishMessage(String deviceName1, byte[] payload1, String deviceName2, byte[] payload2) throws Exception {
        updateDefaultTenantProfileConfig(profileConfiguration -> {
            profileConfiguration.setMaxDevices(3);
        });

        MqttTestClient client = new MqttTestClient();
        client.connectAndWait(gatewayAccessToken);
        client.publishAndWait(GATEWAY_TELEMETRY_TOPIC, payload1);

        // check device is created
        await().atMost(TIMEOUT, TimeUnit.SECONDS).untilAsserted(() -> {
            assertNotNull(doGet("/api/tenant/devices?deviceName=" + deviceName1, Device.class));
        });

        client.publishAndWait(GATEWAY_TELEMETRY_TOPIC, payload2);
        client.disconnectAndWait();

        // check device was not created due to limit
        doGet("/api/tenant/devices?deviceName=" + deviceName2).andExpect(status().isNotFound());

        updateDefaultTenantProfileConfig(profileConfiguration -> {
            profileConfiguration.setMaxDevices(0);
        });
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

    @Test
    public void testPushMetricsGateway() throws Exception {
        MqttTestConfigProperties configProperties = MqttTestConfigProperties.builder()
                .gatewayName("Test metrics gateway")
                .build();
        processBeforeTest(configProperties);

        Map<String, List<Long>> gwLatencies = new HashMap<>();
        Map<String, List<Long>> transportLatencies = new HashMap<>();

        publishLatency(gwLatencies, transportLatencies, 5);

        gatewayMetricsService.reportMetrics();

        List<String> actualKeys = getActualKeysList(savedGateway.getId(), List.of(GATEWAY_METRICS));
        assertEquals(GATEWAY_METRICS, actualKeys.get(0));

        String telemetryUrl = String.format("/api/plugins/telemetry/DEVICE/%s/values/timeseries?startTs=%d&endTs=%d&keys=%s", savedGateway.getId(), 0, System.currentTimeMillis(), GATEWAY_METRICS);

        Map<String, List<Map<String, Object>>> gatewayTelemetry = doGetAsyncTyped(telemetryUrl, new TypeReference<>() {});
        Map<String, Object> latencyCheckTelemetry = gatewayTelemetry.get(GATEWAY_METRICS).get(0);
        Map<String, GatewayMetricsState.ConnectorMetricsResult> latencyCheckValue = JacksonUtil.fromString((String) latencyCheckTelemetry.get("value"), new TypeReference<>() {});
        assertNotNull(latencyCheckValue);

        gwLatencies.forEach((connectorName, gwLatencyList) -> {
            long avgGwLatency = (long) gwLatencyList.stream().mapToLong(Long::longValue).average().getAsDouble();
            long minGwLatency = gwLatencyList.stream().mapToLong(Long::longValue).min().getAsLong();
            long maxGwLatency = gwLatencyList.stream().mapToLong(Long::longValue).max().getAsLong();

            List<Long> transportLatencyList = transportLatencies.get(connectorName);
            assertNotNull(transportLatencyList);

            long avgTransportLatency = (long) transportLatencyList.stream().mapToLong(Long::longValue).average().getAsDouble();
            long minTransportLatency = transportLatencyList.stream().mapToLong(Long::longValue).min().getAsLong();
            long maxTransportLatency = transportLatencyList.stream().mapToLong(Long::longValue).max().getAsLong();

            GatewayMetricsState.ConnectorMetricsResult connectorLatencyResult = latencyCheckValue.get(connectorName);
            assertNotNull(connectorLatencyResult);
            checkConnectorLatencyResult(connectorLatencyResult, avgGwLatency, minGwLatency, maxGwLatency, avgTransportLatency, minTransportLatency, maxTransportLatency);
        });
    }

    private void publishLatency(Map<String, List<Long>> gwLatencies, Map<String, List<Long>> transportLatencies, int n) throws Exception {
        Random random = new Random();
        for (int i = 0; i < n; i++) {
            long publishedTs = System.currentTimeMillis() - 10;
            long gatewayLatencyA = random.nextLong(100, 500);
            var firstData = new GatewayMetadata("connectorA", publishedTs - gatewayLatencyA, publishedTs);
            gwLatencies.computeIfAbsent("connectorA", key -> new ArrayList<>()).add(gatewayLatencyA);
            long gatewayLatencyB = random.nextLong(120, 450);
            var secondData = new GatewayMetadata("connectorB", publishedTs - gatewayLatencyB, publishedTs);
            gwLatencies.computeIfAbsent("connectorB", key -> new ArrayList<>()).add(gatewayLatencyB);

            List<String> expectedKeys = Arrays.asList("key1", "key2", "key3", "key4", "key5");
            String deviceName1 = "Device A";
            String deviceName2 = "Device B";
            String firstMetadata = JacksonUtil.writeValueAsString(firstData);
            String secondMetadata = JacksonUtil.writeValueAsString(secondData);
            String payload = getGatewayTelemetryJsonPayloadWithMetadata(deviceName1, deviceName2, "10000", "20000", firstMetadata, secondMetadata);
            processGatewayTelemetryTest(GATEWAY_TELEMETRY_TOPIC, expectedKeys, payload.getBytes(), deviceName1, deviceName2);

            ArgumentCaptor<Long> transportReceiveTsCaptorA = ArgumentCaptor.forClass(Long.class);
            ArgumentCaptor<Long> transportReceiveTsCaptorB = ArgumentCaptor.forClass(Long.class);
            verify(gatewayMetricsService).process(any(), eq(savedGateway.getId()), eq(List.of(firstData)), transportReceiveTsCaptorA.capture());
            verify(gatewayMetricsService).process(any(), eq(savedGateway.getId()), eq(List.of(secondData)), transportReceiveTsCaptorB.capture());
            Long transportReceiveTsA = transportReceiveTsCaptorA.getValue();
            Long transportReceiveTsB = transportReceiveTsCaptorB.getValue();
            Long transportLatencyA = transportReceiveTsA - publishedTs;
            Long transportLatencyB = transportReceiveTsB - publishedTs;
            transportLatencies.computeIfAbsent("connectorA", key -> new ArrayList<>()).add(transportLatencyA);
            transportLatencies.computeIfAbsent("connectorB", key -> new ArrayList<>()).add(transportLatencyB);
        }
    }

    private void checkConnectorLatencyResult(GatewayMetricsState.ConnectorMetricsResult result, long avgGwLatency, long minGwLatency, long maxGwLatency,
                                             long avgTransportLatency, long minTransportLatency, long maxTransportLatency) {
        assertNotNull(result);
        assertEquals(avgGwLatency, result.avgGwLatency());
        assertEquals(minGwLatency, result.minGwLatency());
        assertEquals(maxGwLatency, result.maxGwLatency());
        assertEquals(avgTransportLatency, result.avgTransportLatency());
        assertEquals(minTransportLatency, result.minTransportLatency());
        assertEquals(maxTransportLatency, result.maxTransportLatency());
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
            values = doGetAsyncTyped(getTelemetryValuesUrl, new TypeReference<>() {
            });
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
        client.disconnectAndWait();

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

    protected String getGatewayTelemetryJsonPayloadWithMetadata(String deviceA, String deviceB, String firstTsValue, String secondTsValue, String firstMetadata, String secondMetadata) {
        String payloadA = "[{\"ts\": " + firstTsValue + ", \"values\": " + PAYLOAD_VALUES_STR + ",  \"metadata\":" + firstMetadata + "}, " +
                "{\"ts\": " + secondTsValue + ", \"values\": " + PAYLOAD_VALUES_STR + "}]";

        String payloadB = "[{\"ts\": " + firstTsValue + ", \"values\": " + PAYLOAD_VALUES_STR + ",  \"metadata\":" + secondMetadata + "}, " +
                "{\"ts\": " + secondTsValue + ", \"values\": " + PAYLOAD_VALUES_STR + "}]";

        return "{\"" + deviceA + "\": " + payloadA + ",  \"" + deviceB + "\": " + payloadB + "}";
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
        callback.getSubscribeLatch().await(DEFAULT_WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertEquals(payload.getBytes(), callback.getPayloadBytes());
        assertEquals(MqttQoS.AT_MOST_ONCE.value(), callback.getMessageArrivedQoS());
    }

}
