// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.trace;

/**
 * Keys of the span attributes the rule engine writes and the trace aggregation, search and UI read back.
 */
public final class TraceSpanAttributes {

    public static final String TENANT_ID = "tenant.id";
    public static final String MESSAGE_ID = "message.id";
    public static final String MESSAGE_TYPE = "message.type";
    public static final String ORIGINATOR_ID = "originator.id";
    public static final String QUEUE_ID = "queue.id";
    public static final String QUEUE_NAME = "queue.name";
    public static final String QUEUE_TIMEOUT = "queue.cancel.time.out.messages";
    public static final String RULE_CHAIN_ID = "rule.chain.id";
    public static final String RULE_CHAIN_NAME = "rule.chain.name";
    public static final String RULE_NODE_ID = "rule.node.id";
    public static final String RULE_NODE_MAX_EXECUTIONS = "rule.node.max.executions";
    public static final String INPUT_MSG_DATA = "input.msg.data";
    public static final String INPUT_MSG_METADATA = "input.msg.metadata";
    public static final String OUTPUT_MSG_DATA = "output.msg.data";
    public static final String OUTPUT_MSG_METADATA = "output.msg.metadata";

    private TraceSpanAttributes() {
    }

}
