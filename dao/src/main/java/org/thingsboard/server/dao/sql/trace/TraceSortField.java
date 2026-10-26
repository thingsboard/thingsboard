// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.trace;

/**
 * Sort columns of the trace search, each with the trace_summary (alias ts) expression it orders by.
 */
public enum TraceSortField {
    START_TIME("ts.start_time", false),
    END_TIME("ts.end_time", false),
    FIRST_SPAN_NAME("(SELECT sp.name FROM span sp WHERE sp.trace_id = ts.trace_id AND sp.tenant_id = ts.tenant_id " +
            "ORDER BY sp.start_time ASC LIMIT 1)", false),
    DURATION("ts.duration", false),
    SPAN_COUNT("ts.span_count", false),
    ERROR_COUNT("ts.error_count", false),
    RULE_NODE_COUNT("COALESCE(jsonb_array_length(ts.rule_node_ids_jsonb), 0)", false),
    CREATED_TIME("ts.created_time", false),
    TOTAL_SPAN_TIME("ts.total_span_time", true),
    IN_QUEUE_TIME("ts.in_queue_time", true);

    private final String expression;
    private final boolean nullsLast;

    TraceSortField(String expression, boolean nullsLast) {
        this.expression = expression;
        this.nullsLast = nullsLast;
    }

    // trace_id breaks ties so pages don't overlap.
    public String orderBy(SortOrder order) {
        return "ORDER BY " + expression + " " + order.name() + (nullsLast ? " NULLS LAST" : "") + ", ts.trace_id ASC ";
    }
}
