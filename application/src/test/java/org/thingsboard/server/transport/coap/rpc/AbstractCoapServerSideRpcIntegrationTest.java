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
package org.thingsboard.server.transport.coap.rpc;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.os72.protobuf.dynamic.DynamicSchema;
import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import com.squareup.wire.schema.internal.parser.ProtoFileElement;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapObserveRelation;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.device.profile.CoapDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.CoapDeviceTypeConfiguration;
import org.thingsboard.server.common.data.device.profile.DefaultCoapDeviceTypeConfiguration;
import org.thingsboard.server.common.data.device.profile.DeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.ProtoTransportPayloadConfiguration;
import org.thingsboard.server.common.data.device.profile.TransportPayloadTypeConfiguration;
import org.thingsboard.server.common.msg.session.FeatureType;
import org.thingsboard.server.transport.coap.AbstractCoapIntegrationTest;
import org.thingsboard.server.transport.coap.CoapTestCallback;
import org.thingsboard.server.transport.coap.CoapTestClient;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
public abstract class AbstractCoapServerSideRpcIntegrationTest extends AbstractCoapIntegrationTest {

    public static final  String RPC_REQUEST_PROTO_SCHEMA = "syntax =\"proto3\";\n" +
            "package rpc;\n" +
            "\n" +
            "message RpcRequestMsg {\n" +
            "  optional string method = 1;\n" +
            "  optional int32 requestId = 2;\n" +
            "  Params params = 3;\n" +
            "\n" +
            "  message Params {\n" +
            "      optional string pin = 1;\n" +
            "      optional int32 value = 2;\n" +
            "   }\n" +
            "}";

    protected static final String DEVICE_RESPONSE = "{\"value1\":\"A\",\"value2\":\"B\"}";

    protected static final Long asyncContextTimeoutToUseRpcPlugin = 10000L;

    protected void processOneWayRpcTest(boolean protobuf) throws Exception {
        client = new CoapTestClient(accessToken, FeatureType.RPC);
        CoapTestCallback callbackCoap = new TestCoapCallbackForRPC(client, 1, true, protobuf);

        CoapObserveRelation observeRelation = client.getObserveRelation(callbackCoap);
        String awaitAlias = "await One Way Rpc (client.getObserveRelation)";
        await(awaitAlias)
                .atMost(10, TimeUnit.SECONDS)
                .until(() -> CoAP.ResponseCode.VALID.equals(callbackCoap.getResponseCode()) &&
                        callbackCoap.getObserve() != null &&
                        0 == callbackCoap.getObserve().intValue());
        validateCurrentStateNotification(callbackCoap);
        int expectedObserveCountAfterGpioRequest = callbackCoap.getObserve().intValue() + 1;
        String setGpioRequest = "{\"method\":\"setGpio\",\"params\":{\"pin\": \"23\",\"value\": 1}}";
        String deviceId = savedDevice.getId().getId().toString();
        String result = doPostAsync("/api/rpc/oneway/" + deviceId, setGpioRequest, String.class, status().isOk());
        awaitAlias = "await One Way Rpc setGpio(method, params, value)";
        await(awaitAlias)
                .atMost(10, TimeUnit.SECONDS)
                .until(() -> CoAP.ResponseCode.CONTENT.equals(callbackCoap.getResponseCode()) &&
                        callbackCoap.getObserve() != null &&
                        expectedObserveCountAfterGpioRequest == callbackCoap.getObserve().intValue());
        validateOneWayStateChangedNotification(callbackCoap, result);

        observeRelation.proactiveCancel();
        assertTrue(observeRelation.isCanceled());
    }

    protected void processTwoWayRpcTest(String expectedResponseResult, boolean protobuf) throws Exception {
        client = new CoapTestClient(accessToken, FeatureType.RPC);
        CoapTestCallback callbackCoap = new TestCoapCallbackForRPC(client, 1, false, protobuf);

        CoapObserveRelation observeRelation = client.getObserveRelation(callbackCoap);
        String awaitAlias = "await Two Way Rpc (client.getObserveRelation)";
        await(awaitAlias)
                .atMost(10, TimeUnit.SECONDS)
                .until(() -> CoAP.ResponseCode.VALID.equals(callbackCoap.getResponseCode()) &&
                        callbackCoap.getObserve() != null &&
                        0 == callbackCoap.getObserve().intValue());
        validateCurrentStateNotification(callbackCoap);

        String setGpioRequest = "{\"method\":\"setGpio\",\"params\":{\"pin\": \"26\",\"value\": 1}}";
        String deviceId = savedDevice.getId().getId().toString();
        int expectedObserveCountAfterGpioRequest1 = callbackCoap.getObserve().intValue() + 1;
        String actualResult = doPostAsync("/api/rpc/twoway/" + deviceId, setGpioRequest, String.class, status().isOk());
        awaitAlias = "await Two Way Rpc (setGpio(method, params, value) first";
        await(awaitAlias)
                .atMost(10, TimeUnit.SECONDS)
                .until(() -> CoAP.ResponseCode.CONTENT.equals(callbackCoap.getResponseCode()) &&
                        callbackCoap.getObserve() != null &&
                        expectedObserveCountAfterGpioRequest1 == callbackCoap.getObserve().intValue());
        validateTwoWayStateChangedNotification(callbackCoap, expectedResponseResult, actualResult);

        int expectedObserveCountAfterGpioRequest2 = callbackCoap.getObserve().intValue() + 1;
        actualResult = doPostAsync("/api/rpc/twoway/" + deviceId, setGpioRequest, String.class, status().isOk());
        awaitAlias = "await Two Way Rpc (setGpio(method, params, value) first";
        await(awaitAlias)
                .atMost(10, TimeUnit.SECONDS)
                .until(() -> CoAP.ResponseCode.CONTENT.equals(callbackCoap.getResponseCode()) &&
                        callbackCoap.getObserve() != null &&
                        expectedObserveCountAfterGpioRequest2 == callbackCoap.getObserve().intValue());

        validateTwoWayStateChangedNotification(callbackCoap, expectedResponseResult, actualResult);

        observeRelation.proactiveCancel();
        assertTrue(observeRelation.isCanceled());
    }

