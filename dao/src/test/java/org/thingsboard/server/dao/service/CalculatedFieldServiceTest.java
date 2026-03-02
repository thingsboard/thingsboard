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
package org.thingsboard.server.dao.service;

import org.apache.commons.lang3.RandomUtils;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.cf.configuration.Argument;
import org.thingsboard.server.common.data.cf.configuration.ArgumentType;
import org.thingsboard.server.common.data.cf.configuration.AttributesOutput;
import org.thingsboard.server.common.data.cf.configuration.CalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.ReferencedEntityKey;
import org.thingsboard.server.common.data.cf.configuration.RelationPathQueryDynamicSourceConfiguration;
import org.thingsboard.server.common.data.cf.configuration.SimpleCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.TimeSeriesOutput;
import org.thingsboard.server.common.data.cf.configuration.aggregation.AggFunction;
import org.thingsboard.server.common.data.cf.configuration.aggregation.AggKeyInput;
import org.thingsboard.server.common.data.cf.configuration.aggregation.AggMetric;
import org.thingsboard.server.common.data.cf.configuration.aggregation.RelatedEntitiesAggregationCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.geofencing.EntityCoordinates;
import org.thingsboard.server.common.data.cf.configuration.geofencing.GeofencingCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.geofencing.ZoneGroupConfiguration;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationPathLevel;
import org.thingsboard.server.dao.cf.CalculatedFieldService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.dao.tenant.TenantProfileService;
import org.thingsboard.server.exception.DataValidationException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.thingsboard.server.common.data.cf.configuration.geofencing.GeofencingReportStrategy.REPORT_TRANSITION_EVENTS_AND_PRESENCE_STATUS;

@DaoSqlTest
public class CalculatedFieldServiceTest extends AbstractServiceTest {

