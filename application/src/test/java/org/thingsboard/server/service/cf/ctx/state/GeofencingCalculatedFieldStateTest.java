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
package org.thingsboard.server.service.cf.ctx.state;

import com.google.common.util.concurrent.Futures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.cf.configuration.CalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.Output;
import org.thingsboard.server.common.data.cf.configuration.RelationPathQueryDynamicSourceConfiguration;
import org.thingsboard.server.common.data.cf.configuration.TimeSeriesOutput;
import org.thingsboard.server.common.data.cf.configuration.geofencing.EntityCoordinates;
import org.thingsboard.server.common.data.cf.configuration.geofencing.GeofencingCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.geofencing.GeofencingReportStrategy;
import org.thingsboard.server.common.data.cf.configuration.geofencing.ZoneGroupConfiguration;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.DoubleDataEntry;
import org.thingsboard.server.common.data.kv.JsonDataEntry;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationPathLevel;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.service.cf.TelemetryCalculatedFieldResult;
import org.thingsboard.server.service.cf.ctx.state.geofencing.GeofencingArgumentEntry;
import org.thingsboard.server.service.cf.ctx.state.geofencing.GeofencingCalculatedFieldState;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.thingsboard.server.common.data.cf.configuration.geofencing.EntityCoordinates.ENTITY_ID_LATITUDE_ARGUMENT_KEY;
import static org.thingsboard.server.common.data.cf.configuration.geofencing.EntityCoordinates.ENTITY_ID_LONGITUDE_ARGUMENT_KEY;
import static org.thingsboard.server.common.data.cf.configuration.geofencing.GeofencingReportStrategy.REPORT_TRANSITION_EVENTS_AND_PRESENCE_STATUS;

@ExtendWith(MockitoExtension.class)
public class GeofencingCalculatedFieldStateTest {

    private final TenantId TENANT_ID = TenantId.fromUUID(UUID.fromString("8f83eeca-b5cd-4955-9241-09d1393768c6"));
    private final DeviceId DEVICE_ID = new DeviceId(UUID.fromString("688b529d-cfbe-4430-91c5-60b4f4e5d3cf"));
    private final AssetId ZONE_1_ID = new AssetId(UUID.fromString("c0e3031c-7df1-45e4-9590-cfd621a4d714"));
    private final AssetId ZONE_2_ID = new AssetId(UUID.fromString("e7da6200-2096-4038-a343-ade9ea4fa3e4"));

    private final SingleValueArgumentEntry latitudeArgEntry = new SingleValueArgumentEntry(System.currentTimeMillis() - 10, new DoubleDataEntry("latitude", 50.4730), 145L);
    private final SingleValueArgumentEntry longitudeArgEntry = new SingleValueArgumentEntry(System.currentTimeMillis() - 6, new DoubleDataEntry("longitude", 30.5050), 165L);

    private final JsonDataEntry allowedZoneDataEntry = new JsonDataEntry("zone", "[[50.472000, 30.504000], [50.472000, 30.506000], [50.474000, 30.506000], [50.474000, 30.504000]]");
    private final BaseAttributeKvEntry allowedZoneAttributeKvEntry = new BaseAttributeKvEntry(allowedZoneDataEntry, System.currentTimeMillis(), 0L);
    private final GeofencingArgumentEntry geofencingAllowedZoneArgEntry = new GeofencingArgumentEntry(Map.of(ZONE_1_ID, allowedZoneAttributeKvEntry));

    private final JsonDataEntry restrictedZoneDataEntry = new JsonDataEntry("zone", "[[50.475000, 30.510000], [50.475000, 30.512000], [50.477000, 30.512000], [50.477000, 30.510000]]");
    private final BaseAttributeKvEntry restrictedZoneAttributeKvEntry = new BaseAttributeKvEntry(restrictedZoneDataEntry, System.currentTimeMillis(), 0L);
    private final GeofencingArgumentEntry geofencingRestrictedZoneArgEntry = new GeofencingArgumentEntry(Map.of(ZONE_2_ID, restrictedZoneAttributeKvEntry));


