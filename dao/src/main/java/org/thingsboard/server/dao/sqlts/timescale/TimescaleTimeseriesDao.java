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
package org.thingsboard.server.dao.sqlts.timescale;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
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
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sqlts.dictionary.TsKvDictionary;
import org.thingsboard.server.dao.model.sqlts.dictionary.TsKvDictionaryCompositeKey;
import org.thingsboard.server.dao.model.sqlts.timescale.TimescaleTsKvEntity;
import org.thingsboard.server.dao.sql.TbSqlBlockingQueue;
import org.thingsboard.server.dao.sql.TbSqlBlockingQueueParams;
import org.thingsboard.server.dao.sqlts.AbstractSqlTimeseriesDao;
import org.thingsboard.server.dao.sqlts.EntityContainer;
import org.thingsboard.server.dao.sqlts.InsertTsRepository;
import org.thingsboard.server.dao.sqlts.dictionary.TsKvDictionaryRepository;
import org.thingsboard.server.dao.timeseries.TimeseriesDao;
import org.thingsboard.server.dao.util.TimescaleDBTsDao;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;


@Component
@Slf4j
@TimescaleDBTsDao
public class TimescaleTimeseriesDao extends AbstractSqlTimeseriesDao implements TimeseriesDao {

    private static final String TS = "ts";

    private final ConcurrentMap<String, Integer> tsKvDictionaryMap = new ConcurrentHashMap<>();

    private static final ReentrantLock tsCreationLock = new ReentrantLock();

    @Autowired
    private TsKvDictionaryRepository dictionaryRepository;

    @Autowired
    private TsKvTimescaleRepository tsKvRepository;

    @Autowired
    private AggregationRepository aggregationRepository;

    @Autowired
    private InsertTsRepository<TimescaleTsKvEntity> insertRepository;

    @Value("${sql.ts_timescale.batch_size:1000}")
    private int batchSize;

    @Value("${sql.ts_timescale.batch_max_delay:100}")
    private long maxDelay;

    @Value("${sql.ts_timescale.stats_print_interval_ms:1000}")
    private long statsPrintIntervalMs;

    private TbSqlBlockingQueue<EntityContainer<TimescaleTsKvEntity>> queue;

    @PostConstruct
    protected void init() {
        super.init();
        TbSqlBlockingQueueParams params = TbSqlBlockingQueueParams.builder()
                .logName("TS Timescale")
                .batchSize(batchSize)
                .maxDelay(maxDelay)
                .statsPrintIntervalMs(statsPrintIntervalMs)
                .build();
        queue = new TbSqlBlockingQueue<>(params);
        queue.init(logExecutor, v -> insertRepository.saveOrUpdate(v));
    }

    @PreDestroy
    protected void destroy() {
        super.init();
        if (queue != null) {
            queue.destroy();
        }
    }

    protected ListenableFuture<List<TsKvEntry>> findAllAsync(TenantId tenantId, EntityId entityId, ReadTsKvQuery query) {
        if (query.getAggregation() == Aggregation.NONE) {
            return findAllAsyncWithLimit(tenantId, entityId, query);
        } else {
            long startTs = query.getStartTs();
            long endTs = query.getEndTs();
            long timeBucket = query.getInterval();
            ListenableFuture<List<Optional<TsKvEntry>>> future = findAndAggregateAsync(tenantId, entityId, query.getKey(), startTs, endTs, timeBucket, query.getAggregation());
            return getTskvEntriesFuture(future);
        }
    }

    @Override
    public ListenableFuture<List<TsKvEntry>> findAllAsync(TenantId tenantId, EntityId entityId, List<ReadTsKvQuery> queries) {
        return processFindAllAsync(tenantId, entityId, queries);
    }

    @Override
    public ListenableFuture<TsKvEntry> findLatest(TenantId tenantId, EntityId entityId, String key) {
        return getFindLatestFuture(entityId, key);
    }

    @Override
    public ListenableFuture<List<TsKvEntry>> findAllLatest(TenantId tenantId, EntityId entityId) {
        return getFindAllLatestFuture(entityId);
    }

