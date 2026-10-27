// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.ruleenginemonitoring;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.thingsboard.server.dao.config.DefaultDataSource;
import org.thingsboard.server.common.data.ruleenginemonitoring.QueueLagTableEntry;
import org.thingsboard.server.common.data.ruleenginemonitoring.QueueLagTimeseriesEntry;
import org.thingsboard.server.common.data.ruleenginemonitoring.QueueTableEntry;
import org.thingsboard.server.common.data.ruleenginemonitoring.QueueTimeseriesEntry;
import org.thingsboard.server.common.data.ruleenginemonitoring.RuleNodeTableRow;
import org.thingsboard.server.common.data.ruleenginemonitoring.RuleNodeTsKvEntry;
import org.thingsboard.server.dao.util.SqlDao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Aggregating reads over the rule node / queue / queue lag stats tables.
 */
@DefaultDataSource
@Repository
@SqlDao
@RequiredArgsConstructor
public class RuleEngineStatsQueryRepository {

    private static final RowMapper<RuleNodeTsKvEntry> RULE_NODE_TS_MAPPER = (rs, rowNum) -> new RuleNodeTsKvEntry(
            getLong(rs, "bucket_time"), getLong(rs, "exec_count"), getLong(rs, "error_count"),
            getLong(rs, "total_duration_ms"), getLong(rs, "avg_duration_ms"), getLong(rs, "max_duration_ms"),
            getUuid(rs, "tenant_id"), rs.getString("service_id"),
            getUuid(rs, "queue_id"), getUuid(rs, "rule_chain_id"), getUuid(rs, "rule_node_id"));

    private static final RowMapper<RuleNodeTableRow> RULE_NODE_TABLE_MAPPER = (rs, rowNum) -> new RuleNodeTableRow(
            getLong(rs, "exec_count"), getLong(rs, "error_count"), getLong(rs, "total_duration_ms"),
            getLong(rs, "avg_duration_ms"), getLong(rs, "max_duration_ms"),
            getUuid(rs, "tenant_id"), rs.getString("service_id"), getUuid(rs, "queue_id"),
            getUuid(rs, "rule_chain_id"), getUuid(rs, "rule_node_id"));

    private static final RowMapper<QueueTimeseriesEntry> QUEUE_TS_MAPPER = (rs, rowNum) -> new QueueTimeseriesEntry(
            getLong(rs, "bucket_time"), getLong(rs, "timeout_count"), getLong(rs, "failure_count"),
            getLong(rs, "success_count"), getUuid(rs, "tenant_id"), rs.getString("service_id"),
            getUuid(rs, "queue_id"), getUuid(rs, "rule_chain_id"), getUuid(rs, "rule_node_id"),
            getUuid(rs, "queue_tenant_id"));

    private static final RowMapper<QueueTableEntry> QUEUE_TABLE_MAPPER = (rs, rowNum) -> new QueueTableEntry(
            getLong(rs, "timeout_count"), getLong(rs, "failure_count"), getLong(rs, "success_count"),
            getUuid(rs, "tenant_id"), rs.getString("service_id"), getUuid(rs, "queue_id"),
            getUuid(rs, "rule_chain_id"), getUuid(rs, "rule_node_id"), getUuid(rs, "queue_tenant_id"));

    private static final RowMapper<QueueLagTimeseriesEntry> QUEUE_LAG_TS_MAPPER = (rs, rowNum) -> new QueueLagTimeseriesEntry(
            getLong(rs, "bucket_time"), getLong(rs, "lag"), getUuid(rs, "queue_tenant_id"), getUuid(rs, "queue_id"));

    private static final RowMapper<QueueLagTableEntry> QUEUE_LAG_TABLE_MAPPER = (rs, rowNum) -> new QueueLagTableEntry(
            getLong(rs, "lag"), getUuid(rs, "queue_tenant_id"), getUuid(rs, "queue_id"));

