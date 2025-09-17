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
package org.thingsboard.server.dao.service;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.cf.configuration.Argument;
import org.thingsboard.server.common.data.cf.configuration.ArgumentType;
import org.thingsboard.server.common.data.cf.configuration.CalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.Output;
import org.thingsboard.server.common.data.cf.configuration.OutputType;
import org.thingsboard.server.common.data.cf.configuration.ReferencedEntityKey;
import org.thingsboard.server.common.data.cf.configuration.RelationQueryDynamicSourceConfiguration;
import org.thingsboard.server.common.data.cf.configuration.SimpleCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.geofencing.EntityCoordinates;
import org.thingsboard.server.common.data.cf.configuration.geofencing.GeofencingCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.geofencing.ZoneGroupConfiguration;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.dao.cf.CalculatedFieldService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.thingsboard.server.common.data.cf.configuration.geofencing.GeofencingReportStrategy.REPORT_TRANSITION_EVENTS_AND_PRESENCE_STATUS;

@DaoSqlTest
public class CalculatedFieldServiceTest extends AbstractServiceTest {

    @Autowired
    private CalculatedFieldService calculatedFieldService;
    @Autowired
    private DeviceService deviceService;
    @Autowired
    private TbTenantProfileCache tbTenantProfileCache;

    private ListeningExecutorService executor;

    @Before
    public void before() {
        executor = MoreExecutors.listeningDecorator(ThingsBoardExecutors.newWorkStealingPool(8, getClass()));
    }

    @After
    public void after() {
        executor.shutdownNow();
    }

    @Test
    public void testSaveCalculatedField() {
        Device device = createTestDevice();
        CalculatedField calculatedField = getCalculatedField(device.getId(), device.getId());
        CalculatedField savedCalculatedField = calculatedFieldService.save(calculatedField);

        assertThat(savedCalculatedField).isNotNull();
        assertThat(savedCalculatedField.getId()).isNotNull();
        assertThat(savedCalculatedField.getCreatedTime()).isGreaterThan(0);
        assertThat(savedCalculatedField.getTenantId()).isEqualTo(calculatedField.getTenantId());
        assertThat(savedCalculatedField.getEntityId()).isEqualTo(calculatedField.getEntityId());
        assertThat(savedCalculatedField.getType()).isEqualTo(calculatedField.getType());
        assertThat(savedCalculatedField.getName()).isEqualTo(calculatedField.getName());
        assertThat(savedCalculatedField.getConfiguration()).isEqualTo(calculatedField.getConfiguration());
        assertThat(savedCalculatedField.getVersion()).isEqualTo(1L);

        savedCalculatedField.setName("Test CF");

        CalculatedField updatedCalculatedField = calculatedFieldService.save(savedCalculatedField);

        assertThat(updatedCalculatedField.getName()).isEqualTo(savedCalculatedField.getName());
        assertThat(updatedCalculatedField.getVersion()).isEqualTo(savedCalculatedField.getVersion() + 1);

        calculatedFieldService.deleteCalculatedField(tenantId, savedCalculatedField.getId());
    }

    @Test
    public void testSaveGeofencingCalculatedField_shouldNotChangeScheduledInterval() {
        // Arrange a device
        Device device = createTestDevice();

        // Build a valid Geofencing configuration
        GeofencingCalculatedFieldConfiguration cfg = new GeofencingCalculatedFieldConfiguration();

        // Coordinates: TS_LATEST, no dynamic source
        EntityCoordinates entityCoordinates = new EntityCoordinates("latitude", "longitude");
        cfg.setEntityCoordinates(entityCoordinates);

        // Zone-group argument (ATTRIBUTE) — no DYNAMIC configuration, so no scheduling even if the scheduled interval is set
        ZoneGroupConfiguration zoneGroupConfiguration = new ZoneGroupConfiguration("allowed", REPORT_TRANSITION_EVENTS_AND_PRESENCE_STATUS, false);
        zoneGroupConfiguration.setRefEntityId(device.getId());
        cfg.setZoneGroups(Map.of("allowed", zoneGroupConfiguration));

        // Set a scheduled interval to some value
        cfg.setScheduledUpdateInterval(600);

        // Create & save Calculated Field
        CalculatedField cf = new CalculatedField();
        cf.setTenantId(tenantId);
        cf.setEntityId(device.getId());
        cf.setType(CalculatedFieldType.GEOFENCING);
        cf.setName("GF clamp test");
        cf.setConfigurationVersion(0);
        cf.setConfiguration(cfg);

        CalculatedField saved = calculatedFieldService.save(cf);

        assertThat(saved).isNotNull();
        assertThat(saved.getConfiguration()).isInstanceOf(GeofencingCalculatedFieldConfiguration.class);

        var geofencingCalculatedFieldConfiguration = (GeofencingCalculatedFieldConfiguration) saved.getConfiguration();

        // Assert: the interval is saved, but scheduling is not enabled
        int savedInterval = geofencingCalculatedFieldConfiguration.getScheduledUpdateInterval();
        boolean scheduledUpdateEnabled = geofencingCalculatedFieldConfiguration.isScheduledUpdateEnabled();

        assertThat(savedInterval).isEqualTo(600);
        assertThat(scheduledUpdateEnabled).isFalse();

        calculatedFieldService.deleteCalculatedField(tenantId, saved.getId());
    }

