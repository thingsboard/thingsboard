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
package org.thingsboard.server.service.ws.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.id.IdBased;
import org.thingsboard.server.common.data.id.NotificationId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.notification.Notification;
import org.thingsboard.server.common.data.notification.NotificationStatus;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.dao.notification.NotificationService;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.dao.notification.NotificationProcessingService;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.subscription.NotificationsSubscription;
import org.thingsboard.server.service.subscription.TbLocalSubscriptionService;
import org.thingsboard.server.service.ws.WebSocketSessionRef;
import org.thingsboard.server.service.ws.notification.cmd.MarkNotificationAsReadCmd;
import org.thingsboard.server.service.ws.notification.cmd.NotificationsSubCmd;
import org.thingsboard.server.service.ws.notification.cmd.UnreadNotificationsUpdate;
import org.thingsboard.server.service.ws.notification.sub.NotificationsSubscriptionUpdate;
import org.thingsboard.server.service.ws.telemetry.WebSocketService;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.UnsubscribeCmd;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@TbCoreComponent
@RequiredArgsConstructor
public class DefaultNotificationCommandsHandler implements NotificationCommandsHandler {

    private final NotificationService notificationService;
    private final WebSocketService wsService;
    private final TbLocalSubscriptionService localSubscriptionService;
    private final NotificationProcessingService notificationProcessingService;
    private final TbServiceInfoProvider serviceInfoProvider;

    @Override
    public void handleUnreadNotificationsSubCmd(WebSocketSessionRef sessionRef, NotificationsSubCmd cmd) {
        SecurityUser user = sessionRef.getSecurityCtx();
        NotificationsSubscription subscription = NotificationsSubscription.builder()
                .serviceId(serviceInfoProvider.getServiceId())
                .sessionId(sessionRef.getSessionId())
                .subscriptionId(cmd.getCmdId())
                .tenantId(user.getTenantId())
                .entityId(user.getId())
                .updateProcessor(this::handleSubscriptionUpdate)
                .limit(cmd.getLimit())
                .build();
        localSubscriptionService.addSubscription(subscription);

        fetchUnreadNotifications(subscription);
        sendUpdate(sessionRef.getSessionId(), subscription.createFullUpdate());
    }

    @Override
    public void handleMarkAsReadCmd(WebSocketSessionRef sessionRef, MarkNotificationAsReadCmd cmd) {
        NotificationId notificationId = new NotificationId(cmd.getNotificationId());
        notificationProcessingService.markNotificationAsRead(sessionRef.getSecurityCtx().getTenantId(), sessionRef.getSecurityCtx().getId(), notificationId);
    }

    @Override
    public void handleUnsubCmd(WebSocketSessionRef sessionRef, UnsubscribeCmd cmd) {
        localSubscriptionService.cancelSubscription(sessionRef.getSessionId(), cmd.getCmdId());
    }

    private void fetchUnreadNotifications(NotificationsSubscription subscription) {
        PageData<Notification> notifications = notificationService.findLatestUnreadNotificationsByUserId(subscription.getTenantId(),
                (UserId) subscription.getEntityId(), subscription.getLimit());
        subscription.getUnreadNotifications().clear();
        subscription.getUnreadNotifications().putAll(notifications.getData().stream().collect(Collectors.toMap(IdBased::getUuidId, n -> n)));
        subscription.getTotalUnreadCount().set((int) notifications.getTotalElements());
    }

    private void handleSubscriptionUpdate(NotificationsSubscription subscription, NotificationsSubscriptionUpdate subscriptionUpdate) {
        if (subscriptionUpdate.getNotification() != null) {
            Notification notification = subscriptionUpdate.getNotification();
            if (notification.getStatus() == NotificationStatus.READ) {
                fetchUnreadNotifications(subscription);
                sendUpdate(subscription.getSessionId(), subscription.createFullUpdate());
            } else {
                Notification previous = subscription.getUnreadNotifications().put(notification.getUuidId(), notification);
                if (previous == null) {
                    subscription.getTotalUnreadCount().incrementAndGet();
                    Set<UUID> beyondLimit = subscription.getUnreadNotifications().keySet().stream()
                            .skip(subscription.getLimit())
                            .collect(Collectors.toSet());
                    beyondLimit.forEach(notificationId -> subscription.getUnreadNotifications().remove(notificationId));
                }
                sendUpdate(subscription.getSessionId(), subscription.createPartialUpdate(notification));
            }
        } else if (subscriptionUpdate.isNotificationRequestDeleted()) {
            if (subscription.getUnreadNotifications().values().stream()
                    .anyMatch(notification -> notification.getRequestId().equals(subscriptionUpdate.getNotificationRequestId()))) {
                fetchUnreadNotifications(subscription);
                sendUpdate(subscription.getSessionId(), subscription.createFullUpdate());
            }
        }
    }

    private void sendUpdate(String sessionId, UnreadNotificationsUpdate update) {
        wsService.sendWsMsg(sessionId, update);
    }

}
