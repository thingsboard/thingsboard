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
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.mqtt.attributes.AbstractMqttAttributesIntegrationTest;
import org.thingsboard.server.mqtt.attributes.request.AbstractMqttAttributesRequestIntegrationTest;
import org.thingsboard.server.transport.mqtt.MqttTopics;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
public abstract class AbstractMqttAttributesUpdatesIntegrationTest extends AbstractMqttAttributesIntegrationTest {

    private static final String POST_ATTRIBUTES_PAYLOAD_DELETED = "{\"deleted\":[\"attribute5\"]}";

    @Before
    public void beforeTest() throws Exception {
        processBeforeTest();
    }

    @After
    public void afterTest() throws Exception {
        processAfterTest();
    }

    @Test
    public void testSubscribeToAttributesUpdatesFromTheServerV1Json() throws Exception {
        processTestSubscribeToAttributesUpdates(
                "Test Subscribe to attribute updates V1 Json",
                MqttTopics.DEVICE_ATTRIBUTES_TOPIC_V1_JSON);
    }

    @Test
    public void testSubscribeToAttributesUpdatesFromTheServerV2Json() throws Exception {
        processTestSubscribeToAttributesUpdates(
                "Test Subscribe to attribute updates V2 Json",
                MqttTopics.DEVICE_ATTRIBUTES_TOPIC_V2_JSON);
    }

//    @Ignore
    @Test
    public void testSubscribeToAttributesUpdatesFromTheServerV2Proto() throws Exception {
        processTestSubscribeToAttributesUpdates(
                "Test Subscribe to attribute updates V2 Proto",
                MqttTopics.DEVICE_ATTRIBUTES_TOPIC_V2_PROTO);
    }

    private void processTestSubscribeToAttributesUpdates(String deviceName, String topicToSubscribeForAttributesUpdates) throws Exception {
        Device device = new Device();
        device.setName(deviceName);
        device.setType("default");
        Device savedDevice = getSavedDevice(device);
        DeviceCredentials deviceCredentials = getDeviceCredentials(savedDevice);
        assertEquals(savedDevice.getId(), deviceCredentials.getDeviceId());
        String accessToken = deviceCredentials.getCredentialsId();
        assertNotNull(accessToken);

//        doPostAsync("/api/plugins/telemetry/DEVICE/" + savedDevice.getId().getId() + "/attributes/SHARED_SCOPE", POST_ATTRIBUTES_PAYLOAD, String.class, status().isOk());
//
//        Thread.sleep(1000);

        MqttAsyncClient client = getMqttAsyncClient(accessToken);

        TestMqttCallback onUpdateCallback = getTestMqttCallback();
        client.setCallback(onUpdateCallback);

        client.subscribe(topicToSubscribeForAttributesUpdates, MqttQoS.AT_MOST_ONCE.value());

        doPostAsync("/api/plugins/telemetry/DEVICE/" + savedDevice.getId().getId() + "/attributes/SHARED_SCOPE", POST_ATTRIBUTES_PAYLOAD, String.class, status().isOk());
        onUpdateCallback.getLatch().await(20, TimeUnit.SECONDS);

        validateUpdateAttributesResponse(topicToSubscribeForAttributesUpdates, onUpdateCallback);

        TestMqttCallback onDeleteCallback = getTestMqttCallback();
        client.setCallback(onDeleteCallback);

        doDelete("/api/plugins/telemetry/DEVICE/" + savedDevice.getId().getId() + "/SHARED_SCOPE?keys=attribute5", String.class);
        onDeleteCallback.getLatch().await(3, TimeUnit.SECONDS);

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
            assertEquals(s, POST_ATTRIBUTES_PAYLOAD_DELETED);
        } else {
            TransportProtos.AttributeUpdateNotificationMsg.Builder attributeUpdateNotificationMsgBuilder = TransportProtos.AttributeUpdateNotificationMsg.newBuilder();
            attributeUpdateNotificationMsgBuilder.addSharedDeleted("attribute5");

            TransportProtos.AttributeUpdateNotificationMsg expectedAttributeUpdateNotificationMsg = attributeUpdateNotificationMsgBuilder.build();
            TransportProtos.AttributeUpdateNotificationMsg actualAttributeUpdateNotificationMsg = TransportProtos.AttributeUpdateNotificationMsg.parseFrom(callback.getPayloadBytes());

            assertEquals(expectedAttributeUpdateNotificationMsg.getSharedDeletedList().size(), actualAttributeUpdateNotificationMsg.getSharedDeletedList().size());
            assertEquals("attribute5", actualAttributeUpdateNotificationMsg.getSharedDeletedList().get(0));
        }
    }

    private TestMqttCallback getTestMqttCallback() {
        CountDownLatch latch = new CountDownLatch(1);
        return new TestMqttCallback(latch);
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
