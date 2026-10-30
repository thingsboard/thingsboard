// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.trace;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Component;
import org.thingsboard.server.dao.model.sql.ruleenginemonitoring.BufferedSpan;
import org.thingsboard.server.dao.model.sql.ruleenginemonitoring.TraceGroup;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.thingsboard.server.common.data.trace.TraceSpanAttributes.MESSAGE_TYPE;
import static org.thingsboard.server.common.data.trace.TraceSpanAttributes.QUEUE_ID;
import static org.thingsboard.server.common.data.trace.TraceSpanAttributes.RULE_CHAIN_ID;
import static org.thingsboard.server.common.data.trace.TraceSpanAttributes.RULE_NODE_ID;
import static org.thingsboard.server.service.trace.TraceUtils.spanUuid;

@Slf4j
@Component
public class TracePathBuilder {

    public Optional<TracePath> build(BufferedTrace trace) {
        if (trace == null || CollectionUtils.isEmpty(trace.spans())) {
            return Optional.empty();
        }

        List<BufferedSpan> pathSpans = trace.spans().stream()
                .filter(TraceUtils::isRuleNodeSpan)
                .toList();
        if (pathSpans.isEmpty()) {
            log.warn("Trace [{}] has no rule-node spans after filtering; skipping", trace.traceId());
            return Optional.empty();
        }

        Map<String, BufferedSpan> spansById = pathSpans.stream()
                .collect(Collectors.toMap(BufferedSpan::getSpanId, Function.identity(), (a, b) -> a));
        List<BufferedSpan> initialSpans = pathSpans.stream()
                .filter(span -> !spansById.containsKey(span.getParentSpanId()))
                .toList();
        if (initialSpans.isEmpty()) {
            log.warn("Trace [{}] has no initial span; skipping", trace.traceId());
            return Optional.empty();
        }

        BufferedSpan firstInitialSpan = initialSpans.get(0);
        UUID rootQueueId = spanUuid(firstInitialSpan, QUEUE_ID);
        UUID rootRuleChainId = spanUuid(firstInitialSpan, RULE_CHAIN_ID);
        String messageType = TraceUtils.spanString(firstInitialSpan, MESSAGE_TYPE);

        boolean sameInitialContext = initialSpans.stream().allMatch(span ->
                Objects.equals(rootQueueId, spanUuid(span, QUEUE_ID))
                        && Objects.equals(rootRuleChainId, spanUuid(span, RULE_CHAIN_ID))
                        && Objects.equals(messageType, TraceUtils.spanString(span, MESSAGE_TYPE)));
        if (!sameInitialContext) {
            log.warn("Trace [{}] has inconsistent initial context across initial spans; skipping", trace.traceId());
            return Optional.empty();
        }

        Set<VisitedRuleNode> visitedRuleNodes = pathSpans.stream()
                .map(span -> new VisitedRuleNode(
                        spanUuid(span, QUEUE_ID),
                        spanUuid(span, RULE_CHAIN_ID),
                        spanUuid(span, RULE_NODE_ID)))
                .collect(Collectors.toSet());

        String pathId = computePathId(rootQueueId, rootRuleChainId, messageType, visitedRuleNodes);

        Map<String, TraceGroup.RuleNodePathMetric> ruleNodePathMetrics = computeRuleNodePathMetrics(pathSpans);
        long totalRuleNodeProcessingTime = ruleNodePathMetrics.values().stream()
                .mapToLong(TraceGroup.RuleNodePathMetric::getTotalTime)
                .sum();
        long totalInQueueTime = computeInQueueTime(pathSpans);

        Map<UUID, Set<UUID>> incomingConnections = new HashMap<>();
        Map<UUID, Set<UUID>> outgoingConnections = new HashMap<>();
        for (BufferedSpan span : pathSpans) {
            UUID nodeId = spanUuid(span, RULE_NODE_ID);
            BufferedSpan parent = span.getParentSpanId() != null ? spansById.get(span.getParentSpanId()) : null;
            if (nodeId == null || parent == null) {
                continue;
            }
            UUID parentNodeId = spanUuid(parent, RULE_NODE_ID);
            if (parentNodeId == null || parentNodeId.equals(nodeId)) {
                continue;
            }
            outgoingConnections.computeIfAbsent(parentNodeId, k -> new HashSet<>()).add(nodeId);
            incomingConnections.computeIfAbsent(nodeId, k -> new HashSet<>()).add(parentNodeId);
        }

        return Optional.of(new TracePath(pathId, trace.tenantId(), trace.traceId(), rootQueueId, messageType,
                rootRuleChainId, visitedRuleNodes, ruleNodePathMetrics,
                totalInQueueTime, totalRuleNodeProcessingTime, incomingConnections, outgoingConnections));
    }

