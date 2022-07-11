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
package org.thingsboard.server.transport.mqtt.rpc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.os72.protobuf.dynamic.DynamicSchema;
import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import com.nimbusds.jose.util.StandardCharset;
import com.squareup.wire.schema.internal.parser.ProtoFileElement;
import io.netty.handler.codec.mqtt.MqttQoS;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.StringUtils;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.Assert;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.TransportPayloadType;
import org.thingsboard.server.common.data.device.profile.DeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.MqttDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.MqttTopics;
import org.thingsboard.server.common.data.device.profile.ProtoTransportPayloadConfiguration;
import org.thingsboard.server.common.data.device.profile.TransportPayloadTypeConfiguration;
import org.thingsboard.server.gen.transport.TransportApiProtos;
import org.thingsboard.server.transport.mqtt.AbstractMqttIntegrationTest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Valerii Sosliuk
 */
@Slf4j
public abstract class AbstractMqttServerSideRpcIntegrationTest extends AbstractMqttIntegrationTest {

    protected static final  String RPC_REQUEST_PROTO_SCHEMA = "syntax =\"proto3\";\n" +
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

    private static final String DEVICE_RESPONSE = "{\"value1\":\"A\",\"value2\":\"B\"}";

    protected Long asyncContextTimeoutToUseRpcPlugin;

    protected void processBeforeTest(String deviceName, String gatewayName, TransportPayloadType payloadType, String telemetryTopic, String attributesTopic) throws Exception {
        super.processBeforeTest(deviceName, gatewayName, payloadType, telemetryTopic, attributesTopic);
        asyncContextTimeoutToUseRpcPlugin = 10000L;
    }

    protected void processOneWayRpcTest(String rpcSubTopic) throws Exception {
        MqttAsyncClient client = getMqttAsyncClient(accessToken);

        CountDownLatch latch = new CountDownLatch(1);
        TestOneWayMqttCallback callback = new TestOneWayMqttCallback(client, latch);
        client.setCallback(callback);

        client.subscribe(rpcSubTopic, MqttQoS.AT_MOST_ONCE.value());

        Thread.sleep(1000);

        String setGpioRequest = "{\"method\":\"setGpio\",\"params\":{\"pin\": \"23\",\"value\": 1}}";
        String deviceId = savedDevice.getId().getId().toString();
        String result = doPostAsync("/api/rpc/oneway/" + deviceId, setGpioRequest, String.class, status().isOk());
        Assert.assertTrue(StringUtils.isEmpty(result));
        latch.await(3, TimeUnit.SECONDS);
        assertEquals(MqttQoS.AT_MOST_ONCE.value(), callback.getQoS());
    }

    protected void processJsonOneWayRpcTestGateway(String deviceName) throws Exception {
        MqttAsyncClient client = getMqttAsyncClient(gatewayAccessToken);
        String payload = "{\"device\":\"" + deviceName + "\"}";
        byte[] payloadBytes = payload.getBytes();
        validateOneWayRpcGatewayResponse(deviceName, client, payloadBytes);
    }

    protected void processJsonTwoWayRpcTest(String rpcSubTopic) throws Exception {
        MqttAsyncClient client = getMqttAsyncClient(accessToken);
        client.subscribe(rpcSubTopic, 1);

        CountDownLatch latch = new CountDownLatch(1);
        TestJsonMqttCallback callback = new TestJsonMqttCallback(client, latch);
        client.setCallback(callback);

        Thread.sleep(1000);

        String setGpioRequest = "{\"method\":\"setGpio\",\"params\":{\"pin\": \"26\",\"value\": 1}}";
        String deviceId = savedDevice.getId().getId().toString();

        String result = doPostAsync("/api/rpc/twoway/" + deviceId, setGpioRequest, String.class, status().isOk());
        String expected = "{\"value1\":\"A\",\"value2\":\"B\"}";
        latch.await(3, TimeUnit.SECONDS);
        Assert.assertEquals(expected, result);
    }

