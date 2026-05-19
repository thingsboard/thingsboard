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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;
import org.thingsboard.server.common.data.notification.rule.trigger.config.NotificationRuleTriggerType;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Schema(
        discriminatorProperty = "triggerType",
        discriminatorMapping = {
                @DiscriminatorMapping(value = "ENTITY_ACTION", schema = DefaultNotificationRuleRecipientsConfig.EntityActionRecipientsConfig.class),
                @DiscriminatorMapping(value = "ALARM", schema = EscalatedNotificationRuleRecipientsConfig.class),
                @DiscriminatorMapping(value = "ALARM_COMMENT", schema = DefaultNotificationRuleRecipientsConfig.AlarmCommentRecipientsConfig.class),
                @DiscriminatorMapping(value = "ALARM_ASSIGNMENT", schema = DefaultNotificationRuleRecipientsConfig.AlarmAssignmentRecipientsConfig.class),
                @DiscriminatorMapping(value = "DEVICE_ACTIVITY", schema = DefaultNotificationRuleRecipientsConfig.DeviceActivityRecipientsConfig.class),
                @DiscriminatorMapping(value = "RULE_ENGINE_COMPONENT_LIFECYCLE_EVENT", schema = DefaultNotificationRuleRecipientsConfig.RuleEngineComponentLifecycleEventRecipientsConfig.class),
                @DiscriminatorMapping(value = "EDGE_CONNECTION", schema = DefaultNotificationRuleRecipientsConfig.EdgeConnectionRecipientsConfig.class),
                @DiscriminatorMapping(value = "EDGE_COMMUNICATION_FAILURE", schema = DefaultNotificationRuleRecipientsConfig.EdgeCommunicationFailureRecipientsConfig.class),
                @DiscriminatorMapping(value = "NEW_PLATFORM_VERSION", schema = DefaultNotificationRuleRecipientsConfig.NewPlatformVersionRecipientsConfig.class),
                @DiscriminatorMapping(value = "ENTITIES_LIMIT", schema = DefaultNotificationRuleRecipientsConfig.EntitiesLimitRecipientsConfig.class),
                @DiscriminatorMapping(value = "API_USAGE_LIMIT", schema = DefaultNotificationRuleRecipientsConfig.ApiUsageLimitRecipientsConfig.class),
                @DiscriminatorMapping(value = "RATE_LIMITS", schema = DefaultNotificationRuleRecipientsConfig.RateLimitsRecipientsConfig.class),
                @DiscriminatorMapping(value = "TASK_PROCESSING_FAILURE", schema = DefaultNotificationRuleRecipientsConfig.TaskProcessingFailureRecipientsConfig.class),
                @DiscriminatorMapping(value = "RESOURCES_SHORTAGE", schema = DefaultNotificationRuleRecipientsConfig.ResourceShortageRecipientsConfig.class)
        })
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "triggerType", include = JsonTypeInfo.As.EXISTING_PROPERTY, defaultImpl = DefaultNotificationRuleRecipientsConfig.class)
@JsonSubTypes({
        @Type(name = "ALARM", value = EscalatedNotificationRuleRecipientsConfig.class),
        @Type(name = "ENTITY_ACTION", value = DefaultNotificationRuleRecipientsConfig.EntityActionRecipientsConfig.class),
        @Type(name = "ALARM_COMMENT", value = DefaultNotificationRuleRecipientsConfig.AlarmCommentRecipientsConfig.class),
        @Type(name = "ALARM_ASSIGNMENT", value = DefaultNotificationRuleRecipientsConfig.AlarmAssignmentRecipientsConfig.class),
        @Type(name = "DEVICE_ACTIVITY", value = DefaultNotificationRuleRecipientsConfig.DeviceActivityRecipientsConfig.class),
        @Type(name = "RULE_ENGINE_COMPONENT_LIFECYCLE_EVENT", value = DefaultNotificationRuleRecipientsConfig.RuleEngineComponentLifecycleEventRecipientsConfig.class),
        @Type(name = "EDGE_CONNECTION", value = DefaultNotificationRuleRecipientsConfig.EdgeConnectionRecipientsConfig.class),
        @Type(name = "EDGE_COMMUNICATION_FAILURE", value = DefaultNotificationRuleRecipientsConfig.EdgeCommunicationFailureRecipientsConfig.class),
        @Type(name = "NEW_PLATFORM_VERSION", value = DefaultNotificationRuleRecipientsConfig.NewPlatformVersionRecipientsConfig.class),
        @Type(name = "ENTITIES_LIMIT", value = DefaultNotificationRuleRecipientsConfig.EntitiesLimitRecipientsConfig.class),
        @Type(name = "API_USAGE_LIMIT", value = DefaultNotificationRuleRecipientsConfig.ApiUsageLimitRecipientsConfig.class),
        @Type(name = "RATE_LIMITS", value = DefaultNotificationRuleRecipientsConfig.RateLimitsRecipientsConfig.class),
        @Type(name = "TASK_PROCESSING_FAILURE", value = DefaultNotificationRuleRecipientsConfig.TaskProcessingFailureRecipientsConfig.class),
        @Type(name = "RESOURCES_SHORTAGE", value = DefaultNotificationRuleRecipientsConfig.ResourceShortageRecipientsConfig.class)
})
public interface NotificationRuleRecipientsConfig extends Serializable {

    NotificationRuleTriggerType getTriggerType();

    @JsonIgnore
    Map<Integer, List<UUID>> getTargetsTable();

}
