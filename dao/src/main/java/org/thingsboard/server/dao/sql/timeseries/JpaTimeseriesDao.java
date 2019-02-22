/**
 * Copyright © 2016-2019 The Thingsboard Authors
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
package org.thingsboard.server.dao.sql.timeseries;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.UUIDConverter;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.common.data.kv.BaseReadTsKvQuery;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.DeleteTsKvQuery;
import org.thingsboard.server.common.data.kv.ReadTsKvQuery;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.TsKvEntity;
import org.thingsboard.server.dao.model.sql.TsKvLatestCompositeKey;
import org.thingsboard.server.dao.model.sql.TsKvLatestEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDaoListeningExecutorService;
import org.thingsboard.server.dao.timeseries.SimpleListenableFuture;
import org.thingsboard.server.dao.timeseries.TimeseriesDao;
import org.thingsboard.server.dao.timeseries.TsInsertExecutorType;
import org.thingsboard.server.dao.util.SqlTsDao;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static org.thingsboard.server.common.data.UUIDConverter.fromTimeUUID;


@Component
@Slf4j
@SqlTsDao
public class JpaTimeseriesDao extends JpaAbstractDaoListeningExecutorService implements TimeseriesDao {

    private static final String DESC_ORDER = "DESC";

    @Value("${sql.ts_inserts_executor_type}")
    private String insertExecutorType;

    @Value("${sql.ts_inserts_fixed_thread_pool_size}")
    private int insertFixedThreadPoolSize;

    private ListeningExecutorService insertService;

    @Autowired
    private TsKvRepository tsKvRepository;

    @Autowired
    private TsKvLatestRepository tsKvLatestRepository;

    @PostConstruct
    public void init() {
        Optional<TsInsertExecutorType> executorTypeOptional = TsInsertExecutorType.parse(insertExecutorType);
        TsInsertExecutorType executorType;
        if (executorTypeOptional.isPresent()) {
            executorType = executorTypeOptional.get();
        } else {
            executorType = TsInsertExecutorType.FIXED;
        }
        switch (executorType) {
            case SINGLE:
                insertService = MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor());
                break;
            case FIXED:
                int poolSize = insertFixedThreadPoolSize;
                if (poolSize <= 0) {
                    poolSize = 10;
                }
                insertService = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(poolSize));
                break;
            case CACHED:
                insertService = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool());
                break;
        }
    }

    @Override
    public ListenableFuture<List<TsKvEntry>> findAllAsync(TenantId tenantId, EntityId entityId, List<ReadTsKvQuery> queries) {
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
                        .flatMap(List::stream)
                        .collect(Collectors.toList());
            }
        }, service);
    }

    private ListenableFuture<List<TsKvEntry>> findAllAsync(TenantId tenantId, EntityId entityId, ReadTsKvQuery query) {
        if (query.getAggregation() == Aggregation.NONE) {
            return findAllAsyncWithLimit(entityId, query);
        } else {
            long stepTs = query.getStartTs();
            List<ListenableFuture<Optional<TsKvEntry>>> futures = new ArrayList<>();
            while (stepTs < query.getEndTs()) {
                long startTs = stepTs;
                long endTs = stepTs + query.getInterval();
                long ts = startTs + (endTs - startTs) / 2;
                futures.add(findAndAggregateAsync(entityId, query.getKey(), startTs, endTs, ts, query.getAggregation()));
                stepTs = endTs;
            }
            ListenableFuture<List<Optional<TsKvEntry>>> future = Futures.allAsList(futures);
            return Futures.transform(future, new Function<List<Optional<TsKvEntry>>, List<TsKvEntry>>() {
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
            }, service);
        }
    }

    private ListenableFuture<Optional<TsKvEntry>> findAndAggregateAsync(EntityId entityId, String key, long startTs, long endTs, long ts, Aggregation aggregation) {
        List<CompletableFuture<TsKvEntity>> entitiesFutures = new ArrayList<>();
        String entityIdStr = fromTimeUUID(entityId.getId());
        switch (aggregation) {
            case AVG:
                entitiesFutures.add(tsKvRepository.findAvg(
                        entityIdStr,
                        entityId.getEntityType(),
                        key,
                        startTs,
                        endTs));

                break;
            case MAX:
                entitiesFutures.add(tsKvRepository.findStringMax(
                        entityIdStr,
                        entityId.getEntityType(),
                        key,
                        startTs,
                        endTs));
                entitiesFutures.add(tsKvRepository.findNumericMax(
                        entityIdStr,
                        entityId.getEntityType(),
                        key,
                        startTs,
                        endTs));

                break;
            case MIN:
                entitiesFutures.add(tsKvRepository.findStringMin(
                        entityIdStr,
                        entityId.getEntityType(),
                        key,
                        startTs,
                        endTs));
                entitiesFutures.add(tsKvRepository.findNumericMin(
                        entityIdStr,
                        entityId.getEntityType(),
                        key,
                        startTs,
                        endTs));
                break;
            case SUM:
                entitiesFutures.add(tsKvRepository.findSum(
                        entityIdStr,
                        entityId.getEntityType(),
                        key,
                        startTs,
                        endTs));
                break;
            case COUNT:
                entitiesFutures.add(tsKvRepository.findCount(
                        entityIdStr,
                        entityId.getEntityType(),
                        key,
                        startTs,
                        endTs));

                break;
            default:
                throw new IllegalArgumentException("Not supported aggregation type: " + aggregation);
        }

        SettableFuture<TsKvEntity> listenableFuture = SettableFuture.create();


        CompletableFuture<List<TsKvEntity>> entities =
                CompletableFuture.allOf(entitiesFutures.toArray(new CompletableFuture[entitiesFutures.size()]))
                .thenApply(v -> entitiesFutures.stream()
                        .map(CompletableFuture::join)
                        .collect(Collectors.toList()));


        entities.whenComplete((tsKvEntities, throwable) -> {
            if (throwable != null) {
                listenableFuture.setException(throwable);
            } else {
                TsKvEntity result = null;
                for (TsKvEntity entity : tsKvEntities) {
                    if (entity.isNotEmpty()) {
                        result = entity;
                        break;
                    }
                }
                listenableFuture.set(result);
            }
        });
        return Futures.transform(listenableFuture, new Function<TsKvEntity, Optional<TsKvEntry>>() {
            @Override
            public Optional<TsKvEntry> apply(@Nullable TsKvEntity entity) {
                if (entity != null && entity.isNotEmpty()) {
                    entity.setEntityId(entityIdStr);
                    entity.setEntityType(entityId.getEntityType());
                    entity.setKey(key);
                    entity.setTs(ts);
                    return Optional.of(DaoUtil.getData(entity));
                } else {
                    return Optional.empty();
                }
            }
        });
    }

    private ListenableFuture<List<TsKvEntry>> findAllAsyncWithLimit(EntityId entityId, ReadTsKvQuery query) {
        return Futures.immediateFuture(
                DaoUtil.convertDataList(
                        tsKvRepository.findAllWithLimit(
                                fromTimeUUID(entityId.getId()),
                                entityId.getEntityType(),
                                query.getKey(),
                                query.getStartTs(),
                                query.getEndTs(),
                                new PageRequest(0, query.getLimit(),
                                        new Sort(Sort.Direction.fromString(
                                                query.getOrderBy()), "ts")))));
    }

    @Override
    public ListenableFuture<TsKvEntry> findLatest(TenantId tenantId, EntityId entityId, String key) {
        TsKvLatestCompositeKey compositeKey =
                new TsKvLatestCompositeKey(
                        entityId.getEntityType(),
                        fromTimeUUID(entityId.getId()),
                        key);
        Optional<TsKvLatestEntity> entry = tsKvLatestRepository.findById(compositeKey);
        TsKvEntry result;
        if (entry.isPresent()) {
            result = DaoUtil.getData(entry.get());
        } else {
            result = new BasicTsKvEntry(System.currentTimeMillis(), new StringDataEntry(key, null));
        }
        return Futures.immediateFuture(result);
    }

    @Override
    public ListenableFuture<List<TsKvEntry>> findAllLatest(TenantId tenantId, EntityId entityId) {
        return Futures.immediateFuture(
                DaoUtil.convertDataList(Lists.newArrayList(
                        tsKvLatestRepository.findAllByEntityTypeAndEntityId(
                                entityId.getEntityType(),
                                UUIDConverter.fromTimeUUID(entityId.getId())))));
    }

    @Override
    public ListenableFuture<Void> save(TenantId tenantId, EntityId entityId, TsKvEntry tsKvEntry, long ttl) {
        TsKvEntity entity = new TsKvEntity();
        entity.setEntityType(entityId.getEntityType());
        entity.setEntityId(fromTimeUUID(entityId.getId()));
        entity.setTs(tsKvEntry.getTs());
        entity.setKey(tsKvEntry.getKey());
        entity.setStrValue(tsKvEntry.getStrValue().orElse(null));
        entity.setDoubleValue(tsKvEntry.getDoubleValue().orElse(null));
        entity.setLongValue(tsKvEntry.getLongValue().orElse(null));
        entity.setBooleanValue(tsKvEntry.getBooleanValue().orElse(null));
        log.trace("Saving entity: {}", entity);
        return insertService.submit(() -> {
            tsKvRepository.save(entity);
            return null;
        });
    }

    @Override
    public ListenableFuture<Void> savePartition(TenantId tenantId, EntityId entityId, long tsKvEntryTs, String key, long ttl) {
        return insertService.submit(() -> null);
    }

    @Override
    public ListenableFuture<Void> saveLatest(TenantId tenantId, EntityId entityId, TsKvEntry tsKvEntry) {
        TsKvLatestEntity latestEntity = new TsKvLatestEntity();
        latestEntity.setEntityType(entityId.getEntityType());
        latestEntity.setEntityId(fromTimeUUID(entityId.getId()));
        latestEntity.setTs(tsKvEntry.getTs());
        latestEntity.setKey(tsKvEntry.getKey());
        latestEntity.setStrValue(tsKvEntry.getStrValue().orElse(null));
        latestEntity.setDoubleValue(tsKvEntry.getDoubleValue().orElse(null));
        latestEntity.setLongValue(tsKvEntry.getLongValue().orElse(null));
        latestEntity.setBooleanValue(tsKvEntry.getBooleanValue().orElse(null));
        return insertService.submit(() -> {
            tsKvLatestRepository.save(latestEntity);
            return null;
        });
    }

    @Override
    public ListenableFuture<Void> remove(TenantId tenantId, EntityId entityId, DeleteTsKvQuery query) {
        return service.submit(() -> {
            tsKvRepository.delete(
                    fromTimeUUID(entityId.getId()),
                    entityId.getEntityType(),
                    query.getKey(),
                    query.getStartTs(),
                    query.getEndTs());
            return null;
        });
    }

    @Override
    public ListenableFuture<Void> removeLatest(TenantId tenantId, EntityId entityId, DeleteTsKvQuery query) {
        ListenableFuture<TsKvEntry> latestFuture = findLatest(tenantId, entityId, query.getKey());

        ListenableFuture<Boolean> booleanFuture = Futures.transform(latestFuture, tsKvEntry -> {
            long ts = tsKvEntry.getTs();
            return ts > query.getStartTs() && ts <= query.getEndTs();
        }, service);

        ListenableFuture<Void> removedLatestFuture = Futures.transformAsync(booleanFuture, isRemove -> {
            if (isRemove) {
                TsKvLatestEntity latestEntity = new TsKvLatestEntity();
                latestEntity.setEntityType(entityId.getEntityType());
                latestEntity.setEntityId(fromTimeUUID(entityId.getId()));
                latestEntity.setKey(query.getKey());
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
        });
        return resultFuture;
    }

    private ListenableFuture<Void> getNewLatestEntryFuture(TenantId tenantId, EntityId entityId, DeleteTsKvQuery query) {
        long startTs = 0;
        long endTs = query.getStartTs() - 1;
        ReadTsKvQuery findNewLatestQuery = new BaseReadTsKvQuery(query.getKey(), startTs, endTs, endTs - startTs, 1,
                Aggregation.NONE, DESC_ORDER);
        ListenableFuture<List<TsKvEntry>> future = findAllAsync(tenantId, entityId, findNewLatestQuery);

        return Futures.transformAsync(future, entryList -> {
            if (entryList.size() == 1) {
                return saveLatest(tenantId, entityId, entryList.get(0));
            } else {
                log.trace("Could not find new latest value for [{}], key - {}", entityId, query.getKey());
            }
            return Futures.immediateFuture(null);
        }, service);
    }

    @Override
    public ListenableFuture<Void> removePartition(TenantId tenantId, EntityId entityId, DeleteTsKvQuery query) {
        return service.submit(() -> null);
    }

    @PreDestroy
    void onDestroy() {
        if (insertService != null) {
            insertService.shutdown();
        }
    }

}
