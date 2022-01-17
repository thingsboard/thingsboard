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
package org.thingsboard.server.dao.timeseries;

import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.common.data.kv.BaseReadTsKvQuery;
import org.thingsboard.server.common.data.kv.ReadTsKvQuery;
import org.thingsboard.server.common.data.kv.TsKvEntry;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

public interface AggregationTimeseriesDao {

    default ListenableFuture<List<TsKvEntry>> findAllAsync(TenantId tenantId, EntityId entityId, ReadTsKvQuery query) {
        if (query.getAggregation() == Aggregation.NONE) {
            return findAllAsyncWithLimit(tenantId, entityId, query);
        } else {
            List<ListenableFuture<Optional<TsKvEntry>>> futures = findIntervals(tenantId, entityId, query);
            return getTskvEntriesFuture(Futures.allAsList(futures));
        }
    }

    default List<ListenableFuture<Optional<TsKvEntry>>> findIntervals(TenantId tenantId, EntityId entityId, ReadTsKvQuery query) {
        List<ListenableFuture<Optional<TsKvEntry>>> futures = new ArrayList<>();
        long endPeriod = query.getEndTs();
        long startPeriod = query.getStartTs();
        long step = query.getInterval();
        while (startPeriod <= endPeriod) {
            long startTs = startPeriod;
            long endTs = Math.min(startPeriod + step, endPeriod + 1);
            long ts = getTsForReadTsKvQuery(startTs, endTs);
            ReadTsKvQuery subQuery = new BaseReadTsKvQuery(query.getKey(), startTs, endTs, ts, 1, query.getAggregation(), query.getOrder());
            ListenableFuture<Optional<TsKvEntry>> aggregateTsKvEntry = findAndAggregateAsync(tenantId, entityId, subQuery, toPartitionTs(startTs), toPartitionTs(endTs), query.getAggregation());
            futures.add(aggregateTsKvEntry);
            startPeriod = endTs;
        }
        return futures;
    }

    default long getTsForReadTsKvQuery(long startTs, long endTs) {
        return endTs - startTs;
    }

    long getIntervalGreaterOrEqualsMinAggregationStep(long interval);

    default ListenableFuture<List<TsKvEntry>> getTskvEntriesFuture(ListenableFuture<List<Optional<TsKvEntry>>> allAsList) {
        return Futures.transform(allAsList, new Function<>() {
            @Nullable
            @Override
            public List<TsKvEntry> apply(@Nullable List<Optional<TsKvEntry>> results) {
                if (results == null || results.isEmpty()) {
                    return null;
                }
                return results.stream()
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .collect(Collectors.toList());
            }
        }, getExecutor());
    }

    Executor getExecutor();

    default ListenableFuture<Optional<TsKvEntry>> findAndAggregateAsync(TenantId tenantId, EntityId entityId, ReadTsKvQuery key, long startTs, long endTs, Aggregation aggregation) {
        return Futures.immediateFuture(null);
    }

    ListenableFuture<List<TsKvEntry>> findAllAsyncWithLimit(TenantId tenantId, EntityId entityId, ReadTsKvQuery query);

    default long toPartitionTs(long ts) {
        return ts;
    }

}
