/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.server.transport.mqtt.credentials.sql;

import com.fasterxml.jackson.core.type.TypeReference;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.apache.commons.lang3.RandomStringUtils;
import org.eclipse.paho.mqttv5.client.MqttAsyncClient;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttSecurityException;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.device.credentials.BasicMqttCredentials;
import org.thingsboard.server.common.data.device.profile.MqttTopics;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.transport.mqtt.AbstractMqttIntegrationTest;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
        loginSysAdmin();

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

        tenantAdmin = createUserAndLogin(tenantAdmin, "testPassword1");

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
        testTelemetryIsDelivered(accessTokenDevice, getMqttAsyncClient(null, USER_NAME1, null));
        testTelemetryIsDelivered(clientIdDevice, getMqttAsyncClient(CLIENT_ID, null, null));
        testTelemetryIsDelivered(clientIdAndUserNameDevice1, getMqttAsyncClient(CLIENT_ID, USER_NAME1, null));
        testTelemetryIsDelivered(clientIdAndUserNameAndPasswordDevice2, getMqttAsyncClient(CLIENT_ID, USER_NAME2, PASSWORD));

        // Also correct. Random clientId and password, but matches access token
        testTelemetryIsDelivered(accessToken2Device, getMqttAsyncClient(RandomStringUtils.randomAlphanumeric(10), USER_NAME2, RandomStringUtils.randomAlphanumeric(10)));
    }

    // Should be MqttSecurityException.class https://github.com/eclipse/paho.mqtt.java/issues/880
    @Test(expected = MqttException.class)
    public void testCorrectClientIdAndUserNameButWrongPassword() throws Exception {
        // Not correct. Correct clientId and username, but wrong password
        testTelemetryIsNotDelivered(clientIdAndUserNameAndPasswordDevice3, getMqttAsyncClient(CLIENT_ID, USER_NAME3, "WRONG PASSWORD"));
    }

    private void testTelemetryIsDelivered(Device device, MqttAsyncClient client) throws Exception {
        testTelemetryIsDelivered(device, client, true);
    }

    private void testTelemetryIsNotDelivered(Device device, MqttAsyncClient client) throws Exception {
        testTelemetryIsDelivered(device, client, false);
    }

    private void testTelemetryIsDelivered(Device device, MqttAsyncClient client, boolean ok) throws Exception {
        String randomKey = RandomStringUtils.randomAlphanumeric(10);
        List<String> expectedKeys = Arrays.asList(randomKey);
        publishMqttMsg(client, JacksonUtil.toString(JacksonUtil.newObjectNode().put(randomKey, true)).getBytes(), MqttTopics.DEVICE_TELEMETRY_TOPIC);

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
        client.disconnect().waitForCompletion();
    }

    @After
    public void after() throws Exception {
        processAfterTest();
    }

    protected MqttAsyncClient getMqttAsyncClient(String clientId, String username, String password) throws MqttException {
        if (StringUtils.isEmpty(clientId)) {
            clientId = UUID.randomUUID().toString();
        }
        MqttAsyncClient client = new MqttAsyncClient(MQTT_URL, clientId, new MemoryPersistence());

        MqttConnectionOptions options = new MqttConnectionOptions();
        if (StringUtils.isNotEmpty(username)) {
            options.setUserName(username);
        }
        if (StringUtils.isNotEmpty(password)) {
            options.setPassword(password.getBytes(StandardCharsets.UTF_8));
        }
        client.connect(options).waitForCompletion();
        return client;
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

    private Device createDevice(String deviceName, String accessToken) throws Exception {
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
