// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.citus;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.assertj.core.api.SoftAssertions;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Guards {@link CitusTables#MANAGED_FOREIGN_KEYS} against silent drift from the real schema.
 * <p>
 * That static catalog backs ONLY the crash-recovery re-add path in {@link DefaultCitusSchemaService}: a managed
 * foreign key added to {@code schema-entities.sql} but forgotten in the catalog would never be re-added after a
 * crash mid re-add, and nothing else would notice (a normal conversion captures definitions live). This installs
 * the real, freshly-installed (pre-distribution) entities schema into an isolated schema namespace on a live
 * PostgreSQL/Citus instance and asserts that every catalog entry exists with a {@code pg_get_constraintdef} body
 * byte-identical to the {@code definition} string the catalog declares — turning drift into a failing test rather
 * than a latent crash-recovery gap. The previous coverage only exercised a 2-entry fixture override, so the real
 * catalog was never validated against an actual schema.
 * <p>
 * Both drift directions are covered: catalog entries missing from the schema (stale catalog) and — the exact
 * failure described above — schema foreign keys on managed tables missing from the catalog (forgotten catalog
 * update).
 */
class CitusManagedForeignKeysCatalogTest extends AbstractCitusContainerTest {

    private static final String AUDIT_SCHEMA = "managed_fk_audit";

    /**
     * Live foreign keys on managed tables that are deliberately NOT in {@link CitusTables#MANAGED_FOREIGN_KEYS},
     * keyed as {@code table.name}. {@code entity_alarm.fk_entity_alarm_id} only exists in its plain single-column
     * form in the pre-distribution schema; once alarm is distributed on originator_id it is replaced by the
     * co-located composite (originator_id, alarm_id) form, which crash recovery rebuilds via
     * {@code DefaultCitusSchemaService.readdEntityAlarmCompositeForeignKey()} instead of the catalog — see the
     * NOTE at the end of {@code MANAGED_FOREIGN_KEYS}.
     */
    private static final Set<String> DELIBERATELY_UNCATALOGED_FOREIGN_KEYS = Set.of("entity_alarm.fk_entity_alarm_id");

    @BeforeAll
    static void installEntitiesSchema() {
        // Install into a dedicated schema so the full schema-entities.sql tables (device, tenant, ...) do not
        // collide with the minimal same-named fixtures other Citus tests create in the shared coordinator's
        // public schema. search_path is pinned to this schema for the class connection so the script's unqualified
        // CREATE TABLEs land here and pg_get_constraintdef renders referenced tables unqualified, matching the
        // unqualified MANAGED_FOREIGN_KEYS definitions (which mirror the production public-schema capture).
        jdbcTemplate.execute("DROP SCHEMA IF EXISTS " + AUDIT_SCHEMA + " CASCADE");
        jdbcTemplate.execute("CREATE SCHEMA " + AUDIT_SCHEMA);
        jdbcTemplate.execute("SET search_path TO " + AUDIT_SCHEMA);
        jdbcTemplate.execute(readSchemaEntitiesSql());
    }

    @AfterAll
    static void dropEntitiesSchema() {
        if (jdbcTemplate != null) {
            jdbcTemplate.execute("DROP SCHEMA IF EXISTS " + AUDIT_SCHEMA + " CASCADE");
            jdbcTemplate.execute("SET search_path TO public");
        }
    }

    @Test
    void everyManagedForeignKeyMatchesTheInstalledSchema() {
        SoftAssertions soft = new SoftAssertions();
        for (CitusTables.ManagedForeignKey fk : CitusTables.MANAGED_FOREIGN_KEYS) {
            List<String> defs = jdbcTemplate.queryForList(
                    "SELECT pg_get_constraintdef(c.oid) FROM pg_constraint c " +
                            "WHERE c.contype = 'f' AND c.conname = ? AND c.conrelid = ?::regclass",
                    String.class, fk.name(), fk.table());
            soft.assertThat(defs)
                    .as("Managed foreign key %s on table %s is declared in MANAGED_FOREIGN_KEYS but is missing from " +
                            "the installed schema (schema-entities.sql drift?)", fk.name(), fk.table())
                    .isNotEmpty();
            if (!defs.isEmpty()) {
                soft.assertThat(defs.get(0))
                        .as("pg_get_constraintdef for %s on %s must match the MANAGED_FOREIGN_KEYS definition " +
                                "(the catalog's re-add DDL must be byte-identical to a live capture)", fk.name(), fk.table())
                        .isEqualTo(fk.definition());
            }
        }
        soft.assertAll();
    }

