// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.citus;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Canonical Citus table classification for ThingsBoard.
 *
 * <p>The distributed definition of ts_kv_latest is the psql variant from schema-ts-latest-psql.sql.
 */
public final class CitusTables {

    private CitusTables() {}

    /**
     * Hash-distributed, single co-location group. {@code attribute_kv} is the anchor (it carries
     * {@code shard_count}); every other table co-locates with it. The anchor MUST stay first —
     * {@link DefaultCitusSchemaService} establishes the colocation group from the first not-yet-distributed
     * table. {@code device}, {@code asset} and {@code entity_view} are distributed on {@code id} (not
     * {@code entity_id}) so an entity row shares a shard with its {@code attribute_kv}/{@code ts_kv_latest}
     * rows ({@code attribute_kv.entity_id == <entity>.id}), enabling co-located pushdown joins.
     *
     * <p>{@code alarm} and {@code entity_alarm} are distributed on {@code originator_id} (the alarm's
     * originator entity id), so an alarm and its originator-keyed entity_alarm index rows co-locate with that
     * same originator's {@code attribute_kv}/{@code ts_kv_latest}/{@code device}/{@code asset}/{@code entity_view}
     * rows ({@code alarm.originator_id == <entity>.id}). This makes alarm_info's LEFT JOIN into
     * {@code device}/{@code asset}/{@code entity_view} (to resolve originator_name) a co-located distributed join
     * — and keeps a single alarm's whole lifecycle (ack/clear/assign and its entity_alarm index rows) on one
     * shard so the mutation paths stay single-shard. The originator-id alarm group must come AFTER the anchor and
     * the entity tables it co-locates with; it never becomes the anchor.
     */
    public static final List<String> DISTRIBUTED_TABLES =
            List.of("attribute_kv", "ts_kv_latest", "device", "asset", "entity_view", "alarm", "entity_alarm");

    /**
     * Replicated to every worker. This is the membership set of tables converted via
     * {@code create_reference_table}: the full managed set minus the distributed tables
     * {@code device}/{@code asset}/{@code entity_view}/{@code alarm}/{@code entity_alarm} and minus the
     * partitioned {@code blob_entity}/{@code report}/{@code alarm_comment} (Citus rejects
     * {@code create_reference_table} on partitioned tables; they stay coordinator-local — nothing FKs into
     * them), plus {@code device_credentials} (1:1 with the distributed {@code device}, replicated here).
     *
     * <p><b>The list order is membership-only and NOT load-bearing.</b> Unlike {@link #DISTRIBUTED_TABLES}
     * (where the anchor-first rule matters — see that field), the order of reference tables is irrelevant:
     * {@link DefaultCitusSchemaService} drops ALL managed foreign keys up front (before any conversion) and
     * re-adds them only after every conversion completes, so no FK-target-before-FK-source / topological
     * ordering is required here. The grouping comments below are descriptive only. Note that an in-set FK
     * topological order does not even exist: {@code ota_package(device_profile_id) → device_profile} and
     * {@code device_profile → ota_package} (firmware/software) form a cycle.
     */
    public static final List<String> REFERENCE_TABLES = List.of(
            "key_dictionary", "tenant_profile", "converter", "rule_chain", "ota_package", "dashboard",
            "device_profile", "asset_profile",
            "report_template", "entity_group",
            // entity tables (tenant->tenant_profile; asset/entity_view are now distributed, not reference)
            "tenant", "customer", "tb_user", "edge",
            "integration", "scheduler_event", "role",
            "api_usage_state", "relation",
            // user_*->tb_user, alarm_types->tenant, device_group_ota_package->entity_group/ota_package
            "user_auth_settings", "user_settings", "alarm_types", "device_group_ota_package",
            // device_credentials: 1:1 with device but read-hot by credentials_id and write-rare; replicated so the
            // auth lookup is local on any node and its credentials_id/device_id UNIQUE constraints stay enforced
            // (reference tables enforce uniqueness cluster-wide). It has no FK to/from any table, so its placement
            // in this list is free and it raises no reference->distributed FK concern with the distributed device.
            "device_credentials"
            // alarm domain: alarm and entity_alarm are NO LONGER reference tables — they are distributed on
            // originator_id and co-located with the anchor group (see DISTRIBUTED_TABLES). The partitioned
            // alarm_comment has no FK to alarm anymore (the FK was dropped from the base schema), so Citus does not
            // auto-track it: it is NOT a Citus-managed table and simply stays a plain coordinator-local partitioned
            // Postgres table.
    );

