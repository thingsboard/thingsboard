/**
 * Copyright © 2016-2020 The Thingsboard Authors
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.mqtt.rpc;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.protobuf.InvalidProtocolBufferException;
import com.nimbusds.jose.util.StandardCharset;
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
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceProfileType;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.TransportPayloadType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.device.profile.DefaultDeviceProfileConfiguration;
import org.thingsboard.server.common.data.device.profile.DeviceProfileData;
import org.thingsboard.server.common.data.device.profile.MqttDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.MqttTopics;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.dao.util.mapping.JacksonUtil;
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

    protected static final String DEVICE_RESPONSE = "{\"value1\":\"A\",\"value2\":\"B\"}";
    private static final String MQTT_URL = "tcp://localhost:1883";

    private Tenant savedTenant;
    private User tenantAdmin;

    protected Device savedDevice;
    protected String accessToken;

    protected Device savedGateway;
    protected String gatewayAccessToken;


    protected Long asyncContextTimeoutToUseRpcPlugin;

    private static final AtomicInteger atomicInteger = new AtomicInteger(2);

    protected void processBeforeTest(TransportPayloadType transportPayloadType) throws Exception {
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

        Device gateway = new Device();
        gateway.setName("RPC test gateway");
        gateway.setType("default");
        ObjectNode additionalInfo = mapper.createObjectNode();
        additionalInfo.put("gateway", true);
        gateway.setAdditionalInfo(additionalInfo);

        if (transportPayloadType != null) {
            DeviceProfile mqttDeviceProfile = createMqttDeviceProfile(transportPayloadType);
            DeviceProfile savedDeviceProfile = doPost("/api/deviceProfile", mqttDeviceProfile, DeviceProfile.class);
            device.setDeviceProfileId(savedDeviceProfile.getId());
            gateway.setDeviceProfileId(savedDeviceProfile.getId());
        }

        savedDevice = getSavedDevice(device);

        DeviceCredentials deviceCredentials = getDeviceCredentials(savedDevice);

        assertEquals(savedDevice.getId(), deviceCredentials.getDeviceId());
        accessToken = deviceCredentials.getCredentialsId();
        assertNotNull(accessToken);

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

    protected void processOneWayRpcTest() throws Exception {
        MqttAsyncClient client = getMqttAsyncClient(accessToken);

        CountDownLatch latch = new CountDownLatch(1);
        TestMqttCallback callback = new TestMqttCallback(client, latch);
        client.setCallback(callback);

        client.subscribe(MqttTopics.DEVICE_RPC_REQUESTS_SUB_TOPIC, MqttQoS.AT_MOST_ONCE.value());

        Thread.sleep(2000);

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

        Thread.sleep(2000);

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

        Thread.sleep(2000);

        Device savedDevice = getDeviceByName(deviceName);
        assertNotNull(savedDevice);

        CountDownLatch latch = new CountDownLatch(1);
        TestMqttCallback callback = new TestMqttCallback(client, latch);
        client.setCallback(callback);

        client.subscribe(MqttTopics.GATEWAY_RPC_TOPIC, MqttQoS.AT_MOST_ONCE.value());

        Thread.sleep(2000);

        String setGpioRequest = "{\"method\": \"toggle_gpio\", \"params\": {\"pin\":1}}";
        String deviceId = savedDevice.getId().getId().toString();
        String result = doPostAsync("/api/plugins/rpc/oneway/" + deviceId, setGpioRequest, String.class, status().isOk());
        Assert.assertTrue(StringUtils.isEmpty(result));
        latch.await(3, TimeUnit.SECONDS);
        assertEquals(MqttQoS.AT_MOST_ONCE.value(), callback.getQoS());
    }

    protected void validateTwoWayRpcGateway(String deviceName, MqttAsyncClient client, byte[] payloadBytes) throws Exception {
        publishMqttMsg(client, payloadBytes, MqttTopics.GATEWAY_CONNECT_TOPIC);

        Thread.sleep(2000);

        Device savedDevice = getDeviceByName(deviceName);
        assertNotNull(savedDevice);

        CountDownLatch latch = new CountDownLatch(1);
        TestMqttCallback callback = new TestMqttCallback(client, latch);
        client.setCallback(callback);

        client.subscribe(MqttTopics.GATEWAY_RPC_TOPIC, MqttQoS.AT_MOST_ONCE.value());

        Thread.sleep(2000);

        String setGpioRequest = "{\"method\": \"toggle_gpio\", \"params\": {\"pin\":1}}";
        String deviceId = savedDevice.getId().getId().toString();
        String result = doPostAsync("/api/plugins/rpc/twoway/" + deviceId, setGpioRequest, String.class, status().isOk());
        latch.await(3, TimeUnit.SECONDS);
        String expected = "{\"success\":true}";
        Assert.assertEquals(expected, result);
    }

    protected MqttAsyncClient getMqttAsyncClient(String accessToken) throws MqttException {
        String clientId = MqttAsyncClient.generateClientId();
        MqttAsyncClient client = new MqttAsyncClient(MQTT_URL, clientId);

        MqttConnectOptions options = new MqttConnectOptions();
        options.setUserName(accessToken);
        client.connect(options).waitForCompletion();
        return client;
    }

    private void publishMqttMsg(MqttAsyncClient client, byte[] payload, String topic) throws MqttException {
        MqttMessage message = new MqttMessage();
        message.setPayload(payload);
        client.publish(topic, message);
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

    private DeviceProfile createMqttDeviceProfile(TransportPayloadType transportPayloadType) {
        DeviceProfile deviceProfile = new DeviceProfile();
        deviceProfile.setName(transportPayloadType.name());
        deviceProfile.setType(DeviceProfileType.DEFAULT);
        deviceProfile.setTransportType(DeviceTransportType.MQTT);
        deviceProfile.setDescription(transportPayloadType.name() + " Test");
        DeviceProfileData deviceProfileData = new DeviceProfileData();
        DefaultDeviceProfileConfiguration configuration = new DefaultDeviceProfileConfiguration();
        MqttDeviceProfileTransportConfiguration transportConfiguration = new MqttDeviceProfileTransportConfiguration();
        transportConfiguration.setTransportPayloadType(transportPayloadType);
        deviceProfileData.setTransportConfiguration(transportConfiguration);
        deviceProfileData.setConfiguration(configuration);
        deviceProfile.setProfileData(deviceProfileData);
        deviceProfile.setDefault(false);
        deviceProfile.setDefaultRuleChainId(null);
        return deviceProfile;
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

    private class TestMqttCallback implements MqttCallback {

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
