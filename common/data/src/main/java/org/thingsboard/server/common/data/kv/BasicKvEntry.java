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

import java.util.Objects;
import java.util.Optional;

public abstract class BasicKvEntry implements KvEntry {

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
