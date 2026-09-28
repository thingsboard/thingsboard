// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.common.data.alarm;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EntityAlarm implements HasTenantId {

    private TenantId tenantId;
    private EntityId entityId;
    // The alarm originator, stored as a bare UUID: entity_alarm has no originator_type column, so only the UUID is
    // persisted (it is the alarm <-> entity_alarm co-location key). Keeping it a UUID lets the value survive a DB
    // round trip instead of being lost when the type cannot be rebuilt.
    private UUID originatorId;
    private long createdTime;
    private String alarmType;

    private CustomerId customerId;
    private UserId assigneeId;
    private AlarmId alarmId;

}
