// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.cf.ctx.state;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.script.api.tbel.TbelCfArg;
import org.thingsboard.script.api.tbel.TbelCfSingleValueArg;
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

    private long ts;
    private BasicKvEntry kvEntryValue;
    private Long version;

    private boolean forceResetPrevious;

    public SingleValueArgumentEntry() {
        this.ts = DEFAULT_LAST_UPDATE_TS;
        this.version = DEFAULT_VERSION;
    }

    public SingleValueArgumentEntry(TsKvProto entry) {
        this.ts = entry.getTs();
        if (entry.hasVersion()) {
            this.version = entry.getVersion();
        }
        this.kvEntryValue = ProtoUtils.fromProto(entry.getKv());
    }

    public SingleValueArgumentEntry(AttributeValueProto entry) {
        this.ts = entry.getLastUpdateTs();
        if (entry.hasVersion()) {
            this.version = entry.getVersion();
        }
        this.kvEntryValue = ProtoUtils.basicKvEntryFromProto(entry);
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
    public boolean updateEntry(ArgumentEntry entry) {
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
