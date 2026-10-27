// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.trace;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Component;
import org.thingsboard.server.dao.model.sql.ruleenginemonitoring.BufferedSpan;
import org.thingsboard.server.dao.sql.trace.TraceSummaryInfo;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static org.thingsboard.server.common.data.trace.TraceSpanAttributes.MESSAGE_ID;
import static org.thingsboard.server.common.data.trace.TraceSpanAttributes.MESSAGE_TYPE;
import static org.thingsboard.server.common.data.trace.TraceSpanAttributes.ORIGINATOR_ID;
import static org.thingsboard.server.common.data.trace.TraceSpanAttributes.QUEUE_NAME;
import static org.thingsboard.server.common.data.trace.TraceSpanAttributes.QUEUE_TIMEOUT;
import static org.thingsboard.server.common.data.trace.TraceSpanAttributes.RULE_CHAIN_ID;
import static org.thingsboard.server.common.data.trace.TraceSpanAttributes.RULE_NODE_ID;

@Component
public class TraceSummaryBuilder {

    private final TracePathBuilder tracePathBuilder;

    public TraceSummaryBuilder(TracePathBuilder tracePathBuilder) {
        this.tracePathBuilder = tracePathBuilder;
    }

    public Map<String, TraceSummaryInfo> buildByTraceId(List<BufferedTrace> traces) {
        Map<String, TraceSummaryInfo> result = new HashMap<>();
        if (CollectionUtils.isEmpty(traces)) {
            return result;
        }
        for (BufferedTrace trace : traces) {
            if (trace == null || trace.tenantId() == null || CollectionUtils.isEmpty(trace.spans())) {
                continue;
            }
            TracePath path = tracePathBuilder.build(trace).orElse(null);
            if (path == null) {
                continue;
            }
            result.put(path.traceId(), build(trace, path));
        }
        return result;
    }

    TraceSummaryInfo build(BufferedTrace trace, TracePath path) {
        Collection<BufferedSpan> spans = trace.spans();
        long startTime = spans.stream().mapToLong(TraceSummaryBuilder::spanStartTime).min().orElse(0L);
        long endTime = spans.stream().mapToLong(TraceSummaryBuilder::spanEndTime).max().orElse(startTime);
        int errorCount = (int) spans.stream().filter(TraceUtils::isErrorSpan).count();
        long createdTime = spans.stream().mapToLong(BufferedSpan::getCreatedTime).min().orElse(0L);
        String firstSpanName = spans.stream()
                .min(Comparator.comparingLong(TraceSummaryBuilder::spanStartTime)
                        .thenComparingLong(BufferedSpan::getCreatedTime))
                .map(BufferedSpan::getName)
                .orElse(null);
        long totalSpanTime = spans.stream().mapToLong(TraceUtils::spanDuration).sum();
        return new TraceSummaryInfo(
                trace.traceId(),
                trace.tenantId(),
                distinctServiceNames(spans),
                startTime,
                endTime,
                Math.max(endTime - startTime, 0L),
                spans.size(),
                errorCount,
                createdTime,
                firstSpanName,
                totalSpanTime,
                path.totalInQueueTime(),
                path.rootQueueId(),
                path.messageType(),
                path.rootRuleChainId() != null ? path.rootRuleChainId().toString() : null,
                distinctAttributeValues(spans, ORIGINATOR_ID),
                distinctAttributeValues(spans, QUEUE_NAME),
                distinctAttributeValues(spans, RULE_CHAIN_ID),
                distinctAttributeValues(spans, RULE_NODE_ID),
                distinctAttributeValues(spans, MESSAGE_TYPE),
                distinctAttributeValues(spans, MESSAGE_ID),
                errorCount > 0,
                spans.stream().anyMatch(span -> hasAttribute(span, QUEUE_TIMEOUT)));
    }

    private static List<String> distinctServiceNames(Collection<BufferedSpan> spans) {
        return spans.stream()
                .map(BufferedSpan::getServiceName)
                .filter(TraceSummaryBuilder::hasText)
                .collect(Collectors.toCollection(TreeSet::new))
                .stream()
                .toList();
    }

    private static List<String> distinctAttributeValues(Collection<BufferedSpan> spans, String attributeKey) {
        return spans.stream()
                .map(span -> TraceUtils.spanString(span, attributeKey))
                .filter(TraceSummaryBuilder::hasText)
                .collect(Collectors.toCollection(TreeSet::new))
                .stream()
                .toList();
    }

    private static boolean hasAttribute(BufferedSpan span, String attributeKey) {
        JsonNode attributes = span.getAttributesJsonb();
        JsonNode value = attributes != null ? attributes.get(attributeKey) : null;
        return value != null && !value.isNull();
    }

    private static long spanStartTime(BufferedSpan span) {
        return span.getStartTime() != null ? span.getStartTime() : 0L;
    }

    private static long spanEndTime(BufferedSpan span) {
        Long end = span.getEndTime();
        return end != null ? end : spanStartTime(span);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
