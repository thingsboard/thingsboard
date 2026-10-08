/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2026 ThingsBoard, Inc. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of ThingsBoard, Inc. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to ThingsBoard, Inc.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 *
 * Dissemination of this information or reproduction of this material is strictly forbidden
 * unless prior written permission is obtained from COMPANY.
 *
 * Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
 * managers or contractors who have executed Confidentiality and Non-disclosure agreements
 * explicitly covering such access.
 *
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
package org.thingsboard.server.dao.sql.ruleenginemonitoring;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.config.DefaultDataSource;
import org.thingsboard.server.dao.model.sql.ruleenginemonitoring.QueueLagTableEntry;
import org.thingsboard.server.dao.model.sql.ruleenginemonitoring.QueueLagTimeseriesEntry;
import org.thingsboard.server.dao.model.sql.ruleenginemonitoring.QueueTableEntry;
import org.thingsboard.server.dao.model.sql.ruleenginemonitoring.QueueTimeseriesEntry;
import org.thingsboard.server.dao.model.sql.ruleenginemonitoring.RuleNodeTableRow;
import org.thingsboard.server.dao.model.sql.ruleenginemonitoring.RuleNodeTsKvEntry;
import org.thingsboard.server.dao.util.SqlDao;

import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@DefaultDataSource
@Component
@SqlDao
public class JpaRuleEngineMonitoringDao implements RuleEngineMonitoringDao {
    private static final String INSERT_QUEUE_STATS_SQL = """
            INSERT INTO rule_node_queue_stats
                (bucket_time, queue_tenant_id, tenant_id, queue_id, rule_chain_id, last_visited_rule_node_id, service_id,
                 timeout_count, failure_count, success_count)
            VALUES (?,?,?,?,?,?,?,?,?,?)
            """;

    private static final String INSERT_SQL = """
            INSERT INTO rule_node_exec_stats
                (bucket_time, tenant_id, queue_id, rule_chain_id, rule_node_id, service_id,
                 exec_count, error_count, total_duration_ms, max_duration_ms, p95_duration_ms)
            VALUES (?,?,?,?,?,?,?,?,?,?,?)
            """;

    private static final String INSERT_QUEUE_LAG_STATS_SQL = """
            INSERT INTO rule_node_queue_lag_stats
                (bucket_time, queue_tenant_id, queue_id, lag)
            VALUES (?,?,?,?)
            """;

    private static final int BATCH_SIZE = 5000;

    protected EntityManager entityManager;

    @PersistenceContext
    public void setEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    private final JdbcTemplate jdbcTemplate;
    private final JdbcTemplate mainJdbcTemplate;

