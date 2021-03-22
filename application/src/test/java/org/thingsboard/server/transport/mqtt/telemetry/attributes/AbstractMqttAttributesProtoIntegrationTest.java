/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
package org.thingsboard.server.transport.mqtt.telemetry.attributes;

import com.github.os72.protobuf.dynamic.DynamicSchema;
import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import com.squareup.wire.schema.internal.parser.ProtoFileElement;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Test;
import org.thingsboard.server.common.data.TransportPayloadType;
import org.thingsboard.server.common.data.device.profile.DeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.MqttDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.ProtoTransportPayloadConfiguration;
import org.thingsboard.server.common.data.device.profile.TransportPayloadTypeConfiguration;
import org.thingsboard.server.gen.transport.TransportApiProtos;
import org.thingsboard.server.gen.transport.TransportProtos;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@Slf4j
public abstract class AbstractMqttAttributesProtoIntegrationTest extends AbstractMqttAttributesIntegrationTest {

    private static final String POST_DATA_ATTRIBUTES_TOPIC = "proto/attributes";

    @After
    public void afterTest() throws Exception {
        processAfterTest();
    }

    @Test
    public void testPushMqttAttributes() throws Exception {
        super.processBeforeTest("Test Post Attributes device", "Test Post Attributes gateway", TransportPayloadType.PROTOBUF, null, POST_DATA_ATTRIBUTES_TOPIC);
        List<String> expectedKeys = Arrays.asList("key1", "key2", "key3", "key4", "key5");
        DeviceProfileTransportConfiguration transportConfiguration = deviceProfile.getProfileData().getTransportConfiguration();
        assertTrue(transportConfiguration instanceof MqttDeviceProfileTransportConfiguration);
        MqttDeviceProfileTransportConfiguration mqttTransportConfiguration = (MqttDeviceProfileTransportConfiguration) transportConfiguration;
        TransportPayloadTypeConfiguration transportPayloadTypeConfiguration = mqttTransportConfiguration.getTransportPayloadTypeConfiguration();
        assertTrue(transportPayloadTypeConfiguration instanceof ProtoTransportPayloadConfiguration);
        ProtoTransportPayloadConfiguration protoTransportPayloadConfiguration = (ProtoTransportPayloadConfiguration) transportPayloadTypeConfiguration;
        ProtoFileElement transportProtoSchemaFile = protoTransportPayloadConfiguration.getTransportProtoSchema(DEVICE_ATTRIBUTES_PROTO_SCHEMA);
        DynamicSchema attributesSchema = protoTransportPayloadConfiguration.getDynamicSchema(transportProtoSchemaFile, ProtoTransportPayloadConfiguration.ATTRIBUTES_PROTO_SCHEMA);

        DynamicMessage.Builder nestedJsonObjectBuilder = attributesSchema.newMessageBuilder("PostAttributes.JsonObject.NestedJsonObject");
        Descriptors.Descriptor nestedJsonObjectBuilderDescriptor = nestedJsonObjectBuilder.getDescriptorForType();
        assertNotNull(nestedJsonObjectBuilderDescriptor);
        DynamicMessage nestedJsonObject = nestedJsonObjectBuilder.setField(nestedJsonObjectBuilderDescriptor.findFieldByName("key"), "value").build();

        DynamicMessage.Builder jsonObjectBuilder = attributesSchema.newMessageBuilder("PostAttributes.JsonObject");
        Descriptors.Descriptor jsonObjectBuilderDescriptor = jsonObjectBuilder.getDescriptorForType();
        assertNotNull(jsonObjectBuilderDescriptor);
        DynamicMessage jsonObject = jsonObjectBuilder
                .setField(jsonObjectBuilderDescriptor.findFieldByName("someNumber"), 42)
                .addRepeatedField(jsonObjectBuilderDescriptor.findFieldByName("someArray"), 1)
                .addRepeatedField(jsonObjectBuilderDescriptor.findFieldByName("someArray"), 2)
                .addRepeatedField(jsonObjectBuilderDescriptor.findFieldByName("someArray"), 3)
                .setField(jsonObjectBuilderDescriptor.findFieldByName("someNestedObject"), nestedJsonObject)
                .build();

        DynamicMessage.Builder postAttributesBuilder = attributesSchema.newMessageBuilder("PostAttributes");
        Descriptors.Descriptor postAttributesMsgDescriptor = postAttributesBuilder.getDescriptorForType();
        assertNotNull(postAttributesMsgDescriptor);
        DynamicMessage postAttributesMsg = postAttributesBuilder
                .setField(postAttributesMsgDescriptor.findFieldByName("key1"), "value1")
                .setField(postAttributesMsgDescriptor.findFieldByName("key2"), true)
                .setField(postAttributesMsgDescriptor.findFieldByName("key3"), 3.0)
                .setField(postAttributesMsgDescriptor.findFieldByName("key4"), 4)
                .setField(postAttributesMsgDescriptor.findFieldByName("key5"), jsonObject)
                .build();
        processAttributesTest(POST_DATA_ATTRIBUTES_TOPIC, expectedKeys, postAttributesMsg.toByteArray());
    }

    @Test
    public void testPushMqttAttributesGateway() throws Exception {
        super.processBeforeTest("Test Post Attributes device", "Test Post Attributes gateway", TransportPayloadType.PROTOBUF, null, null);
        TransportApiProtos.GatewayAttributesMsg.Builder gatewayAttributesMsgProtoBuilder = TransportApiProtos.GatewayAttributesMsg.newBuilder();
        List<String> expectedKeys = Arrays.asList("key1", "key2", "key3", "key4", "key5");
        String deviceName1 = "Device A";
        String deviceName2 = "Device B";
        TransportApiProtos.AttributesMsg firstDeviceAttributesMsgProto = getDeviceAttributesMsgProto(deviceName1, expectedKeys);
        TransportApiProtos.AttributesMsg secondDeviceAttributesMsgProto = getDeviceAttributesMsgProto(deviceName2, expectedKeys);
        gatewayAttributesMsgProtoBuilder.addAllMsg(Arrays.asList(firstDeviceAttributesMsgProto, secondDeviceAttributesMsgProto));
        TransportApiProtos.GatewayAttributesMsg gatewayAttributesMsg = gatewayAttributesMsgProtoBuilder.build();
        processGatewayAttributesTest(expectedKeys, gatewayAttributesMsg.toByteArray(), deviceName1, deviceName2);
    }

    private TransportApiProtos.AttributesMsg getDeviceAttributesMsgProto(String deviceName, List<String> expectedKeys) {
        TransportApiProtos.AttributesMsg.Builder deviceAttributesMsgBuilder = TransportApiProtos.AttributesMsg.newBuilder();
        TransportProtos.PostAttributeMsg msg = getPostAttributeMsg(expectedKeys);
        deviceAttributesMsgBuilder.setDeviceName(deviceName);
        deviceAttributesMsgBuilder.setMsg(msg);
        return deviceAttributesMsgBuilder.build();
    }
}