    private GeofencingCalculatedFieldState state;
    private CalculatedFieldCtx ctx;

    @Mock
    private TenantProfile tenantProfile;
    @Mock
    private TbTenantProfileCache tenantProfileCache;
    @Mock
    private RelationService relationService;
    @InjectMocks
    private ActorSystemContext systemContext;

    @BeforeEach
    void setUp() {
        when(tenantProfileCache.get(any(TenantId.class))).thenReturn(tenantProfile);
        when(tenantProfile.getProfileConfiguration()).thenReturn(Optional.of(new DefaultTenantProfileConfiguration()));
        ctx = new CalculatedFieldCtx(getCalculatedField(), systemContext);
        ctx.init();
        state = new GeofencingCalculatedFieldState(ctx.getEntityId());
        state.setCtx(ctx, null);
        state.init(false);
    }

    @Test
    void testType() {
        assertThat(state.getType()).isEqualTo(CalculatedFieldType.GEOFENCING);
    }

    @Test
    void testUpdateState() {
        state.arguments = new HashMap<>(Map.of(
                ENTITY_ID_LATITUDE_ARGUMENT_KEY, latitudeArgEntry,
                ENTITY_ID_LONGITUDE_ARGUMENT_KEY, longitudeArgEntry
        ));

        Map<String, ArgumentEntry> newArgs = Map.of("allowedZones", geofencingAllowedZoneArgEntry);
        boolean stateUpdated = !state.update(newArgs, ctx).isEmpty();

        assertThat(stateUpdated).isTrue();
        assertThat(state.getArguments()).containsExactlyInAnyOrderEntriesOf(
                Map.of(
                        ENTITY_ID_LATITUDE_ARGUMENT_KEY, latitudeArgEntry,
                        ENTITY_ID_LONGITUDE_ARGUMENT_KEY, longitudeArgEntry,
                        "allowedZones", geofencingAllowedZoneArgEntry
                )
        );
    }

    @Test
    void testUpdateStateWithInvalidArgumentTypeForLatitudeArgument() {
        assertThatThrownBy(() -> state.update(Map.of(ENTITY_ID_LATITUDE_ARGUMENT_KEY, geofencingAllowedZoneArgEntry), ctx))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unsupported argument entry type for latitude argument: GEOFENCING. Only SINGLE_VALUE type is allowed.");
    }

    @Test
    void testUpdateStateWithInvalidArgumentTypeForLongitudeArgument() {
        assertThatThrownBy(() -> state.update(Map.of(ENTITY_ID_LONGITUDE_ARGUMENT_KEY, geofencingAllowedZoneArgEntry), ctx))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unsupported argument entry type for longitude argument: GEOFENCING. Only SINGLE_VALUE type is allowed.");
    }

    @Test
    void testUpdateStateWithInvalidArgumentTypeForGeofencingArgument() {
        assertThatThrownBy(() -> state.update(Map.of("someArgumentName", latitudeArgEntry), ctx))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unsupported argument entry type for someArgumentName argument: SINGLE_VALUE. Only GEOFENCING type is allowed.");
    }

    @Test
    void testUpdateStateWhenUpdateExistingSingleValueArgumentEntry() {
        state.arguments = new HashMap<>(Map.of(ENTITY_ID_LATITUDE_ARGUMENT_KEY, latitudeArgEntry));

        SingleValueArgumentEntry newArgEntry = new SingleValueArgumentEntry(System.currentTimeMillis(), new DoubleDataEntry("latitude", 50.4760), 190L);
        Map<String, ArgumentEntry> newArgs = Map.of("latitude", newArgEntry);
        boolean stateUpdated = !state.update(newArgs, ctx).isEmpty();

        assertThat(stateUpdated).isTrue();
        assertThat(state.getArguments()).isEqualTo(newArgs);
    }