    @Autowired
    private CalculatedFieldService calculatedFieldService;
    @Autowired
    private DeviceService deviceService;
    @Autowired
    private TbTenantProfileCache tbTenantProfileCache;
    @Autowired
    private TenantProfileService tenantProfileService;

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
    }

    @Test
    public void testSaveGeofencingCalculatedField_shouldThrowWhenScheduledIntervalLessThanMinAllowedIntervalInTenantProfile() {
        // Arrange a device
        Device device = createTestDevice();

        // Build a valid Geofencing configuration
        GeofencingCalculatedFieldConfiguration cfg = new GeofencingCalculatedFieldConfiguration();

        // Coordinates: TS_LATEST, no dynamic source
        EntityCoordinates entityCoordinates = new EntityCoordinates("latitude", "longitude");
        cfg.setEntityCoordinates(entityCoordinates);

        // Zone-group argument (ATTRIBUTE) — make it DYNAMIC so scheduling is enabled
        ZoneGroupConfiguration zoneGroupConfiguration = new ZoneGroupConfiguration("allowed", REPORT_TRANSITION_EVENTS_AND_PRESENCE_STATUS, false);
        var dynamicSourceConfiguration = new RelationPathQueryDynamicSourceConfiguration();
        dynamicSourceConfiguration.setLevels(List.of(new RelationPathLevel(EntitySearchDirection.FROM, EntityRelation.CONTAINS_TYPE)));
        zoneGroupConfiguration.setRefDynamicSourceConfiguration(dynamicSourceConfiguration);
        cfg.setZoneGroups(Map.of("allowed", zoneGroupConfiguration));

        AttributesOutput out = new AttributesOutput();
        out.setScope(AttributeScope.SERVER_SCOPE);
        cfg.setOutput(out);

        // Get tenant profile min.
        int min = tbTenantProfileCache.get(tenantId)
                .getDefaultProfileConfiguration()
                .getMinAllowedScheduledUpdateIntervalInSecForCF();

        // Enable scheduling with an interval below tenant min
        cfg.setScheduledUpdateEnabled(true);
        int invalidInterval = RandomUtils.insecure().randomInt(1, min);
        cfg.setScheduledUpdateInterval(invalidInterval);

        // Create & save Calculated Field
        CalculatedField cf = new CalculatedField();
        cf.setTenantId(tenantId);
        cf.setEntityId(device.getId());
        cf.setType(CalculatedFieldType.GEOFENCING);
        cf.setName("GF min allowed scheduled update interval test");
        cf.setConfigurationVersion(0);
        cf.setConfiguration(cfg);

        assertThatThrownBy(() -> calculatedFieldService.save(cf))
                .isInstanceOf(DataValidationException.class)
                .hasCauseInstanceOf(IllegalArgumentException.class)
                .hasMessage("Scheduled update interval (" + invalidInterval +
                        " seconds) is less than minimum allowed interval in tenant profile: " + min + " seconds");
    }

    @Test
    public void testSaveGeofencingCalculatedField_shouldThrowWhenRelationLevelGreaterThanMaxAllowedRelationLevelInTenantProfile() {
        // Arrange a device
        Device device = createTestDevice();

        GeofencingCalculatedFieldConfiguration cfg = new GeofencingCalculatedFieldConfiguration();

        // Coordinates: TS_LATEST, no dynamic source
        EntityCoordinates entityCoordinates = new EntityCoordinates("latitude", "longitude");
        cfg.setEntityCoordinates(entityCoordinates);

        int maxRelationLevel = tbTenantProfileCache.get(tenantId)
                .getDefaultProfileConfiguration()
                .getMaxRelationLevelPerCfArgument();

        // Zone-group argument (ATTRIBUTE)
        ZoneGroupConfiguration zoneGroupConfiguration = new ZoneGroupConfiguration("allowed", REPORT_TRANSITION_EVENTS_AND_PRESENCE_STATUS, false);
        var dynamicSourceConfiguration = new RelationPathQueryDynamicSourceConfiguration();

        List<RelationPathLevel> levels = new ArrayList<>();
        for (int i = 0; i < maxRelationLevel + 1; i++) {
            levels.add(mock(RelationPathLevel.class));
        }

        dynamicSourceConfiguration.setLevels(levels);
        zoneGroupConfiguration.setRefDynamicSourceConfiguration(dynamicSourceConfiguration);
        cfg.setZoneGroups(Map.of("allowed", zoneGroupConfiguration));

        AttributesOutput out = new AttributesOutput();
        out.setScope(AttributeScope.SERVER_SCOPE);
        cfg.setOutput(out);

        // Create & save Calculated Field
        CalculatedField cf = new CalculatedField();
        cf.setTenantId(tenantId);
        cf.setEntityId(device.getId());
        cf.setType(CalculatedFieldType.GEOFENCING);
        cf.setName("GF max relation level test");
        cf.setConfigurationVersion(0);
        cf.setConfiguration(cfg);

        assertThatThrownBy(() -> calculatedFieldService.save(cf))
                .isInstanceOf(DataValidationException.class)
                .hasCauseInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith("Max relation level is greater than configured maximum allowed relation level in tenant profile");
    }

    @Test
    public void testSaveGeofencingCalculatedField_shouldSaveWithoutDataValidationExceptionOnScheduledUpdateInterval() {
        // Arrange a device
        Device device = createTestDevice();

        // Build a valid Geofencing configuration
        GeofencingCalculatedFieldConfiguration cfg = new GeofencingCalculatedFieldConfiguration();

        // Coordinates: TS_LATEST, no dynamic source
        EntityCoordinates entityCoordinates = new EntityCoordinates("latitude", "longitude");
        cfg.setEntityCoordinates(entityCoordinates);

        // Zone-group argument (ATTRIBUTE) — make it DYNAMIC so scheduling is enabled
        ZoneGroupConfiguration zoneGroupConfiguration = new ZoneGroupConfiguration("allowed", REPORT_TRANSITION_EVENTS_AND_PRESENCE_STATUS, false);
        var dynamicSourceConfiguration = new RelationPathQueryDynamicSourceConfiguration();
        dynamicSourceConfiguration.setLevels(List.of(new RelationPathLevel(EntitySearchDirection.FROM, EntityRelation.CONTAINS_TYPE)));
        zoneGroupConfiguration.setRefDynamicSourceConfiguration(dynamicSourceConfiguration);
        cfg.setZoneGroups(Map.of("allowed", zoneGroupConfiguration));

        AttributesOutput out = new AttributesOutput();
        out.setScope(AttributeScope.SERVER_SCOPE);
        cfg.setOutput(out);

        // Get tenant profile min.
        int min = tbTenantProfileCache.get(tenantId)
                .getDefaultProfileConfiguration()
                .getMinAllowedScheduledUpdateIntervalInSecForCF();
        int valueFromConfig = min + 100;

        // Enable scheduling with an interval greater than tenant min
        cfg.setScheduledUpdateEnabled(true);
        cfg.setScheduledUpdateInterval(valueFromConfig);

        // Create & save Calculated Field
        CalculatedField cf = new CalculatedField();
        cf.setTenantId(tenantId);
        cf.setEntityId(device.getId());
        cf.setType(CalculatedFieldType.GEOFENCING);
        cf.setName("GF no validation error test");
        cf.setConfigurationVersion(0);
        cf.setConfiguration(cfg);

        CalculatedField saved = calculatedFieldService.save(cf);

        assertThat(saved).isNotNull();
        assertThat(saved.getConfiguration()).isInstanceOf(GeofencingCalculatedFieldConfiguration.class);

        var geofencingCalculatedFieldConfiguration = (GeofencingCalculatedFieldConfiguration) saved.getConfiguration();

        int savedInterval = geofencingCalculatedFieldConfiguration.getScheduledUpdateInterval();
        assertThat(savedInterval).isEqualTo(valueFromConfig);
    }

    @Test
    public void testSaveGeofencingCalculatedField_shouldAcceptZeroScheduledUpdateIntervalWhenTenantProfileAllows() {
        // GIVEN
        var device = createTestDevice();

        // Store original value and update tenant profile to allow 0 as min scheduled update interval
        TenantProfile tenantProfile = tenantProfileService.findTenantProfileById(tenantId, tenant.getTenantProfileId());
        int originalMinScheduledUpdateInterval = tenantProfile.getDefaultProfileConfiguration().getMinAllowedScheduledUpdateIntervalInSecForCF();
        tenantProfile.getDefaultProfileConfiguration().setMinAllowedScheduledUpdateIntervalInSecForCF(0);
        tenantProfileService.saveTenantProfile(tenantId, tenantProfile);
        tbTenantProfileCache.evict(tenantProfile.getId());

        try {
            // Build a valid Geofencing configuration
            var cfg = new GeofencingCalculatedFieldConfiguration();

            // Coordinates: TS_LATEST, no dynamic source
            var entityCoordinates = new EntityCoordinates("latitude", "longitude");
            cfg.setEntityCoordinates(entityCoordinates);

            // Zone-group argument (ATTRIBUTE) — make it DYNAMIC so scheduling is enabled
            var zoneGroupConfiguration = new ZoneGroupConfiguration("allowed", REPORT_TRANSITION_EVENTS_AND_PRESENCE_STATUS, false);
            var dynamicSourceConfiguration = new RelationPathQueryDynamicSourceConfiguration();
            dynamicSourceConfiguration.setLevels(List.of(new RelationPathLevel(EntitySearchDirection.FROM, EntityRelation.CONTAINS_TYPE)));
            zoneGroupConfiguration.setRefDynamicSourceConfiguration(dynamicSourceConfiguration);
            cfg.setZoneGroups(Map.of("allowed", zoneGroupConfiguration));

            // Enable scheduling with interval = 0
            cfg.setScheduledUpdateEnabled(true);
            cfg.setScheduledUpdateInterval(0);

            // Create Calculated Field
            var cf = new CalculatedField();
            cf.setTenantId(tenantId);
            cf.setEntityId(device.getId());
            cf.setType(CalculatedFieldType.GEOFENCING);
            cf.setName("GF zero scheduled update interval test");
            cf.setConfigurationVersion(0);
            cf.setConfiguration(cfg);

            var out = new AttributesOutput();
            out.setScope(AttributeScope.SERVER_SCOPE);
            cfg.setOutput(out);

            // WHEN
            CalculatedField saved = calculatedFieldService.save(cf);

            // THEN
            assertThat(saved).isNotNull();
            assertThat(saved.getConfiguration()).isInstanceOf(GeofencingCalculatedFieldConfiguration.class);

            var savedConfig = (GeofencingCalculatedFieldConfiguration) saved.getConfiguration();
            assertThat(savedConfig.getScheduledUpdateInterval()).isEqualTo(0);
        } finally {
            // Restore original tenant profile value
            tenantProfile.getProfileConfiguration().orElseThrow().setMinAllowedScheduledUpdateIntervalInSecForCF(originalMinScheduledUpdateInterval);
            tenantProfileService.saveTenantProfile(tenantId, tenantProfile);
            tbTenantProfileCache.evict(tenantProfile.getId());
        }
    }

    @Test
    public void testSaveCalculatedFieldWithExistingName() {
        Device device = createTestDevice();
        CalculatedField calculatedField = getCalculatedField(device.getId(), device.getId());
        calculatedFieldService.save(calculatedField);

        assertThatThrownBy(() -> calculatedFieldService.save(calculatedField))
                .isInstanceOf(DataValidationException.class)
                .hasMessage("Calculated field with such name and type already exists");
    }

    @Test
    public void testFindCalculatedFieldById() {
        CalculatedField savedCalculatedField = saveValidCalculatedField();
        CalculatedField fetchedCalculatedField = calculatedFieldService.findById(tenantId, savedCalculatedField.getId());

        assertThat(fetchedCalculatedField).isEqualTo(savedCalculatedField);
    }

    @Test
    public void testDeleteCalculatedField() {
        CalculatedField savedCalculatedField = saveValidCalculatedField();

        calculatedFieldService.deleteCalculatedField(tenantId, savedCalculatedField.getId());

        assertThat(calculatedFieldService.findById(tenantId, savedCalculatedField.getId())).isNull();
    }

    @Test
    public void testSaveRelatedEntitiesAggregationCF_shouldUseMinScheduledUpdateIntervalFromTenantProfileWhenNotSet() {
        // GIVEN
        var device = createTestDevice();

        var cfg = new RelatedEntitiesAggregationCalculatedFieldConfiguration();
        cfg.setRelation(new RelationPathLevel(EntitySearchDirection.FROM, EntityRelation.CONTAINS_TYPE));

        var argument = new Argument();
        argument.setRefEntityKey(new ReferencedEntityKey("temperature", ArgumentType.TS_LATEST, null));
        cfg.setArguments(Map.of("temp", argument));

        var metric = new AggMetric();
        metric.setFunction(AggFunction.AVG);
        metric.setInput(new AggKeyInput("temp"));
        cfg.setMetrics(Map.of("avgTemp", metric));

        var output = new TimeSeriesOutput();
        output.setName("avgTemperature");
        cfg.setOutput(output);

        int minDeduplicationInterval = (int) tbTenantProfileCache.get(tenantId)
                .getDefaultProfileConfiguration()
                .getMinAllowedDeduplicationIntervalInSecForCF();
        cfg.setDeduplicationIntervalInSec(minDeduplicationInterval);

        // Do NOT set scheduledUpdateInterval - it should default to tenant profile min value

        var cf = new CalculatedField();
        cf.setTenantId(tenantId);
        cf.setEntityId(device.getId());
        cf.setType(CalculatedFieldType.RELATED_ENTITIES_AGGREGATION);
        cf.setName("Related Entities Aggregation CF - default scheduled interval test");
        cf.setConfigurationVersion(0);
        cf.setConfiguration(cfg);

        // WHEN
        CalculatedField saved = calculatedFieldService.save(cf);

        // THEN
        assertThat(saved).isNotNull();
        assertThat(saved.getConfiguration()).isInstanceOf(RelatedEntitiesAggregationCalculatedFieldConfiguration.class);

        var savedConfig = (RelatedEntitiesAggregationCalculatedFieldConfiguration) saved.getConfiguration();
        int expectedMinScheduledUpdateInterval = tbTenantProfileCache.get(tenantId)
                .getDefaultProfileConfiguration()
                .getMinAllowedScheduledUpdateIntervalInSecForCF();

        assertThat(savedConfig.getScheduledUpdateInterval()).isEqualTo(expectedMinScheduledUpdateInterval);
    }

    @Test
    public void testSaveRelatedEntitiesAggregationCF_shouldThrowWhenScheduledUpdateIntervalLessThanMinAllowed() {
        // GIVEN
        var device = createTestDevice();

        var cfg = new RelatedEntitiesAggregationCalculatedFieldConfiguration();
        cfg.setRelation(new RelationPathLevel(EntitySearchDirection.FROM, EntityRelation.CONTAINS_TYPE));

        var argument = new Argument();
        argument.setRefEntityKey(new ReferencedEntityKey("temperature", ArgumentType.TS_LATEST, null));
        cfg.setArguments(Map.of("temp", argument));

        var metric = new AggMetric();
        metric.setFunction(AggFunction.AVG);
        metric.setInput(new AggKeyInput("temp"));
        cfg.setMetrics(Map.of("avgTemp", metric));

        var output = new TimeSeriesOutput();
        output.setName("avgTemperature");
        cfg.setOutput(output);

        int minDeduplicationInterval = (int) tbTenantProfileCache.get(tenantId)
                .getDefaultProfileConfiguration()
                .getMinAllowedDeduplicationIntervalInSecForCF();
        cfg.setDeduplicationIntervalInSec(minDeduplicationInterval);

        int minScheduledUpdateInterval = tbTenantProfileCache.get(tenantId)
                .getDefaultProfileConfiguration()
                .getMinAllowedScheduledUpdateIntervalInSecForCF();
        int invalidInterval = RandomUtils.insecure().randomInt(1, minScheduledUpdateInterval);
        cfg.setScheduledUpdateInterval(invalidInterval);

        var cf = new CalculatedField();
        cf.setTenantId(tenantId);
        cf.setEntityId(device.getId());
        cf.setType(CalculatedFieldType.RELATED_ENTITIES_AGGREGATION);
        cf.setName("Related Entities Aggregation CF - invalid scheduled interval test");
        cf.setConfigurationVersion(0);
        cf.setConfiguration(cfg);

        // WHEN-THEN
        assertThatThrownBy(() -> calculatedFieldService.save(cf))
                .isInstanceOf(DataValidationException.class)
                .hasCauseInstanceOf(IllegalArgumentException.class)
                .hasMessage("Scheduled update interval (" + invalidInterval +
                        " seconds) is less than minimum allowed interval in tenant profile: " + minScheduledUpdateInterval + " seconds");
    }

    @Test
    public void testSaveRelatedEntitiesAggregationCF_shouldAcceptValidScheduledUpdateInterval() {
        // GIVEN
        var device = createTestDevice();

        var cfg = new RelatedEntitiesAggregationCalculatedFieldConfiguration();
        cfg.setRelation(new RelationPathLevel(EntitySearchDirection.FROM, EntityRelation.CONTAINS_TYPE));

        var argument = new Argument();
        argument.setRefEntityKey(new ReferencedEntityKey("temperature", ArgumentType.TS_LATEST, null));
        cfg.setArguments(Map.of("temp", argument));

        var metric = new AggMetric();
        metric.setFunction(AggFunction.AVG);
        metric.setInput(new AggKeyInput("temp"));
        cfg.setMetrics(Map.of("avgTemp", metric));

        var output = new TimeSeriesOutput();
        output.setName("avgTemperature");
        cfg.setOutput(output);

        int minDeduplicationInterval = (int) tbTenantProfileCache.get(tenantId)
                .getDefaultProfileConfiguration()
                .getMinAllowedDeduplicationIntervalInSecForCF();
        cfg.setDeduplicationIntervalInSec(minDeduplicationInterval);

        int minScheduledUpdateInterval = tbTenantProfileCache.get(tenantId)
                .getDefaultProfileConfiguration()
                .getMinAllowedScheduledUpdateIntervalInSecForCF();
        int customScheduledUpdateInterval = minScheduledUpdateInterval + 100;
        cfg.setScheduledUpdateInterval(customScheduledUpdateInterval);

        var cf = new CalculatedField();
        cf.setTenantId(tenantId);
        cf.setEntityId(device.getId());
        cf.setType(CalculatedFieldType.RELATED_ENTITIES_AGGREGATION);
        cf.setName("Related Entities Aggregation CF - valid scheduled interval test");
        cf.setConfigurationVersion(0);
        cf.setConfiguration(cfg);

        // WHEN
        CalculatedField saved = calculatedFieldService.save(cf);

        // THEN
        assertThat(saved).isNotNull();
        assertThat(saved.getConfiguration()).isInstanceOf(RelatedEntitiesAggregationCalculatedFieldConfiguration.class);

        var savedConfig = (RelatedEntitiesAggregationCalculatedFieldConfiguration) saved.getConfiguration();
        assertThat(savedConfig.getScheduledUpdateInterval()).isEqualTo(customScheduledUpdateInterval);
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

        TimeSeriesOutput output = new TimeSeriesOutput();
        output.setName("output");

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
