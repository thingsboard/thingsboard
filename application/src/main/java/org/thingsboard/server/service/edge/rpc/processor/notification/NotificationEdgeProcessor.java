/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
package org.thingsboard.server.service.edge.rpc.processor.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.id.NotificationRuleId;
import org.thingsboard.server.common.data.id.NotificationTargetId;
import org.thingsboard.server.common.data.id.NotificationTemplateId;
import org.thingsboard.server.common.data.notification.rule.NotificationRule;
import org.thingsboard.server.common.data.notification.targets.NotificationTarget;
import org.thingsboard.server.common.data.notification.template.NotificationTemplate;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.NotificationRuleUpdateMsg;
import org.thingsboard.server.gen.edge.v1.NotificationTargetUpdateMsg;
import org.thingsboard.server.gen.edge.v1.NotificationTemplateUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

@Slf4j
@Component
@TbCoreComponent
public class NotificationEdgeProcessor extends BaseEdgeProcessor {

    public DownlinkMsg convertNotificationRuleToDownlink(EdgeEvent edgeEvent) {
        NotificationRuleId notificationRuleId = new NotificationRuleId(edgeEvent.getEntityId());
        DownlinkMsg downlinkMsg = null;
        switch (edgeEvent.getAction()) {
            case ADDED, UPDATED -> {
                NotificationRule notificationRule = notificationRuleService.findNotificationRuleById(edgeEvent.getTenantId(), notificationRuleId);
                if (notificationRule != null) {
                    UpdateMsgType msgType = getUpdateMsgType(edgeEvent.getAction());
                    NotificationRuleUpdateMsg notificationRuleUpdateMsg = notificationMsgConstructor.constructNotificationRuleUpdateMsg(msgType, notificationRule);
                    downlinkMsg = DownlinkMsg.newBuilder()
                            .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                            .addNotificationRuleUpdateMsg(notificationRuleUpdateMsg)
                            .build();
                }
            }
            case DELETED -> {
                NotificationRuleUpdateMsg notificationRuleUpdateMsg = notificationMsgConstructor.constructNotificationRuleDeleteMsg(notificationRuleId);
                downlinkMsg = DownlinkMsg.newBuilder()
                        .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                        .addNotificationRuleUpdateMsg(notificationRuleUpdateMsg)
                        .build();
            }
        }
        return downlinkMsg;
    }

    public DownlinkMsg convertNotificationTargetToDownlink(EdgeEvent edgeEvent) {
        NotificationTargetId notificationTargetId = new NotificationTargetId(edgeEvent.getEntityId());
        DownlinkMsg downlinkMsg = null;
        switch (edgeEvent.getAction()) {
            case ADDED, UPDATED -> {
                NotificationTarget notificationTarget = notificationTargetService.findNotificationTargetById(edgeEvent.getTenantId(), notificationTargetId);
                if (notificationTarget != null) {
                    UpdateMsgType msgType = getUpdateMsgType(edgeEvent.getAction());
                    NotificationTargetUpdateMsg notificationTargetUpdateMsg = notificationMsgConstructor.constructNotificationTargetUpdateMsg(msgType, notificationTarget);
                    downlinkMsg = DownlinkMsg.newBuilder()
                            .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                            .addNotificationTargetUpdateMsg(notificationTargetUpdateMsg)
                            .build();
                }
            }
            case DELETED -> {
                NotificationTargetUpdateMsg notificationTargetUpdateMsg = notificationMsgConstructor.constructNotificationTargetDeleteMsg(notificationTargetId);
                downlinkMsg = DownlinkMsg.newBuilder()
                        .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                        .addNotificationTargetUpdateMsg(notificationTargetUpdateMsg)
                        .build();
            }
        }
        return downlinkMsg;
    }

    public DownlinkMsg convertNotificationTemplateToDownlink(EdgeEvent edgeEvent) {
        NotificationTemplateId notificationTemplateId = new NotificationTemplateId(edgeEvent.getEntityId());
        DownlinkMsg downlinkMsg = null;
        switch (edgeEvent.getAction()) {
            case ADDED, UPDATED -> {
                NotificationTemplate notificationTemplate = notificationTemplateService.findNotificationTemplateById(edgeEvent.getTenantId(), notificationTemplateId);
                if (notificationTemplate != null) {
                    UpdateMsgType msgType = getUpdateMsgType(edgeEvent.getAction());
                    NotificationTemplateUpdateMsg notificationTemplateUpdateMsg = notificationMsgConstructor.constructNotificationTemplateUpdateMsg(msgType, notificationTemplate);
                    downlinkMsg = DownlinkMsg.newBuilder()
                            .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                            .addNotificationTemplateUpdateMsg(notificationTemplateUpdateMsg)
                            .build();
                }
            }
            case DELETED -> {
                NotificationTemplateUpdateMsg notificationTemplateUpdateMsg = notificationMsgConstructor.constructNotificationTemplateDeleteMsg(notificationTemplateId);
                downlinkMsg = DownlinkMsg.newBuilder()
                        .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                        .addNotificationTemplateUpdateMsg(notificationTemplateUpdateMsg)
                        .build();
            }
        }
        return downlinkMsg;
    }

}
