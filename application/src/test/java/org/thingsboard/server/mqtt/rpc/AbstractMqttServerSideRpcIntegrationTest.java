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

import com.datastax.driver.core.utils.UUIDs;
import io.netty.handler.codec.mqtt.MqttQoS;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.*;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.controller.AbstractControllerTest;
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
    }

    @After
    public void afterTest() throws Exception {
        loginSysAdmin();
        if (savedTenant != null) {
            doDelete("/api/tenant/" + savedTenant.getId().getId().toString()).andExpect(status().isOk());
        }
    }

    @Test
    public void testServerMqttOneWayRpc() throws Exception {
        Device device = new Device();
        device.setName("Test One-Way Server-Side RPC");
        device.setType("default");
        Device savedDevice = getSavedDevice(device);
        DeviceCredentials deviceCredentials = getDeviceCredentials(savedDevice);
        assertEquals(savedDevice.getId(), deviceCredentials.getDeviceId());
        String accessToken = deviceCredentials.getCredentialsId();
        assertNotNull(accessToken);

        String clientId = MqttAsyncClient.generateClientId();
        MqttAsyncClient client = new MqttAsyncClient(MQTT_URL, clientId);

        MqttConnectOptions options = new MqttConnectOptions();
        options.setUserName(accessToken);
        client.connect(options).waitForCompletion();

        CountDownLatch latch = new CountDownLatch(1);
        TestMqttCallback callback = new TestMqttCallback(client, latch);
        client.setCallback(callback);

        client.subscribe("v1/devices/me/rpc/request/+", MqttQoS.AT_MOST_ONCE.value());

        Thread.sleep(2000);

        String setGpioRequest = "{\"method\":\"setGpio\",\"params\":{\"pin\": \"23\",\"value\": 1}}";
        String deviceId = savedDevice.getId().getId().toString();
        String result = doPostAsync("/api/plugins/rpc/oneway/" + deviceId, setGpioRequest, String.class, status().isOk());
        Assert.assertTrue(StringUtils.isEmpty(result));
        latch.await(3, TimeUnit.SECONDS);
        assertEquals(MqttQoS.AT_MOST_ONCE.value(), callback.getQoS());
    }

    @Test
    public void testServerMqttOneWayRpcDeviceOffline() throws Exception {
        Device device = new Device();
        device.setName("Test One-Way Server-Side RPC Device Offline");
        device.setType("default");
        Device savedDevice = getSavedDevice(device);
        DeviceCredentials deviceCredentials = getDeviceCredentials(savedDevice);
        assertEquals(savedDevice.getId(), deviceCredentials.getDeviceId());
        String accessToken = deviceCredentials.getCredentialsId();
        assertNotNull(accessToken);

        String setGpioRequest = "{\"method\":\"setGpio\",\"params\":{\"pin\": \"24\",\"value\": 1},\"timeout\": 6000}";
        String deviceId = savedDevice.getId().getId().toString();

        doPostAsync("/api/plugins/rpc/oneway/" + deviceId, setGpioRequest, String.class, status().is(409),
                asyncContextTimeoutToUseRpcPlugin);
    }

    @Test
    public void testServerMqttOneWayRpcDeviceDoesNotExist() throws Exception {
        String setGpioRequest = "{\"method\":\"setGpio\",\"params\":{\"pin\": \"25\",\"value\": 1}}";
        String nonExistentDeviceId = UUIDs.timeBased().toString();

        String result = doPostAsync("/api/plugins/rpc/oneway/" + nonExistentDeviceId, setGpioRequest, String.class,
                status().isNotFound());
        Assert.assertEquals(AccessValidator.DEVICE_WITH_REQUESTED_ID_NOT_FOUND, result);
    }

    @Test
    public void testServerMqttTwoWayRpc() throws Exception {
        Device device = new Device();
        device.setName("Test Two-Way Server-Side RPC");
        device.setType("default");
        Device savedDevice = getSavedDevice(device);
        DeviceCredentials deviceCredentials = getDeviceCredentials(savedDevice);
        assertEquals(savedDevice.getId(), deviceCredentials.getDeviceId());
        String accessToken = deviceCredentials.getCredentialsId();
        assertNotNull(accessToken);

        String clientId = MqttAsyncClient.generateClientId();
        MqttAsyncClient client = new MqttAsyncClient(MQTT_URL, clientId);

        MqttConnectOptions options = new MqttConnectOptions();
        options.setUserName(accessToken);
        client.connect(options).waitForCompletion();
        client.subscribe("v1/devices/me/rpc/request/+", 1);
        client.setCallback(new TestMqttCallback(client, new CountDownLatch(1)));

        Thread.sleep(2000);

        String setGpioRequest = "{\"method\":\"setGpio\",\"params\":{\"pin\": \"26\",\"value\": 1}}";
        String deviceId = savedDevice.getId().getId().toString();

        String result = doPostAsync("/api/plugins/rpc/twoway/" + deviceId, setGpioRequest, String.class, status().isOk());
        Assert.assertEquals("{\"value1\":\"A\",\"value2\":\"B\"}", result);
    }

    @Test
    public void testServerMqttTwoWayRpcDeviceOffline() throws Exception {
        Device device = new Device();
        device.setName("Test Two-Way Server-Side RPC Device Offline");
        device.setType("default");
        Device savedDevice = getSavedDevice(device);
        DeviceCredentials deviceCredentials = getDeviceCredentials(savedDevice);
        assertEquals(savedDevice.getId(), deviceCredentials.getDeviceId());
        String accessToken = deviceCredentials.getCredentialsId();
        assertNotNull(accessToken);

        String setGpioRequest = "{\"method\":\"setGpio\",\"params\":{\"pin\": \"27\",\"value\": 1},\"timeout\": 6000}";
        String deviceId = savedDevice.getId().getId().toString();

        doPostAsync("/api/plugins/rpc/twoway/" + deviceId, setGpioRequest, String.class, status().is(409),
                asyncContextTimeoutToUseRpcPlugin);
    }

    @Test
    public void testServerMqttTwoWayRpcDeviceDoesNotExist() throws Exception {
        String setGpioRequest = "{\"method\":\"setGpio\",\"params\":{\"pin\": \"28\",\"value\": 1}}";
        String nonExistentDeviceId = UUIDs.timeBased().toString();

        String result = doPostAsync("/api/plugins/rpc/twoway/" + nonExistentDeviceId, setGpioRequest, String.class,
                status().isNotFound());
        Assert.assertEquals(AccessValidator.DEVICE_WITH_REQUESTED_ID_NOT_FOUND, result);
    }

    private Device getSavedDevice(Device device) throws Exception {
        return doPost("/api/device", device, Device.class);
    }

    private DeviceCredentials getDeviceCredentials(Device savedDevice) throws Exception {
        return doGet("/api/device/" + savedDevice.getId().getId().toString() + "/credentials", DeviceCredentials.class);
    }

    private static class TestMqttCallback implements MqttCallback {

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
            message.setPayload("{\"value1\":\"A\", \"value2\":\"B\"}".getBytes("UTF-8"));
            qoS = mqttMessage.getQos();
            client.publish(responseTopic, message);
            latch.countDown();
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

        }
    }
}
