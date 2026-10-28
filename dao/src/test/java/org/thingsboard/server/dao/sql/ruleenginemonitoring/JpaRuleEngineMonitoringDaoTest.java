// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.ruleenginemonitoring;

import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.ruleenginemonitoring.RuleEngineMonitoringOption;
import org.thingsboard.server.common.data.ruleenginemonitoring.RuleEngineMonitoringQueueOption;
import org.thingsboard.server.common.data.ruleenginemonitoring.RuleEngineMonitoringRuleNodeOption;
import org.thingsboard.server.dao.AbstractJpaDaoTest;
import org.thingsboard.server.common.data.ruleenginemonitoring.QueueLagTableEntry;
import org.thingsboard.server.common.data.ruleenginemonitoring.QueueLagTimeseriesEntry;
import org.thingsboard.server.common.data.ruleenginemonitoring.QueueTableEntry;
import org.thingsboard.server.common.data.ruleenginemonitoring.QueueTimeseriesEntry;
import org.thingsboard.server.common.data.ruleenginemonitoring.RuleNodeTableRow;
import org.thingsboard.server.common.data.ruleenginemonitoring.RuleNodeTsKvEntry;
import org.thingsboard.server.dao.sql.trace.TraceTestTables;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Pins the DAO contract: every call runs outside a transaction, the way the monitoring services call it.
 */
public class JpaRuleEngineMonitoringDaoTest extends AbstractJpaDaoTest {

    private static final long T0 = 14_166_666L * 120_000L; // aligned to the 1- and 2-minute buckets used below
    private static final long MINUTE = 60_000L;

    @Autowired
    private RuleEngineMonitoringDao dao;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID queueId = UUID.randomUUID();
    private final UUID ruleChainId = UUID.randomUUID();
    private final UUID ruleNodeId = UUID.randomUUID();

    @After
    public void cleanUp() {
        TraceTestTables.truncate(jdbcTemplate);
        jdbcTemplate.update("DELETE FROM rule_node WHERE rule_chain_id = ?", ruleChainId);
        jdbcTemplate.update("DELETE FROM rule_chain WHERE id = ?", ruleChainId);
        jdbcTemplate.update("DELETE FROM queue WHERE id = ?", queueId);
    }

    @Test
    public void givenRuleNodeStats_whenFindTimeseriesAndTable_thenStatsAreAggregated() {
        dao.saveRuleNodeStats(List.of(
                ruleNodeRow(T0, "svc-1", 10, 1, 100, 30),
                ruleNodeRow(T0 + MINUTE, "svc-2", 30, 2, 300, 50)));

        List<RuleNodeTsKvEntry> perMinute = dao.findRuleNodeTimeseries(new RuleNodeTimeseriesQuery(tenantId, T0, T0 + 2 * MINUTE,
                Set.of(RuleNodeDimension.RULE_NODE_ID), null, null, null, null, MINUTE));
        assertEquals(2, perMinute.size());
        assertEquals(Long.valueOf(T0), perMinute.get(0).bucketTime());
        assertEquals(Long.valueOf(10), perMinute.get(0).execCount());
        assertEquals(ruleNodeId, perMinute.get(0).ruleNodeId());
        assertNull(perMinute.get(0).serviceId());

        List<RuleNodeTsKvEntry> perWindow = dao.findRuleNodeTimeseries(new RuleNodeTimeseriesQuery(tenantId, T0, T0 + 2 * MINUTE,
                Set.of(), List.of(queueId), List.of(ruleChainId), List.of(ruleNodeId), List.of("svc-1", "svc-2"), 2 * MINUTE));
        assertEquals(1, perWindow.size());
        assertEquals(Long.valueOf(40), perWindow.get(0).execCount());
        assertEquals(Long.valueOf(10), perWindow.get(0).avgDurationMs());
        assertEquals(Long.valueOf(50), perWindow.get(0).maxDurationMs());

        List<RuleNodeTableRow> byService = dao.findRuleNodeTable(new RuleNodeTableQuery(tenantId, T0, T0 + 2 * MINUTE,
                Set.of(RuleNodeDimension.SERVICE_ID), null, null, null, null));
        assertEquals(2, byService.size());
        List<RuleNodeTableRow> total = dao.findRuleNodeTable(new RuleNodeTableQuery(tenantId, T0, T0 + 2 * MINUTE,
                Set.of(), null, null, null, List.of("svc-2")));
        assertEquals(1, total.size());
        assertEquals(Long.valueOf(2), total.get(0).errorCount());
        assertNull(total.get(0).ruleNodeId());
    }

