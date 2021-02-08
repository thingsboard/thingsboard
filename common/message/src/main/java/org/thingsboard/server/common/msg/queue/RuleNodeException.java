/**
 * Copyright © 2016-2021 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.common.msg.queue;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.rule.RuleNode;

@Slf4j
public class RuleNodeException extends RuleEngineException {
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
            ruleNodeName = "Unknown";
            ruleChainId = new RuleChainId(RuleChainId.NULL_UUID);
            ruleNodeId = new RuleNodeId(RuleNodeId.NULL_UUID);
        }
    }

    public String toJsonString() {
        try {
            return mapper.writeValueAsString(mapper.createObjectNode()
                    .put("ruleNodeId", ruleNodeId.toString())
                    .put("ruleChainId", ruleChainId.toString())
                    .put("ruleNodeName", ruleNodeName)
                    .put("ruleChainName", ruleChainName)
                    .put("message", getMessage()));
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize exception ", e);
            throw new RuntimeException(e);
        }
    }

}
