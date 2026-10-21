// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.controller;

import com.google.common.io.Resources;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
public class RuleEngineMonitoringControllerTest extends AbstractControllerTest {

    private static final List<String> TIMESERIES_URLS = List.of(
            "/api/ruleEngineMonitoring/nodeStats/timeseries",
            "/api/ruleEngineMonitoring/queueStats/timeseries",
            "/api/ruleEngineMonitoring/queueLagStats/timeseries");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Before
    public void setUp() throws Exception {
        // The suite DB is built without schema-trace.sql.
        jdbcTemplate.execute(Resources.toString(Resources.getResource("sql/schema-trace.sql"), StandardCharsets.UTF_8));
        loginTenantAdmin();
    }

    @After
    public void dropTraceSchema() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS span, span_stats_leader, trace_summary, rule_node_exec_stats, " +
                "rule_node_queue_stats, rule_node_queue_lag_stats, trace_group");
    }

    @Test
    public void givenNonPositiveInterval_whenGetTimeseries_thenBadRequest() throws Exception {
        for (String url : TIMESERIES_URLS) {
            doGet(url + "?startTs=0&endTs=3600000&intervalMs=0").andExpect(status().isBadRequest());
            doGet(url + "?startTs=0&endTs=3600000&intervalMs=-5").andExpect(status().isBadRequest());
        }
    }

    @Test
    public void givenEndNotAfterStart_whenGetTimeseries_thenBadRequest() throws Exception {
        for (String url : TIMESERIES_URLS) {
            doGet(url + "?startTs=3600000&endTs=0&intervalMs=60000").andExpect(status().isBadRequest());
            doGet(url + "?startTs=3600000&endTs=3600000").andExpect(status().isBadRequest());
        }
    }

    @Test
    public void givenTooManyIntervals_whenGetTimeseries_thenBadRequest() throws Exception {
        // One day in one-second steps is far above database.ts_max_intervals (700).
        for (String url : TIMESERIES_URLS) {
            doGet(url + "?startTs=0&endTs=86400000&intervalMs=1000").andExpect(status().isBadRequest());
        }
    }

    @Test
    public void givenValidRange_whenGetTimeseries_thenOk() throws Exception {
        for (String url : TIMESERIES_URLS) {
            doGet(url + "?startTs=0&endTs=3600000&intervalMs=60000").andExpect(status().isOk());
            doGet(url + "?startTs=0&endTs=3600000").andExpect(status().isOk());
        }
    }

}
