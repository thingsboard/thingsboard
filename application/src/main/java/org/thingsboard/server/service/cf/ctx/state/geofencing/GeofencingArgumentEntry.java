/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.server.service.cf.ctx.state.geofencing;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.script.api.tbel.TbelCfArg;
import org.thingsboard.script.api.tbel.TbelCfTsGeofencingArg;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.util.ProtoUtils;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.service.cf.ctx.state.ArgumentEntry;
import org.thingsboard.server.service.cf.ctx.state.ArgumentEntryType;

import java.util.Map;
import java.util.stream.Collectors;

@Data
@Slf4j
public class GeofencingArgumentEntry implements ArgumentEntry {

    private Map<EntityId, GeofencingZoneState> zoneStates;

    private boolean forceResetPrevious;

    public GeofencingArgumentEntry() {
    }

    public GeofencingArgumentEntry(EntityId entityId, TransportProtos.AttributeValueProto entry) {
        this.zoneStates = toZones(Map.of(entityId, ProtoUtils.fromProto(entry)));
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
    public boolean updateEntry(ArgumentEntry entry) {
        if (!(entry instanceof GeofencingArgumentEntry geofencingArgumentEntry)) {
            throw new IllegalArgumentException("Unsupported argument entry type for geofencing argument entry: " + entry.getType());
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
        return new TbelCfTsGeofencingArg(zoneStates);
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
        return existingZoneState.update(newZoneState);
    }

}
