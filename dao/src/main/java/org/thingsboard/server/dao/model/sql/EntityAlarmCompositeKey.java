// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.model.sql;

import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.alarm.EntityAlarm;

import java.io.Serializable;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class EntityAlarmCompositeKey implements Serializable {

    @Transient
    private static final long serialVersionUID = -245388185894468450L;

    private UUID entityId;
    private UUID alarmId;

    public EntityAlarmCompositeKey(EntityAlarm entityAlarm) {
        this.entityId = entityAlarm.getEntityId().getId();
        this.alarmId = entityAlarm.getAlarmId().getId();
    }
}
