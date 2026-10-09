// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.install.lts;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.iot_hub.IotHubInstalledItem;
import org.thingsboard.server.common.data.iot_hub.SolutionTemplateInstalledItemDescriptor;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.common.data.widget.WidgetTypeDetails;
import org.thingsboard.server.common.data.widget.WidgetsBundle;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.iot_hub.IotHubInstalledItemService;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.dao.widget.WidgetTypeService;
import org.thingsboard.server.dao.widget.WidgetsBundleService;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Component
@TbCoreComponent
@RequiredArgsConstructor
public class V4_2_2_3Migration implements LtsMigration {

    // Match on the bundle ALIAS, not the JSON filename stem.
    // The "industrial_widgets" bundle shipped in a misspelled file (industial_widgets.json),
    // but the alias inside it is correctly spelled.
    private static final List<String> OBSOLETE_BUNDLE_ALIASES = List.of(
            "air_quality", "indoor_environment", "industrial_widgets", "outdoor_environment");

    private static final int DEFAULT_PAGE_SIZE = 1024;

    private final WidgetsBundleService widgetsBundleService;
    private final WidgetTypeService widgetTypeService;
    private final TenantService tenantService;
    private final AttributesService attributesService;
    private final IotHubInstalledItemService iotHubInstalledItemService;

    @Override
    public String getVersion() {
        return "4.2.2.3";
    }

    @Override
    public void apply() {
        for (String alias : OBSOLETE_BUNDLE_ALIASES) {
            deprecateTypesAndDeleteBundle(alias);
        }
        migrateLegacySolutionTemplatesToIotHub();
    }

    // Marks the bundle's widget types as deprecated (keeping the type entities) and deletes ONLY the bundle entity.
    private void deprecateTypesAndDeleteBundle(String alias) {
        WidgetsBundle bundle = widgetsBundleService.findWidgetsBundleByTenantIdAndAlias(TenantId.SYS_TENANT_ID, alias);
        if (bundle == null) {
            return; // already removed — idempotent
        }
        List<WidgetTypeDetails> types = widgetTypeService.findWidgetTypesDetailsByWidgetsBundleId(TenantId.SYS_TENANT_ID, bundle.getId());
        for (WidgetTypeDetails type : types) {
            if (!type.isDeprecated()) {
                type.setDeprecated(true);
                widgetTypeService.saveWidgetType(type);
            }
        }
        widgetsBundleService.deleteWidgetsBundle(TenantId.SYS_TENANT_ID, bundle.getId());
        log.info("Deprecated {} widget type(s) and removed obsolete system widget bundle: {}", types.size(), alias);
    }

    // --- Legacy solution-template migration ---

    private record LegacySolutionTemplate(UUID itemId,
                                          UUID itemVersionId,
                                          String itemName,
                                          String version,
                                          List<String> tenantTelemetryKeys) {}

