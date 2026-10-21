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
