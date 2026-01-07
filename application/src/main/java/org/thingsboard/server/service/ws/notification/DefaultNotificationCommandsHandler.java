/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
import org.thingsboard.rule.engine.api.NotificationCenter;
import org.thingsboard.server.common.data.id.IdBased;
import org.thingsboard.server.common.data.id.NotificationId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.notification.Notification;
import org.thingsboard.server.common.data.notification.NotificationStatus;
import org.thingsboard.server.common.data.notification.NotificationType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.dao.notification.NotificationService;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.subscription.TbLocalSubscriptionService;
import org.thingsboard.server.service.subscription.TbSubscription;
import org.thingsboard.server.service.ws.WebSocketService;
import org.thingsboard.server.service.ws.WebSocketSessionRef;
import org.thingsboard.server.service.ws.notification.cmd.MarkAllNotificationsAsReadCmd;
import org.thingsboard.server.service.ws.notification.cmd.MarkNotificationsAsReadCmd;
import org.thingsboard.server.service.ws.notification.cmd.NotificationsCountSubCmd;
import org.thingsboard.server.service.ws.notification.cmd.NotificationsSubCmd;
import org.thingsboard.server.service.ws.notification.sub.NotificationRequestUpdate;
import org.thingsboard.server.service.ws.notification.sub.NotificationUpdate;
import org.thingsboard.server.service.ws.notification.sub.NotificationsCountSubscription;
import org.thingsboard.server.service.ws.notification.sub.NotificationsSubscription;
import org.thingsboard.server.service.ws.notification.sub.NotificationsSubscriptionUpdate;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.CmdUpdate;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.UnsubscribeCmd;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.thingsboard.server.common.data.notification.NotificationDeliveryMethod.WEB;

@Service
@TbCoreComponent
@RequiredArgsConstructor
@Slf4j
public class DefaultNotificationCommandsHandler implements NotificationCommandsHandler {

    private final NotificationService notificationService;
    private final TbLocalSubscriptionService localSubscriptionService;
    private final NotificationCenter notificationCenter;
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
                .notificationTypes(cmd.getTypes())
                .build();
        localSubscriptionService.addSubscription(subscription, sessionRef);

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
        localSubscriptionService.addSubscription(subscription, sessionRef);

