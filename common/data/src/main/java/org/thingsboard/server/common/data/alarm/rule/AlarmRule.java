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
import org.thingsboard.server.common.data.device.profile.AlarmRuleConfiguration;
import org.thingsboard.server.common.data.id.AlarmRuleId;

import javax.validation.Valid;

@ApiModel
@Data
@EqualsAndHashCode(callSuper = true)
public class AlarmRule extends AlarmRuleInfo {

    private static final long serialVersionUID = -8491640876881610526L;

    @Valid
    @ApiModelProperty(position = 8, value = "JSON object with Alarm Rule Configuration")
    private AlarmRuleConfiguration configuration;

    public AlarmRule() {
        super();
    }

    public AlarmRule(AlarmRuleId id) {
        super(id);
    }

    public AlarmRule(AlarmRule alarmRule) {
        super(alarmRule);
        this.configuration = alarmRule.getConfiguration();
    }
}
