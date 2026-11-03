// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.edge.rpc.processor.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.NotificationTemplateId;
import org.thingsboard.server.common.data.notification.template.NotificationTemplate;
import org.thingsboard.server.dao.notification.NotificationTemplateService;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.gen.edge.v1.NotificationTemplateUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.EdgeMsgConstructorUtils;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

@Slf4j
@Component
@TbCoreComponent
public class NotificationTemplateEdgeProcessor extends BaseEdgeProcessor {

    @Autowired
    private NotificationTemplateService notificationTemplateService;

    @Override
    public DownlinkMsg convertEdgeEventToDownlink(EdgeEvent edgeEvent, EdgeVersion edgeVersion) {
        NotificationTemplateId notificationTemplateId = new NotificationTemplateId(edgeEvent.getEntityId());
        DownlinkMsg downlinkMsg = null;
        switch (edgeEvent.getAction()) {
            case ADDED, UPDATED -> {
                NotificationTemplate notificationTemplate = notificationTemplateService.findNotificationTemplateById(edgeEvent.getTenantId(), notificationTemplateId);
                if (notificationTemplate != null) {
                    UpdateMsgType msgType = getUpdateMsgType(edgeEvent.getAction());
                    NotificationTemplateUpdateMsg notificationTemplateUpdateMsg = EdgeMsgConstructorUtils.constructNotificationTemplateUpdateMsg(msgType, notificationTemplate);
                    downlinkMsg = DownlinkMsg.newBuilder()
                            .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                            .addNotificationTemplateUpdateMsg(notificationTemplateUpdateMsg)
                            .build();
                }
            }
            case DELETED -> {
                NotificationTemplateUpdateMsg notificationTemplateUpdateMsg = EdgeMsgConstructorUtils.constructNotificationTemplateDeleteMsg(notificationTemplateId);
                downlinkMsg = DownlinkMsg.newBuilder()
                        .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                        .addNotificationTemplateUpdateMsg(notificationTemplateUpdateMsg)
                        .build();
            }
        }
        return downlinkMsg;
    }

    @Override
    public EdgeEventType getEdgeEventType() {
        return EdgeEventType.NOTIFICATION_TEMPLATE;
    }

}
