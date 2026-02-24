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
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.thingsboard.server.common.data.notification.rule.trigger.config.NotificationRuleTriggerType;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Schema(
        discriminatorProperty = "triggerType",
        discriminatorMapping = {
                @DiscriminatorMapping(value = "ENTITY_ACTION", schema = DefaultNotificationRuleRecipientsConfig.class),
                @DiscriminatorMapping(value = "ALARM", schema = EscalatedNotificationRuleRecipientsConfig.class),
                @DiscriminatorMapping(value = "ALARM_COMMENT", schema = DefaultNotificationRuleRecipientsConfig.class),
                @DiscriminatorMapping(value = "ALARM_ASSIGNMENT", schema = DefaultNotificationRuleRecipientsConfig.class),
                @DiscriminatorMapping(value = "DEVICE_ACTIVITY", schema = DefaultNotificationRuleRecipientsConfig.class),
                @DiscriminatorMapping(value = "RULE_ENGINE_COMPONENT_LIFECYCLE_EVENT", schema = DefaultNotificationRuleRecipientsConfig.class),
                @DiscriminatorMapping(value = "EDGE_CONNECTION", schema = DefaultNotificationRuleRecipientsConfig.class),
                @DiscriminatorMapping(value = "EDGE_COMMUNICATION_FAILURE", schema = DefaultNotificationRuleRecipientsConfig.class),
                @DiscriminatorMapping(value = "NEW_PLATFORM_VERSION", schema = DefaultNotificationRuleRecipientsConfig.class),
                @DiscriminatorMapping(value = "ENTITIES_LIMIT", schema = DefaultNotificationRuleRecipientsConfig.class),
                @DiscriminatorMapping(value = "API_USAGE_LIMIT", schema = DefaultNotificationRuleRecipientsConfig.class),
                @DiscriminatorMapping(value = "RATE_LIMITS", schema = DefaultNotificationRuleRecipientsConfig.class),
                @DiscriminatorMapping(value = "TASK_PROCESSING_FAILURE", schema = DefaultNotificationRuleRecipientsConfig.class),
                @DiscriminatorMapping(value = "RESOURCES_SHORTAGE", schema = DefaultNotificationRuleRecipientsConfig.class)
        })
@JsonIgnoreProperties
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "triggerType", visible = true, include = JsonTypeInfo.As.EXISTING_PROPERTY, defaultImpl = DefaultNotificationRuleRecipientsConfig.class)
@JsonSubTypes({
        @Type(name = "ALARM", value = EscalatedNotificationRuleRecipientsConfig.class),
})
@Data
public abstract class NotificationRuleRecipientsConfig implements Serializable {

    @NotNull
    private NotificationRuleTriggerType triggerType;

    @JsonIgnore
    public abstract Map<Integer, List<UUID>> getTargetsTable();

}