    @Autowired
    public JpaRuleEngineMonitoringDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.mainJdbcTemplate = jdbcTemplate;
    }

    public JpaRuleEngineMonitoringDao(JdbcTemplate jdbcTemplate, JdbcTemplate mainJdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.mainJdbcTemplate = mainJdbcTemplate;
    }

    @Override
    public List<RuleNodeTsKvEntry> findRuleNodeTimeseries(RuleNodeTimeseriesQuery query) {
        Set<RuleNodeDimension> groupBy = query.groupBy();
        String bucketExpr = query.intervalMs() != null
                ? "(bucket_time / " + query.intervalMs() + ") * " + query.intervalMs()
                : "bucket_time";

        StringBuilder sql = new StringBuilder("""
                SELECT %s AS bucket_time,
                       SUM(exec_count)                                      AS exec_count,
                       SUM(error_count)                                     AS error_count,
                       SUM(total_duration_ms)                               AS total_duration_ms,
                       SUM(total_duration_ms) / NULLIF(SUM(exec_count), 0)  AS avg_duration_ms,
                       MAX(max_duration_ms)                                 AS max_duration_ms,
                       MAX(p95_duration_ms)                                 AS p95_duration_ms,
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
                groupBy.contains(RuleNodeDimension.TENANT_ID)      ? "tenant_id"         : "CAST(NULL AS uuid)",
                groupBy.contains(RuleNodeDimension.SERVICE_ID)     ? "service_id"        : "CAST(NULL AS varchar)",
                groupBy.contains(RuleNodeDimension.QUEUE_ID)       ? "queue_id"          : "CAST(NULL AS uuid)",
                groupBy.contains(RuleNodeDimension.RULE_CHAIN_ID)  ? "rule_chain_id"     : "CAST(NULL AS uuid)",
                groupBy.contains(RuleNodeDimension.RULE_NODE_ID)   ? "rule_node_id"      : "CAST(NULL AS uuid)"
        ));

        appendRuleNodeFilters(sql, query.queueIds(), query.ruleChainIds(), query.ruleNodeIds(), query.serviceIds());

        List<String> groupByCols = new ArrayList<>();
        groupByCols.add(bucketExpr);
        groupBy.forEach(dim -> groupByCols.add(dim.column));
        sql.append("GROUP BY ").append(String.join(", ", groupByCols)).append("\nORDER BY bucket_time ASC");

        Query nativeQuery = entityManager.createNativeQuery(sql.toString(), "RuleNodeTsKvEntry");
        bindRuleNodeParams(nativeQuery, query.tenantId(), query.startTs(), query.endTs(),
                query.queueIds(), query.ruleChainIds(), query.ruleNodeIds(), query.serviceIds());

        @SuppressWarnings("unchecked")
        List<RuleNodeTsKvEntry> result = nativeQuery.getResultList();
        return result;
    }

    @Override
    public List<RuleNodeTableRow> findRuleNodeTable(RuleNodeTableQuery query) {
        Set<RuleNodeDimension> groupBy = query.groupBy();

        StringBuilder sql = new StringBuilder("""
                SELECT SUM(exec_count)                                      AS exec_count,
                       SUM(error_count)                                     AS error_count,
                       SUM(total_duration_ms)                               AS total_duration_ms,
                       SUM(total_duration_ms) / NULLIF(SUM(exec_count), 0)  AS avg_duration_ms,
                       MAX(max_duration_ms)                                 AS max_duration_ms,
                       MAX(p95_duration_ms)                                 AS p95_duration_ms,
                       %s                                                   AS tenant_id,
                       %s                                                   AS service_id,
                       %s                                                   AS queue_id,
                       %s                                                   AS rule_chain_id,
                       %s                                                   AS rule_node_id
                FROM rule_node_exec_stats
                WHERE bucket_time >= :startTs
                  AND bucket_time < :endTs
                """.formatted(
                groupBy.contains(RuleNodeDimension.TENANT_ID)     ? "tenant_id"     : "CAST(NULL AS uuid)",
                groupBy.contains(RuleNodeDimension.SERVICE_ID)    ? "service_id"    : "CAST(NULL AS varchar)",
                groupBy.contains(RuleNodeDimension.QUEUE_ID)      ? "queue_id"      : "CAST(NULL AS uuid)",
                groupBy.contains(RuleNodeDimension.RULE_CHAIN_ID) ? "rule_chain_id" : "CAST(NULL AS uuid)",
                groupBy.contains(RuleNodeDimension.RULE_NODE_ID)  ? "rule_node_id"  : "CAST(NULL AS uuid)"
        ));

        appendRuleNodeFilters(sql, query.queueIds(), query.ruleChainIds(), query.ruleNodeIds(), query.serviceIds());

        if (!groupBy.isEmpty()) {
            sql.append("GROUP BY ")
               .append(String.join(", ", groupBy.stream().map(d -> d.column).toList()))
               .append("\n");
        }

        Query nativeQuery = entityManager.createNativeQuery(sql.toString(), "RuleNodeTableRow");
        bindRuleNodeParams(nativeQuery, query.tenantId(), query.startTs(), query.endTs(),
                query.queueIds(), query.ruleChainIds(), query.ruleNodeIds(), query.serviceIds());

        @SuppressWarnings("unchecked")
        List<RuleNodeTableRow> result = nativeQuery.getResultList();
        return result;
    }

    @Override
    public List<QueueTimeseriesEntry> findQueueTimeseries(QueueTimeseriesQuery query) {
        Set<RuleNodeDimension> groupBy = query.groupBy();
        String bucketExpr = query.intervalMs() != null
                ? "(bucket_time / " + query.intervalMs() + ") * " + query.intervalMs()
                : "bucket_time";

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
                groupBy.contains(RuleNodeDimension.TENANT_ID)       ?  "tenant_id"                 : "CAST(NULL AS uuid)",
                groupBy.contains(RuleNodeDimension.SERVICE_ID)       ? "service_id"                : "CAST(NULL AS varchar)",
                groupBy.contains(RuleNodeDimension.QUEUE_ID)         ? "queue_id"                  : "CAST(NULL AS uuid)",
                groupBy.contains(RuleNodeDimension.RULE_CHAIN_ID)    ? "rule_chain_id"             : "CAST(NULL AS uuid)",
                groupBy.contains(RuleNodeDimension.LAST_VISITED_RULE_NODE_ID) ? "last_visited_rule_node_id" : "CAST(NULL AS uuid)",
                groupBy.contains(RuleNodeDimension.QUEUE_TENANT_ID)           ? "queue_tenant_id"           : "CAST(NULL AS uuid)"
        ));

        appendQueueFilters(sql, query.queueIds(), query.ruleChainIds(), query.ruleNodeIds(), query.serviceIds());

        List<String> groupByCols = new ArrayList<>();
        groupByCols.add(bucketExpr);
        groupBy.forEach(dim -> groupByCols.add(dim.column));
        sql.append("GROUP BY ").append(String.join(", ", groupByCols)).append("\nORDER BY bucket_time ASC");

        Query nativeQuery = entityManager.createNativeQuery(sql.toString(), "QueueTimeseriesEntry");
        bindQueueParams(nativeQuery, query.tenantId(), query.startTs(), query.endTs(),
                query.queueIds(), query.ruleChainIds(), query.ruleNodeIds(), query.serviceIds());

        @SuppressWarnings("unchecked")
        List<QueueTimeseriesEntry> result = nativeQuery.getResultList();
        return result;
    }

    @Override
    public List<QueueTableEntry> findQueueTable(QueueTableQuery query) {
        Set<RuleNodeDimension> groupBy = query.groupBy();

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
                groupBy.contains(RuleNodeDimension.TENANT_ID)       ? "tenant_id"                 : "CAST(NULL AS uuid)",
                groupBy.contains(RuleNodeDimension.SERVICE_ID)       ? "service_id"                : "CAST(NULL AS varchar)",
                groupBy.contains(RuleNodeDimension.QUEUE_ID)         ? "queue_id"                  : "CAST(NULL AS uuid)",
                groupBy.contains(RuleNodeDimension.RULE_CHAIN_ID)    ? "rule_chain_id"             : "CAST(NULL AS uuid)",
                groupBy.contains(RuleNodeDimension.LAST_VISITED_RULE_NODE_ID) ? "last_visited_rule_node_id" : "CAST(NULL AS uuid)",
                groupBy.contains(RuleNodeDimension.QUEUE_TENANT_ID)           ? "queue_tenant_id"           : "CAST(NULL AS uuid)"
        ));

        appendQueueFilters(sql, query.queueIds(), query.ruleChainIds(), query.ruleNodeIds(), query.serviceIds());

        if (!groupBy.isEmpty()) {
            List<String> groupByCols = groupBy.stream().map(dim -> dim.column).toList();
            sql.append("GROUP BY ").append(String.join(", ", groupByCols)).append("\n");
        }

        Query nativeQuery = entityManager.createNativeQuery(sql.toString(), "QueueTableEntry");
        bindQueueParams(nativeQuery, query.tenantId(), query.startTs(), query.endTs(),
                query.queueIds(), query.ruleChainIds(), query.ruleNodeIds(), query.serviceIds());

        @SuppressWarnings("unchecked")
        List<QueueTableEntry> result = nativeQuery.getResultList();
        return result;
    }

    @Override
    public void saveQueueStats(List<QueueStatsRow> rows) {
        if (rows.isEmpty()) {
            return;
        }
        for (int i = 0; i < rows.size(); i += BATCH_SIZE) {
            List<QueueStatsRow> chunk = rows.subList(i, Math.min(i + BATCH_SIZE, rows.size()));
            jdbcTemplate.batchUpdate(INSERT_QUEUE_STATS_SQL, chunk, chunk.size(), (ps, row) -> {
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
    }

    @Override
    public void saveQueueLagStats(List<QueueLagStatsRow> rows) {
        if (rows.isEmpty()) {
            return;
        }
        for (int i = 0; i < rows.size(); i += BATCH_SIZE) {
            List<QueueLagStatsRow> chunk = rows.subList(i, Math.min(i + BATCH_SIZE, rows.size()));
            jdbcTemplate.batchUpdate(INSERT_QUEUE_LAG_STATS_SQL, chunk, chunk.size(), (ps, row) -> {
                ps.setLong(1, row.bucketTime());
                ps.setObject(2, row.queueTenantId());
                ps.setObject(3, row.queueId());
                ps.setLong(4, row.lag());
            });
        }
    }

    @Override
    public boolean tryAcquireQueueLagLeader(String serviceId, long nowMs, long leaseMs) {
        int updated = jdbcTemplate.update(
                "UPDATE span_stats_leader" +
                        "   SET leader_id = ?," +
                        "       leader_until = ?" +
                        " WHERE lock_name = 'queue-lag'" +
                        "   AND (leader_id = ? OR leader_until < ?)",
                serviceId, nowMs + leaseMs, serviceId, nowMs);
        return updated > 0;
    }

    @Override
    public List<QueueLagTimeseriesEntry> findQueueLagTimeseries(QueueLagTimeseriesQuery query) {
        Set<RuleNodeDimension> groupBy = query.groupBy();
        String bucketExpr = query.intervalMs() != null
                ? "(bucket_time / " + query.intervalMs() + ") * " + query.intervalMs()
                : "bucket_time";

        boolean hasQueueIds = query.queueIds() != null && !query.queueIds().isEmpty();

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
                groupBy.contains(RuleNodeDimension.QUEUE_ID)        ? "queue_id"        : "CAST(NULL AS uuid)"
        ));

        if (hasQueueIds) {
            sql.append("  AND queue_id IN (:queueIds)\n");
        }

        List<String> groupByCols = new ArrayList<>();
        groupByCols.add(bucketExpr);
        groupBy.forEach(dim -> groupByCols.add(dim.column));
        sql.append("GROUP BY ").append(String.join(", ", groupByCols)).append("\nORDER BY bucket_time ASC");

        Query nativeQuery = entityManager.createNativeQuery(sql.toString(), "QueueLagTimeseriesEntry");
        nativeQuery.setParameter("startTs", query.startTs());
        nativeQuery.setParameter("endTs", query.endTs());
        nativeQuery.setParameter("tenantId", query.tenantId());
        if (hasQueueIds) {
            nativeQuery.setParameter("queueIds", query.queueIds());
        }

        @SuppressWarnings("unchecked")
        List<QueueLagTimeseriesEntry> result = nativeQuery.getResultList();
        return result;
    }

    @Override
    public List<QueueLagTableEntry> findQueueLagTable(QueueLagTableQuery query) {
        Set<RuleNodeDimension> groupBy = query.groupBy();

        StringBuilder sql = new StringBuilder("""
                SELECT SUM(lag)  AS lag,
                       %s        AS queue_tenant_id,
                       %s        AS queue_id
                FROM rule_node_queue_lag_stats
                WHERE bucket_time >= :startTs
                  AND bucket_time < :endTs
                  AND queue_tenant_id IN (:systemTenantId, :tenantId)
                """.formatted(
                groupBy.contains(RuleNodeDimension.QUEUE_TENANT_ID) ? "queue_tenant_id" : "CAST(NULL AS uuid)",
                groupBy.contains(RuleNodeDimension.QUEUE_ID)        ? "queue_id"        : "CAST(NULL AS uuid)"
        ));

        if (query.queueIds() != null && !query.queueIds().isEmpty()) {
            sql.append("  AND queue_id IN (:queueIds)\n");
        }

        if (!groupBy.isEmpty()) {
            sql.append("GROUP BY ")
               .append(String.join(", ", groupBy.stream().map(d -> d.column).toList()))
               .append("\n");
        }

        Query nativeQuery = entityManager.createNativeQuery(sql.toString(), "QueueLagTableEntry");
        nativeQuery.setParameter("startTs", query.startTs());
        nativeQuery.setParameter("endTs", query.endTs());
        nativeQuery.setParameter("systemTenantId", TenantId.SYS_TENANT_ID.getId());
        nativeQuery.setParameter("tenantId", query.tenantId());
        if (query.queueIds() != null && !query.queueIds().isEmpty()) {
            nativeQuery.setParameter("queueIds", query.queueIds());
        }

        @SuppressWarnings("unchecked")
        List<QueueLagTableEntry> result = nativeQuery.getResultList();
        return result;
    }

    @Override
    public long findLastQueueLag(UUID tenantId, List<UUID> queueIds) {
        boolean hasQueueIds = queueIds != null && !queueIds.isEmpty();
        long startTs = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(5);

        StringBuilder sql = new StringBuilder("""
                SELECT COALESCE(SUM(latest.lag), 0)
                FROM (
                    SELECT DISTINCT ON (queue_tenant_id, queue_id) lag
                    FROM rule_node_queue_lag_stats
                    WHERE queue_tenant_id = :tenantId
                      AND bucket_time >= :startTs
                """);
        if (hasQueueIds) {
            sql.append("  AND queue_id IN (:queueIds)\n");
        }
        sql.append("""
                    ORDER BY queue_tenant_id, queue_id, bucket_time DESC
                ) latest
                """);

        Query nativeQuery = entityManager.createNativeQuery(sql.toString());
        nativeQuery.setParameter("tenantId", tenantId);
        nativeQuery.setParameter("startTs", startTs);
        if (hasQueueIds) {
            nativeQuery.setParameter("queueIds", queueIds);
        }

        Object lag = nativeQuery.getResultList().stream().findFirst().orElse(null);
        return lag == null ? 0L : ((Number) lag).longValue();
    }

    @Override
    public List<QueueShortInfo> findAllQueuesShortInfo() {
        return mainJdbcTemplate.query(
                "SELECT id, tenant_id, name, partitions, consumer_per_partition FROM queue",
                (rs, rowNum) -> new QueueShortInfo(
                        (UUID) rs.getObject("id"),
                        (UUID) rs.getObject("tenant_id"),
                        rs.getString("name"),
                        rs.getInt("partitions"),
                        rs.getBoolean("consumer_per_partition")
                ));
    }

    @Override
    public List<RuleEngineMonitoringQueueOption> findFilterQueues(UUID tenantId) {
        return mainJdbcTemplate.query("""
                        SELECT id, name, tenant_id
                        FROM queue
                        WHERE tenant_id IN (?, ?)
                        ORDER BY name, id
                        """,
                (rs, rowNum) -> new RuleEngineMonitoringQueueOption(
                        (UUID) rs.getObject("id"),
                        rs.getString("name"),
                        (UUID) rs.getObject("tenant_id")
                ),
                TenantId.SYS_TENANT_ID.getId(), tenantId);
    }

    @Override
    public List<RuleEngineMonitoringOption> findFilterRuleChains(UUID tenantId) {
        return mainJdbcTemplate.query("""
                        SELECT id, name
                        FROM rule_chain
                        WHERE tenant_id = ?
                          AND type = 'CORE'
                        ORDER BY name, id
                        """,
                (rs, rowNum) -> new RuleEngineMonitoringOption(
                        (UUID) rs.getObject("id"),
                        rs.getString("name")
                ),
                tenantId);
    }

    @Override
    public List<RuleEngineMonitoringRuleNodeOption> findFilterRuleNodes(UUID tenantId) {
        return mainJdbcTemplate.query("""
                        SELECT rn.id,
                               rn.name,
                               rc.id AS rule_chain_id,
                               rc.name AS rule_chain_name
                        FROM rule_chain rc
                        JOIN rule_node rn ON rn.rule_chain_id = rc.id
                        WHERE rc.tenant_id = ?
                          AND rc.type = 'CORE'
                        ORDER BY rn.name, rn.id
                        """,
                (rs, rowNum) -> new RuleEngineMonitoringRuleNodeOption(
                        (UUID) rs.getObject("id"),
                        rs.getString("name"),
                        (UUID) rs.getObject("rule_chain_id"),
                        rs.getString("rule_chain_name")
                ),
                tenantId);
    }

    @Override
    public Map<UUID, UUID> findRuleChainIdsByRuleNodeIds(Collection<UUID> ruleNodeIds) {
        if (ruleNodeIds.isEmpty()) {
            return Map.of();
        }
        String sql = "SELECT id, rule_chain_id FROM rule_node WHERE id = ANY(?)";
        Map<UUID, UUID> result = new HashMap<>();
        mainJdbcTemplate.query(con -> {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setArray(1, con.createArrayOf("uuid", ruleNodeIds.toArray(new UUID[0])));
            return ps;
        }, rs -> {
            UUID nodeId = (UUID) rs.getObject("id");
            UUID chainId = (UUID) rs.getObject("rule_chain_id");
            if (chainId != null) {
                result.put(nodeId, chainId);
            }
        });
        return result;
    }

    @Override
    public void saveRuleNodeStats(List<RuleNodeExecStatsRow> rows) {
        if (rows.isEmpty()) {
            return;
        }

        for (int i = 0; i < rows.size(); i += BATCH_SIZE) {
            List<RuleNodeExecStatsRow> chunk = rows.subList(i, Math.min(i + BATCH_SIZE, rows.size()));
            jdbcTemplate.batchUpdate(INSERT_SQL, chunk, chunk.size(), (ps, row) -> {
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
                ps.setLong(11, row.p95DurationMs());
            });
        }
    }

    private void appendQueueFilters(StringBuilder sql,
                                    List<UUID> queueIds, List<UUID> ruleChainIds, List<UUID> ruleNodeIds,
                                    List<String> serviceIds) {
        sql.append("  AND tenant_id = :tenantId\n");
        if (queueIds != null && !queueIds.isEmpty())         sql.append("  AND queue_id IN (:queueIds)\n");
        if (ruleChainIds != null && !ruleChainIds.isEmpty()) sql.append("  AND rule_chain_id IN (:ruleChainIds)\n");
        if (ruleNodeIds != null && !ruleNodeIds.isEmpty())   sql.append("  AND last_visited_rule_node_id IN (:ruleNodeIds)\n");
        if (serviceIds != null && !serviceIds.isEmpty())     sql.append("  AND service_id IN (:serviceIds)\n");
    }

    private void bindQueueParams(Query nativeQuery, UUID tenantId, long startTs, long endTs,
                                 List<UUID> queueIds, List<UUID> ruleChainIds, List<UUID> ruleNodeIds,
                                 List<String> serviceIds) {
        nativeQuery.setParameter("startTs", startTs);
        nativeQuery.setParameter("endTs", endTs);
        nativeQuery.setParameter("tenantId", tenantId);
        bindCommonParams(nativeQuery, queueIds, ruleChainIds, ruleNodeIds, serviceIds);
    }

    private void bindCommonParams(Query nativeQuery, List<UUID> queueIds, List<UUID> ruleChainIds, List<UUID> ruleNodeIds, List<String> serviceIds) {
        if (queueIds != null && !queueIds.isEmpty())         nativeQuery.setParameter("queueIds", queueIds);
        if (ruleChainIds != null && !ruleChainIds.isEmpty()) nativeQuery.setParameter("ruleChainIds", ruleChainIds);
        if (ruleNodeIds != null && !ruleNodeIds.isEmpty())   nativeQuery.setParameter("ruleNodeIds", ruleNodeIds);
        if (serviceIds != null && !serviceIds.isEmpty())     nativeQuery.setParameter("serviceIds", serviceIds);
    }

    private void appendRuleNodeFilters(StringBuilder sql,
                                       List<UUID> queueIds, List<UUID> ruleChainIds, List<UUID> ruleNodeIds,
                                       List<String> serviceIds) {
        sql.append("  AND tenant_id = :tenantId\n");
        if (queueIds != null && !queueIds.isEmpty())         sql.append("  AND queue_id IN (:queueIds)\n");
        if (ruleChainIds != null && !ruleChainIds.isEmpty()) sql.append("  AND rule_chain_id IN (:ruleChainIds)\n");
        if (ruleNodeIds != null && !ruleNodeIds.isEmpty())   sql.append("  AND rule_node_id IN (:ruleNodeIds)\n");
        if (serviceIds != null && !serviceIds.isEmpty())     sql.append("  AND service_id IN (:serviceIds)\n");
    }

    private void bindRuleNodeParams(Query nativeQuery, UUID tenantId, long startTs, long endTs,
                                    List<UUID> queueIds, List<UUID> ruleChainIds, List<UUID> ruleNodeIds,
                                    List<String> serviceIds) {
        nativeQuery.setParameter("startTs", startTs);
        nativeQuery.setParameter("endTs", endTs);
        if (tenantId != null)                                nativeQuery.setParameter("tenantId", tenantId);
        bindCommonParams(nativeQuery, queueIds, ruleChainIds, ruleNodeIds, serviceIds);
    }
}
