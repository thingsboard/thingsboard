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
package org.thingsboard.server.service.notification.rule.trigger;

import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.notification.info.RuleEngineComponentLifecycleEventNotificationInfo;
import org.thingsboard.server.common.data.notification.info.RuleOriginatedNotificationInfo;
import org.thingsboard.server.common.data.notification.rule.trigger.RuleEngineComponentLifecycleEventTrigger;
import org.thingsboard.server.common.data.notification.rule.trigger.config.NotificationRuleTriggerType;
import org.thingsboard.server.common.data.notification.rule.trigger.config.RuleEngineComponentLifecycleEventNotificationRuleTriggerConfig;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.queue.discovery.PartitionService;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RuleEngineComponentLifecycleEventTriggerProcessor implements NotificationRuleTriggerProcessor<RuleEngineComponentLifecycleEventTrigger, RuleEngineComponentLifecycleEventNotificationRuleTriggerConfig> {

    private final PartitionService partitionService;

    @Override
    public boolean matchesFilter(RuleEngineComponentLifecycleEventTrigger trigger, RuleEngineComponentLifecycleEventNotificationRuleTriggerConfig triggerConfig) {
        if (CollectionUtils.isNotEmpty(triggerConfig.getRuleChains())) {
            if (!triggerConfig.getRuleChains().contains(trigger.getRuleChainId().getId())) {
                return false;
            }
        }
        if (!partitionService.isMyPartition(ServiceType.TB_RULE_ENGINE, trigger.getTenantId(), trigger.getComponentId())) {
            return false;
        }

        EntityType componentType = trigger.getComponentId().getEntityType();
        Set<ComponentLifecycleEvent> trackedEvents;
        boolean onlyFailures;
        if (componentType == EntityType.RULE_CHAIN) {
            trackedEvents = triggerConfig.getRuleChainEvents();
            onlyFailures = triggerConfig.isOnlyRuleChainLifecycleFailures();
        } else if (componentType == EntityType.RULE_NODE && triggerConfig.isTrackRuleNodeEvents()) {
            trackedEvents = triggerConfig.getRuleNodeEvents();
            onlyFailures = triggerConfig.isOnlyRuleNodeLifecycleFailures();
        } else {
            return false;
        }
        if (CollectionUtils.isEmpty(trackedEvents)) {
            trackedEvents = Set.of(ComponentLifecycleEvent.STARTED, ComponentLifecycleEvent.UPDATED, ComponentLifecycleEvent.STOPPED);
        }

        if (!trackedEvents.contains(trigger.getEventType())) {
            return false;
        }
        if (onlyFailures) {
            return trigger.getError() != null;
        }
        return true;
    }

    @Override
    public RuleOriginatedNotificationInfo constructNotificationInfo(RuleEngineComponentLifecycleEventTrigger trigger) {
        return RuleEngineComponentLifecycleEventNotificationInfo.builder()
                .ruleChainId(trigger.getRuleChainId())
                .ruleChainName(trigger.getRuleChainName())
                .componentId(trigger.getComponentId())
                .componentName(trigger.getComponentName())
                .action(trigger.getEventType() == ComponentLifecycleEvent.STARTED ? "start" :
                        trigger.getEventType() == ComponentLifecycleEvent.UPDATED ? "update" :
                        trigger.getEventType() == ComponentLifecycleEvent.STOPPED ? "stop" : null)
                .eventType(trigger.getEventType())
                .error(getErrorMsg(trigger.getError()))
                .build();
    }

    private String getErrorMsg(Throwable error) {
        if (error == null) return null;

        StringWriter sw = new StringWriter();
        error.printStackTrace(new PrintWriter(sw));
        return StringUtils.abbreviate(ExceptionUtils.getStackTrace(error), 200);
    }

    @Override
    public NotificationRuleTriggerType getTriggerType() {
        return NotificationRuleTriggerType.RULE_ENGINE_COMPONENT_LIFECYCLE_EVENT;
    }

}
