// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.install.lts;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.WidgetTypeId;
import org.thingsboard.server.common.data.id.WidgetsBundleId;
import org.thingsboard.server.common.data.widget.WidgetTypeDetails;
import org.thingsboard.server.common.data.widget.WidgetsBundle;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.dao.widget.WidgetTypeService;
import org.thingsboard.server.dao.widget.WidgetsBundleService;
import org.thingsboard.server.service.install.InstallScripts;
import org.thingsboard.server.service.install.SchemaViews;
import org.thingsboard.server.service.install.TbClusterSchema;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@DaoSqlTest
public class LtsMigrationIntegrationTest extends AbstractControllerTest {

    private static final long V_4_2_2_2 = 4_002_002_002L;
    private static final long V_4_2_2_3 = 4_002_002_003L;
    private static final long V_4_3_1_4 = 4_003_001_004L;
    private static final String OBSOLETE_ALIAS = "air_quality";
    private static final String SCHEMA_UPDATE_SQL = "schema_update.sql";

    // Versions with a registered bean but no schema_update.sql, because their whole change is programmatic:
    // 4.3.1.5 rewrites one JSON column through the DAO. Entries here opt out of the dir/bean check below,
    // so add one only when the version genuinely ships no SQL.
    private static final Set<String> SQL_LESS_ALLOWED = Set.of("4.3.1.5");

    @Autowired
    private LtsMigrationService ltsMigrationService;
    @Autowired
    private WidgetsBundleService widgetsBundleService;
    @Autowired
    private WidgetTypeService widgetTypeService;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private InstallScripts installScripts;
    @Autowired
    private List<LtsMigration> migrations;
    @Autowired
    private V4_3_1_4Migration v4314Migration;

    private Long originalSchemaVersion;
    private WidgetsBundleId bundleId;
    private WidgetTypeId widgetTypeId;

    @Before
    public void setUp() {
        // The test-context DB (DaoSqlTest) creates tb_schema_settings empty, so ensure the baseline
        // version row a real installed DB always has exists before driving the migration.
        originalSchemaVersion = jdbcTemplate.query("SELECT schema_version FROM tb_schema_settings",
                rs -> rs.next() ? rs.getLong(1) : null);
        if (originalSchemaVersion == null) {
            jdbcTemplate.execute("INSERT INTO tb_schema_settings (schema_version, product) VALUES (" + V_4_2_2_2 + ", 'CE')");
        }

        // Seed an obsolete system widget bundle with one (non-deprecated) widget type linked to it.
        WidgetsBundle bundle = new WidgetsBundle();
        bundle.setTenantId(TenantId.SYS_TENANT_ID);
        bundle.setAlias(OBSOLETE_ALIAS);
        bundle.setTitle("Air quality");
        bundleId = widgetsBundleService.saveWidgetsBundle(bundle).getId();

        WidgetTypeDetails type = new WidgetTypeDetails();
        type.setTenantId(TenantId.SYS_TENANT_ID);
        type.setFqn("air_quality_sample_" + UUID.randomUUID());
        type.setName("Air quality sample");
        type.setDescriptor(JacksonUtil.fromString("{ \"type\": \"latest\" }", JsonNode.class));
        WidgetTypeDetails saved = widgetTypeService.saveWidgetType(type);
        widgetTypeId = saved.getId();
        widgetTypeService.updateWidgetsBundleWidgetFqns(TenantId.SYS_TENANT_ID, bundleId, List.of(saved.getFqn()));

        // Pretend the DB is at 4.2.2.2 so the 4.2.2.3 migration is in range (4.2.2.2, 4.2.2.3].
        jdbcTemplate.execute("UPDATE tb_schema_settings SET schema_version = " + V_4_2_2_2);
    }

    @After
    public void tearDown() {
        // Restore the schema version (or drop the row we seeded) and clean up whatever the migration left behind.
        if (originalSchemaVersion != null) {
            jdbcTemplate.execute("UPDATE tb_schema_settings SET schema_version = " + originalSchemaVersion);
        } else {
            jdbcTemplate.execute("DELETE FROM tb_schema_settings");
        }
        if (widgetTypeId != null && widgetTypeService.findWidgetTypeDetailsById(TenantId.SYS_TENANT_ID, widgetTypeId) != null) {
            widgetTypeService.deleteWidgetType(TenantId.SYS_TENANT_ID, widgetTypeId);
        }
        WidgetsBundle bundle = widgetsBundleService.findWidgetsBundleByTenantIdAndAlias(TenantId.SYS_TENANT_ID, OBSOLETE_ALIAS);
        if (bundle != null) {
            widgetsBundleService.deleteWidgetsBundle(TenantId.SYS_TENANT_ID, bundle.getId());
        }
    }

