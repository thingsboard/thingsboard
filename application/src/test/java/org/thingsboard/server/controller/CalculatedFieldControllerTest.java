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
import org.thingsboard.server.common.data.cf.configuration.PropagationCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.ReferencedEntityKey;
import org.thingsboard.server.common.data.cf.configuration.RelationPathQueryDynamicSourceConfiguration;
import org.thingsboard.server.common.data.cf.configuration.SimpleCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.TimeSeriesOutput;
import org.thingsboard.server.common.data.cf.configuration.aggregation.AggKeyInput;
import org.thingsboard.server.common.data.cf.configuration.aggregation.AggMetric;
import org.thingsboard.server.common.data.cf.configuration.aggregation.single.EntityAggregationCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.aggregation.single.interval.HourInterval;
import org.thingsboard.server.common.data.cf.configuration.aggregation.single.interval.Watermark;
import org.thingsboard.server.common.data.cf.configuration.geofencing.EntityCoordinates;
import org.thingsboard.server.common.data.cf.configuration.geofencing.GeofencingCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.geofencing.ZoneGroupConfiguration;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationPathLevel;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.common.data.cf.configuration.geofencing.GeofencingReportStrategy.REPORT_TRANSITION_EVENTS_AND_PRESENCE_STATUS;

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
        CalculatedField calculatedField = getSimpleCalculatedField(testDevice.getId());

        CalculatedField savedCalculatedField = doPost("/api/calculatedField", calculatedField, CalculatedField.class);

        assertThat(savedCalculatedField).isNotNull();
        assertThat(savedCalculatedField.getId()).isNotNull();
        assertThat(savedCalculatedField.getCreatedTime()).isGreaterThan(0);
        assertThat(savedCalculatedField.getTenantId()).isEqualTo(savedTenant.getId());
        assertThat(savedCalculatedField.getEntityId()).isEqualTo(calculatedField.getEntityId());
        assertThat(savedCalculatedField.getType()).isEqualTo(calculatedField.getType());
        assertThat(savedCalculatedField.getName()).isEqualTo(calculatedField.getName());
        assertThat(savedCalculatedField.getConfiguration()).isEqualTo(getSimpleCalculatedFieldConfig());
        assertThat(savedCalculatedField.getVersion()).isEqualTo(1L);

        savedCalculatedField.setName("Test CF");

        CalculatedField updatedCalculatedField = doPost("/api/calculatedField", savedCalculatedField, CalculatedField.class);

        assertThat(updatedCalculatedField.getName()).isEqualTo(savedCalculatedField.getName());
        assertThat(updatedCalculatedField.getVersion()).isEqualTo(savedCalculatedField.getVersion() + 1);

        doDelete("/api/calculatedField/" + savedCalculatedField.getId().getId().toString())
                .andExpect(status().isOk());
    }

    @Test
    public void testSaveGeofencingCalculatedField() throws Exception {
        Device testDevice = createDevice("Test device", "1234567890");
        CalculatedField calculatedField = getCalculatedField(testDevice.getId(), CalculatedFieldType.GEOFENCING);

        CalculatedField savedCalculatedField = doPost("/api/calculatedField", calculatedField, CalculatedField.class);

        assertThat(savedCalculatedField).isNotNull();
        assertThat(savedCalculatedField.getId()).isNotNull();
        assertThat(savedCalculatedField.getCreatedTime()).isGreaterThan(0);
        assertThat(savedCalculatedField.getTenantId()).isEqualTo(savedTenant.getId());
        assertThat(savedCalculatedField.getEntityId()).isEqualTo(calculatedField.getEntityId());
        assertThat(savedCalculatedField.getType()).isEqualTo(calculatedField.getType());
        assertThat(savedCalculatedField.getName()).isEqualTo(calculatedField.getName());
        assertThat(savedCalculatedField.getConfiguration()).isEqualTo(getGeofencingCalculatedFieldConfig());
        assertThat(savedCalculatedField.getVersion()).isEqualTo(1L);

        savedCalculatedField.setName("Test CF");

        CalculatedField updatedCalculatedField = doPost("/api/calculatedField", savedCalculatedField, CalculatedField.class);

        assertThat(updatedCalculatedField.getName()).isEqualTo(savedCalculatedField.getName());
        assertThat(updatedCalculatedField.getVersion()).isEqualTo(savedCalculatedField.getVersion() + 1);

        doDelete("/api/calculatedField/" + savedCalculatedField.getId().getId().toString())
                .andExpect(status().isOk());
    }

    @Test
    public void testSavePropagationCalculatedField() throws Exception {
        Device testDevice = createDevice("Test device", "1234567890");
        CalculatedField calculatedField = getCalculatedField(testDevice.getId(), CalculatedFieldType.PROPAGATION);

        CalculatedField savedCalculatedField = doPost("/api/calculatedField", calculatedField, CalculatedField.class);

        assertThat(savedCalculatedField).isNotNull();
        assertThat(savedCalculatedField.getId()).isNotNull();
        assertThat(savedCalculatedField.getCreatedTime()).isGreaterThan(0);
        assertThat(savedCalculatedField.getTenantId()).isEqualTo(savedTenant.getId());
        assertThat(savedCalculatedField.getEntityId()).isEqualTo(calculatedField.getEntityId());
        assertThat(savedCalculatedField.getType()).isEqualTo(calculatedField.getType());
        assertThat(savedCalculatedField.getName()).isEqualTo(calculatedField.getName());
        assertThat(savedCalculatedField.getConfiguration()).isEqualTo(getPropagationCalculatedFieldConfig());
        assertThat(savedCalculatedField.getVersion()).isEqualTo(1L);

        savedCalculatedField.setName("Test CF");

        CalculatedField updatedCalculatedField = doPost("/api/calculatedField", savedCalculatedField, CalculatedField.class);

        assertThat(updatedCalculatedField.getName()).isEqualTo(savedCalculatedField.getName());
        assertThat(updatedCalculatedField.getVersion()).isEqualTo(savedCalculatedField.getVersion() + 1);

        doDelete("/api/calculatedField/" + savedCalculatedField.getId().getId().toString())
                .andExpect(status().isOk());
    }

    @Test
    public void testSaveEntityAggregationCalculatedField() throws Exception {
        Device testDevice = createDevice("Test device", "1234567890");
        CalculatedField calculatedField = getCalculatedField(testDevice.getId(), CalculatedFieldType.ENTITY_AGGREGATION);

        CalculatedField savedCalculatedField = doPost("/api/calculatedField", calculatedField, CalculatedField.class);

        assertThat(savedCalculatedField).isNotNull();
        assertThat(savedCalculatedField.getId()).isNotNull();
        assertThat(savedCalculatedField.getCreatedTime()).isGreaterThan(0);
        assertThat(savedCalculatedField.getTenantId()).isEqualTo(savedTenant.getId());
        assertThat(savedCalculatedField.getEntityId()).isEqualTo(calculatedField.getEntityId());
        assertThat(savedCalculatedField.getType()).isEqualTo(calculatedField.getType());
        assertThat(savedCalculatedField.getName()).isEqualTo(calculatedField.getName());
        assertThat(savedCalculatedField.getConfiguration()).isEqualTo(getEntityAggregationCalculatedFieldConfig());
        assertThat(savedCalculatedField.getVersion()).isEqualTo(1L);

        savedCalculatedField.setName("Test CF");

        CalculatedField updatedCalculatedField = doPost("/api/calculatedField", savedCalculatedField, CalculatedField.class);

        assertThat(updatedCalculatedField.getName()).isEqualTo(savedCalculatedField.getName());
        assertThat(updatedCalculatedField.getVersion()).isEqualTo(savedCalculatedField.getVersion() + 1);

        doDelete("/api/calculatedField/" + savedCalculatedField.getId().getId().toString())
                .andExpect(status().isOk());
    }

    @Test
    public void testSavePropagationCalculatedFieldWithNullArguments() throws Exception {
        Device testDevice = createDevice("Test device", "1234567890");
        CalculatedField calculatedField = getCalculatedField(testDevice.getId(), CalculatedFieldType.PROPAGATION, getPropagationCalculatedFieldConfig(null));

        doPost("/api/calculatedField", calculatedField)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("arguments must not be empty")));
    }

    @Test
    public void testGetCalculatedFieldById() throws Exception {
        Device testDevice = createDevice("Test device", "1234567890");
        CalculatedField calculatedField = getSimpleCalculatedField(testDevice.getId());

        CalculatedField savedCalculatedField = doPost("/api/calculatedField", calculatedField, CalculatedField.class);
        CalculatedField fetchedCalculatedField = doGet("/api/calculatedField/" + savedCalculatedField.getId().getId(), CalculatedField.class);

        assertThat(fetchedCalculatedField).isNotNull();
        assertThat(fetchedCalculatedField).isEqualTo(savedCalculatedField);

        doDelete("/api/calculatedField/" + savedCalculatedField.getId().getId().toString())
                .andExpect(status().isOk());
    }

    @Test
    public void testGetCalculatedFields() throws Exception {
        Device testDevice = createDevice("Test device", "1234567890");
        CalculatedField calculatedField = getSimpleCalculatedField(testDevice.getId());
        calculatedField = doPost("/api/calculatedField", calculatedField, CalculatedField.class);

        assertThat(getCalculatedFields(testDevice.getId(), null, new PageLink(10)).getData())
                .singleElement().isEqualTo(calculatedField);
        assertThat(getCalculatedFields(testDevice.getId(), CalculatedFieldType.SIMPLE, new PageLink(10)).getData())
                .singleElement().isEqualTo(calculatedField);
    }

    @Test
    public void testDeleteCalculatedField() throws Exception {
        Device testDevice = createDevice("Test device", "1234567890");
        CalculatedField calculatedField = getSimpleCalculatedField(testDevice.getId());

        CalculatedField savedCalculatedField = doPost("/api/calculatedField", calculatedField, CalculatedField.class);

        assertThat(savedCalculatedField).isNotNull();

        doDelete("/api/calculatedField/" + savedCalculatedField.getId().getId().toString())
                .andExpect(status().isOk());
        doGet("/api/calculatedField/" + savedCalculatedField.getId().getId()).andExpect(status().isNotFound());
    }

    private CalculatedField getSimpleCalculatedField(EntityId entityId) {
        return getCalculatedField(entityId, CalculatedFieldType.SIMPLE);
    }

    private CalculatedField getCalculatedField(EntityId entityId, CalculatedFieldType cfType) {
        return getCalculatedField(entityId, cfType, null);
    }

    private CalculatedField getCalculatedField(EntityId entityId, CalculatedFieldType cfType, CalculatedFieldConfiguration customConfiguration) {
        CalculatedField calculatedField = new CalculatedField();
        calculatedField.setEntityId(entityId);
        calculatedField.setType(cfType);
        calculatedField.setName("Test Calculated Field");
        calculatedField.setConfigurationVersion(1);
        if (customConfiguration != null) {
            calculatedField.setConfiguration(customConfiguration);
        } else switch (cfType) {
            case SIMPLE -> calculatedField.setConfiguration(getSimpleCalculatedFieldConfig());
            case GEOFENCING -> calculatedField.setConfiguration(getGeofencingCalculatedFieldConfig());
            case PROPAGATION -> calculatedField.setConfiguration(getPropagationCalculatedFieldConfig());
            case ENTITY_AGGREGATION -> calculatedField.setConfiguration(getEntityAggregationCalculatedFieldConfig());
        }
        calculatedField.setVersion(1L);
        return calculatedField;
    }

    private CalculatedFieldConfiguration getGeofencingCalculatedFieldConfig() {
        var config = new GeofencingCalculatedFieldConfiguration();

        var refDynamicSourceConfiguration = new RelationPathQueryDynamicSourceConfiguration();
        refDynamicSourceConfiguration.setLevels(List.of(new RelationPathLevel(EntitySearchDirection.TO, "FromSafeArea")));

        var zoneGroupConfiguration = new ZoneGroupConfiguration("perimeter", REPORT_TRANSITION_EVENTS_AND_PRESENCE_STATUS, false);
        zoneGroupConfiguration.setRefDynamicSourceConfiguration(refDynamicSourceConfiguration);

        config.setEntityCoordinates(new EntityCoordinates("latitide", "longitude"));
        config.setZoneGroups(Map.of("safeArea", zoneGroupConfiguration));
        config.setScheduledUpdateEnabled(false);
        config.setOutput(new TimeSeriesOutput());

        return config;
    }

    private CalculatedFieldConfiguration getPropagationCalculatedFieldConfig() {
        Argument arg = new Argument();
        arg.setRefEntityKey(new ReferencedEntityKey("temperature", ArgumentType.TS_LATEST, null));
        return getPropagationCalculatedFieldConfig(Map.of("t", arg));
    }

    private CalculatedFieldConfiguration getPropagationCalculatedFieldConfig(Map<String, Argument> arguments) {
        var config = new PropagationCalculatedFieldConfiguration();

        config.setRelation(new RelationPathLevel(EntitySearchDirection.TO, EntityRelation.CONTAINS_TYPE));

        config.setApplyExpressionToResolvedArguments(false);
        config.setExpression(null);
        config.setOutput(new TimeSeriesOutput());

        Argument arg = new Argument();
        arg.setRefEntityKey(new ReferencedEntityKey("temperature", ArgumentType.TS_LATEST, null));
        config.setArguments(arguments);

        return config;
    }

    private CalculatedFieldConfiguration getEntityAggregationCalculatedFieldConfig() {
        var config = new EntityAggregationCalculatedFieldConfiguration();

        Argument energyArgument = new Argument();
        energyArgument.setRefEntityKey(new ReferencedEntityKey("energy", ArgumentType.TS_LATEST, null));
        config.setArguments(Map.of("en", energyArgument));

        AggMetric metric = new AggMetric();
        metric.setInput(new AggKeyInput("en"));
        metric.setDefaultValue(9999L);
        config.setMetrics(Map.of("consumption", metric));

        config.setWatermark(new Watermark(TimeUnit.DAYS.toSeconds(1)));
        config.setInterval(new HourInterval("Europe/Kiev", TimeUnit.MINUTES.toSeconds(15)));

        config.setOutput(new TimeSeriesOutput());

        return config;
    }

    private CalculatedFieldConfiguration getSimpleCalculatedFieldConfig() {
        SimpleCalculatedFieldConfiguration config = new SimpleCalculatedFieldConfiguration();

        Argument argument = new Argument();
        argument.setRefEntityId(null);
        ReferencedEntityKey refEntityKey = new ReferencedEntityKey("temperature", ArgumentType.TS_LATEST, null);
        argument.setRefEntityKey(refEntityKey);

        config.setArguments(Map.of("T", argument));

        config.setExpression("T - (100 - H) / 5");

        TimeSeriesOutput output = new TimeSeriesOutput();
        output.setName("output");

        config.setOutput(output);

        return config;
    }

}
