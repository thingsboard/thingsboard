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
package org.thingsboard.server.service.notification.rule.trigger;

import com.google.common.base.Strings;
import lombok.Builder;
import lombok.Data;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.notification.info.NotificationInfo;
import org.thingsboard.server.common.data.notification.info.RuleEngineComponentLifecycleEventNotificationInfo;
import org.thingsboard.server.common.data.notification.rule.trigger.NotificationRuleTriggerType;
import org.thingsboard.server.common.data.notification.rule.trigger.RuleEngineComponentLifecycleEventNotificationRuleTriggerConfig;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.service.notification.rule.trigger.RuleEngineComponentLifecycleEventTriggerProcessor.RuleEngineComponentLifecycleEventTriggerObject;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Set;

@Service
public class RuleEngineComponentLifecycleEventTriggerProcessor implements NotificationRuleTriggerProcessor<RuleEngineComponentLifecycleEventTriggerObject, RuleEngineComponentLifecycleEventNotificationRuleTriggerConfig> {

    @Override
    public boolean matchesFilter(RuleEngineComponentLifecycleEventTriggerObject triggerObject, RuleEngineComponentLifecycleEventNotificationRuleTriggerConfig triggerConfig) {
        if (CollectionUtils.isNotEmpty(triggerConfig.getRuleChains())) {
            if (!triggerConfig.getRuleChains().contains(triggerObject.getRuleChainId().getId())) {
                return false;
            }
        }

        EntityType componentType = triggerObject.getComponentId().getEntityType();
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

        if (!trackedEvents.contains(triggerObject.getEventType())) {
            return false;
        }
        if (onlyFailures) {
            return triggerObject.getError() != null;
        }
        return true;
    }

    @Override
    public NotificationInfo constructNotificationInfo(RuleEngineComponentLifecycleEventTriggerObject triggerObject, RuleEngineComponentLifecycleEventNotificationRuleTriggerConfig triggerConfig) {
        return RuleEngineComponentLifecycleEventNotificationInfo.builder()
                .ruleChainId(triggerObject.getRuleChainId())
                .ruleChainName(triggerObject.getRuleChainName())
                .componentId(triggerObject.getComponentId())
                .componentName(triggerObject.getComponentName())
                .action(triggerObject.getEventType() == ComponentLifecycleEvent.STARTED ? "start" :
                        triggerObject.getEventType() == ComponentLifecycleEvent.UPDATED ? "update" :
                        triggerObject.getEventType() == ComponentLifecycleEvent.STOPPED ? "stop" : null)
                .eventType(triggerObject.getEventType())
                .error(getErrorMsg(triggerObject.getError()))
                .build();
    }

    private String getErrorMsg(Exception error) {
        if (error == null) return null;

        StringWriter sw = new StringWriter();
        error.printStackTrace(new PrintWriter(sw));
        return StringUtils.abbreviate(ExceptionUtils.getStackTrace(error), 200);
    }

    @Override
    public NotificationRuleTriggerType getTriggerType() {
        return NotificationRuleTriggerType.RULE_ENGINE_COMPONENT_LIFECYCLE_EVENT;
    }

    @Data
    @Builder
    public static class RuleEngineComponentLifecycleEventTriggerObject {
        private final RuleChainId ruleChainId;
        private final String ruleChainName;
        private final EntityId componentId;
        private final String componentName;
        private final ComponentLifecycleEvent eventType;
        private final Exception error;
    }

}
