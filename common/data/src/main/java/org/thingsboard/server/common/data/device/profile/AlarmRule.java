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
package org.thingsboard.server.common.data.device.profile;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.Data;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.validation.NoXss;

import java.io.Serializable;

@Schema
@Data
public class AlarmRule implements Serializable {

    @Valid
    @Schema(description = "JSON object representing the alarm rule condition")
    private AlarmCondition condition;
    @Schema(description = "JSON object representing time interval during which the rule is active")
    private AlarmSchedule schedule;
    // Advanced
    @NoXss
    @Schema(description = "String value representing the additional details for an alarm rule")
    private String alarmDetails;
    @Schema(description = "JSON object with the dashboard Id representing the reference to alarm details dashboard used by mobile application")
    private DashboardId dashboardId;

}
