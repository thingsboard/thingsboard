/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
import org.thingsboard.server.common.data.device.profile.AlarmConditionFilterKey;
import org.thingsboard.server.common.data.device.profile.AlarmConditionKeyType;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

class DataSnapshot {

    private volatile boolean ready;
    @Getter
    @Setter
    private long ts;
    private final Set<AlarmConditionFilterKey> keys;
    private final Map<AlarmConditionFilterKey, EntityKeyValue> values = new ConcurrentHashMap<>();

    DataSnapshot(Set<AlarmConditionFilterKey> entityKeysToFetch) {
        this.keys = entityKeysToFetch;
    }

    static AlarmConditionFilterKey toConditionKey(EntityKey key) {
        return new AlarmConditionFilterKey(toConditionKeyType(key.getType()), key.getKey());
    }

    static AlarmConditionKeyType toConditionKeyType(EntityKeyType keyType) {
        switch (keyType) {
            case ATTRIBUTE:
            case SERVER_ATTRIBUTE:
            case SHARED_ATTRIBUTE:
            case CLIENT_ATTRIBUTE:
                return AlarmConditionKeyType.ATTRIBUTE;
            case TIME_SERIES:
                return AlarmConditionKeyType.TIME_SERIES;
            case ENTITY_FIELD:
                return AlarmConditionKeyType.ENTITY_FIELD;
            default:
                throw new RuntimeException("Not supported entity key: " + keyType.name());
        }
    }

    void removeValue(EntityKey key) {
        values.remove(toConditionKey(key));
    }

    boolean putValue(AlarmConditionFilterKey key, long newTs, EntityKeyValue value) {
        return putIfKeyExists(key, value, ts != newTs);
    }

    private boolean putIfKeyExists(AlarmConditionFilterKey key, EntityKeyValue value, boolean updateOfTs) {
        if (keys.contains(key)) {
            EntityKeyValue oldValue = values.put(key, value);
            if (updateOfTs) {
                return true;
            } else {
                return oldValue == null || !oldValue.equals(value);
            }
        } else {
            return false;
        }
    }

    EntityKeyValue getValue(AlarmConditionFilterKey key) {
        return values.get(key);
    }
}
