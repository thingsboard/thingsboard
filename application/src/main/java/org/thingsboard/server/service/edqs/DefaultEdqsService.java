/**
 * Copyright © 2016-2024 The Thingsboard Authors
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

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
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
import org.springframework.beans.factory.annotation.Value;
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
import org.thingsboard.server.common.data.edqs.query.EdqsRequest;
import org.thingsboard.server.common.data.edqs.query.EdqsResponse;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.JsonDataEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.msg.edqs.EdqsService;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.edqs.processor.EdqsConverter;
import org.thingsboard.server.edqs.processor.EdqsProducer;
import org.thingsboard.server.edqs.util.EdqsPartitionService;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.EdqsEventMsg;
import org.thingsboard.server.gen.transport.TransportProtos.EdqsRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.FromEdqsMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToCoreNotificationMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToEdqsCoreServiceMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToEdqsMsg;
import org.thingsboard.server.queue.TbQueueRequestTemplate;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.HashPartitionService;
import org.thingsboard.server.queue.edqs.EdqsQueue;
import org.thingsboard.server.queue.environment.DistributedLock;
import org.thingsboard.server.queue.environment.DistributedLockService;
import org.thingsboard.server.queue.provider.EdqsClientQueueFactory;
import org.thingsboard.server.queue.util.AfterStartUp;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(value = "queue.edqs.sync_enabled", havingValue = "true")
public class DefaultEdqsService implements EdqsService {

    private final EdqsClientQueueFactory queueFactory;
    private final EdqsConverter edqsConverter;
    private final EdqsSyncService edqsSyncService;
    private final DistributedLockService distributedLockService;
    private final AttributesService attributesService;
    private final EdqsPartitionService edqsPartitionService;
    @Autowired @Lazy
    private TbClusterService clusterService;
    @Autowired @Lazy
    private HashPartitionService hashPartitionService;

    @Value("${queue.edqs.api_enabled:false}")
    private Boolean apiEnabled;

    private EdqsProducer eventsProducer;
    private TbQueueRequestTemplate<TbProtoQueueMsg<ToEdqsMsg>, TbProtoQueueMsg<FromEdqsMsg>> requestTemplate;
    private ExecutorService executor;
    private DistributedLock<EdqsSyncState> syncLock;

    @PostConstruct
    private void init() {
        executor = ThingsBoardExecutors.newWorkStealingPool(12, getClass());
        eventsProducer = EdqsProducer.builder()
                .queue(EdqsQueue.EVENTS)
                .partitionService(edqsPartitionService)
                .producer(queueFactory.createEdqsMsgProducer(EdqsQueue.EVENTS))
                .build();
        if (apiEnabled) {
            apiEnabled = null;
        }

        requestTemplate = queueFactory.createEdqsRequestTemplate();
        requestTemplate.init();
        syncLock = distributedLockService.getLock("edqs_sync");
    }

    @AfterStartUp(order = AfterStartUp.REGULAR_SERVICE)
    public void onStartUp() {
        executor.submit(() -> {
            try {
                EdqsSyncState syncState = getSyncState();
                if (edqsSyncService.isSyncNeeded() || syncState == null || syncState.getStatus() != EdqsSyncStatus.FINISHED) {
                    if (hashPartitionService.isSystemPartitionMine(ServiceType.TB_CORE)) {
                        processSystemRequest(ToCoreEdqsRequest.builder()
                                .syncRequest(new EdqsSyncRequest())
                                .build());
                    }
                } else { // only if topic/RocksDB is not empty and sync is finished
                    if (apiEnabled == null) {
                        log.info("EDQS is already synced, enabling API");
                        apiEnabled = true;
                    } else {
                        log.info("EDQS is already synced");
                    }
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
                    apiEnabled = msg.getApiEnabled();
                }

                if (msg.getSyncRequest() != null) {
                    syncLock.lock();
                    try {
                        EdqsSyncState syncState = getSyncState();
                        if (syncState != null && syncState.getStatus() == EdqsSyncStatus.FINISHED) {
                            log.info("EDQS sync is already finished");
                            return;
                        }

                        saveSyncState(EdqsSyncStatus.STARTED);
                        edqsSyncService.sync();

                        saveSyncState(EdqsSyncStatus.FINISHED);
                        if (apiEnabled == null) {
                            broadcast(ToCoreEdqsMsg.builder()
                                    .apiEnabled(Boolean.TRUE)
                                    .build());
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

    @Override
    public ListenableFuture<EdqsResponse> processRequest(TenantId tenantId, CustomerId customerId, EdqsRequest request) {
        var requestMsg = newEdqsMsg(tenantId)
                .setRequestMsg(EdqsRequestMsg.newBuilder()
                        .setValue(JacksonUtil.toString(request))
                        .build());
        if (customerId != null && !customerId.isNullUid()) {
            requestMsg.setCustomerIdMSB(customerId.getId().getMostSignificantBits());
            requestMsg.setCustomerIdLSB(customerId.getId().getLeastSignificantBits());
        }

        Integer partition = edqsPartitionService.resolvePartition(tenantId);
        ListenableFuture<TbProtoQueueMsg<FromEdqsMsg>> resultFuture = requestTemplate.send(new TbProtoQueueMsg<>(UUID.randomUUID(), requestMsg.build()), partition);
        return Futures.transform(resultFuture, msg -> {
            TransportProtos.EdqsResponseMsg responseMsg = msg.getValue().getResponseMsg();
            return JacksonUtil.fromString(responseMsg.getValue(), EdqsResponse.class);
        }, MoreExecutors.directExecutor());
    }

    @Override
    public boolean isApiEnabled() {
        return Boolean.TRUE.equals(apiEnabled);
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
                eventsProducer.send(tenantId, objectType, key, newEdqsMsg(tenantId)
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

    private static ToEdqsMsg.Builder newEdqsMsg(TenantId tenantId) {
        return ToEdqsMsg.newBuilder()
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .setTs(System.currentTimeMillis());
    }

    @PreDestroy
    private void preDestroy() {
        executor.shutdown();
        eventsProducer.stop();
        requestTemplate.stop();
    }

    @SneakyThrows
    private EdqsSyncState getSyncState() {
        EdqsSyncState state = attributesService.find(TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID, AttributeScope.SERVER_SCOPE, "edqsSyncState").get(30, TimeUnit.SECONDS)
                .flatMap(KvEntry::getJsonValue)
                .map(value -> JacksonUtil.fromString(value, EdqsSyncState.class))
                .orElse(null);
        log.info("getSyncState = {}", state);
        return state;
    }

    @SneakyThrows
    private void saveSyncState(EdqsSyncStatus status) {
        EdqsSyncState state = new EdqsSyncState(status);
        log.info("saveSyncState {}", state);
        attributesService.save(TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID, AttributeScope.SERVER_SCOPE, new BaseAttributeKvEntry(
                new JsonDataEntry("edqsSyncState", JacksonUtil.toString(state)),
                System.currentTimeMillis())).get(30, TimeUnit.SECONDS);
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
