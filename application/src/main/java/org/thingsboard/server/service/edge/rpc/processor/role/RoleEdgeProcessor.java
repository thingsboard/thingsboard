// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.edge.rpc.processor.role;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.EdgeMsgConstructorUtils;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@Slf4j
@TbCoreComponent
public class RoleEdgeProcessor extends BaseEdgeProcessor {

    @Override
    public DownlinkMsg convertEdgeEventToDownlink(EdgeEvent edgeEvent, EdgeVersion edgeVersion) {
        RoleId roleId = new RoleId(edgeEvent.getEntityId());
        DownlinkMsg downlinkMsg = null;
        UpdateMsgType msgType = getUpdateMsgType(edgeEvent.getAction());
        switch (msgType) {
            case ENTITY_CREATED_RPC_MESSAGE:
            case ENTITY_UPDATED_RPC_MESSAGE:
                Role role = edgeCtx.getRoleService().findRoleById(edgeEvent.getTenantId(), roleId);
                if (role != null) {
                    downlinkMsg = DownlinkMsg.newBuilder()
                            .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                            .addRoleMsg(EdgeMsgConstructorUtils.constructRoleProto(msgType, role))
                            .build();
                }
                break;
            case ENTITY_DELETED_RPC_MESSAGE:
                downlinkMsg = DownlinkMsg.newBuilder()
                        .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                        .addRoleMsg(EdgeMsgConstructorUtils.constructRoleDeleteMsg(roleId))
                        .build();
                break;
        }
        return downlinkMsg;
    }

    @Override
    public ListenableFuture<Void> processEntityNotification(TenantId tenantId, TransportProtos.EdgeNotificationMsgProto edgeNotificationMsg) {
        EdgeEventActionType actionType = EdgeEventActionType.valueOf(edgeNotificationMsg.getAction());
        EdgeEventType type = EdgeEventType.valueOf(edgeNotificationMsg.getType());
        EntityId entityId = EntityIdFactory.getByEdgeEventTypeAndUuid(type,
                new UUID(edgeNotificationMsg.getEntityIdMSB(), edgeNotificationMsg.getEntityIdLSB()));
        EdgeId originatorEdgeId = safeGetEdgeId(edgeNotificationMsg.getOriginatorEdgeIdMSB(), edgeNotificationMsg.getOriginatorEdgeIdLSB());
        return switch (actionType) {
            case ADDED, UPDATED -> {
                ListenableFuture<Role> roleFuture = edgeCtx.getRoleService().findRoleByIdAsync(tenantId, new RoleId(entityId.getId()));
                yield Futures.transformAsync(roleFuture, role -> {
                    if (role == null) {
                        return Futures.immediateFuture(null);
                    }
                    List<ListenableFuture<Void>> futures = new ArrayList<>();
                    PageDataIterable<Edge> edges = new PageDataIterable<>(
                            link -> edgeCtx.getEdgeService().findEdgesByTenantId(tenantId, link), 1024);
                    for (Edge edge : edges) {
                        if (EntityType.TENANT.equals(role.getOwnerId().getEntityType()) ||
                                edge.getOwnerId().equals(role.getOwnerId())) {
                            futures.add(saveEdgeEvent(tenantId, edge.getId(), type, actionType, entityId, null));
                        }
                    }
                    return Futures.transform(Futures.allAsList(futures), voids -> null, dbCallbackExecutorService);
                }, dbCallbackExecutorService);
            }
            case DELETED -> processActionForAllEdges(tenantId, type, actionType, entityId, null, originatorEdgeId);
            default -> Futures.immediateFuture(null);
        };
    }

    @Override
    public EdgeEventType getEdgeEventType() {
        return EdgeEventType.ROLE;
    }

}