    @Test
    public void appliesSqlDeprecatesTypesDeletesBundleAndRecordsVersion() {
        // The base test schema (schema-entities.sql) already ships iot_hub_installed_item, which would make the
        // creation assertion below vacuously true; drop it so this test proves the 4.2.2.3 SQL creates the table.
        jdbcTemplate.execute("DROP TABLE IF EXISTS iot_hub_installed_item");

        // applyMigrations returns the applied versions; recordVersions stamps them (see SystemPatchApplier for
        // where that call sits relative to its own sync work).
        ltsMigrationService.recordVersions(ltsMigrationService.applyMigrations("4.2.2.2", "4.2.2.3", false, () -> {}));

        // 1. The version's SQL ran: iot_hub_installed_item table now exists.
        assertTrue(tableExists("iot_hub_installed_item"));
        // 2. The version was recorded.
        assertEquals(Long.valueOf(V_4_2_2_3),
                jdbcTemplate.queryForObject("SELECT schema_version FROM tb_schema_settings", Long.class));
        // 3. The obsolete bundle entity was deleted.
        assertNull(widgetsBundleService.findWidgetsBundleByTenantIdAndAlias(TenantId.SYS_TENANT_ID, OBSOLETE_ALIAS));
        // 4. The widget type was KEPT and marked deprecated.
        WidgetTypeDetails type = widgetTypeService.findWidgetTypeDetailsById(TenantId.SYS_TENANT_ID, widgetTypeId);
        assertNotNull(type);
        assertTrue(type.isDeprecated());
    }

    @Test
    public void reRunFromCurrentVersionIsNoOp() {
        ltsMigrationService.recordVersions(ltsMigrationService.applyMigrations("4.2.2.2", "4.2.2.3", false, () -> {}));
        // Re-running from the now-current version selects an empty range — nothing changes, nothing throws.
        ltsMigrationService.applyMigrations("4.2.2.3", "4.2.2.3", false, () -> {});

        assertEquals(Long.valueOf(V_4_2_2_3),
                jdbcTemplate.queryForObject("SELECT schema_version FROM tb_schema_settings", Long.class));
        assertNull(widgetsBundleService.findWidgetsBundleByTenantIdAndAlias(TenantId.SYS_TENANT_ID, OBSOLETE_ALIAS));
        assertTrue(widgetTypeService.findWidgetTypeDetailsById(TenantId.SYS_TENANT_ID, widgetTypeId).isDeprecated());
    }

    @Test
    public void offlinePathRunsSchemaThenDataButRecordsNoVersion() {
        // The base test schema (schema-entities.sql) already ships iot_hub_installed_item, which would make the
        // creation assertion below vacuously true; drop it so this test proves the 4.2.2.3 SQL creates the table.
        jdbcTemplate.execute("DROP TABLE IF EXISTS iot_hub_installed_item");

        // Drive the offline major-upgrade path over the real supported range against the real DB.
        ltsMigrationService.runSchemaMigrations("4.2.2.2", "4.2.2.3", false);
        // (a) the schema effects landed: the table the 4.2.2.3 SQL creates now exists.
        assertTrue(tableExists("iot_hub_installed_item"));
        // (c) the offline schema phase records NO schema version -- the offline flow tracks the package version
        // itself instead (unlike the no-downtime path, which eventually records via recordVersions).
        assertEquals(Long.valueOf(V_4_2_2_2),
                jdbcTemplate.queryForObject("SELECT schema_version FROM tb_schema_settings", Long.class));

        ltsMigrationService.runDataMigrations("4.2.2.2", "4.2.2.3", false);
        // (b) the data apply() ran: the obsolete bundle was deleted and its type marked deprecated.
        assertNull(widgetsBundleService.findWidgetsBundleByTenantIdAndAlias(TenantId.SYS_TENANT_ID, OBSOLETE_ALIAS));
        WidgetTypeDetails type = widgetTypeService.findWidgetTypeDetailsById(TenantId.SYS_TENANT_ID, widgetTypeId);
        assertNotNull(type);
        assertTrue(type.isDeprecated());
        // (c) the offline data phase also records NO schema version.
        assertEquals(Long.valueOf(V_4_2_2_2),
                jdbcTemplate.queryForObject("SELECT schema_version FROM tb_schema_settings", Long.class));
    }

