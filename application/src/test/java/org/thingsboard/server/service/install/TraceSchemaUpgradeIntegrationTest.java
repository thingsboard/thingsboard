// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.install;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.FileSystemUtils;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.service.install.lts.MigrationService;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Upgraded installs get the trace tables from {@link SqlTraceDatabaseSchemaService}, which
 * {@code ThingsboardInstallService} runs on the regular upgrade path. The test suite DB is built without
 * schema-trace.sql, so it starts in the pre-tracing state; the test runs the real service against it twice, to also
 * pin that a re-run (every later upgrade) is a no-op. The service reads its script from the install data dir, which in
 * a packaged install holds the dao module's sql/ resources, so the test stages the same file in a temp data dir.
 * <p>
 * LTS patch updates (e.g. 4.4.0.x -> 4.4.1.0) skip that path: SystemPatchApplier runs only the lts migrations at
 * startup, so the 4.4.1.0 migration must create the same tables.
 */
@DaoSqlTest
public class TraceSchemaUpgradeIntegrationTest extends AbstractControllerTest {

    private static final List<String> TRACE_TABLES = List.of(
            "span", "span_stats_leader", "trace_summary", "rule_node_exec_stats", "rule_node_queue_stats",
            "rule_node_queue_lag_stats", "trace_group");

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private MigrationService migrationService;

    @Value("${spring.datasource.url}")
    private String dbUrl;
    @Value("${spring.datasource.username}")
    private String dbUserName;
    @Value("${spring.datasource.password}")
    private String dbPassword;

    private Path dataDir;
    private SqlTraceDatabaseSchemaService traceSchemaService;

    @Before
    public void setUpTraceSchemaService() throws IOException {
        dataDir = Files.createTempDirectory("tb-install-data");
        Path sqlDir = Files.createDirectories(dataDir.resolve("sql"));
        try (InputStream script = getClass().getClassLoader().getResourceAsStream("sql/" + SqlTraceDatabaseSchemaService.SCHEMA_TS_TRACE_SQL)) {
            Files.copy(script, sqlDir.resolve(SqlTraceDatabaseSchemaService.SCHEMA_TS_TRACE_SQL));
        }
        InstallScripts installScripts = Mockito.mock(InstallScripts.class);
        Mockito.when(installScripts.getDataDir()).thenReturn(dataDir.toString());

        traceSchemaService = new SqlTraceDatabaseSchemaService();
        ReflectionTestUtils.setField(traceSchemaService, "installScripts", installScripts);
        ReflectionTestUtils.setField(traceSchemaService, "dbUrl", dbUrl);
        ReflectionTestUtils.setField(traceSchemaService, "dbUserName", dbUserName);
        ReflectionTestUtils.setField(traceSchemaService, "dbPassword", dbPassword);

        // A previous run on the shared suite DB may have been interrupted before its cleanup.
        dropTraceTables();
    }

    @After
    public void tearDown() {
        // drop-all-tables.sql doesn't know the trace tables, so restore the suite DB's base (pre-tracing) state here.
        dropTraceTables();
        FileSystemUtils.deleteRecursively(dataDir.toFile());
    }

    @Test
    public void givenDbWithoutTraceSchema_whenTraceSchemaIsCreatedOnUpgrade_thenAllTablesAndLeaderLocksExistAndRerunIsNoOp() throws Exception {
        TRACE_TABLES.forEach(table -> assertFalse(table + " must not exist before the upgrade", tableExists(table)));

        traceSchemaService.createDatabaseSchema(false);

        TRACE_TABLES.forEach(table -> assertTrue(table + " must exist after the upgrade", tableExists(table)));
        assertEquals(List.of("aggregation", "queue-lag"), findLeaderLockNames());

        // A leader row taken over by a running node must survive the next upgrade untouched.
        jdbcTemplate.update("UPDATE span_stats_leader SET leader_until = 42 WHERE lock_name = 'aggregation'");

        traceSchemaService.createDatabaseSchema(false);

        TRACE_TABLES.forEach(table -> assertTrue(table + " must still exist after a re-run", tableExists(table)));
        assertEquals(List.of("aggregation", "queue-lag"), findLeaderLockNames());
        assertEquals(Long.valueOf(42), jdbcTemplate.queryForObject(
                "SELECT leader_until FROM span_stats_leader WHERE lock_name = 'aggregation'", Long.class));
    }

    @Test
    public void givenDbWithoutTraceSchema_whenPatchedTo4_4_1_0_thenAllTablesAndLeaderLocksExistAndRerunIsNoOp() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS trace_coverage_setting");
        TRACE_TABLES.forEach(table -> assertFalse(table + " must not exist before the patch", tableExists(table)));

        migrationService.applyMigrations("4.4.0.0", "4.4.1.0", false, () -> {});

        assertTrue("trace_coverage_setting must exist after the patch", tableExists("trace_coverage_setting"));
        TRACE_TABLES.forEach(table -> assertTrue(table + " must exist after the patch", tableExists(table)));
        assertEquals(List.of("aggregation", "queue-lag"), findLeaderLockNames());

        // A forced re-run (SKIP_PATCH_VERSION_CHECK) selects 4.4.1.0 again; it must not fail on the existing tables.
        migrationService.applyMigrations("4.4.1.0", "4.4.1.0", true, () -> {});

        assertTrue(tableExists("trace_coverage_setting"));
        TRACE_TABLES.forEach(table -> assertTrue(table + " must still exist after a re-run", tableExists(table)));
    }

    private void dropTraceTables() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS " + String.join(", ", TRACE_TABLES));
    }

    private List<String> findLeaderLockNames() {
        return jdbcTemplate.queryForList("SELECT lock_name FROM span_stats_leader ORDER BY lock_name", String.class);
    }

    private boolean tableExists(String table) {
        Boolean exists = jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = current_schema() AND table_name = ?)",
                Boolean.class, table);
        return Boolean.TRUE.equals(exists);
    }
}
