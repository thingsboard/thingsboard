// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.ruleenginemonitoring;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.thingsboard.server.dao.config.DefaultDataSource;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;
import java.util.UUID;

/**
 * Batch writes for the rule node / queue / queue lag stats tables.
 */
@DefaultDataSource
@Repository
@SqlDao
@RequiredArgsConstructor
public class RuleEngineStatsInsertRepository {

    private static final String INSERT_RULE_NODE_STATS_SQL = """
            INSERT INTO rule_node_exec_stats
                (bucket_time, tenant_id, queue_id, rule_chain_id, rule_node_id, service_id,
                 exec_count, error_count, total_duration_ms, max_duration_ms)
            VALUES (?,?,?,?,?,?,?,?,?,?)
            """;

    private static final String INSERT_QUEUE_STATS_SQL = """
            INSERT INTO rule_node_queue_stats
                (bucket_time, queue_tenant_id, tenant_id, queue_id, rule_chain_id, last_visited_rule_node_id, service_id,
                 timeout_count, failure_count, success_count)
            VALUES (?,?,?,?,?,?,?,?,?,?)
            """;

    private static final String INSERT_QUEUE_LAG_STATS_SQL = """
            INSERT INTO rule_node_queue_lag_stats
                (bucket_time, queue_tenant_id, queue_id, lag)
            VALUES (?,?,?,?)
            """;

    private static final String QUEUE_LAG_LEADER_SQL = """
            UPDATE span_stats_leader
               SET leader_id = ?,
                   leader_until = ?
             WHERE lock_name = 'queue-lag'
               AND (leader_id = ? OR leader_until < ?)
            """;

    private static final int BATCH_SIZE = 5000;

    private static final List<String> STATS_TABLES = List.of("rule_node_exec_stats", "rule_node_queue_stats", "rule_node_queue_lag_stats");

    private final JdbcTemplate jdbcTemplate;

    public void saveRuleNodeStats(List<RuleNodeExecStatsRow> rows) {
        if (rows.isEmpty()) {
            return;
        }
        jdbcTemplate.batchUpdate(INSERT_RULE_NODE_STATS_SQL, rows, BATCH_SIZE, (ps, row) -> {
            ps.setLong(1, row.bucketTime());
            ps.setObject(2, row.tenantId());
            ps.setObject(3, row.queueId());
            ps.setObject(4, row.ruleChainId());
            ps.setObject(5, row.ruleNodeId());
            ps.setString(6, row.serviceId());
            ps.setLong(7, row.execCount());
            ps.setLong(8, row.errorCount());
            ps.setLong(9, row.totalDurationMs());
            ps.setLong(10, row.maxDurationMs());
        });
    }

    public void saveQueueStats(List<QueueStatsRow> rows) {
        if (rows.isEmpty()) {
            return;
        }
        jdbcTemplate.batchUpdate(INSERT_QUEUE_STATS_SQL, rows, BATCH_SIZE, (ps, row) -> {
            ps.setLong(1, row.bucketTime());
            ps.setObject(2, row.queueTenantId());
            ps.setObject(3, row.tenantId());
            ps.setObject(4, row.queueId());
            ps.setObject(5, row.ruleChainId());
            ps.setObject(6, row.lastVisitedRuleNodeId());
            ps.setString(7, row.serviceId());
            ps.setLong(8, row.timeoutCount());
            ps.setLong(9, row.failureCount());
            ps.setLong(10, row.successCount());
        });
    }

    public void saveQueueLagStats(List<QueueLagStatsRow> rows) {
        if (rows.isEmpty()) {
            return;
        }
        jdbcTemplate.batchUpdate(INSERT_QUEUE_LAG_STATS_SQL, rows, BATCH_SIZE, (ps, row) -> {
            ps.setLong(1, row.bucketTime());
            ps.setObject(2, row.queueTenantId());
            ps.setObject(3, row.queueId());
            ps.setLong(4, row.lag());
        });
    }

    public boolean tryAcquireQueueLagLeader(String serviceId, long nowMs, long leaseMs) {
        return jdbcTemplate.update(QUEUE_LAG_LEADER_SQL, serviceId, nowMs + leaseMs, serviceId, nowMs) > 0;
    }

    public void cleanUp(long expTime, int batchSize) {
        if (!statsSchemaExists()) {
            return;
        }
        for (String table : STATS_TABLES) {
            String sql = "DELETE FROM " + table + " WHERE ctid IN (SELECT ctid FROM " + table + " WHERE bucket_time < ? LIMIT ?)";
            int deleted;
            do {
                deleted = jdbcTemplate.update(sql, expTime, batchSize);
            } while (deleted > 0);
        }
    }

    // Install and upgrade create the trace schema; a database built another way (e.g. test suites) may lack it.
    private boolean statsSchemaExists() {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject("SELECT to_regclass('rule_node_exec_stats') IS NOT NULL", Boolean.class));
    }

    // Queue stats and lag also belong to the tenant through its own (isolated) queues.
    public void deleteByTenantId(UUID tenantId) {
        if (!statsSchemaExists()) {
            return;
        }
        jdbcTemplate.update("DELETE FROM rule_node_exec_stats WHERE tenant_id = ?", tenantId);
        jdbcTemplate.update("DELETE FROM rule_node_queue_stats WHERE tenant_id = ? OR queue_tenant_id = ?", tenantId, tenantId);
        jdbcTemplate.update("DELETE FROM rule_node_queue_lag_stats WHERE queue_tenant_id = ?", tenantId);
    }

}
