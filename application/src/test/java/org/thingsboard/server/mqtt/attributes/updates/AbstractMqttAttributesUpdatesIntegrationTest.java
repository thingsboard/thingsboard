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
package org.thingsboard.server.mqtt.attributes.updates;

import com.google.protobuf.InvalidProtocolBufferException;
import io.netty.handler.codec.mqtt.MqttQoS;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.gen.transport.TransportApiProtos;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.mqtt.attributes.AbstractMqttAttributesIntegrationTest;
import org.thingsboard.server.transport.mqtt.MqttTopics;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
public abstract class AbstractMqttAttributesUpdatesIntegrationTest extends AbstractMqttAttributesIntegrationTest {

    private static final String RESPONSE_ATTRIBUTES_PAYLOAD_DELETED = "{\"deleted\":[\"attribute5\"]}";

    private static String getResponseGatewayAttributesUpdatedPayload(String deviceName) {
        return "{\"device\":\"" + deviceName + "\"," +
                "\"data\":{\"attribute1\":\"value1\",\"attribute2\":true,\"attribute3\":42.0,\"attribute4\":73,\"attribute5\":{\"someNumber\":42,\"someArray\":[1,2,3],\"someNestedObject\":{\"key\":\"value\"}}}}";
    }

    private static String getResponseGatewayAttributesDeletedPayload(String deviceName) {
        return "{\"device\":\"" + deviceName + "\",\"data\":{\"deleted\":[\"attribute5\"]}}";
    }

    @Before
    public void beforeTest() throws Exception {
        processBeforeTest("Test Subscribe to attribute updates", "Gateway Test Subscribe to attribute updates");
    }

    @After
    public void afterTest() throws Exception {
        processAfterTest();
    }

    @Test
    public void testSubscribeToAttributesUpdatesFromTheServerV1Json() throws Exception {
        processTestSubscribeToAttributesUpdates(
                MqttTopics.DEVICE_ATTRIBUTES_TOPIC_V1_JSON);
    }

    @Test
    public void testSubscribeToAttributesUpdatesFromTheServerV2Json() throws Exception {
        processTestSubscribeToAttributesUpdates(
                MqttTopics.DEVICE_ATTRIBUTES_TOPIC_V2_JSON);
    }

    @Test
    public void testSubscribeToAttributesUpdatesFromTheServerV2Proto() throws Exception {
        processTestSubscribeToAttributesUpdates(
                MqttTopics.DEVICE_ATTRIBUTES_TOPIC_V2_PROTO);
    }

    @Test
    public void testSubscribeToAttributesUpdatesFromTheServerV1GatewayJson() throws Exception {
        processGatewayTestSubscribeToAttributesUpdates(
                "Gateway Device Subscribe to attribute updates V1 Json",
                MqttTopics.GATEWAY_CONNECT_TOPIC_V1_JSON,
                MqttTopics.GATEWAY_ATTRIBUTES_TOPIC_V1_JSON);
    }

    @Test
    public void testSubscribeToAttributesUpdatesFromTheServerV2GatewayJson() throws Exception {
        processGatewayTestSubscribeToAttributesUpdates(
                "Gateway Device Subscribe to attribute updates V2 Json",
                MqttTopics.GATEWAY_CONNECT_TOPIC_V2_JSON,
                MqttTopics.GATEWAY_ATTRIBUTES_TOPIC_V2_JSON);
    }

    @Test
    public void testSubscribeToAttributesUpdatesFromTheServerV2GatewayProto() throws Exception {
        processGatewayTestSubscribeToAttributesUpdates(
                "Gateway Device Subscribe to attribute updates V2 Proto",
                MqttTopics.GATEWAY_CONNECT_TOPIC_V2_PROTO,
                MqttTopics.GATEWAY_ATTRIBUTES_TOPIC_V2_PROTO);
    }

    private void processTestSubscribeToAttributesUpdates(String topicToSubscribeForAttributesUpdates) throws Exception {

        MqttAsyncClient client = getMqttAsyncClient(accessToken);

        TestMqttCallback onUpdateCallback = getTestMqttCallback();
        client.setCallback(onUpdateCallback);

        client.subscribe(topicToSubscribeForAttributesUpdates, MqttQoS.AT_MOST_ONCE.value());

        Thread.sleep(2000);

        doPostAsync("/api/plugins/telemetry/DEVICE/" + savedDevice.getId().getId() + "/attributes/SHARED_SCOPE", POST_ATTRIBUTES_PAYLOAD, String.class, status().isOk());
        onUpdateCallback.getLatch().await(5, TimeUnit.SECONDS);

        validateUpdateAttributesResponse(topicToSubscribeForAttributesUpdates, onUpdateCallback);

        TestMqttCallback onDeleteCallback = getTestMqttCallback();
        client.setCallback(onDeleteCallback);

        doDelete("/api/plugins/telemetry/DEVICE/" + savedDevice.getId().getId() + "/SHARED_SCOPE?keys=attribute5", String.class);
        onDeleteCallback.getLatch().await(5, TimeUnit.SECONDS);

        validateDeleteAttributesResponse(topicToSubscribeForAttributesUpdates, onDeleteCallback);
    }

