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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.NotificationId;
import org.thingsboard.server.common.data.id.NotificationRequestId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.notification.Notification;
import org.thingsboard.server.common.data.notification.NotificationRequest;
import org.thingsboard.server.common.data.notification.NotificationRequestConfig;
import org.thingsboard.server.common.data.notification.NotificationStatus;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.dao.notification.NotificationProcessingService;
import org.thingsboard.server.dao.notification.NotificationService;
import org.thingsboard.server.dao.notification.NotificationTargetService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.discovery.NotificationsTopicService;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.service.executors.DbCallbackExecutorService;
import org.thingsboard.server.service.subscription.TbSubscriptionUtils;
import org.thingsboard.server.service.telemetry.AbstractSubscriptionService;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
public class DefaultNotificationProcessingService extends AbstractSubscriptionService implements NotificationProcessingService {

    private final NotificationTargetService notificationTargetService;
    private final NotificationService notificationService;
    private final UserService userService;
    private final DbCallbackExecutorService dbCallbackExecutorService;
    private final NotificationsTopicService notificationsTopicService;

    public DefaultNotificationProcessingService(TbClusterService clusterService, PartitionService partitionService,
                                                NotificationTargetService notificationTargetService,
                                                NotificationService notificationService, UserService userService,
                                                DbCallbackExecutorService dbCallbackExecutorService,
                                                NotificationsTopicService notificationsTopicService) {
        super(clusterService, partitionService);
        this.notificationTargetService = notificationTargetService;
        this.notificationService = notificationService;
        this.userService = userService;
        this.dbCallbackExecutorService = dbCallbackExecutorService;
        this.notificationsTopicService = notificationsTopicService;
    }

    @Override
    public NotificationRequest processNotificationRequest(TenantId tenantId, NotificationRequest notificationRequest) {
        List<UserId> recipientsIds = notificationTargetService.findRecipientsForNotificationTarget(tenantId, notificationRequest.getTargetId());
        List<User> recipients = new ArrayList<>();
        for (UserId recipientId : recipientsIds) {
            User recipient = userService.findUserById(tenantId, recipientId); // todo: add caching
            recipients.add(recipient);
        }

        notificationRequest.setTenantId(tenantId);
        NotificationRequest savedNotificationRequest = notificationService.createNotificationRequest(tenantId, notificationRequest);

        if (notificationRequest.getAdditionalConfig() != null) {
            NotificationRequestConfig config = notificationRequest.getAdditionalConfig();
            // todo: delayed sending; check all delayed notification requests on start up, schedule send
        }

        for (User recipient : recipients) {
            dbCallbackExecutorService.submit(() -> {
                Notification notification = createNotification(recipient, savedNotificationRequest);
                onNotificationUpdate(recipient.getTenantId(), recipient.getId(), notification);
            });
        }

        return savedNotificationRequest;
    }

    @Override
    public void markNotificationAsRead(TenantId tenantId, UserId recipientId, NotificationId notificationId) {
        Notification notification = notificationService.updateNotificationStatus(tenantId, notificationId, NotificationStatus.READ);
        onNotificationUpdate(tenantId, recipientId, notification);
    }

    @Override
    public void deleteNotificationRequest(TenantId tenantId, NotificationRequestId notificationRequestId) {
        notificationService.deleteNotificationRequest(tenantId, notificationRequestId);
        onNotificationRequestDeleted(tenantId, notificationRequestId);
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
        return notificationService.createNotification(recipient.getTenantId(), notification);
    }

    private String formatNotificationText(String template, User recipient) {
        Map<String, String> context = Map.of(
                "recipientEmail", recipient.getEmail(),
                "recipientFirstName", Strings.nullToEmpty(recipient.getFirstName()),
                "recipientLastName", Strings.nullToEmpty(recipient.getLastName())
        );
        return TbNodeUtils.processTemplate(template, context);
    }

    private void onNotificationUpdate(TenantId tenantId, UserId recipientId, Notification notification) {
        forwardToSubscriptionManagerServiceOrSendToCore(tenantId, recipientId, subscriptionManagerService -> {
            subscriptionManagerService.onNotificationUpdate(tenantId, recipientId, notification, TbCallback.EMPTY);
        }, () -> {
            return TbSubscriptionUtils.notificationUpdateToProto(tenantId, recipientId, notification);
        });
    }

    public void onNotificationRequestDeleted(TenantId tenantId, NotificationRequestId notificationRequestId) {
        TransportProtos.ToCoreMsg notificationRequestDeletedProto = TbSubscriptionUtils.notificationRequestDeletedToProto(tenantId, notificationRequestId);
        Set<String> coreServices = new HashSet<>(partitionService.getAllServiceIds(ServiceType.TB_CORE));
        for (String serviceId : coreServices) {
            TopicPartitionInfo tpi = notificationsTopicService.getNotificationsTopic(ServiceType.TB_CORE, serviceId);
            clusterService.pushMsgToCore(tpi, UUID.randomUUID(), notificationRequestDeletedProto, null);
        }
    }

    @Override
    protected String getExecutorPrefix() {
        return "notification";
    }

}
