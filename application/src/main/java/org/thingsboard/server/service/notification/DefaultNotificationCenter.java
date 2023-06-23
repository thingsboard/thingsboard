/**
 * Copyright © 2016-2023 The Thingsboard Authors
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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.rule.engine.api.NotificationCenter;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.NotificationId;
import org.thingsboard.server.common.data.id.NotificationRequestId;
import org.thingsboard.server.common.data.id.NotificationRuleId;
import org.thingsboard.server.common.data.id.NotificationTargetId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.limit.LimitedApi;
import org.thingsboard.server.common.data.notification.AlreadySentException;
import org.thingsboard.server.common.data.notification.Notification;
import org.thingsboard.server.common.data.notification.NotificationDeliveryMethod;
import org.thingsboard.server.common.data.notification.NotificationRequest;
import org.thingsboard.server.common.data.notification.NotificationRequestConfig;
import org.thingsboard.server.common.data.notification.NotificationRequestStats;
import org.thingsboard.server.common.data.notification.NotificationRequestStatus;
import org.thingsboard.server.common.data.notification.NotificationStatus;
import org.thingsboard.server.common.data.notification.info.RuleOriginatedNotificationInfo;
import org.thingsboard.server.common.data.notification.settings.NotificationSettings;
import org.thingsboard.server.common.data.notification.settings.UserNotificationSettings;
import org.thingsboard.server.common.data.notification.targets.NotificationRecipient;
import org.thingsboard.server.common.data.notification.targets.NotificationTarget;
import org.thingsboard.server.common.data.notification.targets.platform.PlatformUsersNotificationTargetConfig;
import org.thingsboard.server.common.data.notification.targets.slack.SlackNotificationTargetConfig;
import org.thingsboard.server.common.data.notification.template.DeliveryMethodNotificationTemplate;
import org.thingsboard.server.common.data.notification.template.NotificationTemplate;
import org.thingsboard.server.common.data.notification.template.WebDeliveryMethodNotificationTemplate;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.common.msg.tools.TbRateLimitsException;
import org.thingsboard.server.dao.notification.NotificationRequestService;
import org.thingsboard.server.dao.notification.NotificationService;
import org.thingsboard.server.dao.notification.NotificationSettingsService;
import org.thingsboard.server.dao.notification.NotificationTargetService;
import org.thingsboard.server.dao.notification.NotificationTemplateService;
import org.thingsboard.server.dao.util.limits.RateLimitService;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.NotificationsTopicService;
import org.thingsboard.server.queue.provider.TbQueueProducerProvider;
import org.thingsboard.server.service.executors.NotificationExecutorService;
import org.thingsboard.server.service.notification.channels.NotificationChannel;
import org.thingsboard.server.service.subscription.TbSubscriptionUtils;
import org.thingsboard.server.service.telemetry.AbstractSubscriptionService;
import org.thingsboard.server.service.ws.notification.sub.NotificationRequestUpdate;
import org.thingsboard.server.service.ws.notification.sub.NotificationUpdate;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@SuppressWarnings({"rawtypes"})
public class DefaultNotificationCenter extends AbstractSubscriptionService implements NotificationCenter, NotificationChannel<User, WebDeliveryMethodNotificationTemplate> {

    private final NotificationTargetService notificationTargetService;
    private final NotificationRequestService notificationRequestService;
    private final NotificationService notificationService;
    private final NotificationTemplateService notificationTemplateService;
    private final NotificationSettingsService notificationSettingsService;
    private final NotificationExecutorService notificationExecutor;
    private final NotificationsTopicService notificationsTopicService;
    private final TbQueueProducerProvider producerProvider;
    private final RateLimitService rateLimitService;

    private Map<NotificationDeliveryMethod, NotificationChannel> channels;

    @Override
    public NotificationRequest processNotificationRequest(TenantId tenantId, NotificationRequest request, Consumer<NotificationRequestStats> callback) {
        if (request.getRuleId() == null) {
            if (!rateLimitService.checkRateLimit(LimitedApi.NOTIFICATION_REQUESTS, tenantId)) {
                throw new TbRateLimitsException(EntityType.TENANT);
            }
        }

        NotificationTemplate notificationTemplate;
        if (request.getTemplateId() != null) {
            notificationTemplate = notificationTemplateService.findNotificationTemplateById(tenantId, request.getTemplateId());
        } else {
            notificationTemplate = request.getTemplate();
        }
        if (notificationTemplate == null) throw new IllegalArgumentException("Template is missing");

        Set<NotificationDeliveryMethod> deliveryMethods = new HashSet<>();
        List<NotificationTarget> targets = request.getTargets().stream().map(NotificationTargetId::new)
                .map(id -> notificationTargetService.findNotificationTargetById(tenantId, id))
                .collect(Collectors.toList());

        NotificationRuleId ruleId = request.getRuleId();
        notificationTemplate.getConfiguration().getDeliveryMethodsTemplates().forEach((deliveryMethod, template) -> {
            if (!template.isEnabled()) return;
            try {
                channels.get(deliveryMethod).check(tenantId);
            } catch (Exception e) {
                if (ruleId == null) {
                    throw new IllegalArgumentException(e.getMessage());
                } else {
                    return; // if originated by rule - just ignore delivery method
                }
            }
            if (ruleId == null) {
                if (targets.stream().noneMatch(target -> target.getConfiguration().getType().getSupportedDeliveryMethods().contains(deliveryMethod))) {
                    throw new IllegalArgumentException("Recipients for " + deliveryMethod.getName() + " delivery method not chosen");
                }
            }
            deliveryMethods.add(deliveryMethod);
        });
        if (deliveryMethods.isEmpty()) {
            throw new IllegalArgumentException("No delivery methods to send notification with");
        }

        if (request.getAdditionalConfig() != null) {
            NotificationRequestConfig config = request.getAdditionalConfig();
            if (config.getSendingDelayInSec() > 0 && request.getId() == null) {
                request.setStatus(NotificationRequestStatus.SCHEDULED);
                request = notificationRequestService.saveNotificationRequest(tenantId, request);
                forwardToNotificationSchedulerService(tenantId, request.getId());
                return request;
            }
        }
        NotificationSettings settings = notificationSettingsService.findNotificationSettings(tenantId);

        log.debug("Processing notification request (tenantId: {}, targets: {})", tenantId, request.getTargets());
        request.setStatus(NotificationRequestStatus.PROCESSING);
        request = notificationRequestService.saveNotificationRequest(tenantId, request);

        NotificationProcessingContext ctx = NotificationProcessingContext.builder()
                .tenantId(tenantId)
                .request(request)
                .deliveryMethods(deliveryMethods)
                .template(notificationTemplate)
                .settings(settings)
                .build();

        notificationExecutor.submit(() -> {
            for (NotificationTarget target : targets) {
                processForTarget(target, ctx);
            }

            NotificationRequestId requestId = ctx.getRequest().getId();
            log.debug("[{}] Notification request processing is finished", requestId);
            NotificationRequestStats stats = ctx.getStats();
            try {
                notificationRequestService.updateNotificationRequest(tenantId, requestId, NotificationRequestStatus.SENT, stats);
            } catch (Exception e) {
                log.error("[{}] Failed to update stats for notification request", requestId, e);
            }

            if (callback != null) {
                try {
                    callback.accept(stats);
                } catch (Exception e) {
                    log.error("Failed to process callback for notification request {}", requestId, e);
                }
            }
        });

        return request;
    }

    private void processForTarget(NotificationTarget target, NotificationProcessingContext ctx) {
        Iterable<? extends NotificationRecipient> recipients;
        switch (target.getConfiguration().getType()) {
            case PLATFORM_USERS: {
                PlatformUsersNotificationTargetConfig targetConfig = (PlatformUsersNotificationTargetConfig) target.getConfiguration();
                if (targetConfig.getUsersFilter().getType().isForRules()) {
                    recipients = new PageDataIterable<>(pageLink -> {
                        return notificationTargetService.findRecipientsForRuleNotificationTargetConfig(ctx.getTenantId(), targetConfig, (RuleOriginatedNotificationInfo) ctx.getRequest().getInfo(), pageLink);
                    }, 500);
                } else {
                    recipients = new PageDataIterable<>(pageLink -> {
                        return notificationTargetService.findRecipientsForNotificationTargetConfig(ctx.getTenantId(), targetConfig, pageLink);
                    }, 500);
                }
                break;
            }
            case SLACK: {
                SlackNotificationTargetConfig targetConfig = (SlackNotificationTargetConfig) target.getConfiguration();
                recipients = List.of(targetConfig.getConversation());
                break;
            }
            default: {
                recipients = Collections.emptyList();
            }
        }

        Set<NotificationDeliveryMethod> deliveryMethods = new HashSet<>(ctx.getDeliveryMethods());
        deliveryMethods.removeIf(deliveryMethod -> !target.getConfiguration().getType().getSupportedDeliveryMethods().contains(deliveryMethod));
        log.debug("[{}] Processing notification request for {} target ({}) for delivery methods {}", ctx.getRequest().getId(), target.getConfiguration().getType(), target.getId(), deliveryMethods);
        if (deliveryMethods.isEmpty()) {
            return;
        }

        for (NotificationRecipient recipient : recipients) {
            for (NotificationDeliveryMethod deliveryMethod : deliveryMethods) {
                try {
                    processForRecipient(deliveryMethod, recipient, ctx);
                    ctx.getStats().reportSent(deliveryMethod, recipient);
                } catch (Exception error) {
                    ctx.getStats().reportError(deliveryMethod, error, recipient);
                }
            }
        }
    }

    private void processForRecipient(NotificationDeliveryMethod deliveryMethod, NotificationRecipient recipient, NotificationProcessingContext ctx) throws Exception {
        if (ctx.getStats().contains(deliveryMethod, recipient.getId())) {
            throw new AlreadySentException();
        }
        if (recipient instanceof User && ctx.getRequest().getRuleId() != null) {
            UserNotificationSettings settings = notificationSettingsService.getUserNotificationSettings(ctx.getTenantId(), (User) recipient, false);
            Set<NotificationDeliveryMethod> enabledDeliveryMethods = settings.getEnabledDeliveryMethods(ctx.getRequest().getRuleId());
            if (!enabledDeliveryMethods.contains(deliveryMethod)) {
                throw new RuntimeException("User disabled " + deliveryMethod.getName() + " notifications of this type");
            }
        }

        NotificationChannel notificationChannel = channels.get(deliveryMethod);
        DeliveryMethodNotificationTemplate processedTemplate = ctx.getProcessedTemplate(deliveryMethod, recipient);

        log.trace("[{}] Sending {} notification for recipient {}", ctx.getRequest().getId(), deliveryMethod, recipient);
        notificationChannel.sendNotification(recipient, processedTemplate, ctx);
    }

    @Override
    public void sendNotification(User recipient, WebDeliveryMethodNotificationTemplate processedTemplate, NotificationProcessingContext ctx) throws Exception {
        NotificationRequest request = ctx.getRequest();
        Notification notification = Notification.builder()
                .requestId(request.getId())
                .recipientId(recipient.getId())
                .type(ctx.getNotificationTemplate().getNotificationType())
                .subject(processedTemplate.getSubject())
                .text(processedTemplate.getBody())
                .additionalConfig(processedTemplate.getAdditionalConfig())
                .info(request.getInfo())
                .status(NotificationStatus.SENT)
                .build();
        try {
            notification = notificationService.saveNotification(recipient.getTenantId(), notification);
        } catch (Exception e) {
            log.error("Failed to create notification for recipient {}", recipient.getId(), e);
            throw e;
        }

        NotificationUpdate update = NotificationUpdate.builder()
                .created(true)
                .notification(notification)
                .build();
        onNotificationUpdate(recipient.getTenantId(), recipient.getId(), update);
    }

    @Override
    public void markNotificationAsRead(TenantId tenantId, UserId recipientId, NotificationId notificationId) {
        boolean updated = notificationService.markNotificationAsRead(tenantId, recipientId, notificationId);
        if (updated) {
            log.trace("Marked notification {} as read (recipient id: {}, tenant id: {})", notificationId, recipientId, tenantId);
            NotificationUpdate update = NotificationUpdate.builder()
                    .updated(true)
                    .notificationId(notificationId.getId())
                    .newStatus(NotificationStatus.READ)
                    .build();
            onNotificationUpdate(tenantId, recipientId, update);
        }
    }

    @Override
    public void markAllNotificationsAsRead(TenantId tenantId, UserId recipientId) {
        int updatedCount = notificationService.markAllNotificationsAsRead(tenantId, recipientId);
        if (updatedCount > 0) {
            log.trace("Marked all notifications as read (recipient id: {}, tenant id: {})", recipientId, tenantId);
            NotificationUpdate update = NotificationUpdate.builder()
                    .updated(true)
                    .allNotifications(true)
                    .newStatus(NotificationStatus.READ)
                    .build();
            onNotificationUpdate(tenantId, recipientId, update);
        }
    }

    @Override
    public void deleteNotification(TenantId tenantId, UserId recipientId, NotificationId notificationId) {
        Notification notification = notificationService.findNotificationById(tenantId, notificationId);
        boolean deleted = notificationService.deleteNotification(tenantId, recipientId, notificationId);
        if (deleted) {
            NotificationUpdate update = NotificationUpdate.builder()
                    .deleted(true)
                    .notification(notification)
                    .build();
            onNotificationUpdate(tenantId, recipientId, update);
        }
    }

    @Override
    public Set<NotificationDeliveryMethod> getAvailableDeliveryMethods(TenantId tenantId) {
        return channels.values().stream()
                .filter(channel -> {
                    try {
                        channel.check(tenantId);
                        return true;
                    } catch (Exception e) {
                        return false;
                    }
                })
                .map(NotificationChannel::getDeliveryMethod)
                .collect(Collectors.toSet());
    }

    @Override
    public void check(TenantId tenantId) throws Exception {
    }

    @Override
    public void deleteNotificationRequest(TenantId tenantId, NotificationRequestId notificationRequestId) {
        log.debug("Deleting notification request {}", notificationRequestId);
        NotificationRequest notificationRequest = notificationRequestService.findNotificationRequestById(tenantId, notificationRequestId);
        notificationRequestService.deleteNotificationRequest(tenantId, notificationRequestId);

        if (notificationRequest.isSent()) {
            // TODO: no need to send request update for other than PLATFORM_USERS target type
            onNotificationRequestUpdate(tenantId, NotificationRequestUpdate.builder()
                    .notificationRequestId(notificationRequestId)
                    .deleted(true)
                    .build());
        } else if (notificationRequest.isScheduled()) {
            // TODO: just forward to scheduler service
            clusterService.broadcastEntityStateChangeEvent(tenantId, notificationRequestId, ComponentLifecycleEvent.DELETED);
        }
    }

    private void forwardToNotificationSchedulerService(TenantId tenantId, NotificationRequestId notificationRequestId) {
        TransportProtos.NotificationSchedulerServiceMsg.Builder msg = TransportProtos.NotificationSchedulerServiceMsg.newBuilder()
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .setRequestIdMSB(notificationRequestId.getId().getMostSignificantBits())
                .setRequestIdLSB(notificationRequestId.getId().getLeastSignificantBits())
                .setTs(System.currentTimeMillis());
        TransportProtos.ToCoreMsg toCoreMsg = TransportProtos.ToCoreMsg.newBuilder()
                .setNotificationSchedulerServiceMsg(msg)
                .build();
        clusterService.pushMsgToCore(tenantId, notificationRequestId, toCoreMsg, null);
    }

    private void onNotificationUpdate(TenantId tenantId, UserId recipientId, NotificationUpdate update) {
        log.trace("Submitting notification update for recipient {}: {}", recipientId, update);
        forwardToSubscriptionManagerService(tenantId, recipientId, subscriptionManagerService -> {
            subscriptionManagerService.onNotificationUpdate(tenantId, recipientId, update, TbCallback.EMPTY);
        }, () -> TbSubscriptionUtils.notificationUpdateToProto(tenantId, recipientId, update));
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
    public NotificationDeliveryMethod getDeliveryMethod() {
        return NotificationDeliveryMethod.WEB;
    }

    @Override
    protected String getExecutorPrefix() {
        return "notification";
    }

    @Autowired
    public void setChannels(List<NotificationChannel> channels, NotificationCenter webNotificationChannel) {
        this.channels = channels.stream().collect(Collectors.toMap(NotificationChannel::getDeliveryMethod, c -> c));
        this.channels.put(NotificationDeliveryMethod.WEB, (NotificationChannel) webNotificationChannel);
    }

}
