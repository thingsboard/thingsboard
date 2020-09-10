/**
 * Copyright © 2016-2020 The Thingsboard Authors
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

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.common.data.kv.BaseReadTsKvQuery;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.DeleteTsKvQuery;
import org.thingsboard.server.common.data.kv.ReadTsKvQuery;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.stats.StatsFactory;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.AbstractTsKvEntity;
import org.thingsboard.server.dao.model.sqlts.latest.TsKvLatestCompositeKey;
import org.thingsboard.server.dao.model.sqlts.latest.TsKvLatestEntity;
import org.thingsboard.server.dao.sql.ScheduledLogExecutorComponent;
import org.thingsboard.server.dao.sql.TbSqlBlockingQueueParams;
import org.thingsboard.server.dao.sql.TbSqlBlockingQueueWrapper;
import org.thingsboard.server.dao.sqlts.insert.latest.InsertLatestTsRepository;
import org.thingsboard.server.dao.sqlts.latest.SearchTsKvLatestRepository;
import org.thingsboard.server.dao.sqlts.latest.TsKvLatestRepository;
import org.thingsboard.server.dao.timeseries.SimpleListenableFuture;
import org.thingsboard.server.dao.timeseries.TimeseriesLatestDao;
import org.thingsboard.server.dao.util.SqlTsLatestAnyDao;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
@SqlTsLatestAnyDao
public class SqlTimeseriesLatestDao extends BaseAbstractSqlTimeseriesDao implements TimeseriesLatestDao {

    private static final String DESC_ORDER = "DESC";

    @Autowired
    private TsKvLatestRepository tsKvLatestRepository;

    @Autowired
    protected AggregationTimeseriesDao aggregationTimeseriesDao;

    @Autowired
    private SearchTsKvLatestRepository searchTsKvLatestRepository;

    @Autowired
    private InsertLatestTsRepository insertLatestTsRepository;

    private TbSqlBlockingQueueWrapper<TsKvLatestEntity> tsLatestQueue;

    @Value("${sql.ts_latest.batch_size:1000}")
    private int tsLatestBatchSize;

    @Value("${sql.ts_latest.batch_max_delay:100}")
    private long tsLatestMaxDelay;

    @Value("${sql.ts_latest.stats_print_interval_ms:1000}")
    private long tsLatestStatsPrintIntervalMs;

    @Value("${sql.ts_latest.batch_threads:4}")
    private int tsLatestBatchThreads;

    @Value("${sql.batch_sort:false}")
    protected boolean batchSortEnabled;

    @Autowired
    protected ScheduledLogExecutorComponent logExecutor;

    @Autowired
    private StatsFactory statsFactory;

    @PostConstruct
    protected void init() {
        TbSqlBlockingQueueParams tsLatestParams = TbSqlBlockingQueueParams.builder()
                .logName("TS Latest")
                .batchSize(tsLatestBatchSize)
                .maxDelay(tsLatestMaxDelay)
                .statsPrintIntervalMs(tsLatestStatsPrintIntervalMs)
                .statsNamePrefix("ts.latest")
                .batchSortEnabled(false)
                .build();

        java.util.function.Function<TsKvLatestEntity, Integer> hashcodeFunction = entity -> entity.getEntityId().hashCode();
        tsLatestQueue = new TbSqlBlockingQueueWrapper<>(tsLatestParams, hashcodeFunction, tsLatestBatchThreads, statsFactory);

        tsLatestQueue.init(logExecutor, v -> {
            Map<TsKey, TsKvLatestEntity> trueLatest = new HashMap<>();
            v.forEach(ts -> {
                TsKey key = new TsKey(ts.getEntityId(), ts.getKey());
                TsKvLatestEntity old = trueLatest.get(key);
                if (old == null || old.getTs() < ts.getTs()) {
                    trueLatest.put(key, ts);
                }
            });
            List<TsKvLatestEntity> latestEntities = batchSortEnabled ?
                    trueLatest.values().stream().sorted(Comparator.comparing((Function<TsKvLatestEntity, UUID>) AbstractTsKvEntity::getEntityId)
                            .thenComparingInt(AbstractTsKvEntity::getKey)
                    )
                            .collect(Collectors.toList()) : new ArrayList<>(trueLatest.values());
            insertLatestTsRepository.saveOrUpdate(latestEntities);
        }, (l, r) -> 0);
    }

    @PreDestroy
    protected void destroy() {
        if (tsLatestQueue != null) {
            tsLatestQueue.destroy();
        }
    }

    @Override
    public ListenableFuture<Void> saveLatest(TenantId tenantId, EntityId entityId, TsKvEntry tsKvEntry) {
        return getSaveLatestFuture(entityId, tsKvEntry);
    }

    @Override
    public ListenableFuture<Void> removeLatest(TenantId tenantId, EntityId entityId, DeleteTsKvQuery query) {
        return getRemoveLatestFuture(tenantId, entityId, query);
    }

    @Override
    public ListenableFuture<TsKvEntry> findLatest(TenantId tenantId, EntityId entityId, String key) {
        return getFindLatestFuture(entityId, key);
    }

    @Override
    public ListenableFuture<List<TsKvEntry>> findAllLatest(TenantId tenantId, EntityId entityId) {
        return getFindAllLatestFuture(entityId);
    }

    private ListenableFuture<Void> getNewLatestEntryFuture(TenantId tenantId, EntityId entityId, DeleteTsKvQuery query) {
        ListenableFuture<List<TsKvEntry>> future = findNewLatestEntryFuture(tenantId, entityId, query);
        return Futures.transformAsync(future, entryList -> {
            if (entryList.size() == 1) {
                return getSaveLatestFuture(entityId, entryList.get(0));
            } else {
                log.trace("Could not find new latest value for [{}], key - {}", entityId, query.getKey());
            }
            return Futures.immediateFuture(null);
        }, service);
    }

    private ListenableFuture<List<TsKvEntry>> findNewLatestEntryFuture(TenantId tenantId, EntityId entityId, DeleteTsKvQuery query) {
        long startTs = 0;
        long endTs = query.getStartTs() - 1;
        ReadTsKvQuery findNewLatestQuery = new BaseReadTsKvQuery(query.getKey(), startTs, endTs, endTs - startTs, 1,
                Aggregation.NONE, DESC_ORDER);
        return aggregationTimeseriesDao.findAllAsync(tenantId, entityId, findNewLatestQuery);
    }

    protected ListenableFuture<TsKvEntry> getFindLatestFuture(EntityId entityId, String key) {
        TsKvLatestCompositeKey compositeKey =
                new TsKvLatestCompositeKey(
                        entityId.getId(),
                        getOrSaveKeyId(key));
        Optional<TsKvLatestEntity> entry = tsKvLatestRepository.findById(compositeKey);
        TsKvEntry result;
        if (entry.isPresent()) {
            TsKvLatestEntity tsKvLatestEntity = entry.get();
            tsKvLatestEntity.setStrKey(key);
            result = DaoUtil.getData(tsKvLatestEntity);
        } else {
            result = new BasicTsKvEntry(System.currentTimeMillis(), new StringDataEntry(key, null));
        }
        return Futures.immediateFuture(result);
    }

    protected ListenableFuture<Void> getRemoveLatestFuture(TenantId tenantId, EntityId entityId, DeleteTsKvQuery query) {
        ListenableFuture<TsKvEntry> latestFuture = getFindLatestFuture(entityId, query.getKey());

        ListenableFuture<Boolean> booleanFuture = Futures.transform(latestFuture, tsKvEntry -> {
            long ts = tsKvEntry.getTs();
            return ts > query.getStartTs() && ts <= query.getEndTs();
        }, service);

        ListenableFuture<Void> removedLatestFuture = Futures.transformAsync(booleanFuture, isRemove -> {
            if (isRemove) {
                TsKvLatestEntity latestEntity = new TsKvLatestEntity();
                latestEntity.setEntityId(entityId.getId());
                latestEntity.setKey(getOrSaveKeyId(query.getKey()));
                return service.submit(() -> {
                    tsKvLatestRepository.delete(latestEntity);
                    return null;
                });
            }
            return Futures.immediateFuture(null);
        }, service);

        final SimpleListenableFuture<Void> resultFuture = new SimpleListenableFuture<>();
        Futures.addCallback(removedLatestFuture, new FutureCallback<Void>() {
            @Override
            public void onSuccess(@Nullable Void result) {
                if (query.getRewriteLatestIfDeleted()) {
                    ListenableFuture<Void> savedLatestFuture = Futures.transformAsync(booleanFuture, isRemove -> {
                        if (isRemove) {
                            return getNewLatestEntryFuture(tenantId, entityId, query);
                        }
                        return Futures.immediateFuture(null);
                    }, service);

                    try {
                        resultFuture.set(savedLatestFuture.get());
                    } catch (InterruptedException | ExecutionException e) {
                        log.warn("Could not get latest saved value for [{}], {}", entityId, query.getKey(), e);
                    }
                } else {
                    resultFuture.set(null);
                }
            }

            @Override
            public void onFailure(Throwable t) {
                log.warn("[{}] Failed to process remove of the latest value", entityId, t);
            }
        }, MoreExecutors.directExecutor());
        return resultFuture;
    }

    protected ListenableFuture<List<TsKvEntry>> getFindAllLatestFuture(EntityId entityId) {
        return Futures.immediateFuture(
                DaoUtil.convertDataList(Lists.newArrayList(
                        searchTsKvLatestRepository.findAllByEntityId(entityId.getId()))));
    }

    protected ListenableFuture<Void> getSaveLatestFuture(EntityId entityId, TsKvEntry tsKvEntry) {
        TsKvLatestEntity latestEntity = new TsKvLatestEntity();
        latestEntity.setEntityId(entityId.getId());
        latestEntity.setTs(tsKvEntry.getTs());
        latestEntity.setKey(getOrSaveKeyId(tsKvEntry.getKey()));
        latestEntity.setStrValue(tsKvEntry.getStrValue().orElse(null));
        latestEntity.setDoubleValue(tsKvEntry.getDoubleValue().orElse(null));
        latestEntity.setLongValue(tsKvEntry.getLongValue().orElse(null));
        latestEntity.setBooleanValue(tsKvEntry.getBooleanValue().orElse(null));
        latestEntity.setJsonValue(tsKvEntry.getJsonValue().orElse(null));

        return tsLatestQueue.add(latestEntity);
    }

}
