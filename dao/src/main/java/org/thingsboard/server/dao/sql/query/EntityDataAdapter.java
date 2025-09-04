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
package org.thingsboard.server.dao.sql.query;

import org.apache.commons.lang3.math.NumberUtils;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityDataPageLink;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.query.TsValue;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class EntityDataAdapter {

    public static PageData<EntityData> createEntityData(EntityDataPageLink pageLink,
                                                        List<EntityKeyMapping> selectionMapping,
                                                        List<Map<String, Object>> rows,
                                                        int totalElements) {
        int totalPages = pageLink.getPageSize() > 0 ? (int) Math.ceil((float) totalElements / pageLink.getPageSize()) : 1;
        int startIndex = pageLink.getPageSize() * pageLink.getPage();
        boolean hasNext = pageLink.getPageSize() > 0 && totalElements > startIndex + rows.size();
        List<EntityData> entitiesData = convertListToEntityData(rows, selectionMapping);
        return new PageData<>(entitiesData, totalPages, totalElements, hasNext);
    }

    private static List<EntityData> convertListToEntityData(List<Map<String, Object>> result, List<EntityKeyMapping> selectionMapping) {
        return result.stream().map(row -> toEntityData(row, selectionMapping)).collect(Collectors.toList());
    }

    private static EntityData toEntityData(Map<String, Object> row, List<EntityKeyMapping> selectionMapping) {
        UUID id = (UUID)row.get("id");
        EntityType entityType = EntityType.valueOf((String) row.get("entity_type"));
        EntityId entityId = EntityIdFactory.getByTypeAndUuid(entityType, id);
        Map<EntityKeyType, Map<String, TsValue>> latest = new HashMap<>();
        //Maybe avoid empty hashmaps?
        EntityData entityData = new EntityData(entityId, latest, new HashMap<>(), new HashMap<>());
        for (EntityKeyMapping mapping : selectionMapping) {
            if (!mapping.isIgnore()) {
                EntityKey entityKey = mapping.getEntityKey();
                Object value = row.get(mapping.getValueAlias());
                String strValue;
                long ts;
                if (entityKey.getType().equals(EntityKeyType.ENTITY_FIELD)) {
                    strValue = value != null ? value.toString() : "";
                    ts = System.currentTimeMillis();
                } else {
                    strValue = convertValue(value);
                    Object tsObject = row.get(mapping.getTsAlias());
                    ts = tsObject != null ? Long.parseLong(tsObject.toString()) : 0;
                }
                TsValue tsValue = new TsValue(ts, strValue);
                latest.computeIfAbsent(entityKey.getType(), entityKeyType -> new HashMap<>()).put(entityKey.getKey(), tsValue);
            }
        }
        return entityData;
    }

    static String convertValue(Object value) {
        if (value != null) {
            String strVal = value.toString();
            // check number
            if (NumberUtils.isParsable(strVal)) {
                if (strVal.startsWith("0") && !strVal.startsWith("0.")) {
                    return strVal;
                }
                try {
                    BigInteger longVal = new BigInteger(strVal);
                    return longVal.toString();
                } catch (NumberFormatException ignored) {
                }
                try {
                    double dblVal = Double.parseDouble(strVal);
                    String doubleAsString = Double.toString(dblVal);
                    if (!Double.isInfinite(dblVal) && isSimpleDouble(doubleAsString)) {
                        return doubleAsString;
                    }
                } catch (NumberFormatException ignored) {
                }
            }
            return strVal;
        } else {
            return "";
        }
    }

    private static boolean isSimpleDouble(String valueAsString) {
        return valueAsString.contains(".") && !valueAsString.contains("E") && !valueAsString.contains("e");
    }


}
