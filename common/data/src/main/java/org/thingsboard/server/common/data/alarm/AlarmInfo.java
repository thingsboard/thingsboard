/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.server.common.data.alarm;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;

@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Schema
public class AlarmInfo extends Alarm {

    @Serial
    private static final long serialVersionUID = 2807343093519543363L;

    @Getter
    @Setter
    @Schema(description = "Alarm originator name", example = "Thermostat")
    private String originatorName;

    @Getter
    @Setter
    @Schema(description = "Alarm originator label", example = "Thermostat label")
    private String originatorLabel;

    @Getter
    @Setter
    @Schema(description = "Originator display name", example = "Thermostat")
    private String originatorDisplayName;

    @Getter
    @Setter
    @Schema(description = "Alarm assignee")
    private AlarmAssignee assignee;

    public AlarmInfo() {
        super();
    }

    public AlarmInfo(Alarm alarm) {
        super(alarm);
    }

    public AlarmInfo(AlarmInfo alarmInfo) {
        super(alarmInfo);
        this.originatorName = alarmInfo.getOriginatorName();
        this.originatorLabel = alarmInfo.getOriginatorLabel();
        this.assignee = alarmInfo.getAssignee();
    }

    public AlarmInfo(Alarm alarm, String originatorName, String originatorLabel, AlarmAssignee assignee) {
        super(alarm);
        this.originatorName = originatorName;
        this.originatorLabel = originatorLabel;
        this.assignee = assignee;
    }

}
