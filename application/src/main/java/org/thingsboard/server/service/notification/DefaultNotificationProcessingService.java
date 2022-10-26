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
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.notification.Notification;
import org.thingsboard.server.common.data.notification.NotificationRequest;
import org.thingsboard.server.common.data.notification.NotificationStatus;
import org.thingsboard.server.dao.notification.NotificationService;
import org.thingsboard.server.dao.notification.NotificationTargetService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.service.executors.DbCallbackExecutorService;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.AccessControlService;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;
import org.thingsboard.server.service.telemetry.AbstractSubscriptionService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
    public NotificationRequest processNotificationRequest(SecurityUser user, NotificationRequest notificationRequest) throws ThingsboardException {
        TenantId tenantId = user.getTenantId();
        List<UserId> recipientsIds = notificationTargetService.findRecipientsForNotificationTarget(tenantId, notificationRequest.getTargetId());
        List<User> recipients = new ArrayList<>();
        for (UserId recipientId : recipientsIds) {
            User recipient = userService.findUserById(tenantId, recipientId); // todo: add caching
            accessControlService.checkPermission(user, Resource.USER, Operation.READ, recipientId, recipient);
            recipients.add(recipient);
        }

        notificationRequest.setTenantId(tenantId);
        notificationRequest.setSenderId(user.getId());
        NotificationRequest savedNotificationRequest = notificationService.createNotificationRequest(tenantId, notificationRequest);

        // todo: delayed sending; check all delayed notification requests on start up, schedule send

        for (User recipient : recipients) {
            dbCallbackExecutorService.submit(() -> {
                Notification notification = createNotification(recipient, notificationRequest);
                onNewNotification(notification);
            });
        }

        return savedNotificationRequest;
    }

    private Notification createNotification(User recipient, NotificationRequest notificationRequest) {
        Notification notification = Notification.builder()
                .requestId(notificationRequest.getId())
                .recipientId(recipient.getId())
                .text(formatNotificationText(notificationRequest.getTextTemplate(), recipient))
                .severity(notificationRequest.getNotificationSeverity())
                .status(NotificationStatus.SENT)
                .build();
        notification = notificationService.createNotification(recipient.getTenantId(), notification);
        return notification;
    }

    private void onNewNotification(Notification notification) {
        wsCallBackExecutor.submit(() -> {

        })
    }

    private String formatNotificationText(String template, User recipient) {
        Map<String, String> context = Map.of(
                "recipientEmail", recipient.getEmail(),
                "recipientFirstName", recipient.getFirstName(),
                "recipientLastName", recipient.getLastName()
        );
        return TbNodeUtils.processTemplate(template, context);
    }

    @Override
    protected String getExecutorPrefix() {
        return "notification";
    }

}
