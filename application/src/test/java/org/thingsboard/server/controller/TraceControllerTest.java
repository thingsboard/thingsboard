// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.io.Resources;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.dao.sql.trace.TraceDao;
import org.thingsboard.server.dao.sql.trace.TraceSummaryInfo;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;

@DaoSqlTest
public class TraceControllerTest extends AbstractControllerTest {

    private static final long START = 1_700_000_000_000L;
    private static final int TRACES = 105;
    private static final String SEARCH_URL = "/api/traces?startTime=" + (START - 1) + "&endTime=" + (START + 1_000_000);

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private TraceDao traceDao;

    @Before
    public void setUp() throws Exception {
        // The suite DB is built without schema-trace.sql.
        jdbcTemplate.execute(Resources.toString(Resources.getResource("sql/schema-trace.sql"), StandardCharsets.UTF_8));
        traceDao.upsertTraceSummaries(IntStream.range(0, TRACES).mapToObj(this::traceSummary).toList());
        loginTenantAdmin();
    }

    @After
    public void dropTraceSchema() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS span, span_stats_leader, trace_summary, rule_node_exec_stats, " +
                "rule_node_queue_stats, rule_node_queue_lag_stats, trace_group");
    }

    @Test
    public void givenHugePageSize_whenSearchTraces_thenPageIsCappedAt100() throws Exception {
        JsonNode page = search("&pageSize=100000");

        assertEquals(100, page.get("data").size());
        assertEquals(TRACES, page.get("totalElements").asInt());
    }

    @Test
    public void givenNegativePageOrZeroPageSize_whenSearchTraces_thenClampedInsteadOfFailing() throws Exception {
        assertEquals(20, search("&page=-1").get("data").size());
        assertEquals(1, search("&pageSize=0").get("data").size());
    }

    private JsonNode search(String params) throws Exception {
        return doGet(SEARCH_URL + params, JsonNode.class);
    }

    private TraceSummaryInfo traceSummary(int i) {
        UUID ruleNodeId = UUID.randomUUID();
        return new TraceSummaryInfo("trace-" + i, tenantId.getId(), List.of("svc"), START + i, START + i + 100, 100, 3, 1,
                START + i, "Rule Node", 80, 20, null, "POST_TELEMETRY_REQUEST", null, List.of(), List.of("Main"),
                List.of(), List.of(ruleNodeId.toString()), List.of("POST_TELEMETRY_REQUEST"), List.of(), false, false);
    }

}
