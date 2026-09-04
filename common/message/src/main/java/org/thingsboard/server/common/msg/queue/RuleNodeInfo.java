// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.msg.queue;

import lombok.Getter;
import org.thingsboard.server.common.data.id.RuleNodeId;

public class RuleNodeInfo {
    private final String label;
    @Getter
    private final RuleNodeId ruleNodeId;

    public RuleNodeInfo(RuleNodeId id, String ruleChainName, String ruleNodeName) {
        this.ruleNodeId = id;
        this.label = "[RuleChain: " + ruleChainName + "|RuleNode: " + ruleNodeName + "(" + id + ")]";
    }

    @Override
    public String toString() {
        return label;
    }
}