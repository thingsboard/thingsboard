/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.server.service.edge.rpc.constructor.rule;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.dao.queue.QueueService;
import org.thingsboard.server.gen.edge.v1.RuleChainMetadataUpdateMsg;

import java.util.List;
import java.util.TreeSet;

@Slf4j
public class RuleChainMetadataConstructorV333 extends AbstractRuleChainMetadataConstructor {

    public RuleChainMetadataConstructorV333(QueueService queueService) {
        super(queueService);
    }

    @Override
    protected void constructRuleChainMetadataUpdatedMsg(TenantId tenantId,
                                                        RuleChainMetadataUpdateMsg.Builder builder,
                                                        RuleChainMetaData ruleChainMetaData) throws JsonProcessingException {
        List<RuleNode> nodes = updateCheckpointNodesConfiguration(tenantId, ruleChainMetaData.getNodes());
        builder.addAllNodes(constructNodes(nodes))
                .addAllConnections(constructConnections(ruleChainMetaData.getConnections()))
                .addAllRuleChainConnections(constructRuleChainConnections(ruleChainMetaData.getRuleChainConnections(), new TreeSet<>()));
        if (ruleChainMetaData.getFirstNodeIndex() != null) {
            builder.setFirstNodeIndex(ruleChainMetaData.getFirstNodeIndex());
        } else {
            builder.setFirstNodeIndex(-1);
        }
    }
}
