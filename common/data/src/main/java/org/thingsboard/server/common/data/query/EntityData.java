// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
