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
package org.thingsboard.server.dao.tdengine;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import jakarta.annotation.Priority;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.dao.entityview.EntityViewService;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.thingsboard.server.dao.timeseries.BaseTimeseriesService;
import org.thingsboard.server.dao.timeseries.TimeseriesDao;
import org.thingsboard.server.dao.timeseries.TimeseriesLatestDao;
import org.thingsboard.server.dao.util.TDengineTsOrTsLatestDao;

import java.util.List;

/**
 * for tdengine
 */
@Priority(-10)
@Service("tsService")
@Slf4j
@TDengineTsOrTsLatestDao
public class TDengineBaseTimeseriesService extends BaseTimeseriesService {

    private static final int INSERTS_PER_ENTRY = 3;
    private static final int INSERTS_PER_ENTRY_WITHOUT_LATEST = 2;
    private static final int DELETES_PER_ENTRY = INSERTS_PER_ENTRY;

    public static final Function<List<Integer>, Integer> SUM_ALL_INTEGERS = new Function<List<Integer>, Integer>() {
        @Override
        public @Nullable Integer apply(@Nullable List<Integer> input) {
            int result = 0;
            if (input != null) {
                for (Integer tmp : input) {
                    if (tmp != null) {
                        result += tmp;
                    }
                }
            }
            return result;
        }
    };

    @Value("${database.ts_max_intervals}")
    private long maxTsIntervals;

    @Autowired
    private TimeseriesDao timeseriesDao;

    @Autowired
    private TimeseriesLatestDao timeseriesLatestDao;

    @Autowired
    private EntityViewService entityViewService;

    private void saveAndRegisterFutures(TenantId tenantId, List<ListenableFuture<Integer>> futures, EntityId entityId, List<TsKvEntry> tsKvEntries, long ttl) {
        if (entityId.getEntityType().equals(EntityType.ENTITY_VIEW)) {
            throw new IncorrectParameterException("Telemetry data can't be stored for entity view. Read only");
        }
        futures.add(timeseriesDao.savePartition(tenantId, entityId, tsKvEntries.get(0).getTs(), tsKvEntries.get(0).getKey()));
        futures.add(Futures.transform(((TDengineTimeseriesLatestDao) timeseriesLatestDao).saveLatest(tenantId, entityId, tsKvEntries), v -> 0, MoreExecutors.directExecutor()));
    }

    private void saveAndRegisterFutures(TenantId tenantId, List<ListenableFuture<Integer>> futures, EntityId entityId, TsKvEntry tsKvEntry, long ttl) {
        doSaveAndRegisterFuturesFor(tenantId, futures, entityId, tsKvEntry, ttl);
        futures.add(Futures.transform(timeseriesLatestDao.saveLatest(tenantId, entityId, tsKvEntry), v -> 0, MoreExecutors.directExecutor()));
    }

    private void saveWithoutLatestAndRegisterFutures(TenantId tenantId, List<ListenableFuture<Integer>> futures, EntityId entityId, List<TsKvEntry> tsKvEntries, long ttl) {
        doSaveAndRegisterFuturesFor(tenantId, futures, entityId, tsKvEntries, ttl);
    }

    private void saveWithoutLatestAndRegisterFutures(TenantId tenantId, List<ListenableFuture<Integer>> futures, EntityId entityId, TsKvEntry tsKvEntry, long ttl) {
        doSaveAndRegisterFuturesFor(tenantId, futures, entityId, tsKvEntry, ttl);
    }

    private void doSaveAndRegisterFuturesFor(TenantId tenantId, List<ListenableFuture<Integer>> futures, EntityId entityId, List<TsKvEntry> tsKvEntries, long ttl) {
        if (entityId.getEntityType().equals(EntityType.ENTITY_VIEW)) {
            throw new IncorrectParameterException("Telemetry data can't be stored for entity view. Read only");
        }
        futures.add(timeseriesDao.savePartition(tenantId, entityId, tsKvEntries.get(0).getTs(), tsKvEntries.get(0).getKey()));
        futures.add(((TDengineTimeseriesDao) timeseriesDao).save(tenantId, entityId, tsKvEntries, ttl));
    }

    private void doSaveAndRegisterFuturesFor(TenantId tenantId, List<ListenableFuture<Integer>> futures, EntityId entityId, TsKvEntry tsKvEntry, long ttl) {
        if (entityId.getEntityType().equals(EntityType.ENTITY_VIEW)) {
            throw new IncorrectParameterException("Telemetry data can't be stored for entity view. Read only");
        }
        if (timeseriesDao instanceof TDengineTimeseriesDao) {
            futures.add(timeseriesDao.savePartition(tenantId, entityId, tsKvEntry.getTs(), tsKvEntry.getKey()));
            futures.add(Futures.immediateFuture(null));
            return;
        }
        futures.add(timeseriesDao.savePartition(tenantId, entityId, tsKvEntry.getTs(), tsKvEntry.getKey()));
        futures.add(timeseriesDao.save(tenantId, entityId, tsKvEntry, ttl));
    }

    @Override
    public ListenableFuture<Integer> save(TenantId tenantId, EntityId entityId, List<TsKvEntry> tsKvEntries, long ttl) {
        return doSave(tenantId, entityId, tsKvEntries, ttl, true);
    }

    @Override
    public ListenableFuture<Integer> saveWithoutLatest(TenantId tenantId, EntityId entityId, List<TsKvEntry> tsKvEntries, long ttl) {
        return doSave(tenantId, entityId, tsKvEntries, ttl, false);
    }

    private ListenableFuture<Integer> doSave(TenantId tenantId, EntityId entityId, List<TsKvEntry> tsKvEntries, long ttl, boolean saveLatest) {
        if (null == tsKvEntries || tsKvEntries.size() == 0) {
            return Futures.immediateFuture(null);
        }
        int inserts = saveLatest ? INSERTS_PER_ENTRY : INSERTS_PER_ENTRY_WITHOUT_LATEST;
        List<ListenableFuture<Integer>> futures;
        if (saveLatest) {
            if (timeseriesLatestDao instanceof TDengineTimeseriesLatestDao) {
                futures = Lists.newArrayListWithExpectedSize(1 * inserts);
                saveAndRegisterFutures(tenantId, futures, entityId, tsKvEntries, ttl);
            } else {
                futures = Lists.newArrayListWithExpectedSize(tsKvEntries.size() * inserts);
                for (TsKvEntry tsKvEntry : tsKvEntries) {
                    saveAndRegisterFutures(tenantId, futures, entityId, tsKvEntry, ttl);
                }
                if (timeseriesDao instanceof TDengineTimeseriesDao) {
                    saveWithoutLatestAndRegisterFutures(tenantId, futures, entityId, tsKvEntries, ttl);
                }
            }
        } else {
            if (timeseriesDao instanceof TDengineTimeseriesDao) {
                futures = Lists.newArrayListWithExpectedSize(1 * inserts);
                saveWithoutLatestAndRegisterFutures(tenantId, futures, entityId, tsKvEntries, ttl);
            } else {
                futures = Lists.newArrayListWithExpectedSize(tsKvEntries.size() * inserts);
                for (TsKvEntry tsKvEntry : tsKvEntries) {
                    saveWithoutLatestAndRegisterFutures(tenantId, futures, entityId, tsKvEntry, ttl);
                }
            }
        }
        return Futures.transform(Futures.allAsList(futures), SUM_ALL_INTEGERS, MoreExecutors.directExecutor());
    }
}
