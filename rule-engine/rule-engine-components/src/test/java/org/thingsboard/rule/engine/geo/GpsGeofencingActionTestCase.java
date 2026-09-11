// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.rule.engine.geo;

import lombok.Data;
import org.thingsboard.server.common.data.id.EntityId;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Data
public class GpsGeofencingActionTestCase {

    private EntityId entityId;
    private ConcurrentMap<EntityId, EntityGeofencingState> entityStates;
    private boolean msgInside;
    private boolean reportPresenceStatusOnEachMessage;

    public GpsGeofencingActionTestCase(EntityId entityId, boolean msgInside, boolean reportPresenceStatusOnEachMessage, EntityGeofencingState entityGeofencingState) {
        this.entityId = entityId;
        this.msgInside = msgInside;
        this.reportPresenceStatusOnEachMessage = reportPresenceStatusOnEachMessage;
        this.entityStates = new ConcurrentHashMap<>();
        this.entityStates.put(entityId, entityGeofencingState);
    }

}
