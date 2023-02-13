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
package org.thingsboard.server.common.data.alarm;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import org.thingsboard.server.common.data.User;

import java.util.Objects;

@ApiModel
public class AlarmInfo extends Alarm {

    private static final long serialVersionUID = 2807343093519543363L;

    @Getter
    @Setter
    @ApiModelProperty(position = 19, value = "Alarm originator name", example = "Thermostat")
    private String originatorName;

    @Getter
    @Setter
    @ApiModelProperty(position = 20, value = "Alarm originator label", example = "Thermostat label")
    private String originatorLabel;

    @Getter
    @Setter
    @ApiModelProperty(position = 21, value = "Alarm assignee first name")
    private String assigneeFirstName;

    @Getter
    @Setter
    @ApiModelProperty(position = 22, value = "Alarm assignee last name")
    private String assigneeLastName;

    @Getter
    @Setter
    @ApiModelProperty(position = 23, value = "Alarm assignee email")
    private String assigneeEmail;

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
        this.assigneeFirstName = alarmInfo.assigneeFirstName;
        this.assigneeLastName = alarmInfo.assigneeLastName;
        this.assigneeEmail = alarmInfo.assigneeEmail;
    }

    public AlarmInfo(Alarm alarm, String originatorName, String originatorLabel, String assigneeFirstName, String assigneeLastName, String assigneeEmail) {
        super(alarm);
        this.originatorName = originatorName;
        this.originatorLabel = originatorLabel;
        this.assigneeFirstName = assigneeFirstName;
        this.assigneeLastName = assigneeLastName;
        this.assigneeEmail = assigneeEmail;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        AlarmInfo alarmInfo = (AlarmInfo) o;

        return (Objects.equals(originatorName, alarmInfo.originatorName)) &&
                (Objects.equals(originatorLabel, alarmInfo.originatorLabel)) &&
                (Objects.equals(assigneeFirstName, alarmInfo.assigneeFirstName)) &&
                (Objects.equals(assigneeLastName, alarmInfo.assigneeLastName)) &&
                (Objects.equals(assigneeEmail, alarmInfo.assigneeEmail));
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (originatorName != null ? originatorName.hashCode() : 0)
                + (originatorLabel != null ? originatorLabel.hashCode() : 0)
                + (assigneeFirstName != null ? assigneeFirstName.hashCode() : 0)
                + (assigneeLastName != null ? assigneeLastName.hashCode() : 0)
                + (assigneeEmail != null ? assigneeEmail.hashCode() : 0);
        return result;
    }
}
