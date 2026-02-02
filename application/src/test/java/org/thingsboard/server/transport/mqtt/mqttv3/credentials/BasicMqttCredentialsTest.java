/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.transport.mqtt.mqttv3.credentials;

import com.fasterxml.jackson.core.type.TypeReference;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.device.credentials.BasicMqttCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.transport.mqtt.AbstractMqttIntegrationTest;
import org.thingsboard.server.transport.mqtt.mqttv3.MqttTestClient;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.common.data.device.profile.MqttTopics.DEVICE_TELEMETRY_TOPIC;

@DaoSqlTest
public class BasicMqttCredentialsTest extends AbstractMqttIntegrationTest {

    public static final String CLIENT_ID = "ClientId";
    public static final String USER_NAME1 = "UserName1";
    public static final String USER_NAME2 = "UserName2";
    public static final String USER_NAME3 = "UserName3";
    public static final String PASSWORD = "secret";

    private Device clientIdDevice;
    private Device clientIdAndUserNameDevice1;
    private Device clientIdAndUserNameAndPasswordDevice2;
    private Device clientIdAndUserNameAndPasswordDevice3;
    private Device accessTokenDevice;
    private Device accessToken2Device;


    @Before
    public void before() throws Exception {
        loginTenantAdmin();

        BasicMqttCredentials credValue = new BasicMqttCredentials();
        credValue.setClientId(CLIENT_ID);
        clientIdDevice = createDevice("clientIdDevice", credValue);

        credValue = new BasicMqttCredentials();
        credValue.setClientId(CLIENT_ID);
        credValue.setUserName(USER_NAME1);
        clientIdAndUserNameDevice1 = createDevice("clientIdAndUserNameDevice", credValue);

        credValue = new BasicMqttCredentials();
        credValue.setClientId(CLIENT_ID);
        credValue.setUserName(USER_NAME2);
        credValue.setPassword(PASSWORD);
        clientIdAndUserNameAndPasswordDevice2 = createDevice("clientIdAndUserNameAndPasswordDevice", credValue);

        credValue = new BasicMqttCredentials();
        credValue.setClientId(CLIENT_ID);
        credValue.setUserName(USER_NAME3);
        credValue.setPassword(PASSWORD);
        clientIdAndUserNameAndPasswordDevice3 = createDevice("clientIdAndUserNameAndPasswordDevice2", credValue);

        accessTokenDevice = createDevice("accessTokenDevice", USER_NAME1);
        accessToken2Device = createDevice("accessToken2Device", USER_NAME2);
    }

    @Test
    public void testCorrectCredentials() throws Exception {
        // Check that correct devices receive telemetry
        MqttTestClient mqttTestClient1 = new MqttTestClient();
        mqttTestClient1.connectAndWait(USER_NAME1);

        MqttTestClient mqttTestClient2 = new MqttTestClient(CLIENT_ID);
        mqttTestClient2.connectAndWait();

        MqttTestClient mqttTestClient3 = new MqttTestClient(CLIENT_ID);
        mqttTestClient3.connectAndWait(USER_NAME1);

        MqttTestClient mqttTestClient4 = new MqttTestClient(CLIENT_ID);
        mqttTestClient4.connectAndWait(USER_NAME2, PASSWORD);

        // Also correct. Random clientId and password, but matches access token
        MqttTestClient mqttTestClient5 = new MqttTestClient(StringUtils.randomAlphanumeric(10));
        mqttTestClient5.connectAndWait(USER_NAME2, StringUtils.randomAlphanumeric(10));

        testTelemetryIsDelivered(accessTokenDevice, mqttTestClient1);
        testTelemetryIsDelivered(clientIdDevice, mqttTestClient2);
        testTelemetryIsDelivered(clientIdAndUserNameDevice1, mqttTestClient3);
        testTelemetryIsDelivered(clientIdAndUserNameAndPasswordDevice2, mqttTestClient4);

        // Also correct. Random clientId and password, but matches access token
        testTelemetryIsDelivered(accessToken2Device, mqttTestClient5);
    }

    @Test
    public void testCorrectClientIdAndUserNameButWrongPassword() throws Exception {
        // Not correct. Correct clientId and username, but wrong password
        MqttTestClient mqttTestClient = new MqttTestClient(CLIENT_ID);
        try {
            mqttTestClient.connectAndWait(USER_NAME3, "WRONG PASSWORD");
            Assert.fail(); // This should not happens, because we have a wrong password
        } catch (MqttException e) {
            Assert.assertEquals(5, e.getReasonCode()); // 4 - Reason code not authorized in MQTT v3
        }
        Assertions.assertThrows(MqttException.class, () -> {
            testTelemetryIsNotDelivered(clientIdAndUserNameAndPasswordDevice3, mqttTestClient);
        });
    }

    private void testTelemetryIsDelivered(Device device, MqttTestClient client) throws Exception {
        testTelemetryIsDelivered(device, client, true);
    }

    private void testTelemetryIsNotDelivered(Device device, MqttTestClient client) throws Exception {
        testTelemetryIsDelivered(device, client, false);
    }

    private void testTelemetryIsDelivered(Device device, MqttTestClient client, boolean ok) throws Exception {
        String randomKey = StringUtils.randomAlphanumeric(10);
        List<String> expectedKeys = Arrays.asList(randomKey);
        client.publishAndWait(DEVICE_TELEMETRY_TOPIC, JacksonUtil.toString(JacksonUtil.newObjectNode().put(randomKey, true)).getBytes());

        String deviceId = device.getId().getId().toString();

        long start = System.currentTimeMillis();
        long end = System.currentTimeMillis() + 5000;

        List<String> actualKeys = null;
        while (start <= end) {
            actualKeys = doGetAsyncTyped("/api/plugins/telemetry/DEVICE/" + deviceId + "/keys/timeseries", new TypeReference<>() {
            });
            if (actualKeys.size() == expectedKeys.size()) {
                break;
            }
            Thread.sleep(100);
            start += 100;
        }
        if (ok) {
            assertNotNull(actualKeys);

            Set<String> actualKeySet = new HashSet<>(actualKeys);
            Set<String> expectedKeySet = new HashSet<>(expectedKeys);

            assertEquals(expectedKeySet, actualKeySet);
        } else {
            assertNull(actualKeys);
        }
        client.disconnect();
    }

    private Device createDevice(String deviceName, BasicMqttCredentials clientIdCredValue) throws Exception {
        Device device = new Device();
        device.setName(deviceName);
        device.setType("default");

        device = doPost("/api/device", device, Device.class);

        DeviceCredentials clientIdCred =
                doGet("/api/device/" + device.getId().getId().toString() + "/credentials", DeviceCredentials.class);

        clientIdCred.setCredentialsType(DeviceCredentialsType.MQTT_BASIC);


        clientIdCred.setCredentialsValue(JacksonUtil.toString(clientIdCredValue));
        doPost("/api/device/credentials", clientIdCred).andExpect(status().isOk());
        return device;
    }

    protected Device createDevice(String deviceName, String accessToken) throws Exception {
        Device device = new Device();
        device.setName(deviceName);
        device.setType("default");

        device = doPost("/api/device", device, Device.class);

        DeviceCredentials clientIdCred =
                doGet("/api/device/" + device.getId().getId().toString() + "/credentials", DeviceCredentials.class);

        clientIdCred.setCredentialsType(DeviceCredentialsType.ACCESS_TOKEN);
        clientIdCred.setCredentialsId(accessToken);
        doPost("/api/device/credentials", clientIdCred).andExpect(status().isOk());
        return device;
    }
}
