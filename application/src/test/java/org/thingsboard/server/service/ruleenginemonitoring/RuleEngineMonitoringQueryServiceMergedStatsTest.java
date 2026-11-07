// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.ruleenginemonitoring;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.thingsboard.server.common.data.ruleenginemonitoring.MergedStatsDelta;
import org.thingsboard.server.common.data.ruleenginemonitoring.MergedStatsTableRow;
import org.thingsboard.server.common.data.ruleenginemonitoring.MetricDelta;
import org.thingsboard.server.common.data.ruleenginemonitoring.QueueTableEntry;
import org.thingsboard.server.common.data.ruleenginemonitoring.RuleNodeTableRow;
import org.thingsboard.server.dao.sql.ruleenginemonitoring.QueueTableQuery;
import org.thingsboard.server.dao.sql.ruleenginemonitoring.RuleEngineMonitoringDao;
import org.thingsboard.server.dao.sql.ruleenginemonitoring.RuleNodeDimension;
import org.thingsboard.server.dao.sql.ruleenginemonitoring.RuleNodeTableQuery;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.queue.discovery.PartitionService;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;

class RuleEngineMonitoringQueryServiceMergedStatsTest {

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID QUEUE = UUID.randomUUID();
    private static final UUID CHAIN = UUID.randomUUID();
    private static final UUID X = UUID.randomUUID();
    private static final UUID Y = UUID.randomUUID();
    private static final UUID Z = UUID.randomUUID();
    // The node side groups by rule node, the queue side by the last visited rule node (as the controller maps groupBy).
    private static final Set<RuleNodeDimension> DIMS = Set.of(RuleNodeDimension.RULE_NODE_ID);
    private static final Set<RuleNodeDimension> QUEUE_DIMS = Set.of(RuleNodeDimension.LAST_VISITED_RULE_NODE_ID);
    private static final long BASE_START = 0;
    private static final long BASE_END = 1_000;
    private static final long COMPARE_START = 1_000;
    private static final long COMPARE_END = 2_000;

    private final RuleEngineMonitoringDao dao = Mockito.mock(RuleEngineMonitoringDao.class);
    private final RuleEngineMonitoringQueryService service = new RuleEngineMonitoringQueryService(dao,
            Mockito.mock(PartitionService.class), Mockito.mock(TbTenantProfileCache.class));

    @Test
    void givenNodeAndQueueRowsOfOneNode_whenGetMergedStatsTable_thenTheyAreJoinedIntoOneRow() {
        givenRange(BASE_START, List.of(nodeRow(X, 10, 1, 100, 10, 30)), List.of(queueRow(X, 2), queueRow(Z, 4)));

        List<MergedStatsTableRow> rows = service.getMergedStatsTable(TENANT_ID, BASE_START, BASE_END, DIMS, QUEUE_DIMS,
                null, null, null, null);

        assertThat(rows).containsExactly(
                new MergedStatsTableRow(null, null, X, null, 10L, 1L, 100L, 10L, 30L, 2L),
                // A timeout at a node with no exec stats in the range still gets its own row.
                new MergedStatsTableRow(null, null, Z, null, null, null, null, null, null, 4L));
    }

    @Test
    void givenNodeInBothRangesAndNodeInBaseOnly_whenGetMergedStatsTableCompare_thenDeltasMatchRowsAcrossRanges() {
        givenRange(BASE_START, List.of(nodeRow(X, 10, 0, 100, 10, 20), nodeRow(Y, 4, 0, 40, 10, 10)), List.of(queueRow(X, 2)));
        givenRange(COMPARE_START, List.of(nodeRow(X, 15, 0, 150, 10, 20)), List.of(queueRow(X, 2)));

        List<MergedStatsDelta> deltas = service.getMergedStatsTableCompare(TENANT_ID, BASE_START, BASE_END,
                COMPARE_START, COMPARE_END, DIMS, QUEUE_DIMS, null, null, null, null);

        assertThat(deltas).extracting(MergedStatsDelta::ruleNodeId).containsExactly(X, Y);
        MergedStatsDelta x = deltas.get(0);
        assertThat(x.execCount()).isEqualTo(new MetricDelta(10L, 15L, 5L, 50.0));
        // Unchanged: delta 0, and a change under 1% has no percentage.
        assertThat(x.timeoutCount()).isEqualTo(new MetricDelta(2L, 2L, 0L, null));
        MergedStatsDelta y = deltas.get(1);
        assertThat(y.execCount()).isEqualTo(new MetricDelta(4L, null, null, null));
        assertThat(y.timeoutCount()).isEqualTo(new MetricDelta(null, null, null, null));
    }

    private void givenRange(long startTs, List<RuleNodeTableRow> nodeRows, List<QueueTableEntry> queueRows) {
        Mockito.when(dao.findRuleNodeTable(argThat((RuleNodeTableQuery q) -> q != null && q.startTs() == startTs
                && q.groupBy().equals(DIMS)))).thenReturn(nodeRows);
        Mockito.when(dao.findQueueTable(argThat((QueueTableQuery q) -> q != null && q.startTs() == startTs
                && q.groupBy().equals(QUEUE_DIMS)))).thenReturn(queueRows);
    }

    private static RuleNodeTableRow nodeRow(UUID ruleNodeId, long execs, long errors, long totalMs, long avgMs, long maxMs) {
        return new RuleNodeTableRow(execs, errors, totalMs, avgMs, maxMs, TENANT_ID, null, QUEUE, CHAIN, ruleNodeId);
    }

    private static QueueTableEntry queueRow(UUID lastVisitedRuleNodeId, long timeouts) {
        return new QueueTableEntry(timeouts, 0L, 0L, TENANT_ID, null, QUEUE, CHAIN, lastVisitedRuleNodeId, TENANT_ID);
    }

}
