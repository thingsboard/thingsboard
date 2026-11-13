// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.controller;

import com.fasterxml.jackson.core.type.TypeReference;
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
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.LinkedHashMap;
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

    @Test
    public void testSaveCalculatedFieldWithOtherTenantReference() throws Exception {
        Device testDevice = createDevice("Test device", "1234567890");
        CalculatedField calculatedField = getCalculatedField(testDevice.getId(), Map.of("T", getArgument(tenantId)));

        doPost("/api/calculatedField", calculatedField).andExpect(status().isForbidden());

        PageData<CalculatedField> calculatedFields = doGetTyped("/api/DEVICE/" + testDevice.getId().getId() + "/calculatedFields?pageSize=10&page=0",
                new TypeReference<>() {});
        assertThat(calculatedFields.getData()).isEmpty();
    }

    @Test
    public void testSaveCalculatedFieldWithOwnTenantReference() throws Exception {
        Device testDevice = createDevice("Test device", "1234567890");
        CalculatedField calculatedField = getCalculatedField(testDevice.getId(), Map.of("T", getArgument(savedTenant.getId())));

        CalculatedField savedCalculatedField = doPost("/api/calculatedField", calculatedField, CalculatedField.class);

        assertThat(savedCalculatedField.getId()).isNotNull();
        assertThat(savedCalculatedField.getConfiguration().getReferencedEntities()).containsExactly(savedTenant.getId());
    }

    @Test
    public void testSaveCalculatedFieldChecksEveryReference() throws Exception {
        loginTenantAdmin();
        Device otherTenantDevice = createDevice("Other device", "0987654321");
        login("tenant2@thingsboard.org", "testPassword1");

        Device testDevice = createDevice("Test device", "1234567890");
        Map<String, Argument> arguments = new LinkedHashMap<>();
        arguments.put("T", getArgument(savedTenant.getId()));
        arguments.put("H", getArgument(otherTenantDevice.getId()));
        CalculatedField calculatedField = getCalculatedField(testDevice.getId(), arguments);

        doPost("/api/calculatedField", calculatedField).andExpect(status().isNotFound());
    }

    private CalculatedField getCalculatedField(DeviceId deviceId) {
        return getCalculatedField(deviceId, Map.of("T", getArgument(null)));
    }

    private CalculatedField getCalculatedField(DeviceId deviceId, Map<String, Argument> arguments) {
        CalculatedField calculatedField = new CalculatedField();
        calculatedField.setEntityId(deviceId);
        calculatedField.setType(CalculatedFieldType.SIMPLE);
        calculatedField.setName("Test Calculated Field");
        calculatedField.setConfigurationVersion(1);
        calculatedField.setConfiguration(getCalculatedFieldConfig(arguments));
        calculatedField.setVersion(1L);
        return calculatedField;
    }

    private CalculatedFieldConfiguration getCalculatedFieldConfig() {
        return getCalculatedFieldConfig(Map.of("T", getArgument(null)));
    }

    private CalculatedFieldConfiguration getCalculatedFieldConfig(Map<String, Argument> arguments) {
        SimpleCalculatedFieldConfiguration config = new SimpleCalculatedFieldConfiguration();

        config.setArguments(arguments);

        config.setExpression("T - (100 - H) / 5");

        Output output = new Output();
        output.setName("output");
        output.setType(OutputType.TIME_SERIES);

        config.setOutput(output);

        return config;
    }

    private Argument getArgument(EntityId refEntityId) {
        Argument argument = new Argument();
        argument.setRefEntityId(refEntityId);
        argument.setRefEntityKey(new ReferencedEntityKey("temperature", ArgumentType.TS_LATEST, null));
        return argument;
    }

}
