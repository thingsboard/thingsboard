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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.TransportPayloadType;
import org.thingsboard.server.common.data.device.profile.MqttTopics;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.gen.transport.TransportApiProtos;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.mqtt.telemetry.AbstractMqttTelemetryIntegrationTest;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@Slf4j
public abstract class AbstractMqttAttributesProtoIntegrationTest extends AbstractMqttAttributesIntegrationTest {

    private static final String POST_DATA_ATTRIBUTES_TOPIC = "proto/attributes";

    @Before
    public void beforeTest() throws Exception {
        processBeforeTest("Test Post Attributes device", "Test Post Attributes gateway", TransportPayloadType.PROTOBUF, null, POST_DATA_ATTRIBUTES_TOPIC);
    }

    @After
    public void afterTest() throws Exception {
        processAfterTest();
    }

    @Test
    public void testPushMqttAttributes() throws Exception {
        List<String> expectedKeys = Arrays.asList("key1", "key2", "key3", "key4", "key5");
        TransportProtos.PostAttributeMsg msg = getPostAttributeMsg(expectedKeys);
        processAttributesTest(POST_DATA_ATTRIBUTES_TOPIC, expectedKeys, msg.toByteArray());
    }

    @Test
    public void testPushMqttAttributesGateway() throws Exception {
        TransportApiProtos.GatewayAttributesMsg.Builder gatewayAttributesMsgProtoBuilder = TransportApiProtos.GatewayAttributesMsg.newBuilder();
        List<String> expectedKeys = Arrays.asList("key1", "key2", "key3", "key4", "key5");
        String deviceName1 = "Device A";
        String deviceName2 = "Device B";
        TransportApiProtos.AttributesMsg firstDeviceAttributesMsgProto = getDeviceAttributesMsgProto(deviceName1, expectedKeys);
        TransportApiProtos.AttributesMsg secondDeviceAttributesMsgProto = getDeviceAttributesMsgProto(deviceName2, expectedKeys);
        gatewayAttributesMsgProtoBuilder.addAllMsg(Arrays.asList(firstDeviceAttributesMsgProto, secondDeviceAttributesMsgProto));
        TransportApiProtos.GatewayAttributesMsg gatewayAttributesMsg = gatewayAttributesMsgProtoBuilder.build();
        processGatewayAttributesTest(MqttTopics.GATEWAY_ATTRIBUTES_TOPIC, expectedKeys, gatewayAttributesMsg.toByteArray(), deviceName1, deviceName2);
    }

    private TransportApiProtos.AttributesMsg getDeviceAttributesMsgProto(String deviceName, List<String> expectedKeys) {
        TransportApiProtos.AttributesMsg.Builder deviceAttributesMsgBuilder = TransportApiProtos.AttributesMsg.newBuilder();
        TransportProtos.PostAttributeMsg msg = getPostAttributeMsg(expectedKeys);
        deviceAttributesMsgBuilder.setDeviceName(deviceName);
        deviceAttributesMsgBuilder.setMsg(msg);
        return deviceAttributesMsgBuilder.build();
    }

    private TransportProtos.PostAttributeMsg getPostAttributeMsg(List<String> expectedKeys) {
        List<TransportProtos.KeyValueProto> kvProtos = getKvProtos(expectedKeys);
        TransportProtos.PostAttributeMsg.Builder builder = TransportProtos.PostAttributeMsg.newBuilder();
        builder.addAllKv(kvProtos);
        return builder.build();
    }
}
