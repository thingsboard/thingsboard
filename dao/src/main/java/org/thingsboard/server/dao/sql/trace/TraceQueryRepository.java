// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.trace;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.trace.TracePathDTO;
import org.thingsboard.server.dao.config.DefaultDataSource;
import org.thingsboard.server.dao.model.sql.ruleenginemonitoring.TraceGroup;
import org.thingsboard.server.dao.model.sql.trace.TraceProjection;
import org.thingsboard.server.dao.util.SqlDao;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.thingsboard.server.common.data.trace.TraceSpanAttributes.INPUT_MSG_DATA;
import static org.thingsboard.server.common.data.trace.TraceSpanAttributes.INPUT_MSG_METADATA;

/**
 * Raw-SQL reads for the trace tables: dynamic filters, aggregations and jsonb predicates.
 */
@DefaultDataSource
@Repository
@SqlDao
@RequiredArgsConstructor
public class TraceQueryRepository {

    private static final String TRACE_GROUP_COLUMNS = """
            root_queue_id, root_message_type, root_rule_chain_id,
            visited_rule_nodes_jsonb, related_trace_ids, last_related_trace_added_time,
            last_observed, processed_traces, processed_traces_with_timeout, processed_traces_with_error,
            stored_traces, total_time, max_time, total_in_queue_time,
            total_rule_node_processing_time, rule_node_path_metrics_jsonb
            """;

    private static final String PATH_ERROR_SUM =
            "SUM((SELECT COALESCE(SUM((v->>'errors')::bigint), 0) " +
                    "FROM jsonb_each(rule_node_path_metrics_jsonb) AS e(k, v)))";

    private static final String TRACE_SUMMARY_COLUMNS =
            "ts.trace_id, COALESCE(ts.service_names_jsonb ->> 0, '') AS service_name, " +
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
                    "ts.has_errors, ts.has_queue_timeouts ";

    private static final int TRACE_GROUP_BATCH_SIZE = 1000;

    private static final RowMapper<TraceProjection> TRACE_PROJECTION_MAPPER = (rs, rowNum) -> new TraceProjection(
            rs.getString("trace_id"),
            rs.getString("service_name"),
            getLong(rs, "start_time"),
            getLong(rs, "end_time"),
            getLong(rs, "duration"),
            getInt(rs, "span_count"),
            getInt(rs, "error_count"),
            getLong(rs, "created_time"),
            rs.getString("first_span_name"),
            getLong(rs, "total_span_time"),
            getLong(rs, "in_queue_time"),
            rs.getString("root_queue_id"),
            rs.getString("message_type"),
            rs.getString("root_rule_chain_id"),
            getInt(rs, "rule_node_count"),
            getBoolean(rs, "has_errors"),
            getBoolean(rs, "has_queue_timeouts"));

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public Map<String, TraceGroup> findTraceGroups(long bucketTime, Collection<TraceGroup> groups) {
        if (CollectionUtils.isEmpty(groups)) {
            return Collections.emptyMap();
        }
        List<TraceGroup> groupList = new ArrayList<>(groups);
        Map<String, TraceGroup> result = new HashMap<>();
        String sql = "SELECT tenant_id, path_id, " + TRACE_GROUP_COLUMNS +
                "FROM trace_group WHERE bucket_time = :bucketTime AND (tenant_id, path_id) IN (:keys)";
        for (int start = 0; start < groupList.size(); start += TRACE_GROUP_BATCH_SIZE) {
            List<Object[]> keys = groupList.subList(start, Math.min(start + TRACE_GROUP_BATCH_SIZE, groupList.size())).stream()
                    .map(g -> new Object[]{g.getTenantId(), g.getPathId()})
                    .toList();
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("bucketTime", bucketTime)
                    .addValue("keys", keys);
            jdbcTemplate.query(sql, params, rs -> {
                String pathId = rs.getString("path_id");
                result.put(pathId, mapTraceGroup(rs, rs.getObject("tenant_id", UUID.class), bucketTime, pathId));
            });
        }
        return result;
    }

