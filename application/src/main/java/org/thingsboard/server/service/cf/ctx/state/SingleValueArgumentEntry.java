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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.lang.Nullable;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.script.api.tbel.TbelCfArg;
import org.thingsboard.script.api.tbel.TbelCfSingleValueArg;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BasicKvEntry;
import org.thingsboard.server.common.data.kv.JsonDataEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.util.ProtoUtils;
import org.thingsboard.server.gen.transport.TransportProtos.AttributeValueProto;
import org.thingsboard.server.gen.transport.TransportProtos.TsKvProto;

import static org.thingsboard.server.service.cf.ctx.state.BaseCalculatedFieldState.DEFAULT_LAST_UPDATE_TS;

@Data
@AllArgsConstructor
public class SingleValueArgumentEntry implements ArgumentEntry {

    public static final Long DEFAULT_VERSION = -1L;

    @Nullable
    protected EntityId entityId;

    protected long ts;
    protected BasicKvEntry kvEntryValue;
    protected Long version;

    protected boolean forceResetPrevious;

    public SingleValueArgumentEntry() {
        this.ts = DEFAULT_LAST_UPDATE_TS;
        this.version = DEFAULT_VERSION;
    }

    public SingleValueArgumentEntry(EntityId entityId, ArgumentEntry entry) {
        this(entry);
        this.entityId = entityId;
    }

    public SingleValueArgumentEntry(ArgumentEntry entry) {
        if (entry instanceof SingleValueArgumentEntry singleValueArgumentEntry) {
            this.ts = singleValueArgumentEntry.ts;
            this.kvEntryValue = singleValueArgumentEntry.kvEntryValue;
            this.version = singleValueArgumentEntry.version;
            this.forceResetPrevious = singleValueArgumentEntry.forceResetPrevious;
        }
    }

    public SingleValueArgumentEntry(EntityId entityId, TsKvProto entry) {
        this(entry);
        this.entityId = entityId;
    }

    public SingleValueArgumentEntry(TsKvProto entry) {
        this.ts = entry.getTs();
        if (entry.hasVersion()) {
            this.version = entry.getVersion();
        }
        this.kvEntryValue = ProtoUtils.fromProto(entry.getKv());
    }

    public SingleValueArgumentEntry(EntityId entityId, AttributeValueProto entry) {
        this(entry);
        this.entityId = entityId;
    }

    public SingleValueArgumentEntry(AttributeValueProto entry) {
        this.ts = entry.getLastUpdateTs();
        if (entry.hasVersion()) {
            this.version = entry.getVersion();
        }
        this.kvEntryValue = ProtoUtils.basicKvEntryFromProto(entry);
    }

    public SingleValueArgumentEntry(EntityId entityId, KvEntry entry) {
        this(entry);
        this.entityId = entityId;
    }

    public SingleValueArgumentEntry(KvEntry entry) {
        if (entry instanceof TsKvEntry tsKvEntry) {
            this.ts = tsKvEntry.getTs();
            this.version = tsKvEntry.getVersion();
        } else if (entry instanceof AttributeKvEntry attributeKvEntry) {
            this.ts = attributeKvEntry.getLastUpdateTs();
            this.version = attributeKvEntry.getVersion();
        }
        this.kvEntryValue = ProtoUtils.basicKvEntryFromKvEntry(entry);
    }

    public SingleValueArgumentEntry(EntityId entityId, long ts, BasicKvEntry kvEntryValue, Long version) {
        this(ts, kvEntryValue, version);
        this.entityId = entityId;
    }

    public SingleValueArgumentEntry(long ts, BasicKvEntry kvEntryValue, Long version) {
        this.ts = ts;
        this.kvEntryValue = kvEntryValue;
        this.version = version;
    }

    @Override
    public ArgumentEntryType getType() {
        return ArgumentEntryType.SINGLE_VALUE;
    }

    @Override
    public boolean isEmpty() {
        return kvEntryValue == null;
    }

    @JsonIgnore
    public Object getValue() {
        return isEmpty() ? null : kvEntryValue.getValue();
    }

    @Override
    public TbelCfArg toTbelCfArg() {
        if (isEmpty()) {
            return new TbelCfSingleValueArg(ts, null);
        }
        Object value = kvEntryValue.getValue();
        if (kvEntryValue instanceof JsonDataEntry) {
            try {
                value = JacksonUtil.readValue(kvEntryValue.getValueAsString(), new TypeReference<>() {
                });
            } catch (Exception e) {
            }
        }
        if (value instanceof Long longValue) {
            if (longValue >= Integer.MIN_VALUE && longValue <= Integer.MAX_VALUE) {
                value = longValue.intValue();
            }
        }
        return new TbelCfSingleValueArg(ts, value);
    }

    @Override
    public boolean updateEntry(ArgumentEntry entry, CalculatedFieldCtx ctx) {
        if (entry instanceof SingleValueArgumentEntry singleValueEntry) {
            if (singleValueEntry.getTs() < this.ts) {
                return false;
            }

            Long newVersion = singleValueEntry.getVersion();
            if (newVersion == null || this.version == null || newVersion > this.version) {
                this.ts = singleValueEntry.getTs();
                this.version = newVersion;
                this.kvEntryValue = singleValueEntry.getKvEntryValue();
                return true;
            }
        } else {
            throw new IllegalArgumentException("Unsupported argument entry type for single value argument entry: " + entry.getType());
        }
        return false;
    }

    public boolean isDefaultValue() {
        return DEFAULT_VERSION.equals(this.version);
    }

}
