/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.server.service.edqs;

import com.google.protobuf.ByteString;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ObjectType;
import org.thingsboard.server.common.data.edqs.EdqsEventType;
import org.thingsboard.server.common.data.edqs.EdqsObject;
import org.thingsboard.server.common.data.edqs.EdqsSyncRequest;
import org.thingsboard.server.common.data.edqs.Entity;
import org.thingsboard.server.common.data.edqs.ToCoreEdqsMsg;
import org.thingsboard.server.common.data.edqs.ToCoreEdqsRequest;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.JsonDataEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.msg.edqs.EdqsApiService;
import org.thingsboard.server.common.msg.edqs.EdqsService;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.edqs.processor.EdqsProducer;
import org.thingsboard.server.edqs.state.EdqsPartitionService;
import org.thingsboard.server.edqs.util.EdqsConverter;
import org.thingsboard.server.gen.transport.TransportProtos.EdqsEventMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToCoreNotificationMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToEdqsCoreServiceMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToEdqsMsg;
import org.thingsboard.server.queue.discovery.HashPartitionService;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.queue.discovery.TopicService;
import org.thingsboard.server.queue.environment.DistributedLock;
import org.thingsboard.server.queue.environment.DistributedLockService;
import org.thingsboard.server.queue.provider.EdqsClientQueueFactory;
import org.thingsboard.server.queue.util.AfterStartUp;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(value = "queue.edqs.sync.enabled", havingValue = "true")
public class DefaultEdqsService implements EdqsService {

    private final EdqsClientQueueFactory queueFactory;
    private final EdqsConverter edqsConverter;
    private final EdqsSyncService edqsSyncService;
    private final EdqsApiService edqsApiService;
    private final DistributedLockService distributedLockService;
    private final AttributesService attributesService;
    private final EdqsPartitionService edqsPartitionService;
    private final TopicService topicService;
    private final TbServiceInfoProvider serviceInfoProvider;
    @Autowired @Lazy
    private TbClusterService clusterService;
    @Autowired @Lazy
    private HashPartitionService hashPartitionService;

    private EdqsProducer eventsProducer;
    private ExecutorService executor;
    private DistributedLock syncLock;

    @PostConstruct
    private void init() {
        executor = ThingsBoardExecutors.newWorkStealingPool(12, getClass());
        eventsProducer = EdqsProducer.builder()
                .producer(queueFactory.createEdqsEventsProducer())
                .partitionService(edqsPartitionService)
                .build();
        syncLock = distributedLockService.getLock("edqs_sync");
    }

    @AfterStartUp(order = AfterStartUp.REGULAR_SERVICE)
    public void onStartUp() {
        if (!serviceInfoProvider.isService(ServiceType.TB_CORE)) {
            return;
        }
        executor.submit(() -> {
            try {
                EdqsSyncState syncState = getSyncState();
                if (edqsSyncService.isSyncNeeded() || syncState == null || syncState.getStatus() != EdqsSyncStatus.FINISHED) {
                    if (hashPartitionService.isSystemPartitionMine(ServiceType.TB_CORE)) {
                        processSystemRequest(ToCoreEdqsRequest.builder()
                                .syncRequest(new EdqsSyncRequest())
                                .build());
                    }
                } else if (edqsApiService.isSupported() && edqsApiService.isAutoEnable()) {
                    // only if topic/RocksDB is not empty and sync is finished
                    edqsApiService.setEnabled(true);
                }
            } catch (Throwable e) {
                log.error("Failed to start EDQS service", e);
            }
        });
    }

    @Override
    public void processSystemRequest(ToCoreEdqsRequest request) {
        log.info("Processing system request {}", request);
        if (request.getSyncRequest() != null) {
            saveSyncState(EdqsSyncStatus.REQUESTED);
        }
        broadcast(request.toInternalMsg());
    }

    @Override
    public void processSystemMsg(ToCoreEdqsMsg msg) {
        executor.submit(() -> {
            log.info("Processing system msg {}", msg);
            try {
                if (msg.getApiEnabled() != null) {
                    edqsApiService.setEnabled(msg.getApiEnabled());
                }

                if (msg.getSyncRequest() != null) {
                    syncLock.lock();
                    try {
                        EdqsSyncState syncState = getSyncState();
                        if (syncState != null) {
                            EdqsSyncStatus status = syncState.getStatus();
                            if (status == EdqsSyncStatus.FINISHED || status == EdqsSyncStatus.FAILED) {
                                log.info("EDQS sync is already " + status + ", ignoring the msg");
                                return;
                            }
                        }

                        saveSyncState(EdqsSyncStatus.STARTED);
                        edqsSyncService.sync();
                        saveSyncState(EdqsSyncStatus.FINISHED);

                        if (edqsApiService.isSupported())
                            if (edqsApiService.isAutoEnable()) {
                                log.info("EDQS sync is finished, auto-enabling API");
                                broadcast(ToCoreEdqsMsg.builder()
                                        .apiEnabled(Boolean.TRUE)
                                        .build());
                            } else {
                                log.info("EDQS sync is finished, but leaving API disabled");
                            }
                    } catch (Exception e) {
                        log.error("Failed to complete sync", e);
                        saveSyncState(EdqsSyncStatus.FAILED);
                    } finally {
                        syncLock.unlock();
                    }
                }
            } catch (Throwable e) {
                log.error("Failed to process msg {}", msg, e);
            }
        });
    }

