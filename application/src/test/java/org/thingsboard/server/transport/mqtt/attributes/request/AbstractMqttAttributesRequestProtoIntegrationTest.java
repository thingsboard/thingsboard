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
package org.thingsboard.server.transport.mqtt.attributes.request;

import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Test;
import org.thingsboard.server.common.data.DeviceProfileProvisionType;
import org.thingsboard.server.common.data.TransportPayloadType;
import org.thingsboard.server.common.data.device.profile.MqttTopics;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.transport.mqtt.attributes.AbstractMqttAttributesIntegrationTest;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public abstract class AbstractMqttAttributesRequestProtoIntegrationTest extends AbstractMqttAttributesIntegrationTest {

    @After
    public void afterTest() throws Exception {
        processAfterTest();
    }

    @Test
    public void testRequestAttributesValuesFromTheServer() throws Exception {
        super.processBeforeTest("Test Request attribute values from the server proto", "Gateway Test Request attribute values from the server proto",
                TransportPayloadType.PROTOBUF, null, null, null, ATTRIBUTES_SCHEMA_STR, null, null, null, null, DeviceProfileProvisionType.DISABLED, false, false);
        processProtoTestRequestAttributesValuesFromTheServer(MqttTopics.DEVICE_ATTRIBUTES_TOPIC, MqttTopics.DEVICE_ATTRIBUTES_RESPONSES_TOPIC, MqttTopics.DEVICE_ATTRIBUTES_REQUEST_TOPIC_PREFIX);
    }

    @Test
    public void testRequestAttributesValuesFromTheServerOnShortTopic() throws Exception {
        super.processBeforeTest("Test Request attribute values from the server proto", "Gateway Test Request attribute values from the server proto",
                TransportPayloadType.PROTOBUF, null, null, null, ATTRIBUTES_SCHEMA_STR, null, null, null, null, DeviceProfileProvisionType.DISABLED, false, false);
        processProtoTestRequestAttributesValuesFromTheServer(MqttTopics.DEVICE_ATTRIBUTES_SHORT_TOPIC, MqttTopics.DEVICE_ATTRIBUTES_RESPONSES_SHORT_TOPIC, MqttTopics.DEVICE_ATTRIBUTES_REQUEST_SHORT_TOPIC_PREFIX);
    }

    @Test
    public void testRequestAttributesValuesFromTheServerOnShortProtoTopic() throws Exception {
        super.processBeforeTest("Test Request attribute values from the server proto", "Gateway Test Request attribute values from the server proto",
                TransportPayloadType.PROTOBUF, null, null, null, ATTRIBUTES_SCHEMA_STR, null, null, null, null, DeviceProfileProvisionType.DISABLED, false, false);
        processProtoTestRequestAttributesValuesFromTheServer(MqttTopics.DEVICE_ATTRIBUTES_SHORT_PROTO_TOPIC, MqttTopics.DEVICE_ATTRIBUTES_RESPONSES_SHORT_PROTO_TOPIC, MqttTopics.DEVICE_ATTRIBUTES_REQUEST_SHORT_PROTO_TOPIC_PREFIX);
    }

    @Test
    public void testRequestAttributesValuesFromTheServerGateway() throws Exception {
        super.processBeforeTest("Test Request attribute values from the server proto", "Gateway Test Request attribute values from the server proto", TransportPayloadType.PROTOBUF, null, null);
        processProtoTestGatewayRequestAttributesValuesFromTheServer();
    }

    @Test
    public void testRequestAttributesValuesFromTheServerOnShortJsonTopic() throws Exception {
        super.processBeforeTest("Test Request attribute values from the server proto", "Gateway Test Request attribute values from the server proto", TransportPayloadType.PROTOBUF, null, null);
        processJsonTestRequestAttributesValuesFromTheServer(MqttTopics.DEVICE_ATTRIBUTES_SHORT_JSON_TOPIC, MqttTopics.DEVICE_ATTRIBUTES_RESPONSES_SHORT_JSON_TOPIC, MqttTopics.DEVICE_ATTRIBUTES_REQUEST_SHORT_JSON_TOPIC_PREFIX);
    }


    protected List<TransportProtos.KeyValueProto> getKvProtos(List<String> expectedKeys) {
        List<TransportProtos.KeyValueProto> keyValueProtos = new ArrayList<>();
        TransportProtos.KeyValueProto strKeyValueProto = getKeyValueProto(expectedKeys.get(0), "value1", TransportProtos.KeyValueType.STRING_V);
        TransportProtos.KeyValueProto boolKeyValueProto = getKeyValueProto(expectedKeys.get(1), "true", TransportProtos.KeyValueType.BOOLEAN_V);
        TransportProtos.KeyValueProto dblKeyValueProto = getKeyValueProto(expectedKeys.get(2), "42.0", TransportProtos.KeyValueType.DOUBLE_V);
        TransportProtos.KeyValueProto longKeyValueProto = getKeyValueProto(expectedKeys.get(3), "73", TransportProtos.KeyValueType.LONG_V);
        TransportProtos.KeyValueProto jsonKeyValueProto = getKeyValueProto(expectedKeys.get(4), "{\"someNumber\": 42, \"someArray\": [1,2,3], \"someNestedObject\": {\"key\": \"value\"}}", TransportProtos.KeyValueType.JSON_V);
        keyValueProtos.add(strKeyValueProto);
        keyValueProtos.add(boolKeyValueProto);
        keyValueProtos.add(dblKeyValueProto);
        keyValueProtos.add(longKeyValueProto);
        keyValueProtos.add(jsonKeyValueProto);
        return keyValueProtos;
    }

}
