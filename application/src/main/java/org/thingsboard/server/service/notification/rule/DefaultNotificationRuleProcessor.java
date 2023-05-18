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
package org.thingsboard.server.service.notification.rule;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.thingsboard.rule.engine.api.NotificationCenter;
import org.thingsboard.server.common.data.CacheConstants;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.NotificationRequestId;
import org.thingsboard.server.common.data.id.NotificationRuleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.limit.LimitedApi;
import org.thingsboard.server.common.data.notification.NotificationRequest;
import org.thingsboard.server.common.data.notification.NotificationRequestConfig;
import org.thingsboard.server.common.data.notification.NotificationRequestStatus;
import org.thingsboard.server.common.data.notification.info.NotificationInfo;
import org.thingsboard.server.common.data.notification.rule.NotificationRule;
import org.thingsboard.server.common.data.notification.rule.trigger.NotificationRuleTriggerConfig;
import org.thingsboard.server.common.data.notification.rule.trigger.NotificationRuleTriggerType;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.msg.notification.NotificationRuleProcessor;
import org.thingsboard.server.common.msg.notification.trigger.NotificationRuleTrigger;
import org.thingsboard.server.common.msg.notification.trigger.RuleEngineMsgTrigger;
import org.thingsboard.server.common.msg.plugin.ComponentLifecycleMsg;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.dao.notification.NotificationRequestService;
import org.thingsboard.server.dao.util.limits.RateLimitService;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.service.executors.NotificationExecutorService;
import org.thingsboard.server.service.notification.rule.cache.NotificationRulesCache;
import org.thingsboard.server.service.notification.rule.trigger.NotificationRuleTriggerProcessor;
import org.thingsboard.server.service.notification.rule.trigger.RuleEngineMsgNotificationRuleTriggerProcessor;

import javax.annotation.PostConstruct;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings({"rawtypes", "unchecked"})
public class DefaultNotificationRuleProcessor implements NotificationRuleProcessor {

    private final NotificationRulesCache notificationRulesCache;
    private final NotificationRequestService notificationRequestService;
    private final PartitionService partitionService;
    private final RateLimitService rateLimitService;
    @Autowired @Lazy
    private NotificationCenter notificationCenter;
    private final NotificationExecutorService notificationExecutor;
    private final CacheManager cacheManager;
    private Cache sentNotifications;

    private final Map<NotificationRuleTriggerType, NotificationRuleTriggerProcessor> triggerProcessors = new EnumMap<>(NotificationRuleTriggerType.class);

    @PostConstruct
    private void init() {
        sentNotifications = cacheManager.getCache(CacheConstants.SENT_NOTIFICATIONS_CACHE);
        if (sentNotifications == null) {
            throw new IllegalStateException("Sent notifications cache is not set up");
        }
    }

    @Override
    public void process(NotificationRuleTrigger trigger) {
        NotificationRuleTriggerType triggerType = trigger.getType();
        if (triggerType == null) return;
        TenantId tenantId = triggerType.isTenantLevel() ? trigger.getTenantId() : TenantId.SYS_TENANT_ID;

        try {
            List<NotificationRule> rules = notificationRulesCache.getEnabled(tenantId, triggerType);
            for (NotificationRule rule : rules) {
                notificationExecutor.submit(() -> {
                    try {
                        processNotificationRule(rule, trigger);
                    } catch (Throwable e) {
                        log.error("Failed to process notification rule {} for trigger type {} with trigger object {}", rule.getId(), rule.getTriggerType(), trigger, e);
                    }
                });
            }
        } catch (Throwable e) {
            log.error("Failed to process notification rules for trigger: {}", trigger, e);
        }
    }

    private void processNotificationRule(NotificationRule rule, NotificationRuleTrigger trigger) {
        NotificationRuleTriggerConfig triggerConfig = rule.getTriggerConfig();
        log.debug("Processing notification rule '{}' for trigger type {}", rule.getName(), rule.getTriggerType());

        if (matchesClearRule(trigger, triggerConfig)) {
            List<NotificationRequest> notificationRequests = findAlreadySentNotificationRequests(rule, trigger);
            if (notificationRequests.isEmpty()) {
                return;
            }

            List<UUID> targets = notificationRequests.stream()
                    .filter(NotificationRequest::isSent)
                    .flatMap(notificationRequest -> notificationRequest.getTargets().stream())
                    .distinct().collect(Collectors.toList());
            NotificationInfo notificationInfo = constructNotificationInfo(trigger, triggerConfig);
            submitNotificationRequest(targets, rule, trigger.getOriginatorEntityId(), notificationInfo, 0);

            notificationRequests.forEach(notificationRequest -> {
                if (notificationRequest.isScheduled()) {
                    notificationCenter.deleteNotificationRequest(rule.getTenantId(), notificationRequest.getId());
                }
            });
            return;
        }

        if (matchesFilter(trigger, triggerConfig)) {
            if (!rateLimitService.checkRateLimit(LimitedApi.NOTIFICATION_REQUESTS_PER_RULE, rule.getTenantId(), rule.getId())) {
                log.debug("[{}] Rate limit for notification requests per rule was exceeded (rule '{}')", rule.getTenantId(), rule.getName());
                return;
            }
            if (trigger.deduplicate() && alreadySent(rule, trigger)) {
                return;
            }

            NotificationInfo notificationInfo = constructNotificationInfo(trigger, triggerConfig);
            rule.getRecipientsConfig().getTargetsTable().forEach((delay, targets) -> {
                submitNotificationRequest(targets, rule, trigger.getOriginatorEntityId(), notificationInfo, delay);
            });
        }
    }

