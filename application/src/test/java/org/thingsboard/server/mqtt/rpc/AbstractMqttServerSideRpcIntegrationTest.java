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
package org.thingsboard.server.mqtt.rpc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nimbusds.jose.util.StandardCharset;
import com.datastax.oss.driver.api.core.uuid.Uuids;
import io.netty.handler.codec.mqtt.MqttQoS;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.device.profile.MqttTopics;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.dao.util.mapping.JacksonUtil;
import org.thingsboard.server.gen.transport.TransportApiProtos;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.service.security.AccessValidator;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Valerii Sosliuk
 */
@Slf4j
public abstract class AbstractMqttServerSideRpcIntegrationTest extends AbstractControllerTest {

    private static final String MQTT_URL = "tcp://localhost:1883";
    private static final Long TIME_TO_HANDLE_REQUEST = 500L;

    private Tenant savedTenant;
    private User tenantAdmin;

    private Device savedDevice;
    private String accessToken;

    private Device savedGateway;
    private String gatewayAccessToken;


    private Long asyncContextTimeoutToUseRpcPlugin;

    private static final AtomicInteger atomicInteger = new AtomicInteger(2);


    @Before
    public void beforeTest() throws Exception {
        loginSysAdmin();

        asyncContextTimeoutToUseRpcPlugin = 10000L;

        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        savedTenant = doPost("/api/tenant", tenant, Tenant.class);
        Assert.assertNotNull(savedTenant);

        tenantAdmin = new User();
        tenantAdmin.setAuthority(Authority.TENANT_ADMIN);
        tenantAdmin.setTenantId(savedTenant.getId());
        tenantAdmin.setEmail("tenant" + atomicInteger.getAndIncrement() + "@thingsboard.org");
        tenantAdmin.setFirstName("Joe");
        tenantAdmin.setLastName("Downs");

        createUserAndLogin(tenantAdmin, "testPassword1");

        Device device = new Device();
        device.setName("RPC test device");
        device.setType("default");
        savedDevice = getSavedDevice(device);

        DeviceCredentials deviceCredentials = getDeviceCredentials(savedDevice);

        assertEquals(savedDevice.getId(), deviceCredentials.getDeviceId());
        accessToken = deviceCredentials.getCredentialsId();
        assertNotNull(accessToken);

        Device gateway = new Device();
        gateway.setName("RPC test gateway");
        gateway.setType("default");
        ObjectNode additionalInfo = mapper.createObjectNode();
        additionalInfo.put("gateway", true);
        gateway.setAdditionalInfo(additionalInfo);
        savedGateway = doPost("/api/device", gateway, Device.class);

        DeviceCredentials gatewayCredentials = getDeviceCredentials(savedGateway);

        assertEquals(savedGateway.getId(), gatewayCredentials.getDeviceId());
        gatewayAccessToken = gatewayCredentials.getCredentialsId();
        assertNotNull(gatewayAccessToken);
    }

    @After
    public void afterTest() throws Exception {
        loginSysAdmin();
        if (savedTenant != null) {
            doDelete("/api/tenant/" + savedTenant.getId().getId().toString()).andExpect(status().isOk());
        }
    }

    @Test
    public void testServerMqttOneWayRpcV1Json() throws Exception {
        processOneWayRpcTest(MqttTopics.DEVICE_RPC_REQUESTS_SUB_TOPIC_V1_JSON);
    }

    @Test
    public void testServerMqttOneWayRpcV2Json() throws Exception {
        processOneWayRpcTest(MqttTopics.DEVICE_RPC_REQUESTS_SUB_TOPIC_V2_JSON);
    }

    @Test
    public void testServerMqttOneWayRpcV2Proto() throws Exception {
        processOneWayRpcTest(MqttTopics.DEVICE_RPC_REQUESTS_SUB_TOPIC_V2_PROTO);
    }

    @Test
    public void testServerMqttTwoWayRpcV1Json() throws Exception {
        processTwoWayRpcTest(MqttTopics.DEVICE_RPC_REQUESTS_SUB_TOPIC_V1_JSON);
    }

    @Test
    public void testServerMqttTwoWayRpcV2Json() throws Exception {
        processTwoWayRpcTest(MqttTopics.DEVICE_RPC_REQUESTS_SUB_TOPIC_V2_JSON);
    }