    // Snapshot of legacy PE solution-template ids (pre-IoT Hub) mapped to the
    // current IoT Hub item descriptors (minTbVersion: 420).
    // itemVersionId / itemName / version were resolved once via
    // https://iot-hub.thingsboard.io/api/items/{itemId}/published and baked
    // in so the upgrade is fully self-contained (no network call at runtime).
    private static final Map<String, LegacySolutionTemplate> LEGACY_SOLUTION_TEMPLATES = Map.ofEntries(
            Map.entry("temperature_sensors", new LegacySolutionTemplate(
                    UUID.fromString("7395c99f-235f-499e-b1e5-c9117a0b7e07"),
                    UUID.fromString("922bd1d0-68e9-11f1-992c-0f4d95fca092"),
                    "Temperature & Humidity sensors", "1.0.0", List.of())),
            Map.entry("smart_office", new LegacySolutionTemplate(
                    UUID.fromString("bd2c54df-4b34-4cb6-9f2f-596f0bad4cd4"),
                    UUID.fromString("9081cb50-68e9-11f1-992c-0f4d95fca092"),
                    "Smart office", "1.0.0", List.of())),
            Map.entry("fleet_tracking", new LegacySolutionTemplate(
                    UUID.fromString("51e81996-d9b7-4972-9716-bf2854819c7e"),
                    UUID.fromString("8e1d4e70-68e9-11f1-992c-0f4d95fca092"),
                    "Fleet tracking", "1.0.0", List.of())),
            Map.entry("fuel_level_monitoring", new LegacySolutionTemplate(
                    UUID.fromString("5765a34c-490e-4c98-8ba5-17036deaf5ce"),
                    UUID.fromString("8ec06c40-68e9-11f1-992c-0f4d95fca092"),
                    "Fuel level monitoring", "1.0.0", List.of())),
            Map.entry("swimming_pool_scada_system", new LegacySolutionTemplate(
                    UUID.fromString("f476dc19-e38c-46af-9818-316d4c57c988"),
                    UUID.fromString("91bdcdc0-68e9-11f1-992c-0f4d95fca092"),
                    "Swimming pool SCADA system", "1.0.0", List.of())),
            Map.entry("scada_drilling_system", new LegacySolutionTemplate(
                    UUID.fromString("0f6cad80-cb62-4eb6-ba9d-2372e3bc329e"),
                    UUID.fromString("8f17b400-68e9-11f1-992c-0f4d95fca092"),
                    "SCADA Oil & Gas drilling system", "1.0.0", List.of())),
            Map.entry("scada_energy_management", new LegacySolutionTemplate(
                    UUID.fromString("a0344f65-1947-4a75-afe3-a74c8ae39a8f"),
                    UUID.fromString("8f6ba060-68e9-11f1-992c-0f4d95fca092"),
                    "SCADA Energy management", "1.0.0", List.of())),
            Map.entry("air_quality_index", new LegacySolutionTemplate(
                    UUID.fromString("ff97f8cd-71b3-49fe-9bcf-f0e87340efbe"),
                    UUID.fromString("8cc78270-68e9-11f1-992c-0f4d95fca092"),
                    "Air quality monitoring", "1.0.0", List.of())),
            Map.entry("water_metering", new LegacySolutionTemplate(
                    UUID.fromString("77b3cff4-9e78-44b5-872e-57b7f400f938"),
                    UUID.fromString("9338adf0-68e9-11f1-992c-0f4d95fca092"),
                    "Water metering", "1.0.0", List.of("dailyConsumption", "weeklyConsumption"))),
            Map.entry("smart_retail", new LegacySolutionTemplate(
                    UUID.fromString("029f443a-8e26-41c1-8382-a5f500f8c104"),
                    UUID.fromString("91218dc0-68e9-11f1-992c-0f4d95fca092"),
                    "Smart retail", "1.0.0", List.of("dailyConsumption", "weeklyConsumption"))),
            Map.entry("smart_irrigation", new LegacySolutionTemplate(
                    UUID.fromString("364ab326-699c-4320-b7f6-6e64e90fa21a"),
                    UUID.fromString("9010e110-68e9-11f1-992c-0f4d95fca092"),
                    "Smart irrigation", "1.0.0", List.of())),
            Map.entry("assisted_living", new LegacySolutionTemplate(
                    UUID.fromString("51750ead-5f6d-44a6-bb67-78d7d8bb8dc9"),
                    UUID.fromString("8d5654f0-68e9-11f1-992c-0f4d95fca092"),
                    "Assisted living", "1.0.0", List.of())),
            Map.entry("waste_monitoring", new LegacySolutionTemplate(
                    UUID.fromString("a610d1ad-8fe7-45cf-b839-c6af323b959b"),
                    UUID.fromString("92c21e60-68e9-11f1-992c-0f4d95fca092"),
                    "Waste management", "1.0.0", List.of()))
    );

    private static final String LEGACY_STATUS_KEY_SUFFIX = "_status";
    private static final String LEGACY_ENTITIES_KEY_SUFFIX = "_entities";
    private static final String LEGACY_INSTRUCTIONS_KEY_SUFFIX = "_instructions";

    private static final List<String> LEGACY_ATTRIBUTE_KEYS = LEGACY_SOLUTION_TEMPLATES.keySet().stream()
            .flatMap(id -> Stream.of(
                    id + LEGACY_STATUS_KEY_SUFFIX,
                    id + LEGACY_ENTITIES_KEY_SUFFIX,
                    id + LEGACY_INSTRUCTIONS_KEY_SUFFIX))
            .toList();

    private static final List<UUID> LEGACY_ITEM_IDS = LEGACY_SOLUTION_TEMPLATES.values().stream()
            .map(LegacySolutionTemplate::itemId)
            .toList();

    private void migrateLegacySolutionTemplatesToIotHub() {
        log.info("Starting migration of legacy PE solution templates to IoT Hub installed items...");
        PageDataIterable<TenantId> tenantIds = new PageDataIterable<>(tenantService::findTenantsIds, DEFAULT_PAGE_SIZE);
        for (TenantId tenantId : tenantIds) {
            try {
                migrateLegacySolutionTemplatesForTenant(tenantId);
            } catch (Exception e) {
                log.error("[{}] Failed to migrate legacy solution templates", tenantId, e);
            }
        }
        log.info("Legacy PE solution templates migration completed.");
    }

