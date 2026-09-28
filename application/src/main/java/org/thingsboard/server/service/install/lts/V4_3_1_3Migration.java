// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.install.lts;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.iot_hub.IotHubInstalledItem;
import org.thingsboard.server.common.data.iot_hub.SolutionTemplateInstalledItemDescriptor;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.widget.WidgetTypeDetails;
import org.thingsboard.server.common.data.widget.WidgetsBundle;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.iot_hub.IotHubInstalledItemService;
import org.thingsboard.server.dao.widget.WidgetTypeService;
import org.thingsboard.server.dao.widget.WidgetsBundleService;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Component
@TbCoreComponent
@RequiredArgsConstructor
public class V4_3_1_3Migration implements LtsMigration {

    // Match on the bundle ALIAS, not the JSON filename stem.
    // The "industrial_widgets" bundle shipped in a misspelled file (industial_widgets.json),
    // but the alias inside it is correctly spelled.
    private static final List<String> OBSOLETE_BUNDLE_ALIASES = List.of(
            "air_quality", "indoor_environment", "industrial_widgets", "outdoor_environment");

    // Tenants are migrated concurrently: the per-tenant work is I/O-bound (attribute cache round-trips + DB), so
    // running several at once hides that latency. Capped at 8 to stay well under the JDBC pool (default 16) since
    // each in-flight tenant borrows a connection for its find/save/removeAll calls.
    private static final int MIGRATION_PARALLELISM = Math.max(4, Math.min(Runtime.getRuntime().availableProcessors(), 8));

    private final WidgetsBundleService widgetsBundleService;
    private final WidgetTypeService widgetTypeService;
    private final AttributesService attributesService;
    private final IotHubInstalledItemService iotHubInstalledItemService;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public String getVersion() {
        return "4.3.1.3";
    }

    @Override
    public void apply() {
        // Small, bounded, system-tenant-only work: fine to run inside the schema transaction.
        for (String alias : OBSOLETE_BUNDLE_ALIASES) {
            deprecateTypesAndDeleteBundle(alias);
        }
    }

