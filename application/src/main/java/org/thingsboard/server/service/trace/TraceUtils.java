/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2026 ThingsBoard, Inc. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of ThingsBoard, Inc. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to ThingsBoard, Inc.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 *
 * Dissemination of this information or reproduction of this material is strictly forbidden
 * unless prior written permission is obtained from COMPANY.
 *
 * Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
 * managers or contractors who have executed Confidentiality and Non-disclosure agreements
 * explicitly covering such access.
 *
 * The copyright notice above does not evidence any actual or intended publication
 * or disclosure  of  this source code, which includes
 * information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
 * ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
 * OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
 * THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
 * AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
 * THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
 * DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
 * OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
 */
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
