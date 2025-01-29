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
package org.thingsboard.server.service.queue;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.DonAsynchron;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.actors.calculatedField.CalculatedFieldLinkedTelemetryMsg;
import org.thingsboard.server.actors.calculatedField.CalculatedFieldTelemetryMsg;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.queue.QueueConfig;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.common.util.ProtoUtils;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.gen.transport.TransportProtos.CalculatedFieldEntityUpdateMsgProto;
import org.thingsboard.server.gen.transport.TransportProtos.CalculatedFieldLinkedTelemetryMsgProto;
import org.thingsboard.server.gen.transport.TransportProtos.CalculatedFieldTelemetryMsgProto;
import org.thingsboard.server.gen.transport.TransportProtos.ComponentLifecycleMsgProto;
import org.thingsboard.server.gen.transport.TransportProtos.ToCalculatedFieldMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToCalculatedFieldNotificationMsg;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.QueueKey;
import org.thingsboard.server.queue.discovery.event.PartitionChangeEvent;
import org.thingsboard.server.queue.provider.TbRuleEngineQueueFactory;
import org.thingsboard.server.queue.util.TbRuleEngineComponent;
import org.thingsboard.server.service.apiusage.TbApiUsageStateService;
import org.thingsboard.server.service.cf.CalculatedFieldCache;
import org.thingsboard.server.service.cf.CalculatedFieldExecutionService;
import org.thingsboard.server.service.profile.TbAssetProfileCache;
import org.thingsboard.server.service.profile.TbDeviceProfileCache;
import org.thingsboard.server.service.queue.consumer.MainQueueConsumerManager;
import org.thingsboard.server.service.queue.processing.AbstractConsumerService;
import org.thingsboard.server.service.queue.processing.IdMsgPair;
import org.thingsboard.server.service.security.auth.jwt.settings.JwtSettingsService;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@TbRuleEngineComponent
@Slf4j
public class DefaultTbCalculatedFieldConsumerService extends AbstractConsumerService<ToCalculatedFieldNotificationMsg> implements TbCalculatedFieldConsumerService {

    @Value("${queue.calculated_fields.poll_interval:25}")
    private long pollInterval;
    @Value("${queue.calculated_fields.pack_processing_timeout:60000}")
    private long packProcessingTimeout;
    @Value("${queue.calculated_fields.consumer_per_partition:true}")
    private boolean consumerPerPartition;
    @Value("${queue.calculated_fields.pool_size:8}")
    private int poolSize;

    private final TbRuleEngineQueueFactory queueFactory;

    private final CalculatedFieldExecutionService calculatedFieldExecutionService;

    private MainQueueConsumerManager<TbProtoQueueMsg<ToCalculatedFieldMsg>, CalculatedFieldQueueConfig> mainConsumer;

    private volatile ListeningExecutorService calculatedFieldsExecutor;

    public DefaultTbCalculatedFieldConsumerService(TbRuleEngineQueueFactory tbQueueFactory,
                                                   ActorSystemContext actorContext,
                                                   TbDeviceProfileCache deviceProfileCache,
                                                   TbAssetProfileCache assetProfileCache,
                                                   TbTenantProfileCache tenantProfileCache,
                                                   TbApiUsageStateService apiUsageStateService,
                                                   PartitionService partitionService,
                                                   ApplicationEventPublisher eventPublisher,
                                                   JwtSettingsService jwtSettingsService,
                                                   CalculatedFieldExecutionService calculatedFieldExecutionService,
                                                   CalculatedFieldCache calculatedFieldCache) {
        super(actorContext, tenantProfileCache, deviceProfileCache, assetProfileCache, calculatedFieldCache, apiUsageStateService, partitionService,
                eventPublisher, jwtSettingsService);
        this.queueFactory = tbQueueFactory;
        this.calculatedFieldExecutionService = calculatedFieldExecutionService;
    }

    @PostConstruct
    public void init() {
        super.init("tb-cf");
        this.calculatedFieldsExecutor = MoreExecutors.listeningDecorator(ThingsBoardExecutors.newWorkStealingPool(poolSize, "tb-cf-executor")); // TODO: multiple threads.

        this.mainConsumer = MainQueueConsumerManager.<TbProtoQueueMsg<ToCalculatedFieldMsg>, CalculatedFieldQueueConfig>builder()
                .queueKey(new QueueKey(ServiceType.TB_RULE_ENGINE))
                .config(CalculatedFieldQueueConfig.of(consumerPerPartition, (int) pollInterval))
                .msgPackProcessor(this::processMsgs)
                .consumerCreator((config, partitionId) -> queueFactory.createToCalculatedFieldMsgConsumer())
                .consumerExecutor(consumersExecutor)
                .scheduler(scheduler)
                .taskExecutor(mgmtExecutor)
                .build();
    }