    @Test
    public void testSaveGeofencingCalculatedField_shouldThrowWhenScheduledIntervalIsLessThanMinAllowedIntervalInTenantProfile() {
        // Arrange a device
        Device device = createTestDevice();

        // Build a valid Geofencing configuration
        GeofencingCalculatedFieldConfiguration cfg = new GeofencingCalculatedFieldConfiguration();

        // Coordinates: TS_LATEST, no dynamic source
        EntityCoordinates entityCoordinates = new EntityCoordinates("latitude", "longitude");
        cfg.setEntityCoordinates(entityCoordinates);

        // Zone-group argument (ATTRIBUTE) — make it DYNAMIC so scheduling is enabled
        ZoneGroupConfiguration zoneGroupConfiguration = new ZoneGroupConfiguration("allowed", REPORT_TRANSITION_EVENTS_AND_PRESENCE_STATUS, false);
        var dynamicSourceConfiguration = new RelationQueryDynamicSourceConfiguration();
        dynamicSourceConfiguration.setDirection(EntitySearchDirection.FROM);
        dynamicSourceConfiguration.setMaxLevel(1);
        dynamicSourceConfiguration.setRelationType(EntityRelation.CONTAINS_TYPE);
        zoneGroupConfiguration.setRefDynamicSourceConfiguration(dynamicSourceConfiguration);
        cfg.setZoneGroups(Map.of("allowed", zoneGroupConfiguration));

        // Enable scheduling with an interval below tenant min
        cfg.setScheduledUpdateInterval(600);

        // Create & save Calculated Field
        CalculatedField cf = new CalculatedField();
        cf.setTenantId(tenantId);
        cf.setEntityId(device.getId());
        cf.setType(CalculatedFieldType.GEOFENCING);
        cf.setName("GF clamp test");
        cf.setConfigurationVersion(0);
        cf.setConfiguration(cfg);

        assertThatThrownBy(() -> calculatedFieldService.save(cf))
                .isInstanceOf(DataValidationException.class)
                .hasCauseInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith("Scheduled update interval is less than configured " +
                                        "minimum allowed interval in tenant profile: ");
    }

    @Test
    public void testSaveGeofencingCalculatedField_shouldThrowWhenRelationLevelIsGreaterThanMaxAllowedRelationLevelInTenantProfile() {
        // Arrange a device
        Device device = createTestDevice();

        // Build a valid Geofencing configuration
        GeofencingCalculatedFieldConfiguration cfg = new GeofencingCalculatedFieldConfiguration();

        // Coordinates: TS_LATEST, no dynamic source
        EntityCoordinates entityCoordinates = new EntityCoordinates("latitude", "longitude");
        cfg.setEntityCoordinates(entityCoordinates);

        // Zone-group argument (ATTRIBUTE) — make it DYNAMIC so scheduling is enabled
        ZoneGroupConfiguration zoneGroupConfiguration = new ZoneGroupConfiguration( "allowed", REPORT_TRANSITION_EVENTS_AND_PRESENCE_STATUS, false);
        var dynamicSourceConfiguration = new RelationQueryDynamicSourceConfiguration();
        dynamicSourceConfiguration.setDirection(EntitySearchDirection.FROM);
        dynamicSourceConfiguration.setMaxLevel(Integer.MAX_VALUE);
        dynamicSourceConfiguration.setRelationType(EntityRelation.CONTAINS_TYPE);
        zoneGroupConfiguration.setRefDynamicSourceConfiguration(dynamicSourceConfiguration);
        cfg.setZoneGroups(Map.of("allowed", zoneGroupConfiguration));

        // Create & save Calculated Field
        CalculatedField cf = new CalculatedField();
        cf.setTenantId(tenantId);
        cf.setEntityId(device.getId());
        cf.setType(CalculatedFieldType.GEOFENCING);
        cf.setName("GF clamp test");
        cf.setConfigurationVersion(0);
        cf.setConfiguration(cfg);

        assertThatThrownBy(() -> calculatedFieldService.save(cf))
                .isInstanceOf(DataValidationException.class)
                .hasCauseInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith("Max relation level is greater than configured maximum allowed relation level in tenant profile");
    }