    @Test
    public void appliesSchemaForV4312AddsCalculatedFieldAdditionalInfo() {
        // The test-context schema already ships the 4.3.1.2 columns/constraint, so drop them all first to prove the
        // PE 4.3.1.2 schema_update.sql re-adds every one of them. PE's 4.3.1.2 schema does more than CE: it adds
        // api_usage_state.ai, calculated_field.additional_info, role.excluded_permissions, tb_user.external_id and
        // the tb_user_external_id_unq_key unique constraint (guarded by a DO $$ ... $$ block).
        jdbcTemplate.execute("ALTER TABLE tb_user DROP CONSTRAINT IF EXISTS tb_user_external_id_unq_key");
        jdbcTemplate.execute("ALTER TABLE api_usage_state DROP COLUMN IF EXISTS ai");
        jdbcTemplate.execute("ALTER TABLE calculated_field DROP COLUMN IF EXISTS additional_info");
        jdbcTemplate.execute("ALTER TABLE role DROP COLUMN IF EXISTS excluded_permissions");
        // user_info_view is defined as SELECT u.* FROM tb_user, so the freshly-built test schema captured
        // external_id into the view. Drop the view first (real upgrades never hit this: there the view predates
        // external_id), then recreate all views from schema-views.sql afterwards to leave the shared suite schema intact.
        jdbcTemplate.execute("DROP VIEW IF EXISTS user_info_view CASCADE");
        jdbcTemplate.execute("ALTER TABLE tb_user DROP COLUMN IF EXISTS external_id");

        assertFalse(columnExists("api_usage_state", "ai"));
        assertFalse(columnExists("calculated_field", "additional_info"));
        assertFalse(columnExists("role", "excluded_permissions"));
        assertFalse(columnExists("tb_user", "external_id"));
        assertFalse(constraintExists("tb_user_external_id_unq_key"));

        try {
            // Drive the runner over a range whose target (4.3.1.2) selects only the 4.3.1.2 migration.
            ltsMigrationService.applyMigrations("4.3.1.1", "4.3.1.2", false, () -> {});

            // The 4.3.1.2 schema SQL ran: every PE-specific column and the unique constraint exist again.
            assertTrue(columnExists("api_usage_state", "ai"));
            assertTrue(columnExists("calculated_field", "additional_info"));
            assertTrue(columnExists("role", "excluded_permissions"));
            assertTrue(columnExists("tb_user", "external_id"));
            assertTrue(constraintExists("tb_user_external_id_unq_key"));
        } finally {
            // Always recreate all views (even if an assertion above failed) so the shared DaoSqlTest suite schema
            // keeps user_info_view, which this test dropped CASCADE while reconstructing the pre-migration state.
            SchemaViews.recreate(jdbcTemplate);
        }
    }

    @Test
    public void appliesSchemaForV4314DropsAlarmCommentFkAddsAndBackfillsEntityAlarmOriginator() {
        // The test-context schema already ships the 4.3.1.4 end state (entity_alarm.originator_id present,
        // alarm_comment -> alarm FK absent), so reconstruct the pre-migration state first to prove the
        // 4.3.1.4 migration both drops the FK and adds + backfills the column.
        // Comment cleanup on alarm deletion is asynchronous now (Housekeeper), so an earlier test class in the
        // shared suite DB may have been torn down with alarm_comment rows whose alarm is already gone; the plain
        // ADD CONSTRAINT below validates every existing row, so such orphans must be removed first.
        jdbcTemplate.execute("DELETE FROM alarm_comment WHERE alarm_id NOT IN (SELECT id FROM alarm)");
        jdbcTemplate.execute("ALTER TABLE alarm_comment ADD CONSTRAINT fk_alarm_comment_alarm_id " +
                "FOREIGN KEY (alarm_id) REFERENCES alarm(id) ON DELETE CASCADE");
        jdbcTemplate.execute("ALTER TABLE entity_alarm DROP COLUMN IF EXISTS originator_id");

        assertTrue(constraintExists("fk_alarm_comment_alarm_id"));
        assertFalse(columnExists("entity_alarm", "originator_id"));

        // Seed an alarm owned by originatorA, propagated to a DIFFERENT entity B (so the entity_alarm row's
        // entity_id is B, not the originator). The pre-migration entity_alarm row has no originator_id column yet.
        UUID alarmId = UUID.randomUUID();
        UUID originatorA = UUID.randomUUID();
        UUID entityB = UUID.randomUUID();
        UUID tenantUuid = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO alarm (id, created_time, originator_id, originator_type, tenant_id, type) " +
                "VALUES (?, ?, ?, ?, ?, ?)", alarmId, 1L, originatorA, 1, tenantUuid, "General");
        jdbcTemplate.update("INSERT INTO entity_alarm (tenant_id, entity_type, entity_id, created_time, alarm_type, alarm_id) " +
                "VALUES (?, ?, ?, ?, ?, ?)", tenantUuid, "ASSET", entityB, 1L, "General", alarmId);