    protected void processProtoTwoWayRpcTest(String rpcSubTopic) throws Exception {
        MqttAsyncClient client = getMqttAsyncClient(accessToken);
        client.subscribe(rpcSubTopic, 1);

        CountDownLatch latch = new CountDownLatch(1);
        TestProtoMqttCallback callback = new TestProtoMqttCallback(client, latch);
        client.setCallback(callback);

        Thread.sleep(1000);

        String setGpioRequest = "{\"method\":\"setGpio\",\"params\":{\"pin\": \"26\",\"value\": 1}}";
        String deviceId = savedDevice.getId().getId().toString();

        String result = doPostAsync("/api/rpc/twoway/" + deviceId, setGpioRequest, String.class, status().isOk());
        String expected = "{\"payload\":\"{\\\"value1\\\":\\\"A\\\",\\\"value2\\\":\\\"B\\\"}\"}";
        latch.await(3, TimeUnit.SECONDS);
        Assert.assertEquals(expected, result);
    }

    protected void processProtoTwoWayRpcTestGateway(String deviceName) throws Exception {
        MqttAsyncClient client = getMqttAsyncClient(gatewayAccessToken);
        TransportApiProtos.ConnectMsg connectMsgProto = getConnectProto(deviceName);
        byte[] payloadBytes = connectMsgProto.toByteArray();
        validateProtoTwoWayRpcGatewayResponse(deviceName, client, payloadBytes);
    }

    protected void processProtoOneWayRpcTestGateway(String deviceName) throws Exception {
        MqttAsyncClient client = getMqttAsyncClient(gatewayAccessToken);
        TransportApiProtos.ConnectMsg connectMsgProto = getConnectProto(deviceName);
        byte[] payloadBytes = connectMsgProto.toByteArray();
        validateOneWayRpcGatewayResponse(deviceName, client, payloadBytes);
    }

    private TransportApiProtos.ConnectMsg getConnectProto(String deviceName) {
        TransportApiProtos.ConnectMsg.Builder builder = TransportApiProtos.ConnectMsg.newBuilder();
        builder.setDeviceName(deviceName);
        builder.setDeviceType(TransportPayloadType.PROTOBUF.name());
        return builder.build();
    }

    protected void processSequenceTwoWayRpcTest() throws Exception {
        List<String> expected = new ArrayList<>();
        List<String> result = new ArrayList<>();

        String deviceId = savedDevice.getId().getId().toString();

        for (int i = 0; i < 10; i++) {
            ObjectNode request = JacksonUtil.newObjectNode();
            request.put("method", "test");
            request.put("params", i);
            expected.add(JacksonUtil.toString(request));
            request.put("persistent", true);
            doPostAsync("/api/rpc/twoway/" + deviceId, JacksonUtil.toString(request), String.class, status().isOk());
        }

        MqttAsyncClient client = getMqttAsyncClient(accessToken);
        client.setManualAcks(true);
        CountDownLatch latch = new CountDownLatch(10);
        TestSequenceMqttCallback callback = new TestSequenceMqttCallback(client, latch, result);
        client.setCallback(callback);
        client.subscribe(MqttTopics.DEVICE_RPC_REQUESTS_SUB_TOPIC, 1);

        latch.await(10, TimeUnit.SECONDS);
        Assert.assertEquals(expected, result);
    }

    protected void processJsonTwoWayRpcTestGateway(String deviceName) throws Exception {
        MqttAsyncClient client = getMqttAsyncClient(gatewayAccessToken);

        String payload = "{\"device\":\"" + deviceName + "\"}";
        byte[] payloadBytes = payload.getBytes();

        validateJsonTwoWayRpcGatewayResponse(deviceName, client, payloadBytes);
    }

    protected void validateOneWayRpcGatewayResponse(String deviceName, MqttAsyncClient client, byte[] payloadBytes) throws Exception {
        publishMqttMsg(client, payloadBytes, MqttTopics.GATEWAY_CONNECT_TOPIC);

        Device savedDevice = doExecuteWithRetriesAndInterval(
                () -> getDeviceByName(deviceName),
                20,
                100
        );
        assertNotNull(savedDevice);

        CountDownLatch latch = new CountDownLatch(1);
        TestOneWayMqttCallback callback = new TestOneWayMqttCallback(client, latch);
        client.setCallback(callback);

        client.subscribe(MqttTopics.GATEWAY_RPC_TOPIC, MqttQoS.AT_MOST_ONCE.value());

        Thread.sleep(1000);

        String setGpioRequest = "{\"method\": \"toggle_gpio\", \"params\": {\"pin\":1}}";
        String deviceId = savedDevice.getId().getId().toString();
        String result = doPostAsync("/api/rpc/oneway/" + deviceId, setGpioRequest, String.class, status().isOk());
        Assert.assertTrue(StringUtils.isEmpty(result));
        latch.await(3, TimeUnit.SECONDS);
        assertEquals(MqttQoS.AT_MOST_ONCE.value(), callback.getQoS());
    }