    @Override
    public ListenableFuture<Void> save(TenantId tenantId, EntityId entityId, TsKvEntry tsKvEntry, long ttl) {
        String strKey = tsKvEntry.getKey();
        Integer keyId = getOrSaveKeyId(strKey);
        TimescaleTsKvEntity entity = new TimescaleTsKvEntity();
        entity.setTenantId(tenantId.getId());
        entity.setEntityId(entityId.getId());
        entity.setTs(tsKvEntry.getTs());
        entity.setKey(keyId);
        entity.setStrValue(tsKvEntry.getStrValue().orElse(null));
        entity.setDoubleValue(tsKvEntry.getDoubleValue().orElse(null));
        entity.setLongValue(tsKvEntry.getLongValue().orElse(null));
        entity.setBooleanValue(tsKvEntry.getBooleanValue().orElse(null));
        log.trace("Saving entity to timescale db: {}", entity);
        return queue.add(new EntityContainer(entity, null));
    }

    @Override
    public ListenableFuture<Void> savePartition(TenantId tenantId, EntityId entityId, long tsKvEntryTs, String key, long ttl) {
        return Futures.immediateFuture(null);
    }

    @Override
    public ListenableFuture<Void> saveLatest(TenantId tenantId, EntityId entityId, TsKvEntry tsKvEntry) {
        return getSaveLatestFuture(entityId, tsKvEntry);
    }

    @Override
    public ListenableFuture<Void> remove(TenantId tenantId, EntityId entityId, DeleteTsKvQuery query) {
        String strKey = query.getKey();
        Integer keyId = getOrSaveKeyId(strKey);
        return service.submit(() -> {
            tsKvRepository.delete(
                    tenantId.getId(),
                    entityId.getId(),
                    keyId,
                    query.getStartTs(),
                    query.getEndTs());
            return null;
        });
    }

    @Override
    public ListenableFuture<Void> removeLatest(TenantId tenantId, EntityId entityId, DeleteTsKvQuery query) {
        return getRemoveLatestFuture(tenantId, entityId, query);
    }

    @Override
    public ListenableFuture<Void> removePartition(TenantId tenantId, EntityId entityId, DeleteTsKvQuery query) {
        return service.submit(() -> null);
    }

    private ListenableFuture<List<TsKvEntry>> findAllAsyncWithLimit(TenantId tenantId, EntityId entityId, ReadTsKvQuery query) {
        String strKey = query.getKey();
        Integer keyId = getOrSaveKeyId(strKey);
        List<TimescaleTsKvEntity> timescaleTsKvEntities = tsKvRepository.findAllWithLimit(
                tenantId.getId(),
                entityId.getId(),
                keyId,
                query.getStartTs(),
                query.getEndTs(),
                new PageRequest(0, query.getLimit(),
                        new Sort(Sort.Direction.fromString(
                                query.getOrderBy()), TS)));
        timescaleTsKvEntities.forEach(tsKvEntity -> tsKvEntity.setStrKey(strKey));
        return Futures.immediateFuture(DaoUtil.convertDataList(timescaleTsKvEntities));
    }

    private Integer getOrSaveKeyId(String strKey) {
        Integer keyId = tsKvDictionaryMap.get(strKey);
        if (keyId == null) {
            Optional<TsKvDictionary> tsKvDictionaryOptional;
            tsKvDictionaryOptional = dictionaryRepository.findById(new TsKvDictionaryCompositeKey(strKey));
            if (!tsKvDictionaryOptional.isPresent()) {
                tsCreationLock.lock();
                try {
                    tsKvDictionaryOptional = dictionaryRepository.findById(new TsKvDictionaryCompositeKey(strKey));
                    if (!tsKvDictionaryOptional.isPresent()) {
                        TsKvDictionary tsKvDictionary = new TsKvDictionary();
                        tsKvDictionary.setKey(strKey);
                        try {
                            TsKvDictionary saved = dictionaryRepository.save(tsKvDictionary);
                            tsKvDictionaryMap.put(saved.getKey(), saved.getKeyId());
                            keyId = saved.getKeyId();
                        } catch (ConstraintViolationException e) {
                            tsKvDictionaryOptional = dictionaryRepository.findById(new TsKvDictionaryCompositeKey(strKey));
                            TsKvDictionary dictionary = tsKvDictionaryOptional.orElseThrow(() -> new RuntimeException("Failed to get TsKvDictionary entity from DB!"));
                            tsKvDictionaryMap.put(dictionary.getKey(), dictionary.getKeyId());
                            keyId = dictionary.getKeyId();
                        }
                    } else {
                        keyId = tsKvDictionaryOptional.get().getKeyId();
                    }
                } finally {
                    tsCreationLock.unlock();
                }
            } else {
                keyId = tsKvDictionaryOptional.get().getKeyId();
                tsKvDictionaryMap.put(strKey, keyId);
            }
        }
        return keyId;
    }

