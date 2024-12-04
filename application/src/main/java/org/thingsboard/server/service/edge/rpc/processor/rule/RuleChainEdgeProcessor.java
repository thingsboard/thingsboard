/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
package org.thingsboard.server.service.edge.rpc.processor.rule;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.gen.edge.v1.RuleChainMetadataUpdateMsg;
import org.thingsboard.server.gen.edge.v1.RuleChainUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.rpc.constructor.rule.RuleChainMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.rule.RuleChainMsgConstructorFactory;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

import static org.thingsboard.server.dao.edge.EdgeServiceImpl.EDGE_IS_ROOT_BODY_KEY;

@Slf4j
@Component
@TbCoreComponent
public class RuleChainEdgeProcessor extends BaseEdgeProcessor {

    @Autowired
    private RuleChainMsgConstructorFactory ruleChainMsgConstructorFactory;

    public DownlinkMsg convertRuleChainEventToDownlink(EdgeEvent edgeEvent, EdgeVersion edgeVersion) {
        RuleChainId ruleChainId = new RuleChainId(edgeEvent.getEntityId());
        DownlinkMsg downlinkMsg = null;
        var msgConstructor = (RuleChainMsgConstructor) ruleChainMsgConstructorFactory.getMsgConstructorByEdgeVersion(edgeVersion);
        switch (edgeEvent.getAction()) {
            case ADDED, UPDATED, ASSIGNED_TO_EDGE -> {
                RuleChain ruleChain = edgeCtx.getRuleChainService().findRuleChainById(edgeEvent.getTenantId(), ruleChainId);
                if (ruleChain != null) {
                    boolean isRoot = false;
                    if (edgeEvent.getBody() != null && edgeEvent.getBody().get(EDGE_IS_ROOT_BODY_KEY) != null) {
                        try {
                            isRoot = Boolean.parseBoolean(edgeEvent.getBody().get(EDGE_IS_ROOT_BODY_KEY).asText());
                        } catch (Exception ignored) {
                        }
                    }
                    if (!isRoot) {
                        Edge edge = edgeCtx.getEdgeService().findEdgeById(edgeEvent.getTenantId(), edgeEvent.getEdgeId());
                        isRoot = edge.getRootRuleChainId().equals(ruleChainId);
                    }
                    UpdateMsgType msgType = getUpdateMsgType(edgeEvent.getAction());
                    RuleChainUpdateMsg ruleChainUpdateMsg = msgConstructor.constructRuleChainUpdatedMsg(msgType, ruleChain, isRoot);

                    DownlinkMsg.Builder builder = DownlinkMsg.newBuilder()
                            .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                            .addRuleChainUpdateMsg(ruleChainUpdateMsg);

                    RuleChainMetaData ruleChainMetaData = edgeCtx.getRuleChainService().loadRuleChainMetaData(edgeEvent.getTenantId(), ruleChainId);
                    RuleChainMetadataUpdateMsg ruleChainMetadataUpdateMsg = msgConstructor
                            .constructRuleChainMetadataUpdatedMsg(edgeEvent.getTenantId(), msgType, ruleChainMetaData, edgeVersion);
                    if (ruleChainMetadataUpdateMsg != null) {
                        builder.addRuleChainMetadataUpdateMsg(ruleChainMetadataUpdateMsg);
                    }
                    downlinkMsg = builder.build();
                }
            }
            case DELETED, UNASSIGNED_FROM_EDGE -> downlinkMsg = DownlinkMsg.newBuilder()
                    .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                    .addRuleChainUpdateMsg(msgConstructor.constructRuleChainDeleteMsg(ruleChainId))
                    .build();
        }
        return downlinkMsg;
    }

    public DownlinkMsg convertRuleChainMetadataEventToDownlink(EdgeEvent edgeEvent, EdgeVersion edgeVersion) {
        RuleChainId ruleChainId = new RuleChainId(edgeEvent.getEntityId());
        RuleChain ruleChain = edgeCtx.getRuleChainService().findRuleChainById(edgeEvent.getTenantId(), ruleChainId);
        var msgConstructor = (RuleChainMsgConstructor) ruleChainMsgConstructorFactory.getMsgConstructorByEdgeVersion(edgeVersion);
        if (ruleChain != null) {
            RuleChainMetaData ruleChainMetaData = edgeCtx.getRuleChainService().loadRuleChainMetaData(edgeEvent.getTenantId(), ruleChainId);
            UpdateMsgType msgType = getUpdateMsgType(edgeEvent.getAction());
            RuleChainMetadataUpdateMsg ruleChainMetadataUpdateMsg = msgConstructor
                    .constructRuleChainMetadataUpdatedMsg(edgeEvent.getTenantId(), msgType, ruleChainMetaData, edgeVersion);
            if (ruleChainMetadataUpdateMsg != null) {
                return DownlinkMsg.newBuilder()
                        .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                        .addRuleChainMetadataUpdateMsg(ruleChainMetadataUpdateMsg)
                        .build();
            }
        }
        return null;
    }

}
