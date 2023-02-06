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
package org.thingsboard.server.common.data.device.profile;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.alarm.rule.AlarmRuleTargetEntity;
import org.thingsboard.server.common.data.alarm.rule.filter.AlarmRuleEntityFilter;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

import javax.validation.Valid;
import java.io.Serializable;
import java.util.List;
import java.util.TreeMap;

@ApiModel
@Data
public class AlarmRuleConfiguration implements Serializable {

    private static final long serialVersionUID = 4050882336403853271L;

    @ApiModelProperty(position = 3, value = "List of JSON objects representing source entity filters. These filters select a certain entity on which the specific Alarm Rule condition will be applied.")
    private List<AlarmRuleEntityFilter> sourceEntityFilters;

    @ApiModelProperty(position = 4, value = "JSON object representing target entity where the alarm will be created")
    private AlarmRuleTargetEntity alarmTargetEntity;

    @Valid
    @ApiModelProperty(position = 5, value = "Complex JSON object representing create alarm rules. The unique create alarm rule can be created for each alarm severity type. " +
            "There can be 5 create alarm rules configured per a single alarm type. See method implementation notes and AlarmRule model for more details")
    private TreeMap<AlarmSeverity, AlarmRuleCondition> createRules;
    @Valid
    @ApiModelProperty(position = 6, value = "JSON object representing clear alarm rule")
    private AlarmRuleCondition clearRule;

    // Hidden in advanced settings
    @ApiModelProperty(position = 7, value = "Propagation flag to specify if alarm should be propagated to parent entities of alarm originator", example = "true")
    private boolean propagate;
    @ApiModelProperty(position = 8, value = "Propagation flag to specify if alarm should be propagated to the owner (tenant or customer) of alarm originator", example = "true")
    private boolean propagateToOwner;
    @ApiModelProperty(position = 9, value = "Propagation flag to specify if alarm should be propagated to the tenant entity", example = "true")
    private boolean propagateToTenant;

    @ApiModelProperty(position = 10, value = "JSON array of relation types that should be used for propagation. " +
            "By default, 'propagateRelationTypes' array is empty which means that the alarm will be propagated based on any relation type to parent entities. " +
            "This parameter should be used only in case when 'propagate' parameter is set to true, otherwise, 'propagateRelationTypes' array will be ignored.")
    private List<String> propagateRelationTypes;

}
