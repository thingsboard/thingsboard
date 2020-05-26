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
package org.thingsboard.server.mqtt.telemetry.attributes;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@Slf4j
public abstract class AbstractMqttAttributesIntegrationTest extends AbstractMqttTelemetryIntegrationTest {

    @Before
    public void beforeTest() throws Exception {
        processBeforeTest("Test Post Attributes device", "Test Post Attributes gateway");
    }

    @Test
    public void testPushMqttAttributesV1Json() throws Exception {
        List<String> expectedKeys = Arrays.asList("key1", "key2", "key3", "key4", "key5");
        processAttributesTest(MqttTopics.DEVICE_ATTRIBUTES_TOPIC_V1_JSON, expectedKeys, PAYLOAD_VALUES_STR_V_1.getBytes());
    }

    @Test
    public void testPushMqttAttributesV2Json() throws Exception {
        List<String> expectedKeys = Arrays.asList("key6", "key7", "key8", "key9", "key10");
        processAttributesTest(MqttTopics.DEVICE_ATTRIBUTES_TOPIC_V2_JSON, expectedKeys, PAYLOAD_VALUES_STR_V_2.getBytes());
    }

    @Test
    public void testPushMqttAttributesV2Proto() throws Exception {
        List<String> expectedKeys = Arrays.asList("key11", "key12", "key13", "key14", "key15");
        TransportApiProtos.KvListProto kvListProto = getKvListProto(expectedKeys);
        processAttributesTest(MqttTopics.DEVICE_ATTRIBUTES_TOPIC_V2_PROTO, expectedKeys, kvListProto.toByteArray());
    }

    @Test
    public void testPushMqttAttributesV1GatewayJson() throws Exception {
        List<String> expectedKeys = Arrays.asList("key1", "key2", "key3", "key4", "key5");
        String deviceName1 = "Device A";
        String deviceName2 = "Device B";
        String payload = getGatewayAttributesJsonPayload(deviceName1, deviceName2);
        processGatewayAttributesTest(MqttTopics.GATEWAY_ATTRIBUTES_TOPIC_V1_JSON, expectedKeys, payload.getBytes(), deviceName1, deviceName2);
    }


    @Test
    public void testPushMqttAttributesV2GatewayJson() throws Exception {
        List<String> expectedKeys = Arrays.asList("key1", "key2", "key3", "key4", "key5");
        String deviceName1 = "Device C";
        String deviceName2 = "Device D";
        String payload = getGatewayAttributesJsonPayload(deviceName1, deviceName2);
        processGatewayAttributesTest(MqttTopics.GATEWAY_ATTRIBUTES_TOPIC_V2_JSON, expectedKeys, payload.getBytes(), deviceName1, deviceName2);
    }


    @Test
    public void testPushMqttAttributesV2GatewayProto() throws Exception {
        TransportApiProtos.GatewayAttributesMsg.Builder gatewayAttributesMsgProtoBuilder = TransportApiProtos.GatewayAttributesMsg.newBuilder();
        List<String> expectedKeys = Arrays.asList("key1", "key2", "key3", "key4", "key5");
        String deviceName1 = "Device H";
        String deviceName2 = "Device I";
        TransportApiProtos.AttributesMsg firstDeviceAttributesMsgProto = getDeviceAttributesMsgProto(deviceName1, expectedKeys);
        TransportApiProtos.AttributesMsg secondDeviceAttributesMsgProto = getDeviceAttributesMsgProto(deviceName2, expectedKeys);
        gatewayAttributesMsgProtoBuilder.addAllMsg(Arrays.asList(firstDeviceAttributesMsgProto, secondDeviceAttributesMsgProto));
        TransportApiProtos.GatewayAttributesMsg gatewayAttributesMsg = gatewayAttributesMsgProtoBuilder.build();
        processGatewayAttributesTest(MqttTopics.GATEWAY_ATTRIBUTES_TOPIC_V2_PROTO, expectedKeys, gatewayAttributesMsg.toByteArray(), deviceName1, deviceName2);
    }

    private void processAttributesTest(String topic, List<String> expectedKeys, byte[] payload) throws Exception {
        MqttAsyncClient client = getMqttAsyncClient(accessToken);
        Thread.sleep(3000);
        publishMqttMsg(client, payload, topic);

        DeviceId deviceId = savedDevice.getId();

        Thread.sleep(2000);
        List<String> actualKeys = doGetAsync("/api/plugins/telemetry/DEVICE/" + deviceId + "/keys/attributes/CLIENT_SCOPE", List.class);
        Set<String> actualKeySet = new HashSet<>(actualKeys);

        Set<String> expectedKeySet = new HashSet<>(expectedKeys);

        assertEquals(expectedKeySet, actualKeySet);

        String getAttributesValuesUrl = getAttributesValuesUrl(deviceId, actualKeySet);
        List<Map<String, Object>> values = doGetAsync(getAttributesValuesUrl, List.class);
        assertAttributesValues(values, expectedKeySet);
        String deleteAttributesUrl ="/api/plugins/telemetry/DEVICE/" + deviceId + "/CLIENT_SCOPE?keys=" + String.join(",", actualKeySet);
        doDelete(deleteAttributesUrl);
    }

