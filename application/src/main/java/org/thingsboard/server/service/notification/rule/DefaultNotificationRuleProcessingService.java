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
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.thingsboard.rule.engine.api.NotificationCenter;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.NotificationRequestId;
import org.thingsboard.server.common.data.id.NotificationRuleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.NotificationRequest;
import org.thingsboard.server.common.data.notification.NotificationRequestConfig;
import org.thingsboard.server.common.data.notification.NotificationRequestStatus;
import org.thingsboard.server.common.data.notification.info.NotificationInfo;
import org.thingsboard.server.common.data.notification.rule.NotificationRule;
import org.thingsboard.server.common.data.notification.rule.trigger.NotificationRuleTriggerConfig;
import org.thingsboard.server.common.data.notification.rule.trigger.NotificationRuleTriggerType;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.plugin.ComponentLifecycleMsg;
import org.thingsboard.server.dao.notification.NotificationRequestService;
import org.thingsboard.server.dao.notification.NotificationRuleProcessingService;
import org.thingsboard.server.dao.notification.NotificationRuleService;
import org.thingsboard.server.dao.notification.trigger.NotificationRuleTrigger;
import org.thingsboard.server.dao.notification.trigger.RuleEngineMsgTrigger;
import org.thingsboard.server.service.executors.NotificationExecutorService;
import org.thingsboard.server.service.notification.rule.trigger.NotificationRuleTriggerProcessor;
import org.thingsboard.server.service.notification.rule.trigger.RuleEngineMsgNotificationRuleTriggerProcessor;

import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings({"rawtypes", "unchecked"})
public class DefaultNotificationRuleProcessingService implements NotificationRuleProcessingService {

    private final NotificationRuleService notificationRuleService;
    private final NotificationRequestService notificationRequestService;
    @Autowired @Lazy
    private NotificationCenter notificationCenter;
    private final NotificationExecutorService notificationExecutor;

    private final Map<NotificationRuleTriggerType, NotificationRuleTriggerProcessor> triggerProcessors = new EnumMap<>(NotificationRuleTriggerType.class);

    private final Map<String, NotificationRuleTriggerType> ruleEngineMsgTypeToTriggerType = new HashMap<>();

    @Override
    public void process(NotificationRuleTrigger trigger) {
        List<NotificationRule> rules = notificationRuleService.findNotificationRulesByTenantIdAndTriggerType(
                trigger.getType().isTenantLevel() ? trigger.getTenantId() : TenantId.SYS_TENANT_ID, trigger.getType());
        for (NotificationRule rule : rules) {
            notificationExecutor.submit(() -> {
                try {
                    processNotificationRule(rule, trigger);
                } catch (Throwable e) {
                    log.error("Failed to process notification rule {} for trigger type {} with trigger object {}", rule.getId(), rule.getTriggerType(), trigger, e);
                }
            });
        }
    }

    @Override
    public void process(TenantId tenantId, TbMsg ruleEngineMsg) {
        NotificationRuleTriggerType triggerType = ruleEngineMsgTypeToTriggerType.get(ruleEngineMsg.getType());
        if (triggerType == null) {
            return;
        }
        process(RuleEngineMsgTrigger.builder()
                .tenantId(tenantId)
                .msg(ruleEngineMsg)
                .triggerType(triggerType)
                .build());
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
                notificationCenter.processNotificationRequest(rule.getTenantId(), notificationRequest);
            } catch (Exception e) {
                log.error("Failed to process notification request for rule {}", rule.getId(), e);
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

    @EventListener(ComponentLifecycleMsg.class)
    public void onNotificationRuleDeleted(ComponentLifecycleMsg componentLifecycleMsg) {
        if (componentLifecycleMsg.getEvent() != ComponentLifecycleEvent.DELETED ||
                componentLifecycleMsg.getEntityId().getEntityType() != EntityType.NOTIFICATION_RULE) {
            return;
        }

        TenantId tenantId = componentLifecycleMsg.getTenantId();
        NotificationRuleId notificationRuleId = (NotificationRuleId) componentLifecycleMsg.getEntityId();
        notificationExecutor.submit(() -> {
            List<NotificationRequestId> scheduledForRule = notificationRequestService.findNotificationRequestsIdsByStatusAndRuleId(tenantId, NotificationRequestStatus.SCHEDULED, notificationRuleId);
            for (NotificationRequestId notificationRequestId : scheduledForRule) {
                notificationCenter.deleteNotificationRequest(tenantId, notificationRequestId);
            }
        });
    }

    @Autowired
    public void setTriggerProcessors(Collection<NotificationRuleTriggerProcessor> processors) {
        processors.forEach(processor -> {
            triggerProcessors.put(processor.getTriggerType(), processor);
            if (processor instanceof RuleEngineMsgNotificationRuleTriggerProcessor) {
                Set<String> supportedMsgTypes = ((RuleEngineMsgNotificationRuleTriggerProcessor<?>) processor).getSupportedMsgTypes();
                supportedMsgTypes.forEach(supportedMsgType -> {
                    ruleEngineMsgTypeToTriggerType.put(supportedMsgType, processor.getTriggerType());
                });
            }
        });
    }

}