        fetchUnreadNotificationsCount(subscription);
        sendUpdate(sessionRef.getSessionId(), subscription.createUpdate());
    }

    private void fetchUnreadNotifications(NotificationsSubscription subscription) {
        log.trace("[{}, subId: {}] Fetching unread notifications from DB", subscription.getSessionId(), subscription.getSubscriptionId());
        PageData<Notification> notifications = notificationService.findLatestUnreadNotificationsByRecipientIdAndNotificationTypes(subscription.getTenantId(),
                WEB, (UserId) subscription.getEntityId(), subscription.getNotificationTypes(), subscription.getLimit());
        subscription.getLatestUnreadNotifications().clear();
        notifications.getData().forEach(notification -> {
            subscription.getLatestUnreadNotifications().put(notification.getUuidId(), notification);
        });
        subscription.getTotalUnreadCounter().set((int) notifications.getTotalElements());
    }

    private void fetchUnreadNotificationsCount(NotificationsCountSubscription subscription) {
        log.trace("[{}, subId: {}] Fetching unread notifications count from DB", subscription.getSessionId(), subscription.getSubscriptionId());
        int unreadCount = notificationService.countUnreadNotificationsByRecipientId(subscription.getTenantId(), WEB, (UserId) subscription.getEntityId());
        subscription.getTotalUnreadCounter().set(unreadCount);
    }


    /* Notifications subscription update handling */
    private void handleNotificationsSubscriptionUpdate(TbSubscription<NotificationsSubscriptionUpdate> sub, NotificationsSubscriptionUpdate subscriptionUpdate) {
        NotificationsSubscription subscription = (NotificationsSubscription) sub;
        try {
            if (subscriptionUpdate.getNotificationUpdate() != null) {
                handleNotificationUpdate(subscription, subscriptionUpdate.getNotificationUpdate());
            } else if (subscriptionUpdate.getNotificationRequestUpdate() != null) {
                handleNotificationRequestUpdate(subscription, subscriptionUpdate.getNotificationRequestUpdate());
            }
        } catch (Exception e) {
            log.error("[{}, subId: {}] Failed to handle update for notifications subscription: {}", subscription.getSessionId(), subscription.getSubscriptionId(), subscriptionUpdate, e);
        }
    }

    private void handleNotificationUpdate(NotificationsSubscription subscription, NotificationUpdate update) {
        log.trace("[{}, subId: {}] Handling notification update: {}", subscription.getSessionId(), subscription.getSubscriptionId(), update);
        Notification notification = update.getNotification();
        UUID notificationId = notification != null ? notification.getUuidId() : update.getNotificationId();
        NotificationType notificationType = notification != null ? notification.getType() : update.getNotificationType();
        if (notificationType != null && !subscription.checkNotificationType(notificationType)) {
            return;
        }

        if (update.isCreated()) {
            subscription.getLatestUnreadNotifications().put(notificationId, notification);
            subscription.getTotalUnreadCounter().incrementAndGet();
            if (subscription.getLatestUnreadNotifications().size() > subscription.getLimit()) {
                Set<UUID> beyondLimit = subscription.getSortedNotifications().stream().skip(subscription.getLimit())
                        .map(IdBased::getUuidId).collect(Collectors.toSet());
                beyondLimit.forEach(id -> subscription.getLatestUnreadNotifications().remove(id));
            }
            sendUpdate(subscription.getSessionId(), subscription.createPartialUpdate(notification));
        } else if (update.isUpdated()) {
            if (update.getNewStatus() == NotificationStatus.READ) {
                if (update.isAllNotifications() || subscription.getLatestUnreadNotifications().containsKey(notificationId)) {
                    fetchUnreadNotifications(subscription);
                    sendUpdate(subscription.getSessionId(), subscription.createFullUpdate());
                } else {
                    subscription.getTotalUnreadCounter().decrementAndGet();
                    sendUpdate(subscription.getSessionId(), subscription.createCountUpdate());
                }
            } else if (notification.getStatus() != NotificationStatus.READ) {
                if (subscription.getLatestUnreadNotifications().containsKey(notificationId)) {
                    subscription.getLatestUnreadNotifications().put(notificationId, notification);
                    sendUpdate(subscription.getSessionId(), subscription.createPartialUpdate(notification));
                }
            }
        } else if (update.isDeleted()) {
            if (subscription.getLatestUnreadNotifications().containsKey(notificationId)) {
                fetchUnreadNotifications(subscription);
                sendUpdate(subscription.getSessionId(), subscription.createFullUpdate());
            } else if (notification.getStatus() != NotificationStatus.READ) {
                subscription.getTotalUnreadCounter().decrementAndGet();
                sendUpdate(subscription.getSessionId(), subscription.createCountUpdate());
            }
        }
    }

    private void handleNotificationRequestUpdate(NotificationsSubscription subscription, NotificationRequestUpdate update) {
        log.trace("[{}, subId: {}] Handling notification request update: {}", subscription.getSessionId(), subscription.getSubscriptionId(), update);
        fetchUnreadNotifications(subscription);
        sendUpdate(subscription.getSessionId(), subscription.createFullUpdate());
    }


    /* Notifications count subscription update handling */
    private void handleNotificationsCountSubscriptionUpdate(TbSubscription<NotificationsSubscriptionUpdate> sub, NotificationsSubscriptionUpdate subscriptionUpdate) {
        NotificationsCountSubscription subscription = (NotificationsCountSubscription) sub;
        try {
            if (subscriptionUpdate.getNotificationUpdate() != null) {
                handleNotificationUpdate(subscription, subscriptionUpdate.getNotificationUpdate());
            } else if (subscriptionUpdate.getNotificationRequestUpdate() != null) {
                handleNotificationRequestUpdate(subscription, subscriptionUpdate.getNotificationRequestUpdate());
            }
        } catch (Exception e) {
            log.error("[{}, subId: {}] Failed to handle update for notifications count subscription: {}", subscription.getSessionId(), subscription.getSubscriptionId(), subscriptionUpdate, e);
        }
    }

    private void handleNotificationUpdate(NotificationsCountSubscription subscription, NotificationUpdate update) {
        log.trace("[{}, subId: {}] Handling notification update for count sub: {}", subscription.getSessionId(), subscription.getSubscriptionId(), update);
        if (update.isCreated()) {
            subscription.getTotalUnreadCounter().incrementAndGet();
            sendUpdate(subscription.getSessionId(), subscription.createUpdate());
        } else if (update.isUpdated()) {
            if (update.getNewStatus() == NotificationStatus.READ) {
                if (update.isAllNotifications()) {
                    fetchUnreadNotificationsCount(subscription);
                } else {
                    subscription.getTotalUnreadCounter().decrementAndGet();
                }
                sendUpdate(subscription.getSessionId(), subscription.createUpdate());
            }
        } else if (update.isDeleted()) {
            if (update.getNotification().getStatus() != NotificationStatus.READ) {
                subscription.getTotalUnreadCounter().decrementAndGet();
                sendUpdate(subscription.getSessionId(), subscription.createUpdate());
            }
        }
    }

    private void handleNotificationRequestUpdate(NotificationsCountSubscription subscription, NotificationRequestUpdate update) {
        log.trace("[{}, subId: {}] Handling notification request update for count sub: {}", subscription.getSessionId(), subscription.getSubscriptionId(), update);
        fetchUnreadNotificationsCount(subscription);
        sendUpdate(subscription.getSessionId(), subscription.createUpdate());
    }


    @Override
    public void handleMarkAsReadCmd(WebSocketSessionRef sessionRef, MarkNotificationsAsReadCmd cmd) {
        SecurityUser securityCtx = sessionRef.getSecurityCtx();
        cmd.getNotifications().stream()
                .map(NotificationId::new)
                .forEach(notificationId -> {
                    notificationCenter.markNotificationAsRead(securityCtx.getTenantId(), securityCtx.getId(), notificationId);
                });
    }

    @Override
    public void handleMarkAllAsReadCmd(WebSocketSessionRef sessionRef, MarkAllNotificationsAsReadCmd cmd) {
        SecurityUser securityCtx = sessionRef.getSecurityCtx();
        notificationCenter.markAllNotificationsAsRead(securityCtx.getTenantId(), WEB, securityCtx.getId());
    }

    @Override
    public void handleUnsubCmd(WebSocketSessionRef sessionRef, UnsubscribeCmd cmd) {
        localSubscriptionService.cancelSubscription(sessionRef.getTenantId(), sessionRef.getSessionId(), cmd.getCmdId());
    }

    private void sendUpdate(String sessionId, CmdUpdate update) {
        log.trace("[{}, cmdId: {}] Sending WS update: {}", sessionId, update.getCmdId(), update);
        wsService.sendUpdate(sessionId, update);
    }

}
