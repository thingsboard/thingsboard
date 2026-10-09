// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.trace;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.postgresql.util.PGobject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.dao.config.DefaultDataSource;
import org.thingsboard.server.dao.model.sql.ruleenginemonitoring.TraceGroup;
import org.thingsboard.server.dao.model.sql.trace.SpanEntity;
import org.thingsboard.server.dao.util.SqlDao;

import java.sql.Array;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Raw-SQL writes for the trace tables: batch inserts and upserts the JPA layer can't express.
 */
@DefaultDataSource
@Repository
@SqlDao
@Slf4j
@RequiredArgsConstructor
public class TraceInsertRepository {

    private static final String SPAN_INSERT_SQL = """
            INSERT INTO span (span_id, trace_id, tenant_id, service_name, parent_span_id, name, kind, attributes_jsonb, events_jsonb, links_jsonb, start_time, end_time, duration, status_code, status_message, created_time)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (tenant_id, trace_id, span_id) DO NOTHING
            """;

    private static final String TRACE_SUMMARY_INSERT_SQL = """
            INSERT INTO trace_summary (
                trace_id, tenant_id, service_names_jsonb, start_time, end_time, duration,
                span_count, error_count, created_time, first_span_name,
                total_span_time, in_queue_time, root_queue_id, root_message_type, root_rule_chain_id,
                originator_ids_jsonb, queue_names_jsonb,
                rule_chain_ids_jsonb, rule_node_ids_jsonb, message_types_jsonb, message_ids_jsonb,
                has_errors, has_queue_timeouts, last_updated
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, extract(epoch FROM now())::bigint * 1000)
            ON CONFLICT (tenant_id, trace_id) DO NOTHING
            """;

    private static final String TRACE_GROUP_INSERT_SQL = """
            INSERT INTO trace_group (bucket_time, tenant_id, path_id, root_queue_id, root_message_type,
                                     root_rule_chain_id, visited_rule_nodes_jsonb, related_trace_ids, last_related_trace_added_time,
                                     last_observed, processed_traces, processed_traces_with_timeout, processed_traces_with_error,
                                     stored_traces, total_time, max_time, total_in_queue_time,
                                     total_rule_node_processing_time, rule_node_path_metrics_jsonb)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (tenant_id, bucket_time, path_id) DO NOTHING
            """;

    private static final String TRACE_GROUP_UPDATE_SQL = """
            UPDATE trace_group
            SET rule_node_path_metrics_jsonb = ?,
                related_trace_ids = ?,
                last_related_trace_added_time = ?,
                last_observed = ?,
                processed_traces = ?,
                processed_traces_with_timeout = ?,
                processed_traces_with_error = ?,
                stored_traces = ?,
                total_time = ?,
                max_time = ?,
                total_in_queue_time = ?,
                total_rule_node_processing_time = ?
            WHERE tenant_id = ? AND bucket_time = ? AND path_id = ?
            """;

    private static final String AGGREGATION_LEADER_SQL = """
            UPDATE span_stats_leader
               SET leader_id = ?,
                   leader_until = ?
             WHERE lock_name = 'aggregation'
               AND (leader_id = ? OR leader_until < ?)
            """;

    private static final int TRACE_GROUP_BATCH_SIZE = 1000;

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    @Value("${otel.processor.trace.persistence.postgres.insert-batch-size:250}")
    private int spanInsertBatchSize;

    public void saveSpans(List<SpanEntity> spans) {
        if (CollectionUtils.isEmpty(spans)) {
            return;
        }
        int batchSize = Math.max(1, spanInsertBatchSize);
        log.debug("Saving {} spans using JDBC batch chunks of {}", spans.size(), batchSize);
        transactionTemplate.executeWithoutResult(status ->
                jdbcTemplate.batchUpdate(SPAN_INSERT_SQL, spans, batchSize, this::bindSpan));
    }

