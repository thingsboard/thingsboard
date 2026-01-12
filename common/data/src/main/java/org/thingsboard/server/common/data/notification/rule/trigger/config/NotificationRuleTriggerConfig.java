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
package org.thingsboard.server.common.data.notification.rule.trigger.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "triggerType")
@JsonSubTypes({
        @Type(value = AlarmNotificationRuleTriggerConfig.class, name = "ALARM"),
        @Type(value = DeviceActivityNotificationRuleTriggerConfig.class, name = "DEVICE_ACTIVITY"),
        @Type(value = EntityActionNotificationRuleTriggerConfig.class, name = "ENTITY_ACTION"),
        @Type(value = AlarmCommentNotificationRuleTriggerConfig.class, name = "ALARM_COMMENT"),
        @Type(value = RuleEngineComponentLifecycleEventNotificationRuleTriggerConfig.class, name = "RULE_ENGINE_COMPONENT_LIFECYCLE_EVENT"),
        @Type(value = AlarmAssignmentNotificationRuleTriggerConfig.class, name = "ALARM_ASSIGNMENT"),
        @Type(value = NewPlatformVersionNotificationRuleTriggerConfig.class, name = "NEW_PLATFORM_VERSION"),
        @Type(value = EntitiesLimitNotificationRuleTriggerConfig.class, name = "ENTITIES_LIMIT"),
        @Type(value = ApiUsageLimitNotificationRuleTriggerConfig.class, name = "API_USAGE_LIMIT"),
        @Type(value = RateLimitsNotificationRuleTriggerConfig.class, name = "RATE_LIMITS"),
        @Type(value = EdgeConnectionNotificationRuleTriggerConfig.class, name = "EDGE_CONNECTION"),
        @Type(value = EdgeCommunicationFailureNotificationRuleTriggerConfig.class, name = "EDGE_COMMUNICATION_FAILURE"),
        @Type(value = TaskProcessingFailureNotificationRuleTriggerConfig.class, name = "TASK_PROCESSING_FAILURE"),
        @Type(value = ResourcesShortageNotificationRuleTriggerConfig.class, name = "RESOURCES_SHORTAGE")
})
public interface NotificationRuleTriggerConfig extends Serializable {

    NotificationRuleTriggerType getTriggerType();

    @JsonIgnore
    default String getDeduplicationKey() {
        return "#";
    }

}