    private static final Set<String> DISTRIBUTED_OR_REFERENCE = Set.copyOf(
            Stream.concat(DISTRIBUTED_TABLES.stream(), REFERENCE_TABLES.stream()).toList());

    /**
     * True when {@code table} exists on every worker — i.e. it is hash-distributed ({@link #DISTRIBUTED_TABLES}) or a
     * reference table ({@link #REFERENCE_TABLES}). False for the coordinator-local tables (the partitioned
     * {@code blob_entity}/{@code report}/{@code alarm_comment} and any table not converted to a Citus-managed table).
     * A latest-value pushdown join is only legal when its anchor table is present on every worker; a coordinator-local
     * anchor must keep the coordinator-side (CTE) join form instead.
     */
    public static boolean isDistributedOrReference(String table) {
        return DISTRIBUTED_OR_REFERENCE.contains(table);
    }

    private static final Set<String> DISTRIBUTED_ON_ID = Set.of("device", "asset", "entity_view");

    private static final Set<String> DISTRIBUTED_ON_ORIGINATOR = Set.of("alarm", "entity_alarm");

    private static final Set<String> DISTRIBUTED_ON_ENTITY_ID = Set.of("attribute_kv", "ts_kv_latest");

    /**
     * Distribution column for a table in {@link #DISTRIBUTED_TABLES}. The KV fact tables distribute on
     * {@code entity_id}; the entity tables {@code device}/{@code asset}/{@code entity_view} distribute on their
     * primary key {@code id}; {@code alarm}/{@code entity_alarm} distribute on {@code originator_id}. All are
     * type-compatible ({@code uuid}) so they co-locate. Every distributed table must be classified in exactly
     * one of the three sets above; an unknown table throws rather than silently defaulting — a future table
     * forgotten from all three sets would otherwise be distributed on a plausible-looking wrong column.
     */
    public static String distributionColumn(String table) {
        if (DISTRIBUTED_ON_ID.contains(table)) {
            return "id";
        }
        if (DISTRIBUTED_ON_ORIGINATOR.contains(table)) {
            return "originator_id";
        }
        if (DISTRIBUTED_ON_ENTITY_ID.contains(table)) {
            return "entity_id";
        }
        throw new IllegalArgumentException("No distribution column is defined for table '" + table +
                "'; add it to the appropriate DISTRIBUTED_ON_* set in CitusTables");
    }

    /**
     * Canonical structural identity of a foreign key: the child table, the parent table it references and the
     * ordered, comma-joined child column list. This — deliberately NOT the constraint name — is how
     * {@code DefaultCitusSchemaService} decides whether a managed FK is already present: an equivalent live FK
     * under a different (historical or auto-generated) name must count as present, otherwise the crash repair
     * would add a duplicate constraint next to it (a long ACCESS EXCLUSIVE validation on a big table, then two
     * identical FKs forever). Kept here as the single builder of the key format; the shared SQL fragment
     * {@code DefaultCitusSchemaService.FK_STRUCTURAL_KEY_SQL} mirrors it and must stay in sync.
     */
    public static String foreignKeyStructuralKey(String childTable, String parentTable, String childColumns) {
        return childTable + "|" + parentTable + "|" + childColumns;
    }