    @Test
    public void testSaveGeofencingCalculatedField_shouldUseScheduledIntervalFromConfig() {
        // Arrange a device
        Device device = createTestDevice();

        // Build a valid Geofencing configuration
        GeofencingCalculatedFieldConfiguration cfg = new GeofencingCalculatedFieldConfiguration();

        // Coordinates: TS_LATEST, no dynamic source
        EntityCoordinates entityCoordinates = new EntityCoordinates("latitude", "longitude");
        cfg.setEntityCoordinates(entityCoordinates);

        // Zone-group argument (ATTRIBUTE) — make it DYNAMIC so scheduling is enabled
        ZoneGroupConfiguration zoneGroupConfiguration = new ZoneGroupConfiguration( "allowed", REPORT_TRANSITION_EVENTS_AND_PRESENCE_STATUS, false);
        var dynamicSourceConfiguration = new RelationQueryDynamicSourceConfiguration();
        dynamicSourceConfiguration.setDirection(EntitySearchDirection.FROM);
        dynamicSourceConfiguration.setMaxLevel(1);
        dynamicSourceConfiguration.setRelationType(EntityRelation.CONTAINS_TYPE);
        zoneGroupConfiguration.setRefDynamicSourceConfiguration(dynamicSourceConfiguration);
        cfg.setZoneGroups(Map.of("allowed", zoneGroupConfiguration));

        // Get tenant profile min.
        int min = tbTenantProfileCache.get(tenantId)
                .getDefaultProfileConfiguration()
                .getMinAllowedScheduledUpdateIntervalInSecForCF();


        // Enable scheduling with an interval greater than tenant min
        int valueFromConfig = min + 100;
        cfg.setScheduledUpdateInterval(valueFromConfig);

        // Create & save Calculated Field
        CalculatedField cf = new CalculatedField();
        cf.setTenantId(tenantId);
        cf.setEntityId(device.getId());
        cf.setType(CalculatedFieldType.GEOFENCING);
        cf.setName("GF no clamp test");
        cf.setConfigurationVersion(0);
        cf.setConfiguration(cfg);

        CalculatedField saved = calculatedFieldService.save(cf);

        assertThat(saved).isNotNull();
        assertThat(saved.getConfiguration()).isInstanceOf(GeofencingCalculatedFieldConfiguration.class);

        var geofencingCalculatedFieldConfiguration = (GeofencingCalculatedFieldConfiguration) saved.getConfiguration();

        // Assert: the interval is clamped up to tenant profile min (or stays >= original if already >= min)
        int savedInterval = geofencingCalculatedFieldConfiguration.getScheduledUpdateInterval();
        assertThat(savedInterval).isEqualTo(valueFromConfig);

        calculatedFieldService.deleteCalculatedField(tenantId, saved.getId());
    }

    @Test
    public void testSaveCalculatedFieldWithExistingName() {
        Device device = createTestDevice();
        CalculatedField calculatedField = getCalculatedField(device.getId(), device.getId());
        calculatedFieldService.save(calculatedField);

        assertThatThrownBy(() -> calculatedFieldService.save(calculatedField))
                .isInstanceOf(DataValidationException.class)
                .hasMessage("Calculated Field with such name is already in exists!");
    }

    @Test
    public void testFindCalculatedFieldById() {
        CalculatedField savedCalculatedField = saveValidCalculatedField();
        CalculatedField fetchedCalculatedField = calculatedFieldService.findById(tenantId, savedCalculatedField.getId());

        assertThat(fetchedCalculatedField).isEqualTo(savedCalculatedField);

        calculatedFieldService.deleteCalculatedField(tenantId, savedCalculatedField.getId());
    }

    @Test
    public void testDeleteCalculatedField() {
        CalculatedField savedCalculatedField = saveValidCalculatedField();

        calculatedFieldService.deleteCalculatedField(tenantId, savedCalculatedField.getId());

        assertThat(calculatedFieldService.findById(tenantId, savedCalculatedField.getId())).isNull();
    }

    private CalculatedField saveValidCalculatedField() {
        Device device = createTestDevice();
        CalculatedField calculatedField = getCalculatedField(device.getId(), device.getId());
        return calculatedFieldService.save(calculatedField);
    }

    private CalculatedField getCalculatedField(EntityId entityId, EntityId referencedEntityId) {
        CalculatedField calculatedField = new CalculatedField();
        calculatedField.setTenantId(tenantId);
        calculatedField.setEntityId(entityId);
        calculatedField.setType(CalculatedFieldType.SIMPLE);
        calculatedField.setName("Test Calculated Field");
        calculatedField.setConfigurationVersion(1);
        calculatedField.setConfiguration(getCalculatedFieldConfig(referencedEntityId));
        return calculatedField;
    }

    private CalculatedFieldConfiguration getCalculatedFieldConfig(EntityId referencedEntityId) {
        SimpleCalculatedFieldConfiguration config = new SimpleCalculatedFieldConfiguration();

        Argument argument = new Argument();
        argument.setRefEntityId(referencedEntityId);
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

    private Device createTestDevice() {
        Device device = new Device();
        device.setTenantId(tenantId);
        device.setName("Test");
        return deviceService.saveDevice(device);
    }

}
