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
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

@Schema(
        name = "NotificationRuleTriggerConfig",
        description = "Configuration for notification rule trigger",
        discriminatorProperty = "triggerType",
        discriminatorMapping = {
                @DiscriminatorMapping(value = "ALARM", schema = AlarmNotificationRuleTriggerConfig.class),
                @DiscriminatorMapping(value = "DEVICE_ACTIVITY", schema = DeviceActivityNotificationRuleTriggerConfig.class),
                @DiscriminatorMapping(value = "ENTITY_ACTION", schema = EntityActionNotificationRuleTriggerConfig.class),
                @DiscriminatorMapping(value = "ALARM_COMMENT", schema = AlarmCommentNotificationRuleTriggerConfig.class),
                @DiscriminatorMapping(value = "RULE_ENGINE_COMPONENT_LIFECYCLE_EVENT", schema = RuleEngineComponentLifecycleEventNotificationRuleTriggerConfig.class),
                @DiscriminatorMapping(value = "ALARM_ASSIGNMENT", schema = AlarmAssignmentNotificationRuleTriggerConfig.class),
                @DiscriminatorMapping(value = "NEW_PLATFORM_VERSION", schema = NewPlatformVersionNotificationRuleTriggerConfig.class),
                @DiscriminatorMapping(value = "ENTITIES_LIMIT", schema = EntitiesLimitNotificationRuleTriggerConfig.class),
                @DiscriminatorMapping(value = "API_USAGE_LIMIT", schema = ApiUsageLimitNotificationRuleTriggerConfig.class),
                @DiscriminatorMapping(value = "RATE_LIMITS", schema = RateLimitsNotificationRuleTriggerConfig.class),
                @DiscriminatorMapping(value = "EDGE_CONNECTION", schema = EdgeConnectionNotificationRuleTriggerConfig.class),
                @DiscriminatorMapping(value = "EDGE_COMMUNICATION_FAILURE", schema = EdgeCommunicationFailureNotificationRuleTriggerConfig.class),
                @DiscriminatorMapping(value = "TASK_PROCESSING_FAILURE", schema = TaskProcessingFailureNotificationRuleTriggerConfig.class),
                @DiscriminatorMapping(value = "RESOURCES_SHORTAGE", schema = ResourcesShortageNotificationRuleTriggerConfig.class)
        }
)
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

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    NotificationRuleTriggerType getTriggerType();

    @JsonIgnore
    default String getDeduplicationKey() {
        return "#";
    }

}
