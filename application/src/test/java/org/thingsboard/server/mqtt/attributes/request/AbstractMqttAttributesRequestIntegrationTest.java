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
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.device.profile.MqttTopics;
import org.thingsboard.server.dao.util.mapping.JacksonUtil;
import org.thingsboard.server.mqtt.attributes.AbstractMqttAttributesIntegrationTest;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
public abstract class AbstractMqttAttributesRequestIntegrationTest extends AbstractMqttAttributesIntegrationTest {

    @Before
    public void beforeTest() throws Exception {
        processBeforeTest("Test Request attribute values from the server", "Gateway Test Request attribute values from the server", null, null, null);
    }

    @After
    public void afterTest() throws Exception {
        processAfterTest();
    }

    @Test
    public void testRequestAttributesValuesFromTheServer() throws Exception {
        processTestRequestAttributesValuesFromTheServer();
    }

    @Test
    public void testRequestAttributesValuesFromTheServerGateway() throws Exception {
        processTestGatewayRequestAttributesValuesFromTheServer();
    }

    protected void processTestRequestAttributesValuesFromTheServer() throws Exception {

        MqttAsyncClient client = getMqttAsyncClient(accessToken);

        postAttributesAndSubscribeToTopic(savedDevice, client);

        Thread.sleep(1000);

        TestMqttCallback callback = getTestMqttCallback();
        client.setCallback(callback);

        validateResponse(client, callback.getLatch(), callback);
    }

    protected void processTestGatewayRequestAttributesValuesFromTheServer() throws Exception {

        MqttAsyncClient client = getMqttAsyncClient(gatewayAccessToken);

        postGatewayDeviceClientAttributes(client);

        Thread.sleep(1000);

        Device savedDevice = doGet("/api/tenant/devices?deviceName=" + "Gateway Device Request Attributes", Device.class);
        assertNotNull(savedDevice);
        doPostAsync("/api/plugins/telemetry/DEVICE/" + savedDevice.getId().getId() + "/attributes/SHARED_SCOPE", POST_ATTRIBUTES_PAYLOAD, String.class, status().isOk());

        Thread.sleep(1000);

        client.subscribe(MqttTopics.GATEWAY_ATTRIBUTES_RESPONSE_TOPIC, MqttQoS.AT_LEAST_ONCE.value());

        TestMqttCallback clientAttributesCallback = getTestMqttCallback();
        client.setCallback(clientAttributesCallback);
        validateClientResponseGateway(client, clientAttributesCallback);

        TestMqttCallback sharedAttributesCallback = getTestMqttCallback();
        client.setCallback(sharedAttributesCallback);
        validateSharedResponseGateway(client, sharedAttributesCallback);
    }

    protected void postAttributesAndSubscribeToTopic(Device savedDevice, MqttAsyncClient client) throws Exception {
        doPostAsync("/api/plugins/telemetry/DEVICE/" + savedDevice.getId().getId() + "/attributes/SHARED_SCOPE", POST_ATTRIBUTES_PAYLOAD, String.class, status().isOk());
        client.publish(MqttTopics.DEVICE_ATTRIBUTES_TOPIC, new MqttMessage(POST_ATTRIBUTES_PAYLOAD.getBytes()));
        client.subscribe(MqttTopics.DEVICE_ATTRIBUTES_RESPONSES_TOPIC, MqttQoS.AT_MOST_ONCE.value());
    }

    protected void postGatewayDeviceClientAttributes(MqttAsyncClient client) throws Exception {
        String postClientAttributes = "{\"" + "Gateway Device Request Attributes" + "\":{\"attribute1\":\"value1\",\"attribute2\":true,\"attribute3\":42.0,\"attribute4\":73,\"attribute5\":{\"someNumber\":42,\"someArray\":[1,2,3],\"someNestedObject\":{\"key\":\"value\"}}}}";
        client.publish(MqttTopics.GATEWAY_ATTRIBUTES_TOPIC, new MqttMessage(postClientAttributes.getBytes()));
    }