    private void validateUpdateAttributesResponse(String topicToSubscribeForAttributesUpdates, TestMqttCallback callback) throws InvalidProtocolBufferException {
        assertNotNull(callback.getPayloadBytes());
        if (topicToSubscribeForAttributesUpdates.startsWith(MqttTopics.DEVICE_ATTRIBUTES_TOPIC_V1_JSON) || topicToSubscribeForAttributesUpdates.startsWith(MqttTopics.DEVICE_ATTRIBUTES_TOPIC_V2_JSON)) {
            String s = new String(callback.getPayloadBytes(), StandardCharsets.UTF_8);
            assertEquals(POST_ATTRIBUTES_PAYLOAD, s);
        } else {
            TransportProtos.AttributeUpdateNotificationMsg.Builder attributeUpdateNotificationMsgBuilder = TransportProtos.AttributeUpdateNotificationMsg.newBuilder();
            List<TransportProtos.TsKvProto> tsKvProtoList = getTsKvProtoList();
            attributeUpdateNotificationMsgBuilder.addAllSharedUpdated(tsKvProtoList);

            TransportProtos.AttributeUpdateNotificationMsg expectedAttributeUpdateNotificationMsg = attributeUpdateNotificationMsgBuilder.build();
            TransportProtos.AttributeUpdateNotificationMsg actualAttributeUpdateNotificationMsg = TransportProtos.AttributeUpdateNotificationMsg.parseFrom(callback.getPayloadBytes());

            List<TransportProtos.KeyValueProto> actualSharedUpdatedList = actualAttributeUpdateNotificationMsg.getSharedUpdatedList().stream().map(TransportProtos.TsKvProto::getKv).collect(Collectors.toList());
            List<TransportProtos.KeyValueProto> expectedSharedUpdatedList = expectedAttributeUpdateNotificationMsg.getSharedUpdatedList().stream().map(TransportProtos.TsKvProto::getKv).collect(Collectors.toList());

            assertEquals(expectedSharedUpdatedList.size(), actualSharedUpdatedList.size());
            assertTrue(actualSharedUpdatedList.containsAll(expectedSharedUpdatedList));
        }
    }

    private void validateDeleteAttributesResponse(String topicToSubscribeForAttributesUpdates, TestMqttCallback callback) throws InvalidProtocolBufferException {
        assertNotNull(callback.getPayloadBytes());
        if (topicToSubscribeForAttributesUpdates.startsWith(MqttTopics.DEVICE_ATTRIBUTES_TOPIC_V1_JSON) || topicToSubscribeForAttributesUpdates.startsWith(MqttTopics.DEVICE_ATTRIBUTES_TOPIC_V2_JSON)) {
            String s = new String(callback.getPayloadBytes(), StandardCharsets.UTF_8);
            assertEquals(s, RESPONSE_ATTRIBUTES_PAYLOAD_DELETED);
        } else {
            TransportProtos.AttributeUpdateNotificationMsg.Builder attributeUpdateNotificationMsgBuilder = TransportProtos.AttributeUpdateNotificationMsg.newBuilder();
            attributeUpdateNotificationMsgBuilder.addSharedDeleted("attribute5");

            TransportProtos.AttributeUpdateNotificationMsg expectedAttributeUpdateNotificationMsg = attributeUpdateNotificationMsgBuilder.build();
            TransportProtos.AttributeUpdateNotificationMsg actualAttributeUpdateNotificationMsg = TransportProtos.AttributeUpdateNotificationMsg.parseFrom(callback.getPayloadBytes());

            assertEquals(expectedAttributeUpdateNotificationMsg.getSharedDeletedList().size(), actualAttributeUpdateNotificationMsg.getSharedDeletedList().size());
            assertEquals("attribute5", actualAttributeUpdateNotificationMsg.getSharedDeletedList().get(0));
        }
    }

