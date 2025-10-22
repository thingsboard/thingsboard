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
package org.thingsboard.server.service.queue;

import jakarta.annotation.PreDestroy;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.actors.calculatedField.CalculatedFieldEntityActionEventMsg;
import org.thingsboard.server.actors.calculatedField.CalculatedFieldLinkedTelemetryMsg;
import org.thingsboard.server.actors.calculatedField.CalculatedFieldTelemetryMsg;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.data.queue.QueueConfig;
import org.thingsboard.server.common.msg.cf.CalculatedFieldPartitionChangeMsg;
import org.thingsboard.server.common.msg.plugin.ComponentLifecycleMsg;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.dao.resource.TbResourceDataCache;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.gen.transport.TransportProtos.CalculatedFieldLinkedTelemetryMsgProto;
import org.thingsboard.server.gen.transport.TransportProtos.CalculatedFieldTelemetryMsgProto;
import org.thingsboard.server.gen.transport.TransportProtos.ToCalculatedFieldMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToCalculatedFieldNotificationMsg;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.common.consumer.PartitionedQueueConsumerManager;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.QueueKey;
import org.thingsboard.server.queue.discovery.event.PartitionChangeEvent;
import org.thingsboard.server.queue.provider.TbRuleEngineQueueFactory;
import org.thingsboard.server.queue.util.TbRuleEngineComponent;
import org.thingsboard.server.service.apiusage.TbApiUsageStateService;
import org.thingsboard.server.service.cf.CalculatedFieldCache;
import org.thingsboard.server.service.cf.CalculatedFieldStateService;
import org.thingsboard.server.service.profile.TbAssetProfileCache;
import org.thingsboard.server.service.profile.TbDeviceProfileCache;
import org.thingsboard.server.service.queue.processing.AbstractPartitionBasedConsumerService;
import org.thingsboard.server.service.queue.processing.IdMsgPair;
import org.thingsboard.server.service.security.auth.jwt.settings.JwtSettingsService;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@TbRuleEngineComponent
public class DefaultTbCalculatedFieldConsumerService extends AbstractPartitionBasedConsumerService<ToCalculatedFieldNotificationMsg> implements TbCalculatedFieldConsumerService {

    @Value("${queue.calculated_fields.poll_interval:25}")
    private long pollInterval;
    @Value("${queue.calculated_fields.pack_processing_timeout:60000}")
    private long packProcessingTimeout;

    private final TbRuleEngineQueueFactory queueFactory;
    private final CalculatedFieldStateService stateService;

    public DefaultTbCalculatedFieldConsumerService(TbRuleEngineQueueFactory tbQueueFactory,
                                                   ActorSystemContext actorContext,
                                                   TbDeviceProfileCache deviceProfileCache,
                                                   TbAssetProfileCache assetProfileCache,
                                                   TbResourceDataCache tbResourceDataCache,
                                                   TbTenantProfileCache tenantProfileCache,
                                                   TbApiUsageStateService apiUsageStateService,
                                                   PartitionService partitionService,
                                                   ApplicationEventPublisher eventPublisher,
                                                   JwtSettingsService jwtSettingsService,
                                                   CalculatedFieldCache calculatedFieldCache,
                                                   CalculatedFieldStateService stateService) {
        super(actorContext, tenantProfileCache, deviceProfileCache, assetProfileCache, tbResourceDataCache, calculatedFieldCache, apiUsageStateService, partitionService,
                eventPublisher, jwtSettingsService);
        this.queueFactory = tbQueueFactory;
        this.stateService = stateService;
    }

    @Override
    protected void onStartUp() {
        var queueKey = new QueueKey(ServiceType.TB_RULE_ENGINE, DataConstants.CF_QUEUE_NAME);
        var eventConsumer = PartitionedQueueConsumerManager.<TbProtoQueueMsg<ToCalculatedFieldMsg>>create()
                .queueKey(queueKey)
                .topic(partitionService.getTopic(queueKey))
                .pollInterval(pollInterval)
                .msgPackProcessor(this::processMsgs)
                .consumerCreator((queueConfig, tpi) -> queueFactory.createToCalculatedFieldMsgConsumer(tpi))
                .queueAdmin(queueFactory.getCalculatedFieldQueueAdmin())
                .consumerExecutor(consumersExecutor)
                .scheduler(scheduler)
                .taskExecutor(mgmtExecutor)
                .build();
        stateService.init(eventConsumer);
    }

