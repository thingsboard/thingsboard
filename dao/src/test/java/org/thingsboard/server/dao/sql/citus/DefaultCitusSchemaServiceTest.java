// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.citus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultCitusSchemaServiceTest extends AbstractCitusContainerTest {

    private CitusSchemaService schemaService;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS attribute_kv, ts_kv_latest, device, device_profile, tenant, ota_package, entity_alarm, alarm, alarm_comment CASCADE");
        jdbcTemplate.execute("CREATE TABLE tenant (id uuid primary key)");
        jdbcTemplate.execute("CREATE TABLE ota_package (id uuid primary key, data oid)");
        jdbcTemplate.execute("CREATE TABLE device_profile (id uuid primary key, firmware_id uuid, " +
                "CONSTRAINT fk_firmware_device_profile FOREIGN KEY (firmware_id) REFERENCES ota_package(id))");
        jdbcTemplate.execute("CREATE TABLE device (id uuid primary key, firmware_id uuid, " +
                "CONSTRAINT fk_firmware_device FOREIGN KEY (firmware_id) REFERENCES ota_package(id))");
        jdbcTemplate.execute("CREATE TABLE attribute_kv (entity_id uuid, attribute_type int, attribute_key int, version bigint, " +
                "CONSTRAINT attribute_kv_pkey PRIMARY KEY (entity_id, attribute_type, attribute_key))");
        jdbcTemplate.execute("CREATE TABLE ts_kv_latest (entity_id uuid, key int, version bigint, " +
                "CONSTRAINT ts_kv_latest_pkey PRIMARY KEY (entity_id, key))");
        // alarm/entity_alarm: the plain-Postgres PKs and the single-column entity_alarm -> alarm FK from
        // schema-entities.sql. applyDistribution() must rewrite both PKs to lead with originator_id and replace
        // this FK with the co-located composite (originator_id, alarm_id) -> alarm(originator_id, id) form.
        // tenant_id/created_time/cleared carry no constraint under test; they are here because ensureCitusOnlyIndexes()
        // builds the active-alarm index over them.
        jdbcTemplate.execute("CREATE TABLE alarm (id uuid NOT NULL CONSTRAINT alarm_pkey PRIMARY KEY, " +
                "originator_id uuid, tenant_id uuid, created_time bigint, cleared boolean)");
        jdbcTemplate.execute("CREATE TABLE entity_alarm (entity_id uuid NOT NULL, originator_id uuid, alarm_id uuid, " +
                "CONSTRAINT entity_alarm_pkey PRIMARY KEY (entity_id, alarm_id), " +
                "CONSTRAINT fk_entity_alarm_id FOREIGN KEY (alarm_id) REFERENCES alarm(id) ON DELETE CASCADE)");

        CitusSettings settings = new CitusSettings();
        ReflectionTestUtils.setField(settings, "enabled", true);
        ReflectionTestUtils.setField(settings, "shardCount", 8);
        // limit the managed sets to the tables that exist in this minimal schema (asset/entity_view and the
        // other reference tables are exercised by the full conversion integration test, not this unit fixture)
        schemaService = new DefaultCitusSchemaService(jdbcTemplate, settings) {
            @Override
            protected List<String> distributedTables() {
                return List.of("attribute_kv", "ts_kv_latest", "device", "alarm", "entity_alarm");
            }

            @Override
            protected List<String> referenceTables() {
                return List.of("ota_package", "tenant", "device_profile");
            }

            @Override
            protected List<CitusTables.ManagedForeignKey> managedForeignKeys() {
                // only the managed FKs that exist in this minimal fixture schema; the full set references
                // columns/tables this fixture does not create
                return List.of(
                        new CitusTables.ManagedForeignKey("device_profile", "fk_firmware_device_profile",
                                List.of("firmware_id"), "ota_package",
                                "FOREIGN KEY (firmware_id) REFERENCES ota_package(id)"),
                        new CitusTables.ManagedForeignKey("device", "fk_firmware_device",
                                List.of("firmware_id"), "ota_package",
                                "FOREIGN KEY (firmware_id) REFERENCES ota_package(id)"));
            }
        };
    }

    @Test
    void distributesKvTablesAndReplicatesReferenceTables() {
        schemaService.applyDistribution();

        String kvKind = jdbcTemplate.queryForObject(
                "select partmethod from pg_dist_partition where logicalrelid = 'attribute_kv'::regclass", String.class);
        assertThat(kvKind).isEqualTo("h"); // hash distributed

        // device is now hash-distributed on id (not a reference table) and co-located with attribute_kv
        String deviceKind = jdbcTemplate.queryForObject(
                "select partmethod from pg_dist_partition where logicalrelid = 'device'::regclass", String.class);
        assertThat(deviceKind).isEqualTo("h"); // 'h' = hash distributed

        Long distinctColocationGroups = jdbcTemplate.queryForObject(
                "select count(distinct colocationid) from pg_dist_partition " +
                        "where logicalrelid in ('attribute_kv'::regclass, 'device'::regclass)", Long.class);
        assertThat(distinctColocationGroups).isEqualTo(1L); // device co-located with attribute_kv

        // the dimension tables remain reference tables ('n' = no distribution column)
        String refKind = jdbcTemplate.queryForObject(
                "select partmethod from pg_dist_partition where logicalrelid = 'device_profile'::regclass", String.class);
        assertThat(refKind).isEqualTo("n");

        Integer shards = jdbcTemplate.queryForObject(
                "select count(*) from pg_dist_shard where logicalrelid = 'attribute_kv'::regclass", Integer.class);
        assertThat(shards).isEqualTo(8);
    }

    @Test
    void alarmTablesDistributedOnOriginatorIdCoLocatedWithAnchorWithCompositeFk() {
        schemaService.applyDistribution();

        // alarm and entity_alarm are hash-distributed (not reference) on originator_id.
        String alarmKind = jdbcTemplate.queryForObject(
                "select partmethod from pg_dist_partition where logicalrelid = 'alarm'::regclass", String.class);
        assertThat(alarmKind).isEqualTo("h");
        String entityAlarmKind = jdbcTemplate.queryForObject(
                "select partmethod from pg_dist_partition where logicalrelid = 'entity_alarm'::regclass", String.class);
        assertThat(entityAlarmKind).isEqualTo("h");

        // both distribute on originator_id (column_to_column_name renders the distribution column name).
        String alarmDistCol = jdbcTemplate.queryForObject(
                "select column_to_column_name(logicalrelid, partkey) from pg_dist_partition " +
                        "where logicalrelid = 'alarm'::regclass", String.class);
        assertThat(alarmDistCol).isEqualTo("originator_id");
        String entityAlarmDistCol = jdbcTemplate.queryForObject(
                "select column_to_column_name(logicalrelid, partkey) from pg_dist_partition " +
                        "where logicalrelid = 'entity_alarm'::regclass", String.class);
        assertThat(entityAlarmDistCol).isEqualTo("originator_id");

        // both co-located with the anchor attribute_kv (same colocationid as the whole group).
        Long distinctColocationGroups = jdbcTemplate.queryForObject(
                "select count(distinct colocationid) from pg_dist_partition " +
                        "where logicalrelid in ('attribute_kv'::regclass, 'alarm'::regclass, 'entity_alarm'::regclass)", Long.class);
        assertThat(distinctColocationGroups).isEqualTo(1L);

        // the primary keys were widened to lead with originator_id (Citus requires the distribution column in the PK).
        assertThat(constraintDef("alarm_pkey")).contains("originator_id", "id");
        assertThat(constraintDef("entity_alarm_pkey")).contains("originator_id", "entity_id", "alarm_id");

        // the entity_alarm -> alarm FK ended up as the co-located composite form with ON DELETE CASCADE, NOT the
        // old single-column (alarm_id) -> alarm(id) def (which would be invalid against the widened alarm PK).
        String fkDef = constraintDef("fk_entity_alarm_id");
        assertThat(fkDef)
                .contains("FOREIGN KEY (originator_id, alarm_id)")
                .contains("REFERENCES alarm(originator_id, id)")
                .contains("ON DELETE CASCADE");
    }

    private String constraintDef(String constraintName) {
        return jdbcTemplate.queryForObject(
                "select pg_get_constraintdef(oid) from pg_constraint where conname = ?", String.class, constraintName);
    }

    @Test
    void otaPackageBecomesReferenceWithFkPreservedAndLargeObjectReadable() {
        UUID otaId = UUID.randomUUID();
        Long oid = jdbcTemplate.queryForObject("SELECT lo_from_bytea(0, ?::bytea)", Long.class, new byte[]{(byte) 0xde, (byte) 0xad});
        jdbcTemplate.update("INSERT INTO ota_package (id, data) VALUES (?, ?)", otaId, oid);
        jdbcTemplate.update("INSERT INTO device_profile (id, firmware_id) VALUES (?, ?)", UUID.randomUUID(), otaId);

        schemaService.applyDistribution();

        String otaKind = jdbcTemplate.queryForObject(
                "select partmethod from pg_dist_partition where logicalrelid = 'ota_package'::regclass", String.class);
        assertThat(otaKind).isEqualTo("n");

        Integer fkCount = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.table_constraints " +
                        "where constraint_name = 'fk_firmware_device_profile' and constraint_type = 'FOREIGN KEY'", Integer.class);
        assertThat(fkCount).isEqualTo(1);

        Long oidAfter = jdbcTemplate.queryForObject("SELECT data FROM ota_package WHERE id = ?", Long.class, otaId);
        byte[] bytes = jdbcTemplate.queryForObject("SELECT lo_get(?)", byte[].class, oidAfter);
        assertThat(bytes).containsExactly((byte) 0xde, (byte) 0xad);
    }

    @Test
    void failsLoudlyOnPartialDistributionStateInsteadOfResuming() {
        // Simulate a partial prior run: ONLY one managed table got distributed before a crash, while the rest
        // (and the reference tables) remain local. applyDistribution() is deliberately NOT crash-safe — a genuine
        // partial run would have already dropped every managed foreign key up front and only re-adds them at the
        // very end, so silently resuming would skip FK re-capture on the already-distributed tables and permanently
        // lose those foreign keys. The production guard detects this exact "some managed tables distributed, some
        // not" signature and fails loudly rather than auto-replaying the partial run.
        jdbcTemplate.execute("SELECT create_distributed_table('attribute_kv', 'entity_id', shard_count => 8)");

        assertThatThrownBy(() -> schemaService.applyDistribution())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("partial/inconsistent state")
                .hasMessageContaining("attribute_kv");

        // The guard must short-circuit before touching the remaining tables: ts_kv_latest stays local (not distributed).
        Integer tsPartitioned = jdbcTemplate.queryForObject(
                "select count(*) from pg_dist_partition where logicalrelid = 'ts_kv_latest'::regclass", Integer.class);
        assertThat(tsPartitioned).isZero();
    }

    @Test
    void isIdempotentOnSecondRun() {
        schemaService.applyDistribution();
        schemaService.applyDistribution(); // must not throw
        Integer kv = jdbcTemplate.queryForObject(
                "select count(*) from pg_dist_partition where logicalrelid = 'attribute_kv'::regclass", Integer.class);
        assertThat(kv).isEqualTo(1);
    }

    @Test
    void createsActiveAlarmIndexOnFullyDistributedReRun() {
        schemaService.applyDistribution();
        assertThat(indexCount("idx_alarm_tenant_created_time_active")).isEqualTo(1);

        // A cluster converted by an earlier build hits applyDistribution()'s fully-distributed early return, so an
        // index introduced later only reaches it if it is created above that return. Dropping the index and
        // re-running reproduces exactly that: every managed table is already distributed, nothing is converted.
        jdbcTemplate.execute("DROP INDEX idx_alarm_tenant_created_time_active");
        assertThat(indexCount("idx_alarm_tenant_created_time_active")).isZero();

        schemaService.applyDistribution();
        assertThat(indexCount("idx_alarm_tenant_created_time_active")).isEqualTo(1);
    }

    @Test
    void reAddsManagedForeignKeyLostToCrashOnFullyDistributedReRun() {
        schemaService.applyDistribution();

        // Simulate the crash gap: every managed table is already distributed, but one managed foreign key was
        // dropped before its re-add ALTER (its own autocommit) ran during the prior run, so it is gone from the
        // catalog with no live source to re-capture it from. The guard cannot catch this (nothing is still local).
        jdbcTemplate.execute("ALTER TABLE device DROP CONSTRAINT fk_firmware_device");
        assertThat(fkCount("fk_firmware_device")).isZero();

        // A re-run must detect the missing managed FK (re-derived from the static catalog) and re-add it.
        schemaService.applyDistribution();
        assertThat(fkCount("fk_firmware_device")).isEqualTo(1);
        // the FK that survived must not be duplicated
        assertThat(fkCount("fk_firmware_device_profile")).isEqualTo(1);
    }

    @Test
    void reAddsManagedForeignKeyLostToCrashBeforeAnyDistribution() {
        // Simulate the other crash window: the prior run dropped the managed FKs up front (and here also the
        // single-column entity_alarm -> alarm FK) and crashed before the first successful conversion, so NOTHING
        // is distributed. The partial-state guard cannot fire (it needs at least one distributed managed table)
        // and the re-run's own drop phase finds nothing to capture — without the start-of-run repair the managed
        // FK would be silently lost for good. The composite entity_alarm FK repair must be skipped at the start
        // (alarm's PK is not widened yet, so the composite form is unaddable) and rebuilt at the end as usual.
        jdbcTemplate.execute("ALTER TABLE device DROP CONSTRAINT fk_firmware_device");
        jdbcTemplate.execute("ALTER TABLE entity_alarm DROP CONSTRAINT fk_entity_alarm_id");
        assertThat(fkCount("fk_firmware_device")).isZero();

        schemaService.applyDistribution();

        assertThat(fkCount("fk_firmware_device")).isEqualTo(1);
        // the FK that survived must not be duplicated
        assertThat(fkCount("fk_firmware_device_profile")).isEqualTo(1);
        assertThat(constraintDef("fk_entity_alarm_id")).contains("FOREIGN KEY (originator_id, alarm_id)");
    }

    @Test
    void repairsCompositeEntityAlarmForeignKeyWhenCrashHappenedAfterPrimaryKeyRewrite() {
        // Deeper into the same window: the prior run also got through the alarm-group PK rewrite before crashing,
        // so the composite FK's referenced key alarm(originator_id, id) already exists. The start-of-run repair
        // must handle this state too (re-adding the composite FK is legal here), and the rest of the run must
        // still complete and leave the composite FK in place.
        jdbcTemplate.execute("ALTER TABLE entity_alarm DROP CONSTRAINT fk_entity_alarm_id");
        jdbcTemplate.execute("ALTER TABLE alarm DROP CONSTRAINT alarm_pkey");
        jdbcTemplate.execute("ALTER TABLE alarm ADD CONSTRAINT alarm_pkey PRIMARY KEY (originator_id, id)");
        jdbcTemplate.execute("ALTER TABLE entity_alarm DROP CONSTRAINT entity_alarm_pkey");
        jdbcTemplate.execute("ALTER TABLE entity_alarm ADD CONSTRAINT entity_alarm_pkey PRIMARY KEY (originator_id, entity_id, alarm_id)");

        schemaService.applyDistribution();

        assertThat(constraintDef("fk_entity_alarm_id"))
                .contains("FOREIGN KEY (originator_id, alarm_id)")
                .contains("REFERENCES alarm(originator_id, id)")
                .contains("ON DELETE CASCADE");
        String entityAlarmKind = jdbcTemplate.queryForObject(
                "select partmethod from pg_dist_partition where logicalrelid = 'entity_alarm'::regclass", String.class);
        assertThat(entityAlarmKind).isEqualTo("h");
    }

    @Test
    void preflightFailsOnNullAlarmOriginatorInsteadOfFailingMidConversion() {
        // A pre-4.3.1.4 database: alarm rows may carry NULL originator_id, which would make the widened
        // ADD PRIMARY KEY (originator_id, id) fail after the FKs were already dropped. The pre-flight must fail
        // loudly before touching anything and point the operator at the upgrade that fixes the data.
        jdbcTemplate.update("INSERT INTO alarm (id, originator_id) VALUES (?, NULL)", UUID.randomUUID());

        assertThatThrownBy(() -> schemaService.applyDistribution())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("alarm")
                .hasMessageContaining("4.3.1.4");

        // fails before converting (or dropping) anything
        assertThat(distributedCount("attribute_kv")).isZero();
        assertThat(fkCount("fk_firmware_device")).isEqualTo(1);
    }

    @Test
    void preflightFailsOnNullEntityAlarmOriginatorInsteadOfFailingMidConversion() {
        // entity_alarm.originator_id is NULL on every row until the 4.3.1.4 backfill has run.
        UUID alarmId = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO alarm (id, originator_id) VALUES (?, ?)", alarmId, UUID.randomUUID());
        jdbcTemplate.update("INSERT INTO entity_alarm (entity_id, originator_id, alarm_id) VALUES (?, NULL, ?)",
                UUID.randomUUID(), alarmId);

        assertThatThrownBy(() -> schemaService.applyDistribution())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("entity_alarm")
                .hasMessageContaining("4.3.1.4");

        assertThat(distributedCount("attribute_kv")).isZero();
    }

    @Test
    void preflightFailsOnMissingEntityAlarmOriginatorColumnInsteadOfSqlError() {
        // A genuinely pre-4.3.1.4 database: entity_alarm has NO originator_id column at all (4.3.1.4 is the
        // upgrade that adds it). The pre-flight must treat the missing column as the strongest possible
        // "upgrade never ran" signal and fail with the same operator-friendly message — without the column
        // check the NULL-originator probe would blow up with a raw "column does not exist" SQL error instead.
        jdbcTemplate.execute("ALTER TABLE entity_alarm DROP COLUMN originator_id");

        assertThatThrownBy(() -> schemaService.applyDistribution())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("entity_alarm")
                .hasMessageContaining("no originator_id column")
                .hasMessageContaining("4.3.1.4");

        // fails before converting (or dropping) anything
        assertThat(distributedCount("attribute_kv")).isZero();
        assertThat(fkCount("fk_firmware_device")).isEqualTo(1);
    }

    @Test
    void doesNotDuplicateManagedForeignKeyPresentUnderDifferentName() {
        schemaService.applyDistribution();

        // Same FK, different (historical/auto-generated) name: the start-of-run crash repair must recognize it as
        // structurally present — a name-keyed presence check would see the catalog name as missing and add a
        // DUPLICATE constraint next to it (a long ACCESS EXCLUSIVE validation on a big table, then two identical
        // FKs forever).
        jdbcTemplate.execute("ALTER TABLE device DROP CONSTRAINT fk_firmware_device");
        jdbcTemplate.execute("ALTER TABLE device ADD CONSTRAINT device_firmware_id_fkey " +
                "FOREIGN KEY (firmware_id) REFERENCES ota_package(id)");

        schemaService.applyDistribution();

        Integer deviceForeignKeys = jdbcTemplate.queryForObject(
                "select count(*) from pg_constraint con " +
                        "where con.contype = 'f' and con.conrelid = 'device'::regclass", Integer.class);
        assertThat(deviceForeignKeys).isEqualTo(1);
        assertThat(fkCount("device_firmware_id_fkey")).isEqualTo(1);
        assertThat(fkCount("fk_firmware_device")).isZero();
    }

    @Test
    void preflightFailsOnLegacyAlarmCommentForeignKeyInsteadOfFailingMidConversion() {
        // The legacy alarm_comment -> alarm FK (dropped by the 4.3.1.4 upgrade) depends on the single-column
        // alarm_pkey, so DROP CONSTRAINT alarm_pkey would fail mid-conversion — after the managed FKs were already
        // dropped. alarm_comment is not a managed table, so the conversion itself never drops this FK.
        jdbcTemplate.execute("CREATE TABLE alarm_comment (id uuid primary key, alarm_id uuid, " +
                "CONSTRAINT fk_alarm_comment_alarm_id FOREIGN KEY (alarm_id) REFERENCES alarm(id) ON DELETE CASCADE)");

        assertThatThrownBy(() -> schemaService.applyDistribution())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("alarm_comment")
                .hasMessageContaining("4.3.1.4");

        assertThat(distributedCount("attribute_kv")).isZero();
        assertThat(fkCount("fk_firmware_device")).isEqualTo(1);
    }

    private Integer distributedCount(String table) {
        return jdbcTemplate.queryForObject(
                "select count(*) from pg_dist_partition where logicalrelid = ?::regclass", Integer.class, table);
    }

    private Integer fkCount(String constraintName) {
        return jdbcTemplate.queryForObject(
                "select count(*) from information_schema.table_constraints " +
                        "where constraint_name = ? and constraint_type = 'FOREIGN KEY'", Integer.class, constraintName);
    }

    private Integer indexCount(String indexName) {
        return jdbcTemplate.queryForObject(
                "select count(*) from pg_indexes where indexname = ?", Integer.class, indexName);
    }
}
