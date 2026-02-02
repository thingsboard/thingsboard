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
package org.thingsboard.server.controller;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.cf.configuration.Argument;
import org.thingsboard.server.common.data.cf.configuration.ArgumentType;
import org.thingsboard.server.common.data.cf.configuration.CalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.Output;
import org.thingsboard.server.common.data.cf.configuration.OutputType;
import org.thingsboard.server.common.data.cf.configuration.ReferencedEntityKey;
import org.thingsboard.server.common.data.cf.configuration.SimpleCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
public class CalculatedFieldControllerTest extends AbstractControllerTest {

    private Tenant savedTenant;

    @Before
    public void beforeTest() throws Exception {
        loginSysAdmin();

        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        savedTenant = saveTenant(tenant);
        assertThat(savedTenant).isNotNull();

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
        loginSysAdmin();

        deleteTenant(savedTenant.getId());
    }

    @Test
    public void testSaveCalculatedField() throws Exception {
        Device testDevice = createDevice("Test device", "1234567890");
        CalculatedField calculatedField = getCalculatedField(testDevice.getId());

        CalculatedField savedCalculatedField = doPost("/api/calculatedField", calculatedField, CalculatedField.class);

        assertThat(savedCalculatedField).isNotNull();
        assertThat(savedCalculatedField.getId()).isNotNull();
        assertThat(savedCalculatedField.getCreatedTime()).isGreaterThan(0);
        assertThat(savedCalculatedField.getTenantId()).isEqualTo(savedTenant.getId());
        assertThat(savedCalculatedField.getEntityId()).isEqualTo(calculatedField.getEntityId());
        assertThat(savedCalculatedField.getType()).isEqualTo(calculatedField.getType());
        assertThat(savedCalculatedField.getName()).isEqualTo(calculatedField.getName());
        assertThat(savedCalculatedField.getConfiguration()).isEqualTo(getCalculatedFieldConfig());
        assertThat(savedCalculatedField.getVersion()).isEqualTo(1L);

        savedCalculatedField.setName("Test CF");

        CalculatedField updatedCalculatedField = doPost("/api/calculatedField", savedCalculatedField, CalculatedField.class);

        assertThat(updatedCalculatedField.getName()).isEqualTo(savedCalculatedField.getName());
        assertThat(updatedCalculatedField.getVersion()).isEqualTo(savedCalculatedField.getVersion() + 1);

        doDelete("/api/calculatedField/" + savedCalculatedField.getId().getId().toString())
                .andExpect(status().isOk());
    }

    @Test
    public void testGetCalculatedFieldById() throws Exception {
        Device testDevice = createDevice("Test device", "1234567890");
        CalculatedField calculatedField = getCalculatedField(testDevice.getId());

        CalculatedField savedCalculatedField = doPost("/api/calculatedField", calculatedField, CalculatedField.class);
        CalculatedField fetchedCalculatedField = doGet("/api/calculatedField/" + savedCalculatedField.getId().getId(), CalculatedField.class);

        assertThat(fetchedCalculatedField).isNotNull();
        assertThat(fetchedCalculatedField).isEqualTo(savedCalculatedField);

        doDelete("/api/calculatedField/" + savedCalculatedField.getId().getId().toString())
                .andExpect(status().isOk());
    }

    @Test
    public void testDeleteCalculatedField() throws Exception {
        Device testDevice = createDevice("Test device", "1234567890");
        CalculatedField calculatedField = getCalculatedField(testDevice.getId());

        CalculatedField savedCalculatedField = doPost("/api/calculatedField", calculatedField, CalculatedField.class);

        assertThat(savedCalculatedField).isNotNull();

        doDelete("/api/calculatedField/" + savedCalculatedField.getId().getId().toString())
                .andExpect(status().isOk());
        doGet("/api/calculatedField/" + savedCalculatedField.getId().getId()).andExpect(status().isNotFound());
    }

    private CalculatedField getCalculatedField(DeviceId deviceId) {
        CalculatedField calculatedField = new CalculatedField();
        calculatedField.setEntityId(deviceId);
        calculatedField.setType(CalculatedFieldType.SIMPLE);
        calculatedField.setName("Test Calculated Field");
        calculatedField.setConfigurationVersion(1);
        calculatedField.setConfiguration(getCalculatedFieldConfig());
        calculatedField.setVersion(1L);
        return calculatedField;
    }

    private CalculatedFieldConfiguration getCalculatedFieldConfig() {
        SimpleCalculatedFieldConfiguration config = new SimpleCalculatedFieldConfiguration();

        Argument argument = new Argument();
        argument.setRefEntityId(null);
        ReferencedEntityKey refEntityKey = new ReferencedEntityKey("temperature", ArgumentType.TS_LATEST, null);
        argument.setRefEntityKey(refEntityKey);

        config.setArguments(Map.of("T", argument));

        config.setExpression("T - (100 - H) / 5");

        Output output = new Output();
        output.setName("output");
        output.setType(OutputType.TIME_SERIES);

        config.setOutput(output);

        return config;
    }

}
