/**
 * Copyright © 2016-2017 The Thingsboard Authors
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

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.paho.client.mqttv3.*;
import org.junit.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.dao.service.DaoNoSqlTest;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Valerii Sosliuk
 */
@Slf4j
public abstract class AbstractMqttServerSideRpcIntegrationTest extends AbstractControllerTest {

    private static final String MQTT_URL = "tcp://localhost:1883";
    private static final String FAIL_MSG_IF_HTTP_CLIENT_ERROR_NOT_ENCOUNTERED = "HttpClientErrorException expected, but not encountered";

    private Tenant savedTenant;
    private User tenantAdmin;

    @Before
    public void beforeTest() throws Exception {
        loginSysAdmin();

        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        savedTenant = doPost("/api/tenant", tenant, Tenant.class);
        Assert.assertNotNull(savedTenant);

        tenantAdmin = new User();
        tenantAdmin.setAuthority(Authority.TENANT_ADMIN);
        tenantAdmin.setTenantId(savedTenant.getId());
        tenantAdmin.setEmail("tenant2@thingsboard.org");
        tenantAdmin.setFirstName("Joe");
        tenantAdmin.setLastName("Downs");

        createUserAndLogin(tenantAdmin, "testPassword1");
    }

    @After
    public void afterTest() throws Exception {
        loginSysAdmin();
        if (savedTenant != null) {
            doDelete("/api/tenant/" + savedTenant.getId().getId().toString())
                    .andExpect(status().isOk());
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
        client.subscribe("v1/devices/me/rpc/request/+", 1);
        client.setCallback(new TestMqttCallback(client));

        String setGpioRequest = "{\"method\":\"setGpio\",\"params\":{\"pin\": \"23\",\"value\": 1}}";
        String deviceId = savedDevice.getId().getId().toString();
        String result = doPostAsync("/api/plugins/rpc/oneway/" + deviceId, setGpioRequest, String.class, status().isOk());
        Assert.assertTrue(StringUtils.isEmpty(result));
    }

    @Test
    @Ignore // TODO: figure out the right error code for this case. Ignored due to failure: expected 408 but was: 200
    public void testServerMqttOneWayRpcDeviceOffline() throws Exception {
        Device device = new Device();
        device.setName("Test One-Way Server-Side RPC Device Offline");
        device.setType("default");
        Device savedDevice = getSavedDevice(device);
        DeviceCredentials deviceCredentials = getDeviceCredentials(savedDevice);
        assertEquals(savedDevice.getId(), deviceCredentials.getDeviceId());
        String accessToken = deviceCredentials.getCredentialsId();
        assertNotNull(accessToken);

        String setGpioRequest = "{\"method\":\"setGpio\",\"params\":{\"pin\": \"23\",\"value\": 1}}";
        String deviceId = savedDevice.getId().getId().toString();
        try {
            doPost("/api/plugins/rpc/oneway/" + deviceId, setGpioRequest, String.class, status().is(408));
            Assert.fail(FAIL_MSG_IF_HTTP_CLIENT_ERROR_NOT_ENCOUNTERED);
        } catch (HttpClientErrorException e) {
            log.error(e.getMessage(), e);
            Assert.assertEquals(HttpStatus.REQUEST_TIMEOUT, e.getStatusCode());
            Assert.assertEquals("408 null", e.getMessage());
        }
    }

    @Test
    @Ignore // TODO: figure out the right error code for this case. Ignored due to failure: expected 400 (404?) but was: 401
    public void testServerMqttOneWayRpcDeviceDoesNotExist() throws Exception {
        String setGpioRequest = "{\"method\":\"setGpio\",\"params\":{\"pin\": \"23\",\"value\": 1}}";
        String nonExistentDeviceId = UUID.randomUUID().toString();
        try {
            doPostAsync("/api/plugins/rpc/oneway/" + nonExistentDeviceId, setGpioRequest, String.class, status().is(400));
            Assert.fail(FAIL_MSG_IF_HTTP_CLIENT_ERROR_NOT_ENCOUNTERED);
        } catch (HttpClientErrorException e) {
            log.error(e.getMessage(), e);
            Assert.assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
            Assert.assertEquals("400 null", e.getMessage());
        }
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
        client.setCallback(new TestMqttCallback(client));

        String setGpioRequest = "{\"method\":\"setGpio\",\"params\":{\"pin\": \"23\",\"value\": 1}}";
        String deviceId = savedDevice.getId().getId().toString();

        String result = doPostAsync("/api/plugins/rpc/twoway/" + deviceId, setGpioRequest, String.class, status().isOk());
        Assert.assertEquals("{\"value1\":\"A\",\"value2\":\"B\"}", result);
    }

    @Test
    @Ignore // TODO: figure out the right error code for this case. Ignored due to failure: expected 408 but was: 200
    public void testServerMqttTwoWayRpcDeviceOffline() throws Exception {
        Device device = new Device();
        device.setName("Test Two-Way Server-Side RPC Device Offline");
        device.setType("default");
        Device savedDevice = getSavedDevice(device);
        DeviceCredentials deviceCredentials = getDeviceCredentials(savedDevice);
        assertEquals(savedDevice.getId(), deviceCredentials.getDeviceId());
        String accessToken = deviceCredentials.getCredentialsId();
        assertNotNull(accessToken);

        String setGpioRequest = "{\"method\":\"setGpio\",\"params\":{\"pin\": \"23\",\"value\": 1}}";
        String deviceId = savedDevice.getId().getId().toString();
        try {
            doPost("/api/plugins/rpc/twoway/" + deviceId, setGpioRequest, String.class, status().is(408));
            Assert.fail(FAIL_MSG_IF_HTTP_CLIENT_ERROR_NOT_ENCOUNTERED);
        } catch (HttpClientErrorException e) {
            log.error(e.getMessage(), e);
            Assert.assertEquals(HttpStatus.REQUEST_TIMEOUT, e.getStatusCode());
            Assert.assertEquals("408 null", e.getMessage());
        }
    }

    @Test
    @Ignore // TODO: figure out the right error code for this case. Ignored due to failure: expected 400 (404?) but was: 401
    public void testServerMqttTwoWayRpcDeviceDoesNotExist() throws Exception {
        String setGpioRequest = "{\"method\":\"setGpio\",\"params\":{\"pin\": \"23\",\"value\": 1}}";
        String nonExistentDeviceId = UUID.randomUUID().toString();
        try {
            doPostAsync("/api/plugins/rpc/oneway/" + nonExistentDeviceId, setGpioRequest, String.class, status().is(400));
            Assert.fail(FAIL_MSG_IF_HTTP_CLIENT_ERROR_NOT_ENCOUNTERED);
        } catch (HttpClientErrorException e) {
            log.error(e.getMessage(), e);
            Assert.assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
            Assert.assertEquals("400 null", e.getMessage());
        }
    }

    private Device getSavedDevice(Device device) throws Exception {
        return doPost("/api/device", device, Device.class);
    }

    private DeviceCredentials getDeviceCredentials(Device savedDevice) throws Exception {
        return doGet("/api/device/" + savedDevice.getId().getId().toString() + "/credentials", DeviceCredentials.class);
    }

    private static class TestMqttCallback implements MqttCallback {

        private final MqttAsyncClient client;

        TestMqttCallback(MqttAsyncClient client) {
            this.client = client;
        }

        @Override
        public void connectionLost(Throwable throwable) {
        }

        @Override
        public void messageArrived(String requestTopic, MqttMessage mqttMessage) throws Exception {
            log.info("Message Arrived: " + mqttMessage.getPayload().toString());
            MqttMessage message = new MqttMessage();
            String responseTopic = requestTopic.replace("request", "response");
            message.setPayload("{\"value1\":\"A\", \"value2\":\"B\"}".getBytes());
            client.publish(responseTopic, message);
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

        }
    }
}
