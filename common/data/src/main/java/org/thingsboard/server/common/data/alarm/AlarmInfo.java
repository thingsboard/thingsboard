// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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

    public String getOriginatorDisplayName() {
        return originatorDisplayName != null ? originatorDisplayName : (originatorLabel != null ? originatorLabel : originatorName);
    }

    public AlarmInfo(AlarmInfo alarmInfo) {
        super(alarmInfo);
        this.originatorName = alarmInfo.getOriginatorName();
        this.originatorLabel = alarmInfo.getOriginatorLabel();
        this.originatorDisplayName = alarmInfo.getOriginatorDisplayName();
        this.assignee = alarmInfo.getAssignee();
    }

    public AlarmInfo(Alarm alarm, String originatorName, String originatorLabel, AlarmAssignee assignee) {
        super(alarm);
        this.originatorName = originatorName;
        this.originatorLabel = originatorLabel;
        this.assignee = assignee;
    }

}
