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

import com.google.common.base.Strings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.NotificationId;
import org.thingsboard.server.common.data.id.NotificationRequestId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.notification.Notification;
import org.thingsboard.server.common.data.notification.NotificationRequest;
import org.thingsboard.server.common.data.notification.NotificationStatus;
import org.thingsboard.server.dao.notification.NotificationService;
import org.thingsboard.server.dao.notification.NotificationTargetService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.service.executors.DbCallbackExecutorService;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.AccessControlService;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;
import org.thingsboard.server.service.ws.notification.sub.NotificationsSubscriptionService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class DefaultNotificationProcessingService implements NotificationProcessingService {

    private final NotificationTargetService notificationTargetService;
    private final NotificationService notificationService;
    private final UserService userService;
    private final AccessControlService accessControlService;
    private final DbCallbackExecutorService dbCallbackExecutorService;
    private final NotificationsSubscriptionService notificationsSubscriptionService;

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
                Notification notification = createNotification(recipient, savedNotificationRequest);
                notificationsSubscriptionService.onNewNotification(recipient.getTenantId(), recipient.getId(), notification);
            });
        }

        return savedNotificationRequest;
    }

    @Override
    public void markNotificationAsRead(SecurityUser user, NotificationId notificationId) {
        Notification notification = notificationService.updateNotificationStatus(user.getTenantId(), notificationId, NotificationStatus.READ);
        notificationsSubscriptionService.onNotificationUpdated(user.getTenantId(), user.getId(), notification);
    }

    @Override
    public void deleteNotificationRequest(SecurityUser user, NotificationRequestId notificationRequestId) {
        notificationService.deleteNotificationRequest(user.getTenantId(), notificationRequestId);
        notificationsSubscriptionService.onNotificationRequestDeleted(user.getTenantId(), notificationRequestId);
    }

    private Notification createNotification(User recipient, NotificationRequest notificationRequest) {
        Notification notification = Notification.builder()
                .requestId(notificationRequest.getId())
                .recipientId(recipient.getId())
                .reason(notificationRequest.getNotificationReason())
                .text(formatNotificationText(notificationRequest.getTextTemplate(), recipient))
                .info(notificationRequest.getNotificationInfo())
                .severity(notificationRequest.getNotificationSeverity())
                .status(NotificationStatus.SENT)
                .build();
        notification = notificationService.createNotification(recipient.getTenantId(), notification);
        return notification;
    }

    private String formatNotificationText(String template, User recipient) {
        Map<String, String> context = Map.of(
                "recipientEmail", recipient.getEmail(),
                "recipientFirstName", Strings.nullToEmpty(recipient.getFirstName()),
                "recipientLastName", Strings.nullToEmpty(recipient.getLastName())
        );
        return TbNodeUtils.processTemplate(template, context);
    }

    // handle markAsRead and deleteNotificationRequest - send UnreadNotificationsUpdate with updated list

}
