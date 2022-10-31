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
package org.thingsboard.server.service.ws.notification.sub;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.id.IdBased;
import org.thingsboard.server.common.data.id.NotificationId;
import org.thingsboard.server.common.data.id.NotificationRequestId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.notification.Notification;
import org.thingsboard.server.common.data.notification.NotificationStatus;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.dao.notification.NotificationService;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.discovery.NotificationsTopicService;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.notification.NotificationProcessingService;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.subscription.TbLocalSubscriptionService;
import org.thingsboard.server.service.subscription.TbSubscriptionUtils;
import org.thingsboard.server.service.telemetry.AbstractSubscriptionService;
import org.thingsboard.server.service.ws.WebSocketSessionRef;
import org.thingsboard.server.service.ws.notification.cmd.MarkNotificationAsReadCmd;
import org.thingsboard.server.service.ws.notification.cmd.NotificationsSubCmd;
import org.thingsboard.server.service.ws.notification.cmd.UnreadNotificationsUpdate;
import org.thingsboard.server.service.ws.telemetry.WebSocketService;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.UnsubscribeCmd;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@TbCoreComponent
@Slf4j
public class DefaultNotificationsSubscriptionService extends AbstractSubscriptionService implements NotificationsSubscriptionService {

    private final WebSocketService wsService;
    private final TbLocalSubscriptionService localSubscriptionService;
    private final NotificationService notificationService;
    private final TbServiceInfoProvider serviceInfoProvider;
    private final NotificationsTopicService notificationsTopicService;
    private final NotificationProcessingService notificationProcessingService;

    public DefaultNotificationsSubscriptionService(TbClusterService clusterService, PartitionService partitionService,
                                                   @Lazy WebSocketService wsService, TbLocalSubscriptionService localSubscriptionService,
                                                   NotificationService notificationService, TbServiceInfoProvider serviceInfoProvider,
                                                   NotificationsTopicService notificationsTopicService,
                                                   @Lazy NotificationProcessingService notificationProcessingService) {
        super(clusterService, partitionService);
        this.wsService = wsService;
        this.localSubscriptionService = localSubscriptionService;
        this.notificationService = notificationService;
        this.serviceInfoProvider = serviceInfoProvider;
        this.notificationsTopicService = notificationsTopicService;
        this.notificationProcessingService = notificationProcessingService;
    }

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
        notificationProcessingService.markNotificationAsRead(sessionRef.getSecurityCtx(), notificationId);
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

    @Override
    public void onNewNotification(TenantId tenantId, UserId recipientId, Notification notification) {
        onNotificationUpdate(tenantId, recipientId, notification);
    }

    @Override
    public void onNotificationUpdated(TenantId tenantId, UserId recipientId, Notification notification) {
        onNotificationUpdate(tenantId, recipientId, notification);
    }

    private void onNotificationUpdate(TenantId tenantId, UserId recipientId, Notification notification) {
        forwardToSubscriptionManagerServiceOrSendToCore(tenantId, recipientId, subscriptionManagerService -> {
            subscriptionManagerService.onNotificationUpdate(tenantId, recipientId, notification, TbCallback.EMPTY);
        }, () -> {
            return TbSubscriptionUtils.notificationUpdateToProto(tenantId, recipientId, notification);
        });
    }

    @Override
    public void onNotificationRequestDeleted(TenantId tenantId, NotificationRequestId notificationRequestId) {
        TransportProtos.ToCoreMsg notificationRequestDeletedProto = TbSubscriptionUtils.notificationRequestDeletedToProto(tenantId, notificationRequestId);
        Set<String> coreServices = new HashSet<>(partitionService.getAllServiceIds(ServiceType.TB_CORE));
        for (String serviceId : coreServices) {
            TopicPartitionInfo tpi = notificationsTopicService.getNotificationsTopic(ServiceType.TB_CORE, serviceId);
            clusterService.pushMsgToCore(tpi, UUID.randomUUID(), notificationRequestDeletedProto, null);
        }
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
        wsService.sendWsMsg(sessionId, update);;
    }

    @Override
    protected String getExecutorPrefix() {
        return "notification";
    }

}
