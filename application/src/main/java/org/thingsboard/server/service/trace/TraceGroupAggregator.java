// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.trace;

import org.springframework.stereotype.Component;
import org.thingsboard.server.dao.model.sql.ruleenginemonitoring.TraceGroup;
import org.thingsboard.server.dao.model.sql.ruleenginemonitoring.BufferedSpan;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Component
public class TraceGroupAggregator {

    public Map<TraceGroupKey, TraceGroupContribution> aggregate(Collection<TraceWithPath> traces,
                                                                long bucketTime, long now) {
        Map<TraceGroupKey, Accumulator> accumulators = new HashMap<>();
        for (TraceWithPath traceWithPath : traces) {
            BufferedTrace trace = traceWithPath.trace();
            TracePath path = traceWithPath.path();
            TraceGroupKey key = new TraceGroupKey(path.tenantId(), path.pathId());
            accumulators.computeIfAbsent(key, ignored -> new Accumulator(path, bucketTime, now))
                    .add(path, trace);
        }
        Map<TraceGroupKey, TraceGroupContribution> result = new HashMap<>(accumulators.size());
        accumulators.forEach((key, accumulator) -> result.put(key, accumulator.finish()));
        return result;
    }

    private static final class Accumulator {
        private final long now;
        private final TraceGroup metrics;
        private long seenTraces;
        private String sampledTraceId;

        Accumulator(TracePath firstPath, long bucketTime, long now) {
            this.now = now;
            this.metrics = newMetrics(firstPath, bucketTime, now);
        }

        void add(TracePath path, BufferedTrace trace) {
            seenTraces++;

            if (ThreadLocalRandom.current().nextLong(seenTraces) == 0L) {
                sampledTraceId = path.traceId();
            }
            metrics.setProcessedTraces(metrics.getProcessedTraces() + 1);
            if (hasTimeout(trace)) {
                metrics.setProcessedTracesWithTimeout(metrics.getProcessedTracesWithTimeout() + 1);
            }
            if (hasError(trace)) {
                metrics.setProcessedTracesWithError(metrics.getProcessedTracesWithError() + 1);
            }
            metrics.setLastObserved(now);
            long traceDuration = traceDuration(trace);
            metrics.setTotalTime(metrics.getTotalTime() + traceDuration);
            metrics.setMaxTime(Math.max(metrics.getMaxTime(), traceDuration));
            metrics.setTotalInQueueTime(metrics.getTotalInQueueTime() + path.totalInQueueTime());
            metrics.setTotalRuleNodeProcessingTime(metrics.getTotalRuleNodeProcessingTime() + path.totalRuleNodeProcessingTime());
            TraceGroup.mergeRuleNodePathMetrics(metrics.getRuleNodePathMetrics(), path.ruleNodePathMetrics());
        }

        TraceGroupContribution finish() {
            return new TraceGroupContribution(metrics, sampledTraceId);
        }
    }

    private static TraceGroup newMetrics(TracePath path, long bucketTime, long now) {
        Set<VisitedRuleNode> visited = path.visitedRuleNodes();
        Map<UUID, Set<UUID>> incoming = path.incomingConnections();
        Map<UUID, Set<UUID>> outgoing = path.outgoingConnections();
        List<TraceGroup.VisitedRuleNode> visitedRuleNodes = visited.stream()
                .map(n -> new TraceGroup.VisitedRuleNode(n.queueId(), n.ruleChainId(), n.ruleNodeId(),
                        connectionsOf(incoming, n.ruleNodeId()), connectionsOf(outgoing, n.ruleNodeId())))
                .collect(Collectors.toList());
        TraceGroup group = new TraceGroup();
        group.setBucketTime(bucketTime);
        group.setTenantId(path.tenantId());
        group.setPathId(path.pathId());
        group.setRootQueueId(path.rootQueueId());
        group.setRootMessageType(path.messageType());
        group.setRootRuleChainId(path.rootRuleChainId());
        group.setVisitedRuleNodes(visitedRuleNodes);

        group.setRelatedTraceIds(new ArrayList<>());
        group.setLastObserved(now);
        group.setProcessedTraces(0);
        group.setProcessedTracesWithTimeout(0L);
        group.setProcessedTracesWithError(0L);

        group.setStoredTraces(0);
        group.setTotalTime(0L);
        group.setMaxTime(0L);
        group.setTotalInQueueTime(0L);
        group.setTotalRuleNodeProcessingTime(0L);
        group.setRuleNodePathMetrics(new HashMap<>());
        return group;
    }

    private static long traceDuration(BufferedTrace trace) {
        if (trace == null || trace.spans() == null || trace.spans().isEmpty()) {
            return 0L;
        }
        long start = Long.MAX_VALUE;
        long end = Long.MIN_VALUE;
        for (BufferedSpan span : trace.spans()) {
            Long spanStart = span.getStartTime();
            if (spanStart == null) {
                continue;
            }
            start = Math.min(start, spanStart);
            end = Math.max(end, span.getEndTime() != null ? span.getEndTime() : spanStart);
        }
        return start == Long.MAX_VALUE || end == Long.MIN_VALUE ? 0L : Math.max(end - start, 0L);
    }

    private static boolean hasTimeout(BufferedTrace trace) {
        return trace != null && trace.spans() != null && trace.spans().stream()
                .anyMatch(span -> TraceUtils.QUEUE_TIMEOUT_SPAN_NAME.equals(span.getName()));
    }

    private static boolean hasError(BufferedTrace trace) {
        return trace != null && trace.spans() != null && trace.spans().stream()
                .anyMatch(TraceUtils::isErrorSpan);
    }

    private static List<UUID> connectionsOf(Map<UUID, Set<UUID>> connections, UUID ruleNodeId) {
        Set<UUID> nodes = connections.get(ruleNodeId);
        return nodes == null || nodes.isEmpty() ? new ArrayList<>() : new ArrayList<>(nodes);
    }
}
