/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
package org.thingsboard.server.common.data.alarm.rule;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.id.AlarmRuleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

@Schema
@Data
@EqualsAndHashCode(callSuper = true)
public class AlarmRuleInfo extends BaseData<AlarmRuleId> implements HasName, HasTenantId {

    private static final long serialVersionUID = -8353967477463356805L;

    @Schema(description = "JSON object with Tenant Id", accessMode = Schema.AccessMode.READ_ONLY)
    private TenantId tenantId;

    @NoXss
    @Length(fieldName = "name")
    @Schema(description = "Unique Alarm Rule Name in scope of Tenant", example = "High Temperature Alarm Rule")
    private String name;

    @NoXss
    @Length(fieldName = "alarm type")
    @Schema(description = "String value representing type of the alarm", example = "High Temperature Alarm")
    private String alarmType;

    @Schema(description = "Boolean value representing is alarm rule enabled")
    private boolean enabled;

    @NoXss
    @Schema(description = "Alarm rule description.")
    private String description;

    public AlarmRuleInfo() {
        super();
    }

    public AlarmRuleInfo(AlarmRuleId id) {
        super(id);
    }

    public AlarmRuleInfo(AlarmRuleInfo alarmRuleInfo) {
        super(alarmRuleInfo.getId());
        this.createdTime = alarmRuleInfo.getCreatedTime();
        this.tenantId = alarmRuleInfo.getTenantId();
        this.alarmType = alarmRuleInfo.getAlarmType();
        this.name = alarmRuleInfo.getName();
        this.enabled = alarmRuleInfo.isEnabled();
        this.description = alarmRuleInfo.getDescription();
    }

}