    @Test
    public void testServerMqttTwoWayRpcV2Proto() throws Exception {
        processTwoWayRpcTest(MqttTopics.DEVICE_RPC_REQUESTS_SUB_TOPIC_V2_PROTO);
    }

    @Test
    public void testServerMqttOneWayRpcDeviceOffline() throws Exception {
        String setGpioRequest = "{\"method\":\"setGpio\",\"params\":{\"pin\": \"24\",\"value\": 1},\"timeout\": 6000}";
        String deviceId = savedDevice.getId().getId().toString();

        doPostAsync("/api/plugins/rpc/oneway/" + deviceId, setGpioRequest, String.class, status().is(409),
                asyncContextTimeoutToUseRpcPlugin);
    }

    @Test
    public void testServerMqttOneWayRpcDeviceDoesNotExist() throws Exception {
        String setGpioRequest = "{\"method\":\"setGpio\",\"params\":{\"pin\": \"25\",\"value\": 1}}";
        String nonExistentDeviceId = Uuids.timeBased().toString();

        String result = doPostAsync("/api/plugins/rpc/oneway/" + nonExistentDeviceId, setGpioRequest, String.class,
                status().isNotFound());
        Assert.assertEquals(AccessValidator.DEVICE_WITH_REQUESTED_ID_NOT_FOUND, result);
    }

    @Test
    public void testServerMqttTwoWayRpcDeviceOffline() throws Exception {
        String setGpioRequest = "{\"method\":\"setGpio\",\"params\":{\"pin\": \"27\",\"value\": 1},\"timeout\": 6000}";
        String deviceId = savedDevice.getId().getId().toString();

        doPostAsync("/api/plugins/rpc/twoway/" + deviceId, setGpioRequest, String.class, status().is(409),
                asyncContextTimeoutToUseRpcPlugin);
    }

    @Test
    public void testServerMqttTwoWayRpcDeviceDoesNotExist() throws Exception {
        String setGpioRequest = "{\"method\":\"setGpio\",\"params\":{\"pin\": \"28\",\"value\": 1}}";
        String nonExistentDeviceId = Uuids.timeBased().toString();

        String result = doPostAsync("/api/plugins/rpc/twoway/" + nonExistentDeviceId, setGpioRequest, String.class,
                status().isNotFound());
        Assert.assertEquals(AccessValidator.DEVICE_WITH_REQUESTED_ID_NOT_FOUND, result);
    }

    @Test
    public void testGatewayServerMqttOneWayRpcV1Json() throws Exception {
        processOneWayRpcTestGateway(MqttTopics.GATEWAY_CONNECT_TOPIC_V1_JSON, MqttTopics.GATEWAY_RPC_TOPIC_V1_JSON, "Gateway Device RPC V1 Json");
    }

    @Test
    public void testGatewayServerMqttOneWayRpcV2Json() throws Exception {
        processOneWayRpcTestGateway(MqttTopics.GATEWAY_CONNECT_TOPIC_V2_JSON, MqttTopics.GATEWAY_RPC_TOPIC_V2_JSON, "Gateway Device RPC V2 Json");
    }

    @Test
    public void testGatewayServerMqttOneWayRpcV2Proto() throws Exception {
        processOneWayRpcTestGateway(MqttTopics.GATEWAY_CONNECT_TOPIC_V2_PROTO, MqttTopics.GATEWAY_RPC_TOPIC_V2_PROTO, "Gateway Device RPC V2 Proto");
    }

    @Test
    public void testGatewayServerMqttTwoWayRpcV1Json() throws Exception {
        processTwoWayRpcTestGateway(MqttTopics.GATEWAY_CONNECT_TOPIC_V1_JSON, MqttTopics.GATEWAY_RPC_TOPIC_V1_JSON, "Gateway Device TwoWay RPC V1 Json");
    }

    @Test
    public void testGatewayServerMqttTwoWayRpcV2Json() throws Exception {
        processTwoWayRpcTestGateway(MqttTopics.GATEWAY_CONNECT_TOPIC_V2_JSON, MqttTopics.GATEWAY_RPC_TOPIC_V2_JSON, "Gateway Device TwoWay RPC V2 Json");
    }