    private Map<String, TraceGroup.RuleNodePathMetric> computeRuleNodePathMetrics(List<BufferedSpan> pathSpans) {
        Map<String, TraceGroup.RuleNodePathMetric> metrics = new HashMap<>();
        for (BufferedSpan span : pathSpans) {
            UUID ruleNodeId = spanUuid(span, RULE_NODE_ID);
            if (ruleNodeId == null) {
                continue;
            }
            TraceGroup.RuleNodePathMetric metric = metrics.computeIfAbsent(ruleNodeId.toString(),
                    k -> new TraceGroup.RuleNodePathMetric(0L, 0L, 0L, spanUuid(span, RULE_CHAIN_ID)));
            metric.setTotalTime(metric.getTotalTime() + TraceUtils.spanDuration(span));
            metric.setTotalCount(metric.getTotalCount() + 1);
            if (TraceUtils.isErrorSpan(span)) {
                metric.setErrors(metric.getErrors() + 1);
            }
        }
        return metrics;
    }

    private static long computeInQueueTime(List<BufferedSpan> spans) {
        List<long[]> intervals = spans.stream()
                .map(TracePathBuilder::spanInterval)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingLong(interval -> interval[0]))
                .toList();
        if (intervals.isEmpty()) {
            return 0L;
        }
        long envelopeStart = intervals.get(0)[0];
        long envelopeEnd = intervals.stream().mapToLong(interval -> interval[1]).max().orElse(envelopeStart);
        long activeTime = 0L;
        long currentStart = intervals.get(0)[0];
        long currentEnd = intervals.get(0)[1];
        for (int i = 1; i < intervals.size(); i++) {
            long[] interval = intervals.get(i);
            if (interval[0] <= currentEnd) {
                currentEnd = Math.max(currentEnd, interval[1]);
            } else {
                activeTime += Math.max(currentEnd - currentStart, 0L);
                currentStart = interval[0];
                currentEnd = interval[1];
            }
        }
        activeTime += Math.max(currentEnd - currentStart, 0L);
        return Math.max((envelopeEnd - envelopeStart) - activeTime, 0L);
    }

    private static long[] spanInterval(BufferedSpan span) {
        Long start = span.getStartTime();
        Long end = span.getEndTime();
        if (start == null || end == null) {
            return null;
        }
        return new long[]{start, Math.max(end, start)};
    }

    private static String computePathId(UUID rootQueueId, UUID rootRuleChainId, String messageType,
                                        Set<VisitedRuleNode> visitedRuleNodes) {
        List<VisitedRuleNode> sortedNodes = visitedRuleNodes.stream()
                .sorted(Comparator.comparing((VisitedRuleNode n) -> uuidStr(n.ruleNodeId()))
                        .thenComparing(n -> uuidStr(n.ruleChainId()))
                        .thenComparing(n -> uuidStr(n.queueId())))
                .toList();
        StringBuilder canonical = new StringBuilder()
                .append(uuidStr(rootQueueId)).append('|')
                .append(uuidStr(rootRuleChainId)).append('|')
                .append(messageType);
        for (VisitedRuleNode node : sortedNodes) {
            canonical.append('\n')
                    .append(uuidStr(node.queueId())).append(',')
                    .append(uuidStr(node.ruleChainId())).append(',')
                    .append(uuidStr(node.ruleNodeId()));
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonical.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    private static String uuidStr(UUID uuid) {
        return uuid == null ? "" : uuid.toString();
    }
}
