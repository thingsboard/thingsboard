// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.trace;

import com.fasterxml.jackson.databind.JsonNode;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.dao.model.sql.ruleenginemonitoring.BufferedSpan;

public final class TraceUtils {
    // Span names the rule engine produces itself, rather than one per rule node.
    public static final String QUEUE_TIMEOUT_SPAN_NAME = "Queue Message Processing Time Out";
    public static final String TRACE_MESSAGE_SNAPSHOT_SPAN_NAME = "Trace Message Snapshot";

    private TraceUtils() {
    }

    public static String buildTraceparent(SpanContext ctx) {
        return buildTraceparent(ctx.getTraceId(), ctx.getSpanId(), ctx.getTraceFlags().asHex());
    }

    public static String buildTraceparent(String traceId, String spanId, String flags) {
        return "00-" + traceId + "-" + spanId + "-" + flags;
    }

    public static SpanContext parseRemoteParentSpanContext(String traceparent) {
        if (traceparent == null) {
            return null;
        }
        String[] parts = traceparent.split("-", 4);
        if (parts.length < 4) {
            return null;
        }
        try {
            return SpanContext.createFromRemoteParent(
                    parts[1], parts[2], TraceFlags.fromHex(parts[3], 0), TraceState.getDefault());
        } catch (Exception e) {
            return null;
        }
    }

    public static String extractSpanIdFromTraceparent(String traceparent) {
        if (traceparent == null) {
            return null;
        }
        String[] parts = traceparent.split("-", 4);
        if (parts.length < 4) {
            return null;
        }
        return parts[2];
    }

    public static String buildSpanName(String ruleChainName, RuleNode ruleNode) {
        return "[" + ruleChainName + "] [" + ruleNode.getName() + "]";
    }

    public static boolean isErrorSpan(BufferedSpan span) {
        return StatusCode.ERROR.name().equals(span.getStatusCode());
    }

    /**
     * Duration in ms, or 0 when the span lacks a start or an end time.
     */
    public static long spanDuration(BufferedSpan span) {
        Long start = span.getStartTime();
        Long end = span.getEndTime();
        return start != null && end != null ? Math.max(end - start, 0L) : 0L;
    }

    /**
     * The attribute as text, or null when it is missing, JSON null or empty.
     */
    public static String spanString(BufferedSpan span, String attributeKey) {
        JsonNode attributes = span.getAttributesJsonb();
        JsonNode value = attributes != null ? attributes.get(attributeKey) : null;
        if (value == null || value.isNull()) {
            return null;
        }
        String text = value.asText();
        return text.isEmpty() ? null : text;
    }
}
