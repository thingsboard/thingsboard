// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.ruleenginemonitoring;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.thingsboard.server.dao.config.DefaultDataSource;
import org.thingsboard.server.dao.sqlts.insert.sql.SqlPartitioningRepository;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.ToLongFunction;

/**
 * Batch writes for the rule node / queue / queue lag stats tables. The tables are range-partitioned on bucket_time, so
 * the TTL drops whole partitions instead of deleting rows.
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

    private static final int BATCH_SIZE = 5000;

    private static final String RULE_NODE_STATS_TABLE = "rule_node_exec_stats";
    private static final String QUEUE_STATS_TABLE = "rule_node_queue_stats";
    private static final String QUEUE_LAG_STATS_TABLE = "rule_node_queue_lag_stats";
    private static final List<String> STATS_TABLES = List.of(RULE_NODE_STATS_TABLE, QUEUE_STATS_TABLE, QUEUE_LAG_STATS_TABLE);

    private final JdbcTemplate jdbcTemplate;
    private final SqlPartitioningRepository partitioningRepository;

    @Value("${sql.traces.rule_engine_stats_partition_size:24}")
    private int partitionSizeHours;

    public void saveRuleNodeStats(List<RuleNodeExecStatsRow> rows) {
        if (rows.isEmpty()) {
            return;
        }
        createPartitions(RULE_NODE_STATS_TABLE, rows, RuleNodeExecStatsRow::bucketTime);
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
        createPartitions(QUEUE_STATS_TABLE, rows, QueueStatsRow::bucketTime);
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
        createPartitions(QUEUE_LAG_STATS_TABLE, rows, QueueLagStatsRow::bucketTime);
        jdbcTemplate.batchUpdate(INSERT_QUEUE_LAG_STATS_SQL, rows, BATCH_SIZE, (ps, row) -> {
            ps.setLong(1, row.bucketTime());
            ps.setObject(2, row.queueTenantId());
            ps.setObject(3, row.queueId());
            ps.setLong(4, row.lag());
        });
    }

    private <T> void createPartitions(String table, List<T> rows, ToLongFunction<T> bucketTime) {
        long partitionSizeMs = getPartitionSizeMs();
        rows.stream()
                .mapToLong(bucketTime)
                .map(ts -> partitioningRepository.calculatePartitionStartTime(ts, partitionSizeMs))
                .distinct()
                .forEach(partitionStart -> partitioningRepository.createPartitionIfNotExists(table, partitionStart, partitionSizeMs));
    }

    /**
     * Drops the partitions that ended before {@code expTime}, so the TTL is accurate to the partition size.
     */
    public void cleanUp(long expTime) {
        if (!statsSchemaExists()) {
            return;
        }
        for (String table : STATS_TABLES) {
            partitioningRepository.dropPartitionsBefore(table, expTime, getPartitionSizeMs());
        }
    }

    // On the nodes that don't run the cleanup: forget the partitions the cleanup node will have dropped.
    public void cleanUpPartitionsCache(long expTime) {
        for (String table : STATS_TABLES) {
            partitioningRepository.cleanupPartitionsCache(table, expTime, getPartitionSizeMs());
        }
    }

    private long getPartitionSizeMs() {
        return TimeUnit.HOURS.toMillis(partitionSizeHours);
    }

    // Deletes by ctid in batches of at most batchSize rows, so no single statement deletes a whole tenant.
    private void deleteInBatches(String table, String condition, Object arg, int batchSize) {
        String sql = "DELETE FROM " + table + " WHERE ctid IN (SELECT ctid FROM " + table + " WHERE " + condition + " LIMIT ?)";
        int deleted;
        do {
            deleted = jdbcTemplate.update(sql, arg, batchSize);
        } while (deleted > 0);
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
        // One indexed condition per statement: an OR across tenant_id and queue_tenant_id can't use either index.
        deleteInBatches("rule_node_exec_stats", "tenant_id = ?", tenantId, BATCH_SIZE);
        deleteInBatches("rule_node_queue_stats", "tenant_id = ?", tenantId, BATCH_SIZE);
        deleteInBatches("rule_node_queue_stats", "queue_tenant_id = ?", tenantId, BATCH_SIZE);
        deleteInBatches("rule_node_queue_lag_stats", "queue_tenant_id = ?", tenantId, BATCH_SIZE);
    }

}
