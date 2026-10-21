// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.trace;

import com.google.common.io.Resources;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.trace.TracePathDTO;
import org.thingsboard.server.dao.AbstractJpaDaoTest;
import org.thingsboard.server.dao.model.sql.ruleenginemonitoring.TraceGroup;
import org.thingsboard.server.dao.model.sql.trace.SpanAttributeEntity;
import org.thingsboard.server.dao.model.sql.trace.SpanEntity;
import org.thingsboard.server.dao.model.sql.trace.TraceProjection;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Every call here runs outside a transaction, the way TraceService (REST) calls the DAO.
 */
public class JpaTraceDaoTest extends AbstractJpaDaoTest {

    private static final long HOUR = 3_600_000L;
    private static final long DAY = 86_400_000L;
    private static final long BUCKET = 20_000 * DAY;
    private static final String PATH_ID = "path-1";
    private static final String TRACE_ID = "trace-1";

    @Autowired
    private TraceDao traceDao;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID ruleNodeId = UUID.randomUUID();

    @Before
    public void createTraceSchema() throws Exception {
        // The suite DB is built without schema-trace.sql.
        jdbcTemplate.execute(Resources.toString(Resources.getResource("sql/schema-trace.sql"), StandardCharsets.UTF_8));
    }

    @After
    public void dropTraceSchema() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS span, span_stats_leader, trace_summary, rule_node_exec_stats, " +
                "rule_node_queue_stats, rule_node_queue_lag_stats, trace_group");
        // The span partitions went with the table; the next test must create them again.
        traceDao.cleanUpPartitionsCache(Long.MAX_VALUE);
    }

    @Test
    public void givenTraceGroup_whenFindPathsPaginated_thenPathIsReturned() {
        traceDao.saveTraceGroups(List.of(traceGroup(10, 1)), List.of());

        PageData<TracePathDTO> paths = traceDao.findPathsPaginated(tenantId, BUCKET, BUCKET + DAY,
                null, null, null, null, false, 0, 10, null, null);

        assertEquals(1, paths.getTotalElements());
        TracePathDTO path = paths.getData().get(0);
        assertEquals(PATH_ID, path.getPathId());
        assertEquals(10, path.getProcessedTraces());
        assertEquals(1, path.getErrorCount());
    }

    @Test
    public void givenTraceGroup_whenFilterPathsByRuleNodeAndErrors_thenFiltersApply() {
        traceDao.saveTraceGroups(List.of(traceGroup(10, 1)), List.of());

        assertEquals(1, traceDao.findPathsPaginated(tenantId, BUCKET, BUCKET + DAY,
                null, "POST_TELEMETRY_REQUEST", null, ruleNodeId.toString(), true, 0, 10, TracePathSortField.ERROR_COUNT, SortOrder.ASC).getTotalElements());
        assertEquals(0, traceDao.findPathsPaginated(tenantId, BUCKET, BUCKET + DAY,
                null, null, null, UUID.randomUUID().toString(), false, 0, 10, null, null).getTotalElements());
    }

    @Test
    public void givenTraceGroup_whenFindTraceGroupsByPathId_thenGroupIsReturned() {
        traceDao.saveTraceGroups(List.of(traceGroup(10, 1)), List.of());

        List<TraceGroup> groups = traceDao.findTraceGroupsByPathId(tenantId, PATH_ID, BUCKET, BUCKET + DAY);

        assertEquals(1, groups.size());
        assertEquals(List.of(TRACE_ID), groups.get(0).getRelatedTraceIds());
        assertEquals(ruleNodeId, groups.get(0).getVisitedRuleNodes().get(0).getRuleNodeId());
    }

    @Test
    public void givenDayBucket_whenWindowStartsMidDay_thenPathAndGroupsAreFound() {
        traceDao.saveTraceGroups(List.of(traceGroup(10, 1)), List.of());
        long start = BUCKET + 10 * HOUR;
        long end = BUCKET + 11 * HOUR;

        assertEquals(1, traceDao.findPathsPaginated(tenantId, start, end,
                null, null, null, null, false, 0, 10, null, null).getTotalElements());
        assertEquals(1, traceDao.findTraceGroupsByPathId(tenantId, PATH_ID, start, end).size());
    }

    @Test
    public void givenDayBucket_whenWindowEndsAtThatMidnight_thenNextDayIsExcluded() {
        traceDao.saveTraceGroups(List.of(traceGroup(10, 1)), List.of());
        long start = BUCKET - DAY + HOUR;

        assertEquals(0, traceDao.findPathsPaginated(tenantId, start, BUCKET,
                null, null, null, null, false, 0, 10, null, null).getTotalElements());
        assertEquals(0, traceDao.findTraceGroupsByPathId(tenantId, PATH_ID, start, BUCKET).size());
    }

    @Test
    public void givenTraceGroup_whenUpdated_thenFindTraceGroupsAndCountReflectIt() {
        traceDao.saveTraceGroups(List.of(traceGroup(10, 1)), List.of());
        TraceGroup updated = traceGroup(25, 3);
        updated.setRelatedTraceIds(List.of(TRACE_ID, "trace-2"));

        traceDao.saveTraceGroups(List.of(), List.of(updated));

        Map<String, TraceGroup> found = traceDao.findTraceGroups(BUCKET, List.of(traceGroup(0, 0)));
        assertEquals(25, found.get(PATH_ID).getProcessedTraces());
        assertEquals(3, found.get(PATH_ID).getRuleNodePathMetrics().get(ruleNodeId.toString()).getErrors());
        assertEquals(List.of(TRACE_ID, "trace-2"), found.get(PATH_ID).getRelatedTraceIds());
        assertEquals(1, traceDao.countTraceGroups(BUCKET, tenantId));
        assertEquals(0, traceDao.countTraceGroups(BUCKET, UUID.randomUUID()));
    }

    @Test
    public void givenTraceSummary_whenFindTraceByIdAndSearch_thenTraceIsReturned() {
        assertEquals(1, traceDao.upsertTraceSummaries(List.of(traceSummary())));
        assertEquals(0, traceDao.upsertTraceSummaries(List.of(traceSummary())));

        TraceProjection trace = traceDao.findTraceById(tenantId, TRACE_ID);
        assertNotNull(trace);
        assertEquals("svc", trace.getServiceName());
        assertEquals(Integer.valueOf(3), trace.getSpanCount());
        assertEquals(Integer.valueOf(1), trace.getRuleNodeCount());
        assertTrue(trace.getHasErrors());
        assertNull(traceDao.findTraceById(tenantId, "missing"));

        PageData<TraceProjection> page = search(null, true, ruleNodeId.toString(), null, null);
        assertEquals(1, page.getTotalElements());
        assertEquals(TRACE_ID, page.getData().get(0).getTraceId());
        assertEquals(0, search(null, false, UUID.randomUUID().toString(), null, null).getTotalElements());
    }

    @Test
    public void givenTraceGroupWithRelatedTrace_whenSearchByPathId_thenRelatedTraceIsReturned() {
        traceDao.upsertTraceSummaries(List.of(traceSummary()));
        traceDao.saveTraceGroups(List.of(traceGroup(10, 1)), List.of());

        assertEquals(1, search(PATH_ID, false, null, null, null).getTotalElements());
        assertEquals(0, search("unknown-path", false, null, null, null).getTotalElements());
    }

    @Test
    public void givenSpans_whenFindSpansAndSearchBySpanAttributes_thenSpansAreReturned() {
        traceDao.upsertTraceSummaries(List.of(traceSummary()));
        traceDao.saveSpans(List.of(span("span-1", "{\"input.msg.data\":\"{\\\"temperature\\\":42}\",\"rule.node.id\":\"x\"}")));
        traceDao.saveSpans(List.of(span("span-1", "{}"))); // ON CONFLICT DO NOTHING

        List<SpanEntity> spans = traceDao.findSpansByTraceId(tenantId, TRACE_ID);
        assertEquals(1, spans.size());
        List<SpanAttributeEntity> attributes = traceDao.findSpanAttributes(tenantId, "span-1", TRACE_ID);
        assertEquals(2, attributes.size());

        assertEquals(1, search(null, false, null, "Rule Node", null).getTotalElements());
        assertEquals(1, search(null, false, null, null, "temperature").getTotalElements());
        assertEquals(0, search(null, false, null, null, "humidity").getTotalElements());
        // LIKE wildcards in user input are matched literally.
        assertEquals(0, search(null, false, null, null, "temp%42").getTotalElements());
        assertEquals(1, search(null, false, null, null, "\"temperature\":42").getTotalElements());
    }

    @Test
    public void givenLeaderRow_whenTryAcquireAggregationLeader_thenOnlyOneServiceHoldsTheLease() {
        assertTrue(traceDao.tryAcquireAggregationLeader("node-a", 1_000, 10_000));
        assertTrue(traceDao.tryAcquireAggregationLeader("node-a", 2_000, 10_000));
        assertFalse(traceDao.tryAcquireAggregationLeader("node-b", 3_000, 10_000));
        assertTrue(traceDao.tryAcquireAggregationLeader("node-b", 20_000, 10_000));
    }

    @Test
    public void givenTraceDataOfTwoTenants_whenDeleteByTenantId_thenOnlyThatTenantsDataIsRemoved() {
        UUID otherTenantId = UUID.randomUUID();
        traceDao.upsertTraceSummaries(List.of(traceSummary()));
        traceDao.saveSpans(List.of(span("span-1", "{}"), span("span-2", "{}")));
        traceDao.saveTraceGroups(List.of(traceGroup(10, 1)), List.of());
        jdbcTemplate.update("INSERT INTO span (span_id, trace_id, tenant_id, name, start_time, created_time) " +
                "VALUES ('span-1', ?, ?, 'Rule Node', ?, 0)", TRACE_ID, otherTenantId, BUCKET + 100);

        traceDao.deleteByTenantId(tenantId);

        assertEquals(List.of(otherTenantId), jdbcTemplate.queryForList("SELECT tenant_id FROM span", UUID.class));
        assertEquals(0, countRows("trace_summary", tenantId));
        assertEquals(0, countRows("trace_group", tenantId));
    }

    @Test
    public void givenNoTraceSchema_whenDeleteByTenantId_thenNothingFails() {
        dropTraceSchema();

        traceDao.deleteByTenantId(tenantId);
    }

    @Test
    public void givenOldAndRecentTraceData_whenCleanUp_thenOnlyExpiredDataIsRemoved() {
        long recent = BUCKET + 3 * DAY;
        traceDao.saveSpans(List.of(span("old-span", "{}"), spanAt("recent-span", recent)));
        traceDao.upsertTraceSummaries(List.of(traceSummary(), traceSummaryAt("recent-trace", recent)));
        TraceGroup recentGroup = traceGroup(10, 1);
        recentGroup.setBucketTime(recent);
        traceDao.saveTraceGroups(List.of(traceGroup(10, 1), recentGroup), List.of());
        assertEquals(List.of(BUCKET, recent), partitionStarts());

        traceDao.cleanUp(BUCKET + 2 * DAY, BUCKET + 2 * DAY, 1);

        // the old span's whole partition is dropped; batch size 1 still removes every expired row
        assertEquals(List.of(recent), partitionStarts());
        assertEquals(List.of("recent-span"), jdbcTemplate.queryForList("SELECT span_id FROM span", String.class));
        assertEquals(List.of("recent-trace"), jdbcTemplate.queryForList("SELECT trace_id FROM trace_summary", String.class));
        assertEquals(List.of(recent), jdbcTemplate.queryForList("SELECT bucket_time FROM trace_group", Long.class));
    }

    @Test
    public void givenSpanSavedTwice_whenSaveSpans_thenPartitionedTableStillDeduplicates() {
        traceDao.saveSpans(List.of(span("span-1", "{}")));
        traceDao.saveSpans(List.of(span("span-1", "{}")));

        assertEquals(1, countRows("span", tenantId));
    }

    @Test
    public void givenEverySortField_whenSearchTracesAndPaths_thenQueriesRun() {
        traceDao.upsertTraceSummaries(List.of(traceSummary()));
        traceDao.saveSpans(List.of(span("span-1", "{}")));
        traceDao.saveTraceGroups(List.of(traceGroup(10, 1)), List.of());

        for (TraceSortField sortField : TraceSortField.values()) {
            for (SortOrder sortOrder : SortOrder.values()) {
                assertEquals(sortField + " " + sortOrder, 1, traceDao.findTracesPaginated(tenantId, null, BUCKET, BUCKET + DAY,
                        null, null, null, null, null, null, null, null, false, null, null, null, false, 0, 10,
                        sortField, sortOrder).getTotalElements());
            }
        }
        for (TracePathSortField sortField : TracePathSortField.values()) {
            assertEquals(sortField.name(), 1, traceDao.findPathsPaginated(tenantId, BUCKET, BUCKET + DAY,
                    null, null, null, null, false, 0, 10, sortField, SortOrder.ASC).getData().size());
        }
    }

    @Test
    public void givenNoTraceSchema_whenCleanUp_thenNothingFails() {
        dropTraceSchema();

        traceDao.cleanUp(BUCKET, BUCKET, 10);
    }

    private List<Long> partitionStarts() {
        return jdbcTemplate.queryForList("SELECT CAST(substring(c.relname FROM 6) AS bigint) FROM pg_inherits i " +
                "JOIN pg_class c ON c.oid = i.inhrelid WHERE i.inhparent = 'span'::regclass ORDER BY 1", Long.class);
    }

    private SpanEntity spanAt(String spanId, long startTime) {
        return new SpanEntity(spanId, TRACE_ID, tenantId, null, "Rule Node", "INTERNAL", "svc", "{}",
                "[]", "[]", startTime, startTime + 50, 50L, "OK", null, startTime);
    }

    private TraceSummaryInfo traceSummaryAt(String traceId, long startTime) {
        return new TraceSummaryInfo(traceId, tenantId, List.of("svc"), startTime, startTime + 100, 100, 3, 1,
                startTime, "Rule Node", 80, 20, null, "POST_TELEMETRY_REQUEST", null, List.of(), List.of("Main"),
                List.of(), List.of(ruleNodeId.toString()), List.of("POST_TELEMETRY_REQUEST"), List.of(), true, false);
    }

    private int countRows(String table, UUID tenantId) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE tenant_id = ?", Integer.class, tenantId);
    }

    private PageData<TraceProjection> search(String pathId, boolean errorsOnly, String ruleNodeFilter,
                                             String spanName, String messageData) {
        return traceDao.findTracesPaginated(tenantId, null, BUCKET, BUCKET + DAY, spanName, null, null, null, null,
                messageData, null, null, errorsOnly, null, ruleNodeFilter, pathId, false, 0, 10, TraceSortField.START_TIME, SortOrder.DESC);
    }

    private TraceGroup traceGroup(long processedTraces, long errors) {
        TraceGroup group = new TraceGroup();
        group.setBucketTime(BUCKET);
        group.setTenantId(tenantId);
        group.setPathId(PATH_ID);
        group.setRootMessageType("POST_TELEMETRY_REQUEST");
        group.setVisitedRuleNodes(List.of(new TraceGroup.VisitedRuleNode(null, null, ruleNodeId, List.of(), List.of())));
        group.setRelatedTraceIds(List.of(TRACE_ID));
        group.setLastObserved(BUCKET + 1);
        group.setProcessedTraces(processedTraces);
        group.setTotalTime(processedTraces * 5);
        group.setMaxTime(9);
        group.setRuleNodePathMetrics(Map.of(ruleNodeId.toString(), new TraceGroup.RuleNodePathMetric(50, processedTraces, errors, null)));
        return group;
    }

    private TraceSummaryInfo traceSummary() {
        return new TraceSummaryInfo(TRACE_ID, tenantId, List.of("svc"), BUCKET + 100, BUCKET + 200, 100, 3, 1,
                BUCKET + 100, "Rule Node", 80, 20, null, "POST_TELEMETRY_REQUEST", null, List.of(), List.of("Main"),
                List.of(), List.of(ruleNodeId.toString()), List.of("POST_TELEMETRY_REQUEST"), List.of(), true, false);
    }

    private SpanEntity span(String spanId, String attributesJson) {
        return new SpanEntity(spanId, TRACE_ID, tenantId, null, "Rule Node", "INTERNAL", "svc", attributesJson,
                "[]", "[]", BUCKET + 100, BUCKET + 150, 50L, "OK", null, BUCKET + 100);
    }
}