    private ListenableFuture<List<Optional<TsKvEntry>>> findAndAggregateAsync(TenantId tenantId, EntityId entityId, String key, long startTs, long endTs, long timeBucket, Aggregation aggregation) {
        CompletableFuture<List<TimescaleTsKvEntity>> listCompletableFuture = switchAgregation(key, startTs, endTs, timeBucket, aggregation, entityId.getId(), tenantId.getId());
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
                        entity.setTenantId(tenantId.getId());
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
        });
    }

    private CompletableFuture<List<TimescaleTsKvEntity>> switchAgregation(String key, long startTs, long endTs, long timeBucket, Aggregation aggregation, UUID entityId, UUID tenantId) {
        switch (aggregation) {
            case AVG:
                return findAvg(key, startTs, endTs, timeBucket, entityId, tenantId);
            case MAX:
                return findMax(key, startTs, endTs, timeBucket, entityId, tenantId);
            case MIN:
                return findMin(key, startTs, endTs, timeBucket, entityId, tenantId);
            case SUM:
                return findSum(key, startTs, endTs, timeBucket, entityId, tenantId);
            case COUNT:
                return findCount(key, startTs, endTs, timeBucket, entityId, tenantId);
            default:
                throw new IllegalArgumentException("Not supported aggregation type: " + aggregation);
        }
    }

    private CompletableFuture<List<TimescaleTsKvEntity>> findAvg(String key, long startTs, long endTs, long timeBucket, UUID entityId, UUID tenantId) {
        Integer keyId = getOrSaveKeyId(key);
        return aggregationRepository.findAvg(
                tenantId,
                entityId,
                keyId,
                timeBucket,
                startTs,
                endTs);
    }

    private CompletableFuture<List<TimescaleTsKvEntity>> findMax(String key, long startTs, long endTs, long timeBucket, UUID entityId, UUID tenantId) {
        Integer keyId = getOrSaveKeyId(key);
        return aggregationRepository.findMax(
                tenantId,
                entityId,
                keyId,
                timeBucket,
                startTs,
                endTs);
    }

    private CompletableFuture<List<TimescaleTsKvEntity>> findMin(String key, long startTs, long endTs, long timeBucket, UUID entityId, UUID tenantId) {
        Integer keyId = getOrSaveKeyId(key);
        return aggregationRepository.findMin(
                tenantId,
                entityId,
                keyId,
                timeBucket,
                startTs,
                endTs);

    }

    private CompletableFuture<List<TimescaleTsKvEntity>> findSum(String key, long startTs, long endTs, long timeBucket, UUID entityId, UUID tenantId) {
        Integer keyId = getOrSaveKeyId(key);
        return aggregationRepository.findSum(
                tenantId,
                entityId,
                keyId,
                timeBucket,
                startTs,
                endTs);
    }

    private CompletableFuture<List<TimescaleTsKvEntity>> findCount(String key, long startTs, long endTs, long timeBucket, UUID entityId, UUID tenantId) {
        Integer keyId = getOrSaveKeyId(key);
        return aggregationRepository.findCount(
                tenantId,
                entityId,
                keyId,
                timeBucket,
                startTs,
                endTs);
    }
}