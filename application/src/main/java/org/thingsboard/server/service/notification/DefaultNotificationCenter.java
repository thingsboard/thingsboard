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
package org.thingsboard.server.service.notification;

import com.google.common.util.concurrent.FutureCallback;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.rule.engine.api.NotificationCenter;
import org.thingsboard.server.cache.limits.RateLimitService;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.EntityId;
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
import org.thingsboard.server.common.data.notification.NotificationType;
import org.thingsboard.server.common.data.notification.info.GeneralNotificationInfo;
import org.thingsboard.server.common.data.notification.info.NotificationInfo;
import org.thingsboard.server.common.data.notification.info.RuleOriginatedNotificationInfo;
import org.thingsboard.server.common.data.notification.settings.NotificationSettings;
import org.thingsboard.server.common.data.notification.settings.UserNotificationSettings;
import org.thingsboard.server.common.data.notification.targets.MicrosoftTeamsNotificationTargetConfig;
import org.thingsboard.server.common.data.notification.targets.NotificationRecipient;
import org.thingsboard.server.common.data.notification.targets.NotificationTarget;
import org.thingsboard.server.common.data.notification.targets.platform.PlatformUsersNotificationTargetConfig;
import org.thingsboard.server.common.data.notification.targets.platform.UsersFilter;
import org.thingsboard.server.common.data.notification.targets.slack.SlackNotificationTargetConfig;
import org.thingsboard.server.common.data.notification.template.DeliveryMethodNotificationTemplate;
import org.thingsboard.server.common.data.notification.template.NotificationTemplate;
import org.thingsboard.server.common.data.notification.template.WebDeliveryMethodNotificationTemplate;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.common.msg.tools.TbRateLimitsException;
import org.thingsboard.server.dao.notification.NotificationRequestService;
import org.thingsboard.server.dao.notification.NotificationService;
import org.thingsboard.server.dao.notification.NotificationSettingsService;
import org.thingsboard.server.dao.notification.NotificationTargetService;
import org.thingsboard.server.dao.notification.NotificationTemplateService;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.TopicService;
import org.thingsboard.server.queue.provider.TbQueueProducerProvider;
import org.thingsboard.server.service.executors.NotificationExecutorService;
import org.thingsboard.server.service.notification.channels.NotificationChannel;
import org.thingsboard.server.service.subscription.TbSubscriptionUtils;
import org.thingsboard.server.service.telemetry.AbstractSubscriptionService;
import org.thingsboard.server.service.ws.notification.sub.NotificationRequestUpdate;
import org.thingsboard.server.service.ws.notification.sub.NotificationUpdate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.thingsboard.server.common.data.notification.NotificationDeliveryMethod.WEB;

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
    private final TopicService topicService;
    private final TbQueueProducerProvider producerProvider;
    private final RateLimitService rateLimitService;

    private Map<NotificationDeliveryMethod, NotificationChannel> channels;

    @Override
    public NotificationRequest processNotificationRequest(TenantId tenantId, NotificationRequest request, FutureCallback<NotificationRequestStats> callback) {
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
        if (notificationTemplate == null) {
            throw new IllegalArgumentException("Template is missing");
        }
        NotificationType notificationType = notificationTemplate.getNotificationType();

        Set<NotificationDeliveryMethod> deliveryMethods = new HashSet<>();
        List<NotificationTarget> targets = new ArrayList<>();
        for (UUID targetId : request.getTargets()) {
            NotificationTarget target = notificationTargetService.findNotificationTargetById(tenantId, new NotificationTargetId(targetId));
            if (target != null) {
                targets.add(target);
            } else {
                log.debug("Unknown notification target {} in request {}", targetId, request);
            }
        }
        if (targets.isEmpty()) {
            throw new IllegalArgumentException("No recipients chosen");
        }

        NotificationRuleId ruleId = request.getRuleId();
        notificationTemplate.getConfiguration().getDeliveryMethodsTemplates().forEach((deliveryMethod, template) -> {
            if (!template.isEnabled()) return;
            try {
                channels.get(deliveryMethod).check(tenantId);
            } catch (Exception e) {
                if (ruleId == null && !notificationType.isSystem()) {
                    throw new IllegalArgumentException(e.getMessage());
                } else {
                    return; // if originated by rule or notification type is system - just ignore delivery method
                }
            }
            if (ruleId == null && !notificationType.isSystem()) {
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
        NotificationSettings systemSettings = tenantId.isSysTenantId() ? settings : notificationSettingsService.findNotificationSettings(TenantId.SYS_TENANT_ID);

        log.debug("Processing notification request (tenantId: {}, targets: {})", tenantId, request.getTargets());
        request.setStatus(NotificationRequestStatus.PROCESSING);
        request = notificationRequestService.saveNotificationRequest(tenantId, request);

        NotificationProcessingContext ctx = NotificationProcessingContext.builder()
                .tenantId(tenantId)
                .request(request)
                .deliveryMethods(deliveryMethods)
                .template(notificationTemplate)
                .settings(settings)
                .systemSettings(systemSettings)
                .build();

        processNotificationRequestAsync(ctx, targets, callback);
        return request;
    }

    @Override
    public void sendGeneralWebNotification(TenantId tenantId, UsersFilter recipients, NotificationTemplate template, GeneralNotificationInfo info) {
        NotificationTarget target = new NotificationTarget();
        target.setTenantId(tenantId);
        PlatformUsersNotificationTargetConfig targetConfig = new PlatformUsersNotificationTargetConfig();
        targetConfig.setUsersFilter(recipients);
        target.setConfiguration(targetConfig);

        NotificationRequest notificationRequest = NotificationRequest.builder()
                .tenantId(tenantId)
                .template(template)
                .targets(List.of(EntityId.NULL_UUID)) // this is temporary and will be removed when 'create from scratch' functionality is implemented for recipients
                .info(info)
                .status(NotificationRequestStatus.PROCESSING)
                .build();
        try {
            notificationRequest = notificationRequestService.saveNotificationRequest(tenantId, notificationRequest);
            NotificationProcessingContext ctx = NotificationProcessingContext.builder()
                    .tenantId(tenantId)
                    .request(notificationRequest)
                    .deliveryMethods(Set.of(WEB))
                    .template(template)
                    .build();

            processNotificationRequestAsync(ctx, List.of(target), null);
        } catch (Exception e) {
            log.error("Failed to process notification request for recipients {} for template '{}'", recipients, template.getName(), e);
        }
    }

    @Override
    public void sendSystemNotification(TenantId tenantId, NotificationTargetId targetId, NotificationType type, NotificationInfo info) {
        log.debug("[{}] Sending {} system notification to {}: {}", tenantId, type, targetId, info);
        NotificationTemplate notificationTemplate = notificationTemplateService.findTenantOrSystemNotificationTemplate(tenantId, type)
                .orElseThrow(() -> new IllegalArgumentException("No notification template found for type " + type));
        NotificationRequest notificationRequest = NotificationRequest.builder()
                .tenantId(tenantId)
                .targets(List.of(targetId.getId()))
                .templateId(notificationTemplate.getId())
                .info(info)
                .originatorEntityId(TenantId.SYS_TENANT_ID)
                .build();
        processNotificationRequest(tenantId, notificationRequest, null);
    }

    private void processNotificationRequestAsync(NotificationProcessingContext ctx, List<NotificationTarget> targets, FutureCallback<NotificationRequestStats> callback) {
        notificationExecutor.submit(() -> {
            long startTs = System.currentTimeMillis();
            NotificationRequestId requestId = ctx.getRequest().getId();
            for (NotificationTarget target : targets) {
                try {
                    processForTarget(target, ctx);
                } catch (Exception e) {
                    log.error("[{}] Failed to process notification request for target {}", requestId, target.getId(), e);
                    ctx.getStats().setError(e.getMessage());
                    updateRequestStats(ctx, requestId, ctx.getStats());

                    if (callback != null) {
                        callback.onFailure(e);
                    }
                    return;
                }
            }

            NotificationRequestStats stats = ctx.getStats();
            long time = System.currentTimeMillis() - startTs;
            int sent = stats.getTotalSent().get();
            int errors = stats.getTotalErrors().get();
            if (errors > 0) {
                log.debug("[{}][{}] Notification request processing finished in {} ms (sent: {}, errors: {})", ctx.getTenantId(), requestId, time, sent, errors);
            } else {
                log.debug("[{}][{}] Notification request processing finished in {} ms (sent: {})", ctx.getTenantId(), requestId, time, sent);
            }
            updateRequestStats(ctx, requestId, stats);
            if (callback != null) {
                callback.onSuccess(stats);
            }
        });
    }

    private void updateRequestStats(NotificationProcessingContext ctx, NotificationRequestId requestId, NotificationRequestStats stats) {
        try {
            notificationRequestService.updateNotificationRequest(ctx.getTenantId(), requestId, NotificationRequestStatus.SENT, stats);
        } catch (Exception e) {
            log.error("[{}] Failed to update stats for notification request", requestId, e);
        }
    }

    private void processForTarget(NotificationTarget target, NotificationProcessingContext ctx) {
        Iterable<? extends NotificationRecipient> recipients;
        switch (target.getConfiguration().getType()) {
            case PLATFORM_USERS -> {
                PlatformUsersNotificationTargetConfig targetConfig = (PlatformUsersNotificationTargetConfig) target.getConfiguration();
                if (targetConfig.getUsersFilter().getType().isForRules() && ctx.getRequest().getInfo() instanceof RuleOriginatedNotificationInfo) {
                    recipients = new PageDataIterable<>(pageLink -> {
                        return notificationTargetService.findRecipientsForRuleNotificationTargetConfig(ctx.getTenantId(), targetConfig, (RuleOriginatedNotificationInfo) ctx.getRequest().getInfo(), pageLink);
                    }, 256);
                } else {
                    recipients = new PageDataIterable<>(pageLink -> {
                        return notificationTargetService.findRecipientsForNotificationTargetConfig(target.getTenantId(), targetConfig, pageLink);
                    }, 256);
                }
            }
            case SLACK -> {
                SlackNotificationTargetConfig targetConfig = (SlackNotificationTargetConfig) target.getConfiguration();
                recipients = List.of(targetConfig.getConversation());
            }
            case MICROSOFT_TEAMS -> {
                MicrosoftTeamsNotificationTargetConfig targetConfig = (MicrosoftTeamsNotificationTargetConfig) target.getConfiguration();
                recipients = List.of(targetConfig);
            }
            default -> recipients = Collections.emptyList();
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
        } else {
            ctx.getStats().reportProcessed(deliveryMethod, recipient.getId());
        }

        if (recipient instanceof User) {
            UserNotificationSettings settings = notificationSettingsService.getUserNotificationSettings(ctx.getTenantId(), ((User) recipient).getId(), false);
            if (!settings.isEnabled(ctx.getNotificationType(), deliveryMethod)) {
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
                .type(ctx.getNotificationType())
                .deliveryMethod(WEB)
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
            Notification notification = notificationService.findNotificationById(tenantId, notificationId);
            if (notification.getDeliveryMethod() == WEB) {
                NotificationUpdate update = NotificationUpdate.builder()
                        .updated(true)
                        .notificationId(notificationId.getId())
                        .notificationType(notification.getType())
                        .newStatus(NotificationStatus.READ)
                        .build();
                onNotificationUpdate(tenantId, recipientId, update);
            }
        }
    }

    @Override
    public void markAllNotificationsAsRead(TenantId tenantId, NotificationDeliveryMethod deliveryMethod, UserId recipientId) {
        int updatedCount = notificationService.markAllNotificationsAsRead(tenantId, deliveryMethod, recipientId);
        if (updatedCount > 0 && deliveryMethod == WEB) {
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
        if (deleted && notification.getDeliveryMethod() == WEB) {
            NotificationUpdate update = NotificationUpdate.builder()
                    .deleted(true)
                    .notification(notification)
                    .build();
            onNotificationUpdate(tenantId, recipientId, update);
        }
    }

    @Override
    public List<NotificationDeliveryMethod> getAvailableDeliveryMethods(TenantId tenantId) {
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
                .sorted().toList();
    }

    @Override
    public void check(TenantId tenantId) throws Exception {
    }

    @Override
    public void deleteNotificationRequest(TenantId tenantId, NotificationRequestId notificationRequestId) {
        log.debug("Deleting notification request {}", notificationRequestId);
        NotificationRequest notificationRequest = notificationRequestService.findNotificationRequestById(tenantId, notificationRequestId);
        notificationRequestService.deleteNotificationRequest(tenantId, notificationRequest);

        if (notificationRequest.isSent()) {
            // TODO: no need to send request update for other than PLATFORM_USERS target type
            onNotificationRequestUpdate(tenantId, NotificationRequestUpdate.builder()
                    .notificationRequestId(notificationRequestId)
                    .deleted(true)
                    .build());
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
                TopicPartitionInfo tpi = topicService.getNotificationsTopic(ServiceType.TB_CORE, serviceId);
                producerProvider.getTbCoreNotificationsMsgProducer().send(tpi, new TbProtoQueueMsg<>(UUID.randomUUID(), notificationRequestUpdateProto), null);
            }
        });
    }

    @Override
    public NotificationDeliveryMethod getDeliveryMethod() {
        return WEB;
    }

    @Override
    protected String getExecutorPrefix() {
        return "notification";
    }

    @Autowired
    public void setChannels(List<NotificationChannel> channels, NotificationCenter webNotificationChannel) {
        this.channels = channels.stream().collect(Collectors.toMap(NotificationChannel::getDeliveryMethod, c -> c));
        this.channels.put(WEB, (NotificationChannel) webNotificationChannel);
    }

}
