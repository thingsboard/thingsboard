/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.exception.DataValidationException;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.gen.edge.v1.RuleChainMetadataUpdateMsg;
import org.thingsboard.server.gen.edge.v1.RuleChainUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.EdgeMsgConstructorUtils;

import java.util.UUID;

import static org.thingsboard.server.dao.edge.EdgeServiceImpl.EDGE_IS_ROOT_BODY_KEY;

@Slf4j
@Component
@TbCoreComponent
public class RuleChainEdgeProcessor extends BaseRuleChainProcessor {

    public ListenableFuture<Void> processRuleChainMsgFromEdge(TenantId tenantId, Edge edge, RuleChainUpdateMsg ruleChainUpdateMsg) {
        log.trace("[{}] executing processRuleChainMsgFromEdge [{}] from edge [{}]", tenantId, ruleChainUpdateMsg, edge.getName());
        RuleChainId ruleChainId = new RuleChainId(new UUID(ruleChainUpdateMsg.getIdMSB(), ruleChainUpdateMsg.getIdLSB()));
        try {
            edgeSynchronizationManager.getEdgeId().set(edge.getId());

            switch (ruleChainUpdateMsg.getMsgType()) {
                case ENTITY_CREATED_RPC_MESSAGE:
                case ENTITY_UPDATED_RPC_MESSAGE:
                    return saveOrUpdateRuleChain(tenantId, ruleChainId, ruleChainUpdateMsg, edge);
                case ENTITY_DELETED_RPC_MESSAGE:
                    RuleChain ruleChainToDelete = edgeCtx.getRuleChainService().findRuleChainById(tenantId, ruleChainId);
                    if (ruleChainToDelete != null) {
                        edgeCtx.getRuleChainService().unassignRuleChainFromEdge(tenantId, ruleChainId, edge.getId(), false);
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
            edgeSynchronizationManager.getEdgeId().remove();
        }
    }

    private ListenableFuture<Void> saveOrUpdateRuleChain(TenantId tenantId, RuleChainId ruleChainId, RuleChainUpdateMsg ruleChainUpdateMsg, Edge edge) {
        try {
            Pair<Boolean, Boolean> resultPair = super.saveOrUpdateRuleChain(tenantId, ruleChainId, ruleChainUpdateMsg, RuleChainType.EDGE);
            Boolean created = resultPair.getFirst();
            if (created) {
                createRelationFromEdge(tenantId, edge.getId(), ruleChainId);
                pushRuleChainCreatedEventToRuleEngine(tenantId, edge, ruleChainId, ruleChainUpdateMsg.getEntity());
                edgeCtx.getRuleChainService().assignRuleChainToEdge(tenantId, ruleChainId, edge.getId());
            }
            Boolean isRoot = resultPair.getSecond();
            if (isRoot) {
                edge = edgeCtx.getEdgeService().findEdgeById(tenantId, edge.getId());
                edgeCtx.getEdgeService().setEdgeRootRuleChain(tenantId, edge, ruleChainId);
            }
        } catch (Exception e) {
            log.error("Failed to save or update rule chain", e);
            return Futures.immediateFailedFuture(e);
        }
        return Futures.immediateFuture(null);
    }

    private void pushRuleChainCreatedEventToRuleEngine(TenantId tenantId, Edge edge, RuleChainId ruleChainId, String ruleChainAsString) {
        try {
            TbMsgMetaData msgMetaData = getEdgeActionTbMsgMetaData(edge, null);
            pushEntityEventToRuleEngine(tenantId, ruleChainId, null, TbMsgType.ENTITY_CREATED, ruleChainAsString, msgMetaData);
        } catch (Exception e) {
            log.warn("[{}][{}] Failed to push rule chain action to rule engine: {}", tenantId, ruleChainId, TbMsgType.ENTITY_CREATED.name(), e);
        }
    }

    public ListenableFuture<Void> processRuleChainMetadataMsgFromEdge(TenantId tenantId, Edge edge, RuleChainMetadataUpdateMsg ruleChainMetadataUpdateMsg) {
        log.trace("[{}] executing processRuleChainMetadataMsgFromEdge [{}] from edge [{}]", tenantId, ruleChainMetadataUpdateMsg, edge.getName());
        try {
            edgeSynchronizationManager.getEdgeId().set(edge.getId());

            switch (ruleChainMetadataUpdateMsg.getMsgType()) {
                case ENTITY_CREATED_RPC_MESSAGE:
                case ENTITY_UPDATED_RPC_MESSAGE:
                    saveOrUpdateRuleChainMetadata(tenantId, ruleChainMetadataUpdateMsg);
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
            edgeSynchronizationManager.getEdgeId().remove();
        }
    }

    @Override
    public DownlinkMsg convertEdgeEventToDownlink(EdgeEvent edgeEvent, EdgeVersion edgeVersion) {
        RuleChainId ruleChainId = new RuleChainId(edgeEvent.getEntityId());
        DownlinkMsg downlinkMsg = null;
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
                    RuleChainUpdateMsg ruleChainUpdateMsg = EdgeMsgConstructorUtils.constructRuleChainUpdatedMsg(msgType, ruleChain, isRoot);

                    DownlinkMsg.Builder builder = DownlinkMsg.newBuilder()
                            .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                            .addRuleChainUpdateMsg(ruleChainUpdateMsg);

                    RuleChainMetaData ruleChainMetaData = edgeCtx.getRuleChainService().loadRuleChainMetaData(edgeEvent.getTenantId(), ruleChainId);
                    RuleChainMetadataUpdateMsg ruleChainMetadataUpdateMsg = EdgeMsgConstructorUtils
                            .constructRuleChainMetadataUpdatedMsg(msgType, ruleChainMetaData, edgeVersion);
                    builder.addRuleChainMetadataUpdateMsg(ruleChainMetadataUpdateMsg);
                    downlinkMsg = builder.build();
                }
            }
            case DELETED, UNASSIGNED_FROM_EDGE -> downlinkMsg = DownlinkMsg.newBuilder()
                    .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                    .addRuleChainUpdateMsg(EdgeMsgConstructorUtils.constructRuleChainDeleteMsg(ruleChainId))
                    .build();
        }
        return downlinkMsg;
    }

    @Override
    public EdgeEventType getEdgeEventType() {
        return EdgeEventType.RULE_CHAIN;
    }

}