    @PreDestroy
    public void destroy() {
        super.destroy();
    }

    @Override
    protected void startConsumers() {
        super.startConsumers();
    }

    @Override
    protected void onPartitionChangeEvent(PartitionChangeEvent event) {
        try {
            event.getNewPartitions().forEach((queueKey, partitions) -> {
                if (DataConstants.CF_QUEUE_NAME.equals(queueKey.getQueueName())) {
                    stateService.restore(queueKey, partitions);
                }
            });
            // eventConsumer's partitions will be updated by stateService

            // Cleanup old entities after corresponding consumers are stopped.
            // Any periodic tasks need to check that the entity is still managed by the current server before processing.
            actorContext.tell(new CalculatedFieldPartitionChangeMsg());
        } catch (Throwable t) {
            log.error("Failed to process partition change event: {}", event, t);
        }
    }

    private void processMsgs(List<TbProtoQueueMsg<ToCalculatedFieldMsg>> msgs, TbQueueConsumer<TbProtoQueueMsg<ToCalculatedFieldMsg>> consumer, Object consumerKey, QueueConfig config) throws Exception {
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
                    processMsg(toCfMsg, id, callback);
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
            ctx.getAckMap().forEach((id, msg) -> log.warn("[{}] Timeout to process message: {}", id, msg.getValue()));
            ctx.getFailedMap().forEach((id, msg) -> log.warn("[{}] Failed to process message: {}", id, msg.getValue()));
        }
        consumer.commit();
    }

    private void processMsg(ToCalculatedFieldMsg toCfMsg, UUID id, TbCallback callback) {
        if (toCfMsg.hasTelemetryMsg()) {
            log.trace("[{}] Forwarding regular telemetry message for processing {}", id, toCfMsg.getTelemetryMsg());
            forwardToActorSystem(toCfMsg.getTelemetryMsg(), callback);
        } else if (toCfMsg.hasLinkedTelemetryMsg()) {
            forwardToActorSystem(toCfMsg.getLinkedTelemetryMsg(), callback);
        } else if (toCfMsg.hasEventMsg()) {
            actorContext.tell(CalculatedFieldEntityActionEventMsg.fromProto(toCfMsg.getEventMsg(), callback));
        }
    }

    @Override
    protected ServiceType getServiceType() {
        return ServiceType.TB_RULE_ENGINE;
    }

    @Override
    protected String getPrefix() {
        return "tb-cf";
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
        return queueFactory.createToCalculatedFieldNotificationMsgConsumer();
    }

    @Override
    protected void handleNotification(UUID id, TbProtoQueueMsg<ToCalculatedFieldNotificationMsg> msg, TbCallback callback) {
        ToCalculatedFieldNotificationMsg toCfNotification = msg.getValue();
        if (toCfNotification.hasLinkedTelemetryMsg()) {
            forwardToActorSystem(toCfNotification.getLinkedTelemetryMsg(), callback);
        }
    }

    @EventListener
    public void handleComponentLifecycleEvent(ComponentLifecycleMsg event) {
        if (event.getEntityId().getEntityType() == EntityType.TENANT) {
            if (event.getEvent() == ComponentLifecycleEvent.DELETED) {
                Set<TopicPartitionInfo> partitions = stateService.getPartitions();
                if (CollectionUtils.isEmpty(partitions)) {
                    return;
                }
                stateService.delete(partitions.stream()
                        .filter(tpi -> tpi.getTenantId().isPresent() && tpi.getTenantId().get().equals(event.getTenantId()))
                        .collect(Collectors.toSet()));
            }
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

    private TenantId toTenantId(long tenantIdMSB, long tenantIdLSB) {
        return TenantId.fromUUID(new UUID(tenantIdMSB, tenantIdLSB));
    }

    @Override
    protected void stopConsumers() {
        super.stopConsumers();
        stateService.stop(); // eventConsumer will be stopped by stateService
    }

}
