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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.id.IdBased;
import org.thingsboard.server.common.data.id.NotificationId;
import org.thingsboard.server.common.data.id.NotificationRequestId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.notification.Notification;
import org.thingsboard.server.common.data.notification.NotificationInfo;
import org.thingsboard.server.common.data.notification.NotificationStatus;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.dao.notification.NotificationService;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.rule.engine.api.NotificationManager;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.ws.notification.cmd.NotificationsCountSubCmd;
import org.thingsboard.server.service.ws.notification.sub.NotificationRequestUpdate;
import org.thingsboard.server.service.ws.notification.sub.NotificationUpdate;
import org.thingsboard.server.service.ws.notification.sub.NotificationsSubscription;
import org.thingsboard.server.service.subscription.TbLocalSubscriptionService;
import org.thingsboard.server.service.ws.WebSocketSessionRef;
import org.thingsboard.server.service.ws.notification.cmd.MarkNotificationsAsReadCmd;
import org.thingsboard.server.service.ws.notification.cmd.NotificationsSubCmd;
import org.thingsboard.server.service.ws.notification.sub.NotificationsSubscriptionUpdate;
import org.thingsboard.server.service.ws.notification.sub.NotificationsCountSubscription;
import org.thingsboard.server.service.ws.WebSocketService;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.CmdUpdate;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.UnsubscribeCmd;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@TbCoreComponent
@RequiredArgsConstructor
@Slf4j
public class DefaultNotificationCommandsHandler implements NotificationCommandsHandler {

    private final NotificationService notificationService;
    private final TbLocalSubscriptionService localSubscriptionService;
    private final NotificationManager notificationManager;
    private final TbServiceInfoProvider serviceInfoProvider;
    @Autowired @Lazy
    private WebSocketService wsService;

    @Override
    public void handleUnreadNotificationsSubCmd(WebSocketSessionRef sessionRef, NotificationsSubCmd cmd) {
        log.debug("[{}] Handling unread notifications subscription cmd (cmdId: {})", sessionRef.getSessionId(), cmd.getCmdId());
        SecurityUser securityCtx = sessionRef.getSecurityCtx();
        NotificationsSubscription subscription = NotificationsSubscription.builder()
                .serviceId(serviceInfoProvider.getServiceId())
                .sessionId(sessionRef.getSessionId())
                .subscriptionId(cmd.getCmdId())
                .tenantId(securityCtx.getTenantId())
                .entityId(securityCtx.getId())
                .updateProcessor(this::handleNotificationsSubscriptionUpdate)
                .limit(cmd.getLimit())
                .build();
        localSubscriptionService.addSubscription(subscription);

        fetchUnreadNotifications(subscription);
        sendUpdate(sessionRef.getSessionId(), subscription.createFullUpdate());
    }

    @Override
    public void handleUnreadNotificationsCountSubCmd(WebSocketSessionRef sessionRef, NotificationsCountSubCmd cmd) {
        log.debug("[{}] Handling unread notifications count subscription cmd (cmdId: {})", sessionRef.getSessionId(), cmd.getCmdId());
        SecurityUser securityCtx = sessionRef.getSecurityCtx();
        NotificationsCountSubscription subscription = NotificationsCountSubscription.builder()
                .serviceId(serviceInfoProvider.getServiceId())
                .sessionId(sessionRef.getSessionId())
                .subscriptionId(cmd.getCmdId())
                .tenantId(securityCtx.getTenantId())
                .entityId(securityCtx.getId())
                .updateProcessor(this::handleNotificationsCountSubscriptionUpdate)
                .build();
        localSubscriptionService.addSubscription(subscription);

        fetchUnreadNotificationsCount(subscription);
        sendUpdate(sessionRef.getSessionId(), subscription.createUpdate());
    }

    private void fetchUnreadNotifications(NotificationsSubscription subscription) {
        log.trace("[{}, subId: {}] Fetching unread notifications from DB", subscription.getSessionId(), subscription.getSubscriptionId());
        PageData<Notification> notifications = notificationService.findLatestUnreadNotificationsByUserId(subscription.getTenantId(),
                (UserId) subscription.getEntityId(), subscription.getLimit());
        subscription.getUnreadNotifications().clear();
        subscription.getUnreadNotifications().putAll(notifications.getData().stream().collect(Collectors.toMap(IdBased::getUuidId, n -> n)));
        subscription.getTotalUnreadCounter().set((int) notifications.getTotalElements());
    }

    private void fetchUnreadNotificationsCount(NotificationsCountSubscription subscription) {
        log.trace("[{}, subId: {}] Fetching unread notifications count from DB", subscription.getSessionId(), subscription.getSubscriptionId());
        int unreadCount = notificationService.countUnreadNotificationsByUserId(subscription.getTenantId(), (UserId) subscription.getEntityId());
        subscription.getUnreadCounter().set(unreadCount);
    }


