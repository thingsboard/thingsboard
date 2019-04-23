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
package org.thingsboard.server.dao.dynamodb.timeseries;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.DeleteTsKvQuery;
import org.thingsboard.server.common.data.kv.ReadTsKvQuery;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.dao.timeseries.TimeseriesDao;
import org.thingsboard.server.dao.util.DynamoDBTsDao;

import javax.annotation.PreDestroy;
import java.util.Collection;
import java.util.List;

import static java.util.stream.Collectors.toList;

@Component
@Slf4j
@DynamoDBTsDao
public class DynamoTimeseriesDaoAsync implements TimeseriesDao {

    private final DynamoTimeseriesDao syncDao;
    private final ListeningExecutorService executor;

    @Autowired
    public DynamoTimeseriesDaoAsync(
            DynamoTimeseriesDao syncDao,
            @Qualifier("DynamoDbExecutor") ListeningExecutorService executor) {
        this.syncDao = syncDao;
        this.executor = executor;
    }

    @PreDestroy
    public void stopExecutor() {
        executor.shutdownNow();
    }

    @Override
    public ListenableFuture<List<TsKvEntry>> findAllAsync(TenantId tenantId, EntityId entityId, List<ReadTsKvQuery> queries) {
        List<ListenableFuture<List<TsKvEntry>>> runningQueries = queries.stream()
                .map(q -> executor.submit(() -> syncDao.findBySingleQuery(entityId, q)
                        .stream()
                        .map(te -> (TsKvEntry) te)
                        .collect(toList())))
                .collect(toList());

        ListenableFuture<List<List<TsKvEntry>>> runningRequest = Futures.allAsList(runningQueries);
        ListenableFuture<List<TsKvEntry>> result =
                Futures.transform(runningRequest, data -> data.stream()
                        .flatMap(Collection::stream).collect(toList()));
        return result;
    }

    @Override
    public ListenableFuture<TsKvEntry> findLatest(TenantId tenantId, EntityId entityId, String key) {
        return executor.submit(() -> syncDao.findLatest(entityId, key));
    }

    @Override
    public ListenableFuture<List<TsKvEntry>> findAllLatest(TenantId tenantId, EntityId entityId) {
        return executor.submit(()-> syncDao.findAllLatest(entityId)
            .stream()
            .map(lv -> (TsKvEntry)lv)
            .collect(toList()));
    }

    @Override
    public ListenableFuture<Void> save(TenantId tenantId, EntityId entityId, TsKvEntry tsKvEntry, long ttl) {
        return (ListenableFuture<Void>) executor.submit(() -> syncDao.save(entityId, tsKvEntry));
    }

    @Override
    public ListenableFuture<Void> savePartition(TenantId tenantId, EntityId entityId, long tsKvEntryTs, String key, long ttl) {
        return Futures.immediateFuture(null);
    }

    @Override
    public ListenableFuture<Void> saveLatest(TenantId tenantId, EntityId entityId, TsKvEntry tsKvEntry) {
        return (ListenableFuture<Void>) executor.submit(() -> syncDao.saveLatest(entityId, tsKvEntry));
    }

    @Override
    public ListenableFuture<Void> remove(TenantId tenantId, EntityId entityId, DeleteTsKvQuery query) {
        return (ListenableFuture<Void>) executor.submit(() -> syncDao.remove(entityId, query));
    }

    @Override
    public ListenableFuture<Void> removeLatest(TenantId tenantId, EntityId entityId, DeleteTsKvQuery query) {
        return (ListenableFuture<Void>) executor.submit(() -> syncDao.removeLatest(entityId, query));
    }

    @Override
    public ListenableFuture<Void> removePartition(TenantId tenantId, EntityId entityId, DeleteTsKvQuery query) {
        return Futures.immediateFuture(null);
    }
}
