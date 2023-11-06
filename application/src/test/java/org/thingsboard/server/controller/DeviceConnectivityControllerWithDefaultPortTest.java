/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
package org.thingsboard.server.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.AdditionalAnswers;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ContextConfiguration;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.dao.device.DeviceDao;
import org.thingsboard.server.dao.service.DaoSqlTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.dao.util.DeviceConnectivityUtil.HTTP;
import static org.thingsboard.server.dao.util.DeviceConnectivityUtil.HTTPS;

@ContextConfiguration(classes = {DeviceConnectivityControllerWithDefaultPortTest.Config.class})
@DaoSqlTest
public class DeviceConnectivityControllerWithDefaultPortTest extends AbstractControllerTest {

    ListeningExecutorService executor;

    private Tenant savedTenant;

    static class Config {
        @Bean
        @Primary
        public DeviceDao deviceDao(DeviceDao deviceDao) {
            return Mockito.mock(DeviceDao.class, AdditionalAnswers.delegatesTo(deviceDao));
        }
    }

    @Before
    public void beforeTest() throws Exception {
        executor = MoreExecutors.listeningDecorator(ThingsBoardExecutors.newWorkStealingPool(8, getClass()));

        loginSysAdmin();

        ObjectNode config = JacksonUtil.newObjectNode();

        ObjectNode http = JacksonUtil.newObjectNode();
        http.put("enabled", true);
        http.put("host", "");
        http.put("port", 80);
        config.set("http", http);

        ObjectNode https = JacksonUtil.newObjectNode();
        https.put("enabled", true);
        https.put("host", "");
        https.put("port", 443);
        config.set("https", https);

        ObjectNode mqtt = JacksonUtil.newObjectNode();
        mqtt.put("enabled", false);
        mqtt.put("host", "");
        mqtt.put("port", 1883);
        config.set("mqtt", mqtt);

        ObjectNode mqtts = JacksonUtil.newObjectNode();
        mqtts.put("enabled", false);
        mqtts.put("host", "");
        mqtts.put("port", 8883);
        config.set("mqtts", mqtts);

        ObjectNode coap = JacksonUtil.newObjectNode();
        coap.put("enabled", false);
        coap.put("host", "");
        coap.put("port", 5683);
        config.set("coap", coap);

        ObjectNode coaps = JacksonUtil.newObjectNode();
        coaps.put("enabled", false);
        coaps.put("host", "");
        coaps.put("port", 5684);
        config.set("coaps", coaps);

        AdminSettings adminSettings = doGet("/api/admin/settings/connectivity", AdminSettings.class);
        adminSettings.setJsonValue(config);
        doPost("/api/admin/settings", adminSettings).andExpect(status().isOk());

        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        savedTenant = doPost("/api/tenant", tenant, Tenant.class);
        Assert.assertNotNull(savedTenant);

        User tenantAdmin = new User();
        tenantAdmin.setAuthority(Authority.TENANT_ADMIN);
        tenantAdmin.setTenantId(savedTenant.getId());
        tenantAdmin.setEmail("tenant2@thingsboard.org");
        tenantAdmin.setFirstName("Joe");
        tenantAdmin.setLastName("Downs");

        createUserAndLogin(tenantAdmin, "testPassword1");
    }

    @After
    public void afterTest() throws Exception {
        executor.shutdownNow();

        loginSysAdmin();

        doDelete("/api/tenant/" + savedTenant.getId().getId())
                .andExpect(status().isOk());
    }

    @Test
    public void testFetchPublishTelemetryCommandsForDefaultDevice() throws Exception {
        Device device = new Device();
        device.setName("My device");
        device.setType("default");
        Device savedDevice = doPost("/api/device", device, Device.class);
        JsonNode commands =
                doGetTyped("/api/device-connectivity/" + savedDevice.getId().getId(), new TypeReference<>() {
                });

        DeviceCredentials credentials =
                doGet("/api/device/" + savedDevice.getId().getId() + "/credentials", DeviceCredentials.class);

        assertThat(commands).hasSize(1);
        JsonNode httpCommands = commands.get(HTTP);
        assertThat(httpCommands.get(HTTP).asText()).isEqualTo(String.format("curl -v -X POST http://localhost/api/v1/%s/telemetry " +
                        "--header Content-Type:application/json --data \"{temperature:25}\"",
                credentials.getCredentialsId()));
        assertThat(httpCommands.get(HTTPS).asText()).isEqualTo(String.format("curl -v -X POST https://localhost/api/v1/%s/telemetry " +
                        "--header Content-Type:application/json --data \"{temperature:25}\"",
                credentials.getCredentialsId()));
    }
}
