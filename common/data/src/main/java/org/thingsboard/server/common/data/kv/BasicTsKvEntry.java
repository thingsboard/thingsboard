/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
package org.thingsboard.server.common.data.kv;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.validation.Valid;

import java.util.Objects;
import java.util.Optional;

public class BasicTsKvEntry implements TsKvEntry {
    private static final int MAX_CHARS_PER_DATA_POINT = 512;
    protected final long ts;
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
    @JsonIgnoreProperties(ignoreUnknown = true)
    @Valid
    private final KvEntry kv;

    @JsonCreator
    public BasicTsKvEntry(@JsonProperty("ts") long ts, @JsonProperty("kv") KvEntry kv) {
        this.ts = ts;
        this.kv = kv;
    }

    @JsonIgnore
    @Override
    public String getKey() {
        return kv.getKey();
    }

    @JsonIgnore
    @Override
    public DataType getDataType() {
        return kv.getDataType();
    }

    @JsonIgnore
    @Override
    public Optional<String> getStrValue() {
        return kv.getStrValue();
    }

    @JsonIgnore
    @Override
    public Optional<Long> getLongValue() {
        return kv.getLongValue();
    }

    @JsonIgnore
    @Override
    public Optional<Boolean> getBooleanValue() {
        return kv.getBooleanValue();
    }

    @JsonIgnore
    @Override
    public Optional<Double> getDoubleValue() {
        return kv.getDoubleValue();
    }

    @JsonIgnore
    @Override
    public Optional<String> getJsonValue() {
        return kv.getJsonValue();
    }

    @JsonIgnore
    @Override
    public Object getValue() {
        return kv.getValue();
    }

    @Override
    public long getTs() {
        return ts;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BasicTsKvEntry)) return false;
        BasicTsKvEntry that = (BasicTsKvEntry) o;
        return getTs() == that.getTs() &&
                Objects.equals(kv, that.kv);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getTs(), kv);
    }

    @Override
    public String toString() {
        return "BasicTsKvEntry{" +
                "ts=" + ts +
                ", kv=" + kv +
                '}';
    }

    @JsonIgnore
    @Override
    public String getValueAsString() {
        return kv.getValueAsString();
    }

    @Override
    public int getDataPoints() {
        int length;
        switch (getDataType()) {
            case STRING:
                length = getStrValue().get().length();
                break;
            case JSON:
                length = getJsonValue().get().length();
                break;
            default:
                return 1;
        }
        return Math.max(1, (length + MAX_CHARS_PER_DATA_POINT - 1) / MAX_CHARS_PER_DATA_POINT);
    }

}
