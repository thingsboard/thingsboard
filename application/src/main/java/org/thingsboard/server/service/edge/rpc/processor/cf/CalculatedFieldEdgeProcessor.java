/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.service.edge.rpc.processor.cf;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.exception.DataValidationException;
import org.thingsboard.server.gen.edge.v1.CalculatedFieldUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.EdgeMsgConstructorUtils;

import java.util.UUID;

@Slf4j
@Component
@TbCoreComponent
public class CalculatedFieldEdgeProcessor extends BaseCalculatedFieldProcessor implements CalculatedFieldProcessor {

    @Override
    public ListenableFuture<Void> processCalculatedFieldMsgFromEdge(TenantId tenantId, Edge edge, CalculatedFieldUpdateMsg calculatedFieldUpdateMsg) {
        CalculatedFieldId calculatedFieldId = new CalculatedFieldId(new UUID(calculatedFieldUpdateMsg.getIdMSB(), calculatedFieldUpdateMsg.getIdLSB()));
        try {
            edgeSynchronizationManager.getEdgeId().set(edge.getId());

            switch (calculatedFieldUpdateMsg.getMsgType()) {
                case ENTITY_CREATED_RPC_MESSAGE:
                case ENTITY_UPDATED_RPC_MESSAGE:
                    processCalculatedField(tenantId, calculatedFieldId, calculatedFieldUpdateMsg, edge);
                    return Futures.immediateFuture(null);
                case ENTITY_DELETED_RPC_MESSAGE:
                    CalculatedField calculatedField = edgeCtx.getCalculatedFieldService().findById(tenantId, calculatedFieldId);
                    if (calculatedField != null) {
                        edgeCtx.getCalculatedFieldService().deleteCalculatedField(tenantId, calculatedFieldId);
                    }
                    return Futures.immediateFuture(null);
                case UNRECOGNIZED:
                default:
                    return handleUnsupportedMsgType(calculatedFieldUpdateMsg.getMsgType());
            }
        } catch (DataValidationException e) {
            if (e.getMessage().contains("limit reached")) {
                log.warn("[{}] Number of allowed calculatedField violated {}", tenantId, calculatedFieldUpdateMsg, e);
                return Futures.immediateFuture(null);
            } else {
                return Futures.immediateFailedFuture(e);
            }
        } finally {
            edgeSynchronizationManager.getEdgeId().remove();
        }
    }

    @Override
    public DownlinkMsg convertEdgeEventToDownlink(EdgeEvent edgeEvent, EdgeVersion edgeVersion) {
        CalculatedFieldId calculatedFieldId = new CalculatedFieldId(edgeEvent.getEntityId());
        switch (edgeEvent.getAction()) {
            case ADDED, UPDATED -> {
                CalculatedField calculatedField = edgeCtx.getCalculatedFieldService().findById(edgeEvent.getTenantId(), calculatedFieldId);
                if (calculatedField != null) {
                    UpdateMsgType msgType = getUpdateMsgType(edgeEvent.getAction());
                    CalculatedFieldUpdateMsg calculatedFieldUpdateMsg = EdgeMsgConstructorUtils.constructCalculatedFieldUpdatedMsg(msgType, calculatedField);
                    return DownlinkMsg.newBuilder()
                            .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                            .addCalculatedFieldUpdateMsg(calculatedFieldUpdateMsg)
                            .build();
                }
            }
            case DELETED -> {
                CalculatedFieldUpdateMsg calculatedFieldUpdateMsg = EdgeMsgConstructorUtils.constructCalculatedFieldDeleteMsg(calculatedFieldId);
                return DownlinkMsg.newBuilder()
                        .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                        .addCalculatedFieldUpdateMsg(calculatedFieldUpdateMsg)
                        .build();
            }
        }
        return null;
    }

    @Override
    public EdgeEventType getEdgeEventType() {
        return EdgeEventType.CALCULATED_FIELD;
    }

    @Override
    public ListenableFuture<Void> processEntityNotification(TenantId tenantId, TransportProtos.EdgeNotificationMsgProto edgeNotificationMsg) {
        EdgeEventType type = EdgeEventType.valueOf(edgeNotificationMsg.getType());
        EdgeEventActionType actionType = EdgeEventActionType.valueOf(edgeNotificationMsg.getAction());
        EntityId entityId = EntityIdFactory.getByEdgeEventTypeAndUuid(type, new UUID(edgeNotificationMsg.getEntityIdMSB(), edgeNotificationMsg.getEntityIdLSB()));
        EdgeId originatorEdgeId = safeGetEdgeId(edgeNotificationMsg.getOriginatorEdgeIdMSB(), edgeNotificationMsg.getOriginatorEdgeIdLSB());

        switch (actionType) {
            case UPDATED:
            case ADDED:
                EntityId calculatedFieldOwnerId = JacksonUtil.fromString(edgeNotificationMsg.getBody(), EntityId.class);
                if (calculatedFieldOwnerId != null &&
                        (EntityType.DEVICE.equals(calculatedFieldOwnerId.getEntityType()) || EntityType.ASSET.equals(calculatedFieldOwnerId.getEntityType()))) {
                    JsonNode body = JacksonUtil.toJsonNode(edgeNotificationMsg.getBody());
                    EdgeId edgeId = safeGetEdgeId(edgeNotificationMsg.getEdgeIdMSB(), edgeNotificationMsg.getEdgeIdLSB());

                    return edgeId != null ?
                            saveEdgeEvent(tenantId, edgeId, type, actionType, entityId, body) :
                            processNotificationToRelatedEdges(tenantId, calculatedFieldOwnerId, entityId, type, actionType, originatorEdgeId);
                } else {
                    return processActionForAllEdges(tenantId, type, actionType, entityId, null, originatorEdgeId);
                }
            default:
                return super.processEntityNotification(tenantId, edgeNotificationMsg);
        }
    }

    private void processCalculatedField(TenantId tenantId, CalculatedFieldId calculatedFieldId, CalculatedFieldUpdateMsg calculatedFieldUpdateMsg, Edge edge) {
        Pair<Boolean, Boolean> resultPair = super.saveOrUpdateCalculatedField(tenantId, calculatedFieldId, calculatedFieldUpdateMsg);
        Boolean wasCreated = resultPair.getFirst();
        if (wasCreated) {
            pushCalculatedFieldCreatedEventToRuleEngine(tenantId, edge, calculatedFieldId);
        }
        Boolean nameWasUpdated = resultPair.getSecond();
        if (nameWasUpdated) {
            saveEdgeEvent(tenantId, edge.getId(), EdgeEventType.CALCULATED_FIELD, EdgeEventActionType.UPDATED, calculatedFieldId, null);
        }
    }

    private void pushCalculatedFieldCreatedEventToRuleEngine(TenantId tenantId, Edge edge, CalculatedFieldId calculatedFieldId) {
        try {
            CalculatedField calculatedField = edgeCtx.getCalculatedFieldService().findById(tenantId, calculatedFieldId);
            String calculatedFieldAsString = JacksonUtil.toString(calculatedField);
            TbMsgMetaData msgMetaData = getEdgeActionTbMsgMetaData(edge, edge.getCustomerId());
            pushEntityEventToRuleEngine(tenantId, calculatedFieldId, edge.getCustomerId(), TbMsgType.ENTITY_CREATED, calculatedFieldAsString, msgMetaData);
        } catch (Exception e) {
            log.warn("[{}][{}] Failed to push calculatedField action to rule engine: {}", tenantId, calculatedFieldId, TbMsgType.ENTITY_CREATED.name(), e);
        }
    }

}