    @Test
    public void testGatewayServerMqttTwoWayRpcV2Proto() throws Exception {
        processTwoWayRpcTestGateway(MqttTopics.GATEWAY_CONNECT_TOPIC_V2_PROTO, MqttTopics.GATEWAY_RPC_TOPIC_V2_PROTO, "Gateway Device TwoWay RPC V2 Proto");
    }

    private void processOneWayRpcTest(String topicToSubscribe) throws Exception {
        MqttAsyncClient client = getMqttAsyncClient(accessToken);

        CountDownLatch latch = new CountDownLatch(1);
        TestMqttCallback callback = new TestMqttCallback(client, latch);
        client.setCallback(callback);

        client.subscribe(topicToSubscribe, MqttQoS.AT_MOST_ONCE.value());

        Thread.sleep(2000);

        String setGpioRequest = "{\"method\":\"setGpio\",\"params\":{\"pin\": \"23\",\"value\": 1}}";
        String deviceId = savedDevice.getId().getId().toString();
        String result = doPostAsync("/api/plugins/rpc/oneway/" + deviceId, setGpioRequest, String.class, status().isOk());
        Assert.assertTrue(StringUtils.isEmpty(result));
        latch.await(3, TimeUnit.SECONDS);
        assertEquals(MqttQoS.AT_MOST_ONCE.value(), callback.getQoS());
    }

    private void processOneWayRpcTestGateway(String topicToConnect, String topicToSubscribe, String deviceName) throws Exception {
        MqttAsyncClient client = getMqttAsyncClient(gatewayAccessToken);

        byte[] payloadBytes;
        if (topicToConnect.startsWith(MqttTopics.BASE_GATEWAY_API_TOPIC_V1_JSON) || topicToConnect.startsWith(MqttTopics.BASE_GATEWAY_API_TOPIC_V2_JSON)) {
            String payload = "{\"device\":\"" + deviceName + "\"}";
            payloadBytes = payload.getBytes();
        } else {
            TransportApiProtos.ConnectMsg connectMsgProto = getConnectProto(deviceName);
            payloadBytes = connectMsgProto.toByteArray();
        }

        publishMqttMsg(client, payloadBytes, topicToConnect);

        Thread.sleep(2000);

        Device savedDevice = getDeviceByName(deviceName);
        assertNotNull(savedDevice);

        CountDownLatch latch = new CountDownLatch(1);
        TestMqttCallback callback = new TestMqttCallback(client, latch);
        client.setCallback(callback);

        client.subscribe(topicToSubscribe, MqttQoS.AT_MOST_ONCE.value());

        Thread.sleep(2000);

        String setGpioRequest = "{\"method\": \"toggle_gpio\", \"params\": {\"pin\":1}}";
        String deviceId = savedDevice.getId().getId().toString();
        String result = doPostAsync("/api/plugins/rpc/oneway/" + deviceId, setGpioRequest, String.class, status().isOk());
        Assert.assertTrue(StringUtils.isEmpty(result));
        latch.await(3, TimeUnit.SECONDS);
        assertEquals(MqttQoS.AT_MOST_ONCE.value(), callback.getQoS());
    }

    private void processTwoWayRpcTest(String topicToSubscribe) throws Exception {
        MqttAsyncClient client = getMqttAsyncClient(accessToken);
        client.subscribe(topicToSubscribe, 1);

        CountDownLatch latch = new CountDownLatch(1);
        TestMqttCallback callback = new TestMqttCallback(client, latch);
        client.setCallback(callback);

        Thread.sleep(2000);

        String setGpioRequest = "{\"method\":\"setGpio\",\"params\":{\"pin\": \"26\",\"value\": 1}}";
        String deviceId = savedDevice.getId().getId().toString();

        String result = doPostAsync("/api/plugins/rpc/twoway/" + deviceId, setGpioRequest, String.class, status().isOk());
        String expected = "{\"value1\":\"A\",\"value2\":\"B\"}";
        latch.await(3, TimeUnit.SECONDS);
        Assert.assertEquals(expected, result);
    }