    /**
     * The reverse direction of {@link #everyManagedForeignKeyMatchesTheInstalledSchema()}: every foreign key the
     * installed schema declares on a managed (distributed or reference) table must appear in
     * {@link CitusTables#MANAGED_FOREIGN_KEYS}. A managed FK added to {@code schema-entities.sql} but forgotten
     * in the catalog would otherwise pass unnoticed and silently degrade crash recovery (see the class javadoc).
     */
    @Test
    void everyForeignKeyOnAManagedTableIsDeclaredInTheCatalog() {
        List<String> managedTables = Stream.concat(
                CitusTables.DISTRIBUTED_TABLES.stream(), CitusTables.REFERENCE_TABLES.stream()).toList();
        String placeholders = String.join(", ", Collections.nCopies(managedTables.size(), "?"));
        List<Map<String, Object>> liveForeignKeys = jdbcTemplate.queryForList(
                "SELECT child.relname AS child_table, c.conname AS constraint_name FROM pg_constraint c " +
                        "JOIN pg_class child ON child.oid = c.conrelid " +
                        "JOIN pg_namespace ns ON ns.oid = child.relnamespace " +
                        "WHERE c.contype = 'f' AND ns.nspname = ? AND child.relname IN (" + placeholders + ")",
                Stream.concat(Stream.of(AUDIT_SCHEMA), managedTables.stream()).toArray());
        Set<String> catalogedForeignKeys = CitusTables.MANAGED_FOREIGN_KEYS.stream()
                .map(CitusTables.ManagedForeignKey::qualifiedName)
                .collect(Collectors.toSet());
        SoftAssertions soft = new SoftAssertions();
        for (Map<String, Object> liveForeignKey : liveForeignKeys) {
            String key = liveForeignKey.get("child_table") + "." + liveForeignKey.get("constraint_name");
            if (DELIBERATELY_UNCATALOGED_FOREIGN_KEYS.contains(key)) {
                continue;
            }
            soft.assertThat(catalogedForeignKeys)
                    .as("Foreign key %s exists in the installed schema on a managed table but is missing from " +
                            "MANAGED_FOREIGN_KEYS — it would never be re-added after a crash mid re-add", key)
                    .contains(key);
        }
        soft.assertAll();
    }

    /**
     * The explicit structural components ({@code childColumns}, {@code parentTable}) that back the structural
     * crash-repair presence check ({@code DefaultCitusSchemaService.existingForeignKeyStructures()} vs
     * {@code ManagedForeignKey.structuralKey()}) must agree with the {@code definition} DDL: every definition must
     * start with {@code FOREIGN KEY (<childColumns>) REFERENCES <parentTable>(}. Combined with
     * {@link #everyManagedForeignKeyMatchesTheInstalledSchema()} (definition is byte-identical to the live
     * {@code pg_get_constraintdef}), this transitively pins the structural components to the real schema — drift
     * here would make the structural key lie about what the re-add DDL actually creates.
     */
    @Test
    void structuralComponentsAgreeWithEachCatalogDefinition() {
        SoftAssertions soft = new SoftAssertions();
        for (CitusTables.ManagedForeignKey fk : CitusTables.MANAGED_FOREIGN_KEYS) {
            String expectedPrefix = "FOREIGN KEY (" + String.join(", ", fk.childColumns()) + ") REFERENCES " + fk.parentTable() + "(";
            soft.assertThat(fk.definition())
                    .as("Managed foreign key %s declares childColumns/parentTable that disagree with its definition DDL",
                            fk.qualifiedName())
                    .startsWith(expectedPrefix);
        }
        soft.assertAll();
    }

    private static String readSchemaEntitiesSql() {
        try (InputStream is = CitusManagedForeignKeysCatalogTest.class.getResourceAsStream("/sql/schema-entities.sql")) {
            if (is == null) {
                throw new IllegalStateException("Could not find /sql/schema-entities.sql on the test classpath");
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
