/**
 * Copyright © 2016-2023 The Thingsboard Authors
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

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.gen.edge.v1.RuleChainMetadataUpdateMsg;
import org.thingsboard.server.gen.edge.v1.RuleChainUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.util.UUID;

import static org.thingsboard.server.service.edge.DefaultEdgeNotificationService.EDGE_IS_ROOT_BODY_KEY;

@Component
@Slf4j
@TbCoreComponent
public class RuleChainEdgeProcessor extends BaseRuleChainProcessor {

    public ListenableFuture<Void> processRuleChainMsgFromEdge(TenantId tenantId, Edge edge, RuleChainUpdateMsg ruleChainUpdateMsg) {
        log.trace("[{}] executing processRuleChainMsgFromEdge [{}] from edge [{}]", tenantId, ruleChainUpdateMsg, edge.getName());
        RuleChainId ruleChainId = new RuleChainId(new UUID(ruleChainUpdateMsg.getIdMSB(), ruleChainUpdateMsg.getIdLSB()));
        try {
            edgeSynchronizationManager.getSync().set(true);

            switch (ruleChainUpdateMsg.getMsgType()) {
                case ENTITY_CREATED_RPC_MESSAGE:
                case ENTITY_UPDATED_RPC_MESSAGE:
                    saveOrUpdateRuleChain(tenantId, ruleChainId, ruleChainUpdateMsg, edge);
                    return Futures.immediateFuture(null);
                case ENTITY_DELETED_RPC_MESSAGE:
                    RuleChain ruleChainToDelete = ruleChainService.findRuleChainById(tenantId, ruleChainId);
                    if (ruleChainToDelete != null) {
                        ruleChainService.unassignRuleChainFromEdge(tenantId, ruleChainId, edge.getId(), false);
                    }
                    return Futures.immediateFuture(null);
                case UNRECOGNIZED:
                default:
                    return handleUnsupportedMsgType(ruleChainUpdateMsg.getMsgType());
            }
        } catch (DataValidationException e) {
            if (e.getMessage().contains("limit reached")) {
                log.warn("[{}] Number of allowed rule chains violated {}", tenantId, ruleChainUpdateMsg, e);
                return Futures.immediateFuture(null);
            } else {
                return Futures.immediateFailedFuture(e);
            }
        } finally {
            edgeSynchronizationManager.getSync().remove();
        }
    }


    public ListenableFuture<Void> processRuleChainMetadataMsgFromEdge(TenantId tenantId, Edge edge, RuleChainMetadataUpdateMsg ruleChainMetadataUpdateMsg) {
        log.trace("[{}] executing processRuleChainMetadataMsgFromEdge [{}] from edge [{}]", tenantId, ruleChainMetadataUpdateMsg, edge.getName());
        RuleChainId ruleChainId = new RuleChainId(new UUID(ruleChainMetadataUpdateMsg.getRuleChainIdMSB(), ruleChainMetadataUpdateMsg.getRuleChainIdLSB()));
        try {
            edgeSynchronizationManager.getSync().set(true);

            switch (ruleChainMetadataUpdateMsg.getMsgType()) {
                case ENTITY_CREATED_RPC_MESSAGE:
                case ENTITY_UPDATED_RPC_MESSAGE:
                    saveOrUpdateRuleChainMetadata(tenantId, ruleChainId, ruleChainMetadataUpdateMsg);
                    return Futures.immediateFuture(null);
                case UNRECOGNIZED:
                default:
                    return handleUnsupportedMsgType(ruleChainMetadataUpdateMsg.getMsgType());
            }
        } catch (Exception e) {
            String errMsg = String.format("Can't process rule chain metadata update msg %s", ruleChainMetadataUpdateMsg);
            log.error(errMsg, e);
            return Futures.immediateFailedFuture(new RuntimeException(errMsg, e));
        } finally {
            edgeSynchronizationManager.getSync().remove();
        }
    }

    private void saveOrUpdateRuleChain(TenantId tenantId, RuleChainId ruleChainId, RuleChainUpdateMsg ruleChainUpdateMsg, Edge edge) {
        boolean created = super.saveOrUpdateRuleChain(tenantId, ruleChainId, ruleChainUpdateMsg, RuleChainType.EDGE);
        if (created) {
            createRelationFromEdge(tenantId, edge.getId(), ruleChainId);
            pushRuleChainCreatedEventToRuleEngine(tenantId, edge, ruleChainId);
            ruleChainService.assignRuleChainToEdge(tenantId, ruleChainId, edge.getId());
        }
        if (ruleChainUpdateMsg.getRoot()) {
            edge.setRootRuleChainId(ruleChainId);
            edgeService.saveEdge(edge);
        }
    }

    private void pushRuleChainCreatedEventToRuleEngine(TenantId tenantId, Edge edge, RuleChainId ruleChainId) {
        try {
            RuleChain ruleChain = ruleChainService.findRuleChainById(tenantId, ruleChainId);
            String ruleChainAsString = JacksonUtil.toString(ruleChain);
            TbMsgMetaData msgMetaData = getEdgeActionTbMsgMetaData(edge, null);
            pushEntityEventToRuleEngine(tenantId, ruleChainId, null, TbMsgType.ENTITY_CREATED, ruleChainAsString, msgMetaData);
        } catch (Exception e) {
            log.warn("[{}][{}] Failed to push rule chain action to rule engine: {}", tenantId, ruleChainId, TbMsgType.ENTITY_CREATED.name(), e);
        }
    }

    public DownlinkMsg convertRuleChainEventToDownlink(EdgeEvent edgeEvent, EdgeVersion edgeVersion) {
        RuleChainId ruleChainId = new RuleChainId(edgeEvent.getEntityId());
        DownlinkMsg downlinkMsg = null;
        switch (edgeEvent.getAction()) {
            case ADDED:
            case UPDATED:
            case ASSIGNED_TO_EDGE:
                RuleChain ruleChain = ruleChainService.findRuleChainById(edgeEvent.getTenantId(), ruleChainId);
                if (ruleChain != null) {
                    boolean isRoot = false;
                    if (edgeEvent.getBody() != null && edgeEvent.getBody().get(EDGE_IS_ROOT_BODY_KEY) != null) {
                        try {
                            isRoot = Boolean.parseBoolean(edgeEvent.getBody().get(EDGE_IS_ROOT_BODY_KEY).asText());
                        } catch (Exception ignored) {}
                    }
                    UpdateMsgType msgType = getUpdateMsgType(edgeEvent.getAction());
                    RuleChainUpdateMsg ruleChainUpdateMsg =
                            ruleChainMsgConstructor.constructRuleChainUpdatedMsg(msgType, ruleChain, isRoot);
                    RuleChainMetaData ruleChainMetaData = ruleChainService.loadRuleChainMetaData(edgeEvent.getTenantId(), ruleChainId);
                    RuleChainMetadataUpdateMsg ruleChainMetadataUpdateMsg =
                            ruleChainMsgConstructor.constructRuleChainMetadataUpdatedMsg(edgeEvent.getTenantId(), msgType, ruleChainMetaData, edgeVersion);
                    downlinkMsg = DownlinkMsg.newBuilder()
                            .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                            .addRuleChainUpdateMsg(ruleChainUpdateMsg)
                            .addRuleChainMetadataUpdateMsg(ruleChainMetadataUpdateMsg).build();
                }
                break;
            case DELETED:
            case UNASSIGNED_FROM_EDGE:
                downlinkMsg = DownlinkMsg.newBuilder()
                        .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                        .addRuleChainUpdateMsg(ruleChainMsgConstructor.constructRuleChainDeleteMsg(ruleChainId))
                        .build();
                break;
        }
        return downlinkMsg;
    }

    public DownlinkMsg convertRuleChainMetadataEventToDownlink(EdgeEvent edgeEvent, EdgeVersion edgeVersion) {
        RuleChainId ruleChainId = new RuleChainId(edgeEvent.getEntityId());
        RuleChain ruleChain = ruleChainService.findRuleChainById(edgeEvent.getTenantId(), ruleChainId);
        DownlinkMsg downlinkMsg = null;
        if (ruleChain != null) {
            RuleChainMetaData ruleChainMetaData = ruleChainService.loadRuleChainMetaData(edgeEvent.getTenantId(), ruleChainId);
            UpdateMsgType msgType = getUpdateMsgType(edgeEvent.getAction());
            RuleChainMetadataUpdateMsg ruleChainMetadataUpdateMsg =
                    ruleChainMsgConstructor.constructRuleChainMetadataUpdatedMsg(edgeEvent.getTenantId(), msgType, ruleChainMetaData, edgeVersion);
            if (ruleChainMetadataUpdateMsg != null) {
                downlinkMsg = DownlinkMsg.newBuilder()
                        .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                        .addRuleChainMetadataUpdateMsg(ruleChainMetadataUpdateMsg)
                        .build();
            }
        }
        return downlinkMsg;
    }
}
