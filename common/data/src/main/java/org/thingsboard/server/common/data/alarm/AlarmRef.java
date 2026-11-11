// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.alarm;

import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;

import java.util.UUID;

/**
 * Lightweight reference to an alarm and its originator -- for paths that route a single-shard operation by
 * originator and never need the full {@link Alarm}/{@link AlarmInfo} (TTL cleanup, bulk delete, etc.).
 * Carries the alarm's created time so keyset-paginated scans can advance a (createdTime, id) compound cursor.
 */
public record AlarmRef(AlarmId alarmId, EntityId originator, long createdTime) {

    // Convenience ctor for JPQL `SELECT new ...AlarmRef(a.id, a.originatorId, a.originatorType, a.createdTime)` projections.
    public AlarmRef(UUID alarmId, UUID originatorId, EntityType originatorType, long createdTime) {
        this(new AlarmId(alarmId), EntityIdFactory.getByTypeAndUuid(originatorType, originatorId), createdTime);
    }

}