    protected void validateJsonTwoWayRpcGatewayResponse(String deviceName, MqttAsyncClient client, byte[] payloadBytes) throws Exception {
        publishMqttMsg(client, payloadBytes, MqttTopics.GATEWAY_CONNECT_TOPIC);

        Device savedDevice = doExecuteWithRetriesAndInterval(
                () -> getDeviceByName(deviceName),
                20,
                100
        );
        assertNotNull(savedDevice);

        CountDownLatch latch = new CountDownLatch(1);
        TestJsonMqttCallback callback = new TestJsonMqttCallback(client, latch);
        client.setCallback(callback);

        client.subscribe(MqttTopics.GATEWAY_RPC_TOPIC, MqttQoS.AT_MOST_ONCE.value());

        Thread.sleep(1000);

        String setGpioRequest = "{\"method\": \"toggle_gpio\", \"params\": {\"pin\":1}}";
        String deviceId = savedDevice.getId().getId().toString();
        String result = doPostAsync("/api/rpc/twoway/" + deviceId, setGpioRequest, String.class, status().isOk());
        latch.await(3, TimeUnit.SECONDS);
        String expected = "{\"success\":true}";
        assertEquals(expected, result);
        assertEquals(MqttQoS.AT_MOST_ONCE.value(), callback.getQoS());
    }

    protected void validateProtoTwoWayRpcGatewayResponse(String deviceName, MqttAsyncClient client, byte[] payloadBytes) throws Exception {
        publishMqttMsg(client, payloadBytes, MqttTopics.GATEWAY_CONNECT_TOPIC);

        Device savedDevice = doExecuteWithRetriesAndInterval(
                () -> getDeviceByName(deviceName),
                20,
                100
        );
        assertNotNull(savedDevice);

        CountDownLatch latch = new CountDownLatch(1);
        TestProtoMqttCallback callback = new TestProtoMqttCallback(client, latch);
        client.setCallback(callback);

        client.subscribe(MqttTopics.GATEWAY_RPC_TOPIC, MqttQoS.AT_MOST_ONCE.value());

        Thread.sleep(1000);

        String setGpioRequest = "{\"method\": \"toggle_gpio\", \"params\": {\"pin\":1}}";
        String deviceId = savedDevice.getId().getId().toString();
        String result = doPostAsync("/api/rpc/twoway/" + deviceId, setGpioRequest, String.class, status().isOk());
        latch.await(3, TimeUnit.SECONDS);
        String expected = "{\"success\":true}";
        assertEquals(expected, result);
        assertEquals(MqttQoS.AT_MOST_ONCE.value(), callback.getQoS());
    }

    private Device getDeviceByName(String deviceName) throws Exception {
        return doGet("/api/tenant/devices?deviceName=" + deviceName, Device.class);
    }

    protected MqttMessage processJsonMessageArrived(String requestTopic, MqttMessage mqttMessage) throws MqttException, InvalidProtocolBufferException {
        MqttMessage message = new MqttMessage();
        if (requestTopic.startsWith(MqttTopics.BASE_DEVICE_API_TOPIC) || requestTopic.startsWith(MqttTopics.BASE_DEVICE_API_TOPIC_V2)) {
            message.setPayload(DEVICE_RESPONSE.getBytes(StandardCharset.UTF_8));
        } else {
            JsonNode requestMsgNode = JacksonUtil.toJsonNode(new String(mqttMessage.getPayload(), StandardCharset.UTF_8));
            String deviceName = requestMsgNode.get("device").asText();
            int requestId = requestMsgNode.get("data").get("id").asInt();
            message.setPayload(("{\"device\": \"" + deviceName + "\", \"id\": " + requestId + ", \"data\": {\"success\": true}}").getBytes(StandardCharset.UTF_8));
        }
        return message;
    }

