/**
 * Copyright © 2016-2017 The Thingsboard Authors
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

import java.util.Optional;

/**
 * @author Andrew Shvayka
 */
public class BaseAttributeKvEntry implements AttributeKvEntry {

    private final long lastUpdateTs;
    private final KvEntry kv;

    public BaseAttributeKvEntry(KvEntry kv, long lastUpdateTs) {
        this.kv = kv;
        this.lastUpdateTs = lastUpdateTs;
    }

    @Override
    public long getLastUpdateTs() {
        return lastUpdateTs;
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
    public String getValueAsString() {
        return kv.getValueAsString();
    }

    @Override
    public Object getValue() {
        return kv.getValue();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BaseAttributeKvEntry that = (BaseAttributeKvEntry) o;

        if (lastUpdateTs != that.lastUpdateTs) return false;
        return kv.equals(that.kv);

    }

    @Override
    public int hashCode() {
        int result = (int) (lastUpdateTs ^ (lastUpdateTs >>> 32));
        result = 31 * result + kv.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "BaseAttributeKvEntry{" +
                "lastUpdateTs=" + lastUpdateTs +
                ", kv=" + kv +
                '}';
    }
}
