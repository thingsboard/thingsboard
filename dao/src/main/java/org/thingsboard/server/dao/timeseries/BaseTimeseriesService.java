/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.dao.timeseries;

import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.ObjectType;
import org.thingsboard.server.common.data.edqs.LatestTsKv;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.common.data.kv.BaseDeleteTsKvQuery;
import org.thingsboard.server.common.data.kv.BaseReadTsKvQuery;
import org.thingsboard.server.common.data.kv.DeleteTsKvQuery;
import org.thingsboard.server.common.data.kv.ReadTsKvQuery;
import org.thingsboard.server.common.data.kv.ReadTsKvQueryResult;
import org.thingsboard.server.common.data.kv.TimeseriesSaveResult;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.kv.TsKvLatestRemovingResult;
import org.thingsboard.server.common.msg.edqs.EdqsService;
import org.thingsboard.server.dao.entityview.EntityViewService;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.thingsboard.server.dao.service.Validator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.thingsboard.server.common.data.StringUtils.isBlank;

/**
 * @author Andrew Shvayka
 */
@Service
@Slf4j
public class BaseTimeseriesService implements TimeseriesService {

    private static final int INSERTS_PER_ENTRY = 3;
    private static final int INSERTS_PER_ENTRY_WITHOUT_LATEST = 2;
    private static final int DELETES_PER_ENTRY = INSERTS_PER_ENTRY;
    public static final Function<List<Integer>, Integer> SUM_ALL_INTEGERS = new Function<>() {
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

    @Autowired
    private EdqsService edqsService;

    @Override
    public ListenableFuture<List<ReadTsKvQueryResult>> findAllByQueries(TenantId tenantId, EntityId entityId, List<ReadTsKvQuery> queries) {
        validate(entityId);
        queries.forEach(this::validate);
        if (entityId.getEntityType().equals(EntityType.ENTITY_VIEW)) {
            EntityView entityView = entityViewService.findEntityViewById(tenantId, (EntityViewId) entityId);
            List<String> keys = entityView.getKeys() != null && entityView.getKeys().getTimeseries() != null ?
                    entityView.getKeys().getTimeseries() : Collections.emptyList();
            List<ReadTsKvQuery> filteredQueries =
                    queries.stream()
                            .filter(query -> keys.isEmpty() || keys.contains(query.getKey()))
                            .collect(Collectors.toList());
            return timeseriesDao.findAllAsync(tenantId, entityView.getEntityId(), updateQueriesForEntityView(entityView, filteredQueries));
        }
        return timeseriesDao.findAllAsync(tenantId, entityId, queries);
    }

    @Override
    public ListenableFuture<List<TsKvEntry>> findAll(TenantId tenantId, EntityId entityId, List<ReadTsKvQuery> queries) {
        return Futures.transform(findAllByQueries(tenantId, entityId, queries),
                result -> {
                    if (result != null && !result.isEmpty()) {
                        return result.stream().map(ReadTsKvQueryResult::getData).flatMap(Collection::stream).collect(Collectors.toList());
                    }
                    return Collections.emptyList();
                }, MoreExecutors.directExecutor());
    }

    @Override
    public ListenableFuture<Optional<TsKvEntry>> findLatest(TenantId tenantId, EntityId entityId, String key) {
        validate(entityId);
        return timeseriesLatestDao.findLatestOpt(tenantId, entityId, key);
    }

    @Override
    public ListenableFuture<List<TsKvEntry>> findLatest(TenantId tenantId, EntityId entityId, Collection<String> keys) {
        validate(entityId);
        List<ListenableFuture<TsKvEntry>> futures = new ArrayList<>(keys.size());
        keys.forEach(key -> Validator.validateString(key, k -> "Incorrect key " + k));
        for (String key : keys) {
            futures.add(timeseriesLatestDao.findLatest(tenantId, entityId, key));
        }
        return Futures.allAsList(futures);
    }

    @Override
    public ListenableFuture<List<TsKvEntry>> findAllLatest(TenantId tenantId, EntityId entityId) {
        validate(entityId);
        return timeseriesLatestDao.findAllLatest(tenantId, entityId);
    }

    @Override
    public List<String> findAllKeysByDeviceProfileId(TenantId tenantId, DeviceProfileId deviceProfileId) {
        return timeseriesLatestDao.findAllKeysByDeviceProfileId(tenantId, deviceProfileId);
    }

    @Override
    public List<String> findAllKeysByEntityIds(TenantId tenantId, List<EntityId> entityIds) {
        return timeseriesLatestDao.findAllKeysByEntityIds(tenantId, entityIds);
    }

    @Override
    public ListenableFuture<List<String>> findAllKeysByEntityIdsAsync(TenantId tenantId, List<EntityId> entityIds) {
        return timeseriesLatestDao.findAllKeysByEntityIdsAsync(tenantId, entityIds);
    }

    @Override
    public void cleanup(long systemTtl) {
        timeseriesDao.cleanup(systemTtl);
    }

    @Override
    public ListenableFuture<TimeseriesSaveResult> save(TenantId tenantId, EntityId entityId, TsKvEntry tsKvEntry) {
        validate(entityId);
        return doSave(tenantId, entityId, List.of(tsKvEntry), 0L, true, true);
    }

    @Override
    public ListenableFuture<TimeseriesSaveResult> save(TenantId tenantId, EntityId entityId, List<TsKvEntry> tsKvEntries, long ttl) {
        return doSave(tenantId, entityId, tsKvEntries, ttl, true, true);
    }

    @Override
    public ListenableFuture<TimeseriesSaveResult> saveWithoutLatest(TenantId tenantId, EntityId entityId, List<TsKvEntry> tsKvEntries, long ttl) {
        return doSave(tenantId, entityId, tsKvEntries, ttl, false, true);
    }

    @Override
    public ListenableFuture<TimeseriesSaveResult> saveLatest(TenantId tenantId, EntityId entityId, List<TsKvEntry> tsKvEntries) {
        return doSave(tenantId, entityId, tsKvEntries, 0L, true, false);
    }

    private ListenableFuture<TimeseriesSaveResult> doSave(TenantId tenantId, EntityId entityId, List<TsKvEntry> tsKvEntries, long ttl, boolean saveLatest, boolean saveTs) {
        if (saveTs && entityId.getEntityType().equals(EntityType.ENTITY_VIEW)) {
            throw new IncorrectParameterException("Telemetry data can't be stored for entity view. Read only");
        }
        List<ListenableFuture<Integer>> tsFutures = saveTs ? new ArrayList<>(tsKvEntries.size() * INSERTS_PER_ENTRY_WITHOUT_LATEST) : null;
        List<ListenableFuture<Long>> latestFutures = saveLatest ? new ArrayList<>(tsKvEntries.size()) : null;
        for (TsKvEntry tsKvEntry : tsKvEntries) {
            if (saveTs) {
                tsFutures.add(timeseriesDao.savePartition(tenantId, entityId, tsKvEntry.getTs(), tsKvEntry.getKey()));
                tsFutures.add(timeseriesDao.save(tenantId, entityId, tsKvEntry, ttl));
            }
            if (saveLatest) {
                latestFutures.add(Futures.transform(timeseriesLatestDao.saveLatest(tenantId, entityId, tsKvEntry), version -> {
                    if (version != null) {
                        TenantId edqsTenantId = entityId.getEntityType() == EntityType.TENANT ? (TenantId) entityId : tenantId;
                        edqsService.onUpdate(edqsTenantId, ObjectType.LATEST_TS_KV, new LatestTsKv(entityId, tsKvEntry, version));
                    }
                    return version;
                }, MoreExecutors.directExecutor()));
            }
        }
        ListenableFuture<Integer> dpsFuture = saveTs ? Futures.transform(Futures.allAsList(tsFutures), SUM_ALL_INTEGERS, MoreExecutors.directExecutor()) : Futures.immediateFuture(0);
        ListenableFuture<List<Long>> versionsFuture = saveLatest ? Futures.allAsList(latestFutures) : Futures.immediateFuture(null);
        return Futures.whenAllComplete(dpsFuture, versionsFuture).call(() -> {
            Integer dataPoints = dpsFuture.get();
            List<Long> versions = versionsFuture.get();
            return TimeseriesSaveResult.of(dataPoints, versions);
        }, MoreExecutors.directExecutor());
    }

    private List<ReadTsKvQuery> updateQueriesForEntityView(EntityView entityView, List<ReadTsKvQuery> queries) {
        return queries.stream().map(query -> {
            long startTs;
            if (entityView.getStartTimeMs() != 0 && entityView.getStartTimeMs() > query.getStartTs()) {
                startTs = entityView.getStartTimeMs();
            } else {
                startTs = query.getStartTs();
            }

            long endTs;
            if (entityView.getEndTimeMs() != 0 && entityView.getEndTimeMs() < query.getEndTs()) {
                endTs = entityView.getEndTimeMs();
            } else {
                endTs = query.getEndTs();
            }
            return new BaseReadTsKvQuery(query, startTs, endTs);
        }).collect(Collectors.toList());
    }

    @Override
    public ListenableFuture<List<TsKvLatestRemovingResult>> remove(TenantId tenantId, EntityId entityId, List<DeleteTsKvQuery> deleteTsKvQueries) {
        validate(entityId);
        deleteTsKvQueries.forEach(BaseTimeseriesService::validate);
        List<ListenableFuture<TsKvLatestRemovingResult>> futures = new ArrayList<>(deleteTsKvQueries.size() * DELETES_PER_ENTRY);
        for (DeleteTsKvQuery tsKvQuery : deleteTsKvQueries) {
            deleteAndRegisterFutures(tenantId, futures, entityId, tsKvQuery);
        }
        return Futures.allAsList(futures);
    }

    @Override
    public ListenableFuture<List<TsKvLatestRemovingResult>> removeLatest(TenantId tenantId, EntityId entityId, Collection<String> keys) {
        validate(entityId);
        List<ListenableFuture<TsKvLatestRemovingResult>> futures = new ArrayList<>(keys.size());
        for (String key : keys) {
            DeleteTsKvQuery query = new BaseDeleteTsKvQuery(key, 0, System.currentTimeMillis(), false);
            futures.add(doRemove(tenantId, entityId, query));
        }
        return Futures.allAsList(futures);
    }

    @Override
    public ListenableFuture<List<String>> removeAllLatest(TenantId tenantId, EntityId entityId) {
        validate(entityId);
        return Futures.transformAsync(this.findAllLatest(tenantId, entityId), latest -> {
            if (latest != null && !latest.isEmpty()) {
                List<String> keys = latest.stream().map(TsKvEntry::getKey).collect(Collectors.toList());
                return Futures.transform(this.removeLatest(tenantId, entityId, keys), res -> keys, MoreExecutors.directExecutor());
            } else {
                return Futures.immediateFuture(Collections.emptyList());
            }
        }, MoreExecutors.directExecutor());
    }

    private void deleteAndRegisterFutures(TenantId tenantId, List<ListenableFuture<TsKvLatestRemovingResult>> futures, EntityId entityId, DeleteTsKvQuery query) {
        futures.add(Futures.transform(timeseriesDao.remove(tenantId, entityId, query), v -> null, MoreExecutors.directExecutor()));
        if (query.getDeleteLatest()) {
            futures.add(doRemove(tenantId, entityId, query));
        }
    }

    private ListenableFuture<TsKvLatestRemovingResult> doRemove(TenantId tenantId, EntityId entityId, DeleteTsKvQuery query) {
        return Futures.transform(timeseriesLatestDao.removeLatest(tenantId, entityId, query), result -> {
            if (result.isRemoved()) {
                Long version = result.getVersion();
                TenantId edqsTenantId = entityId.getEntityType() == EntityType.TENANT ? (TenantId) entityId : tenantId;
                edqsService.onDelete(edqsTenantId, ObjectType.LATEST_TS_KV, new LatestTsKv(entityId, query.getKey(), version));
            }
            return result;
        }, MoreExecutors.directExecutor());
    }

    private static void validate(EntityId entityId) {
        Validator.validateEntityId(entityId, id -> "Incorrect entityId " + id);
    }

    private void validate(ReadTsKvQuery query) {
        if (query == null) {
            throw new IncorrectParameterException("ReadTsKvQuery can't be null");
        } else if (isBlank(query.getKey())) {
            throw new IncorrectParameterException("Incorrect ReadTsKvQuery. Key can't be empty");
        } else if (query.getAggregation() == null) {
            throw new IncorrectParameterException("Incorrect ReadTsKvQuery. Aggregation can't be empty");
        }
        if (!Aggregation.NONE.equals(query.getAggregation())) {
            long interval = query.getInterval();
            if (interval < 1) {
                throw new IncorrectParameterException("Invalid TsKvQuery: 'interval' must be greater than 0, but got " + interval +
                        ". Please check your query parameters and ensure 'endTs' is greater than 'startTs' or increase 'interval'.");
            }
            long step = Math.max(interval, 1000);
            long intervalCounts = (query.getEndTs() - query.getStartTs()) / step;
            if (intervalCounts > maxTsIntervals || intervalCounts < 0) {
                throw new IncorrectParameterException("Incorrect TsKvQuery. Number of intervals is to high - " + intervalCounts + ". " +
                        "Please increase 'interval' parameter for your query or reduce the time range of the query.");
            }
        }
    }

    private static void validate(DeleteTsKvQuery query) {
        if (query == null) {
            throw new IncorrectParameterException("DeleteTsKvQuery can't be null");
        } else if (isBlank(query.getKey())) {
            throw new IncorrectParameterException("Incorrect DeleteTsKvQuery. Key can't be empty");
        }
    }

}
