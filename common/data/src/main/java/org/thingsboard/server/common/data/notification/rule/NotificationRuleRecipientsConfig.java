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
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.thingsboard.server.common.data.notification.rule.trigger.config.NotificationRuleTriggerType;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
