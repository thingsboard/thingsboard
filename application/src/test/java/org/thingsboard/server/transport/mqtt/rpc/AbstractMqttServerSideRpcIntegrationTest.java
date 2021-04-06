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
package org.thingsboard.server.transport.mqtt.rpc;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.protobuf.InvalidProtocolBufferException;
import com.nimbusds.jose.util.StandardCharset;
import io.netty.handler.codec.mqtt.MqttQoS;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.Assert;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.TransportPayloadType;
import org.thingsboard.server.common.data.device.profile.MqttTopics;
import org.thingsboard.server.dao.util.mapping.JacksonUtil;
import org.thingsboard.server.transport.mqtt.AbstractMqttIntegrationTest;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Valerii Sosliuk
 */
@Slf4j
public abstract class AbstractMqttServerSideRpcIntegrationTest extends AbstractMqttIntegrationTest {

    protected static final String DEVICE_RESPONSE = "{\"value1\":\"A\",\"value2\":\"B\"}";

    protected Long asyncContextTimeoutToUseRpcPlugin;

    protected void processBeforeTest(String deviceName, String gatewayName, TransportPayloadType payloadType, String telemetryTopic, String attributesTopic) throws Exception {
        super.processBeforeTest(deviceName, gatewayName, payloadType, telemetryTopic, attributesTopic);
        asyncContextTimeoutToUseRpcPlugin = 10000L;
    }

    protected void processOneWayRpcTest() throws Exception {
        MqttAsyncClient client = getMqttAsyncClient(accessToken);

        CountDownLatch latch = new CountDownLatch(1);
        TestMqttCallback callback = new TestMqttCallback(client, latch);
        client.setCallback(callback);

        client.subscribe(MqttTopics.DEVICE_RPC_REQUESTS_SUB_TOPIC, MqttQoS.AT_MOST_ONCE.value());

        Thread.sleep(1000);

        String setGpioRequest = "{\"method\":\"setGpio\",\"params\":{\"pin\": \"23\",\"value\": 1}}";
        String deviceId = savedDevice.getId().getId().toString();
        String result = doPostAsync("/api/plugins/rpc/oneway/" + deviceId, setGpioRequest, String.class, status().isOk());
        Assert.assertTrue(StringUtils.isEmpty(result));
        latch.await(3, TimeUnit.SECONDS);
        assertEquals(MqttQoS.AT_MOST_ONCE.value(), callback.getQoS());
    }

    protected void processOneWayRpcTestGateway(String deviceName) throws Exception {
        MqttAsyncClient client = getMqttAsyncClient(gatewayAccessToken);
        String payload = "{\"device\":\"" + deviceName + "\"}";
        byte[] payloadBytes = payload.getBytes();
        validateOneWayRpcGatewayResponse(deviceName, client, payloadBytes);
    }

    protected void processTwoWayRpcTest() throws Exception {
        MqttAsyncClient client = getMqttAsyncClient(accessToken);
        client.subscribe(MqttTopics.DEVICE_RPC_REQUESTS_SUB_TOPIC, 1);

        CountDownLatch latch = new CountDownLatch(1);
        TestMqttCallback callback = new TestMqttCallback(client, latch);
        client.setCallback(callback);

        Thread.sleep(1000);

        String setGpioRequest = "{\"method\":\"setGpio\",\"params\":{\"pin\": \"26\",\"value\": 1}}";
        String deviceId = savedDevice.getId().getId().toString();

        String result = doPostAsync("/api/plugins/rpc/twoway/" + deviceId, setGpioRequest, String.class, status().isOk());
        String expected = "{\"value1\":\"A\",\"value2\":\"B\"}";
        latch.await(3, TimeUnit.SECONDS);
        Assert.assertEquals(expected, result);
    }

    protected void processTwoWayRpcTestGateway(String deviceName) throws Exception {
        MqttAsyncClient client = getMqttAsyncClient(gatewayAccessToken);

        String payload = "{\"device\":\"" + deviceName + "\"}";
        byte[] payloadBytes = payload.getBytes();

        validateTwoWayRpcGateway(deviceName, client, payloadBytes);
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
        TestMqttCallback callback = new TestMqttCallback(client, latch);
        client.setCallback(callback);

        client.subscribe(MqttTopics.GATEWAY_RPC_TOPIC, MqttQoS.AT_MOST_ONCE.value());

        Thread.sleep(1000);

        String setGpioRequest = "{\"method\": \"toggle_gpio\", \"params\": {\"pin\":1}}";
        String deviceId = savedDevice.getId().getId().toString();
        String result = doPostAsync("/api/plugins/rpc/oneway/" + deviceId, setGpioRequest, String.class, status().isOk());
        Assert.assertTrue(StringUtils.isEmpty(result));
        latch.await(3, TimeUnit.SECONDS);
        assertEquals(MqttQoS.AT_MOST_ONCE.value(), callback.getQoS());
    }

    protected void validateTwoWayRpcGateway(String deviceName, MqttAsyncClient client, byte[] payloadBytes) throws Exception {
        publishMqttMsg(client, payloadBytes, MqttTopics.GATEWAY_CONNECT_TOPIC);

        Device savedDevice = doExecuteWithRetriesAndInterval(
                () -> getDeviceByName(deviceName),
                20,
                100
        );
        assertNotNull(savedDevice);

        CountDownLatch latch = new CountDownLatch(1);
        TestMqttCallback callback = new TestMqttCallback(client, latch);
        client.setCallback(callback);

        client.subscribe(MqttTopics.GATEWAY_RPC_TOPIC, MqttQoS.AT_MOST_ONCE.value());

        Thread.sleep(1000);

        String setGpioRequest = "{\"method\": \"toggle_gpio\", \"params\": {\"pin\":1}}";
        String deviceId = savedDevice.getId().getId().toString();
        String result = doPostAsync("/api/plugins/rpc/twoway/" + deviceId, setGpioRequest, String.class, status().isOk());
        latch.await(3, TimeUnit.SECONDS);
        String expected = "{\"success\":true}";
        assertEquals(expected, result);
        assertEquals(MqttQoS.AT_MOST_ONCE.value(), callback.getQoS());
    }

    private Device getDeviceByName(String deviceName) throws Exception {
        return doGet("/api/tenant/devices?deviceName=" + deviceName, Device.class);
    }

    protected MqttMessage processMessageArrived(String requestTopic, MqttMessage mqttMessage) throws MqttException, InvalidProtocolBufferException {
        MqttMessage message = new MqttMessage();
        if (requestTopic.startsWith(MqttTopics.BASE_DEVICE_API_TOPIC)) {
            message.setPayload(DEVICE_RESPONSE.getBytes(StandardCharset.UTF_8));
        } else {
            JsonNode requestMsgNode = JacksonUtil.toJsonNode(new String(mqttMessage.getPayload(), StandardCharset.UTF_8));
            String deviceName = requestMsgNode.get("device").asText();
            int requestId = requestMsgNode.get("data").get("id").asInt();
            message.setPayload(("{\"device\": \"" + deviceName + "\", \"id\": " + requestId + ", \"data\": {\"success\": true}}").getBytes(StandardCharset.UTF_8));
        }
        return message;
    }

    protected class TestMqttCallback implements MqttCallback {

        private final MqttAsyncClient client;
        private final CountDownLatch latch;
        private Integer qoS;

        TestMqttCallback(MqttAsyncClient client, CountDownLatch latch) {
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
            String responseTopic = requestTopic.replace("request", "response");
            qoS = mqttMessage.getQos();
            client.publish(responseTopic, processMessageArrived(requestTopic, mqttMessage));
            latch.countDown();
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

        }
    }
}
