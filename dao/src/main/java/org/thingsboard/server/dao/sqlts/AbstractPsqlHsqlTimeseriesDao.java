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
package org.thingsboard.server.dao.sqlts;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.dao.model.sql.AbstractTsKvEntity;
import org.thingsboard.server.dao.sql.TbSqlBlockingQueue;
import org.thingsboard.server.dao.sql.TbSqlBlockingQueueParams;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
public abstract class AbstractPsqlHsqlTimeseriesDao<T extends AbstractTsKvEntity> extends AbstractSqlTimeseriesDao {

    @Autowired
    protected InsertTsRepository<T> insertRepository;

    protected TbSqlBlockingQueue<EntityContainer<T>> tsQueue;

    @PostConstruct
    protected void init() {
        super.init();
        TbSqlBlockingQueueParams tsParams = TbSqlBlockingQueueParams.builder()
                .logName("TS")
                .batchSize(tsBatchSize)
                .maxDelay(tsMaxDelay)
                .statsPrintIntervalMs(tsStatsPrintIntervalMs)
                .build();
        tsQueue = new TbSqlBlockingQueue<>(tsParams);
        tsQueue.init(logExecutor, v -> insertRepository.saveOrUpdate(v));
    }

    @PreDestroy
    protected void destroy() {
        super.destroy();
        if (tsQueue != null) {
            tsQueue.destroy();
        }
    }

    protected abstract ListenableFuture<Optional<TsKvEntry>> findAndAggregateAsync(TenantId tenantId, EntityId entityId, String key, long startTs, long endTs, long ts, Aggregation aggregation);

    protected void switchAgregation(TenantId tenantId, EntityId entityId, String key, long startTs, long endTs, Aggregation aggregation, List<CompletableFuture<T>> entitiesFutures) {
        switch (aggregation) {
            case AVG:
                findAvg(tenantId, entityId, key, startTs, endTs, entitiesFutures);
                break;
            case MAX:
                findMax(tenantId, entityId, key, startTs, endTs, entitiesFutures);
                break;
            case MIN:
                findMin(tenantId, entityId, key, startTs, endTs, entitiesFutures);
                break;
            case SUM:
                findSum(tenantId, entityId, key, startTs, endTs, entitiesFutures);
                break;
            case COUNT:
                findCount(tenantId, entityId, key, startTs, endTs, entitiesFutures);
                break;
            default:
                throw new IllegalArgumentException("Not supported aggregation type: " + aggregation);
        }
    }

    protected abstract void findCount(TenantId tenantId, EntityId entityId, String key, long startTs, long endTs, List<CompletableFuture<T>> entitiesFutures);

    protected abstract void findSum(TenantId tenantId, EntityId entityId, String key, long startTs, long endTs, List<CompletableFuture<T>> entitiesFutures);

    protected abstract void findMin(TenantId tenantId, EntityId entityId, String key, long startTs, long endTs, List<CompletableFuture<T>> entitiesFutures);

    protected abstract void findMax(TenantId tenantId, EntityId entityId, String key, long startTs, long endTs, List<CompletableFuture<T>> entitiesFutures);

    protected abstract void findAvg(TenantId tenantId, EntityId entityId, String key, long startTs, long endTs, List<CompletableFuture<T>> entitiesFutures);

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
}