    private static final Set<RuleNodeDimension> RULE_NODE_STATS_DIMENSIONS = EnumSet.of(RuleNodeDimension.TENANT_ID,
            RuleNodeDimension.SERVICE_ID, RuleNodeDimension.QUEUE_ID, RuleNodeDimension.RULE_CHAIN_ID, RuleNodeDimension.RULE_NODE_ID);
    private static final Set<RuleNodeDimension> QUEUE_STATS_DIMENSIONS = EnumSet.of(RuleNodeDimension.QUEUE_TENANT_ID,
            RuleNodeDimension.TENANT_ID, RuleNodeDimension.SERVICE_ID, RuleNodeDimension.QUEUE_ID, RuleNodeDimension.RULE_CHAIN_ID,
            RuleNodeDimension.LAST_VISITED_RULE_NODE_ID);
    private static final Set<RuleNodeDimension> QUEUE_LAG_STATS_DIMENSIONS = EnumSet.of(RuleNodeDimension.QUEUE_TENANT_ID,
            RuleNodeDimension.QUEUE_ID);

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public List<RuleNodeTsKvEntry> findRuleNodeTimeseries(RuleNodeTimeseriesQuery query) {
        Set<RuleNodeDimension> groupBy = query.groupBy();
        requireDimensions(groupBy, RULE_NODE_STATS_DIMENSIONS);
        String bucketExpr = bucketExpression(query.intervalMs());

        StringBuilder sql = new StringBuilder("""
                SELECT %s AS bucket_time,
                       SUM(exec_count)                                      AS exec_count,
                       SUM(error_count)                                     AS error_count,
                       SUM(total_duration_ms)                               AS total_duration_ms,
                       SUM(total_duration_ms) / NULLIF(SUM(exec_count), 0)  AS avg_duration_ms,
                       MAX(max_duration_ms)                                 AS max_duration_ms,
                       %s                                                   AS tenant_id,
                       %s                                                   AS service_id,
                       %s                                                   AS queue_id,
                       %s                                                   AS rule_chain_id,
                       %s                                                   AS rule_node_id
                FROM rule_node_exec_stats
                WHERE bucket_time >= :startTs
                  AND bucket_time < :endTs
                """.formatted(
                bucketExpr,
                groupBy.contains(RuleNodeDimension.TENANT_ID) ? "tenant_id" : "CAST(NULL AS uuid)",
                groupBy.contains(RuleNodeDimension.SERVICE_ID) ? "service_id" : "CAST(NULL AS varchar)",
                groupBy.contains(RuleNodeDimension.QUEUE_ID) ? "queue_id" : "CAST(NULL AS uuid)",
                groupBy.contains(RuleNodeDimension.RULE_CHAIN_ID) ? "rule_chain_id" : "CAST(NULL AS uuid)",
                groupBy.contains(RuleNodeDimension.RULE_NODE_ID) ? "rule_node_id" : "CAST(NULL AS uuid)"
        ));
        MapSqlParameterSource params = timeRangeParams(query.startTs(), query.endTs());
        appendStatsFilters(sql, params, "rule_node_id", query.tenantId(),
                query.queueIds(), query.ruleChainIds(), query.ruleNodeIds(), query.serviceIds());
        appendTimeseriesGroupBy(sql, bucketExpr, groupBy);
        return jdbcTemplate.query(sql.toString(), params, RULE_NODE_TS_MAPPER);
    }

