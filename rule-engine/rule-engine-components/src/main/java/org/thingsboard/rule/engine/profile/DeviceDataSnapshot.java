/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
package org.thingsboard.rule.engine.profile;

import lombok.Getter;
import lombok.Setter;
import org.thingsboard.server.common.data.query.EntityKey;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class DeviceDataSnapshot {

    private volatile boolean ready;
    @Getter @Setter
    private long ts;
    private final Map<EntityKey, EntityKeyValue> values = new ConcurrentHashMap<>();

    public DeviceDataSnapshot(Set<EntityKey> entityKeySet) {
        entityKeySet.forEach(key -> values.put(key, new EntityKeyValue()));
        this.ready = false;
    }

    void putValue(EntityKey key, EntityKeyValue value) {
        values.put(key, value);
    }

    EntityKeyValue getValue(EntityKey key) {
        return values.get(key);
    }

    boolean isReady() {
        return ready;
    }
}