    protected class TestOneWayMqttCallback implements MqttCallback {

        private final MqttAsyncClient client;
        private final CountDownLatch latch;
        private Integer qoS;

        TestOneWayMqttCallback(MqttAsyncClient client, CountDownLatch latch) {
            this.client = client;
            this.latch = latch;
        }

        int getQoS() {
            return qoS;
        }

        @Override
        public void connectionLost(Throwable throwable) {
        }

        @Override
        public void messageArrived(String requestTopic, MqttMessage mqttMessage) throws Exception {
            log.info("Message Arrived: " + Arrays.toString(mqttMessage.getPayload()));
            qoS = mqttMessage.getQos();
            latch.countDown();
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

        }
    }

    protected class TestJsonMqttCallback implements MqttCallback {

        private final MqttAsyncClient client;
        private final CountDownLatch latch;
        private Integer qoS;

        TestJsonMqttCallback(MqttAsyncClient client, CountDownLatch latch) {
            this.client = client;
            this.latch = latch;
        }

        int getQoS() {
            return qoS;
        }

        @Override
        public void connectionLost(Throwable throwable) {
        }

        @Override
        public void messageArrived(String requestTopic, MqttMessage mqttMessage) throws Exception {
            log.info("Message Arrived: " + Arrays.toString(mqttMessage.getPayload()));
            String responseTopic;
            if (requestTopic.startsWith(MqttTopics.BASE_DEVICE_API_TOPIC_V2)) {
                responseTopic = requestTopic.replace("req", "res");
            } else {
                responseTopic = requestTopic.replace("request", "response");
            }
            qoS = mqttMessage.getQos();
            client.publish(responseTopic, processJsonMessageArrived(requestTopic, mqttMessage));
            latch.countDown();
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

        }
    }

    protected class TestProtoMqttCallback implements MqttCallback {

        private final MqttAsyncClient client;
        private final CountDownLatch latch;
        private Integer qoS;

        TestProtoMqttCallback(MqttAsyncClient client, CountDownLatch latch) {
            this.client = client;
            this.latch = latch;
        }

        int getQoS() {
            return qoS;
        }

        @Override
        public void connectionLost(Throwable throwable) {
        }

        @Override
        public void messageArrived(String requestTopic, MqttMessage mqttMessage) throws Exception {
            log.info("Message Arrived: " + Arrays.toString(mqttMessage.getPayload()));
            String responseTopic;
            if (requestTopic.startsWith(MqttTopics.BASE_DEVICE_API_TOPIC_V2)) {
                responseTopic = requestTopic.replace("req", "res");
            } else {
                responseTopic = requestTopic.replace("request", "response");
            }
            qoS = mqttMessage.getQos();
            client.publish(responseTopic, processProtoMessageArrived(requestTopic, mqttMessage));
            latch.countDown();
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

        }
    }

