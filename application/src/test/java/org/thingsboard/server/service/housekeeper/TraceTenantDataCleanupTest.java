// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.housekeeper;

import com.google.common.io.Resources;
import org.awaitility.Awaitility;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.trace.TraceCoverageSetting;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.dao.sql.ruleenginemonitoring.QueueLagStatsRow;
import org.thingsboard.server.dao.sql.ruleenginemonitoring.RuleEngineMonitoringDao;
import org.thingsboard.server.dao.sql.ruleenginemonitoring.RuleNodeExecStatsRow;
import org.thingsboard.server.dao.sql.trace.TraceDao;
import org.thingsboard.server.dao.sql.trace.TraceSummaryInfo;
import org.thingsboard.server.dao.sql.tracecoverage.TraceCoverageSettingDao;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@DaoSqlTest
public class TraceTenantDataCleanupTest extends AbstractControllerTest {

    private static final List<String> TENANT_TABLES = List.of("trace_summary", "rule_node_exec_stats");

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private TraceDao traceDao;
    @Autowired
    private RuleEngineMonitoringDao ruleEngineMonitoringDao;
    @Autowired
    private TraceCoverageSettingDao traceCoverageSettingDao;

    @Before
    public void createTraceSchema() throws Exception {
        // The suite DB is built without schema-trace.sql.
        jdbcTemplate.execute(Resources.toString(Resources.getResource("sql/schema-trace.sql"), StandardCharsets.UTF_8));
    }

    @After
    public void dropTraceSchema() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS span, trace_summary, rule_node_exec_stats, " +
                "rule_node_queue_stats, rule_node_queue_lag_stats, trace_group");
    }

    @Test
    public void givenTenantWithTraceData_whenTenantDeleted_thenTraceDataAndCoverageSettingAreRemoved() throws Exception {
        loginSysAdmin();
        Tenant tenant = new Tenant();
        tenant.setTitle("Traced tenant");
        tenant = saveTenant(tenant);
        UUID id = tenant.getId().getId();
        traceCoverageSettingDao.upsertByTenantId(id, TraceCoverageSetting.defaultSetting(id));
        traceDao.upsertTraceSummaries(List.of(new TraceSummaryInfo("trace-1", id, List.of("svc"), 100, 200, 100, 1, 1,
                100, "Rule Node", 80, 20, null, "POST_TELEMETRY_REQUEST", null, List.of(), List.of("Main"),
                List.of(), List.of(), List.of("POST_TELEMETRY_REQUEST"), List.of(), false, false)));
        UUID queueId = UUID.randomUUID();
        ruleEngineMonitoringDao.saveRuleNodeStats(List.of(new RuleNodeExecStatsRow(id, queueId, UUID.randomUUID(),
                UUID.randomUUID(), "svc", 0, 1, 0, 10, 10)));
        ruleEngineMonitoringDao.saveQueueLagStats(List.of(new QueueLagStatsRow(0, id, queueId, 5)));

        deleteTenant(tenant.getId());

        assertNull(traceCoverageSettingDao.findByTenantId(id));
        // Traces and stats go through the housekeeper.
        Awaitility.await("trace tenant data deleted").atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
            for (String table : TENANT_TABLES) {
                assertEquals(table, Integer.valueOf(0), jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM " + table + " WHERE tenant_id = ?", Integer.class, id));
            }
            assertEquals(Integer.valueOf(0), jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM rule_node_queue_lag_stats WHERE queue_tenant_id = ?", Integer.class, id));
        });
    }

}
