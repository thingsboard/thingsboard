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
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

import java.io.Serializable;
import java.util.List;
import java.util.TreeMap;

@Schema
@Data
public class DeviceProfileAlarm implements Serializable {

    @Schema(description = "String value representing the alarm rule id", example = "highTemperatureAlarmID")
    private String id;
    @Length(fieldName = "alarm type")
    @NoXss
    @Schema(description = "String value representing type of the alarm", example = "High Temperature Alarm")
    private String alarmType;

    @Valid
    @Schema(description = "Complex JSON object representing create alarm rules. The unique create alarm rule can be created for each alarm severity type. " +
            "There can be 5 create alarm rules configured per a single alarm type. See method implementation notes and AlarmRule model for more details")
    private TreeMap<AlarmSeverity, AlarmRule> createRules;
    @Valid
    @Schema(description = "JSON object representing clear alarm rule")
    private AlarmRule clearRule;

    // Hidden in advanced settings
    @Schema(description = "Propagation flag to specify if alarm should be propagated to parent entities of alarm originator", example = "true")
    private boolean propagate;
    @Schema(description = "Propagation flag to specify if alarm should be propagated to the owner (tenant or customer) of alarm originator", example = "true")
    private boolean propagateToOwner;
    @Schema(description = "Propagation flag to specify if alarm should be propagated to the tenant entity", example = "true")
    private boolean propagateToTenant;

    @Schema(description = "JSON array of relation types that should be used for propagation. " +
            "By default, 'propagateRelationTypes' array is empty which means that the alarm will be propagated based on any relation type to parent entities. " +
            "This parameter should be used only in case when 'propagate' parameter is set to true, otherwise, 'propagateRelationTypes' array will be ignored.")
    private List<String> propagateRelationTypes;

}