    @PreDestroy
    public void destroy() {
        super.destroy();
        if (calculatedFieldsExecutor != null) {
            calculatedFieldsExecutor.shutdownNow();
        }
    }

    @Override
    protected void startConsumers() {
        super.startConsumers();
    }

    @Override
    protected void onTbApplicationEvent(PartitionChangeEvent event) {
        log.debug("Subscribing to partitions: {}", event.getCalculatedFieldsPartitions());
        mainConsumer.update(event.getCalculatedFieldsPartitions());
    }

    private void processMsgs(List<TbProtoQueueMsg<ToCalculatedFieldMsg>> msgs, TbQueueConsumer<TbProtoQueueMsg<ToCalculatedFieldMsg>> consumer, CalculatedFieldQueueConfig config) throws Exception {
        List<IdMsgPair<ToCalculatedFieldMsg>> orderedMsgList = msgs.stream().map(msg -> new IdMsgPair<>(UUID.randomUUID(), msg)).toList();
        ConcurrentMap<UUID, TbProtoQueueMsg<ToCalculatedFieldMsg>> pendingMap = orderedMsgList.stream().collect(
                Collectors.toConcurrentMap(IdMsgPair::getUuid, IdMsgPair::getMsg));
        CountDownLatch processingTimeoutLatch = new CountDownLatch(1);
        TbPackProcessingContext<TbProtoQueueMsg<ToCalculatedFieldMsg>> ctx = new TbPackProcessingContext<>(
                processingTimeoutLatch, pendingMap, new ConcurrentHashMap<>());
        PendingMsgHolder<ToCalculatedFieldMsg> pendingMsgHolder = new PendingMsgHolder<>();
        Future<?> packSubmitFuture = consumersExecutor.submit(() -> {
            orderedMsgList.forEach((element) -> {
                UUID id = element.getUuid();
                TbProtoQueueMsg<ToCalculatedFieldMsg> msg = element.getMsg();
                log.trace("[{}] Creating main callback for message: {}", id, msg.getValue());
                TbCallback callback = new TbPackCallback<>(id, ctx);
                try {
                    ToCalculatedFieldMsg toCfMsg = msg.getValue();
                    pendingMsgHolder.setMsg(toCfMsg);
                    if (toCfMsg.hasTelemetryMsg()) {
                        log.trace("[{}] Forwarding regular telemetry message for processing {}", id, toCfMsg.getTelemetryMsg());
                        forwardToActorSystem(toCfMsg.getTelemetryMsg(), callback);
                    } else if (toCfMsg.hasLinkedTelemetryMsg()) {
                        forwardToActorSystem(toCfMsg.getLinkedTelemetryMsg(), callback);
                    } else if (toCfMsg.hasComponentLifecycleMsg()) {
                        log.trace("[{}] Forwarding component lifecycle message for processing {}", id, toCfMsg.getComponentLifecycleMsg());
                        ///  TODO: forward to Actor system
                        forwardToCalculatedFieldService(toCfMsg.getComponentLifecycleMsg(), callback);
                    } else if (toCfMsg.hasEntityUpdateMsg()) {
                        log.trace("[{}] Forwarding entity update message for processing {}", id, toCfMsg.getEntityUpdateMsg());
                        ///  TODO: forward to Actor system
                        forwardToCalculatedFieldService(toCfMsg.getEntityUpdateMsg(), callback);
                    }
                } catch (Throwable e) {
                    log.warn("[{}] Failed to process message: {}", id, msg, e);
                    callback.onFailure(e);
                }
            });
        });
        if (!processingTimeoutLatch.await(packProcessingTimeout, TimeUnit.MILLISECONDS)) {
            if (!packSubmitFuture.isDone()) {
                packSubmitFuture.cancel(true);
                log.info("Timeout to process message: {}", pendingMsgHolder.getMsg());
            }
            if (log.isDebugEnabled()) {
                ctx.getAckMap().forEach((id, msg) -> log.debug("[{}] Timeout to process message: {}", id, msg.getValue()));
            }
            ctx.getFailedMap().forEach((id, msg) -> log.warn("[{}] Failed to process message: {}", id, msg.getValue()));
        }
        consumer.commit();
    }

