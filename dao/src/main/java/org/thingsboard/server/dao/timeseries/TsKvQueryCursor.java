// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.timeseries;

import lombok.Getter;
import org.thingsboard.server.common.data.kv.ReadTsKvQuery;
import org.thingsboard.server.common.data.kv.TsKvEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.thingsboard.server.dao.timeseries.CassandraBaseTimeseriesDao.DESC_ORDER;

/**
 * Created by ashvayka on 21.02.17.
 */
public class TsKvQueryCursor extends QueryCursor {

    @Getter
    private final List<TsKvEntry> data;
    @Getter
    private final String orderBy;

    private int partitionIndex;
    private int currentLimit;

    public TsKvQueryCursor(String entityType, UUID entityId, ReadTsKvQuery baseQuery, List<Long> partitions) {
        super(entityType, entityId, baseQuery, partitions);
        this.orderBy = baseQuery.getOrder();
        this.partitionIndex = isDesc() ? partitions.size() - 1 : 0;
        this.data = new ArrayList<>();
        this.currentLimit = baseQuery.getLimit();
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
        data.addAll(newData);
    }

    private boolean isDesc() {
        return orderBy.equals(DESC_ORDER);
    }
}
