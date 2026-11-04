// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.common.data.housekeeper;

import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.alarm.AlarmRef;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;

import java.io.Serial;
import java.util.List;
import java.util.UUID;

@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AlarmsUnassignHousekeeperTask extends HousekeeperTask {

    @Serial
    private static final long serialVersionUID = 9156667024462937756L;

    private String userTitle;
    // Retained so tasks serialized by older nodes (which carried only alarm ids) still deserialize; those fall back to
    // a per-alarm originator lookup during processing. New tasks populate alarmRefs instead. For backward
    // compatibility with pre-4.3.1.4 tasks only.
    @Deprecated(since = "4.3.1.4", forRemoval = true)
    private List<UUID> alarms;
    // Alarm (id, originator) refs so the deleted-user unassign routes each alarm to its owning shard without a
    // per-alarm AlarmInfo lookup.
    private List<AlarmRef> alarmRefs;

    protected AlarmsUnassignHousekeeperTask(User user) {
        super(user.getTenantId(), user.getId(), HousekeeperTaskType.UNASSIGN_ALARMS);
        this.userTitle = user.getTitle();
    }

    public AlarmsUnassignHousekeeperTask(TenantId tenantId, UserId userId, String userTitle, List<AlarmRef> alarmRefs) {
        super(tenantId, userId, HousekeeperTaskType.UNASSIGN_ALARMS);
        this.userTitle = userTitle;
        this.alarmRefs = alarmRefs;
    }

    @Override
    public String getDescription() {
        List<?> batch = alarmRefs != null ? alarmRefs : alarms;
        return super.getDescription() + (batch != null ? " (" + batch + ")" : "");
    }

}
