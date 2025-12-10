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
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.ai.AiModel;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.AiModelId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
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

            return switch (aiModelUpdateMsg.getMsgType()) {
                case ENTITY_CREATED_RPC_MESSAGE, ENTITY_UPDATED_RPC_MESSAGE -> {
                    processAiModel(tenantId, aiModelId, aiModelUpdateMsg, edge);
                    yield Futures.immediateFuture(null);
                }
                case ENTITY_DELETED_RPC_MESSAGE -> {
                    deleteAiModel(tenantId, edge, aiModelId);
                    yield Futures.immediateFuture(null);
                }
                default -> handleUnsupportedMsgType(aiModelUpdateMsg.getMsgType());
            };
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

    private void processAiModel(TenantId tenantId, AiModelId aiModelId, AiModelUpdateMsg aiModelUpdateMsg, Edge edge) {
        Pair<Boolean, Boolean> resultPair = super.saveOrUpdateAiModel(tenantId, aiModelId, aiModelUpdateMsg);
        Boolean created = resultPair.getFirst();
        if (created) {
            Optional<AiModel> aiModel = edgeCtx.getAiModelService().findAiModelById(tenantId, aiModelId);
            aiModel.ifPresent(model -> pushEntityEventToRuleEngine(tenantId, edge, model, TbMsgType.ENTITY_CREATED));
        }
        Boolean aiModelNameUpdated = resultPair.getSecond();
        if (aiModelNameUpdated) {
            saveEdgeEvent(tenantId, edge.getId(), EdgeEventType.AI_MODEL, EdgeEventActionType.UPDATED, aiModelId, null);
        }
    }

    @Override
    public EdgeEventType getEdgeEventType() {
        return EdgeEventType.AI_MODEL;
    }
}