    @Test
    void testUpdateStateWhenUpdateExistingGeofencingValueArgumentEntryWithTheSameValue() {
        state.arguments = new HashMap<>(Map.of("allowedZones", geofencingAllowedZoneArgEntry));

        Map<String, ArgumentEntry> newArgs = Map.of("allowedZones", geofencingAllowedZoneArgEntry);

        boolean stateUpdated = !state.update(newArgs, ctx).isEmpty();

        assertThat(stateUpdated).isFalse();
        assertThat(state.getArguments()).isEqualTo(newArgs);
    }

    @Test
    void testUpdateStateWhenUpdateExistingSingleValueArgumentEntryWithValueOfAnotherType() {
        state.arguments = new HashMap<>(Map.of(ENTITY_ID_LATITUDE_ARGUMENT_KEY, latitudeArgEntry));

        assertThatThrownBy(() -> state.update(Map.of(ENTITY_ID_LATITUDE_ARGUMENT_KEY, geofencingAllowedZoneArgEntry), ctx))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unsupported argument entry type for single value argument entry: GEOFENCING");
    }


    @Test
    void testUpdateStateWhenUpdateExistingGeofencingValueArgumentEntryWithValueOfAnotherType() {
        state.arguments = new HashMap<>(Map.of("allowedZones", geofencingAllowedZoneArgEntry));

        assertThatThrownBy(() -> state.update(Map.of("allowedZones", latitudeArgEntry), ctx))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unsupported argument entry type for geofencing argument entry: SINGLE_VALUE");
    }

    @Test
    void testIsReadyWhenNotAllArgPresent() {
        assertThat(state.isReady()).isFalse();
        assertThat(state.getReadinessStatus().errorMsg()).contains(state.getRequiredArguments());
    }

    @Test
    void testIsReadyWhenAllArgPresent() {
        state.update(Map.of(
                ENTITY_ID_LATITUDE_ARGUMENT_KEY, latitudeArgEntry,
                ENTITY_ID_LONGITUDE_ARGUMENT_KEY, longitudeArgEntry,
                "allowedZones", geofencingAllowedZoneArgEntry,
                "restrictedZones", geofencingRestrictedZoneArgEntry
        ), ctx);
        assertThat(state.isReady()).isTrue();
        assertThat(state.getReadinessStatus().errorMsg()).isNull();
    }

    @Test
    void testIsReadyWhenEmptyEntryPresents() {
        state.update(Map.of(
                ENTITY_ID_LATITUDE_ARGUMENT_KEY, latitudeArgEntry,
                ENTITY_ID_LONGITUDE_ARGUMENT_KEY, longitudeArgEntry,
                "allowedZones", geofencingAllowedZoneArgEntry,
                "restrictedZones", new GeofencingArgumentEntry(Collections.emptyMap())
        ), ctx);
        assertThat(state.isReady()).isFalse();
        assertThat(state.getReadinessStatus().errorMsg()).contains("restrictedZones");
    }