    public List<TraceGroup> findTraceGroupsByPathId(UUID tenantId, String pathId, long startTime, long endTime) {
        String sql = "SELECT bucket_time, " + TRACE_GROUP_COLUMNS +
                "FROM trace_group WHERE tenant_id = :tenantId AND path_id = :pathId " +
                "AND bucket_time >= :startTime AND bucket_time < :endTime ORDER BY bucket_time";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("tenantId", tenantId)
                .addValue("pathId", pathId)
                .addValue("startTime", startTime)
                .addValue("endTime", endTime);
        return jdbcTemplate.query(sql, params, (rs, rowNum) -> mapTraceGroup(rs, tenantId, rs.getLong("bucket_time"), pathId));
    }

    public long countTraceGroups(long bucketTime, UUID tenantId) {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM trace_group WHERE bucket_time = :bucketTime AND tenant_id = :tenantId",
                new MapSqlParameterSource().addValue("bucketTime", bucketTime).addValue("tenantId", tenantId), Long.class);
        return count != null ? count : 0L;
    }

    public PageData<TracePathDTO> findPathsPaginated(UUID tenantId, long startTime, long endTime,
                                                     String rootQueueId, String rootMessageType, String rootRuleChainId,
                                                     String ruleNodeId, boolean errorsOnly,
                                                     int page, int pageSize, String sortField, String sortOrder) {
        int safePageSize = Math.max(1, pageSize);
        int safePage = Math.max(0, page);

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("tenantId", tenantId)
                .addValue("startTime", startTime)
                .addValue("endTime", endTime);
        StringBuilder where = new StringBuilder("WHERE tenant_id = :tenantId AND bucket_time >= :startTime AND bucket_time < :endTime");
        if (rootQueueId != null) {
            where.append(" AND root_queue_id = :rootQueueId");
            params.addValue("rootQueueId", UUID.fromString(rootQueueId));
        }
        if (rootMessageType != null) {
            where.append(" AND root_message_type = :rootMessageType");
            params.addValue("rootMessageType", rootMessageType);
        }
        if (rootRuleChainId != null) {
            where.append(" AND root_rule_chain_id = :rootRuleChainId");
            params.addValue("rootRuleChainId", UUID.fromString(rootRuleChainId));
        }
        if (ruleNodeId != null) {
            where.append(" AND visited_rule_nodes_jsonb @> CAST(:visitedRuleNode AS jsonb)");
            params.addValue("visitedRuleNode", "[{\"ruleNodeId\":\"" + ruleNodeId + "\"}]");
        }

        String groupBy = " GROUP BY path_id, root_queue_id, root_message_type, root_rule_chain_id";
        String having = errorsOnly ? " HAVING " + PATH_ERROR_SUM + " > 0" : "";

        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM (SELECT path_id FROM trace_group " + where + groupBy + having + ") sub", params, Long.class);
        long totalElements = total != null ? total : 0L;

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
                ", path_id ASC LIMIT :limit OFFSET :offset";
        params.addValue("limit", safePageSize).addValue("offset", (long) safePage * safePageSize);
        List<TracePathDTO> data = jdbcTemplate.query(dataSql, params, (rs, rowNum) -> mapTracePath(rs));

        int totalPages = (int) ((totalElements + safePageSize - 1) / safePageSize);
        boolean hasNext = (long) (safePage + 1) * safePageSize < totalElements;
        return new PageData<>(data, totalPages, totalElements, hasNext);
    }

    public TraceProjection findTraceById(UUID tenantId, String traceId) {
        List<TraceProjection> result = jdbcTemplate.query(
                "SELECT " + TRACE_SUMMARY_COLUMNS + "FROM trace_summary ts WHERE ts.tenant_id = :tenantId AND ts.trace_id = :traceId",
                new MapSqlParameterSource().addValue("tenantId", tenantId).addValue("traceId", traceId),
                TRACE_PROJECTION_MAPPER);
        return result.isEmpty() ? null : result.get(0);
    }

    public Page<TraceProjection> findTraces(UUID tenantId, String serviceName, long startTime, long endTime, String spanName,
                                            String queueName, String originatorId, String messageType, String messageId,
                                            String messageData, String messageMetadata, boolean errorsOnly,
                                            String ruleChainId, String ruleNodeId, Collection<String> pathTraceIds,
                                            boolean hasTimeOutsByQueue, String sortField, String sortOrder, Pageable pageable) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        String filters = buildTraceSummaryFilters(params, tenantId, serviceName, startTime, endTime, spanName, queueName,
                originatorId, messageType, messageId, messageData, messageMetadata, errorsOnly,
                ruleChainId, ruleNodeId, pathTraceIds, hasTimeOutsByQueue);

        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) " + filters, params, Long.class);
        long totalElements = total != null ? total : 0L;
        if (totalElements == 0L) {
            return new PageImpl<>(Collections.emptyList(), pageable, 0L);
        }

        String dataSql = "SELECT " + TRACE_SUMMARY_COLUMNS + filters +
                getOrderByClause(sortField, "ASC".equals(sortOrder)) +
                "LIMIT :pageSize OFFSET :offset";
        params.addValue("pageSize", pageable.getPageSize()).addValue("offset", pageable.getOffset());
        List<TraceProjection> content = jdbcTemplate.query(dataSql, params, TRACE_PROJECTION_MAPPER);
        return new PageImpl<>(content, pageable, totalElements);
    }

    private static String buildTraceSummaryFilters(MapSqlParameterSource params, UUID tenantId, String serviceName,
                                                   long startTime, long endTime, String spanName, String queueName,
                                                   String originatorId, String messageType, String messageId,
                                                   String messageData, String messageMetadata, boolean errorsOnly,
                                                   String ruleChainId, String ruleNodeId, Collection<String> pathTraceIds,
                                                   boolean hasTimeOutsByQueue) {
        StringBuilder sql = new StringBuilder();
        sql.append("FROM trace_summary ts ");
        sql.append("WHERE ts.tenant_id = :tenantId ");
        sql.append("AND ts.start_time >= :startTime AND ts.start_time <= :endTime ");
        params.addValue("tenantId", tenantId).addValue("startTime", startTime).addValue("endTime", endTime);

        if (pathTraceIds != null && !pathTraceIds.isEmpty()) {
            sql.append("AND ts.trace_id IN (:pathTraceIds) ");
            params.addValue("pathTraceIds", pathTraceIds);
        }
        appendJsonbMembership(sql, params, "service_names_jsonb", "serviceName", serviceName);
        if (errorsOnly) {
            sql.append("AND ts.has_errors = true ");
        }
        appendJsonbMembership(sql, params, "rule_chain_ids_jsonb", "ruleChainId", ruleChainId);
        appendJsonbMembership(sql, params, "originator_ids_jsonb", "originatorId", originatorId);
        appendJsonbMembership(sql, params, "message_types_jsonb", "messageType", messageType);
        appendJsonbMembership(sql, params, "message_ids_jsonb", "messageId", messageId);
        if (hasTimeOutsByQueue) {
            sql.append("AND ts.has_queue_timeouts = true ");
        }
        appendJsonbMembership(sql, params, "queue_names_jsonb", "queueName", queueName);
        appendJsonbMembership(sql, params, "rule_node_ids_jsonb", "filterRuleNodeId", ruleNodeId);
        if (isNotEmpty(spanName)) {
            sql.append("AND EXISTS (SELECT 1 FROM span sp WHERE sp.trace_id = ts.trace_id AND sp.tenant_id = ts.tenant_id AND sp.name = :spanName) ");
            params.addValue("spanName", spanName);
        }
        appendSpanAttributeLike(sql, params, INPUT_MSG_DATA, "messageData", messageData);
        appendSpanAttributeLike(sql, params, INPUT_MSG_METADATA, "messageMetadata", messageMetadata);
        return sql.toString();
    }

    private static void appendJsonbMembership(StringBuilder sql, MapSqlParameterSource params, String column, String paramName, String value) {
        if (isNotEmpty(value)) {
            sql.append("AND jsonb_exists(ts.").append(column).append(", :").append(paramName).append(") ");
            params.addValue(paramName, value);
        }
    }

    private static void appendSpanAttributeLike(StringBuilder sql, MapSqlParameterSource params, String attributeKey, String paramName, String value) {
        if (isNotEmpty(value)) {
            sql.append("AND EXISTS (SELECT 1 FROM span s WHERE s.trace_id = ts.trace_id AND s.tenant_id = ts.tenant_id ")
                    .append("AND COALESCE(s.attributes_jsonb ->> '").append(attributeKey).append("', '') LIKE :").append(paramName)
                    .append(" ESCAPE '\\') ");
            params.addValue(paramName, "%" + value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%");
        }
    }

    private static String getOrderByClause(String sortField, boolean isAsc) {
        String direction = isAsc ? "ASC" : "DESC";
        return switch (sortField) {
            case "FIRST_SPAN_NAME" ->
                    "ORDER BY (SELECT sp.name FROM span sp WHERE sp.trace_id = ts.trace_id AND sp.tenant_id = ts.tenant_id ORDER BY sp.start_time ASC LIMIT 1) " + direction + ", ts.trace_id ASC ";
            case "DURATION" -> "ORDER BY ts.duration " + direction + ", ts.trace_id ASC ";
            case "END_TIME" -> "ORDER BY ts.end_time " + direction + ", ts.trace_id ASC ";
            case "SPAN_COUNT" -> "ORDER BY ts.span_count " + direction + ", ts.trace_id ASC ";
            case "ERROR_COUNT" -> "ORDER BY ts.error_count " + direction + ", ts.trace_id ASC ";
            case "RULE_NODE_COUNT" -> "ORDER BY COALESCE(jsonb_array_length(ts.rule_node_ids_jsonb), 0) " + direction + ", ts.trace_id ASC ";
            case "CREATED_TIME" -> "ORDER BY ts.created_time " + direction + ", ts.trace_id ASC ";
            case "TOTAL_SPAN_TIME" -> "ORDER BY ts.total_span_time " + direction + " NULLS LAST, ts.trace_id ASC ";
            case "IN_QUEUE_TIME" -> "ORDER BY ts.in_queue_time " + direction + " NULLS LAST, ts.trace_id ASC ";
            default -> "ORDER BY ts.start_time " + direction + ", ts.trace_id ASC ";
        };
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

    private static TracePathDTO mapTracePath(ResultSet rs) throws SQLException {
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

    private static TraceGroup mapTraceGroup(ResultSet rs, UUID tenantId, long bucketTime, String pathId) throws SQLException {
        TraceGroup group = new TraceGroup();
        group.setBucketTime(bucketTime);
        group.setTenantId(tenantId);
        group.setPathId(pathId);
        group.setRootQueueId(rs.getObject("root_queue_id", UUID.class));
        group.setRootMessageType(rs.getString("root_message_type"));
        group.setRootRuleChainId(rs.getObject("root_rule_chain_id", UUID.class));
        group.setVisitedRuleNodes(JacksonUtil.fromString(rs.getString("visited_rule_nodes_jsonb"), new TypeReference<>() {}));
        Array relatedTraceIds = rs.getArray("related_trace_ids");
        group.setRelatedTraceIds(relatedTraceIds != null
                ? new ArrayList<>(Arrays.asList((String[]) relatedTraceIds.getArray()))
                : new ArrayList<>());
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
        group.setRuleNodePathMetrics(JacksonUtil.fromString(rs.getString("rule_node_path_metrics_jsonb"), new TypeReference<>() {}));
        return group;
    }

    private static Long getLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private static Integer getInt(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    private static Boolean getBoolean(ResultSet rs, String column) throws SQLException {
        boolean value = rs.getBoolean(column);
        return rs.wasNull() ? null : value;
    }

    private static boolean isNotEmpty(String value) {
        return value != null && !value.isEmpty();
    }

}