    private void migrateLegacySolutionTemplatesForTenant(TenantId tenantId) throws Exception {
        List<AttributeKvEntry> entries = attributesService.find(tenantId, tenantId, AttributeScope.SERVER_SCOPE, LEGACY_ATTRIBUTE_KEYS)
                .get(30, TimeUnit.SECONDS);
        if (entries.isEmpty()) {
            return;
        }
        Map<String, AttributeKvEntry> entriesByKey = entries.stream()
                .collect(Collectors.toMap(AttributeKvEntry::getKey, e -> e));

        List<UUID> alreadyInstalled = iotHubInstalledItemService
                .findInstalledItemIdsByTenantIdAndItemIdIn(tenantId, LEGACY_ITEM_IDS);

        List<String> keysToRemove = new ArrayList<>();
        for (Map.Entry<String, LegacySolutionTemplate> entry : LEGACY_SOLUTION_TEMPLATES.entrySet()) {
            String legacyId = entry.getKey();
            LegacySolutionTemplate mapping = entry.getValue();
            AttributeKvEntry statusEntry = entriesByKey.get(legacyId + LEGACY_STATUS_KEY_SUFFIX);
            if (statusEntry == null || !statusEntry.getBooleanValue().orElse(false)) {
                continue;
            }
            AttributeKvEntry entitiesEntry = entriesByKey.get(legacyId + LEGACY_ENTITIES_KEY_SUFFIX);
            AttributeKvEntry instructionsEntry = entriesByKey.get(legacyId + LEGACY_INSTRUCTIONS_KEY_SUFFIX);
            boolean saved = true;
            if (!alreadyInstalled.contains(mapping.itemId())) {
                try {
                    SolutionTemplateInstalledItemDescriptor descriptor = buildLegacyDescriptor(mapping, entitiesEntry, instructionsEntry);
                    IotHubInstalledItem item = new IotHubInstalledItem();
                    item.setTenantId(tenantId);
                    item.setItemId(mapping.itemId());
                    item.setItemVersionId(mapping.itemVersionId());
                    item.setItemName(mapping.itemName());
                    item.setItemType("SOLUTION_TEMPLATE");
                    item.setVersion(mapping.version());
                    item.setDescriptor(descriptor);
                    iotHubInstalledItemService.save(tenantId, item);
                    log.info("[{}] Migrated legacy solution template '{}' (itemId={})", tenantId, legacyId, mapping.itemId());
                } catch (Exception ex) {
                    log.error("[{}] Failed to migrate legacy solution template '{}' — keeping attributes for retry", tenantId, legacyId, ex);
                    saved = false;
                }
            } else {
                log.debug("[{}] Legacy solution template '{}' already present in IoT Hub installed items — skipping save", tenantId, legacyId);
            }
            if (saved) {
                keysToRemove.add(statusEntry.getKey());
                if (entitiesEntry != null) {
                    keysToRemove.add(entitiesEntry.getKey());
                }
                if (instructionsEntry != null) {
                    keysToRemove.add(instructionsEntry.getKey());
                }
            }
        }
        if (!keysToRemove.isEmpty()) {
            attributesService.removeAll(tenantId, tenantId, AttributeScope.SERVER_SCOPE, keysToRemove)
                    .get(30, TimeUnit.SECONDS);
            log.info("[{}] Removed legacy solution-template attributes: {}", tenantId, keysToRemove);
        }
    }

    private SolutionTemplateInstalledItemDescriptor buildLegacyDescriptor(LegacySolutionTemplate mapping,
                                                                          AttributeKvEntry entitiesEntry,
                                                                          AttributeKvEntry instructionsEntry) {
        SolutionTemplateInstalledItemDescriptor descriptor = new SolutionTemplateInstalledItemDescriptor();
        descriptor.setTenantTelemetryKeys(mapping.tenantTelemetryKeys());
        descriptor.setTenantAttributeKeys(List.of());
        descriptor.setMainDashboardPublic(false);
        if (entitiesEntry != null) {
            String entitiesJson = entitiesEntry.getValueAsString();
            if (entitiesJson != null && !entitiesJson.isBlank()) {
                List<EntityId> createdEntityIds = JacksonUtil.fromString(entitiesJson, new TypeReference<>() {});
                descriptor.setCreatedEntityIds(createdEntityIds);
            }
        }
        if (instructionsEntry != null) {
            String instructionsJson = instructionsEntry.getValueAsString();
            if (instructionsJson != null && !instructionsJson.isBlank()) {
                JsonNode instructions = JacksonUtil.toJsonNode(instructionsJson);
                if (instructions != null) {
                    JsonNode dashboardGroupId = instructions.get("dashboardGroupId");
                    if (dashboardGroupId != null && dashboardGroupId.hasNonNull("id")) {
                        descriptor.setDashboardGroupId(new EntityGroupId(UUID.fromString(dashboardGroupId.get("id").asText())));
                    }
                    JsonNode dashboardId = instructions.get("dashboardId");
                    if (dashboardId != null && dashboardId.hasNonNull("id")) {
                        descriptor.setDashboardId(new DashboardId(UUID.fromString(dashboardId.get("id").asText())));
                    }
                    JsonNode details = instructions.get("details");
                    if (details != null && !details.isNull()) {
                        descriptor.setDetails(details.asText());
                    }
                }
            }
        }
        return descriptor;
    }
}