    protected MqttMessage processProtoMessageArrived(String requestTopic, MqttMessage mqttMessage) throws MqttException, InvalidProtocolBufferException {
        MqttMessage message = new MqttMessage();
        if (requestTopic.startsWith(MqttTopics.BASE_DEVICE_API_TOPIC) || requestTopic.startsWith(MqttTopics.BASE_DEVICE_API_TOPIC_V2)) {
            ProtoTransportPayloadConfiguration protoTransportPayloadConfiguration = getProtoTransportPayloadConfiguration();
            ProtoFileElement rpcRequestProtoSchemaFile = protoTransportPayloadConfiguration.getTransportProtoSchema(RPC_REQUEST_PROTO_SCHEMA);
            DynamicSchema rpcRequestProtoSchema = protoTransportPayloadConfiguration.getDynamicSchema(rpcRequestProtoSchemaFile, ProtoTransportPayloadConfiguration.RPC_REQUEST_PROTO_SCHEMA);

            byte[] requestPayload = mqttMessage.getPayload();
            DynamicMessage.Builder rpcRequestMsg = rpcRequestProtoSchema.newMessageBuilder("RpcRequestMsg");
            Descriptors.Descriptor rpcRequestMsgDescriptor = rpcRequestMsg.getDescriptorForType();
            assertNotNull(rpcRequestMsgDescriptor);
            try {
                DynamicMessage dynamicMessage = DynamicMessage.parseFrom(rpcRequestMsgDescriptor, requestPayload);
                List<Descriptors.FieldDescriptor> fields = rpcRequestMsgDescriptor.getFields();
                for (Descriptors.FieldDescriptor fieldDescriptor: fields) {
                    assertTrue(dynamicMessage.hasField(fieldDescriptor));
                }
                ProtoFileElement transportProtoSchemaFile = protoTransportPayloadConfiguration.getTransportProtoSchema(DEVICE_RPC_RESPONSE_PROTO_SCHEMA);
                DynamicSchema rpcResponseProtoSchema = protoTransportPayloadConfiguration.getDynamicSchema(transportProtoSchemaFile, ProtoTransportPayloadConfiguration.RPC_RESPONSE_PROTO_SCHEMA);

                DynamicMessage.Builder rpcResponseBuilder = rpcResponseProtoSchema.newMessageBuilder("RpcResponseMsg");
                Descriptors.Descriptor rpcResponseMsgDescriptor = rpcResponseBuilder.getDescriptorForType();
                assertNotNull(rpcResponseMsgDescriptor);
                DynamicMessage rpcResponseMsg = rpcResponseBuilder
                        .setField(rpcResponseMsgDescriptor.findFieldByName("payload"), DEVICE_RESPONSE)
                        .build();
                message.setPayload(rpcResponseMsg.toByteArray());
            } catch (InvalidProtocolBufferException e) {
                log.warn("Command Response Ack Error, Invalid response received: ", e);
            }
        } else {
            TransportApiProtos.GatewayDeviceRpcRequestMsg msg = TransportApiProtos.GatewayDeviceRpcRequestMsg.parseFrom(mqttMessage.getPayload());
            String deviceName = msg.getDeviceName();
            int requestId = msg.getRpcRequestMsg().getRequestId();
            TransportApiProtos.GatewayRpcResponseMsg gatewayRpcResponseMsg = TransportApiProtos.GatewayRpcResponseMsg.newBuilder()
                    .setDeviceName(deviceName)
                    .setId(requestId)
                    .setData("{\"success\": true}")
                    .build();
            message.setPayload(gatewayRpcResponseMsg.toByteArray());
        }
        return message;
    }

    private ProtoTransportPayloadConfiguration getProtoTransportPayloadConfiguration() {
        DeviceProfileTransportConfiguration transportConfiguration = deviceProfile.getProfileData().getTransportConfiguration();
        assertTrue(transportConfiguration instanceof MqttDeviceProfileTransportConfiguration);
        MqttDeviceProfileTransportConfiguration mqttTransportConfiguration = (MqttDeviceProfileTransportConfiguration) transportConfiguration;
        TransportPayloadTypeConfiguration transportPayloadTypeConfiguration = mqttTransportConfiguration.getTransportPayloadTypeConfiguration();
        assertTrue(transportPayloadTypeConfiguration instanceof ProtoTransportPayloadConfiguration);
        return (ProtoTransportPayloadConfiguration) transportPayloadTypeConfiguration;
    }

    protected class TestSequenceMqttCallback implements MqttCallback {

        private final MqttAsyncClient client;
        private final CountDownLatch latch;
        private final List<String> expected;

        TestSequenceMqttCallback(MqttAsyncClient client, CountDownLatch latch, List<String> expected) {
            this.client = client;
            this.latch = latch;
            this.expected = expected;
        }

        @Override
        public void connectionLost(Throwable throwable) {
        }

        @Override
        public void messageArrived(String requestTopic, MqttMessage mqttMessage) throws Exception {
            log.info("Message Arrived: " + Arrays.toString(mqttMessage.getPayload()));
            expected.add(new String(mqttMessage.getPayload()));
            String responseTopic = requestTopic.replace("request", "response");
            var qoS = mqttMessage.getQos();

            client.messageArrivedComplete(mqttMessage.getId(), qoS);
            client.publish(responseTopic, processJsonMessageArrived(requestTopic, mqttMessage));
            latch.countDown();
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

        }
    }
}
