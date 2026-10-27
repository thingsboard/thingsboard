// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
