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
import org.thingsboard.rule.engine.api.NotificationManager;
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
import org.thingsboard.server.common.data.notification.NotificationRequestStatus;
import org.thingsboard.server.common.data.notification.NotificationStatus;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.notification.NotificationRequestService;
import org.thingsboard.server.dao.notification.NotificationService;
import org.thingsboard.server.dao.notification.NotificationTargetService;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.NotificationsTopicService;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.provider.TbQueueProducerProvider;
import org.thingsboard.server.service.executors.DbCallbackExecutorService;
import org.thingsboard.server.service.subscription.TbSubscriptionUtils;
import org.thingsboard.server.service.telemetry.AbstractSubscriptionService;
import org.thingsboard.server.service.ws.notification.sub.NotificationRequestUpdate;
import org.thingsboard.server.service.ws.notification.sub.NotificationUpdate;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
public class DefaultNotificationManager extends AbstractSubscriptionService implements NotificationManager {

    private final NotificationTargetService notificationTargetService;
    private final NotificationRequestService notificationRequestService;
    private final NotificationService notificationService;
    private final DbCallbackExecutorService dbCallbackExecutorService;
    private final NotificationsTopicService notificationsTopicService;
    private final TbQueueProducerProvider producerProvider;

    public DefaultNotificationManager(TbClusterService clusterService, PartitionService partitionService,
                                      NotificationTargetService notificationTargetService,
                                      NotificationRequestService notificationRequestService,
                                      NotificationService notificationService,
                                      DbCallbackExecutorService dbCallbackExecutorService,
                                      NotificationsTopicService notificationsTopicService,
                                      TbQueueProducerProvider producerProvider) {
        super(clusterService, partitionService);
        this.notificationTargetService = notificationTargetService;
        this.notificationRequestService = notificationRequestService;
        this.notificationService = notificationService;
        this.dbCallbackExecutorService = dbCallbackExecutorService;
        this.notificationsTopicService = notificationsTopicService;
        this.producerProvider = producerProvider;
    }

    @Override
    public NotificationRequest processNotificationRequest(TenantId tenantId, NotificationRequest notificationRequest) {
        log.debug("Processing notification request (tenant id: {}, notification target id: {})", tenantId, notificationRequest.getTargetId());
        notificationRequest.setTenantId(tenantId);
        if (notificationRequest.getAdditionalConfig() != null) {
            NotificationRequestConfig config = notificationRequest.getAdditionalConfig();
            if (config.getSendingDelayInMinutes() > 0 && notificationRequest.getId() == null) {
                notificationRequest.setStatus(NotificationRequestStatus.SCHEDULED);
                NotificationRequest savedNotificationRequest = notificationRequestService.saveNotificationRequest(tenantId, notificationRequest);
                forwardToNotificationSchedulerService(tenantId, savedNotificationRequest.getId(), false);
                return savedNotificationRequest;
            }
        }

        notificationRequest.setStatus(NotificationRequestStatus.PROCESSED);
        NotificationRequest savedNotificationRequest = notificationRequestService.saveNotificationRequest(tenantId, notificationRequest);

        DaoUtil.processBatches(pageLink -> {
            return notificationTargetService.findRecipientsForNotificationTarget(tenantId, notificationRequest.getTargetId(), pageLink);
        }, 100, recipients -> {
            dbCallbackExecutorService.submit(() -> {
                log.debug("Sending notifications for request {} to recipients batch", savedNotificationRequest.getId());
                for (User recipient : recipients) {
                    try {
                        Notification notification = createNotification(recipient, savedNotificationRequest);
                        onNotificationUpdate(recipient.getTenantId(), recipient.getId(), notification, true);
                    } catch (Exception e) {
                        log.error("Failed to create notification for recipient {}", recipient.getId(), e);
                    }
                }
            });
        });

        return savedNotificationRequest;
    }

    private void forwardToNotificationSchedulerService(TenantId tenantId, NotificationRequestId notificationRequestId, boolean deleted) {
        TransportProtos.NotificationSchedulerServiceMsg.Builder msg = TransportProtos.NotificationSchedulerServiceMsg.newBuilder()
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .setRequestIdMSB(notificationRequestId.getId().getMostSignificantBits())
                .setRequestIdLSB(notificationRequestId.getId().getLeastSignificantBits())
                .setTs(System.currentTimeMillis())
                .setDeleted(deleted);
        TransportProtos.ToCoreMsg toCoreMsg = TransportProtos.ToCoreMsg.newBuilder()
                .setNotificationSchedulerServiceMsg(msg)
                .build();
        clusterService.pushMsgToCore(tenantId, notificationRequestId, toCoreMsg, null);
    }

