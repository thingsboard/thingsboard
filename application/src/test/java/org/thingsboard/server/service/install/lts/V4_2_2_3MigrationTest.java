// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.install.lts;

import com.google.common.util.concurrent.Futures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.WidgetTypeId;
import org.thingsboard.server.common.data.id.WidgetsBundleId;
import org.thingsboard.server.common.data.iot_hub.IotHubInstalledItem;
import org.thingsboard.server.common.data.iot_hub.SolutionTemplateInstalledItemDescriptor;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.BooleanDataEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.widget.WidgetTypeDetails;
import org.thingsboard.server.common.data.widget.WidgetsBundle;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.iot_hub.IotHubInstalledItemService;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.dao.widget.WidgetTypeService;
import org.thingsboard.server.dao.widget.WidgetsBundleService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class V4_2_2_3MigrationTest {

    @Mock
    private WidgetsBundleService widgetsBundleService;

    @Mock
    private WidgetTypeService widgetTypeService;

    @Mock
    private TenantService tenantService;

    @Mock
    private AttributesService attributesService;

    @Mock
    private IotHubInstalledItemService iotHubInstalledItemService;

    @InjectMocks
    private V4_2_2_3Migration migration;

    @Test
    void versionIs4223() {
        assertEquals("4.2.2.3", migration.getVersion());
    }

    @Test
    void deprecatesTypesThenDeletesBundleEntityOnly() {
        WidgetsBundle bundle = new WidgetsBundle();
        bundle.setId(new WidgetsBundleId(UUID.randomUUID()));
        bundle.setAlias("air_quality");

        WidgetTypeDetails fresh = new WidgetTypeDetails();
        fresh.setId(new WidgetTypeId(UUID.randomUUID()));
        fresh.setDeprecated(false);
        WidgetTypeDetails already = new WidgetTypeDetails();
        already.setId(new WidgetTypeId(UUID.randomUUID()));
        already.setDeprecated(true);

        when(widgetsBundleService.findWidgetsBundleByTenantIdAndAlias(TenantId.SYS_TENANT_ID, "air_quality")).thenReturn(bundle);
        when(widgetTypeService.findWidgetTypesDetailsByWidgetsBundleId(TenantId.SYS_TENANT_ID, bundle.getId())).thenReturn(List.of(fresh, already));
        when(widgetsBundleService.findWidgetsBundleByTenantIdAndAlias(TenantId.SYS_TENANT_ID, "indoor_environment")).thenReturn(null);
        when(widgetsBundleService.findWidgetsBundleByTenantIdAndAlias(TenantId.SYS_TENANT_ID, "industrial_widgets")).thenReturn(null);
        when(widgetsBundleService.findWidgetsBundleByTenantIdAndAlias(TenantId.SYS_TENANT_ID, "outdoor_environment")).thenReturn(null);
        // No tenants -> the legacy solution-template migration step in apply() is a no-op.
        when(tenantService.findTenantsIds(any())).thenReturn(new PageData<>(List.of(), 0, 0, false));

        migration.apply();

        // only the previously non-deprecated type is re-saved, now deprecated
        verify(widgetTypeService).saveWidgetType(argThat(WidgetTypeDetails::isDeprecated));
        // widget types are NOT deleted
        verify(widgetTypeService, never()).deleteWidgetTypesByBundleId(any(), any());
        // only the bundle entity is removed
        verify(widgetsBundleService).deleteWidgetsBundle(TenantId.SYS_TENANT_ID, bundle.getId());
    }

    @Test
    void absentBundleIsNoOp() {
        when(widgetsBundleService.findWidgetsBundleByTenantIdAndAlias(any(), any())).thenReturn(null);
        when(tenantService.findTenantsIds(any())).thenReturn(new PageData<>(List.of(), 0, 0, false));

        migration.apply();

        verify(widgetTypeService, never()).saveWidgetType(any());
        verify(widgetsBundleService, never()).deleteWidgetsBundle(any(), any());
    }

    // --- Legacy solution-template migration ---

    // Mappings baked into V4_2_2_3Migration.LEGACY_SOLUTION_TEMPLATES.
    private static final String TEMPERATURE_LEGACY_ID = "temperature_sensors";
    private static final UUID TEMPERATURE_ITEM_ID = UUID.fromString("7395c99f-235f-499e-b1e5-c9117a0b7e07");
    private static final UUID TEMPERATURE_ITEM_VERSION_ID = UUID.fromString("922bd1d0-68e9-11f1-992c-0f4d95fca092");
    private static final String TEMPERATURE_ITEM_NAME = "Temperature & Humidity sensors";

    private static final String WATER_METERING_LEGACY_ID = "water_metering";
    private static final UUID WATER_METERING_ITEM_ID = UUID.fromString("77b3cff4-9e78-44b5-872e-57b7f400f938");

    @Test
    void migratesLegacySolutionTemplateToIotHubAndCleansUpAttributes() {
        TenantId tenantId = new TenantId(UUID.randomUUID());
        UUID createdDeviceId = UUID.randomUUID();
        UUID dashboardGroupUuid = UUID.randomUUID();
        UUID dashboardUuid = UUID.randomUUID();

        String entitiesJson = "[{\"entityType\":\"DEVICE\",\"id\":\"" + createdDeviceId + "\"}]";
        String instructionsJson = "{" +
                "\"dashboardGroupId\":{\"entityType\":\"ENTITY_GROUP\",\"id\":\"" + dashboardGroupUuid + "\"}," +
                "\"dashboardId\":{\"entityType\":\"DASHBOARD\",\"id\":\"" + dashboardUuid + "\"}," +
                "\"details\":\"Some installation details\"}";

        stubSingleTenant(tenantId);
        stubAttributes(tenantId, List.of(
                booleanEntry(TEMPERATURE_LEGACY_ID + "_status", true),
                stringEntry(TEMPERATURE_LEGACY_ID + "_entities", entitiesJson),
                stringEntry(TEMPERATURE_LEGACY_ID + "_instructions", instructionsJson)));
        when(iotHubInstalledItemService.findInstalledItemIdsByTenantIdAndItemIdIn(eq(tenantId), any()))
                .thenReturn(List.of());
        when(attributesService.removeAll(eq(tenantId), eq(tenantId), eq(AttributeScope.SERVER_SCOPE), any()))
                .thenReturn(Futures.immediateFuture(List.of()));

        migration.apply();

        ArgumentCaptor<IotHubInstalledItem> itemCaptor = ArgumentCaptor.forClass(IotHubInstalledItem.class);
        verify(iotHubInstalledItemService).save(eq(tenantId), itemCaptor.capture());
        IotHubInstalledItem saved = itemCaptor.getValue();
        assertEquals(tenantId, saved.getTenantId());
        assertEquals(TEMPERATURE_ITEM_ID, saved.getItemId());
        assertEquals(TEMPERATURE_ITEM_VERSION_ID, saved.getItemVersionId());
        assertEquals(TEMPERATURE_ITEM_NAME, saved.getItemName());
        assertEquals("SOLUTION_TEMPLATE", saved.getItemType());
        assertEquals("1.0.0", saved.getVersion());

        assertNotNull(saved.getDescriptor());
        SolutionTemplateInstalledItemDescriptor descriptor =
                (SolutionTemplateInstalledItemDescriptor) saved.getDescriptor();
        // temperature_sensors has empty tenantTelemetryKeys in the baked mapping
        assertEquals(List.of(), descriptor.getTenantTelemetryKeys());
        assertEquals(List.of(), descriptor.getTenantAttributeKeys());
        assertFalse(descriptor.isMainDashboardPublic());
        assertEquals(List.<EntityId>of(new DeviceId(createdDeviceId)), descriptor.getCreatedEntityIds());
        assertEquals(new EntityGroupId(dashboardGroupUuid), descriptor.getDashboardGroupId());
        assertEquals(new DashboardId(dashboardUuid), descriptor.getDashboardId());
        assertEquals("Some installation details", descriptor.getDetails());

        // consume-once cleanup: all three legacy keys for the migrated template are removed
        ArgumentCaptor<List<String>> keysCaptor = ArgumentCaptor.forClass(List.class);
        verify(attributesService).removeAll(eq(tenantId), eq(tenantId), eq(AttributeScope.SERVER_SCOPE), keysCaptor.capture());
        List<String> removedKeys = keysCaptor.getValue();
        assertTrue(removedKeys.contains(TEMPERATURE_LEGACY_ID + "_status"));
        assertTrue(removedKeys.contains(TEMPERATURE_LEGACY_ID + "_entities"));
        assertTrue(removedKeys.contains(TEMPERATURE_LEGACY_ID + "_instructions"));
    }

    @Test
    void doesNotMigrateWhenStatusFlagIsFalseOrAbsent() {
        TenantId tenantId = new TenantId(UUID.randomUUID());

        stubSingleTenant(tenantId);
        // temperature_sensors: status=false (gating) ; water_metering: status absent (only entities present)
        stubAttributes(tenantId, List.of(
                booleanEntry(TEMPERATURE_LEGACY_ID + "_status", false),
                stringEntry(TEMPERATURE_LEGACY_ID + "_entities", "[]"),
                stringEntry(WATER_METERING_LEGACY_ID + "_entities", "[]")));

        migration.apply();

        verify(iotHubInstalledItemService, never()).save(any(), any());
        // nothing migrated -> no attribute cleanup at all
        verify(attributesService, never()).removeAll(any(), any(), any(), any());
    }

    @Test
    void skipsDuplicateSaveButStillCleansUpWhenAlreadyInstalled() {
        TenantId tenantId = new TenantId(UUID.randomUUID());

        stubSingleTenant(tenantId);
        stubAttributes(tenantId, List.of(
                booleanEntry(TEMPERATURE_LEGACY_ID + "_status", true),
                stringEntry(TEMPERATURE_LEGACY_ID + "_entities", "[]"),
                stringEntry(TEMPERATURE_LEGACY_ID + "_instructions", "{}")));
        // the template's itemId is reported as already installed
        when(iotHubInstalledItemService.findInstalledItemIdsByTenantIdAndItemIdIn(eq(tenantId), any()))
                .thenReturn(List.of(TEMPERATURE_ITEM_ID));
        when(attributesService.removeAll(eq(tenantId), eq(tenantId), eq(AttributeScope.SERVER_SCOPE), any()))
                .thenReturn(Futures.immediateFuture(List.of()));

        migration.apply();

        // no duplicate save
        verify(iotHubInstalledItemService, never()).save(any(), any());
        // but legacy attributes are still consumed
        ArgumentCaptor<List<String>> keysCaptor = ArgumentCaptor.forClass(List.class);
        verify(attributesService).removeAll(eq(tenantId), eq(tenantId), eq(AttributeScope.SERVER_SCOPE), keysCaptor.capture());
        List<String> removedKeys = keysCaptor.getValue();
        assertTrue(removedKeys.contains(TEMPERATURE_LEGACY_ID + "_status"));
        assertTrue(removedKeys.contains(TEMPERATURE_LEGACY_ID + "_entities"));
        assertTrue(removedKeys.contains(TEMPERATURE_LEGACY_ID + "_instructions"));
    }

    // Stubs a single tenant for the legacy solution-template migration loop, while keeping the
    // widget-bundle deprecation step a no-op (no obsolete bundles present).
    private void stubSingleTenant(TenantId tenantId) {
        when(widgetsBundleService.findWidgetsBundleByTenantIdAndAlias(any(), any())).thenReturn(null);
        when(tenantService.findTenantsIds(any()))
                .thenReturn(new PageData<>(List.of(tenantId), 1, 1, false));
    }

    private void stubAttributes(TenantId tenantId, List<AttributeKvEntry> entries) {
        when(attributesService.find(eq(tenantId), eq(tenantId), eq(AttributeScope.SERVER_SCOPE), any(Collection.class)))
                .thenReturn(Futures.immediateFuture(new ArrayList<>(entries)));
    }

    private static AttributeKvEntry booleanEntry(String key, boolean value) {
        return new BaseAttributeKvEntry(new BooleanDataEntry(key, value), System.currentTimeMillis());
    }

    private static AttributeKvEntry stringEntry(String key, String value) {
        return new BaseAttributeKvEntry(new StringDataEntry(key, value), System.currentTimeMillis());
    }
}