    public int upsertTraceSummaries(Collection<TraceSummaryInfo> summaries) {
        if (CollectionUtils.isEmpty(summaries)) {
            return 0;
        }
        List<TraceSummaryInfo> summaryList = new ArrayList<>(summaries);
        int[][] updateCounts = transactionTemplate.execute(status ->
                jdbcTemplate.batchUpdate(TRACE_SUMMARY_INSERT_SQL, summaryList, summaryList.size(), this::bindTraceSummary));
        int count = 0;
        for (int[] batch : updateCounts) {
            for (int updateCount : batch) {
                if (updateCount > 0) {
                    count += updateCount;
                } else if (updateCount == Statement.SUCCESS_NO_INFO) {
                    count++;
                }
            }
        }
        return count;
    }

    public void saveTraceGroups(Collection<TraceGroup> toInsertGroups, Collection<TraceGroup> toUpdateGroups) {
        if (CollectionUtils.isEmpty(toInsertGroups) && CollectionUtils.isEmpty(toUpdateGroups)) {
            return;
        }
        transactionTemplate.executeWithoutResult(status -> {
            if (!CollectionUtils.isEmpty(toInsertGroups)) {
                jdbcTemplate.batchUpdate(TRACE_GROUP_INSERT_SQL, new ArrayList<>(toInsertGroups), TRACE_GROUP_BATCH_SIZE, this::bindTraceGroupInsert);
            }
            if (!CollectionUtils.isEmpty(toUpdateGroups)) {
                jdbcTemplate.batchUpdate(TRACE_GROUP_UPDATE_SQL, new ArrayList<>(toUpdateGroups), TRACE_GROUP_BATCH_SIZE, this::bindTraceGroupUpdate);
            }
        });
    }

    public boolean tryAcquireAggregationLeader(String serviceId, long nowMs, long leaseMs) {
        return jdbcTemplate.update(AGGREGATION_LEADER_SQL, serviceId, nowMs + leaseMs, serviceId, nowMs) > 0;
    }

    private void bindSpan(PreparedStatement statement, SpanEntity span) throws SQLException {
        int idx = 1;
        statement.setString(idx++, span.getSpanId());
        statement.setString(idx++, span.getTraceId());
        statement.setObject(idx++, span.getTenantId());
        statement.setString(idx++, span.getServiceName());
        statement.setString(idx++, span.getParentSpanId());
        statement.setString(idx++, span.getName());
        statement.setString(idx++, span.getKind());
        statement.setObject(idx++, toJsonb(normalizeJsonObject(span.getAttributesJsonb())));
        statement.setObject(idx++, toJsonb(normalizeJsonArray(span.getEventsJsonb())));
        statement.setObject(idx++, toJsonb(normalizeJsonArray(span.getLinksJsonb())));
        statement.setObject(idx++, span.getStartTime());
        statement.setObject(idx++, span.getEndTime());
        statement.setObject(idx++, span.getDuration());
        statement.setString(idx++, span.getStatusCode());
        statement.setString(idx++, span.getStatusMessage());
        statement.setObject(idx, span.getCreatedTime());
    }

    private void bindTraceSummary(PreparedStatement statement, TraceSummaryInfo summary) throws SQLException {
        int idx = 1;
        statement.setString(idx++, summary.traceId());
        statement.setObject(idx++, summary.tenantId());
        statement.setObject(idx++, toJsonb(normalizeJsonArray(JacksonUtil.toString(summary.serviceNames()))));
        statement.setLong(idx++, summary.startTime());
        statement.setLong(idx++, summary.endTime());
        statement.setLong(idx++, summary.duration());
        statement.setInt(idx++, summary.spanCount());
        statement.setInt(idx++, summary.errorCount());
        statement.setLong(idx++, summary.createdTime());
        statement.setString(idx++, summary.firstSpanName());
        statement.setLong(idx++, summary.totalSpanTime());
        statement.setLong(idx++, summary.inQueueTime());
        statement.setObject(idx++, summary.rootQueueId());
        statement.setString(idx++, summary.rootMessageType());
        statement.setString(idx++, summary.rootRuleChainId());
        statement.setObject(idx++, toJsonb(normalizeJsonArray(JacksonUtil.toString(summary.originatorIds()))));
        statement.setObject(idx++, toJsonb(normalizeJsonArray(JacksonUtil.toString(summary.queueNames()))));
        statement.setObject(idx++, toJsonb(normalizeJsonArray(JacksonUtil.toString(summary.ruleChainIds()))));
        statement.setObject(idx++, toJsonb(normalizeJsonArray(JacksonUtil.toString(summary.ruleNodeIds()))));
        statement.setObject(idx++, toJsonb(normalizeJsonArray(JacksonUtil.toString(summary.messageTypes()))));
        statement.setObject(idx++, toJsonb(normalizeJsonArray(JacksonUtil.toString(summary.messageIds()))));
        statement.setBoolean(idx++, summary.hasErrors());
        statement.setBoolean(idx, summary.hasQueueTimeouts());
    }