    /**
     * A managed foreign key: the child {@code table} it lives on, its constraint {@code name}, the ordered
     * {@code childColumns} it constrains, the {@code parentTable} it references, and the {@code definition} body
     * that follows {@code ADD CONSTRAINT <name>} in an {@code ALTER TABLE} statement (i.e. the
     * {@code FOREIGN KEY (...) REFERENCES ...} clause, exactly as PostgreSQL's {@code pg_get_constraintdef}
     * renders it). {@link #addStatement()} assembles the full re-add DDL. {@code childColumns}/{@code parentTable}
     * are declared explicitly rather than parsed out of {@code definition};
     * {@code CitusManagedForeignKeysCatalogTest} guards them against drifting from the definition (and the
     * definition against drifting from the real schema).
     */
    public record ManagedForeignKey(String table, String name, List<String> childColumns, String parentTable, String definition) {
        public String addStatement() {
            return "ALTER TABLE " + table + " ADD CONSTRAINT " + name + " " + definition;
        }

        /** Structural identity used by the crash-repair presence check — see {@link #foreignKeyStructuralKey}. */
        public String structuralKey() {
            return foreignKeyStructuralKey(table, parentTable, String.join(",", childColumns));
        }

        /**
         * Table-qualified constraint name ({@code table.name}). PostgreSQL constraint names are only unique per
         * table, so any name-keyed bookkeeping (e.g. the catalog drift-guard test) must qualify by child table.
         */
        public String qualifiedName() {
            return table + "." + name;
        }
    }