    public List<RuleNodeTableRow> findRuleNodeTable(RuleNodeTableQuery query) {
        Set<RuleNodeDimension> groupBy = query.groupBy();
        requireDimensions(groupBy, RULE_NODE_STATS_DIMENSIONS);

        StringBuilder sql = new StringBuilder("""
                SELECT SUM(exec_count)                                      AS exec_count,
                       SUM(error_count)                                     AS error_count,
                       SUM(total_duration_ms)                               AS total_duration_ms,
                       SUM(total_duration_ms) / NULLIF(SUM(exec_count), 0)  AS avg_duration_ms,
                       MAX(max_duration_ms)                                 AS max_duration_ms,
                       %s                                                   AS tenant_id,
                       %s                                                   AS service_id,
                       %s                                                   AS queue_id,
                       %s                                                   AS rule_chain_id,
                       %s                                                   AS rule_node_id
                FROM rule_node_exec_stats
                WHERE bucket_time >= :startTs
                  AND bucket_time < :endTs
                """.formatted(
                groupBy.contains(RuleNodeDimension.TENANT_ID) ? "tenant_id" : "CAST(NULL AS uuid)",
                groupBy.contains(RuleNodeDimension.SERVICE_ID) ? "service_id" : "CAST(NULL AS varchar)",
                groupBy.contains(RuleNodeDimension.QUEUE_ID) ? "queue_id" : "CAST(NULL AS uuid)",
                groupBy.contains(RuleNodeDimension.RULE_CHAIN_ID) ? "rule_chain_id" : "CAST(NULL AS uuid)",
                groupBy.contains(RuleNodeDimension.RULE_NODE_ID) ? "rule_node_id" : "CAST(NULL AS uuid)"
        ));
        MapSqlParameterSource params = timeRangeParams(query.startTs(), query.endTs());
        appendStatsFilters(sql, params, "rule_node_id", query.tenantId(),
                query.queueIds(), query.ruleChainIds(), query.ruleNodeIds(), query.serviceIds());
        appendTableGroupBy(sql, groupBy);
        return jdbcTemplate.query(sql.toString(), params, RULE_NODE_TABLE_MAPPER);
    }

    public List<QueueTimeseriesEntry> findQueueTimeseries(QueueTimeseriesQuery query) {
        Set<RuleNodeDimension> groupBy = query.groupBy();
        requireDimensions(groupBy, QUEUE_STATS_DIMENSIONS);
        String bucketExpr = bucketExpression(query.intervalMs());

        StringBuilder sql = new StringBuilder("""
                SELECT %s AS bucket_time,
                       SUM(timeout_count)  AS timeout_count,
                       SUM(failure_count)  AS failure_count,
                       SUM(success_count)  AS success_count,
                       %s                  AS tenant_id,
                       %s                  AS service_id,
                       %s                  AS queue_id,
                       %s                  AS rule_chain_id,
                       %s                  AS rule_node_id,
                       %s                  AS queue_tenant_id
                FROM rule_node_queue_stats
                WHERE bucket_time >= :startTs
                  AND bucket_time < :endTs
                """.formatted(
                bucketExpr,
                groupBy.contains(RuleNodeDimension.TENANT_ID) ? "tenant_id" : "CAST(NULL AS uuid)",
                groupBy.contains(RuleNodeDimension.SERVICE_ID) ? "service_id" : "CAST(NULL AS varchar)",
                groupBy.contains(RuleNodeDimension.QUEUE_ID) ? "queue_id" : "CAST(NULL AS uuid)",
                groupBy.contains(RuleNodeDimension.RULE_CHAIN_ID) ? "rule_chain_id" : "CAST(NULL AS uuid)",
                groupBy.contains(RuleNodeDimension.LAST_VISITED_RULE_NODE_ID) ? "last_visited_rule_node_id" : "CAST(NULL AS uuid)",
                groupBy.contains(RuleNodeDimension.QUEUE_TENANT_ID) ? "queue_tenant_id" : "CAST(NULL AS uuid)"
        ));
        MapSqlParameterSource params = timeRangeParams(query.startTs(), query.endTs());
        appendStatsFilters(sql, params, "last_visited_rule_node_id", query.tenantId(),
                query.queueIds(), query.ruleChainIds(), query.ruleNodeIds(), query.serviceIds());
        appendTimeseriesGroupBy(sql, bucketExpr, groupBy);
        return jdbcTemplate.query(sql.toString(), params, QUEUE_TS_MAPPER);
    }

