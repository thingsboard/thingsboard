// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.model.sql;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmInfo;

import static org.thingsboard.server.dao.model.ModelConstants.ALARM_TABLE_NAME;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = ALARM_TABLE_NAME)
public final class AlarmEntity extends AbstractAlarmEntity<Alarm> {

    public AlarmEntity() {
        super();
    }

    public AlarmEntity(AlarmInfo alarmInfo) {
        super(alarmInfo);
    }

    public AlarmEntity(Alarm alarm) {
        super(alarm);
    }

    @Override
    public Alarm toData() {
        return super.toAlarm();
    }
}
