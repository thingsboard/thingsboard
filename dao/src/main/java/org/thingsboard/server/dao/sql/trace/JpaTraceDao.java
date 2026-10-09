/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 * <p>
 * Copyright © 2016-2026 ThingsBoard, Inc. All Rights Reserved.
 * <p>
 * NOTICE: All information contained herein is, and remains
 * the property of ThingsBoard, Inc. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to ThingsBoard, Inc.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * <p>
 * Dissemination of this information or reproduction of this material is strictly forbidden
 * unless prior written permission is obtained from COMPANY.
 * <p>
 * Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
 * managers or contractors who have executed Confidentiality and Non-disclosure agreements
 * explicitly covering such access.
 * <p>
 * The copyright notice above does not evidence any actual or intended publication
 * or disclosure  of  this source code, which includes
 * information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
 * ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
 * OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
 * THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
 * AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
 * THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
 * DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
 * OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
 */
package org.thingsboard.server.dao.sql.trace;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.apache.commons.collections4.CollectionUtils;
import org.postgresql.util.PGobject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.trace.TracePathDTO;
import org.thingsboard.server.dao.config.DefaultDataSource;
import org.thingsboard.server.dao.model.sql.ruleenginemonitoring.TraceGroup;
import org.thingsboard.server.dao.model.sql.trace.SpanAttributeEntity;
import org.thingsboard.server.dao.model.sql.trace.SpanEntity;
import org.thingsboard.server.dao.model.sql.trace.SpanEventEntity;
import org.thingsboard.server.dao.model.sql.trace.SpanLinkEntity;
import org.thingsboard.server.dao.model.sql.trace.TraceProjection;
import org.thingsboard.server.dao.util.SqlDao;

import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@DefaultDataSource
@Component
@SqlDao
@Slf4j
@RequiredArgsConstructor
public class JpaTraceDao implements TraceDao {
    private static final String SPAN_INSERT_SQL =
            """
                            INSERT INTO span (span_id, trace_id, tenant_id, service_name, parent_span_id, name, kind, attributes_jsonb, events_jsonb, links_jsonb, start_time, end_time, duration, status_code, status_message, created_time)
                            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                            ON CONFLICT (tenant_id, trace_id, span_id) DO NOTHING
                    """;

    private static final String TRACE_GROUP_INSERT_SQL =
            """
                    INSERT INTO trace_group (bucket_time, tenant_id, path_id, root_queue_id, root_message_type,
                                             root_rule_chain_id, visited_rule_nodes_jsonb, related_trace_ids, last_related_trace_added_time,
                                             last_observed, processed_traces, processed_traces_with_timeout, processed_traces_with_error,
                                             stored_traces, total_time, max_time, total_in_queue_time,
                                             total_rule_node_processing_time, rule_node_path_metrics_jsonb)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT (tenant_id, bucket_time, path_id) DO NOTHING
                    """;

    private static final String PATH_ERROR_SUM =
            "SUM((SELECT COALESCE(SUM((v->>'errors')::bigint), 0) " +
                    "FROM jsonb_each(rule_node_path_metrics_jsonb) AS e(k, v)))";

