// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.trace;

import org.junit.jupiter.api.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.dao.model.sql.ruleenginemonitoring.BufferedSpan;
import org.thingsboard.server.dao.sql.trace.TraceSummaryInfo;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.thingsboard.server.common.data.trace.TraceSpanAttributes.INPUT_MSG_DATA;
import static org.thingsboard.server.common.data.trace.TraceSpanAttributes.QUEUE_NAME;
import static org.thingsboard.server.common.data.trace.TraceSpanAttributes.QUEUE_TIMEOUT;

class TraceSummaryBuilderTest {

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID QUEUE_ID = UUID.randomUUID();
    private static final UUID RULE_CHAIN_ID = UUID.randomUUID();

    private final TraceSummaryBuilder builder = new TraceSummaryBuilder();

    @Test
    void givenSnapshotAndQueueTimeoutSpans_whenBuild_thenTimingAndCountsComeFromRuleNodeSpans() {
        BufferedSpan snapshot = new BufferedSpan();
        snapshot.setSpanId("s");
        snapshot.setName(TraceUtils.TRACE_MESSAGE_SNAPSHOT_SPAN_NAME);
        snapshot.setStartTime(0L);
        snapshot.setEndTime(1L);
        snapshot.setStatusCode("OK");
        snapshot.setAttributesJsonb(JacksonUtil.newObjectNode().put(INPUT_MSG_DATA, "{\"temperature\":42}"));
        BufferedSpan first = span("a", null, "[Root Rule Chain] [first]", 10, 20);
        BufferedSpan second = span("b", "a", "[Root Rule Chain] [second]", 20, 30);
        BufferedSpan timeout = new BufferedSpan();
        timeout.setSpanId("t");
        timeout.setName(TraceUtils.QUEUE_TIMEOUT_SPAN_NAME);
        timeout.setStartTime(500L);
        timeout.setEndTime(500L);
        timeout.setStatusCode("OK");
        timeout.setAttributesJsonb(JacksonUtil.newObjectNode().put(QUEUE_TIMEOUT, true).put(QUEUE_NAME, "Main"));

        BufferedTrace trace = new BufferedTrace(TENANT_ID, "trace-1", false, new LinkedHashSet<>(List.of(snapshot, first, second, timeout)));
        TracePath path = new TracePathBuilder().build(trace).orElseThrow();
        TraceSummaryInfo summary = builder.buildByTraceId(List.of(new TraceWithPath(trace, path))).get("trace-1");

        assertThat(summary.firstSpanName()).isEqualTo("[Root Rule Chain] [first]");
        assertThat(summary.startTime()).isEqualTo(10L);
        assertThat(summary.endTime()).isEqualTo(30L);
        assertThat(summary.duration()).isEqualTo(20L);
        assertThat(summary.spanCount()).isEqualTo(2);
        assertThat(summary.totalSpanTime()).isEqualTo(20L);
        assertThat(summary.hasQueueTimeouts()).isTrue();
        assertThat(summary.queueNames()).containsExactly("Main");
    }

    private static BufferedSpan span(String spanId, String parentSpanId, String name, long start, long end) {
        BufferedSpan span = new BufferedSpan();
        span.setSpanId(spanId);
        span.setParentSpanId(parentSpanId);
        span.setName(name);
        span.setStartTime(start);
        span.setEndTime(end);
        span.setStatusCode("OK");
        span.setAttributesJsonb(JacksonUtil.newObjectNode()
                .put("queue.id", QUEUE_ID.toString())
                .put("rule.chain.id", RULE_CHAIN_ID.toString())
                .put("rule.node.id", UUID.randomUUID().toString())
                .put("message.type", "POST_TELEMETRY_REQUEST"));
        return span;
    }

}
