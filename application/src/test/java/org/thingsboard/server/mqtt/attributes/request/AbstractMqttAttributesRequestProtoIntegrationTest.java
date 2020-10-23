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
package org.thingsboard.server.mqtt.attributes.request;

import com.google.protobuf.InvalidProtocolBufferException;
import io.netty.handler.codec.mqtt.MqttQoS;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.TransportPayloadType;
import org.thingsboard.server.common.data.device.profile.MqttTopics;
import org.thingsboard.server.gen.transport.TransportApiProtos;
import org.thingsboard.server.gen.transport.TransportProtos;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
public abstract class AbstractMqttAttributesRequestProtoIntegrationTest extends AbstractMqttAttributesRequestIntegrationTest {

    @Before
    public void beforeTest() throws Exception {
        processBeforeTest("Test Request attribute values from the server proto", "Gateway Test Request attribute values from the server proto", TransportPayloadType.PROTOBUF, null, null);
    }

    @After
    public void afterTest() throws Exception {
        processAfterTest();
    }

    @Ignore
    @Test
    public void testRequestAttributesValuesFromTheServer() throws Exception {
        processTestRequestAttributesValuesFromTheServer();
    }


    @Test
    public void testRequestAttributesValuesFromTheServerGateway() throws Exception {
        processTestGatewayRequestAttributesValuesFromTheServer();
    }

    protected void postAttributesAndSubscribeToTopic(Device savedDevice, MqttAsyncClient client) throws Exception {
        doPostAsync("/api/plugins/telemetry/DEVICE/" + savedDevice.getId().getId() + "/attributes/SHARED_SCOPE", POST_ATTRIBUTES_PAYLOAD, String.class, status().isOk());
        String keys = "attribute1,attribute2,attribute3,attribute4,attribute5";
        List<String> expectedKeys = Arrays.asList(keys.split(","));
        TransportProtos.PostAttributeMsg postAttributeMsg = getPostAttributeMsg(expectedKeys);
        byte[] payload = postAttributeMsg.toByteArray();
        client.publish(MqttTopics.DEVICE_ATTRIBUTES_TOPIC, new MqttMessage(payload));
        client.subscribe(MqttTopics.DEVICE_ATTRIBUTES_RESPONSES_TOPIC, MqttQoS.AT_MOST_ONCE.value());
    }

    protected void postGatewayDeviceClientAttributes(MqttAsyncClient client) throws Exception {
        String keys = "attribute1,attribute2,attribute3,attribute4,attribute5";
        List<String> expectedKeys = Arrays.asList(keys.split(","));
        TransportProtos.PostAttributeMsg postAttributeMsg = getPostAttributeMsg(expectedKeys);
        TransportApiProtos.AttributesMsg.Builder attributesMsgBuilder = TransportApiProtos.AttributesMsg.newBuilder();
        attributesMsgBuilder.setDeviceName("Gateway Device Request Attributes");
        attributesMsgBuilder.setMsg(postAttributeMsg);
        TransportApiProtos.AttributesMsg attributesMsg = attributesMsgBuilder.build();
        TransportApiProtos.GatewayAttributesMsg.Builder gatewayAttributeMsgBuilder = TransportApiProtos.GatewayAttributesMsg.newBuilder();
        gatewayAttributeMsgBuilder.addMsg(attributesMsg);
        byte[] bytes = gatewayAttributeMsgBuilder.build().toByteArray();
        client.publish(MqttTopics.GATEWAY_ATTRIBUTES_TOPIC, new MqttMessage(bytes));
    }