    @Test
    public void givenQueueStats_whenFindTimeseriesAndTable_thenStatsAreAggregated() {
        dao.saveQueueStats(List.of(
                new QueueStatsRow(T0, tenantId, tenantId, queueId, ruleChainId, ruleNodeId, "svc-1", 1, 2, 30),
                new QueueStatsRow(T0 + MINUTE, tenantId, tenantId, queueId, ruleChainId, ruleNodeId, "svc-1", 0, 1, 20)));

        List<QueueTimeseriesEntry> series = dao.findQueueTimeseries(new QueueTimeseriesQuery(tenantId, T0, T0 + 2 * MINUTE,
                Set.of(RuleNodeDimension.QUEUE_ID, RuleNodeDimension.LAST_VISITED_RULE_NODE_ID), List.of(queueId), null, List.of(ruleNodeId), null, MINUTE));
        assertEquals(2, series.size());
        assertEquals(Long.valueOf(30), series.get(0).successCount());
        assertEquals(queueId, series.get(0).queueId());
        assertEquals(ruleNodeId, series.get(0).ruleNodeId());

        List<QueueTableEntry> table = dao.findQueueTable(new QueueTableQuery(tenantId, T0, T0 + 2 * MINUTE,
                Set.of(RuleNodeDimension.QUEUE_TENANT_ID), null, List.of(ruleChainId), null, List.of("svc-1")));
        assertEquals(1, table.size());
        assertEquals(Long.valueOf(3), table.get(0).failureCount());
        assertEquals(tenantId, table.get(0).queueTenantId());
    }

    @Test
    public void givenQueueLagStats_whenFindTimeseriesTableAndLast_thenLagIsReturned() {
        long now = System.currentTimeMillis();
        dao.saveQueueLagStats(List.of(
                new QueueLagStatsRow(now - 2 * MINUTE, tenantId, queueId, 100),
                new QueueLagStatsRow(now - MINUTE, tenantId, queueId, 40)));

        List<QueueLagTimeseriesEntry> series = dao.findQueueLagTimeseries(new QueueLagTimeseriesQuery(tenantId, now - 3 * MINUTE, now,
                Set.of(RuleNodeDimension.QUEUE_ID), List.of(queueId), null));
        assertEquals(2, series.size());
        assertEquals(Long.valueOf(100), series.get(0).lag());
        assertEquals(queueId, series.get(0).queueId());

        List<QueueLagTableEntry> table = dao.findQueueLagTable(new QueueLagTableQuery(tenantId, now - 3 * MINUTE, now, Set.of(), null));
        assertEquals(1, table.size());
        assertEquals(Long.valueOf(140), table.get(0).lag());

        assertEquals(Long.valueOf(40), dao.findLastQueueLag(tenantId, List.of(queueId), 5 * MINUTE));
        assertEquals(Long.valueOf(40), dao.findLastQueueLag(tenantId, null, 5 * MINUTE));
        assertNull(dao.findLastQueueLag(UUID.randomUUID(), null, 5 * MINUTE));
    }

    @Test
    public void givenSystemQueueLag_whenFindIsolatedTenantsLagTable_thenSystemRowsAreExcluded() {
        dao.saveQueueLagStats(List.of(new QueueLagStatsRow(T0, tenantId, queueId, 5),
                new QueueLagStatsRow(T0, TenantId.SYS_TENANT_ID.getId(), UUID.randomUUID(), 1000)));

        List<QueueLagTableEntry> table = dao.findQueueLagTable(new QueueLagTableQuery(tenantId, T0, T0 + MINUTE, Set.of(), null));

        assertEquals(1, table.size());
        assertEquals(Long.valueOf(5), table.get(0).lag());
    }

