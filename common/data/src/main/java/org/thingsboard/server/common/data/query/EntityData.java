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
package org.thingsboard.server.common.data.query;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.id.EntityId;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EntityData {

    private EntityId entityId;
    private Map<EntityKeyType, Map<String, TsValue>> latest;
    private Map<String, TsValue[]> timeseries;
    private Map<Integer, ComparisonTsValue> aggLatest;

    public EntityData(EntityId entityId, Map<EntityKeyType, Map<String, TsValue>> latest, Map<String, TsValue[]> timeseries) {
        this(entityId, latest, timeseries, null);
    }

    @JsonIgnore
    public void clearTsAndAggData() {
        if (timeseries != null) {
            timeseries.clear();
        }
        if (aggLatest != null) {
            aggLatest.clear();
        }
    }

}
