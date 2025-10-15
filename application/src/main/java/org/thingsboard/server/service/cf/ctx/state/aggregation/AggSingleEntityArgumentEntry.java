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
package org.thingsboard.server.service.cf.ctx.state.aggregation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.BasicKvEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.gen.transport.TransportProtos.AttributeValueProto;
import org.thingsboard.server.gen.transport.TransportProtos.TsKvProto;
import org.thingsboard.server.service.cf.ctx.state.ArgumentEntry;
import org.thingsboard.server.service.cf.ctx.state.ArgumentEntryType;
import org.thingsboard.server.service.cf.ctx.state.SingleValueArgumentEntry;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AggSingleEntityArgumentEntry extends SingleValueArgumentEntry {

    private EntityId entityId;
    private boolean deleted;

    public AggSingleEntityArgumentEntry(EntityId entityId, ArgumentEntry entry) {
        super(entry);
        this.entityId = entityId;
    }

    public AggSingleEntityArgumentEntry(EntityId entityId, TsKvProto entry) {
        super(entry);
        this.entityId = entityId;
    }

    public AggSingleEntityArgumentEntry(EntityId entityId, AttributeValueProto entry) {
        super(entry);
        this.entityId = entityId;
    }

    public AggSingleEntityArgumentEntry(EntityId entityId, KvEntry entry) {
        super(entry);
        this.entityId = entityId;
    }

    public AggSingleEntityArgumentEntry(EntityId entityId, long ts, BasicKvEntry kvEntryValue, Long version) {
        super(ts, kvEntryValue, version);
        this.entityId = entityId;
    }

    @Override
    public boolean updateEntry(ArgumentEntry entry) {
        if (entry instanceof AggSingleEntityArgumentEntry singleValueEntry) {
            if (singleValueEntry.getTs() <= ts) {
                return false;
            }

            Long newVersion = singleValueEntry.getVersion();
            if (newVersion == null || this.version == null || newVersion > this.version) {
                this.ts = singleValueEntry.getTs();
                this.version = newVersion;
                this.kvEntryValue = singleValueEntry.getKvEntryValue();
                this.entityId = singleValueEntry.getEntityId();
                return true;
            }
        } else {
            throw new IllegalArgumentException("Unsupported argument entry type for single value argument entry: " + entry.getType());
        }
        return false;
    }

    @Override
    public ArgumentEntryType getType() {
        return ArgumentEntryType.AGGREGATE_LATEST_SINGLE;
    }
}
