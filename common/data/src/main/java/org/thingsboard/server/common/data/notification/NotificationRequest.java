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
package org.thingsboard.server.common.data.notification;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.NotificationRequestId;
import org.thingsboard.server.common.data.id.NotificationRuleId;
import org.thingsboard.server.common.data.id.NotificationTargetId;
import org.thingsboard.server.common.data.id.NotificationTemplateId;
import org.thingsboard.server.common.data.id.TenantId;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationRequest extends BaseData<NotificationRequestId> implements HasTenantId, HasName {

    private TenantId tenantId;
    @NotNull
    private NotificationTargetId targetId;

    @NotNull
    private NotificationTemplateId templateId;
    @Valid
    private NotificationInfo info;
    @NotEmpty
    private List<NotificationDeliveryMethod> deliveryMethods;
    @NotNull
    @Valid
    private NotificationRequestConfig additionalConfig;

    private NotificationOriginatorType originatorType;
    private EntityId originatorEntityId;
    private NotificationRuleId ruleId;

    private NotificationRequestStatus status;

    private NotificationRequestStats stats;
    @JsonIgnore
    private transient Map<String, String> templateContext;

    @JsonIgnore
    @Override
    public String getName() {
        return "To target " + targetId + " via " + StringUtils.join(deliveryMethods, ", ");
    }

}
