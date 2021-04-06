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
package org.thingsboard.server.transport.coap.attributes.request;

import com.github.os72.protobuf.dynamic.DynamicSchema;
import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import com.squareup.wire.schema.internal.parser.ProtoFileElement;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.junit.After;
import org.junit.Test;
import org.thingsboard.server.common.data.CoapDeviceType;
import org.thingsboard.server.common.data.DeviceProfileProvisionType;
import org.thingsboard.server.common.data.TransportPayloadType;
import org.thingsboard.server.common.data.device.profile.CoapDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.CoapDeviceTypeConfiguration;
import org.thingsboard.server.common.data.device.profile.DefaultCoapDeviceTypeConfiguration;
import org.thingsboard.server.common.data.device.profile.DeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.ProtoTransportPayloadConfiguration;
import org.thingsboard.server.common.data.device.profile.TransportPayloadTypeConfiguration;
import org.thingsboard.server.common.msg.session.FeatureType;
import org.thingsboard.server.gen.transport.TransportProtos;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
public abstract class AbstractCoapAttributesRequestProtoIntegrationTest extends AbstractCoapAttributesRequestIntegrationTest {

    public static final String ATTRIBUTES_SCHEMA_STR = "syntax =\"proto3\";\n" +
            "\n" +
            "package test;\n" +
            "\n" +
            "message PostAttributes {\n" +
            "  string attribute1 = 1;\n" +
            "  bool attribute2 = 2;\n" +
            "  double attribute3 = 3;\n" +
            "  int32 attribute4 = 4;\n" +
            "  JsonObject attribute5 = 5;\n" +
            "\n" +
            "  message JsonObject {\n" +
            "    int32 someNumber = 6;\n" +
            "    repeated int32 someArray = 7;\n" +
            "    NestedJsonObject someNestedObject = 8;\n" +
            "    message NestedJsonObject {\n" +
            "       string key = 9;\n" +
            "    }\n" +
            "  }\n" +
            "}";

    @After
    public void afterTest() throws Exception {
        processAfterTest();
    }

    @Test
    public void testRequestAttributesValuesFromTheServer() throws Exception {
        super.processBeforeTest("Test Request attribute values from the server proto", CoapDeviceType.DEFAULT,
                TransportPayloadType.PROTOBUF, null, ATTRIBUTES_SCHEMA_STR, null, DeviceProfileProvisionType.DISABLED, null, null);
        processTestRequestAttributesValuesFromTheServer();
    }

    protected void postAttributes() throws Exception {
        doPostAsync("/api/plugins/telemetry/DEVICE/" + savedDevice.getId().getId() + "/attributes/SHARED_SCOPE", POST_ATTRIBUTES_PAYLOAD, String.class, status().isOk());
        DeviceProfileTransportConfiguration transportConfiguration = deviceProfile.getProfileData().getTransportConfiguration();
        assertTrue(transportConfiguration instanceof CoapDeviceProfileTransportConfiguration);
        CoapDeviceProfileTransportConfiguration coapTransportConfiguration = (CoapDeviceProfileTransportConfiguration) transportConfiguration;
        CoapDeviceTypeConfiguration coapDeviceTypeConfiguration = coapTransportConfiguration.getCoapDeviceTypeConfiguration();
        assertTrue(coapDeviceTypeConfiguration instanceof DefaultCoapDeviceTypeConfiguration);
        DefaultCoapDeviceTypeConfiguration defaultCoapDeviceTypeConfiguration = (DefaultCoapDeviceTypeConfiguration) coapDeviceTypeConfiguration;
        TransportPayloadTypeConfiguration transportPayloadTypeConfiguration = defaultCoapDeviceTypeConfiguration.getTransportPayloadTypeConfiguration();
        assertTrue(transportPayloadTypeConfiguration instanceof ProtoTransportPayloadConfiguration);
        ProtoTransportPayloadConfiguration protoTransportPayloadConfiguration = (ProtoTransportPayloadConfiguration) transportPayloadTypeConfiguration;
        ProtoFileElement transportProtoSchema = protoTransportPayloadConfiguration.getTransportProtoSchema(ATTRIBUTES_SCHEMA_STR);
        DynamicSchema attributesSchema = protoTransportPayloadConfiguration.getDynamicSchema(transportProtoSchema, ProtoTransportPayloadConfiguration.ATTRIBUTES_PROTO_SCHEMA);

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
                .setField(postAttributesMsgDescriptor.findFieldByName("attribute1"), "value1")
                .setField(postAttributesMsgDescriptor.findFieldByName("attribute2"), true)
                .setField(postAttributesMsgDescriptor.findFieldByName("attribute3"), 42.0)
                .setField(postAttributesMsgDescriptor.findFieldByName("attribute4"), 73)
                .setField(postAttributesMsgDescriptor.findFieldByName("attribute5"), jsonObject)
                .build();
        byte[] payload = postAttributesMsg.toByteArray();
        CoapClient client = getCoapClient(FeatureType.ATTRIBUTES);
        CoapResponse coapResponse = client.setTimeout(CLIENT_REQUEST_TIMEOUT).post(payload, MediaTypeRegistry.APPLICATION_JSON);
        assertEquals(CoAP.ResponseCode.CREATED, coapResponse.getCode());
    }

    protected void validateResponse(CoapResponse getAttributesResponse) throws InvalidProtocolBufferException {
        TransportProtos.GetAttributeResponseMsg expectedAttributesResponse = getExpectedAttributeResponseMsg();
        TransportProtos.GetAttributeResponseMsg actualAttributesResponse = TransportProtos.GetAttributeResponseMsg.parseFrom(getAttributesResponse.getPayload());
        assertEquals(expectedAttributesResponse.getRequestId(), actualAttributesResponse.getRequestId());
        List<TransportProtos.KeyValueProto> expectedClientKeyValueProtos = expectedAttributesResponse.getClientAttributeListList().stream().map(TransportProtos.TsKvProto::getKv).collect(Collectors.toList());
        List<TransportProtos.KeyValueProto> expectedSharedKeyValueProtos = expectedAttributesResponse.getSharedAttributeListList().stream().map(TransportProtos.TsKvProto::getKv).collect(Collectors.toList());
        List<TransportProtos.KeyValueProto> actualClientKeyValueProtos = actualAttributesResponse.getClientAttributeListList().stream().map(TransportProtos.TsKvProto::getKv).collect(Collectors.toList());
        List<TransportProtos.KeyValueProto> actualSharedKeyValueProtos = actualAttributesResponse.getSharedAttributeListList().stream().map(TransportProtos.TsKvProto::getKv).collect(Collectors.toList());
        assertTrue(actualClientKeyValueProtos.containsAll(expectedClientKeyValueProtos));
        assertTrue(actualSharedKeyValueProtos.containsAll(expectedSharedKeyValueProtos));
    }

    private TransportProtos.GetAttributeResponseMsg getExpectedAttributeResponseMsg() {
        TransportProtos.GetAttributeResponseMsg.Builder result = TransportProtos.GetAttributeResponseMsg.newBuilder();
        List<TransportProtos.TsKvProto> tsKvProtoList = getTsKvProtoList();
        result.addAllClientAttributeList(tsKvProtoList);
        result.addAllSharedAttributeList(tsKvProtoList);
        result.setRequestId(0);
        return result.build();
    }

}