    @Test
    void testPerformCalculation() throws ExecutionException, InterruptedException {
        state.arguments = new HashMap<>(Map.of(
                ENTITY_ID_LATITUDE_ARGUMENT_KEY, latitudeArgEntry,
                ENTITY_ID_LONGITUDE_ARGUMENT_KEY, longitudeArgEntry,
                "allowedZones", geofencingAllowedZoneArgEntry,
                "restrictedZones", geofencingRestrictedZoneArgEntry
        ));

        Output output = ctx.getOutput();
        var configuration = (GeofencingCalculatedFieldConfiguration) ctx.getCalculatedField().getConfiguration();

        when(relationService.saveRelationAsync(any(), any())).thenReturn(Futures.immediateFuture(true));
        when(relationService.deleteRelationAsync(any(), any())).thenReturn(Futures.immediateFuture(true));

        TelemetryCalculatedFieldResult result = performCalculation();

        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(output.getType());
        assertThat(result.getScope()).isEqualTo(output.getScope());
        assertThat(result.getResult().get("values")).isEqualTo(
                JacksonUtil.newObjectNode()
                        .put("allowedZonesStatus", "INSIDE")
                        .put("restrictedZonesStatus", "OUTSIDE")
        );

        SingleValueArgumentEntry newLatitude = new SingleValueArgumentEntry(System.currentTimeMillis(), new DoubleDataEntry("latitude", 50.4760), 146L);
        SingleValueArgumentEntry newLongitude = new SingleValueArgumentEntry(System.currentTimeMillis(), new DoubleDataEntry("longitude", 30.5110), 166L);

        // move the device to new coordinates → leaves allowed, enters restricted
        state.update(Map.of(ENTITY_ID_LATITUDE_ARGUMENT_KEY, newLatitude, ENTITY_ID_LONGITUDE_ARGUMENT_KEY, newLongitude), ctx);

        TelemetryCalculatedFieldResult result2 = performCalculation();

        assertThat(result2).isNotNull();
        assertThat(result2.getType()).isEqualTo(output.getType());
        assertThat(result2.getScope()).isEqualTo(output.getScope());
        assertThat(result2.getResult().get("values")).isEqualTo(
                JacksonUtil.newObjectNode()
                        .put("allowedZonesEvent", "LEFT")
                        .put("allowedZonesStatus", "OUTSIDE")
                        .put("restrictedZonesEvent", "ENTERED")
                        .put("restrictedZonesStatus", "INSIDE")
        );

        // Check relations are created and deleted correctly for both iterations.
        ArgumentCaptor<EntityRelation> saveCaptor = ArgumentCaptor.forClass(EntityRelation.class);
        verify(relationService, times(2)).saveRelationAsync(eq(ctx.getTenantId()), saveCaptor.capture());
        List<EntityRelation> saveValues = saveCaptor.getAllValues();
        assertThat(saveValues).hasSize(2);

        EntityRelation relationFromFirstIteration = saveValues.get(0);
        assertThat(relationFromFirstIteration.getTo()).isEqualTo(ctx.getEntityId());
        assertThat(relationFromFirstIteration.getFrom()).isEqualTo(ZONE_1_ID);
        assertThat(relationFromFirstIteration.getType()).isEqualTo("CurrentZone");

        EntityRelation relationFromSecondIteration = saveValues.get(1);
        assertThat(relationFromSecondIteration.getTo()).isEqualTo(ctx.getEntityId());
        assertThat(relationFromSecondIteration.getFrom()).isEqualTo(ZONE_2_ID);
        assertThat(relationFromSecondIteration.getType()).isEqualTo("CurrentZone");

        ArgumentCaptor<EntityRelation> deleteCaptor = ArgumentCaptor.forClass(EntityRelation.class);
        verify(relationService, times(2)).deleteRelationAsync(eq(ctx.getTenantId()), deleteCaptor.capture());
        List<EntityRelation> deleteValues = deleteCaptor.getAllValues();
        assertThat(deleteValues).hasSize(2);

        EntityRelation deleteRelationFromFirstIteration = deleteValues.get(0);
        assertThat(deleteRelationFromFirstIteration.getFrom()).isEqualTo(ZONE_2_ID);
        assertThat(deleteRelationFromFirstIteration.getTo()).isEqualTo(ctx.getEntityId());

        EntityRelation deleteRelationFromSecondIteration = deleteValues.get(1);
        assertThat(deleteRelationFromSecondIteration.getFrom()).isEqualTo(ZONE_1_ID);
        assertThat(deleteRelationFromSecondIteration.getTo()).isEqualTo(ctx.getEntityId());
    }