    @Override
    public void markNotificationAsRead(TenantId tenantId, UserId recipientId, NotificationId notificationId) {
        boolean updated = notificationService.markNotificationAsRead(tenantId, recipientId, notificationId);
        if (updated) {
            log.debug("Marking notification {} as read (recipient id: {}, tenant id: {})", notificationId, recipientId, tenantId);
            Notification notification = notificationService.findNotificationById(tenantId, notificationId);
            onNotificationUpdate(tenantId, recipientId, notification, false);
        }
    }

    @Override
    public void deleteNotificationRequest(TenantId tenantId, NotificationRequestId notificationRequestId) {
        log.debug("Deleting notification request {}", notificationRequestId);
        notificationRequestService.deleteNotificationRequestById(tenantId, notificationRequestId);
        onNotificationRequestUpdate(tenantId, NotificationRequestUpdate.builder()
                .notificationRequestId(notificationRequestId)
                .deleted(true)
                .build());
        forwardToNotificationSchedulerService(tenantId, notificationRequestId, true);
    }

    @Override
    public void updateNotificationRequest(TenantId tenantId, NotificationRequest notificationRequest) {
        log.debug("Updating notification request {}", notificationRequest.getId());
        notificationRequestService.saveNotificationRequest(tenantId, notificationRequest);
        notificationService.updateNotificationsInfosByRequestId(tenantId, notificationRequest.getId(), notificationRequest.getNotificationInfo());
        onNotificationRequestUpdate(tenantId, NotificationRequestUpdate.builder()
                .notificationRequestId(notificationRequest.getId())
                .notificationInfo(notificationRequest.getNotificationInfo())
                .deleted(false)
                .build());
    }

    private Notification createNotification(User recipient, NotificationRequest notificationRequest) {
        log.trace("Creating notification for recipient {} (notification request id: {})", recipient.getId(), notificationRequest.getId());
        Notification notification = Notification.builder()
                .requestId(notificationRequest.getId())
                .recipientId(recipient.getId())
                .reason(notificationRequest.getNotificationReason())
                .text(formatNotificationText(notificationRequest.getTextTemplate(), recipient))
                .info(notificationRequest.getNotificationInfo())
                .severity(notificationRequest.getNotificationSeverity())
                .originatorType(notificationRequest.getOriginatorType())
                .status(NotificationStatus.SENT)
                .build();
        return notificationService.saveNotification(recipient.getTenantId(), notification);
    }

    private String formatNotificationText(String template, User recipient) {
        Map<String, String> context = Map.of(
                "recipientEmail", recipient.getEmail(),
                "recipientFirstName", Strings.nullToEmpty(recipient.getFirstName()),
                "recipientLastName", Strings.nullToEmpty(recipient.getLastName())
        );
        return TbNodeUtils.processTemplate(template, context);
    }

    private void onNotificationUpdate(TenantId tenantId, UserId recipientId, Notification notification, boolean isNew) {
        NotificationUpdate update = NotificationUpdate.builder()
                .notification(notification)
                .isNew(isNew)
                .build();
        log.trace("Submitting notification update for recipient {}: {}", recipientId, update);
        wsCallBackExecutor.submit(() -> {
            forwardToSubscriptionManagerService(tenantId, recipientId, subscriptionManagerService -> {
                subscriptionManagerService.onNotificationUpdate(tenantId, recipientId, update, TbCallback.EMPTY);
            }, () -> {
                return TbSubscriptionUtils.notificationUpdateToProto(tenantId, recipientId, update);
            });
        });
    }

    private void onNotificationRequestUpdate(TenantId tenantId, NotificationRequestUpdate update) {
        log.trace("Submitting notification request update: {}", update);
        wsCallBackExecutor.submit(() -> {
            TransportProtos.ToCoreNotificationMsg notificationRequestUpdateProto = TbSubscriptionUtils.notificationRequestUpdateToProto(tenantId, update);
            Set<String> coreServices = new HashSet<>(partitionService.getAllServiceIds(ServiceType.TB_CORE));
            for (String serviceId : coreServices) {
                TopicPartitionInfo tpi = notificationsTopicService.getNotificationsTopic(ServiceType.TB_CORE, serviceId);
                producerProvider.getTbCoreNotificationsMsgProducer().send(tpi, new TbProtoQueueMsg<>(UUID.randomUUID(), notificationRequestUpdateProto), null);
            }
        });
    }

    @Override
    protected String getExecutorPrefix() {
        return "notification";
    }

}
