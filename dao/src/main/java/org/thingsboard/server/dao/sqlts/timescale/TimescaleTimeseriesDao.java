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
package org.thingsboard.server.dao.sqlts.timescale;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.common.data.kv.DeleteTsKvQuery;
import org.thingsboard.server.common.data.kv.ReadTsKvQuery;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.stats.StatsFactory;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.AbstractTsKvEntity;
import org.thingsboard.server.dao.model.sqlts.timescale.ts.TimescaleTsKvEntity;
import org.thingsboard.server.dao.sql.TbSqlBlockingQueueParams;
import org.thingsboard.server.dao.sql.TbSqlBlockingQueueWrapper;
import org.thingsboard.server.dao.sqlts.AbstractSqlTimeseriesDao;
import org.thingsboard.server.dao.sqlts.insert.InsertTsRepository;
import org.thingsboard.server.dao.timeseries.TimeseriesDao;
import org.thingsboard.server.dao.util.TimescaleDBTsDao;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

@Component
@Slf4j
@TimescaleDBTsDao
public class TimescaleTimeseriesDao extends AbstractSqlTimeseriesDao implements TimeseriesDao {

    @Autowired
    private TsKvTimescaleRepository tsKvRepository;

    @Autowired
    private AggregationRepository aggregationRepository;

    @Autowired
    private StatsFactory statsFactory;

    @Autowired
    protected InsertTsRepository<TimescaleTsKvEntity> insertRepository;

    protected TbSqlBlockingQueueWrapper<TimescaleTsKvEntity> tsQueue;

    @PostConstruct
    protected void init() {
        TbSqlBlockingQueueParams tsParams = TbSqlBlockingQueueParams.builder()
                .logName("TS Timescale")
                .batchSize(tsBatchSize)
                .maxDelay(tsMaxDelay)
                .statsPrintIntervalMs(tsStatsPrintIntervalMs)
                .statsNamePrefix("ts.timescale")
                .batchSortEnabled(batchSortEnabled)
                .build();

        Function<TimescaleTsKvEntity, Integer> hashcodeFunction = entity -> entity.getEntityId().hashCode();
        tsQueue = new TbSqlBlockingQueueWrapper<>(tsParams, hashcodeFunction, timescaleBatchThreads, statsFactory);

        tsQueue.init(logExecutor, v -> insertRepository.saveOrUpdate(v),
                Comparator.comparing((Function<TimescaleTsKvEntity, UUID>) AbstractTsKvEntity::getEntityId)
                        .thenComparing(AbstractTsKvEntity::getKey)
                        .thenComparing(AbstractTsKvEntity::getTs)
        );
    }

    @PreDestroy
    protected void destroy() {
        if (tsQueue != null) {
            tsQueue.destroy();
        }
    }

    @Override
    public ListenableFuture<List<TsKvEntry>> findAllAsync(TenantId tenantId, EntityId entityId, List<ReadTsKvQuery> queries) {
        return processFindAllAsync(tenantId, entityId, queries);
    }

    @Override
    public ListenableFuture<Integer> save(TenantId tenantId, EntityId entityId, TsKvEntry tsKvEntry, long ttl) {
        int dataPointDays = getDataPointDays(tsKvEntry,  computeTtl(ttl));
        String strKey = tsKvEntry.getKey();
        Integer keyId = getOrSaveKeyId(strKey);
        TimescaleTsKvEntity entity = new TimescaleTsKvEntity();
        entity.setEntityId(entityId.getId());
        entity.setTs(tsKvEntry.getTs());
        entity.setKey(keyId);
        entity.setStrValue(tsKvEntry.getStrValue().orElse(null));
        entity.setDoubleValue(tsKvEntry.getDoubleValue().orElse(null));
        entity.setLongValue(tsKvEntry.getLongValue().orElse(null));
        entity.setBooleanValue(tsKvEntry.getBooleanValue().orElse(null));
        entity.setJsonValue(tsKvEntry.getJsonValue().orElse(null));
        log.trace("Saving entity to timescale db: {}", entity);
        return Futures.transform(tsQueue.add(entity), v -> dataPointDays, MoreExecutors.directExecutor());
    }

    @Override
    public ListenableFuture<Integer> savePartition(TenantId tenantId, EntityId entityId, long tsKvEntryTs, String key, long ttl) {
        return Futures.immediateFuture(0);
    }

    @Override
    public ListenableFuture<Void> remove(TenantId tenantId, EntityId entityId, DeleteTsKvQuery query) {
        String strKey = query.getKey();
        Integer keyId = getOrSaveKeyId(strKey);
        return service.submit(() -> {
            tsKvRepository.delete(
                    entityId.getId(),
                    keyId,
                    query.getStartTs(),
                    query.getEndTs());
            return null;
        });
    }

    @Override
    public ListenableFuture<Void> removePartition(TenantId tenantId, EntityId entityId, DeleteTsKvQuery query) {
        return service.submit(() -> null);
    }

