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
package org.thingsboard.server.dao.sql.timeseries;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.UUIDConverter;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.*;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.TsKvEntity;
import org.thingsboard.server.dao.model.sql.TsKvLatestCompositeKey;
import org.thingsboard.server.dao.model.sql.TsKvLatestEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDaoListeningExecutorService;
import org.thingsboard.server.dao.timeseries.TimeseriesDao;
import org.thingsboard.server.dao.util.SqlDao;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static org.thingsboard.server.common.data.UUIDConverter.fromTimeUUID;


@Component
@Slf4j
@SqlDao
public class JpaTimeseriesDao extends JpaAbstractDaoListeningExecutorService implements TimeseriesDao {

    @Autowired
    private TsKvRepository tsKvRepository;

    @Autowired
    private TsKvLatestRepository tsKvLatestRepository;

    @Override
    public ListenableFuture<List<TsKvEntry>> findAllAsync(EntityId entityId, List<TsKvQuery> queries) {
        List<ListenableFuture<List<TsKvEntry>>> futures = queries
                .stream()
                .map(query -> findAllAsync(entityId, query))
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

    private ListenableFuture<List<TsKvEntry>> findAllAsync(EntityId entityId, TsKvQuery query) {
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
        CompletableFuture<TsKvEntity> entity;
        String entityIdStr = fromTimeUUID(entityId.getId());
        switch (aggregation) {
            case AVG:
                entity = tsKvRepository.findAvg(
                        entityIdStr,
                        entityId.getEntityType(),
                        key,
                        startTs,
                        endTs);

                break;
            case MAX:
                entity = tsKvRepository.findMax(
                        entityIdStr,
                        entityId.getEntityType(),
                        key,
                        startTs,
                        endTs);

                break;
            case MIN:
                entity = tsKvRepository.findMin(
                        entityIdStr,
                        entityId.getEntityType(),
                        key,
                        startTs,
                        endTs);

                break;
            case SUM:
                entity = tsKvRepository.findSum(
                        entityIdStr,
                        entityId.getEntityType(),
                        key,
                        startTs,
                        endTs);

                break;
            case COUNT:
                entity = tsKvRepository.findCount(
                        entityIdStr,
                        entityId.getEntityType(),
                        key,
                        startTs,
                        endTs);

                break;
            default:
                throw new IllegalArgumentException("Not supported aggregation type: " + aggregation);
        }

        SettableFuture<TsKvEntity> listenableFuture = SettableFuture.create();
        entity.whenComplete((tsKvEntity, throwable) -> {
            if (throwable != null) {
                listenableFuture.setException(throwable);
            } else {
                listenableFuture.set(tsKvEntity);
            }
        });
        return Futures.transform(listenableFuture, new Function<TsKvEntity, Optional<TsKvEntry>>() {
            @Nullable
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

    private ListenableFuture<List<TsKvEntry>> findAllAsyncWithLimit(EntityId entityId, TsKvQuery query) {
        return Futures.immediateFuture(
                DaoUtil.convertDataList(
                        tsKvRepository.findAllWithLimit(
                                fromTimeUUID(entityId.getId()),
                                entityId.getEntityType(),
                                query.getKey(),
                                query.getStartTs(),
                                query.getEndTs(),
                                new PageRequest(0, query.getLimit()))));
    }

    @Override
    public ListenableFuture<TsKvEntry> findLatest(EntityId entityId, String key) {
        TsKvLatestCompositeKey compositeKey =
                new TsKvLatestCompositeKey(
                        entityId.getEntityType(),
                        fromTimeUUID(entityId.getId()),
                        key);
        TsKvLatestEntity entry = tsKvLatestRepository.findOne(compositeKey);
        TsKvEntry result;
        if (entry != null) {
            result = DaoUtil.getData(entry);
        } else {
            result = new BasicTsKvEntry(System.currentTimeMillis(), new StringDataEntry(key, null));
        }
        return Futures.immediateFuture(result);
    }

    @Override
    public ListenableFuture<List<TsKvEntry>> findAllLatest(EntityId entityId) {
        return Futures.immediateFuture(
                DaoUtil.convertDataList(Lists.newArrayList(
                        tsKvLatestRepository.findAllByEntityTypeAndEntityId(
                                entityId.getEntityType(),
                                UUIDConverter.fromTimeUUID(entityId.getId())))));
    }

    @Override
    public ListenableFuture<Void> save(EntityId entityId, TsKvEntry tsKvEntry, long ttl) {
        TsKvEntity entity = new TsKvEntity();
        entity.setEntityType(entityId.getEntityType());
        entity.setEntityId(fromTimeUUID(entityId.getId()));
        entity.setTs(tsKvEntry.getTs());
        entity.setKey(tsKvEntry.getKey());
        entity.setStrValue(tsKvEntry.getStrValue().orElse(null));
        entity.setDoubleValue(tsKvEntry.getDoubleValue().orElse(null));
        entity.setLongValue(tsKvEntry.getLongValue().orElse(null));
        entity.setBooleanValue(tsKvEntry.getBooleanValue().orElse(null));
        return service.submit(() -> {
            tsKvRepository.save(entity);
            return null;
        });
    }

    @Override
    public ListenableFuture<Void> savePartition(EntityId entityId, long tsKvEntryTs, String key, long ttl) {
        return service.submit(() -> null);
    }

    @Override
    public ListenableFuture<Void> saveLatest(EntityId entityId, TsKvEntry tsKvEntry) {
        TsKvLatestEntity latestEntity = new TsKvLatestEntity();
        latestEntity.setEntityType(entityId.getEntityType());
        latestEntity.setEntityId(fromTimeUUID(entityId.getId()));
        latestEntity.setTs(tsKvEntry.getTs());
        latestEntity.setKey(tsKvEntry.getKey());
        latestEntity.setStrValue(tsKvEntry.getStrValue().orElse(null));
        latestEntity.setDoubleValue(tsKvEntry.getDoubleValue().orElse(null));
        latestEntity.setLongValue(tsKvEntry.getLongValue().orElse(null));
        latestEntity.setBooleanValue(tsKvEntry.getBooleanValue().orElse(null));
        return service.submit(() -> {
            tsKvLatestRepository.save(latestEntity);
            return null;
        });
    }

}