    public List<QueueTableEntry> findQueueTable(QueueTableQuery query) {
        Set<RuleNodeDimension> groupBy = query.groupBy();
        requireDimensions(groupBy, QUEUE_STATS_DIMENSIONS);

        StringBuilder sql = new StringBuilder("""
                SELECT SUM(timeout_count)  AS timeout_count,
                       SUM(failure_count)  AS failure_count,
                       SUM(success_count)  AS success_count,
                       %s                  AS tenant_id,
                       %s                  AS service_id,
                       %s                  AS queue_id,
                       %s                  AS rule_chain_id,
                       %s                  AS rule_node_id,
                       %s                  AS queue_tenant_id
                FROM rule_node_queue_stats
                WHERE bucket_time >= :startTs
                  AND bucket_time < :endTs
                """.formatted(
                groupBy.contains(RuleNodeDimension.TENANT_ID) ? "tenant_id" : "CAST(NULL AS uuid)",
                groupBy.contains(RuleNodeDimension.SERVICE_ID) ? "service_id" : "CAST(NULL AS varchar)",
                groupBy.contains(RuleNodeDimension.QUEUE_ID) ? "queue_id" : "CAST(NULL AS uuid)",
                groupBy.contains(RuleNodeDimension.RULE_CHAIN_ID) ? "rule_chain_id" : "CAST(NULL AS uuid)",
                groupBy.contains(RuleNodeDimension.LAST_VISITED_RULE_NODE_ID) ? "last_visited_rule_node_id" : "CAST(NULL AS uuid)",
                groupBy.contains(RuleNodeDimension.QUEUE_TENANT_ID) ? "queue_tenant_id" : "CAST(NULL AS uuid)"
        ));
        MapSqlParameterSource params = timeRangeParams(query.startTs(), query.endTs());
        appendStatsFilters(sql, params, "last_visited_rule_node_id", query.tenantId(),
                query.queueIds(), query.ruleChainIds(), query.ruleNodeIds(), query.serviceIds());
        appendTableGroupBy(sql, groupBy);
        return jdbcTemplate.query(sql.toString(), params, QUEUE_TABLE_MAPPER);
    }

    public List<QueueLagTimeseriesEntry> findQueueLagTimeseries(QueueLagTimeseriesQuery query) {
        Set<RuleNodeDimension> groupBy = query.groupBy();
        requireDimensions(groupBy, QUEUE_LAG_STATS_DIMENSIONS);
        String bucketExpr = bucketExpression(query.intervalMs());

        StringBuilder sql = new StringBuilder("""
                SELECT %s AS bucket_time,
                       MAX(lag)  AS lag,
                       %s        AS queue_tenant_id,
                       %s        AS queue_id
                FROM rule_node_queue_lag_stats
                WHERE bucket_time >= :startTs
                  AND bucket_time < :endTs
                  AND queue_tenant_id = :tenantId
                """.formatted(
                bucketExpr,
                groupBy.contains(RuleNodeDimension.QUEUE_TENANT_ID) ? "queue_tenant_id" : "CAST(NULL AS uuid)",
                groupBy.contains(RuleNodeDimension.QUEUE_ID) ? "queue_id" : "CAST(NULL AS uuid)"
        ));
        MapSqlParameterSource params = timeRangeParams(query.startTs(), query.endTs()).addValue("tenantId", query.tenantId());
        appendIn(sql, params, "queue_id", "queueIds", query.queueIds());
        appendTimeseriesGroupBy(sql, bucketExpr, groupBy);
        return jdbcTemplate.query(sql.toString(), params, QUEUE_LAG_TS_MAPPER);
    }

    public List<QueueLagTableEntry> findQueueLagTable(QueueLagTableQuery query) {
        Set<RuleNodeDimension> groupBy = query.groupBy();
        requireDimensions(groupBy, QUEUE_LAG_STATS_DIMENSIONS);

        StringBuilder sql = new StringBuilder("""
                SELECT SUM(lag)  AS lag,
                       %s        AS queue_tenant_id,
                       %s        AS queue_id
                FROM rule_node_queue_lag_stats
                WHERE bucket_time >= :startTs
                  AND bucket_time < :endTs
                  AND queue_tenant_id = :tenantId
                """.formatted(
                groupBy.contains(RuleNodeDimension.QUEUE_TENANT_ID) ? "queue_tenant_id" : "CAST(NULL AS uuid)",
                groupBy.contains(RuleNodeDimension.QUEUE_ID) ? "queue_id" : "CAST(NULL AS uuid)"
        ));
        MapSqlParameterSource params = timeRangeParams(query.startTs(), query.endTs()).addValue("tenantId", query.tenantId());
        appendIn(sql, params, "queue_id", "queueIds", query.queueIds());
        appendTableGroupBy(sql, groupBy);
        return jdbcTemplate.query(sql.toString(), params, QUEUE_LAG_TABLE_MAPPER);
    }

