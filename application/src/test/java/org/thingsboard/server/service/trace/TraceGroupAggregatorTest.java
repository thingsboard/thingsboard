// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.trace;

import org.junit.jupiter.api.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.dao.model.sql.ruleenginemonitoring.BufferedSpan;
import org.thingsboard.server.dao.model.sql.ruleenginemonitoring.TraceGroup;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class TraceGroupAggregatorTest {

    private static final UUID TENANT_ID = UUID.fromString("00000000-0000-0000-0000-0000000000aa");
    private static final UUID QUEUE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID RULE_CHAIN_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID NODE_1 = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID NODE_2 = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID NODE_3 = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final long BUCKET_TIME = 86_400_000L;
    private static final long NOW = BUCKET_TIME + 1_000L;

    private final TraceGroupAggregator aggregator = new TraceGroupAggregator();

    @Test
    void givenTracesOnTheSamePath_whenAggregate_thenOneGroupSumsTheirMetrics() {
        BufferedTrace first = trace("trace-1", span("a", null, NODE_1, 0, 10), span("b", "a", NODE_2, 10, 20));
        BufferedTrace second = trace("trace-2", span("a", null, NODE_1, 0, 10), span("b", "a", NODE_2, 10, 30));

        Map<TraceGroupKey, TraceGroupContribution> result = aggregator.aggregate(withPaths(first, second), BUCKET_TIME, NOW);

        assertThat(result).hasSize(1);
        TraceGroupContribution contribution = result.values().iterator().next();
        TraceGroup metrics = contribution.metrics();
        assertThat(metrics.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(metrics.getBucketTime()).isEqualTo(BUCKET_TIME);
        assertThat(metrics.getLastObserved()).isEqualTo(NOW);
        assertThat(metrics.getProcessedTraces()).isEqualTo(2);
        assertThat(metrics.getTotalTime()).isEqualTo(20 + 30);
        assertThat(metrics.getMaxTime()).isEqualTo(30);
        assertThat(metrics.getProcessedTracesWithError()).isZero();
        assertThat(metrics.getProcessedTracesWithTimeout()).isZero();
        assertThat(metrics.getRuleNodePathMetrics().get(NODE_1.toString()).getTotalCount()).isEqualTo(2);
        assertThat(metrics.getRuleNodePathMetrics().get(NODE_2.toString()).getTotalCount()).isEqualTo(2);
        assertThat(contribution.sampleCandidateTraceId()).isIn("trace-1", "trace-2");
    }

    @Test
    void givenErrorAndTimeoutTraces_whenAggregate_thenEachIsCountedOnce() {
        BufferedSpan failed = span("b", "a", NODE_2, 10, 20);
        failed.setStatusCode("ERROR");
        BufferedTrace withError = trace("trace-1", span("a", null, NODE_1, 0, 10), failed);
        // The timeout span is not part of the path, so this trace lands in the same group.
        BufferedSpan timeout = span("t", "a", UUID.randomUUID(), 20, 21);
        timeout.setName(TraceUtils.QUEUE_TIMEOUT_SPAN_NAME);
        BufferedTrace withTimeout = trace("trace-2", span("a", null, NODE_1, 0, 10), span("b", "a", NODE_2, 10, 20), timeout);
        BufferedTrace clean = trace("trace-3", span("a", null, NODE_1, 0, 10), span("b", "a", NODE_2, 10, 20));

        Map<TraceGroupKey, TraceGroupContribution> result =
                aggregator.aggregate(withPaths(withError, withTimeout, clean), BUCKET_TIME, NOW);

        assertThat(result).hasSize(1);
        TraceGroup metrics = result.values().iterator().next().metrics();
        assertThat(metrics.getProcessedTraces()).isEqualTo(3);
        assertThat(metrics.getProcessedTracesWithError()).isEqualTo(1);
        assertThat(metrics.getProcessedTracesWithTimeout()).isEqualTo(1);
    }

    @Test
    void givenTracesOnDifferentPaths_whenAggregate_thenOneGroupPerPath() {
        BufferedTrace first = trace("trace-1", span("a", null, NODE_1, 0, 10), span("b", "a", NODE_2, 10, 20));
        BufferedTrace second = trace("trace-2", span("a", null, NODE_1, 0, 10), span("c", "a", NODE_3, 10, 20));

        Map<TraceGroupKey, TraceGroupContribution> result = aggregator.aggregate(withPaths(first, second), BUCKET_TIME, NOW);

        assertThat(result).hasSize(2);
        assertThat(result.values()).allSatisfy(contribution -> {
            assertThat(contribution.metrics().getProcessedTraces()).isEqualTo(1);
            assertThat(contribution.sampleCandidateTraceId()).isNotNull();
        });
    }

    @Test
    void givenTraceWithoutRuleNodeSpans_whenAggregate_thenItIsSkipped() {
        BufferedTrace unattributed = trace("trace-1", span("s", null, null, 0, 0));

        assertThat(aggregator.aggregate(withPaths(unattributed), BUCKET_TIME, NOW)).isEmpty();
    }

    // As the aggregation job does: a trace without a buildable path is skipped.
    private static List<TraceWithPath> withPaths(BufferedTrace... traces) {
        TracePathBuilder pathBuilder = new TracePathBuilder();
        return Stream.of(traces)
                .flatMap(trace -> pathBuilder.build(trace).map(path -> new TraceWithPath(trace, path)).stream())
                .toList();
    }

    private static BufferedTrace trace(String traceId, BufferedSpan... spans) {
        return new BufferedTrace(TENANT_ID, traceId, false, new LinkedHashSet<>(List.of(spans)));
    }

    private static BufferedSpan span(String spanId, String parentSpanId, UUID ruleNodeId, long start, long end) {
        BufferedSpan span = new BufferedSpan();
        span.setSpanId(spanId);
        span.setParentSpanId(parentSpanId);
        span.setName("[Root Rule Chain] [node]");
        span.setStartTime(start);
        span.setEndTime(end);
        span.setStatusCode("OK");
        span.setAttributesJsonb(JacksonUtil.newObjectNode()
                .put("queue.id", QUEUE_ID.toString())
                .put("rule.chain.id", RULE_CHAIN_ID.toString())
                .put("rule.node.id", ruleNodeId != null ? ruleNodeId.toString() : null)
                .put("message.type", "POST_TELEMETRY_REQUEST"));
        return span;
    }

}
