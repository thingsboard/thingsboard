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
package org.thingsboard.server.dao.model.sql;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.alarm.AlarmAssignee;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.id.UserId;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import static org.thingsboard.server.dao.model.ModelConstants.ALARM_ASSIGNEE_EMAIL_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.ALARM_ASSIGNEE_FIRST_NAME_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.ALARM_ASSIGNEE_LAST_NAME_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.ALARM_ORIGINATOR_LABEL_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.ALARM_ORIGINATOR_NAME_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.ALARM_VIEW_NAME;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = ALARM_VIEW_NAME)
public class AlarmInfoEntity extends AbstractAlarmEntity<AlarmInfo> {

    @Column(name = ALARM_ORIGINATOR_NAME_PROPERTY)
    private String originatorName;
    @Column(name = ALARM_ORIGINATOR_LABEL_PROPERTY)
    private String originatorLabel;
    @Column(name = ALARM_ASSIGNEE_FIRST_NAME_PROPERTY)
    private String assigneeFirstName;
    @Column(name = ALARM_ASSIGNEE_LAST_NAME_PROPERTY)
    private String assigneeLastName;
    @Column(name = ALARM_ASSIGNEE_EMAIL_PROPERTY)
    private String assigneeEmail;

    public AlarmInfoEntity() {
        super();
    }

    public AlarmInfoEntity(AlarmEntity alarmEntity,
                           String assigneeFirstName,
                           String assigneeLastName,
                           String assigneeEmail) {
        super(alarmEntity);
        this.assigneeFirstName = assigneeFirstName;
        this.assigneeLastName = assigneeLastName;
        this.assigneeEmail = assigneeEmail;
    }

    @Override
    public AlarmInfo toData() {
        AlarmInfo alarmInfo = new AlarmInfo(super.toAlarm());
        alarmInfo.setOriginatorName(originatorName);
        alarmInfo.setOriginatorLabel(originatorLabel);
        if (getAssigneeId() != null) {
            alarmInfo.setAssignee(new AlarmAssignee(new UserId(getAssigneeId()), assigneeFirstName, assigneeLastName, assigneeEmail));
        }
        return alarmInfo;
    }
}
