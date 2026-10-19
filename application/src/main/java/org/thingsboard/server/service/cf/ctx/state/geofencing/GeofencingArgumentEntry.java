// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.cf.ctx.state.geofencing;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.script.api.tbel.TbelCfArg;
import org.thingsboard.script.api.tbel.TbelCfGeofencingArg;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.util.ProtoUtils;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.service.cf.ctx.state.ArgumentEntry;
import org.thingsboard.server.service.cf.ctx.state.ArgumentEntryType;
import org.thingsboard.server.service.cf.ctx.state.CalculatedFieldCtx;
import org.thingsboard.server.service.cf.ctx.state.HasLatestTs;

import java.util.Map;
import java.util.stream.Collectors;

import static org.thingsboard.server.service.cf.ctx.state.BaseCalculatedFieldState.DEFAULT_LAST_UPDATE_TS;

@Data
@Slf4j
public class GeofencingArgumentEntry implements ArgumentEntry, HasLatestTs {

    private Map<EntityId, GeofencingZoneState> zoneStates;

    private boolean forceResetPrevious;

    public GeofencingArgumentEntry() {
    }

    public GeofencingArgumentEntry(EntityId entityId, TransportProtos.AttributeValueProto entry) {
        this(Map.of(entityId, ProtoUtils.fromProto(entry)));
    }

    public GeofencingArgumentEntry(Map<EntityId, KvEntry> entityIdkvEntryMap) {
        this.zoneStates = toZones(entityIdkvEntryMap);
    }

    @Override
    public ArgumentEntryType getType() {
        return ArgumentEntryType.GEOFENCING;
    }

    @Override
    public Object getValue() {
        return zoneStates;
    }

    @Override
    public long getLatestTs() {
        return zoneStates.values().stream()
                .mapToLong(GeofencingZoneState::getTs).max().orElse(DEFAULT_LAST_UPDATE_TS);
    }

    @Override
    public boolean updateEntry(ArgumentEntry entry, CalculatedFieldCtx ctx) {
        if (!(entry instanceof GeofencingArgumentEntry geofencingArgumentEntry)) {
            throw new IllegalArgumentException("Unsupported argument entry type for geofencing argument entry: " + entry.getType());
        }
        if (geofencingArgumentEntry.isEmpty()) {
            zoneStates.clear();
            return true;
        }
        boolean updated = false;
        for (var zoneEntry : geofencingArgumentEntry.getZoneStates().entrySet()) {
            if (updateZone(zoneEntry)) {
                updated = true;
            }
        }
        return updated;
    }

    @Override
    public boolean isEmpty() {
        return zoneStates == null || zoneStates.isEmpty();
    }

    @Override
    public TbelCfArg toTbelCfArg() {
        return new TbelCfGeofencingArg(zoneStates);
    }

    private Map<EntityId, GeofencingZoneState> toZones(Map<EntityId, KvEntry> entityIdKvEntryMap) {
        return entityIdKvEntryMap.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> new GeofencingZoneState(entry.getKey(), entry.getValue())));
    }

    private boolean updateZone(Map.Entry<EntityId, GeofencingZoneState> zoneEntry) {
        EntityId zoneId = zoneEntry.getKey();
        GeofencingZoneState newZoneState = zoneEntry.getValue();

        GeofencingZoneState existingZoneState = zoneStates.get(zoneId);
        if (existingZoneState == null) {
            zoneStates.put(zoneId, newZoneState);
            return true;
        }
        if (newZoneState.getPerimeterDefinition() == null) {
            zoneStates.remove(zoneId);
            return true;
        }
        return existingZoneState.update(newZoneState);
    }

}