    @Test
    public void givenDrainedQueue_whenFindLastQueueLag_thenZeroSampleWins() {
        long now = System.currentTimeMillis();
        dao.saveQueueLagStats(List.of(new QueueLagStatsRow(now - 2 * MINUTE, tenantId, queueId, 500),
                new QueueLagStatsRow(now - MINUTE, tenantId, queueId, 0)));

        assertEquals(Long.valueOf(0), dao.findLastQueueLag(tenantId, null, 5 * MINUTE));
    }

    @Test
    public void givenLongFlushInterval_whenFindLastQueueLag_thenOlderSampleIsStillFound() {
        dao.saveQueueLagStats(List.of(new QueueLagStatsRow(System.currentTimeMillis() - 10 * MINUTE, tenantId, queueId, 300)));

        assertEquals(Long.valueOf(300), dao.findLastQueueLag(tenantId, null, 22 * MINUTE)); // 2 x 10-minute flush + 2-minute lease
        assertNull(dao.findLastQueueLag(tenantId, null, 5 * MINUTE)); // stale: lag unknown
    }

    @Test
    public void givenDimensionOfAnotherTable_whenFindStats_thenRejected() {
        // the repository's IllegalArgumentException, as Spring's exception translation hands it out
        assertThrows(InvalidDataAccessApiUsageException.class, () -> dao.findQueueTable(new QueueTableQuery(tenantId, T0, T0 + MINUTE,
                Set.of(RuleNodeDimension.RULE_NODE_ID), null, null, null, null)));
        assertThrows(InvalidDataAccessApiUsageException.class, () -> dao.findRuleNodeTable(new RuleNodeTableQuery(tenantId, T0, T0 + MINUTE,
                Set.of(RuleNodeDimension.LAST_VISITED_RULE_NODE_ID), null, null, null, null)));
        assertThrows(InvalidDataAccessApiUsageException.class, () -> dao.findQueueLagTable(new QueueLagTableQuery(tenantId, T0, T0 + MINUTE,
                Set.of(RuleNodeDimension.SERVICE_ID), null)));
    }

    @Test
    public void givenRuleChainAndQueue_whenLookupFilterOptions_thenMainDbEntitiesAreReturned() {
        jdbcTemplate.update("INSERT INTO rule_chain (id, created_time, tenant_id, name, type, root, debug_mode) VALUES (?, 0, ?, 'Chain', 'CORE', false, false)",
                ruleChainId, tenantId);
        jdbcTemplate.update("INSERT INTO rule_node (id, created_time, rule_chain_id, type, name) VALUES (?, 0, ?, 'TbLogNode', 'Log')",
                ruleNodeId, ruleChainId);
        jdbcTemplate.update("INSERT INTO queue (id, created_time, tenant_id, name, topic, poll_interval, partitions, consumer_per_partition, pack_processing_timeout) " +
                "VALUES (?, 0, ?, ?, 'tb_rule_engine.test', 25, 3, true, 2000)", queueId, tenantId, "Q-" + queueId);

        assertEquals(Map.of(ruleNodeId, ruleChainId), dao.findRuleChainIdsByRuleNodeIds(List.of(ruleNodeId, UUID.randomUUID())));
        assertEquals(List.of(new RuleEngineMonitoringOption(ruleChainId, "Chain")), dao.findFilterRuleChains(tenantId));
        assertEquals(List.of(new RuleEngineMonitoringRuleNodeOption(ruleNodeId, "Log", ruleChainId, "Chain")), dao.findFilterRuleNodes(tenantId));
        assertTrue(dao.findFilterQueues(tenantId).contains(new RuleEngineMonitoringQueueOption(queueId, "Q-" + queueId, tenantId)));
        assertTrue(dao.findFilterQueues(tenantId).stream().allMatch(q ->
                q.tenantId().equals(tenantId) || q.tenantId().equals(TenantId.SYS_TENANT_ID.getId())));
        assertTrue(dao.findAllQueuesShortInfo().contains(new QueueShortInfo(queueId, tenantId, "Q-" + queueId, 3, true)));
    }