    protected void processOnLoadResponse(CoapResponse response, CoapTestClient client, Integer observe, CountDownLatch latch) {
        JsonNode responseJson = JacksonUtil.fromBytes(response.getPayload());
        client.setURI(CoapTestClient.getFeatureTokenUrl(accessToken, FeatureType.RPC, responseJson.get("id").asInt()));
        client.postMethod(new CoapHandler() {
            @Override
            public void onLoad(CoapResponse response) {
                log.warn("Command Response Ack: {}, {}", response.getCode(), response.getResponseText());
                latch.countDown();
            }

            @Override
            public void onError() {
                log.warn("Command Response Ack Error, No connect");
            }
        }, DEVICE_RESPONSE, MediaTypeRegistry.APPLICATION_JSON);
    }

    protected void processOnLoadProtoResponse(CoapResponse response, CoapTestClient client, Integer observe, CountDownLatch latch) {
        ProtoTransportPayloadConfiguration protoTransportPayloadConfiguration = getProtoTransportPayloadConfiguration();
        ProtoFileElement rpcRequestProtoSchemaFile = protoTransportPayloadConfiguration.getTransportProtoSchema(RPC_REQUEST_PROTO_SCHEMA);
        DynamicSchema rpcRequestProtoSchema = protoTransportPayloadConfiguration.getDynamicSchema(rpcRequestProtoSchemaFile, ProtoTransportPayloadConfiguration.RPC_REQUEST_PROTO_SCHEMA);

        byte[] requestPayload = response.getPayload();
        DynamicMessage.Builder rpcRequestMsg = rpcRequestProtoSchema.newMessageBuilder("RpcRequestMsg");
        Descriptors.Descriptor rpcRequestMsgDescriptor = rpcRequestMsg.getDescriptorForType();
        try {
            DynamicMessage dynamicMessage = DynamicMessage.parseFrom(rpcRequestMsgDescriptor, requestPayload);
            Descriptors.FieldDescriptor requestIdDescriptor = rpcRequestMsgDescriptor.findFieldByName("requestId");
            int requestId = (int) dynamicMessage.getField(requestIdDescriptor);
            ProtoFileElement rpcResponseProtoSchemaFile = protoTransportPayloadConfiguration.getTransportProtoSchema(DEVICE_RPC_RESPONSE_PROTO_SCHEMA);
            DynamicSchema rpcResponseProtoSchema = protoTransportPayloadConfiguration.getDynamicSchema(rpcResponseProtoSchemaFile, ProtoTransportPayloadConfiguration.RPC_RESPONSE_PROTO_SCHEMA);
            DynamicMessage.Builder rpcResponseBuilder = rpcResponseProtoSchema.newMessageBuilder("RpcResponseMsg");
            Descriptors.Descriptor rpcResponseMsgDescriptor = rpcResponseBuilder.getDescriptorForType();
            DynamicMessage rpcResponseMsg = rpcResponseBuilder
                    .setField(rpcResponseMsgDescriptor.findFieldByName("payload"), DEVICE_RESPONSE)
                    .build();
            client.setURI(CoapTestClient.getFeatureTokenUrl(accessToken, FeatureType.RPC, requestId));
            client.postMethod(new CoapHandler() {
                @Override
                public void onLoad(CoapResponse response) {
                    log.warn("Command Response Ack: {}", response.getCode());
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

    private void validateCurrentStateNotification(CoapTestCallback callback) {
        assertArrayEquals(EMPTY_PAYLOAD, callback.getPayloadBytes());
    }

    private void validateOneWayStateChangedNotification(CoapTestCallback callback, String result) {
        assertTrue(StringUtils.isEmpty(result));
        assertNotNull(callback.getPayloadBytes());
    }

    private void validateTwoWayStateChangedNotification(CoapTestCallback callback, String expectedResult, String actualResult) {
        assertEquals(expectedResult, actualResult);
        assertNotNull(callback.getPayloadBytes());
    }

    protected class TestCoapCallbackForRPC extends CoapTestCallback {

        private final CoapTestClient client;
        private final boolean isOneWayRpc;
        private final boolean protobuf;

        TestCoapCallbackForRPC(CoapTestClient client, int subscribeCount, boolean isOneWayRpc, boolean protobuf) {
            super(subscribeCount);
            this.client = client;
            this.isOneWayRpc = isOneWayRpc;
            this.protobuf = protobuf;
        }

        @Override
        public void onLoad(CoapResponse response) {
            payloadBytes = response.getPayload();
            responseCode = response.getCode();
            observe = response.getOptions().getObserve();
            if (observe != null) {
                if (!isOneWayRpc && observe > 0) {
                    if (!protobuf){
                        processOnLoadResponse(response, client, observe, latch);
                    } else {
                        processOnLoadProtoResponse(response, client, observe, latch);
                    }
                } else {
                    latch.countDown();
                }
            }
        }

        @Override
        public void onError() {
            log.warn("Command Response Ack Error, No connect");
        }
    }
}