    private void bindTraceGroupInsert(PreparedStatement statement, TraceGroup group) throws SQLException {
        int idx = 1;
        statement.setLong(idx++, group.getBucketTime());
        statement.setObject(idx++, group.getTenantId());
        statement.setString(idx++, group.getPathId());
        statement.setObject(idx++, group.getRootQueueId());
        statement.setString(idx++, group.getRootMessageType());
        statement.setObject(idx++, group.getRootRuleChainId());
        statement.setObject(idx++, toJsonb(normalizeJsonArray(JacksonUtil.toString(group.getVisitedRuleNodes()))));
        statement.setArray(idx++, toVarcharArray(statement, group.getRelatedTraceIds()));
        statement.setLong(idx++, group.getLastRelatedTraceAddedTime());
        statement.setLong(idx++, group.getLastObserved());
        statement.setLong(idx++, group.getProcessedTraces());
        statement.setLong(idx++, group.getProcessedTracesWithTimeout());
        statement.setLong(idx++, group.getProcessedTracesWithError());
        statement.setLong(idx++, group.getStoredTraces());
        statement.setLong(idx++, group.getTotalTime());
        statement.setLong(idx++, group.getMaxTime());
        statement.setLong(idx++, group.getTotalInQueueTime());
        statement.setLong(idx++, group.getTotalRuleNodeProcessingTime());
        statement.setObject(idx, toJsonb(normalizeJsonObject(JacksonUtil.toString(group.getRuleNodePathMetrics()))));
    }

    private void bindTraceGroupUpdate(PreparedStatement statement, TraceGroup group) throws SQLException {
        int idx = 1;
        statement.setObject(idx++, toJsonb(normalizeJsonObject(JacksonUtil.toString(group.getRuleNodePathMetrics()))));
        statement.setArray(idx++, toVarcharArray(statement, group.getRelatedTraceIds()));
        statement.setLong(idx++, group.getLastRelatedTraceAddedTime());
        statement.setLong(idx++, group.getLastObserved());
        statement.setLong(idx++, group.getProcessedTraces());
        statement.setLong(idx++, group.getProcessedTracesWithTimeout());
        statement.setLong(idx++, group.getProcessedTracesWithError());
        statement.setLong(idx++, group.getStoredTraces());
        statement.setLong(idx++, group.getTotalTime());
        statement.setLong(idx++, group.getMaxTime());
        statement.setLong(idx++, group.getTotalInQueueTime());
        statement.setLong(idx++, group.getTotalRuleNodeProcessingTime());
        statement.setObject(idx++, group.getTenantId());
        statement.setLong(idx++, group.getBucketTime());
        statement.setString(idx, group.getPathId());
    }

    private static Array toVarcharArray(PreparedStatement statement, List<String> values) throws SQLException {
        List<String> safe = values == null ? Collections.emptyList() : values;
        return statement.getConnection().createArrayOf("varchar", safe.toArray(new String[0]));
    }

    private static PGobject toJsonb(String value) throws SQLException {
        PGobject jsonb = new PGobject();
        jsonb.setType("jsonb");
        jsonb.setValue(value);
        return jsonb;
    }

    private static String normalizeJsonObject(String value) {
        return (value == null || value.isBlank()) ? "{}" : value;
    }

    private static String normalizeJsonArray(String value) {
        return (value == null || value.isBlank()) ? "[]" : value;
    }

}
