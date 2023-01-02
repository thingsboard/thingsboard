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
package org.thingsboard.server.common.data.alarm.rule;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.HasCustomerId;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.SearchTextBased;
import org.thingsboard.server.common.data.id.AlarmRuleId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.validation.NoXss;

@ApiModel
@Data
@EqualsAndHashCode(callSuper = true)
public class AlarmRuleInfo extends SearchTextBased<AlarmRuleId> implements HasName, HasTenantId, HasCustomerId {

    private static final long serialVersionUID = -8353967477463356805L;

    @ApiModelProperty(position = 3, value = "JSON object with Tenant Id", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private TenantId tenantId;

    @ApiModelProperty(position = 4, value = "JSON object with Customer Id", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private CustomerId customerId;

    @NoXss
    @ApiModelProperty(position = 5, value = "JSON object with Tenant Id", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private String alarmType;

    @NoXss
    @ApiModelProperty(position = 6, value = "JSON object with Tenant Id", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private String name;

    @ApiModelProperty(position = 7, value = "JSON object with Tenant Id", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private boolean enabled;

    @NoXss
    @ApiModelProperty(position = 8, value = "Alarm rule description. ")
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
        this.customerId = alarmRuleInfo.getCustomerId();
        this.alarmType = alarmRuleInfo.getAlarmType();
        this.name = alarmRuleInfo.getName();
        this.enabled = alarmRuleInfo.isEnabled();
        this.description = alarmRuleInfo.getDescription();
    }

    @Override
    public String getSearchText() {
        return getName();
    }
}
