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
package org.thingsboard.server.service.edge.rpc.processor.ai;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.ai.AiModel;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.AiModelId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.gen.edge.v1.AiModelUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.EdgeMsgConstructorUtils;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@TbCoreComponent
public class AiModelEdgeProcessor extends BaseAiModelProcessor implements AiModelProcessor {

    @Override
    public ListenableFuture<Void> processAiModelMsgFromEdge(TenantId tenantId, Edge edge, AiModelUpdateMsg aiModelUpdateMsg) {
        AiModelId aiModelId = new AiModelId(new UUID(aiModelUpdateMsg.getIdMSB(), aiModelUpdateMsg.getIdLSB()));
        try {
            edgeSynchronizationManager.getEdgeId().set(edge.getId());

            switch (aiModelUpdateMsg.getMsgType()) {
                case ENTITY_CREATED_RPC_MESSAGE:
                case ENTITY_UPDATED_RPC_MESSAGE:
                    processAiModel(tenantId, aiModelId, aiModelUpdateMsg, edge);
                    return Futures.immediateFuture(null);
                case UNRECOGNIZED:
                default:
                    return handleUnsupportedMsgType(aiModelUpdateMsg.getMsgType());
            }
        } catch (DataValidationException e) {
            return Futures.immediateFailedFuture(e);
        } finally {
            edgeSynchronizationManager.getEdgeId().remove();
        }
    }

    @Override
    public DownlinkMsg convertEdgeEventToDownlink(EdgeEvent edgeEvent, EdgeVersion edgeVersion) {
        AiModelId aiModelId = new AiModelId(edgeEvent.getEntityId());
        switch (edgeEvent.getAction()) {
            case ADDED, UPDATED -> {
                Optional<AiModel> aiModel = edgeCtx.getAiModelService().findAiModelById(edgeEvent.getTenantId(), aiModelId);
                if (aiModel.isPresent()) {
                    UpdateMsgType msgType = getUpdateMsgType(edgeEvent.getAction());
                    AiModelUpdateMsg aiModelUpdateMsg = EdgeMsgConstructorUtils.constructAiModelUpdatedMsg(msgType, aiModel.get());
                    return DownlinkMsg.newBuilder()
                            .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                            .addAiModelUpdateMsg(aiModelUpdateMsg)
                            .build();
                }
            }
            case DELETED -> {
                AiModelUpdateMsg aiModelUpdateMsg = EdgeMsgConstructorUtils.constructAiModelDeleteMsg(aiModelId);
                return DownlinkMsg.newBuilder()
                        .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                        .addAiModelUpdateMsg(aiModelUpdateMsg)
                        .build();
            }
        }
        return null;
    }

    @Override
    public EdgeEventType getEdgeEventType() {
        return EdgeEventType.AI_MODEL;
    }

    private void processAiModel(TenantId tenantId, AiModelId aiModelId, AiModelUpdateMsg aiModelUpdateMsg, Edge edge) {
        Pair<Boolean, Boolean> resultPair = super.saveOrUpdateAiModel(tenantId, aiModelId, aiModelUpdateMsg);
        Boolean wasCreated = resultPair.getFirst();
        if (wasCreated) {
            pushAiModelCreatedEventToRuleEngine(tenantId, edge, aiModelId);
        }
        Boolean nameWasUpdated = resultPair.getSecond();
        if (nameWasUpdated) {
            saveEdgeEvent(tenantId, edge.getId(), EdgeEventType.AI_MODEL, EdgeEventActionType.UPDATED, aiModelId, null);
        }
    }

    private void pushAiModelCreatedEventToRuleEngine(TenantId tenantId, Edge edge, AiModelId aiModelId) {
        try {
            Optional<AiModel> aiModel = edgeCtx.getAiModelService().findAiModelById(tenantId, aiModelId);
            if (aiModel.isPresent()) {
                String aiModelAsString = JacksonUtil.toString(aiModel.get());
                TbMsgMetaData msgMetaData = getEdgeActionTbMsgMetaData(edge, edge.getCustomerId());
                pushEntityEventToRuleEngine(tenantId, aiModelId, edge.getCustomerId(), TbMsgType.ENTITY_CREATED, aiModelAsString, msgMetaData);
            } else {
                log.warn("[{}][{}] Failed to find aiModel", tenantId, aiModelId);
            }
        } catch (Exception e) {
            log.warn("[{}][{}] Failed to push aiModel action to rule engine: {}", tenantId, aiModelId, TbMsgType.ENTITY_CREATED.name(), e);
        }
    }

}