    @Override
    public void onUpdate(TenantId tenantId, EntityId entityId, Object entity) {
        EntityType entityType = entityId.getEntityType();
        ObjectType objectType = ObjectType.fromEntityType(entityType);
        if (!isEdqsType(tenantId, objectType)) {
            log.trace("[{}][{}] Ignoring update event, type {} not supported", tenantId, entityId, entityType);
            return;
        }
        onUpdate(tenantId, objectType, edqsConverter.toEntity(entityType, entity));
    }

    @Override
    public void onUpdate(TenantId tenantId, ObjectType objectType, EdqsObject object) {
        processEvent(tenantId, objectType, EdqsEventType.UPDATED, object);
    }

    @Override
    public void onDelete(TenantId tenantId, EntityId entityId) {
        EntityType entityType = entityId.getEntityType();
        ObjectType objectType = ObjectType.fromEntityType(entityType);
        if (!isEdqsType(tenantId, objectType)) {
            log.trace("[{}][{}] Ignoring deletion event, type {} not supported", tenantId, entityId, entityType);
            return;
        }
        onDelete(tenantId, objectType, new Entity(entityType, entityId.getId(), Long.MAX_VALUE));
    }

    @Override
    public void onDelete(TenantId tenantId, ObjectType objectType, EdqsObject object) {
        processEvent(tenantId, objectType, EdqsEventType.DELETED, object);
    }

    protected void processEvent(TenantId tenantId, ObjectType objectType, EdqsEventType eventType, EdqsObject object) {
        executor.submit(() -> {
            try {
                String key = object.key();
                Long version = object.version();
                EdqsEventMsg.Builder eventMsg = EdqsEventMsg.newBuilder()
                        .setKey(key)
                        .setObjectType(objectType.name())
                        .setData(ByteString.copyFrom(edqsConverter.serialize(objectType, object)))
                        .setEventType(eventType.name());
                if (version != null) {
                    eventMsg.setVersion(version);
                }
                eventsProducer.send(tenantId, objectType, key, ToEdqsMsg.newBuilder()
                        .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                        .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                        .setTs(System.currentTimeMillis())
                        .setEventMsg(eventMsg)
                        .build());
            } catch (Throwable e) {
                log.error("[{}] Failed to push {} event for {} {}", tenantId, eventType, objectType, object, e);
            }
        });
    }

    private boolean isEdqsType(TenantId tenantId, ObjectType objectType) {
        if (objectType == null) {
            return false;
        }
        if (!tenantId.isSysTenantId()) {
            return ObjectType.edqsTypes.contains(objectType);
        } else {
            return ObjectType.edqsSystemTypes.contains(objectType);
        }
    }

    private void broadcast(ToCoreEdqsMsg msg) {
        clusterService.broadcastToCore(ToCoreNotificationMsg.newBuilder()
                .setToEdqsCoreServiceMsg(ToEdqsCoreServiceMsg.newBuilder()
                        .setValue(ByteString.copyFrom(JacksonUtil.writeValueAsBytes(msg))))
                .build());
    }

    @SneakyThrows
    private EdqsSyncState getSyncState() {
        EdqsSyncState state = attributesService.find(TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID, AttributeScope.SERVER_SCOPE, "edqsSyncState").get(30, TimeUnit.SECONDS)
                .flatMap(KvEntry::getJsonValue)
                .map(value -> JacksonUtil.fromString(value, EdqsSyncState.class))
                .orElse(null);
        log.info("EDQS sync state: {}", state);
        return state;
    }

    @SneakyThrows
    private void saveSyncState(EdqsSyncStatus status) {
        EdqsSyncState state = new EdqsSyncState(status);
        log.info("New EDQS sync state: {}", state);
        attributesService.save(TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID, AttributeScope.SERVER_SCOPE, new BaseAttributeKvEntry(
                new JsonDataEntry("edqsSyncState", JacksonUtil.toString(state)),
                System.currentTimeMillis())).get(30, TimeUnit.SECONDS);
    }

    @PreDestroy
    private void stop() {
        executor.shutdown();
        eventsProducer.stop();
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    private static class EdqsSyncState {
        private EdqsSyncStatus status;
    }

    private enum EdqsSyncStatus {
        REQUESTED,
        STARTED,
        FINISHED,
        FAILED
    }

}
