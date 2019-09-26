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
package org.thingsboard.server.dao.sqlts.hsql;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.common.data.kv.DeleteTsKvQuery;
import org.thingsboard.server.common.data.kv.ReadTsKvQuery;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sqlts.hsql.TsKvEntity;
import org.thingsboard.server.dao.sqlts.AbstractSqlTimeseriesDao;
import org.thingsboard.server.dao.sqlts.AbstractTimeseriesInsertRepository;
import org.thingsboard.server.dao.timeseries.TimeseriesDao;
import org.thingsboard.server.dao.util.HsqlDao;
import org.thingsboard.server.dao.util.SqlTsDao;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.thingsboard.server.common.data.UUIDConverter.fromTimeUUID;


@Component
@Slf4j
@SqlTsDao
@HsqlDao
public class JpaHsqlTimeseriesDao extends AbstractSqlTimeseriesDao<TsKvEntity> implements TimeseriesDao {

    @Autowired
    private TsKvHsqlRepository tsKvRepository;

    @Autowired
    private AbstractTimeseriesInsertRepository insertRepository;

    @Override
    public ListenableFuture<List<TsKvEntry>> findAllAsync(TenantId tenantId, EntityId entityId, List<ReadTsKvQuery> queries) {
        return processFindAllAsync(tenantId, entityId, queries);
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
    public ListenableFuture<Void> saveLatest(TenantId tenantId, EntityId entityId, TsKvEntry tsKvEntry) {
        return getSaveLatestFuture(entityId, tsKvEntry);
    }

    @Override
    public ListenableFuture<Void> removeLatest(TenantId tenantId, EntityId entityId, DeleteTsKvQuery query) {
        return getRemoveLatestFuture(tenantId, entityId, query);
    }

    @Override
    public ListenableFuture<TsKvEntry> findLatest(TenantId tenantId, EntityId entityId, String key) {
        return getTsKvEntryListenableFuture(entityId, key);
    }

    @Override
    public ListenableFuture<List<TsKvEntry>> findAllLatest(TenantId tenantId, EntityId entityId) {
        return getFindAllLatestFuture(entityId);
    }

    @Override
    public ListenableFuture<Void> savePartition(TenantId tenantId, EntityId entityId, long tsKvEntryTs, String key, long ttl) {
        return insertService.submit(() -> null);
    }

    @Override
    public ListenableFuture<Void> removePartition(TenantId tenantId, EntityId entityId, DeleteTsKvQuery query) {
        return service.submit(() -> null);
    }

    protected ListenableFuture<Optional<TsKvEntry>> findAndAggregateAsync(EntityId entityId, String key, long startTs, long endTs, long ts, Aggregation aggregation) {
        List<CompletableFuture<TsKvEntity>> entitiesFutures = new ArrayList<>();
        String entityIdStr = fromTimeUUID(entityId.getId());
        switchAgregation(entityId, key, startTs, endTs, aggregation, entitiesFutures, entityIdStr);
        return Futures.transform(setFutures(entitiesFutures), entity -> {
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

    protected ListenableFuture<List<TsKvEntry>> findAllAsyncWithLimit(EntityId entityId, ReadTsKvQuery query) {
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

}