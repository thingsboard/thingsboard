// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.alarm;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Schema
public class AlarmInfo extends Alarm {

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
        this.originatorName = alarmInfo.originatorName;
        this.originatorLabel = alarmInfo.originatorLabel;
        this.assignee = alarmInfo.getAssignee();
    }

    public AlarmInfo(Alarm alarm, String originatorName, String originatorLabel, AlarmAssignee assignee) {
        super(alarm);
        this.originatorName = originatorName;
        this.originatorLabel = originatorLabel;
        this.assignee = assignee;
    }

}