    protected void validateResponse(MqttAsyncClient client, CountDownLatch latch, TestMqttCallback callback) throws MqttException, InterruptedException, InvalidProtocolBufferException {
        String keys = "attribute1,attribute2,attribute3,attribute4,attribute5";
        String payloadStr = "{\"clientKeys\":\"" + keys + "\", \"sharedKeys\":\"" + keys + "\"}";
        MqttMessage mqttMessage = new MqttMessage();
        mqttMessage.setPayload(payloadStr.getBytes());
        client.publish(MqttTopics.DEVICE_ATTRIBUTES_REQUEST_TOPIC_PREFIX + "1", mqttMessage);
        latch.await(3, TimeUnit.SECONDS);
        assertEquals(MqttQoS.AT_MOST_ONCE.value(), callback.getQoS());
        String expectedRequestPayload = "{\"client\":{\"attribute1\":\"value1\",\"attribute2\":true,\"attribute3\":42.0,\"attribute4\":73,\"attribute5\":{\"someNumber\":42,\"someArray\":[1,2,3],\"someNestedObject\":{\"key\":\"value\"}}},\"shared\":{\"attribute1\":\"value1\",\"attribute2\":true,\"attribute3\":42.0,\"attribute4\":73,\"attribute5\":{\"someNumber\":42,\"someArray\":[1,2,3],\"someNestedObject\":{\"key\":\"value\"}}}}";
        assertEquals(JacksonUtil.toJsonNode(expectedRequestPayload), JacksonUtil.toJsonNode(new String(callback.getPayloadBytes(), StandardCharsets.UTF_8)));
    }

    protected void validateClientResponseGateway(MqttAsyncClient client, TestMqttCallback callback) throws MqttException, InterruptedException, InvalidProtocolBufferException {
        String payloadStr = "{\"id\": 1, \"device\": \"" + "Gateway Device Request Attributes" + "\", \"client\": true, \"keys\": [\"attribute1\", \"attribute2\", \"attribute3\", \"attribute4\", \"attribute5\"]}";
        MqttMessage mqttMessage = new MqttMessage();
        mqttMessage.setPayload(payloadStr.getBytes());
        client.publish(MqttTopics.GATEWAY_ATTRIBUTES_REQUEST_TOPIC, mqttMessage);
        callback.getLatch().await(3, TimeUnit.SECONDS);
        assertEquals(MqttQoS.AT_LEAST_ONCE.value(), callback.getQoS());
        String expectedRequestPayload = "{\"id\":1,\"device\":\"" + "Gateway Device Request Attributes" + "\",\"values\":{\"attribute1\":\"value1\",\"attribute2\":true,\"attribute3\":42.0,\"attribute4\":73,\"attribute5\":{\"someNumber\":42,\"someArray\":[1,2,3],\"someNestedObject\":{\"key\":\"value\"}}}}";
        assertEquals(JacksonUtil.toJsonNode(expectedRequestPayload), JacksonUtil.toJsonNode(new String(callback.getPayloadBytes(), StandardCharsets.UTF_8)));
    }

    protected void validateSharedResponseGateway(MqttAsyncClient client, TestMqttCallback callback) throws MqttException, InterruptedException, InvalidProtocolBufferException {
        String payloadStr = "{\"id\": 1, \"device\": \"" + "Gateway Device Request Attributes" + "\", \"client\": false, \"keys\": [\"attribute1\", \"attribute2\", \"attribute3\", \"attribute4\", \"attribute5\"]}";
        MqttMessage mqttMessage = new MqttMessage();
        mqttMessage.setPayload(payloadStr.getBytes());
        client.publish(MqttTopics.GATEWAY_ATTRIBUTES_REQUEST_TOPIC, mqttMessage);
        callback.getLatch().await(3, TimeUnit.SECONDS);
        assertEquals(MqttQoS.AT_LEAST_ONCE.value(), callback.getQoS());
        String expectedRequestPayload = "{\"id\":1,\"device\":\"" + "Gateway Device Request Attributes" + "\",\"values\":{\"attribute1\":\"value1\",\"attribute2\":true,\"attribute3\":42.0,\"attribute4\":73,\"attribute5\":{\"someNumber\":42,\"someArray\":[1,2,3],\"someNestedObject\":{\"key\":\"value\"}}}}";
        assertEquals(JacksonUtil.toJsonNode(expectedRequestPayload), JacksonUtil.toJsonNode(new String(callback.getPayloadBytes(), StandardCharsets.UTF_8)));
    }
}
