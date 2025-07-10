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
package org.thingsboard.server.service.cf.ctx.state;

import lombok.Data;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.geo.PerimeterDefinition;
import org.thingsboard.script.api.tbel.TbelCfArg;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.KvEntry;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

// TODO: implement
@Data
public class GeofencingArgumentEntry implements ArgumentEntry {

    private Map<EntityId, PerimeterDefinition> geofencingIdToPerimeter;
    private boolean forceResetPrevious;

    public GeofencingArgumentEntry(Map<EntityId, KvEntry> entityIdKvEntryMap) {
        this.geofencingIdToPerimeter = toPerimetersMap(entityIdKvEntryMap);
    }

    @Override
    public ArgumentEntryType getType() {
        return ArgumentEntryType.GEOFENCING;
    }

    @Override
    public Object getValue() {
        return geofencingIdToPerimeter;
    }

    @Override
    public boolean updateEntry(ArgumentEntry entry) {
        if (!(entry instanceof GeofencingArgumentEntry geofencingArgumentEntry)) {
            throw new IllegalArgumentException("Unsupported argument entry type for geofencing argument entry: " + entry.getType());
        }
        if (Objects.equals(this.geofencingIdToPerimeter, geofencingArgumentEntry.getGeofencingIdToPerimeter())) {
            return false; // No change
        }
        this.geofencingIdToPerimeter = geofencingArgumentEntry.getGeofencingIdToPerimeter();
        return true;
    }

    @Override
    public boolean isEmpty() {
        return geofencingIdToPerimeter == null || geofencingIdToPerimeter.isEmpty();
    }

    @Override
    public TbelCfArg toTbelCfArg() {
        return null;
    }

    private Map<EntityId, PerimeterDefinition> toPerimetersMap(Map<EntityId, KvEntry> entityIdKvEntryMap) {
        return entityIdKvEntryMap.entrySet().stream().map(entry -> {
                    if (entry.getValue().getJsonValue().isEmpty()) {
                        return null;
                    }
                    String rawPerimeterValue = entry.getValue().getJsonValue().get();
                    PerimeterDefinition perimeter = JacksonUtil.fromString(rawPerimeterValue, PerimeterDefinition.class);
                    return Map.entry(entry.getKey(), perimeter);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

}
