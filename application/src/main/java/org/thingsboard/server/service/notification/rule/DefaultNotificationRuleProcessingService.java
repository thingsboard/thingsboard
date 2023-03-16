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
import org.thingsboard.server.common.data.UpdateMessage;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.NotificationRequestId;
import org.thingsboard.server.common.data.id.NotificationRuleId;
import org.thingsboard.server.common.data.id.RuleChainId;
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
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.dao.alarm.AlarmApiCallResult;
import org.thingsboard.server.dao.notification.NotificationRequestService;
import org.thingsboard.server.dao.notification.NotificationRuleService;
import org.thingsboard.server.service.executors.NotificationExecutorService;
import org.thingsboard.server.service.notification.rule.trigger.EntitiesLimitTriggerProcessor.EntitiesLimitTriggerObject;
import org.thingsboard.server.service.notification.rule.trigger.NotificationRuleTriggerProcessor;
import org.thingsboard.server.service.notification.rule.trigger.RuleEngineComponentLifecycleEventTriggerProcessor.RuleEngineComponentLifecycleEventTriggerObject;
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
@SuppressWarnings("rawtypes")
public class DefaultNotificationRuleProcessingService implements NotificationRuleProcessingService {

    private final NotificationRuleService notificationRuleService;
    private final NotificationRequestService notificationRequestService;
    @Autowired @Lazy
    private NotificationCenter notificationCenter;
    private final NotificationExecutorService notificationExecutor;

    private final Map<NotificationRuleTriggerType, NotificationRuleTriggerProcessor> triggerProcessors = new EnumMap<>(NotificationRuleTriggerType.class);

    private final Map<String, NotificationRuleTriggerType> ruleEngineMsgTypeToTriggerType = new HashMap<>();

    @Override
    public void process(TenantId tenantId, TbMsg ruleEngineMsg) {
        String msgType = ruleEngineMsg.getType();
        NotificationRuleTriggerType triggerType = ruleEngineMsgTypeToTriggerType.get(msgType);
        if (triggerType == null) {
            return;
        }
        processTrigger(tenantId, triggerType, ruleEngineMsg.getOriginator(), ruleEngineMsg);
    }

    @Override
    public void process(TenantId tenantId, AlarmApiCallResult alarmUpdate) {
        processTrigger(tenantId, NotificationRuleTriggerType.ALARM, alarmUpdate.getAlarm().getId(), alarmUpdate);
    }

    @Override
    public void process(TenantId tenantId, RuleChainId ruleChainId, String ruleChainName, EntityId componentId, String componentName, ComponentLifecycleEvent eventType, Exception error) {
        RuleEngineComponentLifecycleEventTriggerObject triggerObject = RuleEngineComponentLifecycleEventTriggerObject.builder()
                .ruleChainId(ruleChainId)
                .ruleChainName(ruleChainName)
                .componentId(componentId)
                .componentName(componentName)
                .eventType(eventType)
                .error(error)
                .build();
        processTrigger(tenantId, NotificationRuleTriggerType.RULE_ENGINE_COMPONENT_LIFECYCLE_EVENT, componentId, triggerObject);
    }

    @Override
    public void process(UpdateMessage platformUpdateMessage) {
//        if (!partitionService.resolve(ServiceType.TB_CORE, TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID).isMyPartition()) {
//            return;
//        }
//        // todo: don't send repetitive notification after platform restart?
//
//        processTrigger(TenantId.SYS_TENANT_ID, NotificationRuleTriggerType.NEW_PLATFORM_VERSION, TenantId.SYS_TENANT_ID, platformUpdateMessage);
    }

    @Override
    public void process(TenantId tenantId, EntityType entityType, long limit, long currentCount) {
//        EntitiesLimitTriggerObject triggerObject = EntitiesLimitTriggerObject.builder()
//                .entityType(entityType)
//                .limit(limit)
//                .currentCount(currentCount)
//                .build();
    }

    @Override
    public void process(ComponentLifecycleMsg componentLifecycleMsg) {
//        EntityId entityId = componentLifecycleMsg.getEntityId();
//        switch (entityId.getEntityType()) {
//            case TENANT:
//
//        }
    }

    private void processTrigger(TenantId tenantId, NotificationRuleTriggerType triggerType, EntityId originatorEntityId, Object triggerObject) {
        List<NotificationRule> rules = notificationRuleService.findNotificationRulesByTenantIdAndTriggerType(tenantId, triggerType);
        for (NotificationRule rule : rules) {
            notificationExecutor.submit(() -> {
                try {
                    processNotificationRule(rule, originatorEntityId, triggerObject);
                } catch (Throwable e) {
                    log.error("Failed to process notification rule {} for trigger type {} with trigger object {}", rule.getId(), rule.getTriggerType(), triggerObject, e);
                }
            });
        }
    }

    private void processNotificationRule(NotificationRule rule, EntityId originatorEntityId, Object triggerObject) {
        NotificationRuleTriggerConfig triggerConfig = rule.getTriggerConfig();
        log.debug("Processing notification rule '{}' for trigger type {}", rule.getName(), rule.getTriggerType());

        if (matchesClearRule(triggerObject, triggerConfig)) {
            List<NotificationRequest> notificationRequests = notificationRequestService.findNotificationRequestsByRuleIdAndOriginatorEntityId(rule.getTenantId(), rule.getId(), originatorEntityId);
            if (notificationRequests.isEmpty()) {
                return;
            }

            List<UUID> targets = notificationRequests.stream()
                    .filter(NotificationRequest::isSent)
                    .flatMap(notificationRequest -> notificationRequest.getTargets().stream())
                    .distinct().collect(Collectors.toList());
            NotificationInfo notificationInfo = constructNotificationInfo(triggerObject, triggerConfig);
            submitNotificationRequest(targets, rule, originatorEntityId, notificationInfo, 0);

            notificationRequests.forEach(notificationRequest -> {
                if (notificationRequest.isScheduled()) {
                    notificationCenter.deleteNotificationRequest(rule.getTenantId(), notificationRequest.getId());
                }
            });
            return;
        }

        if (matchesFilter(triggerObject, triggerConfig)) {
            NotificationInfo notificationInfo = constructNotificationInfo(triggerObject, triggerConfig);
            rule.getRecipientsConfig().getTargetsTable().forEach((delay, targets) -> {
                submitNotificationRequest(targets, rule, originatorEntityId, notificationInfo, delay);
            });
        }
    }

    private boolean matchesFilter(Object triggerObject, NotificationRuleTriggerConfig triggerConfig) {
        return triggerProcessors.get(triggerConfig.getTriggerType()).matchesFilter(triggerObject, triggerConfig);
    }

    private boolean matchesClearRule(Object triggerObject, NotificationRuleTriggerConfig triggerConfig) {
        return triggerProcessors.get(triggerConfig.getTriggerType()).matchesClearRule(triggerObject, triggerConfig);
    }

    private NotificationInfo constructNotificationInfo(Object triggerObject, NotificationRuleTriggerConfig triggerConfig) {
        return triggerProcessors.get(triggerConfig.getTriggerType()).constructNotificationInfo(triggerObject, triggerConfig);
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
