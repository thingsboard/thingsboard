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
package org.thingsboard.server.dao.model.sql;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.alarm.AlarmAssignee;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.id.UserId;

import static org.thingsboard.server.dao.model.ModelConstants.ALARM_ASSIGNEE_EMAIL_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.ALARM_ASSIGNEE_FIRST_NAME_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.ALARM_ASSIGNEE_LAST_NAME_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.ALARM_ORIGINATOR_LABEL_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.ALARM_ORIGINATOR_NAME_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.ALARM_STATUS_PROPERTY;
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
    @Column(name = ALARM_STATUS_PROPERTY)
    private String status;

    public AlarmInfoEntity() {
        super();
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
