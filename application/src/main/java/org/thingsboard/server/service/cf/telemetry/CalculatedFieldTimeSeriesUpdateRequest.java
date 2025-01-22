/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
package org.thingsboard.server.service.cf.telemetry;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.thingsboard.rule.engine.api.TimeseriesSaveRequest;
import org.thingsboard.server.common.data.cf.configuration.ArgumentType;
import org.thingsboard.server.common.data.cf.configuration.ReferencedEntityKey;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.service.cf.ctx.state.CalculatedFieldCtx;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
public class CalculatedFieldTimeSeriesUpdateRequest implements CalculatedFieldTelemetryUpdateRequest {

    private TenantId tenantId;
    private EntityId entityId;
    private List<? extends KvEntry> kvEntries;
    private List<CalculatedFieldId> previousCalculatedFieldIds;

    public CalculatedFieldTimeSeriesUpdateRequest(TimeseriesSaveRequest request) {
        this.tenantId = request.getTenantId();
        this.entityId = request.getEntityId();
        this.kvEntries = request.getEntries();
        this.previousCalculatedFieldIds = request.getPreviousCalculatedFieldIds();
    }

    @Override
    public Map<String, KvEntry> getMappedTelemetry(CalculatedFieldCtx ctx, EntityId referencedEntityId) {
        Map<String, KvEntry> mappedKvEntries = new HashMap<>();
        Map<TbPair<EntityId, ReferencedEntityKey>, String> referencedKeys = ctx.getReferencedEntityKeys();

        kvEntries.forEach(entry -> {
            String key = entry.getKey();

            ReferencedEntityKey tsLatestKey = new ReferencedEntityKey(key, ArgumentType.TS_LATEST, null);
            String argTsLatestName = referencedKeys.get(new TbPair<>(referencedEntityId, tsLatestKey));

            if (argTsLatestName != null) {
                mappedKvEntries.put(argTsLatestName, entry);
            } else {
                ReferencedEntityKey tsRollingKey = new ReferencedEntityKey(key, ArgumentType.TS_ROLLING, null);
                String argTsRollingName = referencedKeys.get(new TbPair<>(referencedEntityId, tsRollingKey));

                if (argTsRollingName != null) {
                    mappedKvEntries.put(argTsRollingName, entry);
                }
            }
        });

        return mappedKvEntries;
    }
}
