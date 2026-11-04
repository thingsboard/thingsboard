// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.edge.rpc.processor.group;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.GroupPermissionId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.common.data.permission.GroupPermission;
import org.thingsboard.server.dao.grouppermission.GroupPermissionService;
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

@Slf4j
@Component
@TbCoreComponent
public class GroupPermissionsEdgeProcessor extends BaseEdgeProcessor {

    @Autowired
    protected GroupPermissionService groupPermissionService;

    @Override
    public DownlinkMsg convertEdgeEventToDownlink(EdgeEvent edgeEvent, EdgeVersion edgeVersion) {
        GroupPermissionId groupPermissionId = new GroupPermissionId(edgeEvent.getEntityId());
        DownlinkMsg downlinkMsg = null;
        UpdateMsgType msgType = getUpdateMsgType(edgeEvent.getAction());
        switch (msgType) {
            case ENTITY_CREATED_RPC_MESSAGE, ENTITY_UPDATED_RPC_MESSAGE -> {
                GroupPermission groupPermission = groupPermissionService.findGroupPermissionById(edgeEvent.getTenantId(), groupPermissionId);
                if (groupPermission != null) {
                    downlinkMsg = DownlinkMsg.newBuilder()
                            .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                            .addGroupPermissionMsg(EdgeMsgConstructorUtils.constructGroupPermissionProto(msgType, groupPermission))
                            .build();
                }
            }
            case ENTITY_DELETED_RPC_MESSAGE -> downlinkMsg = DownlinkMsg.newBuilder()
                    .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                    .addGroupPermissionMsg(EdgeMsgConstructorUtils.constructGroupPermissionDeleteMsg(groupPermissionId))
                    .build();
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
                ListenableFuture<GroupPermission> gpFuture = groupPermissionService.findGroupPermissionByIdAsync(tenantId, new GroupPermissionId(entityId.getId()));
                yield Futures.transformAsync(gpFuture, groupPermission -> {
                    if (groupPermission == null) {
                        return Futures.immediateFuture(null);
                    }
                    List<ListenableFuture<Void>> futures = new ArrayList<>();
                    PageDataIterable<EdgeId> edgeIds = new PageDataIterable<>(
                            link -> edgeCtx.getEdgeService().findRelatedEdgeIdsByEntityId(tenantId, groupPermission.getUserGroupId(), EntityType.USER, link), 1024);
                    for (EdgeId edgeId : edgeIds) {
                        EntityType entityGroupType = groupPermission.getEntityGroupType();
                        EntityGroupId targetGroupId = entityGroupType == null ? groupPermission.getUserGroupId() : groupPermission.getEntityGroupId();
                        EntityType targetGroupType = entityGroupType == null ? EntityType.USER : entityGroupType;

                        ListenableFuture<Boolean> checkFuture =
                                edgeCtx.getEntityGroupService().checkEntityGroupAssignedToEdgeAsync(tenantId, edgeId, targetGroupId, targetGroupType);
                        futures.add(Futures.transformAsync(checkFuture, exists -> {
                            if (Boolean.TRUE.equals(exists)) {
                                return saveEdgeEvent(tenantId, edgeId, type, actionType, entityId, null);
                            } else {
                                return Futures.immediateFuture(null);
                            }
                        }, dbCallbackExecutorService));
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
        return EdgeEventType.GROUP_PERMISSION;
    }

}
