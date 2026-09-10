// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
