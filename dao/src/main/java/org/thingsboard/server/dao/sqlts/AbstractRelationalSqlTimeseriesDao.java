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
package org.thingsboard.server.dao.sqlts;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
import org.thingsboard.server.dao.model.sql.AbsractTsKvEntity;
import org.thingsboard.server.dao.model.sqlts.latest.TsKvLatestCompositeKey;
import org.thingsboard.server.dao.model.sqlts.latest.TsKvLatestEntity;
import org.thingsboard.server.dao.sqlts.latest.TsKvLatestRepository;
import org.thingsboard.server.dao.timeseries.SimpleListenableFuture;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static org.thingsboard.server.common.data.UUIDConverter.fromTimeUUID;

@Slf4j
public abstract class AbstractRelationalSqlTimeseriesDao<T extends AbsractTsKvEntity> extends AbstractSqlTimeseriesDao {

    @Autowired
    private TsKvLatestRepository tsKvLatestRepository;

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
                futures.add(findAndAggregateAsync(entityId, query.getKey(), startTs, endTs, ts, query.getAggregation()));
                stepTs = endTs;
            }
            return getTskvEntriesFuture(Futures.allAsList(futures));
        }
    }

    protected abstract ListenableFuture<Optional<TsKvEntry>> findAndAggregateAsync(EntityId entityId, String key, long startTs, long endTs, long ts, Aggregation aggregation);

    protected abstract ListenableFuture<List<TsKvEntry>> findAllAsyncWithLimit(EntityId entityId, ReadTsKvQuery query);

    protected SettableFuture<T> setFutures(List<CompletableFuture<T>> entitiesFutures) {
        SettableFuture<T> listenableFuture = SettableFuture.create();
        CompletableFuture<List<T>> entities =
                CompletableFuture.allOf(entitiesFutures.toArray(new CompletableFuture[entitiesFutures.size()]))
                        .thenApply(v -> entitiesFutures.stream()
                                .map(CompletableFuture::join)
                                .collect(Collectors.toList()));

        entities.whenComplete((tsKvEntities, throwable) -> {
            if (throwable != null) {
                listenableFuture.setException(throwable);
            } else {
                T result = null;
                for (T entity : tsKvEntities) {
                    if (entity.isNotEmpty()) {
                        result = entity;
                        break;
                    }
                }
                listenableFuture.set(result);
            }
        });
        return listenableFuture;
    }

    protected ListenableFuture<TsKvEntry> getTsKvEntryListenableFuture(EntityId entityId, String key) {
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

    protected ListenableFuture<Void> getSaveLatestFuture(EntityId entityId, TsKvEntry tsKvEntry) {
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

    protected ListenableFuture<TsKvEntry> getFindLatestFuture(EntityId entityId, String key) {
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

    protected ListenableFuture<Void> getRemoveLatestFuture(TenantId tenantId, EntityId entityId, DeleteTsKvQuery query) {
        ListenableFuture<TsKvEntry> latestFuture = getFindLatestFuture(entityId, query.getKey());

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

    protected ListenableFuture<List<TsKvEntry>> getFindAllLatestFuture(EntityId entityId) {
        return Futures.immediateFuture(
                DaoUtil.convertDataList(Lists.newArrayList(
                        tsKvLatestRepository.findAllByEntityTypeAndEntityId(
                                entityId.getEntityType(),
                                UUIDConverter.fromTimeUUID(entityId.getId())))));
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


}