    /* Notifications subscription update handling */
    private void handleNotificationsSubscriptionUpdate(NotificationsSubscription subscription, NotificationsSubscriptionUpdate subscriptionUpdate) {
        if (subscriptionUpdate.getNotificationUpdate() != null) {
            handleNotificationUpdate(subscription, subscriptionUpdate.getNotificationUpdate());
        } else if (subscriptionUpdate.getNotificationRequestUpdate() != null) {
            handleNotificationRequestUpdate(subscription, subscriptionUpdate.getNotificationRequestUpdate());
        }
    }

    private void handleNotificationUpdate(NotificationsSubscription subscription, NotificationUpdate update) {
        log.trace("[{}, subId: {}] Handling notification update: {}", subscription.getSessionId(), subscription.getSubscriptionId(), update);
        Notification notification = update.getNotification();
        if (notification.getStatus() == NotificationStatus.READ) {
            fetchUnreadNotifications(subscription);
            sendUpdate(subscription.getSessionId(), subscription.createFullUpdate());
        } else {
            subscription.getUnreadNotifications().put(notification.getUuidId(), notification);
            if (update.isNew()) {
                subscription.getTotalUnreadCounter().incrementAndGet();
                Set<UUID> beyondLimit = subscription.getUnreadNotifications().keySet().stream()
                        .skip(subscription.getLimit())
                        .collect(Collectors.toSet());
                beyondLimit.forEach(notificationId -> subscription.getUnreadNotifications().remove(notificationId));
            }
            sendUpdate(subscription.getSessionId(), subscription.createPartialUpdate(notification));
        }
    }

    private void handleNotificationRequestUpdate(NotificationsSubscription subscription, NotificationRequestUpdate update) {
        log.trace("[{}, subId: {}] Handling notification request update: {}", subscription.getSessionId(), subscription.getSubscriptionId(), update);
        NotificationRequestId notificationRequestId = update.getNotificationRequestId();
        if (update.isDeleted()) {
            if (subscription.getUnreadNotifications().values().stream()
                    .anyMatch(notification -> notification.getRequestId().equals(notificationRequestId))) {
                fetchUnreadNotifications(subscription);
                sendUpdate(subscription.getSessionId(), subscription.createFullUpdate());
            }
        } else {
            NotificationInfo notificationInfo = update.getNotificationInfo();
            subscription.getUnreadNotifications().values().stream()
                    .filter(notification -> notification.getRequestId().equals(notificationRequestId))
                    .forEach(notification -> {
                        notification.setInfo(notificationInfo);
                        sendUpdate(subscription.getSessionId(), subscription.createPartialUpdate(notification));
                    });
        }
    }


    /* Notifications count subscription update handling */
    private void handleNotificationsCountSubscriptionUpdate(NotificationsCountSubscription subscription, NotificationsSubscriptionUpdate subscriptionUpdate) {
        if (subscriptionUpdate.getNotificationUpdate() != null) {
            handleNotificationUpdate(subscription, subscriptionUpdate.getNotificationUpdate());
        } else if (subscriptionUpdate.getNotificationRequestUpdate() != null) {
            handleNotificationRequestUpdate(subscription, subscriptionUpdate.getNotificationRequestUpdate());
        }
    }

    private void handleNotificationUpdate(NotificationsCountSubscription subscription, NotificationUpdate update) {
        log.trace("[{}, subId: {}] Handling notification update for count sub: {}", subscription.getSessionId(), subscription.getSubscriptionId(), update);
        Notification notification = update.getNotification();
        if (update.isNew()) {
            subscription.getUnreadCounter().incrementAndGet();
        } else if (notification.getStatus() == NotificationStatus.READ) {
            // for now this can only happen when user marks notification as read
            subscription.getUnreadCounter().decrementAndGet();
        }
        sendUpdate(subscription.getSessionId(), subscription.createUpdate());
    }

    private void handleNotificationRequestUpdate(NotificationsCountSubscription subscription, NotificationRequestUpdate update) {
        log.trace("[{}, subId: {}] Handling notification request update for count sub: {}", subscription.getSessionId(), subscription.getSubscriptionId(), update);
        if (update.isDeleted()) {
            fetchUnreadNotificationsCount(subscription);
            sendUpdate(subscription.getSessionId(), subscription.createUpdate());
        }
    }


    private void sendUpdate(String sessionId, CmdUpdate update) {
        log.trace("[{}] Sending WS update: {}", sessionId, update);
        wsService.sendWsMsg(sessionId, update);
    }


    @Override
    public void handleMarkAsReadCmd(WebSocketSessionRef sessionRef, MarkNotificationsAsReadCmd cmd) {
        SecurityUser securityCtx = sessionRef.getSecurityCtx();
        cmd.getNotifications().stream()
                .map(NotificationId::new)
                .forEach(notificationId -> {
                    notificationManager.markNotificationAsRead(securityCtx.getTenantId(), securityCtx.getId(), notificationId);
                    // fixme: should send bulk update event, not a separate event for each notification
                });
    }

    @Override
    public void handleUnsubCmd(WebSocketSessionRef sessionRef, UnsubscribeCmd cmd) {
        localSubscriptionService.cancelSubscription(sessionRef.getSessionId(), cmd.getCmdId());
    }

}