    @Override
    protected ServiceType getServiceType() {
        return ServiceType.TB_RULE_ENGINE;
    }

    @Override
    protected long getNotificationPollDuration() {
        return pollInterval;
    }

    @Override
    protected long getNotificationPackProcessingTimeout() {
        return packProcessingTimeout;
    }

    @Override
    protected int getMgmtThreadPoolSize() {
        return Math.max(Runtime.getRuntime().availableProcessors(), 4);
    }

    @Override
    protected TbQueueConsumer<TbProtoQueueMsg<ToCalculatedFieldNotificationMsg>> createNotificationsConsumer() {
        return queueFactory.createToCalculatedFieldNotificationsMsgConsumer();
    }

    @Override
    protected void handleNotification(UUID id, TbProtoQueueMsg<ToCalculatedFieldNotificationMsg> msg, TbCallback callback) {
        ToCalculatedFieldNotificationMsg toCfNotification = msg.getValue();
        if (toCfNotification.hasComponentLifecycle()) {
            // from upstream (maybe removed since we don't need to init state for each partition)
            forwardToActorSystem(toCfNotification.getComponentLifecycle(), callback);
            handleComponentLifecycleMsg(id, ProtoUtils.fromProto(toCfNotification.getComponentLifecycle()));
        } else if (toCfNotification.hasEntityUpdateMsg()) {
            processEntityUpdateMsg(toCfNotification.getEntityUpdateMsg());
            // from upstream (maybe removed since we don't need to update state for each partition)
            forwardToActorSystem(toCfNotification.getEntityUpdateMsg(), callback);
        } else if (toCfNotification.hasLinkedTelemetryMsg()) {
            forwardToActorSystem(toCfNotification.getLinkedTelemetryMsg(), callback);
        }
    }

    private void forwardToActorSystem(CalculatedFieldTelemetryMsgProto msg, TbCallback callback) {
        var tenantId = toTenantId(msg.getTenantIdMSB(), msg.getTenantIdLSB());
        var entityId = EntityIdFactory.getByTypeAndUuid(msg.getEntityType(), new UUID(msg.getEntityIdMSB(), msg.getEntityIdLSB()));
        actorContext.tell(new CalculatedFieldTelemetryMsg(tenantId, entityId, msg, callback));
    }

    private void forwardToActorSystem(CalculatedFieldLinkedTelemetryMsgProto linkedMsg, TbCallback callback) {
        var msg = linkedMsg.getMsg();
        var tenantId = toTenantId(msg.getTenantIdMSB(), msg.getTenantIdLSB());
        var entityId = EntityIdFactory.getByTypeAndUuid(msg.getEntityType(), new UUID(msg.getEntityIdMSB(), msg.getEntityIdLSB()));
        actorContext.tell(new CalculatedFieldLinkedTelemetryMsg(tenantId, entityId, linkedMsg, callback));
    }

    private void forwardToCalculatedFieldService(ComponentLifecycleMsgProto msg, TbCallback callback) {
        var tenantId = toTenantId(msg.getTenantIdMSB(), msg.getTenantIdLSB());
        var calculatedFieldId = new CalculatedFieldId(new UUID(msg.getEntityIdMSB(), msg.getEntityIdLSB()));
        ListenableFuture<?> future = calculatedFieldsExecutor.submit(() -> calculatedFieldExecutionService.onCalculatedFieldLifecycleMsg(msg, callback));
        DonAsynchron.withCallback(future,
                __ -> callback.onSuccess(),
                t -> {
                    log.warn("[{}] Failed to process calculated field message for calculated field [{}]", tenantId.getId(), calculatedFieldId.getId(), t);
                    callback.onFailure(t);
                });
    }

    private void forwardToActorSystem(ComponentLifecycleMsgProto msg, TbCallback callback) {
        var tenantId = toTenantId(msg.getTenantIdMSB(), msg.getTenantIdLSB());
        var calculatedFieldId = new CalculatedFieldId(new UUID(msg.getEntityIdMSB(), msg.getEntityIdLSB()));
        ListenableFuture<?> future = calculatedFieldsExecutor.submit(() -> calculatedFieldExecutionService.onCalculatedFieldLifecycleMsg(msg, callback));
        DonAsynchron.withCallback(future,
                __ -> callback.onSuccess(),
                t -> {
                    log.warn("[{}] Failed to process calculated field message for calculated field [{}]", tenantId.getId(), calculatedFieldId.getId(), t);
                    callback.onFailure(t);
                });
    }

