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

@Component
public class TraceSummaryBuilder {
    private static final String ORIGINATOR_ID_ATTR = "originator.id";
    private static final String QUEUE_NAME_ATTR = "queue.name";
    private static final String RULE_CHAIN_ID_ATTR = "rule.chain.id";
    private static final String RULE_NODE_ID_ATTR = "rule.node.id";
    private static final String MESSAGE_TYPE_ATTR = "message.type";
    private static final String MESSAGE_ID_ATTR = "message.id";
    private static final String QUEUE_TIMEOUT_ATTR = "queue.cancel.time.out.messages";

    private final TracePathBuilder tracePathBuilder;

    public TraceSummaryBuilder(TracePathBuilder tracePathBuilder) {
        this.tracePathBuilder = tracePathBuilder;
    }

    public Map<String, TraceSummaryInfo> buildByTraceId(List<LoadedTrace> loaded) {
        Map<String, TraceSummaryInfo> result = new HashMap<>();
        if (CollectionUtils.isEmpty(loaded)) {
            return result;
        }
        for (LoadedTrace loadedTrace : loaded) {
            if (loadedTrace == null || loadedTrace.trace() == null
                    || loadedTrace.trace().tenantId() == null
                    || CollectionUtils.isEmpty(loadedTrace.trace().spans())) {
                continue;
            }
            TracePath path = tracePathBuilder.build(loadedTrace.trace()).orElse(null);
            if (path == null) {
                continue;
            }
            result.put(path.traceId(), build(loadedTrace.trace(), path));
        }
        return result;
    }

    TraceSummaryInfo build(BufferedTrace trace, TracePath path) {
        Collection<BufferedSpan> spans = trace.spans();
        long startTime = spans.stream().mapToLong(TraceSummaryBuilder::spanStartTime).min().orElse(0L);
        long endTime = spans.stream().mapToLong(TraceSummaryBuilder::spanEndTime).max().orElse(startTime);
        int errorCount = (int) spans.stream().filter(TraceSummaryBuilder::isErrorSpan).count();
        long createdTime = spans.stream().mapToLong(BufferedSpan::getCreatedTime).min().orElse(0L);
        String firstSpanName = spans.stream()
                .min(Comparator.comparingLong(TraceSummaryBuilder::spanStartTime)
                        .thenComparingLong(BufferedSpan::getCreatedTime))
                .map(BufferedSpan::getName)
                .orElse(null);
        long totalSpanTime = spans.stream().mapToLong(TraceSummaryBuilder::spanDuration).sum();
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
                distinctAttributeValues(spans, ORIGINATOR_ID_ATTR),
                distinctAttributeValues(spans, QUEUE_NAME_ATTR),
                distinctAttributeValues(spans, RULE_CHAIN_ID_ATTR),
                distinctAttributeValues(spans, RULE_NODE_ID_ATTR),
                distinctAttributeValues(spans, MESSAGE_TYPE_ATTR),
                distinctAttributeValues(spans, MESSAGE_ID_ATTR),
                errorCount > 0,
                spans.stream().anyMatch(span -> hasAttribute(span, QUEUE_TIMEOUT_ATTR)));
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
                .map(span -> spanString(span, attributeKey))
                .filter(TraceSummaryBuilder::hasText)
                .collect(Collectors.toCollection(TreeSet::new))
                .stream()
                .toList();
    }

    private static boolean isErrorSpan(BufferedSpan span) {
        return "ERROR".equals(span.getStatusCode());
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

    private static long spanDuration(BufferedSpan span) {
        return Math.max(spanEndTime(span) - spanStartTime(span), 0L);
    }

    private static String spanString(BufferedSpan span, String attributeKey) {
        JsonNode attributes = span.getAttributesJsonb();
        JsonNode value = attributes != null ? attributes.get(attributeKey) : null;
        return value != null && !value.isNull() ? value.asText(null) : null;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
