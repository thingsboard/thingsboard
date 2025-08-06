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
import lombok.EqualsAndHashCode;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.geo.Coordinates;
import org.thingsboard.common.util.geo.PerimeterDefinition;
import org.thingsboard.rule.engine.util.GpsGeofencingEvents;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.gen.transport.TransportProtos.GeofencingZoneProto;

import java.util.UUID;

@Data
public class GeofencingZoneState {

    private final EntityId zoneId;

    private long ts;
    private Long version;
    private PerimeterDefinition perimeterDefinition;

    @EqualsAndHashCode.Exclude
    private Boolean inside;

    public GeofencingZoneState(EntityId zoneId, KvEntry entry) {
        this.zoneId = zoneId;
        if (entry instanceof TsKvEntry tsKvEntry) {
            this.ts = tsKvEntry.getTs();
            this.version = tsKvEntry.getVersion();
        } else if (entry instanceof AttributeKvEntry attributeKvEntry) {
            this.ts = attributeKvEntry.getLastUpdateTs();
            this.version = attributeKvEntry.getVersion();
        }
        this.perimeterDefinition = JacksonUtil.fromString(entry.getJsonValue().orElseThrow(), PerimeterDefinition.class);
    }

    public GeofencingZoneState(GeofencingZoneProto proto) {
        this.zoneId = EntityIdFactory.getByTypeAndUuid(proto.getZoneId().getType(),
                new UUID(proto.getZoneId().getZoneIdMSB(), proto.getZoneId().getZoneIdLSB()));
        this.ts = proto.getTs();
        this.version = proto.getVersion();
        this.perimeterDefinition = JacksonUtil.fromString(proto.getPerimeterDefinition(), PerimeterDefinition.class);
        if (proto.hasInside()) {
            this.inside = proto.getInside();
        }
    }

    public boolean update(GeofencingZoneState newZoneState) {
        if (newZoneState.getTs() <= this.ts) {
            return false;
        }
        Long newVersion = newZoneState.getVersion();
        if (newVersion == null || this.version == null || newVersion > this.version) {
            this.ts = newZoneState.getTs();
            this.version = newVersion;
            this.perimeterDefinition = newZoneState.getPerimeterDefinition();
            // TODO: should we reinitialize state if zone changed?
            // this.inside = null;
            return true;
        }
        return false;
    }

    public String evaluate(Coordinates entityCoordinates) {
        boolean inside = perimeterDefinition.checkMatches(entityCoordinates);
        // TODO: maybe handle this.inside == null as ENTERED or OUTSIDE.
        //  Since if this.inside == null then we don't have a state for this zone yet
        //  and logically say that we are OUTSIDE instead of LEFT.
        if (this.inside == null || this.inside != inside) {
            this.inside = inside;
            return inside ? GpsGeofencingEvents.ENTERED : GpsGeofencingEvents.LEFT;
        }
        return inside ? GpsGeofencingEvents.INSIDE : GpsGeofencingEvents.OUTSIDE;
    }

}
