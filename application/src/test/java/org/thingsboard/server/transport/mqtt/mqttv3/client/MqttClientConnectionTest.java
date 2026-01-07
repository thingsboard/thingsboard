/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.server.transport.mqtt.mqttv3.client;

import org.awaitility.Awaitility;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.transport.limits.TransportRateLimitService;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.transport.mqtt.MqttTestConfigProperties;
import org.thingsboard.server.transport.mqtt.mqttv3.MqttTestClient;

import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@DaoSqlTest
public class MqttClientConnectionTest extends AbstractMqttClientConnectionTest {

    @MockitoSpyBean
    TransportRateLimitService rateLimitService;

    @Before
    public void beforeTest() throws Exception {
        MqttTestConfigProperties configProperties = MqttTestConfigProperties.builder()
                .deviceName("Test MqttV5 client device")
                .build();
        processBeforeTest(configProperties);
    }

    @Test
    public void testClientWithCorrectAccessToken() throws Exception {
        processClientWithCorrectAccessTokenTest();
    }

    @Test
    public void testClientWithWrongAccessToken() throws Exception {
        processClientWithWrongAccessTokenTest();
    }

    @Test
    public void testClientWithWrongClientIdAndEmptyUsernamePassword() throws Exception {
        processClientWithWrongClientIdAndEmptyUsernamePasswordTest();
    }

    @Test
    public void testClientShouldBeDisconnectedAfterTenantDeletion() throws Exception {
        loginSysAdmin();
        Tenant tenant = new Tenant();
        tenant.setTitle("Mqtt Client Connection Test Tenant");
        Tenant savedTenant = saveTenant(tenant);

        User tenantAdmin = new User();
        tenantAdmin.setAuthority(Authority.TENANT_ADMIN);
        tenantAdmin.setTenantId(savedTenant.getTenantId());
        tenantAdmin.setEmail("mqttTestClient@thingsboard.org");
        createUserAndLogin(tenantAdmin, TENANT_ADMIN_PASSWORD);

        savedDevice = createDevice(RandomStringUtils.randomAlphabetic(10), "default", false);
        DeviceCredentials deviceCredentials =
                doGet("/api/device/" + savedDevice.getId().getId().toString() + "/credentials", DeviceCredentials.class);
        accessToken = deviceCredentials.getCredentialsId();

        MqttTestClient client = new MqttTestClient();
        client.connectAndWait(accessToken);
        Assert.assertTrue(client.isConnected());

        loginSysAdmin();

        Mockito.clearInvocations(rateLimitService);
        deleteTenant(savedTenant.getId());

        Awaitility.await()
                .atMost(10, TimeUnit.SECONDS)
                .until(() -> !client.isConnected());

        verify(rateLimitService, never()).checkLimits(Mockito.any(), Mockito.any(), Mockito.any(), anyInt(), anyBoolean());
    }
}