    private List<NotificationRequest> findAlreadySentNotificationRequests(NotificationRule rule, NotificationRuleTrigger trigger) {
        return notificationRequestService.findNotificationRequestsByRuleIdAndOriginatorEntityId(rule.getTenantId(), rule.getId(), trigger.getOriginatorEntityId());
    }

    private void submitNotificationRequest(List<UUID> targets, NotificationRule rule,
                                           EntityId originatorEntityId, NotificationInfo notificationInfo, int delayInSec) {
        NotificationRequestConfig config = new NotificationRequestConfig();
        if (delayInSec > 0) {
            config.setSendingDelayInSec(delayInSec);
        }
        NotificationRequest notificationRequest = NotificationRequest.builder()
                .tenantId(rule.getTenantId())
                .targets(targets)
                .templateId(rule.getTemplateId())
                .additionalConfig(config)
                .info(notificationInfo)
                .ruleId(rule.getId())
                .originatorEntityId(originatorEntityId)
                .build();
        notificationExecutor.submit(() -> {
            try {
                log.debug("Submitting notification request for rule '{}' with delay of {} sec to targets {}", rule.getName(), delayInSec, targets);
                notificationCenter.processNotificationRequest(rule.getTenantId(), notificationRequest, null);
            } catch (Exception e) {
                log.error("Failed to process notification request for tenant {} for rule {}", rule.getTenantId(), rule.getId(), e);
            }
        });
    }

    private boolean matchesFilter(NotificationRuleTrigger trigger, NotificationRuleTriggerConfig triggerConfig) {
        return triggerProcessors.get(triggerConfig.getTriggerType()).matchesFilter(trigger, triggerConfig);
    }

    private boolean matchesClearRule(NotificationRuleTrigger trigger, NotificationRuleTriggerConfig triggerConfig) {
        return triggerProcessors.get(triggerConfig.getTriggerType()).matchesClearRule(trigger, triggerConfig);
    }

    private NotificationInfo constructNotificationInfo(NotificationRuleTrigger trigger, NotificationRuleTriggerConfig triggerConfig) {
        return triggerProcessors.get(triggerConfig.getTriggerType()).constructNotificationInfo(trigger);
    }

    private boolean alreadySent(NotificationRule rule, NotificationRuleTrigger trigger) {
        String deduplicationKey = String.join("_", rule.getDeduplicationKey(), trigger.getDeduplicationKey());

        AtomicBoolean isNew = new AtomicBoolean(false);
        Long lastSentTs = sentNotifications.get(deduplicationKey, () -> {
            isNew.set(true);
            log.trace("[{}] Putting to sentNotifications cache: {}", rule.getId(), trigger);
            return System.currentTimeMillis();
        });
        if (isNew.get()) {
            return false;
        }

        long deduplicationDuration = trigger.getDeduplicationDuration();
        long passed = System.currentTimeMillis() - lastSentTs;
        if (deduplicationDuration == 0 || passed <= deduplicationDuration) {
            log.debug("Notification for {} trigger was already sent {} ms ago, deduplication duration is {} ms. Key: '{}'",
                    trigger.getType(), passed, deduplicationDuration, deduplicationKey);
            sentNotifications.put(deduplicationKey, lastSentTs); // updating cache anyway so that the value is not removed by ttl
            return true;
        } else {
            return false;
        }
    }

    @EventListener(ComponentLifecycleMsg.class)
    public void onNotificationRuleDeleted(ComponentLifecycleMsg componentLifecycleMsg) {
        if (componentLifecycleMsg.getEvent() != ComponentLifecycleEvent.DELETED ||
                componentLifecycleMsg.getEntityId().getEntityType() != EntityType.NOTIFICATION_RULE) {
            return;
        }

        TenantId tenantId = componentLifecycleMsg.getTenantId();
        NotificationRuleId notificationRuleId = (NotificationRuleId) componentLifecycleMsg.getEntityId();
        if (partitionService.isMyPartition(ServiceType.TB_CORE, tenantId, notificationRuleId)) {
            notificationExecutor.submit(() -> {
                List<NotificationRequestId> scheduledForRule = notificationRequestService.findNotificationRequestsIdsByStatusAndRuleId(tenantId, NotificationRequestStatus.SCHEDULED, notificationRuleId);
                for (NotificationRequestId notificationRequestId : scheduledForRule) {
                    notificationCenter.deleteNotificationRequest(tenantId, notificationRequestId);
                }
            });
        }
    }

    @Autowired
    public void setTriggerProcessors(Collection<NotificationRuleTriggerProcessor> processors) {
        Map<String, NotificationRuleTriggerType> ruleEngineMsgTypeToTriggerType = new HashMap<>();
        processors.forEach(processor -> {
            triggerProcessors.put(processor.getTriggerType(), processor);
            if (processor instanceof RuleEngineMsgNotificationRuleTriggerProcessor) {
                Set<String> supportedMsgTypes = ((RuleEngineMsgNotificationRuleTriggerProcessor<?>) processor).getSupportedMsgTypes();
                supportedMsgTypes.forEach(supportedMsgType -> {
                    ruleEngineMsgTypeToTriggerType.put(supportedMsgType, processor.getTriggerType());
                });
            }
        });
        RuleEngineMsgTrigger.msgTypeToTriggerType = ruleEngineMsgTypeToTriggerType;
    }

}
