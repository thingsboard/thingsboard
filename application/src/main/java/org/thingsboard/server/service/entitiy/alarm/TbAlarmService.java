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
package org.thingsboard.server.service.entitiy.alarm;

import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
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

    void unassignDeletedUserAlarms(TenantId tenantId, UserId userId, String userTitle, List<UUID> alarms, long unassignTs);

    boolean delete(Alarm alarm, User user);

}
