// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.trace;

import org.junit.jupiter.api.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.dao.model.sql.ruleenginemonitoring.BufferedSpan;
import org.thingsboard.server.dao.model.sql.ruleenginemonitoring.TraceGroup.RuleNodePathMetric;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TracePathBuilderTest {

    private static final UUID TENANT_ID = UUID.fromString("00000000-0000-0000-0000-0000000000aa");
    private static final UUID QUEUE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID RULE_CHAIN_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID NODE_1 = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID NODE_2 = UUID.fromString("44444444-4444-4444-4444-444444444444");
    // SHA-256 of "<queue>|<rule chain>|POST_TELEMETRY_REQUEST" plus one "\n<queue>,<rule chain>,<node>" line per
    // visited node, sorted by node id. Stored trace groups are keyed by it, so it must never change by accident.
    private static final String PATH_ID = "f82388f6ba593049828b2925d28485cf11f0ad9c34973d8f20ad4270528a61f7";

    private final TracePathBuilder builder = new TracePathBuilder();

    @Test
    void givenKnownTrace_whenBuild_thenPathIdIsPinned() {
        TracePath path = builder.build(trace(span("a", null, NODE_1, 0, 10), span("b", "a", NODE_2, 10, 20))).orElseThrow();

        assertThat(path.pathId()).isEqualTo(PATH_ID);
        assertThat(path.rootQueueId()).isEqualTo(QUEUE_ID);
        assertThat(path.rootRuleChainId()).isEqualTo(RULE_CHAIN_ID);
        assertThat(path.messageType()).isEqualTo("POST_TELEMETRY_REQUEST");
    }

    @Test
    void givenSameNodesInAnotherOrderOrVisitedTwice_whenBuild_thenSamePathId() {
        TracePath reordered = builder.build(trace(
                span("b", "a", NODE_2, 10, 20), span("a", null, NODE_1, 0, 10), span("c", "b", NODE_2, 20, 25))).orElseThrow();

        assertThat(reordered.pathId()).isEqualTo(PATH_ID);
    }

    @Test
    void givenTimeoutAndUnattributedSpans_whenBuild_thenTheyAreLeftOutOfThePath() {
        BufferedSpan timeout = span("t", "a", UUID.randomUUID(), 10, 11);
        timeout.setName(TraceUtils.QUEUE_TIMEOUT_SPAN_NAME);
        BufferedSpan snapshot = span("s", null, null, 0, 0);
        snapshot.setAttributesJsonb(JacksonUtil.newObjectNode());

        TracePath path = builder.build(trace(span("a", null, NODE_1, 0, 10), span("b", "a", NODE_2, 10, 20),
                timeout, snapshot)).orElseThrow();

        assertThat(path.pathId()).isEqualTo(PATH_ID);
    }

    @Test
    void givenOverlappingAndGappedSpans_whenBuild_thenInQueueTimeIsTheUncoveredPartOfTheEnvelope() {
        // envelope [0, 30]; active [0, 10] and [15, 30] (the second span overlaps the third) -> 5 ms in queue
        TracePath path = builder.build(trace(
                span("a", null, NODE_1, 0, 10),
                span("b", "a", NODE_2, 15, 20),
                span("c", "a", NODE_2, 16, 30))).orElseThrow();

        assertThat(path.totalInQueueTime()).isEqualTo(5);
        assertThat(path.totalRuleNodeProcessingTime()).isEqualTo(10 + 5 + 14);
    }

    @Test
    void givenParentAndChildSpans_whenBuild_thenMetricsAndConnectionsFollowTheRuleNodes() {
        BufferedSpan failed = span("c", "a", NODE_2, 20, 23);
        failed.setStatusCode("ERROR");

        TracePath path = builder.build(trace(span("a", null, NODE_1, 0, 10), span("b", "a", NODE_2, 10, 20), failed))
                .orElseThrow();

        assertMetric(path.ruleNodePathMetrics().get(NODE_1.toString()), 10, 1, 0);
        assertMetric(path.ruleNodePathMetrics().get(NODE_2.toString()), 13, 2, 1);
        assertThat(path.outgoingConnections()).isEqualTo(Map.of(NODE_1, Set.of(NODE_2)));
        assertThat(path.incomingConnections()).isEqualTo(Map.of(NODE_2, Set.of(NODE_1)));
    }

    @Test
    void givenNoRuleNodeSpans_whenBuild_thenNoPath() {
        BufferedSpan snapshot = span("s", null, null, 0, 0);
        snapshot.setAttributesJsonb(JacksonUtil.newObjectNode());

        assertThat(builder.build(trace(snapshot))).isEmpty();
    }

    private static BufferedTrace trace(BufferedSpan... spans) {
        return new BufferedTrace(TENANT_ID, "trace-1", false, new LinkedHashSet<>(List.of(spans)));
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

    private static void assertMetric(RuleNodePathMetric metric, long totalTime, long count, long errors) {
        assertThat(metric.getTotalTime()).isEqualTo(totalTime);
        assertThat(metric.getTotalCount()).isEqualTo(count);
        assertThat(metric.getErrors()).isEqualTo(errors);
        assertThat(metric.getRuleChainId()).isEqualTo(RULE_CHAIN_ID);
    }

}
