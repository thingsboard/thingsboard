// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.util;

import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.query.AlarmData;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.query.TsValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class DataSourceUtils {

    public static List<EntityKey> setEntityKeyIfNotExists(List<EntityKey> entityKeys, EntityKeyType type, String keyName) {
        EntityKey targetEntityKey =
                entityKeys.stream().filter(key -> key.getKey().equals(keyName) && key.getType().equals(type)).findAny().orElse(null);
        if (targetEntityKey == null) {
            entityKeys = new ArrayList<>(entityKeys);
            entityKeys.add(new EntityKey(type, keyName));
        }
        return entityKeys;
    }

    public static Optional<String> getEntityLatestValue(EntityData entity, EntityKeyType keyType, String key) {
        return getLatestValue(entity.getLatest(), keyType, key);
    }

    public static Optional<String> getAlarmLatestValue(AlarmData alarm, EntityKeyType keyType, String key) {
        return getLatestValue(alarm.getLatest(), keyType, key);
    }

    private static Optional<String> getLatestValue(Map<EntityKeyType, Map<String, TsValue>> latest, EntityKeyType keyType, String key) {
        if (latest != null && latest.containsKey(keyType)) {
            Map<String, TsValue> values = latest.get(keyType);
            TsValue value = values.get(key);
            if (value != null) {
                return Optional.ofNullable(value.getValue());
            }
        }
        return Optional.empty();
    }

    public static EntityData entityDataFromEntityId(EntityId entityId) {
        return new EntityData(entityId, false, false, Collections.emptyMap(), Collections.emptyMap());
    }
}
