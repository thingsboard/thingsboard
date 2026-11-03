// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.edge.rpc.processor.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.NotificationTargetId;
import org.thingsboard.server.common.data.notification.targets.NotificationTarget;
import org.thingsboard.server.dao.notification.NotificationTargetService;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.gen.edge.v1.NotificationTargetUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.EdgeMsgConstructorUtils;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

@Slf4j
@Component
@TbCoreComponent
public class NotificationTargetEdgeProcessor extends BaseEdgeProcessor {

    @Autowired
    private NotificationTargetService notificationTargetService;

    @Override
    public DownlinkMsg convertEdgeEventToDownlink(EdgeEvent edgeEvent, EdgeVersion edgeVersion) {
        NotificationTargetId notificationTargetId = new NotificationTargetId(edgeEvent.getEntityId());
        DownlinkMsg downlinkMsg = null;
        switch (edgeEvent.getAction()) {
            case ADDED, UPDATED -> {
                NotificationTarget notificationTarget = notificationTargetService.findNotificationTargetById(edgeEvent.getTenantId(), notificationTargetId);
                if (notificationTarget != null) {
                    UpdateMsgType msgType = getUpdateMsgType(edgeEvent.getAction());
                    NotificationTargetUpdateMsg notificationTargetUpdateMsg = EdgeMsgConstructorUtils.constructNotificationTargetUpdateMsg(msgType, notificationTarget);
                    downlinkMsg = DownlinkMsg.newBuilder()
                            .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                            .addNotificationTargetUpdateMsg(notificationTargetUpdateMsg)
                            .build();
                }
            }
            case DELETED -> {
                NotificationTargetUpdateMsg notificationTargetUpdateMsg = EdgeMsgConstructorUtils.constructNotificationTargetDeleteMsg(notificationTargetId);
                downlinkMsg = DownlinkMsg.newBuilder()
                        .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                        .addNotificationTargetUpdateMsg(notificationTargetUpdateMsg)
                        .build();
            }
        }
        return downlinkMsg;
    }

    @Override
    public EdgeEventType getEdgeEventType() {
        return EdgeEventType.NOTIFICATION_TARGET;
    }

}
