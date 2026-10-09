// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.edge.rpc.processor.entityview;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.edge.rpc.EdgeVersionComparator;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.exception.DataValidationException;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.gen.edge.v1.EntityViewUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.EdgeMsgConstructorUtils;

import java.util.UUID;

@Slf4j
@Component
@TbCoreComponent
public class EntityViewEdgeProcessor extends BaseEntityViewProcessor implements EntityViewProcessor {

    @Override
    public ListenableFuture<Void> processEntityViewMsgFromEdge(TenantId tenantId, Edge edge, EntityViewUpdateMsg entityViewUpdateMsg, EdgeVersion edgeVersion) {
        log.trace("[{}] executing processEntityViewMsgFromEdge [{}] from edge [{}]", tenantId, entityViewUpdateMsg, edge.getId());
        EntityViewId entityViewId = new EntityViewId(new UUID(entityViewUpdateMsg.getIdMSB(), entityViewUpdateMsg.getIdLSB()));
        try {
            edgeSynchronizationManager.getEdgeId().set(edge.getId());

            return switch (entityViewUpdateMsg.getMsgType()) {
                case ENTITY_CREATED_RPC_MESSAGE, ENTITY_UPDATED_RPC_MESSAGE -> {
                    saveOrUpdateEntityView(tenantId, entityViewId, entityViewUpdateMsg, edge);
                    yield Futures.immediateFuture(null);
                }
                case ENTITY_DELETED_RPC_MESSAGE -> {
                    if (entityViewUpdateMsg.hasEntityGroupIdMSB() && entityViewUpdateMsg.hasEntityGroupIdLSB()) {
                        EntityGroupId entityGroupId = new EntityGroupId(
                                new UUID(entityViewUpdateMsg.getEntityGroupIdMSB(), entityViewUpdateMsg.getEntityGroupIdLSB()));
                        edgeCtx.getEntityGroupService().removeEntityFromEntityGroup(tenantId, entityGroupId, entityViewId);
                    } else if (EdgeVersionComparator.INSTANCE.compare(edgeVersion, EdgeVersion.V_4_3_0) >= 0) {
                        deleteEntityView(tenantId, edge, entityViewId);
                    } else {
                        removeEntityViewFromEdgeAllEntityViewGroup(tenantId, edge, entityViewId);
                    }
                    yield Futures.immediateFuture(null);
                }
                default -> handleUnsupportedMsgType(entityViewUpdateMsg.getMsgType());
            };
        } catch (DataValidationException | ThingsboardException e) {
            if (e.getMessage().contains("limit reached")) {
                log.warn("[{}] Number of allowed entity views violated {}", tenantId, entityViewUpdateMsg, e);
                return Futures.immediateFuture(null);
            } else {
                return Futures.immediateFailedFuture(e);
            }
        } finally {
            edgeSynchronizationManager.getEdgeId().remove();
        }
    }

    private void saveOrUpdateEntityView(TenantId tenantId, EntityViewId entityViewId, EntityViewUpdateMsg entityViewUpdateMsg, Edge edge) throws ThingsboardException {
        Pair<Boolean, Boolean> resultPair = super.saveOrUpdateEntityView(tenantId, entityViewId, entityViewUpdateMsg);
        Boolean created = resultPair.getFirst();
        if (created) {
            createRelationFromEdge(tenantId, edge.getId(), entityViewId);
            pushEntityViewCreatedEventToRuleEngine(tenantId, edge, entityViewId);
        }
        addEntityViewToEdgeAllEntityViewGroup(tenantId, edge, entityViewId);
        Boolean assetNameUpdated = resultPair.getSecond();
        if (assetNameUpdated) {
            saveEdgeEvent(tenantId, edge.getId(), EdgeEventType.ENTITY_VIEW, EdgeEventActionType.UPDATED, entityViewId, null);
        }
    }

    private void pushEntityViewCreatedEventToRuleEngine(TenantId tenantId, Edge edge, EntityViewId entityViewId) {
        EntityView entityView = edgeCtx.getEntityViewService().findEntityViewById(tenantId, entityViewId);
        pushEntityEventToRuleEngine(tenantId, edge, entityView, TbMsgType.ENTITY_CREATED);
    }

    private void addEntityViewToEdgeAllEntityViewGroup(TenantId tenantId, Edge edge, EntityViewId entityViewId) {
        EntityView entityView = edgeCtx.getEntityViewService().findEntityViewById(tenantId, entityViewId);
        addEntityToEdgeAllGroup(tenantId, edge, entityView);
    }

    private void removeEntityViewFromEdgeAllEntityViewGroup(TenantId tenantId, Edge edge, EntityViewId entityViewId) {
        EntityView entityViewToDelete = edgeCtx.getEntityViewService().findEntityViewById(tenantId, entityViewId);
        removeEntityFromEdgeAllGroup(tenantId, edge, entityViewToDelete);
    }

    @Override
    public DownlinkMsg convertEdgeEventToDownlink(EdgeEvent edgeEvent, EdgeVersion edgeVersion) {
        EntityViewId entityViewId = new EntityViewId(edgeEvent.getEntityId());
        EntityGroupId entityGroupId = edgeEvent.getEntityGroupId() != null ? new EntityGroupId(edgeEvent.getEntityGroupId()) : null;
        switch (edgeEvent.getAction()) {
            case ADDED, ADDED_TO_ENTITY_GROUP, UPDATED, ASSIGNED_TO_EDGE -> {
                EntityView entityView = edgeCtx.getEntityViewService().findEntityViewById(edgeEvent.getTenantId(), entityViewId);
                if (entityView != null) {
                    UpdateMsgType msgType = getUpdateMsgType(edgeEvent.getAction());
                    EntityViewUpdateMsg entityViewUpdateMsg = EdgeMsgConstructorUtils.constructEntityViewUpdatedMsg(msgType, entityView, entityGroupId);
                    return DownlinkMsg.newBuilder()
                            .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                            .addEntityViewUpdateMsg(entityViewUpdateMsg)
                            .build();
                }
            }
            case DELETED, REMOVED_FROM_ENTITY_GROUP, UNASSIGNED_FROM_EDGE, CHANGE_OWNER -> {
                return DownlinkMsg.newBuilder()
                        .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                        .addEntityViewUpdateMsg(EdgeMsgConstructorUtils.constructEntityViewDeleteMsg(entityViewId, entityGroupId))
                        .build();
            }
        }
        return null;
    }

    @Override
    protected void setCustomerId(TenantId tenantId, CustomerId customerId, EntityView entityView, EntityViewUpdateMsg entityViewUpdateMsg) {
        CustomerId customerUUID = entityView.getCustomerId() != null ? entityView.getCustomerId() : customerId;
        entityView.setCustomerId(customerUUID);
    }

    @Override
    public EdgeEventType getEdgeEventType() {
        return EdgeEventType.ENTITY_VIEW;
    }

}
