// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.edge.rpc.processor.menu;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.menu.CustomMenu;
import org.thingsboard.server.dao.menu.CustomMenuService;
import org.thingsboard.server.gen.edge.v1.CustomMenuProto;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.EdgeMsgConstructorUtils;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@TbCoreComponent
public class CustomMenuEdgeProcessor extends BaseEdgeProcessor {

    @Autowired
    private CustomMenuService customMenuService;

    @Override
    public DownlinkMsg convertEdgeEventToDownlink(EdgeEvent edgeEvent, EdgeVersion edgeVersion) {
        CustomMenu customMenu = JacksonUtil.convertValue(edgeEvent.getBody(), CustomMenu.class);
        if (customMenu == null) {
            return null;
        }
        try {
            UpdateMsgType msgType = getUpdateMsgType(edgeEvent.getAction());
            CustomMenuProto customMenuProto = null;

            switch (edgeEvent.getAction()) {
                case ADDED, UPDATED -> {
                    List<EntityId> entityIds = customMenuService.findCustomMenuAssigneeList(customMenu).stream().map(EntityInfo::getId).toList();
                    customMenuProto = EdgeMsgConstructorUtils.constructCustomMenuMsg(msgType, customMenu, entityIds);
                }
                case DELETED -> {
                    customMenuProto = EdgeMsgConstructorUtils.constructCustomMenuMsg(msgType, customMenu, null);
                }
            }
            return DownlinkMsg.newBuilder().setDownlinkMsgId(EdgeUtils.nextPositiveInt()).setCustomMenuProto(customMenuProto).build();
        } catch (Exception e) {
            log.error("Error processing custom menu for edgeEvent [{}]", edgeEvent, e);
            return null;
        }
    }

    @Override
    public ListenableFuture<Void> processEntityNotification(TenantId tenantId, TransportProtos.EdgeNotificationMsgProto edgeNotificationMsg) {
        EdgeEventType type = EdgeEventType.valueOf(edgeNotificationMsg.getType());
        EntityId entityId = EntityIdFactory.getByEdgeEventTypeAndUuid(EdgeEventType.valueOf(edgeNotificationMsg.getEntityType()),
                new UUID(edgeNotificationMsg.getEntityIdMSB(), edgeNotificationMsg.getEntityIdLSB()));
        EdgeId sourceEdgeId = safeGetEdgeId(edgeNotificationMsg.getOriginatorEdgeIdMSB(), edgeNotificationMsg.getOriginatorEdgeIdLSB());
        EdgeEventActionType actionType = EdgeEventActionType.valueOf(edgeNotificationMsg.getAction());
        return processActionForAllEdges(tenantId, type, actionType, entityId, JacksonUtil.toJsonNode(edgeNotificationMsg.getBody()), sourceEdgeId);
    }

    @Override
    public EdgeEventType getEdgeEventType() {
        return EdgeEventType.CUSTOM_MENU;
    }

}