    protected void validateResponse(MqttAsyncClient client, CountDownLatch latch, TestMqttCallback callback) throws MqttException, InterruptedException, InvalidProtocolBufferException {
        String keys = "attribute1,attribute2,attribute3,attribute4,attribute5";
        TransportApiProtos.AttributesRequest.Builder attributesRequestBuilder = TransportApiProtos.AttributesRequest.newBuilder();
        attributesRequestBuilder.setClientKeys(keys);
        attributesRequestBuilder.setSharedKeys(keys);
        TransportApiProtos.AttributesRequest attributesRequest = attributesRequestBuilder.build();
        MqttMessage mqttMessage = new MqttMessage();
        mqttMessage.setPayload(attributesRequest.toByteArray());
        client.publish(MqttTopics.DEVICE_ATTRIBUTES_REQUEST_TOPIC_PREFIX + "1", mqttMessage);
        latch.await(3, TimeUnit.SECONDS);
        assertEquals(MqttQoS.AT_MOST_ONCE.value(), callback.getQoS());
        TransportProtos.GetAttributeResponseMsg expectedAttributesResponse = getExpectedAttributeResponseMsg();
        TransportProtos.GetAttributeResponseMsg actualAttributesResponse = TransportProtos.GetAttributeResponseMsg.parseFrom(callback.getPayloadBytes());
        assertEquals(expectedAttributesResponse.getRequestId(), actualAttributesResponse.getRequestId());
        List<TransportProtos.KeyValueProto> expectedClientKeyValueProtos = expectedAttributesResponse.getClientAttributeListList().stream().map(TransportProtos.TsKvProto::getKv).collect(Collectors.toList());
        List<TransportProtos.KeyValueProto> expectedSharedKeyValueProtos = expectedAttributesResponse.getSharedAttributeListList().stream().map(TransportProtos.TsKvProto::getKv).collect(Collectors.toList());
        List<TransportProtos.KeyValueProto> actualClientKeyValueProtos = actualAttributesResponse.getClientAttributeListList().stream().map(TransportProtos.TsKvProto::getKv).collect(Collectors.toList());
        List<TransportProtos.KeyValueProto> actualSharedKeyValueProtos = actualAttributesResponse.getSharedAttributeListList().stream().map(TransportProtos.TsKvProto::getKv).collect(Collectors.toList());
        assertTrue(actualClientKeyValueProtos.containsAll(expectedClientKeyValueProtos));
        assertTrue(actualSharedKeyValueProtos.containsAll(expectedSharedKeyValueProtos));
    }

    protected void validateClientResponseGateway(MqttAsyncClient client, TestMqttCallback callback) throws MqttException, InterruptedException, InvalidProtocolBufferException {
            String keys = "attribute1,attribute2,attribute3,attribute4,attribute5";
            TransportApiProtos.GatewayAttributesRequestMsg gatewayAttributesRequestMsg = getGatewayAttributesRequestMsg(keys, true);
            client.publish(MqttTopics.GATEWAY_ATTRIBUTES_REQUEST_TOPIC, new MqttMessage(gatewayAttributesRequestMsg.toByteArray()));
            callback.getLatch().await(3, TimeUnit.SECONDS);
            assertEquals(MqttQoS.AT_LEAST_ONCE.value(), callback.getQoS());
            TransportApiProtos.GatewayAttributeResponseMsg expectedGatewayAttributeResponseMsg = getExpectedGatewayAttributeResponseMsg(true);
            TransportApiProtos.GatewayAttributeResponseMsg actualGatewayAttributeResponseMsg = TransportApiProtos.GatewayAttributeResponseMsg.parseFrom(callback.getPayloadBytes());
            assertEquals(expectedGatewayAttributeResponseMsg.getDeviceName(), actualGatewayAttributeResponseMsg.getDeviceName());

            TransportProtos.GetAttributeResponseMsg expectedResponseMsg = expectedGatewayAttributeResponseMsg.getResponseMsg();
            TransportProtos.GetAttributeResponseMsg actualResponseMsg = actualGatewayAttributeResponseMsg.getResponseMsg();
            assertEquals(expectedResponseMsg.getRequestId(), actualResponseMsg.getRequestId());

            List<TransportProtos.KeyValueProto> expectedClientKeyValueProtos = expectedResponseMsg.getClientAttributeListList().stream().map(TransportProtos.TsKvProto::getKv).collect(Collectors.toList());
            List<TransportProtos.KeyValueProto> actualClientKeyValueProtos = actualResponseMsg.getClientAttributeListList().stream().map(TransportProtos.TsKvProto::getKv).collect(Collectors.toList());
            assertTrue(actualClientKeyValueProtos.containsAll(expectedClientKeyValueProtos));
    }

