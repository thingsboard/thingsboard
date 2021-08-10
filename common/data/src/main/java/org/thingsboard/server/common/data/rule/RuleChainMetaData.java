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
package org.thingsboard.server.common.data.rule;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.id.RuleChainId;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by igor on 3/13/18.
 */
@Builder
@Data
@NoArgsConstructor
public class RuleChainMetaData {

    private RuleChainId ruleChainId;

    private Integer firstNodeIndex;

    private List<RuleNode> nodes;

    private List<NodeConnectionInfo> connections;

    private List<RuleChainConnectionInfo> ruleChainConnections;

    public RuleChainMetaData addConnectionInfo(int fromIndex, int toIndex, String type) {
        NodeConnectionInfo connectionInfo = new NodeConnectionInfo();
        connectionInfo.setFromIndex(fromIndex);
        connectionInfo.setToIndex(toIndex);
        connectionInfo.setType(type);
        if (connections == null) {
            connections = new ArrayList<>();
        }
        connections.add(connectionInfo);
        return this;
    }
    public RuleChainMetaData addRuleChainConnectionInfo(int fromIndex, RuleChainId targetRuleChainId, String type, JsonNode additionalInfo) {
        RuleChainConnectionInfo connectionInfo = new RuleChainConnectionInfo();
        connectionInfo.setFromIndex(fromIndex);
        connectionInfo.setTargetRuleChainId(targetRuleChainId);
        connectionInfo.setType(type);
        connectionInfo.setAdditionalInfo(additionalInfo);
        if (ruleChainConnections == null) {
            ruleChainConnections = new ArrayList<>();
        }
        ruleChainConnections.add(connectionInfo);
        return this;
    }
}
