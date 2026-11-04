// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.entitiy.alarm;

import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmRef;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;

import java.util.List;
import java.util.UUID;

public interface TbAlarmService {

    Alarm save(Alarm entity, User user) throws ThingsboardException;

    AlarmInfo ack(Alarm alarm, User user) throws ThingsboardException;

    AlarmInfo ack(Alarm alarm, long ackTs, User user) throws ThingsboardException;

    AlarmInfo clear(Alarm alarm, User user) throws ThingsboardException;

    AlarmInfo clear(Alarm alarm, long clearTs, User user) throws ThingsboardException;

    AlarmInfo assign(Alarm alarm, UserId assigneeId, long assignTs, User user) throws ThingsboardException;

    AlarmInfo unassign(Alarm alarm, long unassignTs, User user) throws ThingsboardException;

    void unassignDeletedUserAlarms(TenantId tenantId, UserId userId, String userTitle, List<AlarmRef> alarmRefs, long unassignTs);

    // Backward-compatible fallback for in-flight tasks that carry only alarm ids (no originator): the originator is
    // resolved per alarm before routing the unassign. For backward compatibility with pre-4.3.1.4 tasks only.
    @Deprecated(since = "4.3.1.4", forRemoval = true)
    void unassignDeletedUserAlarmsByIds(TenantId tenantId, UserId userId, String userTitle, List<UUID> alarmIds, long unassignTs);

    boolean delete(Alarm alarm, User user);

}