    @Override
    public ListenableFuture<List<TsKvEntry>> findAllAsync(TenantId tenantId, EntityId entityId, ReadTsKvQuery query) {
        if (query.getAggregation() == Aggregation.NONE) {
            return findAllAsyncWithLimit(entityId, query);
        } else {
            long startTs = query.getStartTs();
            long endTs = query.getEndTs();
            long timeBucket = query.getInterval();
            ListenableFuture<List<Optional<TsKvEntry>>> future = findAllAndAggregateAsync(entityId, query.getKey(), startTs, endTs, timeBucket, query.getAggregation());
            return getTskvEntriesFuture(future);
        }
    }

    private ListenableFuture<List<TsKvEntry>> findAllAsyncWithLimit(EntityId entityId, ReadTsKvQuery query) {
        String strKey = query.getKey();
        Integer keyId = getOrSaveKeyId(strKey);
        List<TimescaleTsKvEntity> timescaleTsKvEntities = tsKvRepository.findAllWithLimit(
                entityId.getId(),
                keyId,
                query.getStartTs(),
                query.getEndTs(),
                PageRequest.of(0, query.getLimit(),
                        Sort.by(Sort.Direction.fromString(
                                query.getOrder()), "ts")));
        timescaleTsKvEntities.forEach(tsKvEntity -> tsKvEntity.setStrKey(strKey));
        return Futures.immediateFuture(DaoUtil.convertDataList(timescaleTsKvEntities));
    }

    private ListenableFuture<List<Optional<TsKvEntry>>> findAllAndAggregateAsync(EntityId entityId, String key, long startTs, long endTs, long timeBucket, Aggregation aggregation) {
        CompletableFuture<List<TimescaleTsKvEntity>> listCompletableFuture = switchAggregation(key, startTs, endTs, timeBucket, aggregation, entityId.getId());
        SettableFuture<List<TimescaleTsKvEntity>> listenableFuture = SettableFuture.create();
        listCompletableFuture.whenComplete((timescaleTsKvEntities, throwable) -> {
            if (throwable != null) {
                listenableFuture.setException(throwable);
            } else {
                listenableFuture.set(timescaleTsKvEntities);
            }
        });
        return Futures.transform(listenableFuture, timescaleTsKvEntities -> {
            if (!CollectionUtils.isEmpty(timescaleTsKvEntities)) {
                List<Optional<TsKvEntry>> result = new ArrayList<>();
                timescaleTsKvEntities.forEach(entity -> {
                    if (entity != null && entity.isNotEmpty()) {
                        entity.setEntityId(entityId.getId());
                        entity.setStrKey(key);
                        result.add(Optional.of(DaoUtil.getData(entity)));
                    } else {
                        result.add(Optional.empty());
                    }
                });
                return result;
            } else {
                return Collections.emptyList();
            }
        }, MoreExecutors.directExecutor());
    }

    private CompletableFuture<List<TimescaleTsKvEntity>> switchAggregation(String key, long startTs, long endTs, long timeBucket, Aggregation aggregation, UUID entityId) {
        switch (aggregation) {
            case AVG:
                return findAvg(key, startTs, endTs, timeBucket, entityId);
            case MAX:
                return findMax(key, startTs, endTs, timeBucket, entityId);
            case MIN:
                return findMin(key, startTs, endTs, timeBucket, entityId);
            case SUM:
                return findSum(key, startTs, endTs, timeBucket, entityId);
            case COUNT:
                return findCount(key, startTs, endTs, timeBucket, entityId);
            default:
                throw new IllegalArgumentException("Not supported aggregation type: " + aggregation);
        }
    }

    private CompletableFuture<List<TimescaleTsKvEntity>> findCount(String key, long startTs, long endTs, long timeBucket, UUID entityId) {
        Integer keyId = getOrSaveKeyId(key);
        return aggregationRepository.findCount(
                entityId,
                keyId,
                timeBucket,
                startTs,
                endTs);
    }

    private CompletableFuture<List<TimescaleTsKvEntity>> findSum(String key, long startTs, long endTs, long timeBucket, UUID entityId) {
        Integer keyId = getOrSaveKeyId(key);
        return aggregationRepository.findSum(
                entityId,
                keyId,
                timeBucket,
                startTs,
                endTs);
    }

    private CompletableFuture<List<TimescaleTsKvEntity>> findMin(String key, long startTs, long endTs, long timeBucket, UUID entityId) {
        Integer keyId = getOrSaveKeyId(key);
        return aggregationRepository.findMin(
                entityId,
                keyId,
                timeBucket,
                startTs,
                endTs);
    }

    private CompletableFuture<List<TimescaleTsKvEntity>> findMax(String key, long startTs, long endTs, long timeBucket, UUID entityId) {
        Integer keyId = getOrSaveKeyId(key);
        return aggregationRepository.findMax(
                entityId,
                keyId,
                timeBucket,
                startTs,
                endTs);
    }

    private CompletableFuture<List<TimescaleTsKvEntity>> findAvg(String key, long startTs, long endTs, long timeBucket, UUID entityId) {
        Integer keyId = getOrSaveKeyId(key);
        return aggregationRepository.findAvg(
                entityId,
                keyId,
                timeBucket,
                startTs,
                endTs);
    }

}
