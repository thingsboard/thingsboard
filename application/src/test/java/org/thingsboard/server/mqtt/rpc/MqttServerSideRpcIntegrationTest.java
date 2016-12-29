/**
 * Copyright © 2016 The Thingsboard Authors
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
import org.eclipse.paho.client.mqttv3.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.thingsboard.client.tools.RestClient;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.mqtt.AbstractFeatureIntegrationTest;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Valerii Sosliuk
 */
@Slf4j
public class MqttServerSideRpcIntegrationTest extends AbstractFeatureIntegrationTest {

    private static final String MQTT_URL = "tcp://localhost:1883";
    private static final String BASE_URL = "http://localhost:8080";

    private static final String USERNAME = "tenant@thingsboard.org";
    private static final String PASSWORD = "tenant";

    private RestClient restClient;

    @Before
    public void beforeTest() throws Exception {
        restClient = new RestClient(BASE_URL);
        restClient.login(USERNAME, PASSWORD);
    }

    @Test
    public void testServerMqttOneWayRpc() throws Exception {
        Device device = new Device();
        device.setName("Test One-Way Server-Side RPC");
        Device savedDevice = getSavedDevice(device);
        DeviceCredentials deviceCredentials = getDeviceCredentials(savedDevice);
        assertEquals(savedDevice.getId(), deviceCredentials.getDeviceId());
        String accessToken = deviceCredentials.getCredentialsId();
        assertNotNull(accessToken);

        String clientId = MqttAsyncClient.generateClientId();
        MqttAsyncClient client = new MqttAsyncClient(MQTT_URL, clientId);

        MqttConnectOptions options = new MqttConnectOptions();
        options.setUserName(accessToken);
        client.connect(options);
        Thread.sleep(3000);
        client.subscribe("v1/devices/me/rpc/request/+", 1);
        client.setCallback(new TestMqttCallback(client));

        String setGpioRequest = "{\"method\":\"setGpio\",\"params\":{\"pin\": \"23\",\"value\": 1}}";
        String deviceId = savedDevice.getId().getId().toString();
        ResponseEntity result = restClient.getRestTemplate().postForEntity(BASE_URL + "api/plugins/rpc/oneway/" + deviceId, setGpioRequest, String.class);
        Assert.assertEquals(HttpStatus.OK, result.getStatusCode());
        Assert.assertNull(result.getBody());
    }

    @Test
    public void testServerMqttOneWayRpcDeviceOffline() throws Exception {
        Device device = new Device();
        device.setName("Test One-Way Server-Side RPC Device Offline");
        Device savedDevice = getSavedDevice(device);
        DeviceCredentials deviceCredentials = getDeviceCredentials(savedDevice);
        assertEquals(savedDevice.getId(), deviceCredentials.getDeviceId());
        String accessToken = deviceCredentials.getCredentialsId();
        assertNotNull(accessToken);

        String setGpioRequest = "{\"method\":\"setGpio\",\"params\":{\"pin\": \"23\",\"value\": 1}}";
        String deviceId = savedDevice.getId().getId().toString();
        try {
            restClient.getRestTemplate().postForEntity(BASE_URL + "api/plugins/rpc/oneway/" + deviceId, setGpioRequest, String.class);
            Assert.fail("HttpClientErrorException expected, but not encountered");
        } catch (HttpClientErrorException e) {
            log.error(e.getMessage(), e);
            Assert.assertEquals(HttpStatus.REQUEST_TIMEOUT, e.getStatusCode());
            Assert.assertEquals("408 null", e.getMessage());
        }
    }

