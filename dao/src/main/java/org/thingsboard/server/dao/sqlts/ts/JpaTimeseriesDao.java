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
package org.thingsboard.server.dao.sqlts.ts;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.UUIDConverter;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.DeleteTsKvQuery;
import org.thingsboard.server.common.data.kv.ReadTsKvQuery;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sqlts.ts.TsKvEntity;
import org.thingsboard.server.dao.model.sqlts.ts.TsKvLatestCompositeKey;
import org.thingsboard.server.dao.model.sqlts.ts.TsKvLatestEntity;
import org.thingsboard.server.dao.sqlts.AbstractSqlTimeseriesDao;
import org.thingsboard.server.dao.sqlts.AbstractTimeseriesInsertRepository;
import org.thingsboard.server.dao.timeseries.SimpleListenableFuture;
import org.thingsboard.server.dao.timeseries.TimeseriesDao;
import org.thingsboard.server.dao.util.SqlTsDao;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static org.thingsboard.server.common.data.UUIDConverter.fromTimeUUID;


@Component
@Slf4j
@SqlTsDao
public class JpaTimeseriesDao extends AbstractSqlTimeseriesDao implements TimeseriesDao {

    @Autowired
    private TsKvRepository tsKvRepository;

    @Autowired
    private TsKvLatestRepository tsKvLatestRepository;

    @Autowired
    private AbstractTimeseriesInsertRepository insertRepository;

    @Override
    public ListenableFuture<List<TsKvEntry>> findAllAsync(TenantId tenantId, EntityId entityId, List<ReadTsKvQuery> queries) {
        return processFindAllAsync(tenantId, entityId, queries);
    }

    protected ListenableFuture<List<TsKvEntry>> findAllAsync(TenantId tenantId, EntityId entityId, ReadTsKvQuery query) {
        if (query.getAggregation() == Aggregation.NONE) {
            return findAllAsyncWithLimit(entityId, query);
        } else {
            long stepTs = query.getStartTs();
            List<ListenableFuture<Optional<TsKvEntry>>> futures = new ArrayList<>();
            while (stepTs < query.getEndTs()) {
                long startTs = stepTs;
                long endTs = stepTs + query.getInterval();
                long ts = startTs + (endTs - startTs) / 2;
                futures.add(findAndAggregateAsync(tenantId, entityId, query.getKey(), startTs, endTs, ts, query.getAggregation()));
                stepTs = endTs;
            }
            return getTskvEntriesFuture(Futures.allAsList(futures));
        }
    }

    private ListenableFuture<Optional<TsKvEntry>> findAndAggregateAsync(TenantId tenantId, EntityId entityId, String key, long startTs, long endTs, long ts, Aggregation aggregation) {
        List<CompletableFuture<TsKvEntity>> entitiesFutures = new ArrayList<>();
        String entityIdStr = fromTimeUUID(entityId.getId());
        switchAgregation(entityId, key, startTs, endTs, aggregation, entitiesFutures, entityIdStr);

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
        return Futures.transform(listenableFuture, entity -> {
            if (entity != null && entity.isNotEmpty()) {
                entity.setEntityId(entityIdStr);
                entity.setEntityType(entityId.getEntityType());
                entity.setKey(key);
                entity.setTs(ts);
                return Optional.of(DaoUtil.getData(entity));
            } else {
                return Optional.empty();
            }
        });
    }

    private void switchAgregation(EntityId entityId, String key, long startTs, long endTs, Aggregation aggregation, List<CompletableFuture<TsKvEntity>> entitiesFutures, String entityIdStr) {
        switch (aggregation) {
            case AVG:
                findAvg(entityId, key, startTs, endTs, entitiesFutures, entityIdStr);
                break;
            case MAX:
                findMax(entityId, key, startTs, endTs, entitiesFutures, entityIdStr);
                break;
            case MIN:
                findMin(entityId, key, startTs, endTs, entitiesFutures, entityIdStr);
                break;
            case SUM:
                findSum(entityId, key, startTs, endTs, entitiesFutures, entityIdStr);
                break;
            case COUNT:
                findCount(entityId, key, startTs, endTs, entitiesFutures, entityIdStr);
                break;
            default:
                throw new IllegalArgumentException("Not supported aggregation type: " + aggregation);
        }
    }

    private void findCount(EntityId entityId, String key, long startTs, long endTs, List<CompletableFuture<TsKvEntity>> entitiesFutures, String entityIdStr) {
        entitiesFutures.add(tsKvRepository.findCount(
                entityIdStr,
                entityId.getEntityType(),
                key,
                startTs,
                endTs));
    }

    private void findSum(EntityId entityId, String key, long startTs, long endTs, List<CompletableFuture<TsKvEntity>> entitiesFutures, String entityIdStr) {
        entitiesFutures.add(tsKvRepository.findSum(
                entityIdStr,
                entityId.getEntityType(),
                key,
                startTs,
                endTs));
    }

    private void findMin(EntityId entityId, String key, long startTs, long endTs, List<CompletableFuture<TsKvEntity>> entitiesFutures, String entityIdStr) {
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
    }

    private void findMax(EntityId entityId, String key, long startTs, long endTs, List<CompletableFuture<TsKvEntity>> entitiesFutures, String entityIdStr) {
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
    }

    private void findAvg(EntityId entityId, String key, long startTs, long endTs, List<CompletableFuture<TsKvEntity>> entitiesFutures, String entityIdStr) {
        entitiesFutures.add(tsKvRepository.findAvg(
                entityIdStr,
                entityId.getEntityType(),
                key,
                startTs,
                endTs));
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
            insertRepository.saveOrUpdate(entity);
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
        ListenableFuture<List<TsKvEntry>> future = findNewLatestEntryFuture(tenantId, entityId, query);
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
}