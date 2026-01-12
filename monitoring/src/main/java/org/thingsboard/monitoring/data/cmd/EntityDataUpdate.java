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
