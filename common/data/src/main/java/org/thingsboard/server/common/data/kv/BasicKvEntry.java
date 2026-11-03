// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.kv;

import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

import java.util.Objects;
import java.util.Optional;

public abstract class BasicKvEntry implements KvEntry {

    @Length(fieldName = "attribute key")
    @NoXss
    private final String key;

    protected BasicKvEntry(String key) {
        this.key = key;
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public Optional<String> getStrValue() {
        return Optional.ofNullable(null);
    }

    @Override
    public Optional<Long> getLongValue() {
        return Optional.ofNullable(null);
    }

    @Override
    public Optional<Boolean> getBooleanValue() {
        return Optional.ofNullable(null);
    }

    @Override
    public Optional<Double> getDoubleValue() {
        return Optional.ofNullable(null);
    }

    @Override
    public Optional<String> getJsonValue() {
        return Optional.ofNullable(null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BasicKvEntry)) return false;
        BasicKvEntry that = (BasicKvEntry) o;
        return Objects.equals(key, that.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key);
    }

    @Override
    public String toString() {
        return "BasicKvEntry{" +
                "key='" + key + '\'' +
                '}';
    }
}