    private void processGatewayTestSubscribeToAttributesUpdates(String deviceName, String topicToConnectDevice, String topicToSubscribeForAttributesUpdates) throws Exception {

        MqttAsyncClient client = getMqttAsyncClient(gatewayAccessToken);

        TestMqttCallback onUpdateCallback = getTestMqttCallback();
        client.setCallback(onUpdateCallback);

        Device device = new Device();
        device.setName(deviceName);
        device.setType("default");

        byte[] connectPayloadBytes;
        if (topicToConnectDevice.startsWith(MqttTopics.BASE_GATEWAY_API_TOPIC_V2_PROTO)) {
            TransportApiProtos.ConnectMsg connectProto = getConnectProto(deviceName);
            connectPayloadBytes = connectProto.toByteArray();
        } else {
            String connectPayload = "{\"device\":\"" + deviceName + "\"}";
            connectPayloadBytes = connectPayload.getBytes();
        }

        publishMqttMsg(client, connectPayloadBytes, topicToConnectDevice);

        Thread.sleep(2000);

        Device savedDevice = doGet("/api/tenant/devices?deviceName=" + deviceName, Device.class);
        assertNotNull(savedDevice);

        client.subscribe(topicToSubscribeForAttributesUpdates, MqttQoS.AT_MOST_ONCE.value());

        Thread.sleep(2000);

        doPostAsync("/api/plugins/telemetry/DEVICE/" + savedDevice.getId().getId() + "/attributes/SHARED_SCOPE", POST_ATTRIBUTES_PAYLOAD, String.class, status().isOk());
        onUpdateCallback.getLatch().await(3, TimeUnit.SECONDS);

        validateGatewayUpdateAttributesResponse(topicToSubscribeForAttributesUpdates, onUpdateCallback, deviceName);

        TestMqttCallback onDeleteCallback = getTestMqttCallback();
        client.setCallback(onDeleteCallback);

        doDelete("/api/plugins/telemetry/DEVICE/" + savedDevice.getId().getId() + "/SHARED_SCOPE?keys=attribute5", String.class);
        onDeleteCallback.getLatch().await(3, TimeUnit.SECONDS);

        validateGatewayDeleteAttributesResponse(topicToSubscribeForAttributesUpdates, onDeleteCallback, deviceName);

    }

    private void validateGatewayUpdateAttributesResponse(String topicToSubscribeForAttributesUpdates, TestMqttCallback callback, String expectedDeviceName) throws InvalidProtocolBufferException {
        assertNotNull(callback.getPayloadBytes());
        if (topicToSubscribeForAttributesUpdates.startsWith(MqttTopics.GATEWAY_ATTRIBUTES_TOPIC_V1_JSON) || topicToSubscribeForAttributesUpdates.startsWith(MqttTopics.GATEWAY_ATTRIBUTES_TOPIC_V2_JSON)) {
            String s = new String(callback.getPayloadBytes(), StandardCharsets.UTF_8);
            assertEquals(getResponseGatewayAttributesUpdatedPayload(expectedDeviceName), s);
        } else {
            TransportProtos.AttributeUpdateNotificationMsg.Builder attributeUpdateNotificationMsgBuilder = TransportProtos.AttributeUpdateNotificationMsg.newBuilder();
            List<TransportProtos.TsKvProto> tsKvProtoList = getTsKvProtoList();
            attributeUpdateNotificationMsgBuilder.addAllSharedUpdated(tsKvProtoList);
            TransportProtos.AttributeUpdateNotificationMsg expectedAttributeUpdateNotificationMsg = attributeUpdateNotificationMsgBuilder.build();

            TransportApiProtos.GatewayAttributeUpdateNotificationMsg.Builder gatewayAttributeUpdateNotificationMsgBuilder = TransportApiProtos.GatewayAttributeUpdateNotificationMsg.newBuilder();
            gatewayAttributeUpdateNotificationMsgBuilder.setDeviceName(expectedDeviceName);
            gatewayAttributeUpdateNotificationMsgBuilder.setNotificationMsg(expectedAttributeUpdateNotificationMsg);

            TransportApiProtos.GatewayAttributeUpdateNotificationMsg expectedGatewayAttributeUpdateNotificationMsg = gatewayAttributeUpdateNotificationMsgBuilder.build();
            TransportApiProtos.GatewayAttributeUpdateNotificationMsg actualGatewayAttributeUpdateNotificationMsg = TransportApiProtos.GatewayAttributeUpdateNotificationMsg.parseFrom(callback.getPayloadBytes());

            assertEquals(expectedGatewayAttributeUpdateNotificationMsg.getDeviceName(), actualGatewayAttributeUpdateNotificationMsg.getDeviceName());

            List<TransportProtos.KeyValueProto> actualSharedUpdatedList = actualGatewayAttributeUpdateNotificationMsg.getNotificationMsg().getSharedUpdatedList().stream().map(TransportProtos.TsKvProto::getKv).collect(Collectors.toList());
            List<TransportProtos.KeyValueProto> expectedSharedUpdatedList = expectedGatewayAttributeUpdateNotificationMsg.getNotificationMsg().getSharedUpdatedList().stream().map(TransportProtos.TsKvProto::getKv).collect(Collectors.toList());

            assertEquals(expectedSharedUpdatedList.size(), actualSharedUpdatedList.size());
            assertTrue(actualSharedUpdatedList.containsAll(expectedSharedUpdatedList));
        }
    }

