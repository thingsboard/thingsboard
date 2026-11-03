// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.edge.rpc.processor.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.NotificationRuleId;
import org.thingsboard.server.common.data.notification.rule.NotificationRule;
import org.thingsboard.server.dao.notification.NotificationRuleService;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.gen.edge.v1.NotificationRuleUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.EdgeMsgConstructorUtils;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

@Slf4j
@Component
@TbCoreComponent
public class NotificationRuleEdgeProcessor extends BaseEdgeProcessor {

    @Autowired
    private NotificationRuleService notificationRuleService;

    @Override
    public DownlinkMsg convertEdgeEventToDownlink(EdgeEvent edgeEvent, EdgeVersion edgeVersion) {
        NotificationRuleId notificationRuleId = new NotificationRuleId(edgeEvent.getEntityId());
        DownlinkMsg downlinkMsg = null;
        switch (edgeEvent.getAction()) {
            case ADDED, UPDATED -> {
                NotificationRule notificationRule = notificationRuleService.findNotificationRuleById(edgeEvent.getTenantId(), notificationRuleId);
                if (notificationRule != null) {
                    UpdateMsgType msgType = getUpdateMsgType(edgeEvent.getAction());
                    NotificationRuleUpdateMsg notificationRuleUpdateMsg = EdgeMsgConstructorUtils.constructNotificationRuleUpdateMsg(msgType, notificationRule);
                    downlinkMsg = DownlinkMsg.newBuilder()
                            .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                            .addNotificationRuleUpdateMsg(notificationRuleUpdateMsg)
                            .build();
                }
            }
            case DELETED -> {
                NotificationRuleUpdateMsg notificationRuleUpdateMsg = EdgeMsgConstructorUtils.constructNotificationRuleDeleteMsg(notificationRuleId);
                downlinkMsg = DownlinkMsg.newBuilder()
                        .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                        .addNotificationRuleUpdateMsg(notificationRuleUpdateMsg)
                        .build();
            }
        }
        return downlinkMsg;
    }

    @Override
    public EdgeEventType getEdgeEventType() {
        return EdgeEventType.NOTIFICATION_RULE;
    }

}
