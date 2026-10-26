// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.ruleenginemonitoring;

public enum RuleNodeDimension {
    QUEUE_TENANT_ID("queue_tenant_id"),
    TENANT_ID("tenant_id"),
    SERVICE_ID("service_id"),
    QUEUE_ID("queue_id"),
    RULE_CHAIN_ID("rule_chain_id"),
    RULE_NODE_ID("rule_node_id"),
    LAST_VISITED_RULE_NODE_ID("last_visited_rule_node_id");

    public final String column;

    RuleNodeDimension(String column) {
        this.column = column;
    }
}