    private void validateGatewayDeleteAttributesResponse(String topicToSubscribeForAttributesUpdates, TestMqttCallback callback, String expectedDeviceName) throws InvalidProtocolBufferException {
        assertNotNull(callback.getPayloadBytes());
        if (topicToSubscribeForAttributesUpdates.startsWith(MqttTopics.GATEWAY_ATTRIBUTES_TOPIC_V1_JSON) || topicToSubscribeForAttributesUpdates.startsWith(MqttTopics.GATEWAY_ATTRIBUTES_TOPIC_V2_JSON)) {
            String s = new String(callback.getPayloadBytes(), StandardCharsets.UTF_8);
            assertEquals(s, getResponseGatewayAttributesDeletedPayload(expectedDeviceName));
        } else {
            TransportProtos.AttributeUpdateNotificationMsg.Builder attributeUpdateNotificationMsgBuilder = TransportProtos.AttributeUpdateNotificationMsg.newBuilder();
            attributeUpdateNotificationMsgBuilder.addSharedDeleted("attribute5");
            TransportProtos.AttributeUpdateNotificationMsg attributeUpdateNotificationMsg = attributeUpdateNotificationMsgBuilder.build();

            TransportApiProtos.GatewayAttributeUpdateNotificationMsg.Builder gatewayAttributeUpdateNotificationMsgBuilder = TransportApiProtos.GatewayAttributeUpdateNotificationMsg.newBuilder();
            gatewayAttributeUpdateNotificationMsgBuilder.setDeviceName(expectedDeviceName);
            gatewayAttributeUpdateNotificationMsgBuilder.setNotificationMsg(attributeUpdateNotificationMsg);

            TransportApiProtos.GatewayAttributeUpdateNotificationMsg expectedGatewayAttributeUpdateNotificationMsg = gatewayAttributeUpdateNotificationMsgBuilder.build();
            TransportApiProtos.GatewayAttributeUpdateNotificationMsg actualGatewayAttributeUpdateNotificationMsg = TransportApiProtos.GatewayAttributeUpdateNotificationMsg.parseFrom(callback.getPayloadBytes());

            assertEquals(expectedGatewayAttributeUpdateNotificationMsg.getDeviceName(), actualGatewayAttributeUpdateNotificationMsg.getDeviceName());

            TransportProtos.AttributeUpdateNotificationMsg expectedAttributeUpdateNotificationMsg = expectedGatewayAttributeUpdateNotificationMsg.getNotificationMsg();
            TransportProtos.AttributeUpdateNotificationMsg actualAttributeUpdateNotificationMsg = actualGatewayAttributeUpdateNotificationMsg.getNotificationMsg();

            assertEquals(expectedAttributeUpdateNotificationMsg.getSharedDeletedList().size(), actualAttributeUpdateNotificationMsg.getSharedDeletedList().size());
            assertEquals("attribute5", actualAttributeUpdateNotificationMsg.getSharedDeletedList().get(0));
        }
    }

    private TestMqttCallback getTestMqttCallback() {
        CountDownLatch latch = new CountDownLatch(1);
        return new TestMqttCallback(latch);
    }

    private void publishMqttMsg(MqttAsyncClient client, byte[] payload, String topic) throws MqttException {
        MqttMessage message = new MqttMessage();
        message.setPayload(payload);
        client.publish(topic, message);
    }

    private TransportApiProtos.ConnectMsg getConnectProto(String deviceName) {
        TransportApiProtos.ConnectMsg.Builder builder = TransportApiProtos.ConnectMsg.newBuilder();
        builder.setDeviceName(deviceName);
        return builder.build();
    }

    private static class TestMqttCallback implements MqttCallback {

        private final CountDownLatch latch;
        private Integer qoS;
        private byte[] payloadBytes;

        TestMqttCallback(CountDownLatch latch) {
            this.latch = latch;
        }

        int getQoS() {
            return qoS;
        }

        byte[] getPayloadBytes() {
            return payloadBytes;
        }

        public CountDownLatch getLatch() { return latch; }

        @Override
        public void connectionLost(Throwable throwable) {
        }

        @Override
        public void messageArrived(String requestTopic, MqttMessage mqttMessage) throws Exception {
            qoS = mqttMessage.getQos();
            payloadBytes = mqttMessage.getPayload();
            latch.countDown();
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

        }
    }
}
