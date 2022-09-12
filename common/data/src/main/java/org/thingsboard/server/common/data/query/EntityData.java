/**
 * Copyright © 2016-2022 The Thingsboard Authors
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

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.Aggregation;

import java.util.Map;

@Data
@RequiredArgsConstructor
public class EntityData {

    private final EntityId entityId;
    private final Map<EntityKeyType, Map<String, TsValue>> latest;
    private final Map<String, TsValue[]> timeseries;
    private final Map<Aggregation, Map<String, TsValue>> aggLatest;
    private final Map<String, TsValue[]> aggFloating;

    public EntityData(EntityId entityId, Map<EntityKeyType, Map<String, TsValue>> latest, Map<String, TsValue[]> timeseries) {
        this(entityId, latest, timeseries, null, null);
    }
}
