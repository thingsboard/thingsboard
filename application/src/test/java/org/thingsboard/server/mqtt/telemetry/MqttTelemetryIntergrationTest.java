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
package org.thingsboard.server.mqtt.telemetry;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.Before;
import org.junit.Test;
import org.springframework.web.util.UriComponentsBuilder;
import org.thingsboard.client.tools.RestClient;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.mqtt.AbstractFeatureIntegrationTest;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Valerii Sosliuk
 */
@Slf4j
public class MqttTelemetryIntergrationTest extends AbstractFeatureIntegrationTest {

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
        device.setName("Test device");
        savedDevice = restClient.getRestTemplate().postForEntity(BASE_URL + "/api/device", device, Device.class).getBody();
        DeviceCredentials deviceCredentials =
                restClient.getRestTemplate().getForEntity(BASE_URL + "/api/device/" + savedDevice.getId().getId().toString() + "/credentials", DeviceCredentials.class).getBody();
        assertEquals(savedDevice.getId(), deviceCredentials.getDeviceId());
        accessToken = deviceCredentials.getCredentialsId();
        assertNotNull(accessToken);
    }

    @Test
    public void testPushMqttRpcData() throws Exception {
        String clientId = MqttAsyncClient.generateClientId();
        MqttAsyncClient client = new MqttAsyncClient(MQTT_URL, clientId);

        MqttConnectOptions options = new MqttConnectOptions();
        options.setUserName(accessToken);
        client.connect(options);
        Thread.sleep(3000);
        MqttMessage message = new MqttMessage();
        message.setPayload("{\"key1\":\"value1\", \"key2\":true, \"key3\": 3.0, \"key4\": 4}".getBytes());
        client.publish("v1/devices/me/telemetry", message);

        String deviceId = savedDevice.getId().getId().toString();

        Thread.sleep(1000);
        List keys = restClient.getRestTemplate().getForEntity(BASE_URL + "/api/plugins/telemetry/" + deviceId +  "/keys/timeseries", List.class).getBody();
        assertEquals(Arrays.asList("key1", "key2", "key3", "key4"), keys);

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(BASE_URL + "/api/plugins/telemetry/" + deviceId +  "/values/timeseries")
                .queryParam("keys", String.join(",", keys));
        URI uri = builder.build().encode().toUri();
        Map<String, List<Map<String, String>>> values = restClient.getRestTemplate().getForEntity(uri, Map.class).getBody();

        assertEquals("value1", values.get("key1").get(0).get("value"));
        assertEquals("true", values.get("key2").get(0).get("value"));
        assertEquals("3.0", values.get("key3").get(0).get("value"));
        assertEquals("4", values.get("key4").get(0).get("value"));
    }
}
