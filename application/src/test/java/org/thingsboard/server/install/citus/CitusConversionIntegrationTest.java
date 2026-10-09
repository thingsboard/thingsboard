// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.install.citus;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.thingsboard.server.dao.sql.citus.AbstractCitusContainerTest;
import org.thingsboard.server.dao.sql.citus.CitusTables;
import org.thingsboard.server.dao.sql.citus.DefaultCitusSchemaService;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Application-module integration test holding the Citus-conversion scenarios that are NOT reproducible through
 * the real DAO / service stack (the {@code @CitusDaoSqlTest} harness and the dao module's
 * {@code DefaultCitusSchemaServiceTest}).
 * <p>
 * Everything those real tests already cover has been trimmed away. The two scenarios kept here are:
 * <ul>
 *   <li>{@link #convertsPopulatedSchemaPreservingDataAndIsIdempotent} — data survival of rows seeded into the
 *       DISTRIBUTED {@code attribute_kv}/{@code ts_kv_latest} tables BEFORE {@code applyDistribution()} runs.
 *       {@code DefaultCitusSchemaServiceTest} seeds only reference tables (no re-shard); only this test proves the
 *       blocking {@code create_distributed_table} re-shard preserves pre-existing KV rows.</li>
 *   <li>{@link #integrationInfoViewQueryableUnderNewTopology} — the {@code integration_info} view under the new
 *       topology, plus the proof that native PARTITIONED tables stay coordinator-local and queryable. No
 *       {@code @CitusDaoSqlTest} variant exercises {@code integration_info}, so this is the only Citus-cluster
 *       coverage of that view.</li>
 * </ul>
 */
class CitusConversionIntegrationTest extends AbstractCitusContainerTest {

    private static final String CONVERSION_SCHEMA = "citus_conversion";

    @BeforeAll
    static void isolateSchema() {
        // Build and distribute the minimal fixtures inside a dedicated schema, with the class connection's
        // search_path pinned to it, so the distributed attribute_kv/device/... tables never land in the shared
        // coordinator's public schema that sibling Citus tests read. The whole schema is dropped in @AfterAll.
        jdbcTemplate.execute("DROP SCHEMA IF EXISTS " + CONVERSION_SCHEMA + " CASCADE");
        jdbcTemplate.execute("CREATE SCHEMA " + CONVERSION_SCHEMA);
        jdbcTemplate.execute("SET search_path TO " + CONVERSION_SCHEMA);
    }

    @AfterAll
    static void dropSchema() {
        if (jdbcTemplate != null) {
            // DROP SCHEMA CASCADE issues Citus-aware DROP TABLEs, cleaning up shards on the worker as well, so no
            // leftover distributed tables poison siblings.
            jdbcTemplate.execute("DROP SCHEMA IF EXISTS " + CONVERSION_SCHEMA + " CASCADE");
            jdbcTemplate.execute("SET search_path TO public");
        }
    }

    @Test
    void convertsPopulatedSchemaPreservingDataAndIsIdempotent() {
        // 1. minimal populated schema
        jdbcTemplate.execute("DROP TABLE IF EXISTS attribute_kv, ts_kv_latest, device, asset, entity_view, device_profile, tenant, ota_package, entity_alarm, alarm CASCADE");
        jdbcTemplate.execute("CREATE TABLE tenant (id uuid primary key)");
        jdbcTemplate.execute("CREATE TABLE ota_package (id uuid primary key, data oid)");
        jdbcTemplate.execute("CREATE TABLE device_profile (id uuid primary key, firmware_id uuid, " +
                "CONSTRAINT fk_firmware_device_profile FOREIGN KEY (firmware_id) REFERENCES ota_package(id))");
        jdbcTemplate.execute("CREATE TABLE device (id uuid primary key, firmware_id uuid, " +
                "CONSTRAINT fk_firmware_device FOREIGN KEY (firmware_id) REFERENCES ota_package(id))");
        // asset and entity_view are part of the real DISTRIBUTED_TABLES set (hash-distributed on id), so they must
        // exist before applyDistribution() iterates that set; they carry no FKs relevant to this conversion test.
        jdbcTemplate.execute("CREATE TABLE asset (id uuid primary key)");
        jdbcTemplate.execute("CREATE TABLE entity_view (id uuid primary key)");
        // alarm and entity_alarm are also in the real DISTRIBUTED_TABLES set (hash-distributed on originator_id);
        // created with the plain-Postgres PKs and single-column FK from schema-entities.sql, which the conversion
        // rewrites to the originator_id-leading forms.
        // alarm also carries tenant_id, created_time and cleared, mirrored from schema-entities.sql:
        // ensureCitusOnlyIndexes() builds a partial index over those three columns on every run, above
        // applyDistribution()'s early return, so the fixture has to provide them.
        jdbcTemplate.execute("CREATE TABLE alarm (id uuid NOT NULL CONSTRAINT alarm_pkey PRIMARY KEY, originator_id uuid, " +
                "tenant_id uuid, created_time bigint NOT NULL, cleared boolean)");
        jdbcTemplate.execute("CREATE TABLE entity_alarm (entity_id uuid NOT NULL, originator_id uuid, alarm_id uuid, " +
                "CONSTRAINT entity_alarm_pkey PRIMARY KEY (entity_id, alarm_id), " +
                "CONSTRAINT fk_entity_alarm_id FOREIGN KEY (alarm_id) REFERENCES alarm(id) ON DELETE CASCADE)");
        jdbcTemplate.execute("CREATE TABLE attribute_kv (entity_id uuid, attribute_type int, attribute_key int, version bigint, " +
                "CONSTRAINT attribute_kv_pkey PRIMARY KEY (entity_id, attribute_type, attribute_key))");
        jdbcTemplate.execute("CREATE TABLE ts_kv_latest (entity_id uuid, key int, version bigint, " +
                "CONSTRAINT ts_kv_latest_pkey PRIMARY KEY (entity_id, key))");

        // 2. seed rows into the KV tables that will be re-sharded by the conversion
        int attributeRows = 5;
        for (int i = 0; i < attributeRows; i++) {
            jdbcTemplate.update("INSERT INTO attribute_kv (entity_id, attribute_type, attribute_key, version) VALUES (?, ?, ?, ?)",
                    UUID.randomUUID(), 1, i, (long) i);
        }
        int tsRows = 3;
        for (int i = 0; i < tsRows; i++) {
            jdbcTemplate.update("INSERT INTO ts_kv_latest (entity_id, key, version) VALUES (?, ?, ?)",
                    UUID.randomUUID(), i, (long) i);
        }

        Integer seededAttributeCount = jdbcTemplate.queryForObject("select count(*) from attribute_kv", Integer.class);
        Integer seededTsCount = jdbcTemplate.queryForObject("select count(*) from ts_kv_latest", Integer.class);
        assertThat(seededAttributeCount).isEqualTo(attributeRows);
        assertThat(seededTsCount).isEqualTo(tsRows);

        // 3. build the schema service, limiting the reference set to the tables present in this schema
        DefaultCitusSchemaService schemaService = new DefaultCitusSchemaService(jdbcTemplate, newCitusSettings(8)) {
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

        // 4. convert the populated schema in place. device is hash-distributed (real DISTRIBUTED_TABLES);
        // its FK into ota_package is dropped before the reference tables are created and re-added afterwards.
        schemaService.applyDistribution();

        // 5a. attribute_kv must now be hash-distributed
        assertThat(partMethod("attribute_kv")).isEqualTo("h");

        // 5b. existing data must survive the blocking re-shard
        Integer attributeCountAfter = jdbcTemplate.queryForObject("select count(*) from attribute_kv", Integer.class);
        Integer tsCountAfter = jdbcTemplate.queryForObject("select count(*) from ts_kv_latest", Integer.class);
        assertThat(attributeCountAfter).isEqualTo(seededAttributeCount);
        assertThat(tsCountAfter).isEqualTo(seededTsCount);

        // 6. idempotency: a second run must not throw and must not duplicate distribution metadata
        assertThatCode(schemaService::applyDistribution).doesNotThrowAnyException();
        Integer kvPartitionRows = jdbcTemplate.queryForObject(
                "select count(*) from pg_dist_partition where logicalrelid = 'attribute_kv'::regclass", Integer.class);
        assertThat(kvPartitionRows).isEqualTo(1);

        // data still intact after the idempotent re-run
        Integer attributeCountFinal = jdbcTemplate.queryForObject("select count(*) from attribute_kv", Integer.class);
        Integer tsCountFinal = jdbcTemplate.queryForObject("select count(*) from ts_kv_latest", Integer.class);
        assertThat(attributeCountFinal).isEqualTo(seededAttributeCount);
        assertThat(tsCountFinal).isEqualTo(seededTsCount);
    }

    /**
     * Validates that the production {@code integration_info} view in {@code dao/src/main/resources/sql/schema-views.sql}
     * remains Citus-LEGAL and QUERYABLE under the NEW topology (device distributed-on-id, dimension tables reference),
     * and that native PARTITIONED tables stay coordinator-local yet queryable.
     * <p>
     * No {@code @CitusDaoSqlTest} variant exercises {@code integration_info} (there is no Citus IntegrationService /
     * IntegrationInfoDao test), so this is the only Citus-cluster coverage of that view. {@code integration_info} is a
     * {@code DISTINCT ON ... entity_id IN (select id from integration)} subquery against the distributed
     * {@code attribute_kv} table, integration being a reference table — the Citus-safe shape the other {@code *_info}
     * views (alarm_info, device_info) mirror. The alarm_info / device_info originator-resolution and co-located-join
     * shapes are already covered by {@code CitusAlarmControllerTest} / {@code CitusDeviceServiceTest}, so they are not
     * re-transcribed here.
     */
    @Test
    void integrationInfoViewQueryableUnderNewTopology() {
        jdbcTemplate.execute("DROP VIEW IF EXISTS integration_info CASCADE");
        jdbcTemplate.execute("DROP TABLE IF EXISTS attribute_kv, ts_kv_latest, device, asset, entity_view, " +
                "integration, key_dictionary, report, blob_entity, entity_alarm, alarm CASCADE");

        // --- the hash-distributed tables: attribute_kv (anchor) + ts_kv_latest + device (on id) ---
        // attribute_kv columns referenced by integration_info: entity_id, attribute_type, attribute_key,
        // last_update_ts, json_v, version.
        jdbcTemplate.execute("CREATE TABLE attribute_kv (entity_id uuid, attribute_type int, attribute_key int, " +
                "bool_v boolean, json_v json, last_update_ts bigint, version bigint, " +
                "CONSTRAINT attribute_kv_pkey PRIMARY KEY (entity_id, attribute_type, attribute_key))");
        jdbcTemplate.execute("CREATE TABLE ts_kv_latest (entity_id uuid, key int, version bigint, " +
                "CONSTRAINT ts_kv_latest_pkey PRIMARY KEY (entity_id, key))");
        jdbcTemplate.execute("CREATE TABLE device (id uuid PRIMARY KEY, tenant_id uuid, name varchar)");
        // asset and entity_view are part of the real DISTRIBUTED_TABLES set (hash-distributed on id), so they must
        // exist before applyDistribution() iterates that set, even though integration_info itself does not reference them.
        jdbcTemplate.execute("CREATE TABLE asset (id uuid PRIMARY KEY, tenant_id uuid, name varchar)");
        jdbcTemplate.execute("CREATE TABLE entity_view (id uuid PRIMARY KEY, tenant_id uuid, name varchar)");
        // alarm and entity_alarm are also in the real DISTRIBUTED_TABLES set (hash-distributed on originator_id),
        // created with the plain-Postgres PKs and single-column FK that the conversion rewrites.
        // alarm also carries tenant_id, created_time and cleared, mirrored from schema-entities.sql:
        // ensureCitusOnlyIndexes() builds a partial index over those three columns on every run, above
        // applyDistribution()'s early return, so the fixture has to provide them.
        jdbcTemplate.execute("CREATE TABLE alarm (id uuid NOT NULL CONSTRAINT alarm_pkey PRIMARY KEY, originator_id uuid, " +
                "tenant_id uuid, created_time bigint NOT NULL, cleared boolean)");
        jdbcTemplate.execute("CREATE TABLE entity_alarm (entity_id uuid NOT NULL, originator_id uuid, alarm_id uuid, " +
                "CONSTRAINT entity_alarm_pkey PRIMARY KEY (entity_id, alarm_id), " +
                "CONSTRAINT fk_entity_alarm_id FOREIGN KEY (alarm_id) REFERENCES alarm(id) ON DELETE CASCADE)");

        // --- reference (dimension) tables ---
        jdbcTemplate.execute("CREATE TABLE key_dictionary (key varchar(255) NOT NULL, key_id serial, " +
                "CONSTRAINT key_dictionary_id_pkey PRIMARY KEY (key_id), CONSTRAINT key_dictionary_key_unq_key UNIQUE (key))");
        // integration: integration_info does SELECT i.<many cols>; reference table, FK target of the IN-subquery.
        jdbcTemplate.execute("CREATE TABLE integration (id uuid PRIMARY KEY, created_time bigint, tenant_id uuid, " +
                "name varchar, type varchar, debug_settings varchar, enabled boolean, is_remote boolean, " +
                "allow_create_devices_or_assets boolean, is_edge_template boolean)");

        // --- native partitioned tables that must remain coordinator-local (Citus cannot make these reference) ---
        jdbcTemplate.execute("CREATE TABLE report (id uuid NOT NULL, created_time bigint NOT NULL, name varchar) " +
                "PARTITION BY RANGE (created_time)");
        jdbcTemplate.execute("CREATE TABLE report_0 PARTITION OF report FOR VALUES FROM (0) TO (1000)");
        jdbcTemplate.execute("CREATE TABLE blob_entity (id uuid NOT NULL, created_time bigint NOT NULL, name varchar) " +
                "PARTITION BY RANGE (created_time)");
        jdbcTemplate.execute("CREATE TABLE blob_entity_0 PARTITION OF blob_entity FOR VALUES FROM (0) TO (1000)");
        jdbcTemplate.update("INSERT INTO report (id, created_time, name) VALUES (?, ?, ?)", UUID.randomUUID(), 1L, "r");
        jdbcTemplate.update("INSERT INTO blob_entity (id, created_time, name) VALUES (?, ?, ?)", UUID.randomUUID(), 1L, "b");

        // --- apply the NEW topology: real DISTRIBUTED_TABLES (device-on-id colocation runs); dimension tables
        // converted to reference. ---
        DefaultCitusSchemaService schemaService = new DefaultCitusSchemaService(jdbcTemplate, newCitusSettings(8)) {
            @Override
            protected List<String> referenceTables() {
                return List.of("key_dictionary", "integration");
            }

            @Override
            protected List<CitusTables.ManagedForeignKey> managedForeignKeys() {
                // this minimal fixture schema carries no managed foreign keys at all
                return List.of();
            }
        };
        schemaService.applyDistribution();

        // sanity: device is distributed and co-located with the attribute_kv anchor
        assertThat(partMethod("device")).isEqualTo("h");
        assertThat(colocationId("device")).isEqualTo(colocationId("attribute_kv"));

        // partitioned tables stay coordinator-local: absent from Citus metadata, still queryable
        Integer localMeta = jdbcTemplate.queryForObject(
                "select count(*) from pg_dist_partition where logicalrelid in " +
                        "('report'::regclass, 'blob_entity'::regclass)", Integer.class);
        assertThat(localMeta).isZero();
        // exactly the one row seeded into each table above: proves the seeded data survived the conversion and
        // remains readable on the coordinator-local partitioned tables
        assertThat(jdbcTemplate.queryForObject("select count(*) from blob_entity", Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("select count(*) from report", Integer.class)).isEqualTo(1);

        // --- create the production integration_info view from its REAL definition in schema-views.sql ---
        // (read off the classpath rather than transcribed, so the test cannot drift from the shipped DDL).
        jdbcTemplate.execute(readIntegrationInfoViewDdl());

        // Seed one enabled integration plus two integration_status_% attributes for it with DIFFERENT
        // last_update_ts values, so the view's DISTINCT ON (entity_id) ORDER BY last_update_ts DESC must resolve
        // status to the NEWEST one. The newest value deliberately carries the LOWER key_id and is inserted first,
        // so neither attribute_key order nor insertion order can accidentally produce the right answer.
        UUID integrationId = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO integration (id, created_time, tenant_id, name, type, enabled) " +
                "VALUES (?, ?, ?, ?, ?, ?)", integrationId, 1L, UUID.randomUUID(), "int", "MQTT", true);

        int newestStatusKeyId = 101;
        int olderStatusKeyId = 102;
        jdbcTemplate.update("INSERT INTO key_dictionary (key, key_id) VALUES (?, ?)", "integration_status_new", newestStatusKeyId);
        jdbcTemplate.update("INSERT INTO key_dictionary (key, key_id) VALUES (?, ?)", "integration_status_old", olderStatusKeyId);

        // attribute_type = 2 is the SERVER_SCOPE bucket the view filters on; both rows share the integration's id as
        // entity_id and differ only by attribute_key. cast(json_v as varchar) renders a json string with its quotes.
        String newestStatus = "\"ACTIVE\"";
        String olderStatus = "\"CONNECTING\"";
        jdbcTemplate.update("INSERT INTO attribute_kv (entity_id, attribute_type, attribute_key, json_v, last_update_ts) " +
                "VALUES (?, 2, ?, cast(? as json), ?)", integrationId, newestStatusKeyId, newestStatus, 2000L);
        jdbcTemplate.update("INSERT INTO attribute_kv (entity_id, attribute_type, attribute_key, json_v, last_update_ts) " +
                "VALUES (?, 2, ?, cast(? as json), ?)", integrationId, olderStatusKeyId, olderStatus, 1000L);

        // The view must return the NEWEST attribute's value (max last_update_ts), proving its DISTINCT ON / ORDER BY
        // contract holds under the new Citus topology — and, incidentally, that it is legal and queryable there.
        String status = jdbcTemplate.queryForObject(
                "select status from integration_info where id = ?", String.class, integrationId);
        assertThat(status).isEqualTo(newestStatus);
    }

    /**
     * The {@code CREATE OR REPLACE VIEW integration_info ...} statement extracted from the shipped
     * {@code dao/src/main/resources/sql/schema-views.sql} (the block from its {@code CREATE OR REPLACE VIEW} up to
     * the terminating semicolon), so the test exercises the real production DDL rather than a transcribed copy.
     */
    private static String readIntegrationInfoViewDdl() {
        String views = readSchemaViewsSql();
        String marker = "CREATE OR REPLACE VIEW integration_info";
        int start = views.indexOf(marker);
        if (start < 0) {
            throw new IllegalStateException("integration_info view not found in schema-views.sql");
        }
        int end = views.indexOf(';', start);
        if (end < 0) {
            throw new IllegalStateException("Unterminated integration_info view definition in schema-views.sql");
        }
        return views.substring(start, end);
    }

    private static String readSchemaViewsSql() {
        try (InputStream is = CitusConversionIntegrationTest.class.getResourceAsStream("/sql/schema-views.sql")) {
            if (is == null) {
                throw new IllegalStateException("Could not find /sql/schema-views.sql on the test classpath");
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
