// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@DaoSqlTest
public class LtsMigrationIntegrationTest extends AbstractControllerTest {

    private static final Runnable NO_POST_SCHEMA_WORK = () -> {
    };

    private static final long V_4_2_2_2 = 4_002_002_002L;
    private static final long V_4_2_2_3 = 4_002_002_003L;
    private static final String OBSOLETE_ALIAS = "air_quality";

    // Versions whose family is older than the current package family ship SQL-less beans intentionally
    // (their schema/data changes are reproduced by the current-family migrations), so a missing dir is OK.
    private static final Set<String> SQL_LESS_ALLOWED = Set.of();

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

    private Long originalSchemaVersion;
    private String originalClusterId;
    private String originalLicenseClaimToken;
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

        // The 4.3.1.6 tests drop and re-create tb_cluster, so keep the row the shared test DB already has.
        jdbcTemplate.query("SELECT cluster_id, license_claim_token FROM tb_cluster", rs -> {
            if (rs.next()) {
                originalClusterId = rs.getString("cluster_id");
                originalLicenseClaimToken = rs.getString("license_claim_token");
            }
            return null;
        });

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
        // Guarded so that a migration that failed to recreate tb_cluster reports its own assertion, not a tear-down error.
        if (tableExists("tb_cluster")) {
            jdbcTemplate.execute("DELETE FROM tb_cluster");
            if (originalClusterId != null) {
                jdbcTemplate.update("INSERT INTO tb_cluster (cluster_id, license_claim_token) VALUES (?::uuid, ?)",
                        originalClusterId, originalLicenseClaimToken);
            }
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
        ltsMigrationService.applyMigrations("4.2.2.2", "4.2.2.3", () -> {
            assertTrue(tableExists("iot_hub_installed_item"));
            assertEquals(Long.valueOf(V_4_2_2_2),
                    jdbcTemplate.queryForObject("SELECT schema_version FROM tb_schema_settings", Long.class));
        });

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
        ltsMigrationService.applyMigrations("4.2.2.2", "4.2.2.3", NO_POST_SCHEMA_WORK);
        // Re-running from the now-current version selects an empty range — nothing changes, nothing throws.
        ltsMigrationService.applyMigrations("4.2.2.3", "4.2.2.3", NO_POST_SCHEMA_WORK);

        assertEquals(Long.valueOf(V_4_2_2_3),
                jdbcTemplate.queryForObject("SELECT schema_version FROM tb_schema_settings", Long.class));
        assertNull(widgetsBundleService.findWidgetsBundleByTenantIdAndAlias(TenantId.SYS_TENANT_ID, OBSOLETE_ALIAS));
        assertTrue(widgetTypeService.findWidgetTypeDetailsById(TenantId.SYS_TENANT_ID, widgetTypeId).isDeprecated());
    }

    @Test
    public void postSchemaWorkFailureLeavesTheVersionUnrecordedSoTheNextBootRetries() {
        IllegalStateException failure = new IllegalStateException("post-schema work failed");
        try {
            ltsMigrationService.applyMigrations("4.2.2.2", "4.2.2.3", () -> {
                throw failure;
            });
            fail("Expected the post-schema failure to propagate");
        } catch (IllegalStateException e) {
            assertEquals(failure, e);
        }

        assertTrue(tableExists("iot_hub_installed_item"));
        assertNull(widgetsBundleService.findWidgetsBundleByTenantIdAndAlias(TenantId.SYS_TENANT_ID, OBSOLETE_ALIAS));
        assertEquals(Long.valueOf(V_4_2_2_2),
                jdbcTemplate.queryForObject("SELECT schema_version FROM tb_schema_settings", Long.class));

        ltsMigrationService.applyMigrations("4.2.2.2", "4.2.2.3", NO_POST_SCHEMA_WORK);

        assertEquals(Long.valueOf(V_4_2_2_3),
                jdbcTemplate.queryForObject("SELECT schema_version FROM tb_schema_settings", Long.class));
        assertTrue(widgetTypeService.findWidgetTypeDetailsById(TenantId.SYS_TENANT_ID, widgetTypeId).isDeprecated());
    }

    @Test
    public void offlinePathRunsSchemaThenDataButRecordsNoVersion() {
        // Drive the offline major-upgrade path over the real supported range against the real DB.
        ltsMigrationService.runSchemaMigrations("4.2.2.2", "4.2.2.3");
        // (a) the schema effects landed: the table the 4.2.2.3 SQL creates now exists.
        assertTrue(tableExists("iot_hub_installed_item"));
        // (c) the offline schema phase records NO schema version (unlike applyMigrations).
        assertEquals(Long.valueOf(V_4_2_2_2),
                jdbcTemplate.queryForObject("SELECT schema_version FROM tb_schema_settings", Long.class));

        ltsMigrationService.runDataMigrations("4.2.2.2", "4.2.2.3");
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
        // The test-context schema already ships calculated_field.additional_info, so drop it first to prove the
        // 4.3.1.2 schema_update.sql (ALTER TABLE calculated_field ADD COLUMN IF NOT EXISTS additional_info) re-adds it.
        jdbcTemplate.execute("ALTER TABLE calculated_field DROP COLUMN IF EXISTS additional_info");
        assertFalse(columnExists("calculated_field", "additional_info"));

        // Drive the runner over a range whose target (4.3.1.2) selects only the 4.3.1.2 migration.
        ltsMigrationService.applyMigrations("4.3.1.1", "4.3.1.2", NO_POST_SCHEMA_WORK);

        // The 4.3.1.2 schema SQL ran: the column exists again.
        assertTrue(columnExists("calculated_field", "additional_info"));
    }

    @Test
    public void appliesSchemaForV4316CreatesTbClusterAndMintsOneClusterId() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS tb_cluster");
        assertFalse(tableExists("tb_cluster"));

        ltsMigrationService.applyMigrations("4.3.1.3", "4.3.1.6", NO_POST_SCHEMA_WORK);

        assertTrue(tableExists("tb_cluster"));
        assertEquals(Long.valueOf(1L), jdbcTemplate.queryForObject("SELECT COUNT(*) FROM tb_cluster", Long.class));
    }

    @Test
    public void v4316KeepsAnExistingClusterId() {
        UUID existing = UUID.randomUUID();
        jdbcTemplate.execute("DROP TABLE IF EXISTS tb_cluster");
        ltsMigrationService.applyMigrations("4.3.1.3", "4.3.1.6", NO_POST_SCHEMA_WORK);
        jdbcTemplate.update("UPDATE tb_cluster SET cluster_id = ?::uuid", existing.toString());

        ltsMigrationService.applyMigrations("4.3.1.3", "4.3.1.6", NO_POST_SCHEMA_WORK);

        assertEquals(List.of(existing.toString()),
                jdbcTemplate.queryForList("SELECT cluster_id FROM tb_cluster", String.class));
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
}
