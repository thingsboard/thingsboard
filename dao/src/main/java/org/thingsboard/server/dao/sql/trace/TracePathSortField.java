// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.trace;

/**
 * Sort columns of the execution path list, each with the aggregated column it orders by.
 */
public enum TracePathSortField {
    TRACES_COUNT("processed_traces"),
    STORED_TRACES("stored_traces"),
    TIMEOUT_TRACES("processed_traces_with_timeout"),
    ERROR_TRACES("processed_traces_with_error"),
    LAST_OBSERVED("last_observed"),
    AVG_TIME("avg_time"),
    AVG_IN_QUEUE_TIME("avg_in_queue_time"),
    AVG_RULE_NODE_PROCESSING_TIME("avg_rule_node_processing_time"),
    MAX_TIME("max_time"),
    ERROR_COUNT("error_count"),
    ROOT_QUEUE("root_queue_id"),
    ROOT_MESSAGE_TYPE("root_message_type"),
    ROOT_RULE_CHAIN("root_rule_chain_id");

    private final String column;

    TracePathSortField(String column) {
        this.column = column;
    }

    public String getColumn() {
        return column;
    }
}
