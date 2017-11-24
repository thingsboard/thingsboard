/**
 * Copyright © 2016-2017 The Thingsboard Authors
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
package org.thingsboard.server.dao.depthDatum;

import lombok.Getter;
import org.thingsboard.server.common.data.kv.DsKvEntry;
import org.thingsboard.server.common.data.kv.DsKvQuery;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.kv.TsKvQuery;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by ashvayka on 21.02.17.
 */
public class DsKvQueryCursor {
    @Getter
    private final String entityType;
    @Getter
    private final UUID entityId;
    @Getter
    private final String key;
    @Getter
    private final Double startDs;
    @Getter
    private final Double endDs;
    private final List<Double> partitions;
    @Getter
    private final List<DsKvEntry> data;

    private int partitionIndex;
    private int currentLimit;

    public DsKvQueryCursor(String entityType, UUID entityId, DsKvQuery baseQuery, List<Double> partitions) {
        this.entityType = entityType;
        this.entityId = entityId;
        this.key = baseQuery.getKey();
        this.startDs = baseQuery.getStartDs();
        this.endDs = baseQuery.getEndDs();
        this.partitions = partitions;
        this.partitionIndex = partitions.size() - 1;
        this.data = new ArrayList<>();
        this.currentLimit = baseQuery.getLimit();
    }

    public boolean hasNextPartition() {
        return partitionIndex >= 0;
    }

    public boolean isFull() {
        return currentLimit <= 0;
    }

    public Double getNextPartition() {
        Double partition = partitions.get(partitionIndex);
        partitionIndex--;
        return partition;
    }

    public int getCurrentLimit() {
        return currentLimit;
    }

    public void addData(List<DsKvEntry> newData) {
        currentLimit -= newData.size();
        data.addAll(newData);
    }
}
