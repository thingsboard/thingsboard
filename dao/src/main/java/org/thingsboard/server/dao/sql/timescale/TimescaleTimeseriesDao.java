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
package org.thingsboard.server.dao.sql.timescale;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.common.data.kv.BaseReadTsKvQuery;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.DeleteTsKvQuery;
import org.thingsboard.server.common.data.kv.ReadTsKvQuery;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.kv.TsKvQuery;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.TimescaleTsKvEntity;
import org.thingsboard.server.dao.sql.AbstractSqlTimeseriesDao;
import org.thingsboard.server.dao.timeseries.TimeseriesDao;
import org.thingsboard.server.dao.util.TimescaleDBTsDao;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static org.thingsboard.server.common.data.UUIDConverter.fromTimeUUID;


@Component
@Slf4j
@TimescaleDBTsDao
public class TimescaleTimeseriesDao extends AbstractSqlTimeseriesDao<TimescaleTsKvEntity> implements TimeseriesDao {

    private static final String TS = "ts";

    @Autowired
    private TsKvTimescaleRepository tsKvRepository;

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
            return findAllAsyncWithLimit(tenantId, entityId, query);
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

    private ListenableFuture<List<TsKvEntry>> findAllAsyncWithLimit(TenantId tenantId, EntityId entityId, ReadTsKvQuery query) {
        return Futures.immediateFuture(
                DaoUtil.convertDataList(
                        tsKvRepository.findAllWithLimit(
                                fromTimeUUID(tenantId.getId()),
                                fromTimeUUID(entityId.getId()),
                                query.getKey(),
                                query.getStartTs(),
                                query.getEndTs(),
                                new PageRequest(0, query.getLimit(),
                                        new Sort(Sort.Direction.fromString(
                                                query.getOrderBy()), "ts")))));
    }


    @Override
    public ListenableFuture<TsKvEntry> findLatest(TenantId tenantId, EntityId entityId, String key) {
        ListenableFuture<List<TimescaleTsKvEntity>> future = getLatest(tenantId, entityId, key, 0L, System.currentTimeMillis());
        return Futures.transform(future, latest -> {
            if (!CollectionUtils.isEmpty(latest)) {
                return DaoUtil.getData(latest.get(0));
            } else {
                return new BasicTsKvEntry(System.currentTimeMillis(), new StringDataEntry(key, null));
            }
        }, service);
    }

    @Override
    public ListenableFuture<List<TsKvEntry>> findAllLatest(TenantId tenantId, EntityId entityId) {
        return Futures.immediateFuture(DaoUtil.convertDataList(Lists.newArrayList(tsKvRepository.findAllLatestValues(fromTimeUUID(tenantId.getId()), fromTimeUUID(entityId.getId())))));
    }

    @Override
    public ListenableFuture<Void> save(TenantId tenantId, EntityId entityId, TsKvEntry tsKvEntry, long ttl) {
        TimescaleTsKvEntity entity = new TimescaleTsKvEntity();
        entity.setTenantId(fromTimeUUID(tenantId.getId()));
        entity.setEntityId(fromTimeUUID(entityId.getId()));
        entity.setTs(tsKvEntry.getTs());
        entity.setKey(tsKvEntry.getKey());
        entity.setStrValue(tsKvEntry.getStrValue().orElse(null));
        entity.setDoubleValue(tsKvEntry.getDoubleValue().orElse(null));
        entity.setLongValue(tsKvEntry.getLongValue().orElse(null));
        entity.setBooleanValue(tsKvEntry.getBooleanValue().orElse(null));
        log.trace("Saving entity to timescale db: {}", entity);
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
        return insertService.submit(() -> null);
    }

    @Override
    public ListenableFuture<Void> remove(TenantId tenantId, EntityId entityId, DeleteTsKvQuery query) {
        return service.submit(() -> {
            tsKvRepository.delete(
                    fromTimeUUID(tenantId.getId()),
                    fromTimeUUID(entityId.getId()),
                    query.getKey(),
                    query.getStartTs(),
                    query.getEndTs());
            return null;
        });
    }

    @Override
    public ListenableFuture<Void> removeLatest(TenantId tenantId, EntityId entityId, DeleteTsKvQuery query) {
        ListenableFuture<List<TimescaleTsKvEntity>> future = findLatestByQuery(tenantId, entityId, query);
        ListenableFuture<Boolean> booleanFuture = Futures.transform(future, latest -> {
            if (!CollectionUtils.isEmpty(latest)) {
                TimescaleTsKvEntity entity = latest.get(0);
                long ts = entity.getTs();
                if (ts > query.getStartTs() && ts <= query.getEndTs()) {
                    tsKvRepository.delete(entity);
                    return true;
                }
            }
            return false;
        }, service);
        return Futures.transformAsync(booleanFuture, isRemove -> {
            if (isRemove && query.getRewriteLatestIfDeleted()) {
                return getNewLatestEntryFuture(tenantId, entityId, query);
            }
            return Futures.immediateFuture(null);
        }, service);

    }

    @Override
    public ListenableFuture<Void> removePartition(TenantId tenantId, EntityId entityId, DeleteTsKvQuery query) {
        return service.submit(() -> null);
    }

