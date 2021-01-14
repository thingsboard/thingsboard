/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
package org.thingsboard.server.dao.sqlts;

import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.ReadTsKvQuery;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.dao.sql.ScheduledLogExecutorComponent;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
public abstract class AbstractSqlTimeseriesDao extends BaseAbstractSqlTimeseriesDao implements AggregationTimeseriesDao {

    protected static final long SECONDS_IN_DAY = TimeUnit.DAYS.toSeconds(1);

    @Autowired
    protected ScheduledLogExecutorComponent logExecutor;

    @Value("${sql.ts.batch_size:1000}")
    protected int tsBatchSize;

    @Value("${sql.ts.batch_max_delay:100}")
    protected long tsMaxDelay;

    @Value("${sql.ts.stats_print_interval_ms:1000}")
    protected long tsStatsPrintIntervalMs;

    @Value("${sql.ts.batch_threads:4}")
    protected int tsBatchThreads;

    @Value("${sql.timescale.batch_threads:4}")
    protected int timescaleBatchThreads;

    @Value("${sql.batch_sort:false}")
    protected boolean batchSortEnabled;

    @Value("${sql.ttl.ts.ts_key_value_ttl:0}")
    private long systemTtl;

    protected ListenableFuture<List<TsKvEntry>> processFindAllAsync(TenantId tenantId, EntityId entityId, List<ReadTsKvQuery> queries) {
        List<ListenableFuture<List<TsKvEntry>>> futures = queries
                .stream()
                .map(query -> findAllAsync(tenantId, entityId, query))
                .collect(Collectors.toList());
        return Futures.transform(Futures.allAsList(futures), new Function<List<List<TsKvEntry>>, List<TsKvEntry>>() {
            @Nullable
            @Override
            public List<TsKvEntry> apply(@Nullable List<List<TsKvEntry>> results) {
                if (results == null || results.isEmpty()) {
                    return null;
                }
                return results.stream()
                        .filter(Objects::nonNull)
                        .flatMap(List::stream)
                        .collect(Collectors.toList());
            }
        }, service);
    }

    protected long computeTtl(long ttl) {
        if (systemTtl > 0) {
            if (ttl == 0) {
                ttl = systemTtl;
            } else {
                ttl = Math.min(systemTtl, ttl);
            }
        }
        return ttl;
    }

    protected int getDataPointDays(TsKvEntry tsKvEntry, long ttl) {
        return tsKvEntry.getDataPoints() * Math.max(1, (int) (ttl / SECONDS_IN_DAY));
    }
}
