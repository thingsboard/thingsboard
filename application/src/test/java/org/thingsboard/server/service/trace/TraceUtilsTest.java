// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.trace;

import org.junit.jupiter.api.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.dao.model.sql.ruleenginemonitoring.BufferedSpan;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The span helpers shared by TraceSummaryBuilder and TracePathBuilder, so the trace summary and the path stats agree.
 */
class TraceUtilsTest {

    @Test
    void givenStatusCodes_whenIsErrorSpan_thenOnlyErrorMatches() {
        assertThat(TraceUtils.isErrorSpan(span("ERROR", 0L, 10L, "{}"))).isTrue();
        assertThat(TraceUtils.isErrorSpan(span("OK", 0L, 10L, "{}"))).isFalse();
        assertThat(TraceUtils.isErrorSpan(span(null, 0L, 10L, "{}"))).isFalse();
    }

    @Test
    void givenMissingStartOrEnd_whenSpanDuration_thenZero() {
        assertThat(TraceUtils.spanDuration(span("OK", 100L, 150L, "{}"))).isEqualTo(50L);
        assertThat(TraceUtils.spanDuration(span("OK", null, 1_700_000_000_000L, "{}"))).isZero();
        assertThat(TraceUtils.spanDuration(span("OK", 100L, null, "{}"))).isZero();
        assertThat(TraceUtils.spanDuration(span("OK", 150L, 100L, "{}"))).isZero();
    }

    @Test
    void givenMissingNullOrEmptyAttribute_whenSpanString_thenNull() {
        BufferedSpan span = span("OK", 0L, 1L, "{\"message.type\":\"POST_TELEMETRY_REQUEST\",\"empty\":\"\",\"nil\":null,\"num\":5}");

        assertThat(TraceUtils.spanString(span, "message.type")).isEqualTo("POST_TELEMETRY_REQUEST");
        assertThat(TraceUtils.spanString(span, "num")).isEqualTo("5");
        assertThat(TraceUtils.spanString(span, "empty")).isNull();
        assertThat(TraceUtils.spanString(span, "nil")).isNull();
        assertThat(TraceUtils.spanString(span, "missing")).isNull();
        assertThat(TraceUtils.spanString(span("OK", 0L, 1L, null), "message.type")).isNull();
    }

    private static BufferedSpan span(String statusCode, Long startTime, Long endTime, String attributesJson) {
        BufferedSpan span = new BufferedSpan();
        span.setStatusCode(statusCode);
        span.setStartTime(startTime);
        span.setEndTime(endTime);
        span.setAttributesJsonb(attributesJson != null ? JacksonUtil.toJsonNode(attributesJson) : null);
        return span;
    }

}