        try {
            // Drive the runner over a range whose target (4.3.1.4) selects only the 4.3.1.4 migration.
            ltsMigrationService.recordVersions(ltsMigrationService.applyMigrations("4.3.1.3", "4.3.1.4", false, () -> {}));

            // The schema_update.sql ran: the alarm_comment -> alarm cascade FK is gone and the column is back.
            assertFalse(constraintExists("fk_alarm_comment_alarm_id"));
            assertTrue(columnExists("entity_alarm", "originator_id"));
            // The post-commit backfill ran: the propagated row carries the parent alarm's originator (A), not B.
            UUID backfilled = jdbcTemplate.queryForObject(
                    "SELECT originator_id FROM entity_alarm WHERE entity_id = ? AND alarm_id = ?",
                    UUID.class, entityB, alarmId);
            assertEquals(originatorA, backfilled);
            // The version was recorded.
            assertEquals(Long.valueOf(V_4_3_1_4),
                    jdbcTemplate.queryForObject("SELECT schema_version FROM tb_schema_settings", Long.class));
        } finally {
            // If applyMigrations threw before its ADD COLUMN ran, the column this test dropped would stay missing
            // and break every subsequent alarm test in the shared suite; restore it unconditionally and re-fill the
            // originators the DROP COLUMN destroyed on rows owned by other test classes.
            jdbcTemplate.execute("ALTER TABLE entity_alarm ADD COLUMN IF NOT EXISTS originator_id uuid");
            jdbcTemplate.execute("UPDATE entity_alarm ea SET originator_id = a.originator_id FROM alarm a " +
                    "WHERE a.id = ea.alarm_id AND ea.originator_id IS NULL");
            // Leave the shared suite schema clean: drop the seeded rows (FK cascade is already gone) and the FK,
            // restoring the base end state the rest of the suite expects.
            jdbcTemplate.update("DELETE FROM entity_alarm WHERE alarm_id = ?", alarmId);
            jdbcTemplate.update("DELETE FROM alarm WHERE id = ?", alarmId);
            jdbcTemplate.execute("ALTER TABLE alarm_comment DROP CONSTRAINT IF EXISTS fk_alarm_comment_alarm_id");
        }
    }

    @Test
    public void backfillsEntityAlarmOriginatorAcrossMultipleKeysetWindowsAndReRunIsNoOp() {
        // Rows left in the shared suite DB by earlier test classes would also turn NULL after the DROP + re-ADD
        // below and join the keyset walk, shifting where the batch-of-3 window boundaries fall and silently voiding
        // the composite-cursor coverage this test exists for; clear the table so the boundary construction in the
        // seeding comment is guaranteed, not hoped. The leftover rows belong to already-finished test classes.
        jdbcTemplate.execute("DELETE FROM entity_alarm");

        // Reconstruct the pre-migration state: entity_alarm without originator_id. The FK half of 4.3.1.4 is
        // covered by the test above; the schema_update.sql's DROP ... IF EXISTS makes its absence here harmless.
        jdbcTemplate.execute("ALTER TABLE entity_alarm DROP COLUMN IF EXISTS originator_id");
        assertFalse(columnExists("entity_alarm", "originator_id"));

        // Seven rows over four entities whose PostgreSQL uuid order (byte-wise, i.e. unsigned) is fixed by
        // construction -- entity4's high bit is set on purpose so the backfill's cursor-advance guard is exercised
        // in the range where a signed compare would disagree. With batch size 3 the seven rows are guaranteed to
        // span several windows, and on an otherwise-empty table the first boundary falls BETWEEN two rows of
        // entity2 (alarm2 in window one, alarm3 in window two), so the composite (entity_id, alarm_id) cursor is
        // exercised, not just its entity_id half.
        UUID entity1 = UUID.fromString("10000000-0000-0000-0000-000000000000");
        UUID entity2 = UUID.fromString("20000000-0000-0000-0000-000000000000");
        UUID entity3 = UUID.fromString("30000000-0000-0000-0000-000000000000");
        UUID entity4 = UUID.fromString("f0000000-0000-0000-0000-000000000000");
        List<UUID> rowEntities = List.of(entity1, entity2, entity2, entity2, entity3, entity3, entity4);
        UUID tenantUuid = UUID.randomUUID();
        List<UUID> rowAlarms = new ArrayList<>();
        Map<UUID, UUID> expectedOriginatorByAlarmId = new LinkedHashMap<>();
        for (int i = 0; i < rowEntities.size(); i++) {
            // Alarm ids ascend with the seeding order, so rows sharing an entity_id keep that order in the walk.
            UUID alarmId = UUID.fromString(String.format("a0000000-0000-0000-0000-%012d", i + 1));
            UUID originator = UUID.randomUUID();
            jdbcTemplate.update("INSERT INTO alarm (id, created_time, originator_id, originator_type, tenant_id, type) " +
                    "VALUES (?, ?, ?, ?, ?, ?)", alarmId, 1L, originator, 1, tenantUuid, "General");
            jdbcTemplate.update("INSERT INTO entity_alarm (tenant_id, entity_type, entity_id, created_time, alarm_type, alarm_id) " +
                    "VALUES (?, ?, ?, ?, ?, ?)", tenantUuid, "ASSET", rowEntities.get(i), 1L, "General", alarmId);
            rowAlarms.add(alarmId);
            expectedOriginatorByAlarmId.put(alarmId, originator);
        }

        int originalBatchSize = v4314Migration.batchSize;
        v4314Migration.batchSize = 3;
        try {
            // Drive the runner over a range whose target (4.3.1.4) selects only the 4.3.1.4 migration.
            ltsMigrationService.applyMigrations("4.3.1.3", "4.3.1.4", false, () -> {});

            // Every seeded row got ITS OWN alarm's originator, across all three windows and both boundaries.
            for (int i = 0; i < rowAlarms.size(); i++) {
                UUID backfilled = jdbcTemplate.queryForObject(
                        "SELECT originator_id FROM entity_alarm WHERE entity_id = ? AND alarm_id = ?",
                        UUID.class, rowEntities.get(i), rowAlarms.get(i));
                assertEquals(expectedOriginatorByAlarmId.get(rowAlarms.get(i)), backfilled);
            }
            // The first pass left nothing NULL, so the IS NULL filter gives a second pass nothing to update...
            assertEquals(Long.valueOf(0L), jdbcTemplate.queryForObject(
                    "SELECT count(*) FROM entity_alarm WHERE originator_id IS NULL", Long.class));

            // ...and re-running the backfill (the crash-recovery path) walks the table again but changes nothing.
            v4314Migration.applyAfterCommit();
            for (int i = 0; i < rowAlarms.size(); i++) {
                UUID afterReRun = jdbcTemplate.queryForObject(
                        "SELECT originator_id FROM entity_alarm WHERE entity_id = ? AND alarm_id = ?",
                        UUID.class, rowEntities.get(i), rowAlarms.get(i));
                assertEquals(expectedOriginatorByAlarmId.get(rowAlarms.get(i)), afterReRun);
            }
        } finally {
            // If applyMigrations threw before its ADD COLUMN ran, the column this test dropped would stay missing
            // and break every subsequent alarm test in the shared suite; restore it unconditionally and re-fill the
            // originators the DROP COLUMN destroyed on rows owned by other test classes.
            jdbcTemplate.execute("ALTER TABLE entity_alarm ADD COLUMN IF NOT EXISTS originator_id uuid");
            jdbcTemplate.execute("UPDATE entity_alarm ea SET originator_id = a.originator_id FROM alarm a " +
                    "WHERE a.id = ea.alarm_id AND ea.originator_id IS NULL");
            // Leave the shared suite schema clean: restore the production batch size and drop the seeded rows.
            v4314Migration.batchSize = originalBatchSize;
            jdbcTemplate.update("DELETE FROM entity_alarm WHERE tenant_id = ?", tenantUuid);
            jdbcTemplate.update("DELETE FROM alarm WHERE tenant_id = ?", tenantUuid);
        }
    }

    @Test
    public void appliesSchemaForV4316CreatesTbClusterAndMintsOneClusterId() {
        TbClusterRow snapshot = snapshotTbCluster();
        try {
            jdbcTemplate.execute("DROP TABLE IF EXISTS tb_cluster");
            assertFalse(tableExists("tb_cluster"));

            ltsMigrationService.applyMigrations("4.3.1.5", "4.3.1.6", false, () -> {});

            assertTrue(tableExists("tb_cluster"));
            assertEquals(Long.valueOf(1L), jdbcTemplate.queryForObject("SELECT COUNT(*) FROM tb_cluster", Long.class));
        } finally {
            restoreTbCluster(snapshot);
        }
    }

    @Test
    public void v4316KeepsAnExistingClusterId() {
        TbClusterRow snapshot = snapshotTbCluster();
        try {
            UUID existing = UUID.randomUUID();
            jdbcTemplate.execute("DROP TABLE IF EXISTS tb_cluster");
            ltsMigrationService.applyMigrations("4.3.1.5", "4.3.1.6", false, () -> {});
            jdbcTemplate.update("UPDATE tb_cluster SET cluster_id = ?::uuid", existing.toString());

            ltsMigrationService.applyMigrations("4.3.1.5", "4.3.1.6", false, () -> {});

            assertEquals(List.of(existing.toString()),
                    jdbcTemplate.queryForList("SELECT cluster_id FROM tb_cluster", String.class));
        } finally {
            restoreTbCluster(snapshot);
        }
    }

    /**
     * The single {@code tb_cluster} row of the shared {@code @DaoSqlTest} database, so the two tests above can
     * put it back. They both drop the table, and the 4.3.1.6 script recreates only {@code cluster_id} and
     * {@code license_claim_token} - so without a restore the suite's table would permanently lose the four
     * columns {@code TbClusterStore} reads, and its seeded identity would be replaced by a freshly minted one.
     * Whether the rest of the suite then went green would depend on class ordering.
     */
    private record TbClusterRow(UUID clusterId, String licenseSecret, String licenseClaimToken,
                                Long nonProductionUptimeMs, Long nonProductionLastTick,
                                Long nonProductionConfirmedTs) {
    }

    private TbClusterRow snapshotTbCluster() {
        return new TbClusterRow(
                jdbcTemplate.queryForObject("SELECT cluster_id FROM tb_cluster", UUID.class),
                jdbcTemplate.queryForObject("SELECT license_secret FROM tb_cluster", String.class),
                jdbcTemplate.queryForObject("SELECT license_claim_token FROM tb_cluster", String.class),
                jdbcTemplate.queryForObject("SELECT non_production_uptime_ms FROM tb_cluster", Long.class),
                jdbcTemplate.queryForObject("SELECT non_production_last_tick FROM tb_cluster", Long.class),
                jdbcTemplate.queryForObject("SELECT non_production_confirmed_ts FROM tb_cluster", Long.class));
    }

    /**
     * Unconditional, and it recreates the table itself: if {@code applyMigrations} threw before its CREATE ran,
     * the table would otherwise stay dropped entirely.
     */
    private void restoreTbCluster(TbClusterRow snapshot) {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS tb_cluster (cluster_id uuid NOT NULL, " +
                "CONSTRAINT tb_cluster_pkey PRIMARY KEY (cluster_id))");
        jdbcTemplate.execute("ALTER TABLE tb_cluster ADD COLUMN IF NOT EXISTS license_secret varchar");
        jdbcTemplate.execute("ALTER TABLE tb_cluster ADD COLUMN IF NOT EXISTS license_claim_token varchar");
        jdbcTemplate.execute("ALTER TABLE tb_cluster ADD COLUMN IF NOT EXISTS non_production_uptime_ms bigint NOT NULL DEFAULT 0");
        jdbcTemplate.execute("ALTER TABLE tb_cluster ADD COLUMN IF NOT EXISTS non_production_last_tick bigint");
        jdbcTemplate.execute("ALTER TABLE tb_cluster ADD COLUMN IF NOT EXISTS non_production_confirmed_ts bigint");
        jdbcTemplate.execute("CREATE UNIQUE INDEX IF NOT EXISTS tb_cluster_single_row ON tb_cluster ((true))");
        // Whatever the migration minted goes, and the row the suite started with comes back with its own id.
        jdbcTemplate.execute("DELETE FROM tb_cluster");
        jdbcTemplate.update("INSERT INTO tb_cluster (cluster_id, license_secret, license_claim_token, " +
                        "non_production_uptime_ms, non_production_last_tick, non_production_confirmed_ts) " +
                        "VALUES (?::uuid, ?, ?, ?, ?, ?)",
                snapshot.clusterId().toString(), snapshot.licenseSecret(), snapshot.licenseClaimToken(),
                snapshot.nonProductionUptimeMs() == null ? 0L : snapshot.nonProductionUptimeMs(),
                snapshot.nonProductionLastTick(), snapshot.nonProductionConfirmedTs());
    }

    /**
     * The upgrade scripts and {@link TbClusterSchema} necessarily repeat {@code schema-entities.sql}'s
     * {@code tb_cluster} DDL, because neither the patch path nor the CE-to-PE conversion runs that file, and a
     * drift is silent: upgraded deployments would carry a differently shaped table from freshly installed ones.
     * <p>
     * Containment rather than equality, because a later version may add a column in {@code schema-entities.sql}
     * for fresh installs and reach existing deployments through that version's own {@code ALTER}, leaving an
     * older script legitimately behind. What may never happen is a script declaring a clause the fresh-install
     * DDL does not. The single-row index is compared exactly: there is only ever one right form of it.
     */
    @Test
    public void everyTbClusterDdlCopyIsConsistentWithSchemaEntities() {
        String schemaEntities = readResource("sql/schema-entities.sql");

        // One CREATE TABLE and one CREATE UNIQUE INDEX. Pinned because the comparisons below look at the first
        // match of each kind: a second tb_cluster index added to the production schema would otherwise be missing
        // from every copy without a single assertion noticing.
        assertEquals("tb_cluster statements in schema-entities.sql", 2, tbClusterStatementCount(schemaEntities));

        // Asserted found before anything is compared to it: both helpers answer null/empty when their regex
        // matches nothing, so a reformat that defeated the pattern in every file would otherwise compare
        // null to null and pass without checking a thing.
        String declaredIndex = tbClusterIndexStatement(schemaEntities);
        assertNotNull("tb_cluster single-row index found in schema-entities.sql", declaredIndex);
        Map<String, String> declared = tbClusterClauses(schemaEntities);
        assertFalse("tb_cluster clauses found in schema-entities.sql", declared.isEmpty());

        List<Path> copies = tbClusterDdlCopies();
        // The walk discovers by the same DDL forms the comparison parses, so a script whose tb_cluster block
        // was reformatted past those regexes would leave the compared set without a word - the one blind spot
        // a self-selecting scan has. These two always carry the shape, so they are the floor under it:
        // upgrade/pe makes the table on a CE-to-PE conversion, the current LTS baseline on a fresh upgrade.
        List<Path> required = List.of(
                upgradeDir().resolve(Paths.get("pe", SCHEMA_UPDATE_SQL)),
                upgradeDir().resolve(Paths.get("lts", "4.4.0.0", SCHEMA_UPDATE_SQL)));
        assertTrue("tb_cluster DDL copies " + copies + " include " + required, copies.containsAll(required));

        for (Path script : copies) {
            String sql = readFile(script);
            // Required of a script that creates the table; of one that only adds columns, only compared when
            // it states an index at all - that script reaches a table whose index is already there, so a
            // future one is under no obligation to restate it.
            String copiedIndex = tbClusterIndexStatement(sql);
            if (!tbClusterClauses(sql).isEmpty() || copiedIndex != null) {
                assertEquals("tb_cluster single-row index in " + script, declaredIndex, copiedIndex);
            }

            tbClusterDeclarations(sql).forEach(declaration ->
                    assertEquals("tb_cluster " + declaration.getKey() + " in " + script
                                    + " differs from schema-entities.sql",
                            declared.get(declaration.getKey()), declaration.getValue()));
        }

        assertEquals("tb_cluster single-row index in TbClusterSchema",
                declaredIndex, tbClusterIndexStatement(TbClusterSchema.CREATE_CLUSTER_SINGLE_ROW_INDEX_QUERY));
        // Equality, not containment: this copy is Java the licence context runs verbatim, so a clause the
        // fresh-install DDL declares and this one omits - the primary key above all - is a table shape only the
        // self-heal would ever produce.
        assertEquals("tb_cluster clauses in TbClusterSchema",
                declared, tbClusterClauses(TbClusterSchema.CREATE_CLUSTER_TABLE_QUERY));
    }

    /**
     * A PE 4.3.x-to-4.4 offline upgrade runs these two scripts and no other {@code tb_cluster} DDL, so together
     * they have to produce the whole table. The comparison above is containment per script, and containment
     * stays green when a column reaches neither of them.
     */
    @Test
    public void offlineUpgradeScriptsTogetherDeclareTheWholeTbClusterTable() {
        Set<String> declared = tbClusterClauses(readResource("sql/schema-entities.sql")).keySet();
        assertFalse("tb_cluster clauses found in schema-entities.sql", declared.isEmpty());

        Set<String> upgraded = Stream.of("4.3.1.6", "4.4.0.0")
                .map(version -> upgradeDir().resolve(Paths.get("lts", version, SCHEMA_UPDATE_SQL)))
                .flatMap(script -> tbClusterDeclarations(readFile(script)).stream())
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        assertEquals("tb_cluster clauses the 4.3.1.6 and 4.4.0.0 scripts declare between them",
                declared, upgraded);
    }

    /**
     * Every shipped script that carries a copy of the {@code tb_cluster} shape, found by walking the upgrade
     * tree rather than from a list: the next LTS directory to touch the table would otherwise be checked by
     * nothing while the test above read as proof of coverage. Discovery is by the DDL forms the comparison
     * itself parses, not by a mention of the table name - a script that only names it in a comment or deletes
     * from it says nothing about shape and would fail here for nothing.
     */
    private List<Path> tbClusterDdlCopies() {
        Path upgradeDir = upgradeDir();
        try (Stream<Path> tree = Files.walk(upgradeDir)) {
            return tree
                    .filter(path -> SCHEMA_UPDATE_SQL.equals(path.getFileName().toString()))
                    .filter(path -> !tbClusterDeclarations(readFile(path)).isEmpty())
                    .sorted()
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to scan " + upgradeDir, e);
        }
    }

    /** Where the shipped upgrade scripts live. */
    private Path upgradeDir() {
        return Paths.get(installScripts.getDataDir(), "upgrade");
    }

    @Test
    public void offlineCrossFamilyUpgradeRunsInRangeOlderFamilyAndBaselineSchema() {
        // A 4.3.x -> 4.4 offline upgrade must run BOTH the in-range 4.3.1.x beans and the new 4.4.0.0 baseline
        // bean via the loosened selector. Drop the columns/tables they own so we can prove each one re-lands.
        jdbcTemplate.execute("ALTER TABLE rule_chain DROP COLUMN IF EXISTS notes");            // owned by lts/4.4.0.0
        jdbcTemplate.execute("ALTER TABLE calculated_field DROP COLUMN IF EXISTS additional_info"); // owned by 4.3.1.2
        jdbcTemplate.execute("DROP TABLE IF EXISTS iot_hub_installed_item");                   // created by 4.3.1.3
        assertFalse(columnExists("rule_chain", "notes"));
        assertFalse(columnExists("calculated_field", "additional_info"));
        assertFalse(tableExists("iot_hub_installed_item"));

        // Offline schema phase over the whole supported cross-family range.
        ltsMigrationService.runSchemaMigrations("4.3.0.0", "4.4.0.0", false);

        // 4.4.0.0 baseline DDL landed (this is what used to live in basic/schema_update.sql)...
        assertTrue(columnExists("rule_chain", "notes"));
        // ...and the in-range older-family 4.3.1.x beans ran too.
        assertTrue(columnExists("calculated_field", "additional_info"));
        assertTrue(tableExists("iot_hub_installed_item"));
    }

    @Test
    public void migrationDirectoriesAndBeansStayInSyncBothWays() {
        Path ltsDir = Paths.get(installScripts.getDataDir(), "upgrade", "lts");
        Set<String> dirVersions = listDirVersions(ltsDir);
        Set<String> beanVersions = migrations.stream().map(LtsMigration::getVersion).collect(Collectors.toSet());

        // Every on-disk migration directory must have a registered bean with the same version.
        // Otherwise select() (which iterates beans, not dirs) silently skips the SQL dir.
        Set<String> dirsWithoutBean = dirVersions.stream()
                .filter(v -> !beanVersions.contains(v))
                .collect(Collectors.toSet());
        assertTrue("Migration directories without a registered LtsMigration bean: " + dirsWithoutBean,
                dirsWithoutBean.isEmpty());

        // Every registered bean must have a matching directory, unless it is explicitly allowed to be SQL-less.
        // Otherwise a typo'd dir name silently runs no SQL for that bean.
        Set<String> beansWithoutDir = beanVersions.stream()
                .filter(v -> !dirVersions.contains(v))
                .filter(v -> !SQL_LESS_ALLOWED.contains(v))
                .collect(Collectors.toSet());
        assertTrue("Registered LtsMigration beans without a matching directory (and not SQL-less allowed): " + beansWithoutDir,
                beansWithoutDir.isEmpty());
    }

    private Set<String> listDirVersions(Path ltsDir) {
        if (!Files.isDirectory(ltsDir)) {
            return Set.of();
        }
        try (Stream<Path> entries = Files.list(ltsDir)) {
            return entries.filter(Files::isDirectory)
                    .map(p -> p.getFileName().toString())
                    .collect(Collectors.toSet());
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to list LTS migration directories: " + ltsDir, e);
        }
    }

    /** How many CREATE TABLE / CREATE UNIQUE INDEX statements name tb_cluster. */
    private static int tbClusterStatementCount(String sql) {
        Matcher matcher = Pattern.compile("^\\s*CREATE\\s+(?:TABLE|UNIQUE\\s+INDEX)[^;]*\\btb_cluster\\b[^;]*;",
                Pattern.CASE_INSENSITIVE | Pattern.MULTILINE).matcher(sql);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    /** The CREATE UNIQUE INDEX statement naming tb_cluster, whitespace-normalised, or null if absent. */
    private static String tbClusterIndexStatement(String sql) {
        Matcher matcher = Pattern.compile("^\\s*(CREATE\\s+UNIQUE\\s+INDEX[^;]*\\btb_cluster\\b[^;]*;)",
                Pattern.CASE_INSENSITIVE | Pattern.MULTILINE).matcher(sql);
        return matcher.find() ? matcher.group(1).replaceAll("\\s+", " ").trim() : null;
    }

    /**
     * Every declaration a script makes about {@code tb_cluster}: the {@code CREATE TABLE} body and each
     * {@code ALTER TABLE ... ADD COLUMN}. A list rather than a map by name, because a column stated in both forms
     * has to be checked in both - which one lands depends on whether the table already exists.
     */
    private static List<Map.Entry<String, String>> tbClusterDeclarations(String sql) {
        List<Map.Entry<String, String>> declarations = new ArrayList<>(tbClusterClauses(sql).entrySet());
        declarations.addAll(tbClusterAddedColumns(sql).entrySet());
        return declarations;
    }

    /**
     * Column name to its declaration, from the {@code ALTER TABLE tb_cluster ADD COLUMN} statements,
     * normalised into the same form {@link #tbClusterClauses} produces so the two can be compared against one
     * declaration. Statement-anchored, so unrelated statements between them - a de-duplicating
     * {@code DELETE}, say - are simply not matched.
     */
    private static Map<String, String> tbClusterAddedColumns(String sql) {
        Map<String, String> columns = new LinkedHashMap<>();
        Matcher matcher = Pattern.compile(
                        "^\\s*ALTER\\s+TABLE\\s+tb_cluster\\s+ADD\\s+COLUMN\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?([^;]+);",
                        Pattern.CASE_INSENSITIVE | Pattern.MULTILINE)
                .matcher(sql);
        while (matcher.find()) {
            String normalised = matcher.group(1).replaceAll("\\s+", " ").trim();
            columns.put(normalised.split(" ")[0].toLowerCase(), normalised);
        }
        return columns;
    }

    /**
     * Every clause of the {@code CREATE TABLE tb_cluster} body by name - columns and table-level constraints
     * alike, a constraint keyed by its own name rather than by the {@code CONSTRAINT} keyword.
     */
    private static Map<String, String> tbClusterClauses(String sql) {
        Matcher table = Pattern.compile("CREATE\\s+TABLE[^;(]*\\btb_cluster\\b[^(]*\\((.*?)\\);",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(sql);
        Map<String, String> clauses = new LinkedHashMap<>();
        if (!table.find()) {
            return clauses;
        }
        for (String line : table.group(1).split(",\\s*(?=\\n)")) {
            String normalised = line.replaceAll("\\s+", " ").trim().replaceAll(",$", "");
            if (normalised.isEmpty()) {
                continue;
            }
            String[] tokens = normalised.split(" ");
            String name = "CONSTRAINT".equalsIgnoreCase(tokens[0]) ? tokens[1] : tokens[0];
            clauses.put(name.toLowerCase(), normalised);
        }
        return clauses;
    }

    private static String readFile(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read " + path, e);
        }
    }

    private static String readResource(String name) {
        try (InputStream in = LtsMigrationIntegrationTest.class.getClassLoader().getResourceAsStream(name)) {
            assertNotNull(name + " on the test classpath", in);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read " + name, e);
        }
    }

    private boolean tableExists(String table) {
        Boolean exists = jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = ?)", Boolean.class, table);
        return Boolean.TRUE.equals(exists);
    }

    private boolean columnExists(String table, String column) {
        Boolean exists = jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = ? AND column_name = ?)",
                Boolean.class, table, column);
        return Boolean.TRUE.equals(exists);
    }

    private boolean constraintExists(String constraintName) {
        Boolean exists = jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = ?)", Boolean.class, constraintName);
        return Boolean.TRUE.equals(exists);
    }
}