    @Test
    void testPerformCalculationWithOnlyTransitionEventsReportingStrategy() throws ExecutionException, InterruptedException {
        state.arguments = new HashMap<>(Map.of(
                ENTITY_ID_LATITUDE_ARGUMENT_KEY, latitudeArgEntry,
                ENTITY_ID_LONGITUDE_ARGUMENT_KEY, longitudeArgEntry,
                "allowedZones", geofencingAllowedZoneArgEntry,
                "restrictedZones", geofencingRestrictedZoneArgEntry
        ));

        Output output = ctx.getOutput();

        var calculatedFieldConfig = getCalculatedFieldConfig(GeofencingReportStrategy.REPORT_TRANSITION_EVENTS_ONLY);

        ctx.setCalculatedField(getCalculatedField(calculatedFieldConfig));
        ctx.init();

        var configuration = (GeofencingCalculatedFieldConfiguration) ctx.getCalculatedField().getConfiguration();

        when(relationService.saveRelationAsync(any(), any())).thenReturn(Futures.immediateFuture(true));
        when(relationService.deleteRelationAsync(any(), any())).thenReturn(Futures.immediateFuture(true));

        TelemetryCalculatedFieldResult result = performCalculation();

        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(output.getType());
        assertThat(result.getScope()).isEqualTo(output.getScope());
        assertThat(result.getResult().get("values")).isEmpty();

        SingleValueArgumentEntry newLatitude = new SingleValueArgumentEntry(System.currentTimeMillis(), new DoubleDataEntry("latitude", 50.4760), 146L);
        SingleValueArgumentEntry newLongitude = new SingleValueArgumentEntry(System.currentTimeMillis(), new DoubleDataEntry("longitude", 30.5110), 166L);

        // move the device to new coordinates → leaves allowed, enters restricted
        state.update(Map.of(ENTITY_ID_LATITUDE_ARGUMENT_KEY, newLatitude, ENTITY_ID_LONGITUDE_ARGUMENT_KEY, newLongitude), ctx);

        TelemetryCalculatedFieldResult result2 = performCalculation();

        assertThat(result2).isNotNull();
        assertThat(result2.getType()).isEqualTo(output.getType());
        assertThat(result2.getScope()).isEqualTo(output.getScope());
        assertThat(result2.getResult().get("values")).isEqualTo(
                JacksonUtil.newObjectNode()
                        .put("allowedZonesEvent", "LEFT")
                        .put("restrictedZonesEvent", "ENTERED")
        );

        // Check relations are created and deleted correctly for both iterations.
        ArgumentCaptor<EntityRelation> saveCaptor = ArgumentCaptor.forClass(EntityRelation.class);
        verify(relationService, times(2)).saveRelationAsync(eq(ctx.getTenantId()), saveCaptor.capture());
        List<EntityRelation> saveValues = saveCaptor.getAllValues();
        assertThat(saveValues).hasSize(2);

        EntityRelation relationFromFirstIteration = saveValues.get(0);
        assertThat(relationFromFirstIteration.getTo()).isEqualTo(ctx.getEntityId());
        assertThat(relationFromFirstIteration.getFrom()).isEqualTo(ZONE_1_ID);
        assertThat(relationFromFirstIteration.getType()).isEqualTo("CurrentZone");

        EntityRelation relationFromSecondIteration = saveValues.get(1);
        assertThat(relationFromSecondIteration.getTo()).isEqualTo(ctx.getEntityId());
        assertThat(relationFromSecondIteration.getFrom()).isEqualTo(ZONE_2_ID);
        assertThat(relationFromSecondIteration.getType()).isEqualTo("CurrentZone");

        ArgumentCaptor<EntityRelation> deleteCaptor = ArgumentCaptor.forClass(EntityRelation.class);
        verify(relationService, times(2)).deleteRelationAsync(eq(ctx.getTenantId()), deleteCaptor.capture());
        List<EntityRelation> deleteValues = deleteCaptor.getAllValues();
        assertThat(deleteValues).hasSize(2);

        EntityRelation deleteRelationFromFirstIteration = deleteValues.get(0);
        assertThat(deleteRelationFromFirstIteration.getFrom()).isEqualTo(ZONE_2_ID);
        assertThat(deleteRelationFromFirstIteration.getTo()).isEqualTo(ctx.getEntityId());

        EntityRelation deleteRelationFromSecondIteration = deleteValues.get(1);
        assertThat(deleteRelationFromSecondIteration.getFrom()).isEqualTo(ZONE_1_ID);
        assertThat(deleteRelationFromSecondIteration.getTo()).isEqualTo(ctx.getEntityId());
    }

