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
package org.thingsboard.server.common.data.device.profile;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.validation.NoXss;

import javax.validation.Valid;
import java.io.Serializable;

@ApiModel
@Data
public class AlarmRuleCondition implements Serializable {

    private static final long serialVersionUID = -2205295678085379808L;
    @Valid
    @ApiModelProperty(position = 1, value = "JSON object representing the alarm rule condition")
    private AlarmCondition condition;
    @ApiModelProperty(position = 2, value = "JSON object representing time interval during which the rule is active")
    private AlarmSchedule schedule;
    // Advanced
    @NoXss
    @ApiModelProperty(position = 3, value = "String value representing the additional details for an alarm rule")
    private String alarmDetails;
    @ApiModelProperty(position = 4, value = "JSON object with the dashboard Id representing the reference to alarm details dashboard used by mobile application")
    private DashboardId dashboardId;

}