    private void processTwoWayRpcTestGateway(String topicToConnect, String topicToSubscribe, String deviceName) throws Exception {
        MqttAsyncClient client = getMqttAsyncClient(gatewayAccessToken);

        byte[] payloadBytes;
        if (topicToConnect.startsWith(MqttTopics.BASE_GATEWAY_API_TOPIC_V1_JSON) || topicToConnect.startsWith(MqttTopics.BASE_GATEWAY_API_TOPIC_V2_JSON)) {
            String payload = "{\"device\":\"" + deviceName + "\"}";
            payloadBytes = payload.getBytes();
        } else {
            TransportApiProtos.ConnectMsg connectMsgProto = getConnectProto(deviceName);
            payloadBytes = connectMsgProto.toByteArray();
        }

        publishMqttMsg(client, payloadBytes, topicToConnect);

        Thread.sleep(2000);

        Device savedDevice = getDeviceByName(deviceName);
        assertNotNull(savedDevice);

        CountDownLatch latch = new CountDownLatch(1);
        TestMqttCallback callback = new TestMqttCallback(client, latch);
        client.setCallback(callback);

        client.subscribe(topicToSubscribe, MqttQoS.AT_MOST_ONCE.value());

        Thread.sleep(2000);

        String setGpioRequest = "{\"method\": \"toggle_gpio\", \"params\": {\"pin\":1}}";
        String deviceId = savedDevice.getId().getId().toString();
        String result = doPostAsync("/api/plugins/rpc/twoway/" + deviceId, setGpioRequest, String.class, status().isOk());
        latch.await(3, TimeUnit.SECONDS);
        String expected = "{\"success\":true}";
        Assert.assertEquals(expected, result);
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

    private Device getSavedDevice(Device device) throws Exception {
        return doPost("/api/device", device, Device.class);
    }

    private Device getDeviceByName(String deviceName) throws Exception {
        return doGet("/api/tenant/devices?deviceName=" + deviceName, Device.class);
    }

    private DeviceCredentials getDeviceCredentials(Device savedDevice) throws Exception {
        return doGet("/api/device/" + savedDevice.getId().getId().toString() + "/credentials", DeviceCredentials.class);
    }

    private MqttAsyncClient getMqttAsyncClient(String accessToken) throws MqttException {
        String clientId = MqttAsyncClient.generateClientId();
        MqttAsyncClient client = new MqttAsyncClient(MQTT_URL, clientId);

        MqttConnectOptions options = new MqttConnectOptions();
        options.setUserName(accessToken);
        client.connect(options).waitForCompletion();
        return client;
    }

    private static class TestMqttCallback implements MqttCallback {

        private static final String DEVICE_RESPONSE = "{\"value1\":\"A\",\"value2\":\"B\"}";
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
            MqttMessage message = new MqttMessage();
            String responseTopic = requestTopic.replace("request", "response");

            if (requestTopic.startsWith(MqttTopics.BASE_DEVICE_API_TOPIC_V1_JSON) || requestTopic.startsWith(MqttTopics.BASE_DEVICE_API_TOPIC_V2_JSON)) {
                message.setPayload(DEVICE_RESPONSE.getBytes(StandardCharset.UTF_8));
            } else if (requestTopic.startsWith(MqttTopics.BASE_DEVICE_API_TOPIC_V2_PROTO)) {
                TransportProtos.ToDeviceRpcResponseMsg toDeviceRpcResponseMsg = TransportProtos.ToDeviceRpcResponseMsg.newBuilder()
                        .setPayload(DEVICE_RESPONSE)
                        .setRequestId(0)
                        .build();
                message.setPayload(toDeviceRpcResponseMsg.toByteArray());
            } else if (requestTopic.startsWith(MqttTopics.BASE_GATEWAY_API_TOPIC_V1_JSON) || requestTopic.startsWith(MqttTopics.BASE_GATEWAY_API_TOPIC_V2_JSON)) {
                JsonNode requestMsgNode = JacksonUtil.toJsonNode(new String(mqttMessage.getPayload(), StandardCharset.UTF_8));
                String deviceName = requestMsgNode.get("device").asText();
                int requestId = requestMsgNode.get("data").get("id").asInt();
                message.setPayload(("{\"device\": \"" + deviceName + "\", \"id\": " + requestId + ", \"data\": {\"success\": true}}").getBytes(StandardCharset.UTF_8));
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

            qoS = mqttMessage.getQos();
            client.publish(responseTopic, message);
            latch.countDown();
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

        }
    }
}
