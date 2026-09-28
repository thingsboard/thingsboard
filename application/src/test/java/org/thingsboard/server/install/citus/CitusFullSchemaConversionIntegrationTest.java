// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.install.citus;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.thingsboard.server.dao.PostgreSqlInitializer;
import org.thingsboard.server.dao.sql.citus.AbstractCitusContainerTest;
import org.thingsboard.server.dao.sql.citus.DefaultCitusSchemaService;

import java.sql.Connection;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Full-real-schema counterpart to {@link CitusConversionIntegrationTest}: instead of a hand-rolled minimal
 * schema this test installs the REAL production schema (entities + indexes + views + functions via
 * {@link PostgreSqlInitializer#initDb(Connection)}) on a real Citus cluster and runs the REAL production
 * {@link DefaultCitusSchemaService#applyDistribution()} (default {@code CitusTables} membership, no overrides),
 * then asserts the conversion preserves the tricky foreign-key topology that a minimal schema cannot reproduce.
 * <p>
 * It covers the PR-review findings that the conversion must survive:
 * <ol>
 *   <li><b>device_profile &lt;-&gt; ota_package FK cycle</b> — both are REFERENCE tables and mutually reference each
 *       other ({@code device_profile.firmware_id/software_id -> ota_package} and
 *       {@code ota_package.device_profile_id -> device_profile ON DELETE CASCADE}). No FK-target-before-source
 *       ordering exists, so the cycle must round-trip through the drop-all-FKs / create-reference / re-add-all-FKs
 *       flow.</li>
 *   <li><b>partitioned-child auto-capture</b> — {@code report} is a native PARTITIONED table (Citus rejects
 *       {@code create_reference_table} on it) that carries a FK into a reference table ({@code report_template}).
 *       Citus 12.1's {@code enable_local_reference_table_foreign_keys} auto-captures it as a Citus LOCAL table
 *       rather than leaving it as a plain coordinator-local Postgres table. It must be Citus-local (not
 *       distributed, not reference) and must keep its cascading FK action.</li>
 *   <li><b>alarm/entity_alarm distribution on originator_id</b> — both are distributed on {@code originator_id}
 *       and co-located with the anchor group ({@code attribute_kv}/…). Their primary keys are widened to lead
 *       with {@code originator_id}, and the {@code entity_alarm -> alarm} FK becomes the co-located composite
 *       {@code (originator_id, alarm_id) -> alarm(originator_id, id) ON DELETE CASCADE}. The partitioned
 *       {@code alarm_comment} has NO FK to alarm anymore, so Citus does not auto-track it: it stays a plain
 *       coordinator-local partitioned table (absent from {@code citus_tables}).</li>
 * </ol>
 * This is an expensive full-schema boot, so all assertions live in one {@link Test} method with clearly labelled
 * sub-sections.
 */
class CitusFullSchemaConversionIntegrationTest extends AbstractCitusContainerTest {

    private static final String CONVERSION_SCHEMA = "full_schema_conversion";

    @BeforeAll
    static void isolateSchema() {
        // Install and distribute the full schema inside a dedicated schema, with the class connection's search_path
        // pinned to it, so the entities/views/functions/system-data tables (and their distribution) never land in
        // the shared coordinator's public schema that sibling Citus tests read. The whole schema is dropped in
        // @AfterAll; the install SQL and DefaultCitusSchemaService are unqualified / current_schema()-scoped, so they
        // operate entirely within this namespace.
        jdbcTemplate.execute("DROP SCHEMA IF EXISTS " + CONVERSION_SCHEMA + " CASCADE");
        jdbcTemplate.execute("CREATE SCHEMA " + CONVERSION_SCHEMA);
        jdbcTemplate.execute("SET search_path TO " + CONVERSION_SCHEMA);
    }

    @AfterAll
    static void dropSchema() {
        if (jdbcTemplate != null) {
            // DROP SCHEMA CASCADE issues Citus-aware DROP TABLEs, cleaning up shards on the worker as well, so the
            // coordinator is left exactly as sibling tests expect: no leftover distributed tables in public.
            jdbcTemplate.execute("DROP SCHEMA IF EXISTS " + CONVERSION_SCHEMA + " CASCADE");
            jdbcTemplate.execute("SET search_path TO public");
        }
    }

    @Test
    void fullRealSchemaConversionPreservesFkTopology() throws SQLException {
        // 1. install the FULL real schema (entities + idx + views + functions + system data) into the isolated schema
        Connection conn = jdbcTemplate.getDataSource().getConnection();
        PostgreSqlInitializer.initDb(conn);

        // 2. run the REAL production distribution: default CitusTables membership, no test overrides
        DefaultCitusSchemaService schemaService = new DefaultCitusSchemaService(jdbcTemplate, newCitusSettings(4));
        schemaService.applyDistribution();

        // 3-6. the converted FK topology holds
        assertConvertedTopology();

        // 7. idempotency: a second run over the fully-applied schema is a harmless no-op (mirrors the sibling
        //    tests). The re-run takes a DIFFERENT code path (the fully-distributed early-return branch after the
        //    unconditional FK re-add/repair calls), so the topology is re-verified afterwards: a repair regression
        //    that, say, dropped a managed FK without re-adding it must fail here, not pass silently.
        assertThatCode(schemaService::applyDistribution).doesNotThrowAnyException();
        assertConvertedTopology();
    }

    private void assertConvertedTopology() {
        // ---------------------------------------------------------------------------------------------------
        // 3. device_profile <-> ota_package FK cycle survives, both as REFERENCE tables.
        //    partmethod 'n' == reference table (mirrors DefaultCitusSchemaServiceTest's reference-table check).
        // ---------------------------------------------------------------------------------------------------
        assertThat(partMethod("device_profile"))
                .as("device_profile must be a Citus reference table after conversion")
                .isEqualTo("n");
        assertThat(partMethod("ota_package"))
                .as("ota_package must be a Citus reference table after conversion")
                .isEqualTo("n");

        // device_profile -> ota_package (firmware/software): both re-added after the conversion
        assertThat(constraintExists("fk_firmware_device_profile"))
                .as("device_profile.firmware_id -> ota_package FK must survive the conversion").isTrue();
        assertThat(constraintExists("fk_software_device_profile"))
                .as("device_profile.software_id -> ota_package FK must survive the conversion").isTrue();

        // ota_package -> device_profile ON DELETE CASCADE: the back-edge of the cycle, re-added with its cascade.
        assertThat(constraintExists("fk_device_profile_ota_package"))
                .as("ota_package.device_profile_id -> device_profile FK must survive the conversion").isTrue();
        assertThat(constraintDef("fk_device_profile_ota_package"))
                .as("ota_package -> device_profile FK must keep its ON DELETE CASCADE action")
                .contains("ON DELETE CASCADE");

        // ---------------------------------------------------------------------------------------------------
        // 4. partitioned-child auto-capture: report (FK -> report_template) is a native PARTITIONED table that
        //    Citus 12.1 auto-captures as a LOCAL table via its FK into a reference table. It must NOT be
        //    distributed/reference, but MUST be present as a Citus local table. citus_tables.citus_table_type is
        //    the documented stable signal ('local' for Citus local tables); a plain coordinator-local Postgres
        //    table (no Citus capture) would be absent from citus_tables.
        //
        //    alarm_comment, by contrast, is a native PARTITIONED table with NO FK to alarm anymore (the FK was
        //    dropped from the base schema), so Citus does NOT auto-capture it: it stays a plain coordinator-local
        //    Postgres table, absent from citus_tables.
        // ---------------------------------------------------------------------------------------------------
        assertThat(citusTableType("report"))
                .as("report must be auto-captured as a Citus local table (FK into reference report_template)")
                .isEqualTo("local");
        assertThat(citusTableType("alarm_comment"))
                .as("alarm_comment must remain a plain coordinator-local table (no FK to alarm, not Citus-tracked)")
                .isNull();

        // and they must NOT have been distributed or made reference tables
        assertThat(partMethodOrNull("report"))
                .as("report must not be hash-distributed or a reference table").isNotEqualTo("h");
        assertThat(partMethodOrNull("alarm_comment"))
                .as("alarm_comment must not be hash-distributed or a reference table").isNotEqualTo("h");

        // alarm_comment no longer carries any FK to alarm (it was dropped from the base schema in an earlier task).
        assertThat(constraintExists("fk_alarm_comment_alarm_id"))
                .as("alarm_comment -> alarm FK must NOT exist (dropped from the base schema)").isFalse();

        // ---------------------------------------------------------------------------------------------------
        // 5. alarm/entity_alarm are distributed on originator_id and co-located with the anchor group; the
        //    entity_alarm -> alarm FK survives as the co-located composite form with ON DELETE CASCADE.
        // ---------------------------------------------------------------------------------------------------
        assertThat(partMethod("alarm"))
                .as("alarm must be hash-distributed on originator_id after conversion").isEqualTo("h");
        assertThat(partMethod("entity_alarm"))
                .as("entity_alarm must be hash-distributed on originator_id after conversion").isEqualTo("h");

        // co-located with the anchor attribute_kv (whole originator-id alarm group shares the anchor's colocationid).
        Long alarmColocationGroups = jdbcTemplate.queryForObject(
                "select count(distinct colocationid) from pg_dist_partition " +
                        "where logicalrelid in ('attribute_kv'::regclass, 'alarm'::regclass, 'entity_alarm'::regclass)",
                Long.class);
        assertThat(alarmColocationGroups)
                .as("alarm and entity_alarm must co-locate with the anchor attribute_kv").isEqualTo(1L);

        assertThat(constraintExists("fk_entity_alarm_id"))
                .as("entity_alarm -> alarm FK must survive the conversion").isTrue();
        assertThat(constraintDef("fk_entity_alarm_id"))
                .as("entity_alarm -> alarm FK must be the co-located composite (originator_id, alarm_id) -> " +
                        "alarm(originator_id, id) ON DELETE CASCADE")
                .contains("FOREIGN KEY (originator_id, alarm_id)")
                .contains("REFERENCES alarm(originator_id, id)")
                .contains("ON DELETE CASCADE");

        // ---------------------------------------------------------------------------------------------------
        // 6. cascade / set-null FK clauses survive on the auto-captured partitioned child report.
        // ---------------------------------------------------------------------------------------------------
        assertThat(constraintExists("fk_report_template"))
                .as("report -> report_template FK must survive the conversion").isTrue();
        assertThat(constraintDef("fk_report_template"))
                .as("report -> report_template FK must keep its ON DELETE SET NULL action")
                .contains("ON DELETE SET NULL");
    }

    /**
     * Like {@link #partMethod(String)} but returns {@code null} when the table is absent from pg_dist_partition
     * (a plain coordinator-local table). Used for the negative assertions on the partitioned children, which may
     * or may not appear in pg_dist_partition depending on how Citus 12.1 records local-table capture.
     */
    private String partMethodOrNull(String table) {
        return jdbcTemplate.query(
                "select partmethod from pg_dist_partition where logicalrelid = ?::regclass",
                rs -> rs.next() ? rs.getString(1) : null, table);
    }

    /**
     * Citus {@code citus_table_type} for the table ('distributed' / 'reference' / 'local'), or {@code null} when
     * the table is absent from {@code citus_tables} (i.e. a plain coordinator-local Postgres table that Citus
     * never captured). Returning null rather than throwing keeps the assertion message meaningful.
     */
    private String citusTableType(String table) {
        // citus_tables is a view the citus extension creates in the public schema; the class connection's
        // search_path is pinned to the isolated fixture schema (no public fallback), so the view must be
        // schema-qualified. The ?::regclass cast stays unqualified — it must resolve within the fixture schema.
        return jdbcTemplate.query(
                "select citus_table_type from public.citus_tables where table_name = ?::regclass",
                rs -> rs.next() ? rs.getString(1) : null, table);
    }

    // The pg_constraint probes are pinned to the fixture schema: search_path does not scope catalog queries, and
    // sibling Citus tests create identically-named FKs in their own schemas on the same shared coordinator, so an
    // unscoped conname lookup could false-pass (or make queryForObject throw on duplicates) if such a schema coexists.

    private boolean constraintExists(String constraintName) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from pg_constraint where conname = ? and contype = 'f' " +
                        "and connamespace = current_schema()::regnamespace", Integer.class, constraintName);
        return count != null && count > 0;
    }

    private String constraintDef(String constraintName) {
        return jdbcTemplate.queryForObject(
                "select pg_get_constraintdef(oid) from pg_constraint where conname = ? and contype = 'f' " +
                        "and connamespace = current_schema()::regnamespace",
                String.class, constraintName);
    }
}
