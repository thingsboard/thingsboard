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
package org.thingsboard.server.dao.timeseries;

import lombok.Builder;
import lombok.Getter;
import org.thingsboard.server.common.data.kv.ReadTsKvQuery;
import org.thingsboard.server.common.data.kv.TsKvEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import static org.thingsboard.server.dao.timeseries.CassandraBaseTimeseriesDao.DESC_ORDER;

/**
 * Created by ashvayka on 21.02.17.
 */
public class TsKvQueryCursor extends QueryCursor {

    @Getter
    private final List<TsKvEntry> data;
    private final Consumer<List<TsKvEntry>> dataProcessor; // if non-null, "data" will be empty
    @Getter
    private final String orderBy;

    private int partitionIndex;
    private int currentLimit;

    @Builder
    public TsKvQueryCursor(String entityType, UUID entityId, ReadTsKvQuery baseQuery, List<Long> partitions, Consumer<List<TsKvEntry>> dataProcessor) {
        super(entityType, entityId, baseQuery, partitions);
        this.orderBy = baseQuery.getOrder();
        this.partitionIndex = isDesc() ? partitions.size() - 1 : 0;
        this.data = new ArrayList<>();
        this.currentLimit = baseQuery.getLimit();
        this.dataProcessor = dataProcessor;
    }

    @Override
    public boolean hasNextPartition() {
        return isDesc() ? partitionIndex >= 0 : partitionIndex <= partitions.size() - 1;
    }

    public boolean isFull() {
        return currentLimit <= 0;
    }

    @Override
    public long getNextPartition() {
        long partition = partitions.get(partitionIndex);
        if (isDesc()) {
            partitionIndex--;
        } else {
            partitionIndex++;
        }
        return partition;
    }

    public int getCurrentLimit() {
        return currentLimit;
    }

    public void addData(List<TsKvEntry> newData) {
        currentLimit -= newData.size();
        if (dataProcessor == null) {
            data.addAll(newData);
        } else {
            dataProcessor.accept(newData);
        }
    }

    private boolean isDesc() {
        return orderBy.equals(DESC_ORDER);
    }

}
