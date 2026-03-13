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
package org.thingsboard.server.common.data.notification.rule;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.notification.rule.trigger.config.NotificationRuleTriggerType;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Schema(description = "Default notification rule recipients configuration")
@Data
@EqualsAndHashCode
public abstract class DefaultNotificationRuleRecipientsConfig implements NotificationRuleRecipientsConfig {

    @NotEmpty
    private List<UUID> targets;

    @Override
    public Map<Integer, List<UUID>> getTargetsTable() {
        return Map.of(0, targets);
    }

    public static DefaultNotificationRuleRecipientsConfig forTriggerType(NotificationRuleTriggerType triggerType) {
        return switch (triggerType) {
            case ENTITY_ACTION -> new EntityActionRecipientsConfig();
            case ALARM_COMMENT -> new AlarmCommentRecipientsConfig();
            case ALARM_ASSIGNMENT -> new AlarmAssignmentRecipientsConfig();
            case DEVICE_ACTIVITY -> new DeviceActivityRecipientsConfig();
            case RULE_ENGINE_COMPONENT_LIFECYCLE_EVENT -> new RuleEngineComponentLifecycleEventRecipientsConfig();
            case EDGE_CONNECTION -> new EdgeConnectionRecipientsConfig();
            case EDGE_COMMUNICATION_FAILURE -> new EdgeCommunicationFailureRecipientsConfig();
            case NEW_PLATFORM_VERSION -> new NewPlatformVersionRecipientsConfig();
            case ENTITIES_LIMIT -> new EntitiesLimitRecipientsConfig();
            case API_USAGE_LIMIT -> new ApiUsageLimitRecipientsConfig();
            case RATE_LIMITS -> new RateLimitsRecipientsConfig();
            case TASK_PROCESSING_FAILURE -> new TaskProcessingFailureRecipientsConfig();
            case RESOURCES_SHORTAGE -> new ResourceShortageRecipientsConfig();
            default -> throw new IllegalArgumentException("Unsupported trigger type for default recipients config: " + triggerType);
        };
    }

    public static class EntityActionRecipientsConfig extends DefaultNotificationRuleRecipientsConfig {
        @Override
        public NotificationRuleTriggerType getTriggerType() {
            return NotificationRuleTriggerType.ENTITY_ACTION;
        }
    }

    public static class AlarmCommentRecipientsConfig extends DefaultNotificationRuleRecipientsConfig {
        @Override
        public NotificationRuleTriggerType getTriggerType() {
            return NotificationRuleTriggerType.ALARM_COMMENT;
        }
    }

    public static class AlarmAssignmentRecipientsConfig extends DefaultNotificationRuleRecipientsConfig {
        @Override
        public NotificationRuleTriggerType getTriggerType() {
            return NotificationRuleTriggerType.ALARM_ASSIGNMENT;
        }
    }

    public static class DeviceActivityRecipientsConfig extends DefaultNotificationRuleRecipientsConfig {
        @Override
        public NotificationRuleTriggerType getTriggerType() {
            return NotificationRuleTriggerType.DEVICE_ACTIVITY;
        }
    }

    public static class RuleEngineComponentLifecycleEventRecipientsConfig extends DefaultNotificationRuleRecipientsConfig {
        @Override
        public NotificationRuleTriggerType getTriggerType() {
            return NotificationRuleTriggerType.RULE_ENGINE_COMPONENT_LIFECYCLE_EVENT;
        }
    }

    public static class EdgeConnectionRecipientsConfig extends DefaultNotificationRuleRecipientsConfig {
        @Override
        public NotificationRuleTriggerType getTriggerType() {
            return NotificationRuleTriggerType.EDGE_CONNECTION;
        }
    }

    public static class EdgeCommunicationFailureRecipientsConfig extends DefaultNotificationRuleRecipientsConfig {
        @Override
        public NotificationRuleTriggerType getTriggerType() {
            return NotificationRuleTriggerType.EDGE_COMMUNICATION_FAILURE;
        }
    }

    public static class NewPlatformVersionRecipientsConfig extends DefaultNotificationRuleRecipientsConfig {
        @Override
        public NotificationRuleTriggerType getTriggerType() {
            return NotificationRuleTriggerType.NEW_PLATFORM_VERSION;
        }
    }

    public static class EntitiesLimitRecipientsConfig extends DefaultNotificationRuleRecipientsConfig {
        @Override
        public NotificationRuleTriggerType getTriggerType() {
            return NotificationRuleTriggerType.ENTITIES_LIMIT;
        }
    }

    public static class ApiUsageLimitRecipientsConfig extends DefaultNotificationRuleRecipientsConfig {
        @Override
        public NotificationRuleTriggerType getTriggerType() {
            return NotificationRuleTriggerType.API_USAGE_LIMIT;
        }
    }

    public static class RateLimitsRecipientsConfig extends DefaultNotificationRuleRecipientsConfig {
        @Override
        public NotificationRuleTriggerType getTriggerType() {
            return NotificationRuleTriggerType.RATE_LIMITS;
        }
    }

    public static class TaskProcessingFailureRecipientsConfig extends DefaultNotificationRuleRecipientsConfig {
        @Override
        public NotificationRuleTriggerType getTriggerType() {
            return NotificationRuleTriggerType.TASK_PROCESSING_FAILURE;
        }
    }

    public static class ResourceShortageRecipientsConfig extends DefaultNotificationRuleRecipientsConfig {
        @Override
        public NotificationRuleTriggerType getTriggerType() {
            return NotificationRuleTriggerType.RESOURCES_SHORTAGE;
        }
    }

}