    @Test
    void testPerformCalculationWithOnlyPresenceStatusReportingStrategy() throws ExecutionException, InterruptedException {
        state.arguments = new HashMap<>(Map.of(
                ENTITY_ID_LATITUDE_ARGUMENT_KEY, latitudeArgEntry,
                ENTITY_ID_LONGITUDE_ARGUMENT_KEY, longitudeArgEntry,
                "allowedZones", geofencingAllowedZoneArgEntry,
                "restrictedZones", geofencingRestrictedZoneArgEntry
        ));

        Output output = ctx.getOutput();

        var calculatedFieldConfig = getCalculatedFieldConfig(GeofencingReportStrategy.REPORT_PRESENCE_STATUS_ONLY);

        ctx.setCalculatedField(getCalculatedField(calculatedFieldConfig));
        ctx.init();

        var configuration = (GeofencingCalculatedFieldConfiguration) ctx.getCalculatedField().getConfiguration();

        when(relationService.saveRelationAsync(any(), any())).thenReturn(Futures.immediateFuture(true));
        when(relationService.deleteRelationAsync(any(), any())).thenReturn(Futures.immediateFuture(true));

        TelemetryCalculatedFieldResult result = performCalculation();

        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(output.getType());
        assertThat(result.getScope()).isEqualTo(output.getScope());
        assertThat(result.getResult().get("values")).isEqualTo(
                JacksonUtil.newObjectNode()
                        .put("allowedZonesStatus", "INSIDE")
                        .put("restrictedZonesStatus", "OUTSIDE")
        );

        SingleValueArgumentEntry newLatitude = new SingleValueArgumentEntry(System.currentTimeMillis(), new DoubleDataEntry("latitude", 50.4760), 146L);
        SingleValueArgumentEntry newLongitude = new SingleValueArgumentEntry(System.currentTimeMillis(), new DoubleDataEntry("longitude", 30.5110), 166L);

        // move the device to new coordinates → leaves allowed, enters restricted
        state.update(Map.of(ENTITY_ID_LATITUDE_ARGUMENT_KEY, newLatitude, ENTITY_ID_LONGITUDE_ARGUMENT_KEY, newLongitude), ctx);

        TelemetryCalculatedFieldResult result2 = performCalculation();

        assertThat(result2).isNotNull();
        assertThat(result2.getType()).isEqualTo(output.getType());
        assertThat(result2.getScope()).isEqualTo(output.getScope());
        assertThat(result2.getResult().get("values")).isEqualTo(
                JacksonUtil.newObjectNode()
                        .put("allowedZonesStatus", "OUTSIDE")
                        .put("restrictedZonesStatus", "INSIDE")
        );

        // Check relations are created and deleted correctly for both iterations.
        ArgumentCaptor<EntityRelation> saveCaptor = ArgumentCaptor.forClass(EntityRelation.class);
        verify(relationService, times(2)).saveRelationAsync(eq(ctx.getTenantId()), saveCaptor.capture());
        List<EntityRelation> saveValues = saveCaptor.getAllValues();
        assertThat(saveValues).hasSize(2);

        EntityRelation relationFromFirstIteration = saveValues.get(0);
        assertThat(relationFromFirstIteration.getTo()).isEqualTo(ctx.getEntityId());
        assertThat(relationFromFirstIteration.getFrom()).isEqualTo(ZONE_1_ID);
        assertThat(relationFromFirstIteration.getType()).isEqualTo("CurrentZone");

        EntityRelation relationFromSecondIteration = saveValues.get(1);
        assertThat(relationFromSecondIteration.getTo()).isEqualTo(ctx.getEntityId());
        assertThat(relationFromSecondIteration.getFrom()).isEqualTo(ZONE_2_ID);
        assertThat(relationFromSecondIteration.getType()).isEqualTo("CurrentZone");

        ArgumentCaptor<EntityRelation> deleteCaptor = ArgumentCaptor.forClass(EntityRelation.class);
        verify(relationService, times(2)).deleteRelationAsync(eq(ctx.getTenantId()), deleteCaptor.capture());
        List<EntityRelation> deleteValues = deleteCaptor.getAllValues();
        assertThat(deleteValues).hasSize(2);

        EntityRelation deleteRelationFromFirstIteration = deleteValues.get(0);
        assertThat(deleteRelationFromFirstIteration.getFrom()).isEqualTo(ZONE_2_ID);
        assertThat(deleteRelationFromFirstIteration.getTo()).isEqualTo(ctx.getEntityId());

        EntityRelation deleteRelationFromSecondIteration = deleteValues.get(1);
        assertThat(deleteRelationFromSecondIteration.getFrom()).isEqualTo(ZONE_1_ID);
        assertThat(deleteRelationFromSecondIteration.getTo()).isEqualTo(ctx.getEntityId());
    }