    private ListenableFuture<Void> getNewLatestEntryFuture(TenantId tenantId, EntityId entityId, DeleteTsKvQuery query) {
        long startTs = 0;
        long endTs = query.getStartTs() - 1;
        ReadTsKvQuery findNewLatestQuery = new BaseReadTsKvQuery(query.getKey(), startTs, endTs, endTs - startTs, 1,
                Aggregation.NONE, DESC_ORDER);
        ListenableFuture<List<TsKvEntry>> future = findAllAsync(tenantId, entityId, findNewLatestQuery);

        return Futures.transformAsync(future, entryList -> {
            if (entryList.size() == 1) {
                return save(tenantId, entityId, entryList.get(0), 0L);
            } else {
                log.trace("Could not find new latest value for [{}], key - {}", entityId, query.getKey());
            }
            return Futures.immediateFuture(null);
        }, service);
    }

    private ListenableFuture<List<TimescaleTsKvEntity>> findLatestByQuery(TenantId tenantId, EntityId entityId, TsKvQuery query) {
        return getLatest(tenantId, entityId, query.getKey(), query.getStartTs(), query.getEndTs());
    }

    private ListenableFuture<List<TimescaleTsKvEntity>> getLatest(TenantId tenantId, EntityId entityId, String key, long start, long end) {
        return Futures.immediateFuture(tsKvRepository.findAllWithLimit(
                fromTimeUUID(tenantId.getId()),
                fromTimeUUID(entityId.getId()),
                key,
                start,
                end,
                new PageRequest(0, 1,
                        new Sort(Sort.Direction.DESC, TS))));
    }

    @Override
    protected ListenableFuture<Optional<TsKvEntry>> findAndAggregateAsync(TenantId tenantId, EntityId entityId, String key, long startTs, long endTs, long ts, Aggregation aggregation) {
        List<CompletableFuture<TimescaleTsKvEntity>> entitiesFutures = new ArrayList<>();
        String entityIdStr = fromTimeUUID(entityId.getId());
        String tenantIdStr = fromTimeUUID(tenantId.getId());
        switchAgregation(entityId, key, startTs, endTs, aggregation, entitiesFutures, entityIdStr, tenantIdStr);

        SettableFuture<TimescaleTsKvEntity> listenableFuture = SettableFuture.create();
        CompletableFuture<List<TimescaleTsKvEntity>> entities =
                CompletableFuture.allOf(entitiesFutures.toArray(new CompletableFuture[entitiesFutures.size()]))
                        .thenApply(v -> entitiesFutures.stream()
                                .map(CompletableFuture::join)
                                .collect(Collectors.toList()));

        entities.whenComplete((tsKvEntities, throwable) -> {
            if (throwable != null) {
                listenableFuture.setException(throwable);
            } else {
                TimescaleTsKvEntity result = null;
                for (TimescaleTsKvEntity entity : tsKvEntities) {
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
                entity.setKey(key);
                entity.setTs(ts);
                return Optional.of(DaoUtil.getData(entity));
            } else {
                return Optional.empty();
            }
        });
    }

    @Override
    protected void finfAvg(EntityId entityId, String key, long startTs, long endTs, List<CompletableFuture<TimescaleTsKvEntity>> entitiesFutures, String entityIdStr, String tenantIdStr) {
        entitiesFutures.add(tsKvRepository.findAvg(
                tenantIdStr,
                entityIdStr,
                key,
                startTs,
                endTs));
    }

    @Override
    protected void findMax(EntityId entityId, String key, long startTs, long endTs, List<CompletableFuture<TimescaleTsKvEntity>> entitiesFutures, String entityIdStr, String tenantIdStr) {
        entitiesFutures.add(tsKvRepository.findStringMax(
                tenantIdStr,
                entityIdStr,
                key,
                startTs,
                endTs));
        entitiesFutures.add(tsKvRepository.findNumericMax(
                tenantIdStr,
                entityIdStr,
                key,
                startTs,
                endTs));
    }

    @Override
    protected void findMin(EntityId entityId, String key, long startTs, long endTs, List<CompletableFuture<TimescaleTsKvEntity>> entitiesFutures, String entityIdStr, String tenantIdStr) {
        entitiesFutures.add(tsKvRepository.findStringMin(
                tenantIdStr,
                entityIdStr,
                key,
                startTs,
                endTs));
        entitiesFutures.add(tsKvRepository.findNumericMin(
                tenantIdStr,
                entityIdStr,
                key,
                startTs,
                endTs));
    }

    @Override
    protected void findSum(EntityId entityId, String key, long startTs, long endTs, List<CompletableFuture<TimescaleTsKvEntity>> entitiesFutures, String entityIdStr, String tenantIdStr) {
        entitiesFutures.add(tsKvRepository.findSum(
                tenantIdStr,
                entityIdStr,
                key,
                startTs,
                endTs));
    }

    @Override
    protected void findCount(EntityId entityId, String key, long startTs, long endTs, List<CompletableFuture<TimescaleTsKvEntity>> entitiesFutures, String entityIdStr, String tenantIdStr) {
        entitiesFutures.add(tsKvRepository.findCount(
                tenantIdStr,
                entityIdStr,
                key,
                startTs,
                endTs));
    }
}