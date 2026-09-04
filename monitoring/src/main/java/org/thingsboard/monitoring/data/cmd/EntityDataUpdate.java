// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.monitoring.data.cmd;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityKeyType;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class EntityDataUpdate {

    @JsonIgnoreProperties(ignoreUnknown = true)
    private List<EntityData> update;

    public Map<String, String> getLatest(UUID entityId) {
        if (update == null || update.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, String> result = new HashMap<>();
        update.stream()
                .filter(entityData -> entityData.getEntityId().getId().equals(entityId)).findFirst()
                .map(EntityData::getLatest).map(latest -> latest.get(EntityKeyType.TIME_SERIES))
                .ifPresent(latest -> latest.forEach((key, tsValue) -> result.put(key, tsValue.getValue())));
        return result;
    }

}