    protected void validateSharedResponseGateway(MqttAsyncClient client, TestMqttCallback callback) throws MqttException, InterruptedException, InvalidProtocolBufferException {
            String keys = "attribute1,attribute2,attribute3,attribute4,attribute5";
            TransportApiProtos.GatewayAttributesRequestMsg gatewayAttributesRequestMsg = getGatewayAttributesRequestMsg(keys, false);
            client.publish(MqttTopics.GATEWAY_ATTRIBUTES_REQUEST_TOPIC, new MqttMessage(gatewayAttributesRequestMsg.toByteArray()));
            callback.getLatch().await(3, TimeUnit.SECONDS);
            assertEquals(MqttQoS.AT_LEAST_ONCE.value(), callback.getQoS());
            TransportApiProtos.GatewayAttributeResponseMsg expectedGatewayAttributeResponseMsg = getExpectedGatewayAttributeResponseMsg(false);
            TransportApiProtos.GatewayAttributeResponseMsg actualGatewayAttributeResponseMsg = TransportApiProtos.GatewayAttributeResponseMsg.parseFrom(callback.getPayloadBytes());
            assertEquals(expectedGatewayAttributeResponseMsg.getDeviceName(), actualGatewayAttributeResponseMsg.getDeviceName());

            TransportProtos.GetAttributeResponseMsg expectedResponseMsg = expectedGatewayAttributeResponseMsg.getResponseMsg();
            TransportProtos.GetAttributeResponseMsg actualResponseMsg = actualGatewayAttributeResponseMsg.getResponseMsg();
            assertEquals(expectedResponseMsg.getRequestId(), actualResponseMsg.getRequestId());

            List<TransportProtos.KeyValueProto> expectedSharedKeyValueProtos = expectedResponseMsg.getSharedAttributeListList().stream().map(TransportProtos.TsKvProto::getKv).collect(Collectors.toList());
            List<TransportProtos.KeyValueProto> actualSharedKeyValueProtos = actualResponseMsg.getSharedAttributeListList().stream().map(TransportProtos.TsKvProto::getKv).collect(Collectors.toList());

            assertTrue(actualSharedKeyValueProtos.containsAll(expectedSharedKeyValueProtos));
    }

    private TransportApiProtos.GatewayAttributesRequestMsg getGatewayAttributesRequestMsg(String keys, boolean client) {
        return TransportApiProtos.GatewayAttributesRequestMsg.newBuilder()
                .setClient(client)
                .addAllKeys(Arrays.asList(keys.split(",")))
                .setDeviceName("Gateway Device Request Attributes")
                .setId(1).build();
    }

    private TransportProtos.GetAttributeResponseMsg getExpectedAttributeResponseMsg() {
        TransportProtos.GetAttributeResponseMsg.Builder result = TransportProtos.GetAttributeResponseMsg.newBuilder();
        List<TransportProtos.TsKvProto> tsKvProtoList = getTsKvProtoList();
        result.addAllClientAttributeList(tsKvProtoList);
        result.addAllSharedAttributeList(tsKvProtoList);
        result.setRequestId(1);
        return result.build();
    }

    private TransportApiProtos.GatewayAttributeResponseMsg getExpectedGatewayAttributeResponseMsg(boolean client) {
        TransportApiProtos.GatewayAttributeResponseMsg.Builder gatewayAttributeResponseMsg = TransportApiProtos.GatewayAttributeResponseMsg.newBuilder();
        TransportProtos.GetAttributeResponseMsg.Builder getAttributeResponseMsgBuilder = TransportProtos.GetAttributeResponseMsg.newBuilder();
        List<TransportProtos.TsKvProto> tsKvProtoList = getTsKvProtoList();
        if (client) {
            getAttributeResponseMsgBuilder.addAllClientAttributeList(tsKvProtoList);
        } else {
            getAttributeResponseMsgBuilder.addAllSharedAttributeList(tsKvProtoList);
        }
        getAttributeResponseMsgBuilder.setRequestId(1);
        TransportProtos.GetAttributeResponseMsg getAttributeResponseMsg = getAttributeResponseMsgBuilder.build();
        gatewayAttributeResponseMsg.setDeviceName("Gateway Device Request Attributes");
        gatewayAttributeResponseMsg.setResponseMsg(getAttributeResponseMsg);
        return gatewayAttributeResponseMsg.build();
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
