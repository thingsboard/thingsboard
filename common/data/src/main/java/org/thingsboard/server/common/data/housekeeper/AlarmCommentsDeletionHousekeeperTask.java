// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.housekeeper;

import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;

import java.io.Serial;
import java.util.List;
import java.util.UUID;

@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AlarmCommentsDeletionHousekeeperTask extends HousekeeperTask {

    @Serial
    private static final long serialVersionUID = 3661985779441436080L;

    // When null, the task deletes comments for the single alarm in entityId (the shape CleanUpService produces for a
    // single-alarm cleanup); when set, it bulk-deletes the comments of all listed alarms in one statement.
    private List<UUID> alarms;

    public AlarmCommentsDeletionHousekeeperTask(TenantId tenantId, AlarmId alarmId) {
        super(tenantId, alarmId, HousekeeperTaskType.DELETE_ALARM_COMMENTS);
    }

    // contextEntityId is description/log context only; the deletion keys off the alarms list.
    public AlarmCommentsDeletionHousekeeperTask(TenantId tenantId, EntityId contextEntityId, List<UUID> alarms) {
        super(tenantId, contextEntityId, HousekeeperTaskType.DELETE_ALARM_COMMENTS);
        this.alarms = alarms;
    }

    @Override
    public String getDescription() {
        return super.getDescription() + (alarms != null ? " (" + alarms + ")" : "");
    }

}
