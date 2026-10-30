// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.trace;

import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.trace.RuleNodeTreeNodeDTO;
import org.thingsboard.server.common.data.trace.TracePathDetailsDTO;
import org.thingsboard.server.dao.model.sql.ruleenginemonitoring.TraceGroup;
import org.thingsboard.server.dao.model.sql.ruleenginemonitoring.TraceGroup.RuleNodePathMetric;
import org.thingsboard.server.dao.model.sql.ruleenginemonitoring.TraceGroup.VisitedRuleNode;
import org.thingsboard.server.dao.queue.QueueService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.sql.ruleenginemonitoring.RuleEngineMonitoringDao;
import org.thingsboard.server.dao.sql.trace.TraceDao;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.service.component.ComponentDiscoveryService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TraceServicePathDetailsTest {

    private static final TenantId TENANT_ID = TenantId.fromUUID(UUID.randomUUID());
    private static final String PATH_ID = "path-1";
    private static final long DAY = 86_400_000L;
    private static final UUID QUEUE = UUID.randomUUID();
    private static final UUID CHAIN = UUID.randomUUID();
    private static final UUID A = UUID.randomUUID();
    private static final UUID B = UUID.randomUUID();
    private static final UUID C = UUID.randomUUID();
    private static final UUID D = UUID.randomUUID();
    private static final UUID X = UUID.randomUUID();
    private static final UUID Y = UUID.randomUUID();

    private final TraceDao traceDao = mock(TraceDao.class);
    private final TraceService service = new TraceService(traceDao, mock(RuleChainService.class), mock(QueueService.class),
            mock(RuleEngineMonitoringDao.class), mock(PartitionService.class), mock(ComponentDiscoveryService.class));

    @Test
    void givenGroupsAcrossTwoBucketsWithBranchAndBackEdge_whenGetPathDetails_thenTreeAndMetricsAreMerged() throws Exception {
        // Day 1: A -> B -> C.
        TraceGroup day1 = group(DAY, 10, 1, 0, 2, 100, 30,
                List.of(node(A, List.of(), List.of(B)), node(B, List.of(A), List.of(C)), node(C, List.of(B), List.of())),
                Map.of(A.toString(), metric(10, 10, 0), B.toString(), metric(20, 10, 1), C.toString(), metric(30, 10, 0)));
        // Day 2: A -> D, a back edge C -> A, and a cycle X <-> Y that nothing else reaches.
        TraceGroup day2 = group(2 * DAY, 5, 0, 1, 1, 50, 40,
                List.of(node(A, List.of(C), List.of(D)), node(D, List.of(A), List.of()), node(C, List.of(), List.of(A)),
                        node(X, List.of(Y), List.of(Y)), node(Y, List.of(X), List.of(X))),
                Map.of(A.toString(), metric(5, 5, 0), D.toString(), metric(7, 5, 2)));
        when(traceDao.findTraceGroupsByPathId(TENANT_ID.getId(), PATH_ID, DAY, 3 * DAY)).thenReturn(List.of(day1, day2));

        TracePathDetailsDTO details = service.getPathDetails(TENANT_ID, PATH_ID, DAY, 3 * DAY);

        assertThat(details.getProcessedTraces()).isEqualTo(15);
        assertThat(details.getProcessedTracesWithError()).isEqualTo(1);
        assertThat(details.getProcessedTracesWithTimeout()).isEqualTo(1);
        assertThat(details.getStoredTraces()).isEqualTo(3);
        assertThat(details.getTotalTime()).isEqualTo(150);
        assertThat(details.getMaxTime()).isEqualTo(40);
        assertThat(details.getErrorCount()).isEqualTo(3);

        // Every node has an incoming edge, so roots come from the orphan pass: A (first seen), then the X <-> Y cycle.
        List<RuleNodeTreeNodeDTO> roots = details.getRuleNodeTree();
        assertThat(roots).extracting(RuleNodeTreeNodeDTO::getRuleNodeId).containsExactly(A.toString(), X.toString());

        RuleNodeTreeNodeDTO a = roots.get(0);
        assertThat(a.getChildren()).extracting(RuleNodeTreeNodeDTO::getRuleNodeId).containsExactlyInAnyOrder(B.toString(), D.toString());
        RuleNodeTreeNodeDTO b = child(a, B);
        assertThat(b.getChildren()).extracting(RuleNodeTreeNodeDTO::getRuleNodeId).containsExactly(C.toString());
        // The back edge C -> A is cut: A is already placed.
        assertThat(b.getChildren().get(0).getChildren()).isEmpty();
        assertThat(child(a, D).getChildren()).isEmpty();
        assertThat(roots.get(1).getChildren()).extracting(RuleNodeTreeNodeDTO::getRuleNodeId).containsExactly(Y.toString());

        // Metrics are summed across buckets and attached by rule node id.
        assertThat(a.getTotalTime()).isEqualTo(15);
        assertThat(a.getTotalCount()).isEqualTo(15);
        assertThat(b.getErrors()).isEqualTo(1);
        assertThat(child(a, D).getErrors()).isEqualTo(2);
        assertThat(roots.get(1).getTotalCount()).isZero();
    }

    private static RuleNodeTreeNodeDTO child(RuleNodeTreeNodeDTO parent, UUID id) {
        return parent.getChildren().stream().filter(c -> c.getRuleNodeId().equals(id.toString())).findFirst().orElseThrow();
    }

    private static TraceGroup group(long bucketTime, long processed, long withError, long withTimeout, long stored,
                                    long totalTime, long maxTime, List<VisitedRuleNode> nodes,
                                    Map<String, RuleNodePathMetric> metrics) {
        TraceGroup group = new TraceGroup();
        group.setBucketTime(bucketTime);
        group.setTenantId(TENANT_ID.getId());
        group.setPathId(PATH_ID);
        group.setProcessedTraces(processed);
        group.setProcessedTracesWithError(withError);
        group.setProcessedTracesWithTimeout(withTimeout);
        group.setStoredTraces(stored);
        group.setTotalTime(totalTime);
        group.setMaxTime(maxTime);
        group.setVisitedRuleNodes(nodes);
        group.setRuleNodePathMetrics(new HashMap<>(metrics));
        return group;
    }

    private static VisitedRuleNode node(UUID id, List<UUID> from, List<UUID> to) {
        return new VisitedRuleNode(QUEUE, CHAIN, id, from, to);
    }

    private static RuleNodePathMetric metric(long totalTime, long totalCount, long errors) {
        return new RuleNodePathMetric(totalTime, totalCount, errors, CHAIN);
    }

}