    private void forwardToCalculatedFieldService(CalculatedFieldEntityUpdateMsgProto msg, TbCallback callback) {
        var tenantId = toTenantId(msg.getTenantIdMSB(), msg.getTenantIdLSB());
        var entityId = EntityIdFactory.getByTypeAndUuid(msg.getEntityType(), new UUID(msg.getEntityIdMSB(), msg.getEntityIdLSB()));
        ListenableFuture<?> future = calculatedFieldsExecutor.submit(() -> calculatedFieldExecutionService.onEntityUpdateMsg(msg, callback));
        DonAsynchron.withCallback(future,
                __ -> callback.onSuccess(),
                t -> {
                    log.warn("[{}] Failed to process entity updated message for entity [{}]", tenantId.getId(), entityId.getId(), t);
                    callback.onFailure(t);
                });
    }

    private void forwardToActorSystem(CalculatedFieldEntityUpdateMsgProto msg, TbCallback callback) {
        var tenantId = toTenantId(msg.getTenantIdMSB(), msg.getTenantIdLSB());
        var entityId = EntityIdFactory.getByTypeAndUuid(msg.getEntityType(), new UUID(msg.getEntityIdMSB(), msg.getEntityIdLSB()));
        ListenableFuture<?> future = calculatedFieldsExecutor.submit(() -> calculatedFieldExecutionService.onEntityUpdateMsg(msg, callback));
        DonAsynchron.withCallback(future,
                __ -> callback.onSuccess(),
                t -> {
                    log.warn("[{}] Failed to process entity updated message for entity [{}]", tenantId.getId(), entityId.getId(), t);
                    callback.onFailure(t);
                });
    }

    private void processEntityUpdateMsg(CalculatedFieldEntityUpdateMsgProto entityUpdateMsg) {
        var tenantId = toTenantId(entityUpdateMsg.getTenantIdMSB(), entityUpdateMsg.getTenantIdLSB());
        var entityId = EntityIdFactory.getByTypeAndUuid(entityUpdateMsg.getEntityType(), new UUID(entityUpdateMsg.getEntityIdMSB(), entityUpdateMsg.getEntityIdLSB()));
        if (entityUpdateMsg.getAdded()) {
            var newProfile = EntityIdFactory.getByTypeAndUuid(entityUpdateMsg.getEntityProfileType(), new UUID(entityUpdateMsg.getNewProfileIdMSB(), entityUpdateMsg.getNewProfileIdLSB()));
            calculatedFieldCache.getEntitiesByProfile(tenantId, newProfile).add(entityId);
        } else if (entityUpdateMsg.getDeleted()) {
            var oldProfile = EntityIdFactory.getByTypeAndUuid(entityUpdateMsg.getEntityProfileType(), new UUID(entityUpdateMsg.getOldProfileIdMSB(), entityUpdateMsg.getOldProfileIdLSB()));
            calculatedFieldCache.getEntitiesByProfile(tenantId, oldProfile).remove(entityId);
        } else if (entityUpdateMsg.getUpdated()) {
            var oldProfile = EntityIdFactory.getByTypeAndUuid(entityUpdateMsg.getEntityProfileType(), new UUID(entityUpdateMsg.getOldProfileIdMSB(), entityUpdateMsg.getOldProfileIdLSB()));
            var newProfile = EntityIdFactory.getByTypeAndUuid(entityUpdateMsg.getEntityProfileType(), new UUID(entityUpdateMsg.getNewProfileIdMSB(), entityUpdateMsg.getNewProfileIdLSB()));
            calculatedFieldCache.getEntitiesByProfile(tenantId, oldProfile).remove(entityId);
            calculatedFieldCache.getEntitiesByProfile(tenantId, newProfile).add(entityId);
        }
    }

    private void throwNotHandled(Object msg, TbCallback callback) {
        log.warn("Message not handled: {}", msg);
        callback.onFailure(new RuntimeException("Message not handled!"));
    }

    private TenantId toTenantId(long tenantIdMSB, long tenantIdLSB) {
        return TenantId.fromUUID(new UUID(tenantIdMSB, tenantIdLSB));
    }

    @Override
    protected void stopConsumers() {
        super.stopConsumers();
        mainConsumer.stop();
        mainConsumer.awaitStop();
    }

    @Data(staticConstructor = "of")
    public static class CalculatedFieldQueueConfig implements QueueConfig {
        private final boolean consumerPerPartition;
        private final int pollInterval;
    }

}