    /**
     * Deterministic catalog of every foreign key whose child is one of the managed tables
     * ({@link #DISTRIBUTED_TABLES} + {@link #REFERENCE_TABLES}), mirroring the {@code CONSTRAINT ... FOREIGN KEY}
     * declarations in {@code schema-entities.sql}. The {@code definition} strings are written to match
     * {@code pg_get_constraintdef} output so a re-add here is byte-identical to the live-captured re-add that
     * {@code DefaultCitusSchemaService.dropManagedForeignKeys()} performs on a normal run.
     *
     * <p>This list backs ONLY the crash-recovery path
     * ({@code DefaultCitusSchemaService.readdMissingForeignKeys(...)}, run at the start of every conversion): after
     * a crashed prior run — whether it got as far as distributing everything or crashed before distributing
     * anything — the live tables can no longer be queried for FKs that were dropped-but-never-re-added (each
     * {@code ADD CONSTRAINT} is its own autocommit, so a crash mid re-add loses them from the catalog with no live
     * source to recover from). The expected set is therefore re-derived from this static, install-invariant
     * declaration and only the genuinely missing FKs are re-added. The normal conversion phases never consult this
     * list; they capture definitions live via {@code pg_get_constraintdef}. Consequently, drift between this list and
     * the schema degrades ONLY crash recovery (a newly added managed FK forgotten here would not be re-added
     * after a crash mid re-add) — it never affects a normal conversion. Keep it in sync when adding or changing a
     * foreign key on a managed table.
     */
    public static final List<ManagedForeignKey> MANAGED_FOREIGN_KEYS = List.of(
            // distributed entity tables
            new ManagedForeignKey("device", "fk_device_profile", List.of("device_profile_id"), "device_profile", "FOREIGN KEY (device_profile_id) REFERENCES device_profile(id)"),
            new ManagedForeignKey("device", "fk_firmware_device", List.of("firmware_id"), "ota_package", "FOREIGN KEY (firmware_id) REFERENCES ota_package(id)"),
            new ManagedForeignKey("device", "fk_software_device", List.of("software_id"), "ota_package", "FOREIGN KEY (software_id) REFERENCES ota_package(id)"),
            new ManagedForeignKey("asset", "fk_asset_profile", List.of("asset_profile_id"), "asset_profile", "FOREIGN KEY (asset_profile_id) REFERENCES asset_profile(id)"),
            // reference tables
            new ManagedForeignKey("device_profile", "fk_default_rule_chain_device_profile", List.of("default_rule_chain_id"), "rule_chain", "FOREIGN KEY (default_rule_chain_id) REFERENCES rule_chain(id)"),
            new ManagedForeignKey("device_profile", "fk_default_dashboard_device_profile", List.of("default_dashboard_id"), "dashboard", "FOREIGN KEY (default_dashboard_id) REFERENCES dashboard(id)"),
            new ManagedForeignKey("device_profile", "fk_firmware_device_profile", List.of("firmware_id"), "ota_package", "FOREIGN KEY (firmware_id) REFERENCES ota_package(id)"),
            new ManagedForeignKey("device_profile", "fk_software_device_profile", List.of("software_id"), "ota_package", "FOREIGN KEY (software_id) REFERENCES ota_package(id)"),
            new ManagedForeignKey("device_profile", "fk_default_edge_rule_chain_device_profile", List.of("default_edge_rule_chain_id"), "rule_chain", "FOREIGN KEY (default_edge_rule_chain_id) REFERENCES rule_chain(id)"),
            new ManagedForeignKey("asset_profile", "fk_default_rule_chain_asset_profile", List.of("default_rule_chain_id"), "rule_chain", "FOREIGN KEY (default_rule_chain_id) REFERENCES rule_chain(id)"),
            new ManagedForeignKey("asset_profile", "fk_default_dashboard_asset_profile", List.of("default_dashboard_id"), "dashboard", "FOREIGN KEY (default_dashboard_id) REFERENCES dashboard(id)"),
            new ManagedForeignKey("asset_profile", "fk_default_edge_rule_chain_asset_profile", List.of("default_edge_rule_chain_id"), "rule_chain", "FOREIGN KEY (default_edge_rule_chain_id) REFERENCES rule_chain(id)"),
            new ManagedForeignKey("ota_package", "fk_device_profile_ota_package", List.of("device_profile_id"), "device_profile", "FOREIGN KEY (device_profile_id) REFERENCES device_profile(id) ON DELETE CASCADE"),
            new ManagedForeignKey("integration", "fk_integration_converter", List.of("converter_id"), "converter", "FOREIGN KEY (converter_id) REFERENCES converter(id)"),
            new ManagedForeignKey("integration", "fk_integration_downlink_converter", List.of("downlink_converter_id"), "converter", "FOREIGN KEY (downlink_converter_id) REFERENCES converter(id)"),
            new ManagedForeignKey("tenant", "fk_tenant_profile", List.of("tenant_profile_id"), "tenant_profile", "FOREIGN KEY (tenant_profile_id) REFERENCES tenant_profile(id)"),
            new ManagedForeignKey("user_auth_settings", "fk_user_auth_settings_user_id", List.of("user_id"), "tb_user", "FOREIGN KEY (user_id) REFERENCES tb_user(id)"),
            new ManagedForeignKey("user_settings", "fk_user_id", List.of("user_id"), "tb_user", "FOREIGN KEY (user_id) REFERENCES tb_user(id) ON DELETE CASCADE"),
            new ManagedForeignKey("alarm_types", "fk_entity_tenant_id", List.of("tenant_id"), "tenant", "FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE CASCADE"),
            new ManagedForeignKey("device_group_ota_package", "fk_ota_package_device_group_ota_package", List.of("ota_package_id"), "ota_package", "FOREIGN KEY (ota_package_id) REFERENCES ota_package(id) ON DELETE CASCADE"),
            new ManagedForeignKey("device_group_ota_package", "fk_entity_group_device_group_ota_package", List.of("group_id"), "entity_group", "FOREIGN KEY (group_id) REFERENCES entity_group(id) ON DELETE CASCADE")
            // NOTE: entity_alarm's fk_entity_alarm_id is deliberately NOT listed here. On this (alarm-sharding)
            // topology alarm/entity_alarm are distributed on originator_id, so the constraint is the co-located
            // composite (originator_id, alarm_id) -> alarm(originator_id, id) form, not the single-column
            // (alarm_id) -> alarm(id) form of the plain schema. Listing the plain form would make the crash-recovery
            // path (DefaultCitusSchemaService.readdMissingForeignKeys) re-add an invalid FK once alarm's PK is
            // (originator_id, id); the composite FK is instead rebuilt/repaired by
            // DefaultCitusSchemaService.readdEntityAlarmCompositeForeignKey().
    );
}
