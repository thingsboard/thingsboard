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
package org.thingsboard.server.transport.mqtt.attributes.request;

import com.google.protobuf.InvalidProtocolBufferException;
import io.netty.handler.codec.mqtt.MqttQoS;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.device.profile.MqttTopics;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.transport.mqtt.attributes.AbstractMqttAttributesIntegrationTest;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
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
        processTestRequestAttributesValuesFromTheServer(MqttTopics.DEVICE_ATTRIBUTES_TOPIC, MqttTopics.DEVICE_ATTRIBUTES_RESPONSES_TOPIC, MqttTopics.DEVICE_ATTRIBUTES_REQUEST_TOPIC_PREFIX);
    }

    @Test
    public void testRequestAttributesValuesFromTheServerOnShortTopic() throws Exception {
        processTestRequestAttributesValuesFromTheServer(MqttTopics.DEVICE_ATTRIBUTES_SHORT_TOPIC, MqttTopics.DEVICE_ATTRIBUTES_RESPONSES_SHORT_TOPIC, MqttTopics.DEVICE_ATTRIBUTES_REQUEST_SHORT_TOPIC_PREFIX);
    }

    @Test
    public void testRequestAttributesValuesFromTheServerOnShortJsonTopic() throws Exception {
        processTestRequestAttributesValuesFromTheServer(MqttTopics.DEVICE_ATTRIBUTES_SHORT_JSON_TOPIC, MqttTopics.DEVICE_ATTRIBUTES_RESPONSES_SHORT_JSON_TOPIC, MqttTopics.DEVICE_ATTRIBUTES_REQUEST_SHORT_JSON_TOPIC_PREFIX);
    }

    @Test
    public void testRequestAttributesValuesFromTheServerGateway() throws Exception {
        processTestGatewayRequestAttributesValuesFromTheServer();
    }

    protected void processTestRequestAttributesValuesFromTheServer(String attrPubTopic, String attrSubTopic, String attrReqTopicPrefix) throws Exception {

        MqttAsyncClient client = getMqttAsyncClient(accessToken);

        postAttributesAndSubscribeToTopic(savedDevice, client, attrPubTopic, attrSubTopic);

        Thread.sleep(5000);

        TestMqttCallback callback = getTestMqttCallback();
        client.setCallback(callback);

        validateResponse(client, callback.getLatch(), callback, attrReqTopicPrefix);
    }

    protected void processTestGatewayRequestAttributesValuesFromTheServer() throws Exception {

        MqttAsyncClient client = getMqttAsyncClient(gatewayAccessToken);

        postGatewayDeviceClientAttributes(client);

        Device savedDevice = doExecuteWithRetriesAndInterval(() -> doGet("/api/tenant/devices?deviceName=" + "Gateway Device Request Attributes", Device.class),
                20,
                100);

        assertNotNull(savedDevice);

        Thread.sleep(2000);

        doPostAsync("/api/plugins/telemetry/DEVICE/" + savedDevice.getId().getId() + "/attributes/SHARED_SCOPE", POST_ATTRIBUTES_PAYLOAD, String.class, status().isOk());

        Thread.sleep(5000);

        client.subscribe(MqttTopics.GATEWAY_ATTRIBUTES_RESPONSE_TOPIC, MqttQoS.AT_LEAST_ONCE.value()).waitForCompletion(TimeUnit.MINUTES.toMillis(1));

        TestMqttCallback clientAttributesCallback = getTestMqttCallback();
        client.setCallback(clientAttributesCallback);
        validateClientResponseGateway(client, clientAttributesCallback);

        TestMqttCallback sharedAttributesCallback = getTestMqttCallback();
        client.setCallback(sharedAttributesCallback);
        validateSharedResponseGateway(client, sharedAttributesCallback);
    }

    protected void postAttributesAndSubscribeToTopic(Device savedDevice, MqttAsyncClient client, String attrPubTopic, String attrSubTopic) throws Exception {
        doPostAsync("/api/plugins/telemetry/DEVICE/" + savedDevice.getId().getId() + "/attributes/SHARED_SCOPE", POST_ATTRIBUTES_PAYLOAD, String.class, status().isOk());
        client.publish(attrPubTopic, new MqttMessage(POST_ATTRIBUTES_PAYLOAD.getBytes())).waitForCompletion(TimeUnit.MINUTES.toMillis(1));
        client.subscribe(attrSubTopic, MqttQoS.AT_MOST_ONCE.value()).waitForCompletion(TimeUnit.MINUTES.toMillis(1));
    }

    protected void postGatewayDeviceClientAttributes(MqttAsyncClient client) throws Exception {
        String postClientAttributes = "{\"" + "Gateway Device Request Attributes" + "\":{\"attribute1\":\"value1\",\"attribute2\":true,\"attribute3\":42.0,\"attribute4\":73,\"attribute5\":{\"someNumber\":42,\"someArray\":[1,2,3],\"someNestedObject\":{\"key\":\"value\"}}}}";
        client.publish(MqttTopics.GATEWAY_ATTRIBUTES_TOPIC, new MqttMessage(postClientAttributes.getBytes())).waitForCompletion(TimeUnit.MINUTES.toMillis(1));
    }

    protected void validateResponse(MqttAsyncClient client, CountDownLatch latch, TestMqttCallback callback, String attrReqTopicPrefix) throws MqttException, InterruptedException, InvalidProtocolBufferException {
        String keys = "attribute1,attribute2,attribute3,attribute4,attribute5";
        String payloadStr = "{\"clientKeys\":\"" + keys + "\", \"sharedKeys\":\"" + keys + "\"}";
        MqttMessage mqttMessage = new MqttMessage();
        mqttMessage.setPayload(payloadStr.getBytes());
        client.publish(attrReqTopicPrefix + "1", mqttMessage).waitForCompletion(TimeUnit.MINUTES.toMillis(1));
        latch.await(1, TimeUnit.MINUTES);
        assertEquals(MqttQoS.AT_MOST_ONCE.value(), callback.getQoS());
        String expectedRequestPayload = "{\"client\":{\"attribute1\":\"value1\",\"attribute2\":true,\"attribute3\":42.0,\"attribute4\":73,\"attribute5\":{\"someNumber\":42,\"someArray\":[1,2,3],\"someNestedObject\":{\"key\":\"value\"}}},\"shared\":{\"attribute1\":\"value1\",\"attribute2\":true,\"attribute3\":42.0,\"attribute4\":73,\"attribute5\":{\"someNumber\":42,\"someArray\":[1,2,3],\"someNestedObject\":{\"key\":\"value\"}}}}";
        assertEquals(JacksonUtil.toJsonNode(expectedRequestPayload), JacksonUtil.toJsonNode(new String(callback.getPayloadBytes(), StandardCharsets.UTF_8)));
    }

    protected void validateClientResponseGateway(MqttAsyncClient client, TestMqttCallback callback) throws MqttException, InterruptedException, InvalidProtocolBufferException {
        String payloadStr = "{\"id\": 1, \"device\": \"" + "Gateway Device Request Attributes" + "\", \"client\": true, \"keys\": [\"attribute1\", \"attribute2\", \"attribute3\", \"attribute4\", \"attribute5\"]}";
        MqttMessage mqttMessage = new MqttMessage();
        mqttMessage.setPayload(payloadStr.getBytes());
        client.publish(MqttTopics.GATEWAY_ATTRIBUTES_REQUEST_TOPIC, mqttMessage).waitForCompletion(TimeUnit.MINUTES.toMillis(1));
        callback.getLatch().await(1, TimeUnit.MINUTES);
        assertEquals(MqttQoS.AT_LEAST_ONCE.value(), callback.getQoS());
        String expectedRequestPayload = "{\"id\":1,\"device\":\"" + "Gateway Device Request Attributes" + "\",\"values\":{\"attribute1\":\"value1\",\"attribute2\":true,\"attribute3\":42.0,\"attribute4\":73,\"attribute5\":{\"someNumber\":42,\"someArray\":[1,2,3],\"someNestedObject\":{\"key\":\"value\"}}}}";
        assertEquals(JacksonUtil.toJsonNode(expectedRequestPayload), JacksonUtil.toJsonNode(new String(callback.getPayloadBytes(), StandardCharsets.UTF_8)));
    }

    protected void validateSharedResponseGateway(MqttAsyncClient client, TestMqttCallback callback) throws MqttException, InterruptedException, InvalidProtocolBufferException {
        String payloadStr = "{\"id\": 1, \"device\": \"" + "Gateway Device Request Attributes" + "\", \"client\": false, \"keys\": [\"attribute1\", \"attribute2\", \"attribute3\", \"attribute4\", \"attribute5\"]}";
        MqttMessage mqttMessage = new MqttMessage();
        mqttMessage.setPayload(payloadStr.getBytes());
        client.publish(MqttTopics.GATEWAY_ATTRIBUTES_REQUEST_TOPIC, mqttMessage).waitForCompletion(TimeUnit.MINUTES.toMillis(1));
        callback.getLatch().await(1, TimeUnit.MINUTES);
        assertEquals(MqttQoS.AT_LEAST_ONCE.value(), callback.getQoS());
        String expectedRequestPayload = "{\"id\":1,\"device\":\"" + "Gateway Device Request Attributes" + "\",\"values\":{\"attribute1\":\"value1\",\"attribute2\":true,\"attribute3\":42.0,\"attribute4\":73,\"attribute5\":{\"someNumber\":42,\"someArray\":[1,2,3],\"someNestedObject\":{\"key\":\"value\"}}}}";
        assertEquals(JacksonUtil.toJsonNode(expectedRequestPayload), JacksonUtil.toJsonNode(new String(callback.getPayloadBytes(), StandardCharsets.UTF_8)));
    }
}
