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
import org.junit.Test;
import org.thingsboard.client.tools.RestClient;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.mqtt.AbstractFeatureIntegrationTest;

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

    private Device savedDevice;

    private String accessToken;
    private RestClient restClient;

    @Before
    public void beforeTest() throws Exception {
        restClient = new RestClient(BASE_URL);
        restClient.login(USERNAME, PASSWORD);

        Device device = new Device();
        device.setName("Test Server-Side RPC Device");
        savedDevice = restClient.getRestTemplate().postForEntity(BASE_URL + "/api/device", device, Device.class).getBody();
        DeviceCredentials deviceCredentials =
                restClient.getRestTemplate().getForEntity(BASE_URL + "/api/device/" + savedDevice.getId().getId().toString() + "/credentials", DeviceCredentials.class).getBody();
        assertEquals(savedDevice.getId(), deviceCredentials.getDeviceId());
        accessToken = deviceCredentials.getCredentialsId();
        assertNotNull(accessToken);
    }

    @Test
    public void testServerMqttTwoWayRpc() throws Exception {
        String clientId = MqttAsyncClient.generateClientId();
        MqttAsyncClient client = new MqttAsyncClient(MQTT_URL, clientId);

        MqttConnectOptions options = new MqttConnectOptions();
        options.setUserName(accessToken);
        client.connect(options);
        Thread.sleep(3000);
        client.subscribe("v1/devices/me/rpc/request/+",1);
        client.setCallback(new TestMqttCallback(client));

        String setGpioRequest = "{\"method\":\"setGpio\",\"params\":{\"pin\": \"23\",\"value\": 1}}";
        String deviceId = savedDevice.getId().getId().toString();
        String result = restClient.getRestTemplate().postForEntity(BASE_URL + "api/plugins/rpc/twoway/" + deviceId, setGpioRequest, String.class).getBody();
        log.info("Result: " + result);
        Assert.assertEquals("{\"value1\":\"A\",\"value2\":\"B\"}", result);
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