    @Test
    public void testServerMqttOneWayRpcDeviceDoesNotExist() throws Exception {
        String setGpioRequest = "{\"method\":\"setGpio\",\"params\":{\"pin\": \"23\",\"value\": 1}}";
        String nonExistentDeviceId = UUID.randomUUID().toString();
        try {
            restClient.getRestTemplate().postForEntity(BASE_URL + "api/plugins/rpc/oneway/" + nonExistentDeviceId, setGpioRequest, String.class);
            Assert.fail("HttpClientErrorException expected, but not encountered");
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
        Device savedDevice = getSavedDevice(device);
        DeviceCredentials deviceCredentials = getDeviceCredentials(savedDevice);
        assertEquals(savedDevice.getId(), deviceCredentials.getDeviceId());
        String accessToken = deviceCredentials.getCredentialsId();
        assertNotNull(accessToken);

        String clientId = MqttAsyncClient.generateClientId();
        MqttAsyncClient client = new MqttAsyncClient(MQTT_URL, clientId);

        MqttConnectOptions options = new MqttConnectOptions();
        options.setUserName(accessToken);
        client.connect(options);
        Thread.sleep(3000);
        client.subscribe("v1/devices/me/rpc/request/+", 1);
        client.setCallback(new TestMqttCallback(client));

        String setGpioRequest = "{\"method\":\"setGpio\",\"params\":{\"pin\": \"23\",\"value\": 1}}";
        String deviceId = savedDevice.getId().getId().toString();
        String result = getStringResult(setGpioRequest, "twoway", deviceId);
        Assert.assertEquals("{\"value1\":\"A\",\"value2\":\"B\"}", result);
    }

    @Test
    public void testServerMqttTwoWayRpcDeviceOffline() throws Exception {
        Device device = new Device();
        device.setName("Test Two-Way Server-Side RPC Device Offline");
        Device savedDevice = getSavedDevice(device);
        DeviceCredentials deviceCredentials = getDeviceCredentials(savedDevice);
        assertEquals(savedDevice.getId(), deviceCredentials.getDeviceId());
        String accessToken = deviceCredentials.getCredentialsId();
        assertNotNull(accessToken);

        String setGpioRequest = "{\"method\":\"setGpio\",\"params\":{\"pin\": \"23\",\"value\": 1}}";
        String deviceId = savedDevice.getId().getId().toString();
        try {
            restClient.getRestTemplate().postForEntity(BASE_URL + "api/plugins/rpc/twoway/" + deviceId, setGpioRequest, String.class);
            Assert.fail("HttpClientErrorException expected, but not encountered");
        } catch (HttpClientErrorException e) {
            log.error(e.getMessage(), e);
            Assert.assertEquals(HttpStatus.REQUEST_TIMEOUT, e.getStatusCode());
            Assert.assertEquals("408 null", e.getMessage());
        }
    }

    @Test
    public void testServerMqttTwoWayRpcDeviceDoesNotExist() throws Exception {
        String setGpioRequest = "{\"method\":\"setGpio\",\"params\":{\"pin\": \"23\",\"value\": 1}}";
        String nonExistentDeviceId = UUID.randomUUID().toString();
        try {
            restClient.getRestTemplate().postForEntity(BASE_URL + "api/plugins/rpc/oneway/" + nonExistentDeviceId, setGpioRequest, String.class);
            Assert.fail("HttpClientErrorException expected, but not encountered");
        } catch (HttpClientErrorException e) {
            log.error(e.getMessage(), e);
            Assert.assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
            Assert.assertEquals("400 null", e.getMessage());
        }
    }

    private Device getSavedDevice(Device device) {
        return restClient.getRestTemplate().postForEntity(BASE_URL + "/api/device", device, Device.class).getBody();
    }

    private DeviceCredentials getDeviceCredentials(Device savedDevice) {
        return restClient.getRestTemplate().getForEntity(BASE_URL + "/api/device/" + savedDevice.getId().getId().toString() + "/credentials", DeviceCredentials.class).getBody();
    }

    private String getStringResult(String requestData, String callType, String deviceId) {
        return restClient.getRestTemplate().postForEntity(BASE_URL + "api/plugins/rpc/" + callType + "/" + deviceId, requestData, String.class).getBody();
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