    private static final String TRACE_GROUP_UPDATE_SQL =
            """
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

    private static final long DAY_BUCKET_MS = 86_400_000L;

    private static final int TRACE_GROUP_BATCH_SIZE = 1000;

    @Value("${otel.processor.trace.persistence.postgres.insert-batch-size:250}")
    private int spanInsertBatchSize;

    protected EntityManager entityManager;

    @PersistenceContext
    public void setEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    private final SpanRepository spanRepository;

    @Override
    @Transactional
    public void saveSpans(List<SpanEntity> spans) {
        if (CollectionUtils.isEmpty(spans)) {
            return;
        }

        int effectiveBatchSize = Math.max(1, spanInsertBatchSize);
        log.debug("Saving {} spans using JDBC batch chunks of {}", spans.size(), effectiveBatchSize);
        for (int fromIndex = 0; fromIndex < spans.size(); fromIndex += effectiveBatchSize) {
            int toIndex = Math.min(fromIndex + effectiveBatchSize, spans.size());
            insertSpanChunk(spans.subList(fromIndex, toIndex));
        }
    }

    @Override
    public Map<String, TraceGroup> findTraceGroups(long bucketTime, Collection<TraceGroup> groups) {
        if (CollectionUtils.isEmpty(groups)) {
            return Collections.emptyMap();
        }
        List<TraceGroup> groupList = new ArrayList<>(groups);
        Map<String, TraceGroup> result = new HashMap<>();
        entityManager.unwrap(Session.class).doWork(connection -> {
            for (int start = 0; start < groupList.size(); start += TRACE_GROUP_BATCH_SIZE) {
                List<TraceGroup> chunk = groupList.subList(start,
                        Math.min(start + TRACE_GROUP_BATCH_SIZE, groupList.size()));
                String placeholders = chunk.stream().map(g -> "(?, ?)").collect(Collectors.joining(", "));
                String sql = """
                        SELECT tenant_id, path_id, root_queue_id, root_message_type, root_rule_chain_id,
                               visited_rule_nodes_jsonb, related_trace_ids, last_related_trace_added_time,
                               last_observed, processed_traces, processed_traces_with_timeout, processed_traces_with_error,
                               stored_traces, total_time, max_time, total_in_queue_time,
                               total_rule_node_processing_time, rule_node_path_metrics_jsonb
                        FROM trace_group
                        WHERE bucket_time = ? AND (tenant_id, path_id) IN (%s)
                        """.formatted(placeholders);
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    int idx = 1;
                    statement.setLong(idx++, bucketTime);
                    for (TraceGroup g : chunk) {
                        statement.setObject(idx++, g.getTenantId());
                        statement.setString(idx++, g.getPathId());
                    }
                    try (ResultSet rs = statement.executeQuery()) {
                        while (rs.next()) {
                            UUID tenantId = rs.getObject("tenant_id", UUID.class);
                            String pathId = rs.getString("path_id");
                            result.put(pathId, mapTraceGroup(rs, tenantId, bucketTime, pathId));
                        }
                    }
                }
            }
        });
        return result;
    }

    @Override
    public List<TraceGroup> findTraceGroupsByPathId(UUID tenantId, String pathId, long startTime, long endTime) {
        List<TraceGroup> result = new ArrayList<>();
        entityManager.unwrap(Session.class).doWork(connection -> {
            String sql = """
                    SELECT bucket_time, root_queue_id, root_message_type, root_rule_chain_id, visited_rule_nodes_jsonb,
                           related_trace_ids, last_related_trace_added_time, last_observed, processed_traces, stored_traces,
                           processed_traces_with_timeout, processed_traces_with_error, total_time, max_time,
                           total_in_queue_time, total_rule_node_processing_time,
                           rule_node_path_metrics_jsonb
                    FROM trace_group
                    WHERE tenant_id = ? AND path_id = ? AND bucket_time >= ? AND bucket_time < ?
                    ORDER BY bucket_time
                    """;
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setObject(1, tenantId);
                statement.setString(2, pathId);
                statement.setLong(3, startTime);
                statement.setLong(4, endTime);
                try (ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) {
                        result.add(mapTraceGroup(rs, tenantId, rs.getLong("bucket_time"), pathId));
                    }
                }
            }
        });
        return result;
    }

    @Override
    public long countTraceGroups(long bucketTime, UUID tenantId) {
        long[] count = {0L};
        entityManager.unwrap(Session.class).doWork(connection -> {
            String sql = "SELECT COUNT(*) FROM trace_group WHERE bucket_time = ? AND tenant_id = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setLong(1, bucketTime);
                statement.setObject(2, tenantId);
                try (ResultSet rs = statement.executeQuery()) {
                    if (rs.next()) {
                        count[0] = rs.getLong(1);
                    }
                }
            }
        });
        return count[0];
    }

    @Override
    @Transactional
    public void saveTraceGroups(Collection<TraceGroup> toInsertGroups, Collection<TraceGroup> toUpdateGroups) {
        if (!CollectionUtils.isEmpty(toInsertGroups)) {
            List<TraceGroup> toInsertGroupList = new ArrayList<>(toInsertGroups);
            entityManager.unwrap(Session.class).doWork(connection -> {
                for (int start = 0; start < toInsertGroupList.size(); start += TRACE_GROUP_BATCH_SIZE) {
                    List<TraceGroup> chunk = toInsertGroupList.subList(start,
                            Math.min(start + TRACE_GROUP_BATCH_SIZE, toInsertGroupList.size()));
                    try (PreparedStatement statement = connection.prepareStatement(TRACE_GROUP_INSERT_SQL)) {
                        for (TraceGroup group : chunk) {
                            bindTraceGroupInsert(statement, connection, group);
                            statement.addBatch();
                        }
                        statement.executeBatch();
                    }
                }
            });
        }

        if (!CollectionUtils.isEmpty(toUpdateGroups)) {
            List<TraceGroup> toUpdateGroupList = new ArrayList<>(toUpdateGroups);
            entityManager.unwrap(Session.class).doWork(connection -> {
                for (int start = 0; start < toUpdateGroupList.size(); start += TRACE_GROUP_BATCH_SIZE) {
                    List<TraceGroup> chunk = toUpdateGroupList.subList(start,
                            Math.min(start + TRACE_GROUP_BATCH_SIZE, toUpdateGroupList.size()));
                    try (PreparedStatement statement = connection.prepareStatement(TRACE_GROUP_UPDATE_SQL)) {
                        for (TraceGroup group : chunk) {
                            bindTraceGroupUpdate(statement, connection, group);
                            statement.addBatch();
                        }
                        statement.executeBatch();
                    }
                }
            });
        }
    }

    @Override
    public PageData<TracePathDTO> findPathsPaginated(UUID tenantId, long startTime, long endTime,
                                                     String rootQueueId, String rootMessageType, String rootRuleChainId,
                                                     String ruleNodeId, boolean errorsOnly,
                                                     int page, int pageSize, String sortField, String sortOrder) {
        int safePageSize = Math.max(1, pageSize);
        int safePage = Math.max(0, page);

        StringBuilder where = new StringBuilder("WHERE tenant_id = ? AND bucket_time >= ? AND bucket_time < ?");
        if (rootQueueId != null) {
            where.append(" AND root_queue_id = ?");
        }
        if (rootMessageType != null) {
            where.append(" AND root_message_type = ?");
        }
        if (rootRuleChainId != null) {
            where.append(" AND root_rule_chain_id = ?");
        }
        if (ruleNodeId != null) {
            where.append(" AND visited_rule_nodes_jsonb @> ?::jsonb");
        }

        String groupBy = " GROUP BY path_id, root_queue_id, root_message_type, root_rule_chain_id";
        String having = errorsOnly ? " HAVING " + PATH_ERROR_SUM + " > 0" : "";

        String dataSql = "SELECT path_id, root_queue_id, root_message_type, root_rule_chain_id, " +
                "SUM(processed_traces) AS processed_traces, " +
                "SUM(processed_traces_with_timeout) AS processed_traces_with_timeout, " +
                "SUM(processed_traces_with_error) AS processed_traces_with_error, " +
                "SUM(stored_traces) AS stored_traces, " +
                "SUM(total_time) AS total_time, MAX(max_time) AS max_time, " +
                "SUM(total_in_queue_time) AS total_in_queue_time, " +
                "SUM(total_rule_node_processing_time) AS total_rule_node_processing_time, " +
                "COALESCE(SUM(total_time)::float8 / NULLIF(SUM(processed_traces), 0), 0) AS avg_time, " +
                "COALESCE(SUM(total_in_queue_time)::float8 / NULLIF(SUM(processed_traces), 0), 0) AS avg_in_queue_time, " +
                "COALESCE(SUM(total_rule_node_processing_time)::float8 / NULLIF(SUM(processed_traces), 0), 0) AS avg_rule_node_processing_time, " +
                "MAX(last_observed) AS last_observed, " +
                PATH_ERROR_SUM + " AS error_count " +
                "FROM trace_group " + where + groupBy + having +
                " ORDER BY " + pathSortColumn(sortField) + " " + ("ASC".equalsIgnoreCase(sortOrder) ? "ASC" : "DESC") +
                ", path_id ASC LIMIT ? OFFSET ?";

        String countSql = "SELECT COUNT(*) FROM (SELECT path_id FROM trace_group " + where + groupBy + having + ") sub";

        AtomicReference<PageData<TracePathDTO>> result = new AtomicReference<>();
        entityManager.unwrap(Session.class).doWork(connection -> {
            long total;
            try (PreparedStatement statement = connection.prepareStatement(countSql)) {
                bindPathFilters(statement, tenantId, startTime, endTime, rootQueueId, rootMessageType, rootRuleChainId, ruleNodeId);
                try (ResultSet rs = statement.executeQuery()) {
                    rs.next();
                    total = rs.getLong(1);
                }
            }
            List<TracePathDTO> data = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(dataSql)) {
                int idx = bindPathFilters(statement, tenantId, startTime, endTime, rootQueueId, rootMessageType, rootRuleChainId, ruleNodeId);
                statement.setInt(idx++, safePageSize);
                statement.setInt(idx, safePage * safePageSize);
                try (ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) {
                        data.add(mapTracePath(rs));
                    }
                }
            }
            int totalPages = (int) ((total + safePageSize - 1) / safePageSize);
            boolean hasNext = (long) (safePage + 1) * safePageSize < total;
            result.set(new PageData<>(data, totalPages, total, hasNext));
        });
        return result.get();
    }

    @Override
    public TraceProjection findTraceById(UUID tenantId, String traceId) {
        return findTraceSummaryById(tenantId, traceId);
    }

    @Override
    public List<SpanEntity> findSpansByTraceId(UUID tenantId, String traceId) {
        return spanRepository.findByTenantIdAndTraceId(tenantId, traceId);
    }

    @Override
    public List<SpanAttributeEntity> findSpanAttributes(UUID tenantId, String spanId, String traceId) {
        List<SpanEntity> spans = spanRepository.findByTenantIdAndTraceIdAndSpanId(tenantId, traceId, spanId);
        if (spans.isEmpty()) {
            return Collections.emptyList();
        }
        return toSpanAttributes(spans.get(0));
    }

    @Override
    public List<SpanAttributeEntity> findSpanAttributesByTraceId(UUID tenantId, String traceId) {
        return spanRepository.findByTenantIdAndTraceId(tenantId, traceId).stream()
                .map(this::toSpanAttributes)
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    @Override
    public List<SpanEventEntity> findSpanEvents(UUID tenantId, String spanId, String traceId) {
        List<SpanEntity> spans = spanRepository.findByTenantIdAndTraceIdAndSpanId(tenantId, traceId, spanId);
        if (spans.isEmpty()) {
            return Collections.emptyList();
        }
        return toSpanEvents(spans.get(0));
    }

    @Override
    public List<SpanLinkEntity> findSpanLinks(UUID tenantId, String spanId, String traceId) {
        List<SpanEntity> spans = spanRepository.findByTenantIdAndTraceIdAndSpanId(tenantId, traceId, spanId);
        if (spans.isEmpty()) {
            return Collections.emptyList();
        }
        return toSpanLinks(spans.get(0));
    }

    @Override
    public PageData<TraceProjection> findTracesPaginated(UUID tenantId, String serviceName, long startTime, long endTime, String spanName, String queueName, String originatorId, String messageType, String messageId, String messageData, String messageMetadata, String traceId, boolean errorsOnly, String ruleChainId, String ruleNodeId, String pathId, boolean hasTimeOutsByQueue, int page, int pageSize, String sortField, String sortOrder) {
        if (traceId != null && !traceId.isEmpty()) {
            TraceProjection trace = findTraceSummaryById(tenantId, traceId);
            if (trace != null) {
                return new PageData<>(Collections.singletonList(trace), 1, 1, false);
            } else {
                return new PageData<>(Collections.emptyList(), 0, 0, false);
            }
        }

        Collection<String> pathTraceIds = null;
        if (pathId != null && !pathId.isEmpty()) {
            long bucketStart = floorToDay(startTime);
            long bucketEnd = floorToDay(endTime) + DAY_BUCKET_MS;
            List<TraceGroup> groups = findTraceGroupsByPathId(tenantId, pathId, bucketStart, bucketEnd);
            Set<String> ids = new HashSet<>();
            for (TraceGroup group : groups) {
                if (group.getRelatedTraceIds() != null) {
                    for (String id : group.getRelatedTraceIds()) {
                        if (id != null && !id.isEmpty()) {
                            ids.add(id);
                        }
                    }
                }
            }
            if (ids.isEmpty()) {
                return new PageData<>(Collections.emptyList(), 0, 0, false);
            }
            pathTraceIds = ids;
        }

        Pageable pageable = PageRequest.of(page, pageSize);
        Page<TraceProjection> resultPage = findTracesWithAttributeFilters(tenantId, serviceName, startTime, endTime, spanName, queueName, originatorId, messageType, messageId, messageData, messageMetadata, errorsOnly, ruleChainId, ruleNodeId, pathTraceIds, hasTimeOutsByQueue, sortField, sortOrder, pageable);

        return new PageData<>(resultPage.getContent(), resultPage.getTotalPages(), resultPage.getTotalElements(), resultPage.hasNext());
    }

    private static long floorToDay(long timestampMs) {
        return (timestampMs / DAY_BUCKET_MS) * DAY_BUCKET_MS;
    }

    private void bindTraceGroupInsert(PreparedStatement statement, Connection connection, TraceGroup group) throws SQLException {
        int idx = 1;
        statement.setLong(idx++, group.getBucketTime());
        statement.setObject(idx++, group.getTenantId());
        statement.setString(idx++, group.getPathId());
        statement.setObject(idx++, group.getRootQueueId());
        statement.setString(idx++, group.getRootMessageType());
        statement.setObject(idx++, group.getRootRuleChainId());
        statement.setObject(idx++, toJsonb(normalizeJsonArray(JacksonUtil.toString(group.getVisitedRuleNodes()))));
        statement.setArray(idx++, toVarcharArray(connection, group.getRelatedTraceIds()));
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

    private void bindTraceGroupUpdate(PreparedStatement statement, Connection connection, TraceGroup group) throws SQLException {
        int idx = 1;
        statement.setObject(idx++, toJsonb(normalizeJsonObject(JacksonUtil.toString(group.getRuleNodePathMetrics()))));
        statement.setArray(idx++, toVarcharArray(connection, group.getRelatedTraceIds()));
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

    private static int bindPathFilters(PreparedStatement statement, UUID tenantId, long startTime, long endTime,
                                       String rootQueueId, String rootMessageType, String rootRuleChainId,
                                       String ruleNodeId) throws SQLException {
        int idx = 1;
        statement.setObject(idx++, tenantId);
        statement.setLong(idx++, startTime);
        statement.setLong(idx++, endTime);
        if (rootQueueId != null) {
            statement.setObject(idx++, UUID.fromString(rootQueueId));
        }
        if (rootMessageType != null) {
            statement.setString(idx++, rootMessageType);
        }
        if (rootRuleChainId != null) {
            statement.setObject(idx++, UUID.fromString(rootRuleChainId));
        }
        if (ruleNodeId != null) {
            statement.setString(idx++, "[{\"ruleNodeId\":\"" + ruleNodeId + "\"}]");
        }
        return idx;
    }

    private static String pathSortColumn(String sortField) {
        if (sortField == null) {
            return "processed_traces";
        }
        return switch (sortField) {
            case "LAST_OBSERVED" -> "last_observed";
            case "AVG_TIME" -> "avg_time";
            case "AVG_IN_QUEUE_TIME" -> "avg_in_queue_time";
            case "AVG_RULE_NODE_PROCESSING_TIME" -> "avg_rule_node_processing_time";
            case "MAX_TIME" -> "max_time";
            case "ERROR_COUNT" -> "error_count";
            case "ROOT_QUEUE" -> "root_queue_id";
            case "ROOT_MESSAGE_TYPE" -> "root_message_type";
            case "ROOT_RULE_CHAIN" -> "root_rule_chain_id";
            case "TRACES_COUNT" -> "processed_traces";
            case "STORED_TRACES" -> "stored_traces";
            case "TIMEOUT_TRACES" -> "processed_traces_with_timeout";
            case "ERROR_TRACES" -> "processed_traces_with_error";
            default -> "processed_traces";
        };
    }

    private TracePathDTO mapTracePath(ResultSet rs) throws SQLException {
        UUID rootQueueId = rs.getObject("root_queue_id", UUID.class);
        UUID rootRuleChainId = rs.getObject("root_rule_chain_id", UUID.class);
        return TracePathDTO.builder()
                .pathId(rs.getString("path_id"))
                .rootQueueId(rootQueueId != null ? rootQueueId.toString() : null)
                .rootMessageType(rs.getString("root_message_type"))
                .rootRuleChainId(rootRuleChainId != null ? rootRuleChainId.toString() : null)
                .processedTraces(rs.getLong("processed_traces"))
                .processedTracesWithTimeout(rs.getLong("processed_traces_with_timeout"))
                .processedTracesWithError(rs.getLong("processed_traces_with_error"))
                .storedTraces(rs.getLong("stored_traces"))
                .totalTime(rs.getLong("total_time"))
                .maxTime(rs.getLong("max_time"))
                .totalInQueueTime(rs.getLong("total_in_queue_time"))
                .totalRuleNodeProcessingTime(rs.getLong("total_rule_node_processing_time"))
                .errorCount(rs.getLong("error_count"))
                .lastObserved(rs.getLong("last_observed"))
                .build();
    }

    private TraceGroup mapTraceGroup(ResultSet rs, UUID tenantId, long bucketTime, String pathId) throws SQLException {
        TraceGroup group = new TraceGroup();
        group.setBucketTime(bucketTime);
        group.setTenantId(tenantId);
        group.setPathId(pathId);
        group.setRootQueueId(rs.getObject("root_queue_id", UUID.class));
        group.setRootMessageType(rs.getString("root_message_type"));
        group.setRootRuleChainId(rs.getObject("root_rule_chain_id", UUID.class));
        group.setVisitedRuleNodes(JacksonUtil.fromString(rs.getString("visited_rule_nodes_jsonb"),
                new TypeReference<>() {
                }));
        Array relatedTraceIds = rs.getArray("related_trace_ids");
        if (relatedTraceIds != null) {
            group.setRelatedTraceIds(new ArrayList<>(Arrays.asList((String[]) relatedTraceIds.getArray())));
        } else {
            group.setRelatedTraceIds(new ArrayList<>());
        }
        group.setLastRelatedTraceAddedTime(rs.getLong("last_related_trace_added_time"));
        group.setLastObserved(rs.getLong("last_observed"));
        group.setProcessedTraces(rs.getLong("processed_traces"));
        group.setProcessedTracesWithTimeout(rs.getLong("processed_traces_with_timeout"));
        group.setProcessedTracesWithError(rs.getLong("processed_traces_with_error"));
        group.setStoredTraces(rs.getLong("stored_traces"));
        group.setTotalTime(rs.getLong("total_time"));
        group.setMaxTime(rs.getLong("max_time"));
        group.setTotalInQueueTime(rs.getLong("total_in_queue_time"));
        group.setTotalRuleNodeProcessingTime(rs.getLong("total_rule_node_processing_time"));
        group.setRuleNodePathMetrics(JacksonUtil.fromString(rs.getString("rule_node_path_metrics_jsonb"),
                new TypeReference<>() {
                }));
        return group;
    }

    private static Array toVarcharArray(Connection connection, List<String> values) throws SQLException {
        List<String> safe = values == null ? Collections.emptyList() : values;
        return connection.createArrayOf("varchar", safe.toArray(new String[0]));
    }

    private TraceProjection findTraceSummaryById(UUID tenantId, String traceId) {
        String sql = "SELECT ts.trace_id, " + traceSummaryFirstServiceNameExpression("ts") + " AS service_name, " +
                "ts.start_time, ts.end_time, (ts.end_time - ts.start_time) AS duration, " +
                "ts.span_count, ts.error_count, ts.created_time, ts.first_span_name, " +
                "ts.total_span_time, ts.in_queue_time, " +
                "ts.root_queue_id::text AS root_queue_id, " +
                "COALESCE(ts.root_message_type, ts.message_types_jsonb ->> 0) AS message_type, " +
                "COALESCE(ts.root_rule_chain_id, ts.rule_chain_ids_jsonb ->> 0) AS root_rule_chain_id, " +
                "jsonb_array_length(ts.service_names_jsonb) AS service_count, " +
                "jsonb_array_length(ts.queue_names_jsonb) AS queue_count, " +
                "jsonb_array_length(ts.rule_chain_ids_jsonb) AS rule_chain_count, " +
                "jsonb_array_length(ts.rule_node_ids_jsonb) AS rule_node_count, " +
                "ts.has_errors, ts.has_queue_timeouts " +
                "FROM trace_summary ts WHERE ts.tenant_id = :tenantId AND ts.trace_id = :traceId";
        Query query = entityManager.createNativeQuery(sql, "TraceMapping");
        query.setParameter("tenantId", tenantId);
        query.setParameter("traceId", traceId);
        @SuppressWarnings("unchecked") List<TraceProjection> result = query.getResultList();
        return result.isEmpty() ? null : result.get(0);
    }

    private void insertSpanChunk(List<SpanEntity> chunk) {
        entityManager.unwrap(Session.class).doWork(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(SPAN_INSERT_SQL)) {
                for (SpanEntity span : chunk) {
                    bindSpanBatchParameters(statement, span);
                    statement.addBatch();
                }
                statement.executeBatch();
            }
        });
    }

    private void bindSpanBatchParameters(PreparedStatement statement, SpanEntity span) throws SQLException {
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

    private String getNativeQueryOrderByClause(String sortField, boolean isAsc, String alias) {
        String direction = isAsc ? "ASC" : "DESC";
        String a = alias + ".";

        return switch (sortField) {
            case "FIRST_SPAN_NAME" ->
                    "ORDER BY (SELECT sp.name FROM span sp WHERE sp.trace_id = " + a + "trace_id AND sp.tenant_id = " + a + "tenant_id ORDER BY sp.start_time ASC LIMIT 1) " + direction + ", " + a + "trace_id ASC ";
            case "DURATION" -> "ORDER BY " + a + "duration " + direction + ", " + a + "trace_id ASC ";
            case "END_TIME" -> "ORDER BY " + a + "end_time " + direction + ", " + a + "trace_id ASC ";
            case "SPAN_COUNT" -> "ORDER BY " + a + "span_count " + direction + ", " + a + "trace_id ASC ";
            case "ERROR_COUNT" -> "ORDER BY " + a + "error_count " + direction + ", " + a + "trace_id ASC ";
            case "RULE_NODE_COUNT" ->
                    "ORDER BY COALESCE(jsonb_array_length(" + a + "rule_node_ids_jsonb), 0) " + direction + ", " + a + "trace_id ASC ";
            case "CREATED_TIME" -> "ORDER BY " + a + "created_time " + direction + ", " + a + "trace_id ASC ";
            case "TOTAL_SPAN_TIME" ->
                    "ORDER BY " + a + "total_span_time " + direction + " NULLS LAST, " + a + "trace_id ASC ";
            case "IN_QUEUE_TIME" ->
                    "ORDER BY " + a + "in_queue_time " + direction + " NULLS LAST, " + a + "trace_id ASC ";
            default -> "ORDER BY " + a + "start_time " + direction + ", " + a + "trace_id ASC ";
        };
    }

    private static String traceScopedSpanAttributePredicate(String outerAlias, String attributeKey, String paramName, boolean useLike) {
        return "AND EXISTS (" +
                "SELECT 1 FROM span s " +
                "WHERE s.trace_id = " + outerAlias + ".trace_id " +
                "AND s.tenant_id = " + outerAlias + ".tenant_id " +
                "AND COALESCE(s.attributes_jsonb ->> '" + attributeKey + "', '') " +
                (useLike ? "LIKE :" + paramName + " ESCAPE '\\'" : "= :" + paramName) +
                ") ";
    }

    private static String traceScopedSpanNamePredicate(String outerAlias) {
        return "AND EXISTS (" +
                "SELECT 1 FROM span sp " +
                "WHERE sp.trace_id = " + outerAlias + ".trace_id " +
                "AND sp.tenant_id = " + outerAlias + ".tenant_id " +
                "AND sp.name = :spanName" +
                ") ";
    }

    private static String traceSummaryJsonbMembershipPredicate(String outerAlias, String columnName, String paramName) {
        return "AND jsonb_exists(" + outerAlias + "." + columnName + ", :" + paramName + ") ";
    }

    private static String traceSummaryFirstServiceNameExpression(String outerAlias) {
        return "COALESCE(" + outerAlias + ".service_names_jsonb ->> 0, '')";
    }

    private String buildTraceSummaryFilters(UUID tenantId, String serviceName, long startTime, long endTime, String spanName,
                                            String queueName, String originatorId, String messageType, String messageId,
                                            String messageData, String messageMetadata,
                                            boolean errorsOnly, String ruleChainId, String ruleNodeId,
                                            Collection<String> pathTraceIds, boolean hasTimeOutsByQueue) {
        StringBuilder sql = new StringBuilder();
        sql.append("FROM trace_summary ts ");
        sql.append("WHERE ts.tenant_id = :tenantId ");
        sql.append("AND ts.start_time >= :startTime AND ts.start_time <= :endTime ");

        if (pathTraceIds != null && !pathTraceIds.isEmpty()) {
            sql.append("AND ts.trace_id IN (:pathTraceIds) ");
        }

        if (serviceName != null && !serviceName.isEmpty()) {
            sql.append(traceSummaryJsonbMembershipPredicate("ts", "service_names_jsonb", "serviceName"));
        }
        if (errorsOnly) {
            sql.append("AND ts.has_errors = true ");
        }
        if (ruleChainId != null && !ruleChainId.isEmpty()) {
            sql.append(traceSummaryJsonbMembershipPredicate("ts", "rule_chain_ids_jsonb", "ruleChainId"));
        }
        if (originatorId != null && !originatorId.isEmpty()) {
            sql.append(traceSummaryJsonbMembershipPredicate("ts", "originator_ids_jsonb", "originatorId"));
        }
        if (messageType != null && !messageType.isEmpty()) {
            sql.append(traceSummaryJsonbMembershipPredicate("ts", "message_types_jsonb", "messageType"));
        }
        if (messageId != null && !messageId.isEmpty()) {
            sql.append(traceSummaryJsonbMembershipPredicate("ts", "message_ids_jsonb", "messageId"));
        }
        if (hasTimeOutsByQueue) {
            sql.append("AND ts.has_queue_timeouts = true ");
            if (queueName != null && !queueName.isEmpty()) {
                sql.append(traceSummaryJsonbMembershipPredicate("ts", "queue_names_jsonb", "queueName"));
            }
        } else if (queueName != null && !queueName.isEmpty()) {
            sql.append(traceSummaryJsonbMembershipPredicate("ts", "queue_names_jsonb", "queueName"));
        }

        if (ruleNodeId != null && !ruleNodeId.isEmpty()) {
            sql.append(traceSummaryJsonbMembershipPredicate("ts", "rule_node_ids_jsonb", "filterRuleNodeId"));
        }
        if (spanName != null && !spanName.isEmpty()) {
            sql.append(traceScopedSpanNamePredicate("ts"));
        }
        if (messageData != null && !messageData.isEmpty()) {
            sql.append(traceScopedSpanAttributePredicate("ts", "input.msg.data", "messageData", true));
        }
        if (messageMetadata != null && !messageMetadata.isEmpty()) {
            sql.append(traceScopedSpanAttributePredicate("ts", "input.msg.metadata", "messageMetadata", true));
        }

        return sql.toString();
    }

    private void setTraceSummaryFilterParameters(Query query, UUID tenantId, String serviceName, long startTime, long endTime,
                                                 String spanName, String queueName, String originatorId, String messageType,
                                                 String messageId, String messageData, String messageMetadata,
                                                 String ruleChainId, String ruleNodeId, Collection<String> pathTraceIds) {
        query.setParameter("tenantId", tenantId);
        query.setParameter("startTime", startTime);
        query.setParameter("endTime", endTime);

        if (pathTraceIds != null && !pathTraceIds.isEmpty()) {
            query.setParameter("pathTraceIds", pathTraceIds);
        }

        if (serviceName != null && !serviceName.isEmpty()) {
            query.setParameter("serviceName", serviceName);
        }
        if (ruleChainId != null && !ruleChainId.isEmpty()) {
            query.setParameter("ruleChainId", ruleChainId);
        }
        if (originatorId != null && !originatorId.isEmpty()) {
            query.setParameter("originatorId", originatorId);
        }
        if (messageType != null && !messageType.isEmpty()) {
            query.setParameter("messageType", messageType);
        }
        if (messageId != null && !messageId.isEmpty()) {
            query.setParameter("messageId", messageId);
        }
        if (queueName != null && !queueName.isEmpty()) {
            query.setParameter("queueName", queueName);
        }
        if (ruleNodeId != null && !ruleNodeId.isEmpty()) {
            query.setParameter("filterRuleNodeId", ruleNodeId);
        }
        if (spanName != null && !spanName.isEmpty()) {
            query.setParameter("spanName", spanName);
        }
        if (messageData != null && !messageData.isEmpty()) {
            String escaped = "%" + messageData.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
            query.setParameter("messageData", escaped);
        }
        if (messageMetadata != null && !messageMetadata.isEmpty()) {
            String escaped = "%" + messageMetadata.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
            query.setParameter("messageMetadata", escaped);
        }
    }

    private Page<TraceProjection> findTracesWithAttributeFilters(UUID tenantId, String serviceName, long startTime, long endTime, String spanName, String queueName, String originatorId, String messageType, String messageId, String messageData, String messageMetadata, boolean errorsOnly, String ruleChainId, String ruleNodeId, Collection<String> pathTraceIds, boolean hasTimeOutsByQueue, String sortField, String sortOrder, Pageable pageable) {
        boolean isAsc = "ASC".equals(sortOrder);
        String filters = buildTraceSummaryFilters(tenantId, serviceName, startTime, endTime, spanName, queueName,
                originatorId, messageType, messageId, messageData, messageMetadata, errorsOnly,
                ruleChainId, ruleNodeId, pathTraceIds, hasTimeOutsByQueue);

        Query countQuery = entityManager.createNativeQuery("SELECT COUNT(*) " + filters);
        setTraceSummaryFilterParameters(countQuery, tenantId, serviceName, startTime, endTime, spanName, queueName,
                originatorId, messageType, messageId, messageData, messageMetadata, ruleChainId,
                ruleNodeId, pathTraceIds);
        Number totalElementsResult = (Number) countQuery.getSingleResult();
        long totalElements = totalElementsResult != null ? totalElementsResult.longValue() : 0L;
        if (totalElements == 0L) {
            return new PageImpl<>(Collections.emptyList(), pageable, 0L);
        }

        StringBuilder dataSql = new StringBuilder();
        dataSql.append("SELECT ts.trace_id, ").append(traceSummaryFirstServiceNameExpression("ts")).append(" AS service_name, ts.start_time, ts.end_time, ");
        dataSql.append("(ts.end_time - ts.start_time) AS duration, ");
        dataSql.append("ts.span_count, ts.error_count, ts.created_time, ");
        dataSql.append("ts.first_span_name, ");
        dataSql.append("ts.total_span_time, ts.in_queue_time, ");
        dataSql.append("ts.root_queue_id::text AS root_queue_id, ");
        dataSql.append("COALESCE(ts.root_message_type, ts.message_types_jsonb ->> 0) AS message_type, ");
        dataSql.append("COALESCE(ts.root_rule_chain_id, ts.rule_chain_ids_jsonb ->> 0) AS root_rule_chain_id, ");
        dataSql.append("jsonb_array_length(ts.service_names_jsonb) AS service_count, ");
        dataSql.append("jsonb_array_length(ts.queue_names_jsonb) AS queue_count, ");
        dataSql.append("jsonb_array_length(ts.rule_chain_ids_jsonb) AS rule_chain_count, ");
        dataSql.append("jsonb_array_length(ts.rule_node_ids_jsonb) AS rule_node_count, ");
        dataSql.append("ts.has_errors, ts.has_queue_timeouts ");
        dataSql.append(filters);
        dataSql.append(getNativeQueryOrderByClause(sortField, isAsc, "ts"));
        dataSql.append("LIMIT :pageSize OFFSET :offset");

        Query query = entityManager.createNativeQuery(dataSql.toString(), "TraceMapping");
        setTraceSummaryFilterParameters(query, tenantId, serviceName, startTime, endTime, spanName, queueName,
                originatorId, messageType, messageId, messageData, messageMetadata, ruleChainId,
                ruleNodeId, pathTraceIds);
        query.setParameter("pageSize", pageable.getPageSize());
        query.setParameter("offset", pageable.getOffset());

        @SuppressWarnings("unchecked") List<TraceProjection> content = query.getResultList();
        return new PageImpl<>(content, pageable, totalElements);
    }

    private String normalizeJsonObject(String value) {
        return (value == null || value.isBlank()) ? "{}" : value;
    }

    private String normalizeJsonArray(String value) {
        return (value == null || value.isBlank()) ? "[]" : value;
    }

    private List<SpanAttributeEntity> toSpanAttributes(SpanEntity span) {
        Map<String, Object> attributes = parseJsonObject(span.getAttributesJsonb());
        if (attributes.isEmpty()) {
            return Collections.emptyList();
        }

        List<SpanAttributeEntity> result = new ArrayList<>(attributes.size());
        attributes.forEach((key, value) -> {
            SpanAttributeEntity attr = new SpanAttributeEntity();
            attr.setSpanId(span.getSpanId());
            attr.setTraceId(span.getTraceId());
            attr.setAttributeKey(key);
            attr.setCreatedTime(span.getCreatedTime());
            if (value instanceof Boolean boolValue) {
                attr.setAttributeValueType("BOOLEAN");
                attr.setAttributeValueBoolean(boolValue);
            } else if (value instanceof Number numberValue) {
                attr.setAttributeValueType("DOUBLE");
                attr.setAttributeValueNumber(numberValue.doubleValue());
            } else if (value != null) {
                attr.setAttributeValueType("STRING");
                if (value instanceof Map<?, ?> || value instanceof List<?>) {
                    attr.setAttributeValueString(JacksonUtil.toString(value));
                } else {
                    attr.setAttributeValueString(String.valueOf(value));
                }
            } else {
                attr.setAttributeValueType("STRING");
                attr.setAttributeValueString(null);
            }
            result.add(attr);
        });
        return result;
    }

    private List<SpanEventEntity> toSpanEvents(SpanEntity span) {
        List<Map<String, Object>> events = parseJsonArray(span.getEventsJsonb());
        if (events.isEmpty()) {
            return Collections.emptyList();
        }
        List<SpanEventEntity> result = new ArrayList<>(events.size());
        for (Map<String, Object> event : events) {
            SpanEventEntity entity = new SpanEventEntity();
            entity.setId(parseUuid(event.get("id")));
            if (entity.getId() == null) {
                entity.setId(UUID.randomUUID());
            }
            entity.setSpanId(span.getSpanId());
            entity.setTraceId(span.getTraceId());
            entity.setEventName(asString(firstNonNull(event.get("eventName"), event.get("event_name"))));
            entity.setEventTime(asLong(firstNonNull(event.get("eventTime"), event.get("event_time")), 0L));
            Object attributes = firstNonNull(event.get("attributes"), event.get("attributesJson"), event.get("attributes_json"));
            if (attributes != null) {
                entity.setAttributesJson(attributes instanceof String ? (String) attributes : JacksonUtil.toString(attributes));
            }
            entity.setCreatedTime(asLong(firstNonNull(event.get("createdTime"), event.get("created_time")), span.getCreatedTime()));
            result.add(entity);
        }
        return result;
    }

    private List<SpanLinkEntity> toSpanLinks(SpanEntity span) {
        List<Map<String, Object>> links = parseJsonArray(span.getLinksJsonb());
        if (links.isEmpty()) {
            return Collections.emptyList();
        }
        List<SpanLinkEntity> result = new ArrayList<>(links.size());
        for (Map<String, Object> link : links) {
            SpanLinkEntity entity = new SpanLinkEntity();
            entity.setId(parseUuid(link.get("id")));
            if (entity.getId() == null) {
                entity.setId(UUID.randomUUID());
            }
            entity.setSpanId(span.getSpanId());
            entity.setTraceId(span.getTraceId());
            entity.setLinkedTraceId(asString(firstNonNull(link.get("linkedTraceId"), link.get("linked_trace_id"))));
            entity.setLinkedSpanId(asString(firstNonNull(link.get("linkedSpanId"), link.get("linked_span_id"))));
            Object attributes = firstNonNull(link.get("attributes"), link.get("attributesJson"), link.get("attributes_json"));
            if (attributes != null) {
                entity.setAttributesJson(attributes instanceof String ? (String) attributes : JacksonUtil.toString(attributes));
            }
            entity.setCreatedTime(asLong(firstNonNull(link.get("createdTime"), link.get("created_time")), span.getCreatedTime()));
            result.add(entity);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJsonObject(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            Object value = JacksonUtil.OBJECT_MAPPER.readValue(json, Map.class);
            return value instanceof Map<?, ?> map ? (Map<String, Object>) map : Collections.emptyMap();
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseJsonArray(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            Object value = JacksonUtil.OBJECT_MAPPER.readValue(json, List.class);
            if (!(value instanceof List<?> list)) {
                return Collections.emptyList();
            }
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object entry : list) {
                if (entry instanceof Map<?, ?> map) {
                    result.add((Map<String, Object>) map);
                }
            }
            return result;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private UUID parseUuid(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return UUID.fromString(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }

    private static String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static long asLong(Object value, long defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private static Object firstNonNull(Object... values) {
        for (Object value : values) {
            if (value != null && !(value instanceof String str && str.isBlank())) {
                return value;
            }
        }
        return null;
    }

    private PGobject toJsonb(String value) throws SQLException {
        PGobject jsonb = new PGobject();
        jsonb.setType("jsonb");
        jsonb.setValue(value);
        return jsonb;
    }

    @Override
    @Transactional
    public boolean tryAcquireAggregationLeader(String serviceId, long nowMs, long leaseMs) {
        int updated = entityManager.createNativeQuery(
                        "UPDATE span_stats_leader" +
                                "   SET leader_id = :serviceId," +
                                "       leader_until = :until" +
                                " WHERE lock_name = 'aggregation'" +
                                "   AND (leader_id = :serviceId OR leader_until < :now)")
                .setParameter("serviceId", serviceId)
                .setParameter("now", nowMs)
                .setParameter("until", nowMs + leaseMs)
                .executeUpdate();
        return updated > 0;
    }

    @Override
    @Transactional
    public int upsertTraceSummaries(Collection<TraceSummaryInfo> summaries) {
        if (summaries == null || summaries.isEmpty()) {
            return 0;
        }
        int[] count = {0};
        entityManager.unwrap(Session.class).doWork(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO trace_summary (" +
                            "    trace_id, tenant_id, service_names_jsonb, start_time, end_time, duration," +
                            "    span_count, error_count, created_time, first_span_name," +
                            "    total_span_time, in_queue_time, root_queue_id, root_message_type, root_rule_chain_id," +
                            "    originator_ids_jsonb, queue_names_jsonb," +
                            "    rule_chain_ids_jsonb, rule_node_ids_jsonb, message_types_jsonb, message_ids_jsonb," +
                            "    has_errors, has_queue_timeouts, last_updated" +
                            ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, " +
                            "extract(epoch FROM now())::bigint * 1000)" +
                            " ON CONFLICT (tenant_id, trace_id) DO NOTHING")) {
                for (TraceSummaryInfo summary : summaries) {
                    bindTraceSummaryBatchParameters(ps, summary);
                    ps.addBatch();
                }
                for (int updateCount : ps.executeBatch()) {
                    if (updateCount > 0) {
                        count[0] += updateCount;
                    } else if (updateCount == Statement.SUCCESS_NO_INFO) {
                        count[0]++;
                    }
                }
            }
        });
        return count[0];
    }

    private void bindTraceSummaryBatchParameters(PreparedStatement statement, TraceSummaryInfo summary) throws SQLException {
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
}
