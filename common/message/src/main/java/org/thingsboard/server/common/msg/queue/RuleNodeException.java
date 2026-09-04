// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.msg.queue;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.rule.RuleNode;

@Slf4j
public class RuleNodeException extends RuleEngineException {

    private static final long serialVersionUID = -1776681087370749776L;
    public static final String UNKNOWN = "Unknown";

    @Getter
    private final String ruleChainName;
    @Getter
    private final String ruleNodeName;
    @Getter
    private final RuleChainId ruleChainId;
    @Getter
    private final RuleNodeId ruleNodeId;


    public RuleNodeException(String message, String ruleChainName, RuleNode ruleNode) {
        super(message);
        this.ruleChainName = ruleChainName;
        if (ruleNode != null) {
            this.ruleNodeName = ruleNode.getName();
            this.ruleChainId = ruleNode.getRuleChainId();
            this.ruleNodeId = ruleNode.getId();
        } else {
            ruleNodeName = UNKNOWN;
            ruleChainId = new RuleChainId(RuleChainId.NULL_UUID);
            ruleNodeId = new RuleNodeId(RuleNodeId.NULL_UUID);
        }
    }

    public String toJsonString(int maxMessageLength) {
        try {
            return mapper.writeValueAsString(mapper.createObjectNode()
                    .put("ruleNodeId", ruleNodeId.toString())
                    .put("ruleChainId", ruleChainId.toString())
                    .put("ruleNodeName", ruleNodeName)
                    .put("ruleChainName", ruleChainName)
                    .put("message", truncateIfNecessary(getMessage(), maxMessageLength)));
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize exception ", e);
            throw new RuntimeException(e);
        }
    }

}
