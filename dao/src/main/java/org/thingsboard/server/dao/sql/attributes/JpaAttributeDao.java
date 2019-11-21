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
package org.thingsboard.server.dao.sql.attributes;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.UUIDConverter;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.attributes.AttributesDao;
import org.thingsboard.server.dao.model.sql.AttributeKvCompositeKey;
import org.thingsboard.server.dao.model.sql.AttributeKvEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDaoListeningExecutorService;
import org.thingsboard.server.dao.util.SqlDao;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.thingsboard.server.common.data.UUIDConverter.fromTimeUUID;

@Component
@Slf4j
@SqlDao
public class JpaAttributeDao extends JpaAbstractDaoListeningExecutorService implements AttributesDao {

    @Autowired
    private AttributeKvRepository attributeKvRepository;

    @Autowired
    private AttributeKvInsertRepository attributeKvInsertRepository;

    private final BlockingQueue<AttributeKvEntityFutureWrapper> queue = new LinkedBlockingQueue<>();

    @Value("${sql.attributes.batch_size:1000}")
    private int batchSize;

    @Value("${sql.attributes.batch_max_delay:100}")
    private long maxDelay;

    @Value("${sql.attributes.stats_print_interval_ms:1000}")
    private long statsPrintIntervalMs;

    private final AtomicInteger addedCount = new AtomicInteger();
    private final AtomicInteger savedCount = new AtomicInteger();
    private final AtomicInteger failedCount = new AtomicInteger();

    private ExecutorService executor;
    private ScheduledExecutorService schedulerLogExecutor;

    @PostConstruct
    private void init() {
        executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            List<AttributeKvEntityFutureWrapper> entities = new ArrayList<>(batchSize);
            while (true) {
                try {
                    long currentTs = System.currentTimeMillis();
                    AttributeKvEntityFutureWrapper attr = queue.poll(maxDelay, TimeUnit.MILLISECONDS);
                    if (attr == null) {
                        continue;
                    }
                    queue.drainTo(entities, batchSize - 1);
                    boolean fullPack = entities.size() == batchSize;
                    log.debug("Going to save {} attributes", entities.size());
                    attributeKvInsertRepository.saveOrUpdate(entities.stream().map(AttributeKvEntityFutureWrapper::getEntity).collect(Collectors.toList()));
                    entities.forEach(v -> v.getFuture().set(null));
                    savedCount.addAndGet(entities.size());
                    if (!fullPack) {
                        long remainingDelay = maxDelay - (System.currentTimeMillis() - currentTs);
                        if (remainingDelay > 0) {
                            Thread.sleep(remainingDelay);
                        }
                    }
                } catch (Exception e) {
                    failedCount.addAndGet(entities.size());
                    entities.forEach(entityFutureWrapper -> entityFutureWrapper.getFuture().setException(e));
                    log.error("Failed to save {} attributes", entities.size(), e);
                } finally {
                    entities.clear();
                }
            }
        });
        schedulerLogExecutor = Executors.newSingleThreadScheduledExecutor();
        schedulerLogExecutor.scheduleAtFixedRate(() -> {
            log.info("Attributes queueSize [{}] totalAdded [{}] totalSaved [{}] totalFailed [{}]",
                    queue.size(), addedCount.getAndSet(0), savedCount.getAndSet(0), failedCount.getAndSet(0));
        }, statsPrintIntervalMs, statsPrintIntervalMs, TimeUnit.MILLISECONDS);
    }

    @PreDestroy
    private void destroy() {
        if (executor != null) {
            executor.shutdownNow();
        }
        if (schedulerLogExecutor != null) {
            schedulerLogExecutor.shutdownNow();
        }
    }

    @Override
    public ListenableFuture<Optional<AttributeKvEntry>> find(TenantId tenantId, EntityId entityId, String attributeType, String attributeKey) {
        AttributeKvCompositeKey compositeKey =
                getAttributeKvCompositeKey(entityId, attributeType, attributeKey);
        return Futures.immediateFuture(
                Optional.ofNullable(DaoUtil.getData(attributeKvRepository.findById(compositeKey))));
    }

    @Override
    public ListenableFuture<List<AttributeKvEntry>> find(TenantId tenantId, EntityId entityId, String attributeType, Collection<String> attributeKeys) {
        List<AttributeKvCompositeKey> compositeKeys =
                attributeKeys
                        .stream()
                        .map(attributeKey ->
                                getAttributeKvCompositeKey(entityId, attributeType, attributeKey))
                        .collect(Collectors.toList());
        return Futures.immediateFuture(
                DaoUtil.convertDataList(Lists.newArrayList(attributeKvRepository.findAllById(compositeKeys))));
    }

    @Override
    public ListenableFuture<List<AttributeKvEntry>> findAll(TenantId tenantId, EntityId entityId, String attributeType) {
        return Futures.immediateFuture(
                DaoUtil.convertDataList(Lists.newArrayList(
                        attributeKvRepository.findAllByEntityTypeAndEntityIdAndAttributeType(
                                entityId.getEntityType(),
                                UUIDConverter.fromTimeUUID(entityId.getId()),
                                attributeType))));
    }

    @Override
    public ListenableFuture<Void> save(TenantId tenantId, EntityId entityId, String attributeType, AttributeKvEntry attribute) {
        AttributeKvEntity entity = new AttributeKvEntity();
        entity.setId(new AttributeKvCompositeKey(entityId.getEntityType(), fromTimeUUID(entityId.getId()), attributeType, attribute.getKey()));
        entity.setLastUpdateTs(attribute.getLastUpdateTs());
        entity.setStrValue(attribute.getStrValue().orElse(null));
        entity.setDoubleValue(attribute.getDoubleValue().orElse(null));
        entity.setLongValue(attribute.getLongValue().orElse(null));
        entity.setBooleanValue(attribute.getBooleanValue().orElse(null));
        return addToQueue(entity);
    }

    private ListenableFuture<Void> addToQueue(AttributeKvEntity entity) {
        //TODO: add wrapper entity + SettableFuture. Invoke SettableFuture when you process all messages.
        SettableFuture<Void> future = SettableFuture.create();
        queue.add(AttributeKvEntityFutureWrapper.create(future, entity));
        addedCount.incrementAndGet();
        return future;
    }

    @Override
    public ListenableFuture<List<Void>> removeAll(TenantId tenantId, EntityId entityId, String attributeType, List<String> keys) {
        List<AttributeKvEntity> entitiesToDelete = keys
                .stream()
                .map(key -> {
                    AttributeKvEntity entityToDelete = new AttributeKvEntity();
                    entityToDelete.setId(new AttributeKvCompositeKey(entityId.getEntityType(), fromTimeUUID(entityId.getId()), attributeType, key));
                    return entityToDelete;
                }).collect(Collectors.toList());

        return service.submit(() -> {
            attributeKvRepository.deleteAll(entitiesToDelete);
            return null;
        });
    }

    private AttributeKvCompositeKey getAttributeKvCompositeKey(EntityId entityId, String attributeType, String attributeKey) {
        return new AttributeKvCompositeKey(
                entityId.getEntityType(),
                fromTimeUUID(entityId.getId()),
                attributeType,
                attributeKey);
    }
}
