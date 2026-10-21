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