    @Override
    public void applyAfterCommit() {
        // Heavy per-tenant backfill: runs outside the schema transaction so it never holds the schema-change
        // locks while walking tenants (see LtsMigration#applyAfterCommit).
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
    // current IoT Hub item descriptors (minTbVersion: 430).
    // itemVersionId / itemName / version were resolved once via
    // https://iot-hub.thingsboard.io/api/items/{itemId}/published and baked
    // in so the upgrade is fully self-contained (no network call at runtime).
    private static final Map<String, LegacySolutionTemplate> LEGACY_SOLUTION_TEMPLATES = Map.ofEntries(
            Map.entry("temperature_sensors", new LegacySolutionTemplate(
                    UUID.fromString("9a64ef1f-c926-45fb-9a5f-0e88e40c485d"),
                    UUID.fromString("91ee53a0-68e9-11f1-992c-0f4d95fca092"),
                    "Temperature & Humidity sensors", "1.0.0", List.of())),
            Map.entry("smart_office", new LegacySolutionTemplate(
                    UUID.fromString("bfb5793e-5b99-4d11-8ce3-079bfcd49257"),
                    UUID.fromString("90564e80-68e9-11f1-992c-0f4d95fca092"),
                    "Smart office", "1.0.0", List.of())),
            Map.entry("fleet_tracking", new LegacySolutionTemplate(
                    UUID.fromString("f7589b43-0bbe-45fe-90bb-2bbbb563f361"),
                    UUID.fromString("8dc14bc0-68e9-11f1-992c-0f4d95fca092"),
                    "Site fleet tracking", "1.0.0", List.of())),
            Map.entry("fuel_level_monitoring", new LegacySolutionTemplate(
                    UUID.fromString("d41ad82d-d86a-44df-bc01-d2262de48162"),
                    UUID.fromString("8e729a60-68e9-11f1-992c-0f4d95fca092"),
                    "Fuel level monitoring", "1.0.0", List.of())),
            Map.entry("swimming_pool_scada_system", new LegacySolutionTemplate(
                    UUID.fromString("5f958baa-ee8a-417e-961f-9d553bbac7e4"),
                    UUID.fromString("917ccd20-68e9-11f1-992c-0f4d95fca092"),
                    "Swimming pool SCADA system", "1.0.0", List.of())),
            Map.entry("scada_drilling_system", new LegacySolutionTemplate(
                    UUID.fromString("3b24a28e-6ad8-42c0-90f7-88d63901d4e9"),
                    UUID.fromString("8eefe0b0-68e9-11f1-992c-0f4d95fca092"),
                    "SCADA Oil & Gas drilling system", "1.0.0", List.of())),
            Map.entry("scada_energy_management", new LegacySolutionTemplate(
                    UUID.fromString("b9a79174-68d6-43d4-b0bb-288958a70787"),
                    UUID.fromString("8f415c10-68e9-11f1-992c-0f4d95fca092"),
                    "SCADA Energy management", "1.0.0", List.of())),
            Map.entry("air_quality_index", new LegacySolutionTemplate(
                    UUID.fromString("70066f96-adc8-4d29-8975-4753b326e8c8"),
                    UUID.fromString("8c8633b0-68e9-11f1-992c-0f4d95fca092"),
                    "Air quality monitoring", "1.0.0", List.of())),
            Map.entry("water_metering", new LegacySolutionTemplate(
                    UUID.fromString("9acfcdf8-660b-47d5-8f23-1ef92c5c4f38"),
                    UUID.fromString("92fa1e50-68e9-11f1-992c-0f4d95fca092"),
                    "Water metering", "1.0.0", List.of("dailyConsumption", "weeklyConsumption"))),
            Map.entry("smart_retail", new LegacySolutionTemplate(
                    UUID.fromString("d54a8c77-9565-4142-8dd2-8be69c2b6211"),
                    UUID.fromString("90c38f40-68e9-11f1-992c-0f4d95fca092"),
                    "Smart retail", "1.0.0", List.of("dailyConsumption", "weeklyConsumption"))),
            Map.entry("smart_irrigation", new LegacySolutionTemplate(
                    UUID.fromString("e3d61121-55e4-42dc-81f1-86f088909c4d"),
                    UUID.fromString("8fbf1790-68e9-11f1-992c-0f4d95fca092"),
                    "Smart irrigation", "1.0.0", List.of())),
            Map.entry("assisted_living", new LegacySolutionTemplate(
                    UUID.fromString("4aed1971-57a6-4030-8d47-a854356a2a22"),
                    UUID.fromString("8d07e6d0-68e9-11f1-992c-0f4d95fca092"),
                    "Assisted living", "1.0.0", List.of())),
            Map.entry("waste_monitoring", new LegacySolutionTemplate(
                    UUID.fromString("993fee8b-a0de-43c8-a113-72db46d7ceb3"),
                    UUID.fromString("9271db80-68e9-11f1-992c-0f4d95fca092"),
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

    // The "<id>_status" attribute is the trigger the per-tenant migration keys on, so it is the minimal set to
    // discover on: a tenant that never installed a legacy template has none of these keys.
    private static final List<String> LEGACY_STATUS_KEYS = LEGACY_SOLUTION_TEMPLATES.keySet().stream()
            .map(id -> id + LEGACY_STATUS_KEY_SUFFIX)
            .toList();

    // Finds only the tenants that actually installed a legacy solution template, instead of scanning every tenant.
    // attribute_kv stores the scope as an int (AttributeScope#getId) and the key as a key_dictionary id; joining
    // key_dictionary resolves the ids and joining tenant keeps the original "tenant entities only" semantics
    // (a legacy key on a non-tenant entity is ignored, as before).
    private List<UUID> findTenantIdsWithLegacyTemplates() {
        String placeholders = LEGACY_STATUS_KEYS.stream().map(key -> "?").collect(Collectors.joining(", "));
        String sql = "SELECT DISTINCT a.entity_id FROM attribute_kv a " +
                "JOIN key_dictionary kd ON kd.key_id = a.attribute_key " +
                "JOIN tenant t ON t.id = a.entity_id " +
                "WHERE a.attribute_type = " + AttributeScope.SERVER_SCOPE.getId() +
                " AND kd.key IN (" + placeholders + ")";
        return jdbcTemplate.queryForList(sql, UUID.class, LEGACY_STATUS_KEYS.toArray());
    }

    private void migrateLegacySolutionTemplatesToIotHub() {
        log.info("Starting migration of legacy PE solution templates to IoT Hub installed items...");
        List<UUID> tenantUuids = findTenantIdsWithLegacyTemplates();
        log.info("Found {} tenant(s) with legacy solution-template attributes to migrate", tenantUuids.size());
        if (tenantUuids.isEmpty()) {
            return;
        }
        // Each tenant is migrated through the services below, which self-commit; there is no surrounding
        // transaction, so only brief row-level locks are held and a failure on one tenant does not roll back the
        // others. A tenant whose attributes were already removed on a previous run no longer matches the discovery
        // query, so re-runs resume from where they left off. Tenants are independent, so they run concurrently.
        ExecutorService executor = Executors.newFixedThreadPool(MIGRATION_PARALLELISM,
                ThingsBoardThreadFactory.forName("lts-solution-template-migration"));
        // Bounds in-flight work to the pool size instead of queueing every tenant at once: the submit loop blocks
        // once all workers are busy and resumes as each finishes, so memory stays flat regardless of tenant count.
        Semaphore inFlight = new Semaphore(MIGRATION_PARALLELISM);
        try {
            for (UUID tenantUuid : tenantUuids) {
                inFlight.acquire();
                TenantId tenantId = TenantId.fromUUID(tenantUuid);
                executor.submit(() -> {
                    try {
                        migrateLegacySolutionTemplatesForTenant(tenantId);
                    } catch (Exception e) {
                        log.error("[{}] Failed to migrate legacy solution templates", tenantId, e);
                    } finally {
                        inFlight.release();
                    }
                });
            }
            inFlight.acquire(MIGRATION_PARALLELISM); // drain: returns only once the last submitted tenants finish
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while migrating legacy solution templates", e);
        } finally {
            // shutdownNow (not shutdown) so that on an interrupt the still-running tenant tasks are actually
            // cancelled -- their blocking future waits are interruptible -- rather than left running in the
            // background on non-daemon threads after this method has already reported failure. On the normal path
            // the drain above guarantees nothing is still running, so this just tears the idle pool down.
            executor.shutdownNow();
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
