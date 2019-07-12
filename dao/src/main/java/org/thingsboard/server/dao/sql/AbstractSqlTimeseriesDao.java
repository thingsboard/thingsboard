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
package org.thingsboard.server.dao.sql;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.springframework.beans.factory.annotation.Value;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.dao.model.sql.AbsractTsKvEntity;
import org.thingsboard.server.dao.timeseries.TsInsertExecutorType;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

public abstract class AbstractSqlTimeseriesDao<T extends AbsractTsKvEntity> extends JpaAbstractDaoListeningExecutorService {

    protected static final String DESC_ORDER = "DESC";

    @Value("${sql.ts_inserts_executor_type}")
    protected String insertExecutorType;

    @Value("${sql.ts_inserts_fixed_thread_pool_size}")
    protected int insertFixedThreadPoolSize;

    protected ListeningExecutorService insertService;

    @PostConstruct
    void init() {
        Optional<TsInsertExecutorType> executorTypeOptional = TsInsertExecutorType.parse(insertExecutorType);
        TsInsertExecutorType executorType;
        executorType = executorTypeOptional.orElse(TsInsertExecutorType.FIXED);
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

    @PreDestroy
    void preDestroy() {
        if (insertService != null) {
            insertService.shutdown();
        }
    }

    protected abstract ListenableFuture<Optional<TsKvEntry>> findAndAggregateAsync(TenantId tenantId, EntityId entityId, String key, long startTs, long endTs, long ts, Aggregation aggregation);

    protected void switchAgregation(EntityId entityId, String key, long startTs, long endTs, Aggregation aggregation, List<CompletableFuture<T>> entitiesFutures, String entityIdStr, String tenantIdStr) {
        switch (aggregation) {
            case AVG:
                finfAvg(entityId, key, startTs, endTs, entitiesFutures, entityIdStr, tenantIdStr);
                break;
            case MAX:
                findMax(entityId, key, startTs, endTs, entitiesFutures, entityIdStr, tenantIdStr);
                break;
            case MIN:
                findMin(entityId, key, startTs, endTs, entitiesFutures, entityIdStr, tenantIdStr);
                break;
            case SUM:
                findSum(entityId, key, startTs, endTs, entitiesFutures, entityIdStr, tenantIdStr);
                break;
            case COUNT:
                findCount(entityId, key, startTs, endTs, entitiesFutures, entityIdStr, tenantIdStr);
                break;
            default:
                throw new IllegalArgumentException("Not supported aggregation type: " + aggregation);
        }
    }

    protected abstract void finfAvg(EntityId entityId, String key, long startTs, long endTs, List<CompletableFuture<T>> entitiesFutures, String entityIdStr, String tenantIdStr);

    protected abstract void findMax(EntityId entityId, String key, long startTs, long endTs, List<CompletableFuture<T>> entitiesFutures, String entityIdStr, String tenantIdStr);

    protected abstract void findMin(EntityId entityId, String key, long startTs, long endTs, List<CompletableFuture<T>> entitiesFutures, String entityIdStr, String tenantIdStr);

    protected abstract void findSum(EntityId entityId, String key, long startTs, long endTs, List<CompletableFuture<T>> entitiesFutures, String entityIdStr, String tenantIdStr);

    protected abstract void findCount(EntityId entityId, String key, long startTs, long endTs, List<CompletableFuture<T>> entitiesFutures, String entityIdStr, String tenantIdStr);
}