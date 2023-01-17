/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.id.NotificationRuleId;
import org.thingsboard.server.common.data.id.NotificationTemplateId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.rule.trigger.NotificationRuleTriggerConfig;
import org.thingsboard.server.common.data.notification.rule.trigger.NotificationRuleTriggerType;

import javax.validation.Valid;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
@EqualsAndHashCode(callSuper = true)
public class NotificationRule extends BaseData<NotificationRuleId> implements HasTenantId, HasName {

    private TenantId tenantId;
    @NotBlank
    private String name;
    @NotNull
    private NotificationTemplateId templateId;

    @NotNull
    private NotificationRuleTriggerType triggerType;
    @NotNull
    private NotificationRuleTriggerConfig triggerConfig;
    @NotNull
    @Valid
    private NotificationRuleRecipientsConfig recipientsConfig; // todo: add pg_tgrm index (but index is 2.5x size of the column)

    @JsonIgnore
    @AssertTrue(message = "trigger type not matching")
    public boolean isValid() {
        return triggerType == triggerConfig.getTriggerType() &&
                triggerType == recipientsConfig.getTriggerType();
    }

}