    @Test
    public void givenStatsOfTwoTenants_whenDeleteByTenantId_thenOnlyThatTenantsStatsAreRemoved() {
        UUID otherTenantId = UUID.randomUUID();
        dao.saveRuleNodeStats(List.of(ruleNodeRow(T0, "svc-1", 1, 0, 10, 10),
                new RuleNodeExecStatsRow(otherTenantId, queueId, ruleChainId, ruleNodeId, "svc-1", T0, 1, 0, 10, 10)));
        dao.saveQueueStats(List.of(
                // the tenant's message on a system queue, and another tenant's message on the tenant's isolated queue
                new QueueStatsRow(T0, UUID.randomUUID(), tenantId, queueId, ruleChainId, ruleNodeId, "svc-1", 1, 0, 0),
                new QueueStatsRow(T0, tenantId, otherTenantId, queueId, ruleChainId, ruleNodeId, "svc-1", 1, 0, 0),
                new QueueStatsRow(T0, otherTenantId, otherTenantId, queueId, ruleChainId, ruleNodeId, "svc-1", 1, 0, 0)));
        dao.saveQueueLagStats(List.of(new QueueLagStatsRow(T0, tenantId, queueId, 5),
                new QueueLagStatsRow(T0, otherTenantId, queueId, 5)));

        dao.deleteByTenantId(tenantId);

        assertEquals(List.of(otherTenantId), jdbcTemplate.queryForList("SELECT tenant_id FROM rule_node_exec_stats", UUID.class));
        assertEquals(List.of(otherTenantId), jdbcTemplate.queryForList("SELECT queue_tenant_id FROM rule_node_queue_stats", UUID.class));
        assertEquals(List.of(otherTenantId), jdbcTemplate.queryForList("SELECT queue_tenant_id FROM rule_node_queue_lag_stats", UUID.class));
    }

    @Test
    public void givenOldAndRecentStats_whenCleanUp_thenOnlyExpiredStatsAreRemoved() {
        dao.saveRuleNodeStats(List.of(ruleNodeRow(T0, "svc-1", 1, 0, 10, 10), ruleNodeRow(T0 + 10 * MINUTE, "svc-1", 1, 0, 10, 10)));
        dao.saveQueueStats(List.of(new QueueStatsRow(T0, tenantId, tenantId, queueId, ruleChainId, ruleNodeId, "svc-1", 1, 0, 0),
                new QueueStatsRow(T0 + 10 * MINUTE, tenantId, tenantId, queueId, ruleChainId, ruleNodeId, "svc-1", 1, 0, 0)));
        dao.saveQueueLagStats(List.of(new QueueLagStatsRow(T0, tenantId, queueId, 5),
                new QueueLagStatsRow(T0 + 10 * MINUTE, tenantId, queueId, 5)));

        dao.cleanUp(T0 + MINUTE, 1);

        for (String table : List.of("rule_node_exec_stats", "rule_node_queue_stats", "rule_node_queue_lag_stats")) {
            assertEquals(table, List.of(T0 + 10 * MINUTE), jdbcTemplate.queryForList("SELECT bucket_time FROM " + table, Long.class));
        }
    }

    @Test
    public void givenNoTraceSchema_whenDeleteByTenantId_thenNothingFails() {
        cleanUp();

        dao.deleteByTenantId(tenantId);
    }

    private RuleNodeExecStatsRow ruleNodeRow(long bucketTime, String serviceId, long execCount, long errorCount,
                                             long totalDurationMs, long maxDurationMs) {
        return new RuleNodeExecStatsRow(tenantId, queueId, ruleChainId, ruleNodeId, serviceId, bucketTime,
                execCount, errorCount, totalDurationMs, maxDurationMs);
    }
}
