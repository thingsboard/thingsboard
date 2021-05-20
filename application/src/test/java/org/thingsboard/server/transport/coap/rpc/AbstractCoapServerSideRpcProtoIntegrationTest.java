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
package org.thingsboard.server.transport.coap.rpc;

import com.github.os72.protobuf.dynamic.DynamicSchema;
import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import com.squareup.wire.schema.internal.parser.ProtoFileElement;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapObserveRelation;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.coap.Request;
import org.junit.After;
import org.junit.Before;
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

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
public abstract class AbstractCoapServerSideRpcProtoIntegrationTest extends AbstractCoapServerSideRpcIntegrationTest {

    private static final  String RPC_REQUEST_PROTO_SCHEMA = "syntax =\"proto3\";\n" +
            "package rpc;\n" +
            "\n" +
            "message RpcRequestMsg {\n" +
            "  string method = 1;\n" +
            "  int32 requestId = 2;\n" +
            "  Params params = 3;\n" +
            "\n" +
            "  message Params {\n" +
            "      string pin = 1;\n" +
            "      int32 value = 2;\n" +
            "   }\n" +
            "}";

    @Before
    public void beforeTest() throws Exception {
        processBeforeTest("RPC test device", CoapDeviceType.DEFAULT, TransportPayloadType.PROTOBUF, null, null, null, RPC_REQUEST_PROTO_SCHEMA, null, null, DeviceProfileProvisionType.DISABLED);
    }

    @After
    public void afterTest() throws Exception {
        super.processAfterTest();
    }

    @Test
    public void testServerCoapOneWayRpc() throws Exception {
        processOneWayRpcTest();
    }

    @Test
    public void testServerCoapTwoWayRpc() throws Exception {
        processTwoWayRpcTest();
    }

    protected void processTwoWayRpcTest() throws Exception {
        CoapClient client = getCoapClient(FeatureType.RPC);
        client.useCONs();

        CountDownLatch latch = new CountDownLatch(1);
        TestCoapCallback testCoapCallback = new TestCoapCallback(client, latch, false);

        Request request = Request.newGet().setObserve();
        request.setType(CoAP.Type.CON);
        CoapObserveRelation observeRelation = client.observe(request, testCoapCallback);

        String setGpioRequest = "{\"method\":\"setGpio\",\"params\":{\"pin\": \"26\",\"value\": 1}}";
        String deviceId = savedDevice.getId().getId().toString();

        String expected = "{\"payload\":\"{\\\"value1\\\":\\\"A\\\",\\\"value2\\\":\\\"B\\\"}\"}";

        String result = doPostAsync("/api/plugins/rpc/twoway/" + deviceId, setGpioRequest, String.class, status().isOk());
        latch.await(3, TimeUnit.SECONDS);

        assertEquals(expected, result);
        assertEquals(0, testCoapCallback.getObserve().intValue());
        observeRelation.proactiveCancel();
        assertTrue(observeRelation.isCanceled());
    }

    @Override
    protected void processOnLoadResponse(CoapResponse response, CoapClient client, Integer observe, CountDownLatch latch) {
        client.setURI(getRpcResponseFeatureTokenUrl(accessToken, observe));
        ProtoTransportPayloadConfiguration protoTransportPayloadConfiguration = getProtoTransportPayloadConfiguration();
        ProtoFileElement rpcRequestProtoSchemaFile = protoTransportPayloadConfiguration.getTransportProtoSchema(RPC_REQUEST_PROTO_SCHEMA);
        DynamicSchema rpcRequestProtoSchema = protoTransportPayloadConfiguration.getDynamicSchema(rpcRequestProtoSchemaFile, ProtoTransportPayloadConfiguration.RPC_REQUEST_PROTO_SCHEMA);

        byte[] requestPayload = response.getPayload();
        DynamicMessage.Builder rpcRequestMsg = rpcRequestProtoSchema.newMessageBuilder("RpcRequestMsg");
        Descriptors.Descriptor rpcRequestMsgDescriptor = rpcRequestMsg.getDescriptorForType();
        assertNotNull(rpcRequestMsgDescriptor);
        try {
            DynamicMessage dynamicMessage = DynamicMessage.parseFrom(rpcRequestMsgDescriptor, requestPayload);
            List<Descriptors.FieldDescriptor> fields = rpcRequestMsgDescriptor.getFields();
            for (Descriptors.FieldDescriptor fieldDescriptor: fields) {
                assertTrue(dynamicMessage.hasField(fieldDescriptor));
            }
            ProtoFileElement rpcResponseProtoSchemaFile = protoTransportPayloadConfiguration.getTransportProtoSchema(DEVICE_RPC_RESPONSE_PROTO_SCHEMA);
            DynamicSchema rpcResponseProtoSchema = protoTransportPayloadConfiguration.getDynamicSchema(rpcResponseProtoSchemaFile, ProtoTransportPayloadConfiguration.RPC_RESPONSE_PROTO_SCHEMA);
            DynamicMessage.Builder rpcResponseBuilder = rpcResponseProtoSchema.newMessageBuilder("RpcResponseMsg");
            Descriptors.Descriptor rpcResponseMsgDescriptor = rpcResponseBuilder.getDescriptorForType();
            assertNotNull(rpcResponseMsgDescriptor);
            DynamicMessage rpcResponseMsg = rpcResponseBuilder
                    .setField(rpcResponseMsgDescriptor.findFieldByName("payload"), DEVICE_RESPONSE)
                    .build();
            client.post(new CoapHandler() {
                @Override
                public void onLoad(CoapResponse response) {
                    log.warn("Command Response Ack: {}, {}", response.getCode(), response.getResponseText());
                    latch.countDown();
                }

                @Override
                public void onError() {
                    log.warn("Command Response Ack Error, No connect");
                }
            }, rpcResponseMsg.toByteArray(), MediaTypeRegistry.APPLICATION_JSON);
        } catch (InvalidProtocolBufferException e) {
            log.warn("Command Response Ack Error, Invalid response received: ", e);
        }
    }

    private ProtoTransportPayloadConfiguration getProtoTransportPayloadConfiguration() {
        DeviceProfileTransportConfiguration transportConfiguration = deviceProfile.getProfileData().getTransportConfiguration();
        assertTrue(transportConfiguration instanceof CoapDeviceProfileTransportConfiguration);
        CoapDeviceProfileTransportConfiguration coapDeviceProfileTransportConfiguration = (CoapDeviceProfileTransportConfiguration) transportConfiguration;
        CoapDeviceTypeConfiguration coapDeviceTypeConfiguration = coapDeviceProfileTransportConfiguration.getCoapDeviceTypeConfiguration();
        assertTrue(coapDeviceTypeConfiguration instanceof DefaultCoapDeviceTypeConfiguration);
        DefaultCoapDeviceTypeConfiguration defaultCoapDeviceTypeConfiguration = (DefaultCoapDeviceTypeConfiguration) coapDeviceTypeConfiguration;
        TransportPayloadTypeConfiguration transportPayloadTypeConfiguration = defaultCoapDeviceTypeConfiguration.getTransportPayloadTypeConfiguration();
        assertTrue(transportPayloadTypeConfiguration instanceof ProtoTransportPayloadConfiguration);
        return (ProtoTransportPayloadConfiguration) transportPayloadTypeConfiguration;
    }
}
