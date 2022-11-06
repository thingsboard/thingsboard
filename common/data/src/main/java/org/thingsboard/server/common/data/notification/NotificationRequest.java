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
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.NotificationRequestId;
import org.thingsboard.server.common.data.id.NotificationRuleId;
import org.thingsboard.server.common.data.id.NotificationTargetId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.validation.NoXss;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationRequest extends BaseData<NotificationRequestId> implements HasTenantId, HasName {

    private TenantId tenantId;
    @NotNull(message = "Target is not specified")
    private NotificationTargetId targetId;
    @NoXss
    private String notificationReason; // "Alarm", "Scheduled event". "General" by default
    //    @NoXss
    @NotBlank(message = "Notification text template is missing")
    private String textTemplate;
    @Valid
    private NotificationInfo notificationInfo;
    private NotificationSeverity notificationSeverity;
    private NotificationRequestConfig additionalConfig;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private NotificationRequestStatus status;

    @JsonIgnore
    private NotificationRuleId ruleId; // maybe move to child class
    @JsonIgnore
    private AlarmId alarmId;

    public static final String GENERAL_NOTIFICATION_REASON = "General";
    public static final String ALARM_NOTIFICATION_REASON = "Alarm";

    @Override
    public String getName() {
        return notificationReason;
    }

}