    private void processGatewayAttributesTest(String topic, List<String> expectedKeys, byte[] payload, String firstDeviceName, String secondDeviceName) throws Exception {
        MqttAsyncClient client = getMqttAsyncClient(gatewayAccessToken);
        Thread.sleep(3000);

        publishMqttMsg(client, payload, topic);

        Thread.sleep(2000);

        Device firstDevice = doGet("/api/tenant/devices?deviceName=" + firstDeviceName, Device.class);
        assertNotNull(firstDevice);
        Device secondDevice = doGet("/api/tenant/devices?deviceName=" + secondDeviceName, Device.class);
        assertNotNull(secondDevice);

        List<String> firstDeviceActualKeys = doGetAsync("/api/plugins/telemetry/DEVICE/" + firstDevice.getId() + "/keys/attributes/CLIENT_SCOPE", List.class);
        Set<String> firstDeviceActualKeySet = new HashSet<>(firstDeviceActualKeys);

        List<String> secondDeviceActualKeys = doGetAsync("/api/plugins/telemetry/DEVICE/" + secondDevice.getId() + "/keys/attributes/CLIENT_SCOPE", List.class);
        Set<String> secondDeviceActualKeySet = new HashSet<>(secondDeviceActualKeys);

        Set<String> expectedKeySet = new HashSet<>(expectedKeys);

        assertEquals(expectedKeySet, firstDeviceActualKeySet);
        assertEquals(expectedKeySet, secondDeviceActualKeySet);

        String getAttributesValuesUrlFirstDevice = getAttributesValuesUrl(firstDevice.getId(), firstDeviceActualKeySet);
        String getAttributesValuesUrlSecondDevice = getAttributesValuesUrl(firstDevice.getId(), secondDeviceActualKeySet);

        List<Map<String, Object>> firstDeviceValues = doGetAsync(getAttributesValuesUrlFirstDevice, List.class);
        List<Map<String, Object>> secondDeviceValues = doGetAsync(getAttributesValuesUrlSecondDevice, List.class);

        assertAttributesValues(firstDeviceValues, expectedKeySet);
        assertAttributesValues(secondDeviceValues, expectedKeySet);

    }

    private String getAttributesValuesUrl(DeviceId deviceId, Set<String> actualKeySet) {
        return "/api/plugins/telemetry/DEVICE/" + deviceId + "/values/attributes/CLIENT_SCOPE?keys=" + String.join(",", actualKeySet);
    }

    private String getGatewayAttributesJsonPayload(String deviceA, String deviceB) {
        return "{\"" + deviceA + "\": " + PAYLOAD_VALUES_STR_V_1 + ",  \"" + deviceB + "\": " + PAYLOAD_VALUES_STR_V_1 + "}";
    }

    private void assertAttributesValues(List<Map<String, Object>> deviceValues, Set<String> expectedKeySet) {
        for (Map<String, Object> map : deviceValues) {
            String key = (String) map.get("key");
            Object value = map.get("value");
            assertTrue(expectedKeySet.contains(key));
            switch (key) {
                case "key1":
                case "key6":
                case "key11":
                    assertEquals("value1", value);
                    break;
                case "key2":
                case "key7":
                case "key12":
                    assertEquals(true, value);
                    break;
                case "key3":
                case "key8":
                case "key13":
                    assertEquals(3.0, value);
                    break;
                case "key4":
                case "key9":
                case "key14":
                    assertEquals(4, value);
                    break;
//                case "key5":
//                    assertEquals(4, value);
            }
        }
    }

    private TransportApiProtos.AttributesMsg getDeviceAttributesMsgProto(String deviceName, List<String> expectedKeys) {
        TransportApiProtos.AttributesMsg.Builder deviceAttributesMsgBuilder = TransportApiProtos.AttributesMsg.newBuilder();
        TransportApiProtos.KvListProto kvListProto = getKvListProto(expectedKeys);
        deviceAttributesMsgBuilder.setDeviceName(deviceName);
        deviceAttributesMsgBuilder.setKv(kvListProto);
        return deviceAttributesMsgBuilder.build();
    }

    private TransportApiProtos.KvListProto getKvListProto(List<String> expectedKeys) {
        List<TransportApiProtos.KeyValueProto> kvProtos = getKvProtos(expectedKeys);
        TransportApiProtos.KvListProto.Builder builder = TransportApiProtos.KvListProto.newBuilder();
        builder.addAllKv(kvProtos);
        return builder.build();
    }
}
