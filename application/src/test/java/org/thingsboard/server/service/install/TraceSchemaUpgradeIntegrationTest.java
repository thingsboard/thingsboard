// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.install;

import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.thingsboard.server.dao.sql.ruleenginemonitoring.RuleEngineMonitoringDao;
import org.thingsboard.server.dao.sql.trace.TraceDao;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.service.install.lts.MigrationService;

import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * New installs and offline upgrades get the tracing and monitoring tables from schema-entities.sql. LTS patch updates
 * (e.g. 4.4.0.x -> 4.4.1.0) skip that path: SystemPatchApplier runs only the lts migrations at startup, so the
 * 4.4.1.0 schema_update.sql must create the same tables.
 */
@DaoSqlTest
public class TraceSchemaUpgradeIntegrationTest extends AbstractControllerTest {

    private static final List<String> TRACE_TABLES = List.of(
            "trace_coverage_setting", "span", "trace_summary", "trace_group", "rule_node_exec_stats",
            "rule_node_queue_stats", "rule_node_queue_lag_stats");

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private MigrationService migrationService;
    @Autowired
    private TraceDao traceDao;
    @Autowired
    private RuleEngineMonitoringDao ruleEngineMonitoringDao;

    @After
    public void restoreTraceTables() {
        // The suite DB is built from schema-entities.sql with these tables; put them back whatever the test did.
        migrationService.applyMigrations("4.4.1.0", "4.4.1.0", true, () -> {});
        // The partitions went with the dropped tables; later tests in this context must create them again.
        traceDao.cleanUpPartitionsCache(Long.MAX_VALUE);
        ruleEngineMonitoringDao.cleanUpPartitionsCache(Long.MAX_VALUE);
    }

    @Test
    public void givenDbWithoutTraceTables_whenPatchedTo4_4_1_0_thenAllTablesExistAndRerunIsNoOp() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS " + String.join(", ", TRACE_TABLES));
        TRACE_TABLES.forEach(table -> assertFalse(table + " must not exist before the patch", tableExists(table)));

        migrationService.applyMigrations("4.4.0.0", "4.4.1.0", false, () -> {});

        TRACE_TABLES.forEach(table -> assertTrue(table + " must exist after the patch", tableExists(table)));

        // A forced re-run (SKIP_PATCH_VERSION_CHECK) selects 4.4.1.0 again; it must not fail on the existing tables.
        migrationService.applyMigrations("4.4.1.0", "4.4.1.0", true, () -> {});

        TRACE_TABLES.forEach(table -> assertTrue(table + " must still exist after a re-run", tableExists(table)));
    }

    private boolean tableExists(String table) {
        Boolean exists = jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = current_schema() AND table_name = ?)",
                Boolean.class, table);
        return Boolean.TRUE.equals(exists);
    }
}