    public long findLastQueueLag(UUID tenantId, List<UUID> queueIds, long staleAfterMs) {
        StringBuilder sql = new StringBuilder("""
                SELECT COALESCE(SUM(latest.lag), 0)
                FROM (
                    SELECT DISTINCT ON (queue_tenant_id, queue_id) lag
                    FROM rule_node_queue_lag_stats
                    WHERE queue_tenant_id = :tenantId
                      AND bucket_time >= :startTs
                """);
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("tenantId", tenantId)
                .addValue("startTs", System.currentTimeMillis() - staleAfterMs);
        appendIn(sql, params, "queue_id", "queueIds", queueIds);
        sql.append("""
                    ORDER BY queue_tenant_id, queue_id, bucket_time DESC
                ) latest
                """);
        Long lag = jdbcTemplate.queryForObject(sql.toString(), params, Long.class);
        return lag == null ? 0L : lag;
    }

    private static String bucketExpression(Long intervalMs) {
        return intervalMs != null ? "(bucket_time / " + intervalMs + ") * " + intervalMs : "bucket_time";
    }

    private static MapSqlParameterSource timeRangeParams(long startTs, long endTs) {
        return new MapSqlParameterSource().addValue("startTs", startTs).addValue("endTs", endTs);
    }

    private static void appendStatsFilters(StringBuilder sql, MapSqlParameterSource params, String ruleNodeColumn, UUID tenantId,
                                           List<UUID> queueIds, List<UUID> ruleChainIds, List<UUID> ruleNodeIds,
                                           List<String> serviceIds) {
        sql.append("  AND tenant_id = :tenantId\n");
        if (tenantId != null) {
            params.addValue("tenantId", tenantId);
        }
        appendIn(sql, params, "queue_id", "queueIds", queueIds);
        appendIn(sql, params, "rule_chain_id", "ruleChainIds", ruleChainIds);
        appendIn(sql, params, ruleNodeColumn, "ruleNodeIds", ruleNodeIds);
        appendIn(sql, params, "service_id", "serviceIds", serviceIds);
    }

    private static void appendIn(StringBuilder sql, MapSqlParameterSource params, String column, String paramName, List<?> values) {
        if (values != null && !values.isEmpty()) {
            sql.append("  AND ").append(column).append(" IN (:").append(paramName).append(")\n");
            params.addValue(paramName, values);
        }
    }

    // The dimensions become GROUP BY columns, so each table accepts only the ones it has.
    private static void requireDimensions(Set<RuleNodeDimension> groupBy, Set<RuleNodeDimension> allowed) {
        for (RuleNodeDimension dimension : groupBy) {
            if (!allowed.contains(dimension)) {
                throw new IllegalArgumentException("Unsupported group by dimension " + dimension + ", expected one of " + allowed);
            }
        }
    }

    private static void appendTimeseriesGroupBy(StringBuilder sql, String bucketExpr, Set<RuleNodeDimension> groupBy) {
        List<String> groupByCols = new ArrayList<>();
        groupByCols.add(bucketExpr);
        groupBy.forEach(dim -> groupByCols.add(dim.column));
        sql.append("GROUP BY ").append(String.join(", ", groupByCols)).append("\nORDER BY bucket_time ASC");
    }

    private static void appendTableGroupBy(StringBuilder sql, Set<RuleNodeDimension> groupBy) {
        if (!groupBy.isEmpty()) {
            sql.append("GROUP BY ").append(String.join(", ", groupBy.stream().map(d -> d.column).toList())).append("\n");
        }
    }

    private static Long getLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private static UUID getUuid(ResultSet rs, String column) throws SQLException {
        return rs.getObject(column, UUID.class);
    }

}
