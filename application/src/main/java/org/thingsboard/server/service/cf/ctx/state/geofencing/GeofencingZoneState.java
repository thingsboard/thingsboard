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
import lombok.EqualsAndHashCode;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.geo.Coordinates;
import org.thingsboard.common.util.geo.PerimeterDefinition;
import org.thingsboard.server.common.data.cf.configuration.geofencing.GeofencingPresenceStatus;
import org.thingsboard.server.common.data.cf.configuration.geofencing.GeofencingTransitionEvent;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.util.ProtoUtils;
import org.thingsboard.server.gen.transport.TransportProtos.GeofencingZoneProto;

import static org.thingsboard.server.common.data.cf.configuration.geofencing.GeofencingPresenceStatus.INSIDE;
import static org.thingsboard.server.common.data.cf.configuration.geofencing.GeofencingPresenceStatus.OUTSIDE;

@Data
public class GeofencingZoneState {

    private final EntityId zoneId;

    private long ts;
    private Long version;
    private PerimeterDefinition perimeterDefinition;

    @EqualsAndHashCode.Exclude
    private GeofencingPresenceStatus lastPresence;

    public GeofencingZoneState(EntityId zoneId, KvEntry entry) {
        this.zoneId = zoneId;
        if (!(entry instanceof AttributeKvEntry attributeKvEntry)) {
            throw new IllegalArgumentException("Unsupported KvEntry type for geofencing zone state: " + entry.getClass().getSimpleName());
        }
        this.ts = attributeKvEntry.getLastUpdateTs();
        this.version = attributeKvEntry.getVersion();
        this.perimeterDefinition = JacksonUtil.fromString(entry.getValueAsString(), PerimeterDefinition.class);
    }

    public GeofencingZoneState(GeofencingZoneProto proto) {
        this.zoneId = ProtoUtils.fromProto(proto.getZoneId());
        this.ts = proto.getTs();
        this.version = proto.getVersion();
        this.perimeterDefinition = JacksonUtil.fromString(proto.getPerimeterDefinition(), PerimeterDefinition.class);
        if (proto.hasInside()) {
            this.lastPresence = proto.getInside() ? INSIDE : OUTSIDE;
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
            this.lastPresence = null;
            return true;
        }
        return false;
    }

    public GeofencingEvalResult evaluate(Coordinates entityCoordinates) {
        boolean nowInside = perimeterDefinition.checkMatches(entityCoordinates);

        GeofencingPresenceStatus status = nowInside ? INSIDE : OUTSIDE;

        // first evaluation
        if (this.lastPresence == null) {
            this.lastPresence = status;
            return new GeofencingEvalResult(null, status, true);
        }
        // State changed
        if (this.lastPresence != status) {
            this.lastPresence = status;
            GeofencingTransitionEvent transition = (status == GeofencingPresenceStatus.INSIDE) ?
                    GeofencingTransitionEvent.ENTERED : GeofencingTransitionEvent.LEFT;
            return new GeofencingEvalResult(transition, status, false);
        }
        // State unchanged
        return new GeofencingEvalResult(null, status, false);
    }

}
