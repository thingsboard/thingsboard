// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.kv;

import jakarta.validation.Valid;
import lombok.Data;

import java.util.Optional;

/**
 * @author Andrew Shvayka
 */
@Data
public class BaseAttributeKvEntry implements AttributeKvEntry {

    private static final long serialVersionUID = -6460767583563159407L;

    private final long lastUpdateTs;
    @Valid
    private final KvEntry kv;

    private final Long version;

    public BaseAttributeKvEntry(KvEntry kv, long lastUpdateTs) {
        this.kv = kv;
        this.lastUpdateTs = lastUpdateTs;
        this.version = null;
    }

    public BaseAttributeKvEntry(KvEntry kv, long lastUpdateTs, Long version) {
        this.kv = kv;
        this.lastUpdateTs = lastUpdateTs;
        this.version = version;
    }

    public BaseAttributeKvEntry(long lastUpdateTs, KvEntry kv) {
        this(kv, lastUpdateTs);
    }

    @Override
    public String getKey() {
        return kv.getKey();
    }

    @Override
    public DataType getDataType() {
        return kv.getDataType();
    }

    @Override
    public Optional<String> getStrValue() {
        return kv.getStrValue();
    }

    @Override
    public Optional<Long> getLongValue() {
        return kv.getLongValue();
    }

    @Override
    public Optional<Boolean> getBooleanValue() {
        return kv.getBooleanValue();
    }

    @Override
    public Optional<Double> getDoubleValue() {
        return kv.getDoubleValue();
    }

    @Override
    public Optional<String> getJsonValue() {
        return kv.getJsonValue();
    }

    @Override
    public String getValueAsString() {
        return kv.getValueAsString();
    }

    @Override
    public Object getValue() {
        return kv.getValue();
    }

}