    private CalculatedField getCalculatedField() {
        return getCalculatedField(getCalculatedFieldConfig(REPORT_TRANSITION_EVENTS_AND_PRESENCE_STATUS));
    }

    private CalculatedField getCalculatedField(CalculatedFieldConfiguration configuration) {
        CalculatedField calculatedField = new CalculatedField();
        calculatedField.setTenantId(TENANT_ID);
        calculatedField.setEntityId(DEVICE_ID);
        calculatedField.setType(CalculatedFieldType.GEOFENCING);
        calculatedField.setName("Test Geofencing Calculated Field");
        calculatedField.setConfigurationVersion(1);
        calculatedField.setConfiguration(configuration);
        calculatedField.setVersion(1L);
        return calculatedField;
    }

    private CalculatedFieldConfiguration getCalculatedFieldConfig(GeofencingReportStrategy reportStrategy) {
        var config = new GeofencingCalculatedFieldConfiguration();

        EntityCoordinates entityCoordinates = new EntityCoordinates("latitude", "longitude");
        config.setEntityCoordinates(entityCoordinates);

        ZoneGroupConfiguration allowedZonesGroup = new ZoneGroupConfiguration("zone", reportStrategy, true);
        var allowedZoneDynamicSourceConfiguration = new RelationPathQueryDynamicSourceConfiguration();
        allowedZoneDynamicSourceConfiguration.setLevels(List.of(new RelationPathLevel(EntitySearchDirection.TO, "AllowedZone")));
        allowedZonesGroup.setRefDynamicSourceConfiguration(allowedZoneDynamicSourceConfiguration);
        allowedZonesGroup.setRelationType("CurrentZone");
        allowedZonesGroup.setDirection(EntitySearchDirection.TO);

        ZoneGroupConfiguration restrictedZonesGroup = new ZoneGroupConfiguration("zone", reportStrategy, true);
        var restrictedZoneDynamicSourceConfiguration = new RelationPathQueryDynamicSourceConfiguration();
        restrictedZoneDynamicSourceConfiguration.setLevels(List.of(new RelationPathLevel(EntitySearchDirection.TO, "RestrictedZone")));
        restrictedZonesGroup.setRefDynamicSourceConfiguration(restrictedZoneDynamicSourceConfiguration);
        restrictedZonesGroup.setRelationType("CurrentZone");
        restrictedZonesGroup.setDirection(EntitySearchDirection.TO);

        config.setZoneGroups(Map.of("allowedZones", allowedZonesGroup, "restrictedZones", restrictedZonesGroup));

        config.setOutput(new TimeSeriesOutput());
        return config;
    }

    private TelemetryCalculatedFieldResult performCalculation() throws InterruptedException, ExecutionException {
        return (TelemetryCalculatedFieldResult) state.performCalculation(Collections.emptyMap(), ctx).get();
    }

}
