/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.server.service.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.notification.Notification;
import org.thingsboard.server.common.data.notification.NotificationRequest;
import org.thingsboard.server.dao.notification.NotificationService;
import org.thingsboard.server.dao.notification.NotificationTargetService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.service.executors.DbCallbackExecutorService;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.AccessControlService;
import org.thingsboard.server.service.telemetry.AbstractSubscriptionService;

import java.util.List;

@Service
@Slf4j
public class DefaultNotificationProcessingService extends AbstractSubscriptionService implements NotificationProcessingService {

    private final NotificationTargetService notificationTargetService;
    private final NotificationService notificationService;
    private final UserService userService;
    private final AccessControlService accessControlService;
    private final DbCallbackExecutorService dbCallbackExecutorService;

    public DefaultNotificationProcessingService(TbClusterService clusterService, PartitionService partitionService,
                                                NotificationTargetService notificationTargetService,
                                                NotificationService notificationService, UserService userService,
                                                AccessControlService accessControlService,
                                                DbCallbackExecutorService dbCallbackExecutorService) {
        super(clusterService, partitionService);
        this.notificationTargetService = notificationTargetService;
        this.notificationService = notificationService;
        this.userService = userService;
        this.accessControlService = accessControlService;
        this.dbCallbackExecutorService = dbCallbackExecutorService;
    }

    @Override
    public void processNotificationRequest(SecurityUser user, NotificationRequest notificationRequest) {
        TenantId tenantId = user.getTenantId();
        notificationRequest = notificationService.createNotificationRequest(tenantId, notificationRequest);

        List<UserId> recipients = notificationTargetService.findRecipientsForNotificationTarget(tenantId, notificationRequest.getTargetId());
        for (UserId recipientId : recipients) {
            try {
                // todo: check read permission for recipientId
                Notification notification = Notification.builder()
                        .tenantId(tenantId)
                        .requestId(notificationRequest.getId())
                        .status(null)
                        .recipientId(recipientId)
                        .text(formatNotificationText(notificationRequest.getTextTemplate(), null))
                        .severity(notificationRequest.getSeverity())
                        .senderId(notificationRequest.getSenderId())
                        .build();
                notification = notificationService.createNotification(tenantId, notification);
                onNewNotification(notification);
            } catch (Exception e) {
                // fixme: handle
            }
        }

    }

    private void onNewNotification(Notification notification) {
    }

    private String formatNotificationText(String template, Object context) {
        return template;
    }

    @Override
    protected String getExecutorPrefix() {
        return "notification";
    }

}
