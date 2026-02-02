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
package org.thingsboard.server.service.queue;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.QueueId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.data.queue.Queue;
import org.thingsboard.server.common.data.rpc.RpcError;
import org.thingsboard.server.common.msg.plugin.ComponentLifecycleMsg;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.common.msg.rpc.FromDeviceRpcResponse;
import org.thingsboard.server.common.util.ProtoUtils;
import org.thingsboard.server.dao.queue.QueueService;
import org.thingsboard.server.dao.resource.TbResourceDataCache;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.QueueDeleteMsg;
import org.thingsboard.server.gen.transport.TransportProtos.QueueUpdateMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToRuleEngineNotificationMsg;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.QueueKey;
import org.thingsboard.server.queue.discovery.event.PartitionChangeEvent;
import org.thingsboard.server.queue.util.TbRuleEngineComponent;
import org.thingsboard.server.service.apiusage.TbApiUsageStateService;
import org.thingsboard.server.service.cf.CalculatedFieldCache;
import org.thingsboard.server.service.profile.TbAssetProfileCache;
import org.thingsboard.server.service.profile.TbDeviceProfileCache;
import org.thingsboard.server.service.queue.processing.AbstractPartitionBasedConsumerService;
import org.thingsboard.server.service.queue.ruleengine.TbRuleEngineConsumerContext;
import org.thingsboard.server.service.queue.ruleengine.TbRuleEngineQueueConsumerManager;
import org.thingsboard.server.service.rpc.TbRuleEngineDeviceRpcService;
import org.thingsboard.server.service.security.auth.jwt.settings.JwtSettingsService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

@Service
@TbRuleEngineComponent
public class DefaultTbRuleEngineConsumerService extends AbstractPartitionBasedConsumerService<ToRuleEngineNotificationMsg> implements TbRuleEngineConsumerService {

    private final TbRuleEngineConsumerContext ctx;
    private final QueueService queueService;
    private final TbRuleEngineDeviceRpcService tbDeviceRpcService;

    private final ConcurrentMap<QueueKey, TbRuleEngineQueueConsumerManager> consumers = new ConcurrentHashMap<>();

    public DefaultTbRuleEngineConsumerService(TbRuleEngineConsumerContext ctx,
                                              ActorSystemContext actorContext,
                                              TbRuleEngineDeviceRpcService tbDeviceRpcService,
                                              QueueService queueService,
                                              TbDeviceProfileCache deviceProfileCache,
                                              TbAssetProfileCache assetProfileCache,
                                              TbResourceDataCache tbResourceDataCache,
                                              TbTenantProfileCache tenantProfileCache,
                                              TbApiUsageStateService apiUsageStateService,
                                              PartitionService partitionService,
                                              ApplicationEventPublisher eventPublisher,
                                              JwtSettingsService jwtSettingsService,
                                              CalculatedFieldCache calculatedFieldCache) {
        super(actorContext, tenantProfileCache, deviceProfileCache, assetProfileCache, tbResourceDataCache, calculatedFieldCache, apiUsageStateService, partitionService, eventPublisher, jwtSettingsService);
        this.ctx = ctx;
        this.tbDeviceRpcService = tbDeviceRpcService;
        this.queueService = queueService;
    }

    @Override
    protected void onStartUp() {
        List<Queue> queues = queueService.findAllQueues();
        for (Queue configuration : queues) {
            if (partitionService.isManagedByCurrentService(configuration.getTenantId())) {
                QueueKey queueKey = new QueueKey(ServiceType.TB_RULE_ENGINE, configuration);
                createConsumer(queueKey, configuration);
            }
        }
    }

    @Override
    protected void onPartitionChangeEvent(PartitionChangeEvent event) {
        event.getNewPartitions().forEach((queueKey, partitions) -> {
            if (DataConstants.CF_QUEUE_NAME.equals(queueKey.getQueueName()) || DataConstants.CF_STATES_QUEUE_NAME.equals(queueKey.getQueueName())) {
                return;
            }
            if (partitionService.isManagedByCurrentService(queueKey.getTenantId())) {
                var consumer = getConsumer(queueKey).orElseGet(() -> {
                    Queue config = queueService.findQueueByTenantIdAndName(queueKey.getTenantId(), queueKey.getQueueName());
                    if (config == null) {
                        if (!partitions.isEmpty()) {
                            log.error("[{}] Queue configuration is missing", queueKey, new RuntimeException("stacktrace"));
                        }
                        return null;
                    }
                    return createConsumer(queueKey, config);
                });
                if (consumer != null) {
                    consumer.update(partitions);
                }
            }
        });
        consumers.keySet().stream()
                .collect(Collectors.groupingBy(QueueKey::getTenantId))
                .forEach((tenantId, queueKeys) -> {
                    if (!partitionService.isManagedByCurrentService(tenantId)) {
                        queueKeys.forEach(queueKey -> {
                            removeConsumer(queueKey).ifPresent(TbRuleEngineQueueConsumerManager::stop);
                        });
                    }
                });
    }

    @Override
    protected void stopConsumers() {
        super.stopConsumers();
        consumers.values().forEach(TbRuleEngineQueueConsumerManager::stop);
        consumers.values().forEach(TbRuleEngineQueueConsumerManager::awaitStop);
    }

    @Override
    protected ServiceType getServiceType() {
        return ServiceType.TB_RULE_ENGINE;
    }

    @Override
    protected String getPrefix() {
        return "tb-rule-engine";
    }

    @Override
    protected long getNotificationPollDuration() {
        return ctx.getPollDuration();
    }

    @Override
    protected long getNotificationPackProcessingTimeout() {
        return ctx.getPackProcessingTimeout();
    }

    @Override
    protected int getMgmtThreadPoolSize() {
        return ctx.getMgmtThreadPoolSize();
    }

    @Override
    protected TbQueueConsumer<TbProtoQueueMsg<ToRuleEngineNotificationMsg>> createNotificationsConsumer() {
        return ctx.getQueueFactory().createToRuleEngineNotificationsMsgConsumer();
    }

    @Override
    protected void handleNotification(UUID id, TbProtoQueueMsg<ToRuleEngineNotificationMsg> msg, TbCallback callback) {
        ToRuleEngineNotificationMsg nfMsg = msg.getValue();
        if (nfMsg.hasComponentLifecycle()) {
            handleComponentLifecycleMsg(id, ProtoUtils.fromProto(nfMsg.getComponentLifecycle()));
            callback.onSuccess();
        } else if (nfMsg.hasFromDeviceRpcResponse()) {
            TransportProtos.FromDeviceRPCResponseProto proto = nfMsg.getFromDeviceRpcResponse();
            RpcError error = proto.getError() > 0 ? RpcError.values()[proto.getError()] : null;
            FromDeviceRpcResponse response = new FromDeviceRpcResponse(new UUID(proto.getRequestIdMSB(), proto.getRequestIdLSB())
                    , proto.getResponse(), error);
            tbDeviceRpcService.processRpcResponseFromDevice(response);
            callback.onSuccess();
        } else if (nfMsg.getQueueUpdateMsgsCount() > 0) {
            updateQueues(nfMsg.getQueueUpdateMsgsList());
            callback.onSuccess();
        } else if (nfMsg.getQueueDeleteMsgsCount() > 0) {
            deleteQueues(nfMsg.getQueueDeleteMsgsList());
            callback.onSuccess();
        } else {
            log.trace("Received notification with missing handler");
            callback.onSuccess();
        }
    }

    private void updateQueues(List<QueueUpdateMsg> queueUpdateMsgs) {
        for (QueueUpdateMsg queueUpdateMsg : queueUpdateMsgs) {
            log.info("Received queue update msg: [{}]", queueUpdateMsg);
            TenantId tenantId = TenantId.fromUUID(new UUID(queueUpdateMsg.getTenantIdMSB(), queueUpdateMsg.getTenantIdLSB()));
            if (partitionService.isManagedByCurrentService(tenantId)) {
                QueueId queueId = new QueueId(new UUID(queueUpdateMsg.getQueueIdMSB(), queueUpdateMsg.getQueueIdLSB()));
                String queueName = queueUpdateMsg.getQueueName();
                QueueKey queueKey = new QueueKey(ServiceType.TB_RULE_ENGINE, queueName, tenantId);
                Queue queue = queueService.findQueueById(tenantId, queueId);

                getConsumer(queueKey).ifPresentOrElse(consumer -> consumer.update(queue),
                        () -> createConsumer(queueKey, queue));
            }
        }

        partitionService.updateQueues(queueUpdateMsgs);
        partitionService.recalculatePartitions(ctx.getServiceInfoProvider().getServiceInfo(),
                new ArrayList<>(partitionService.getOtherServices(ServiceType.TB_RULE_ENGINE)));
    }

    private void deleteQueues(List<QueueDeleteMsg> queueDeleteMsgs) {
        for (QueueDeleteMsg queueDeleteMsg : queueDeleteMsgs) {
            log.info("Received queue delete msg: [{}]", queueDeleteMsg);
            TenantId tenantId = TenantId.fromUUID(new UUID(queueDeleteMsg.getTenantIdMSB(), queueDeleteMsg.getTenantIdLSB()));
            QueueKey queueKey = new QueueKey(ServiceType.TB_RULE_ENGINE, queueDeleteMsg.getQueueName(), tenantId);
            removeConsumer(queueKey).ifPresent(consumer -> consumer.delete(true));
        }

        partitionService.removeQueues(queueDeleteMsgs);
        partitionService.recalculatePartitions(ctx.getServiceInfoProvider().getServiceInfo(), new ArrayList<>(partitionService.getOtherServices(ServiceType.TB_RULE_ENGINE)));
    }

    @EventListener
    public void handleComponentLifecycleEvent(ComponentLifecycleMsg event) {
        if (event.getEntityId().getEntityType() == EntityType.TENANT) {
            if (event.getEvent() == ComponentLifecycleEvent.DELETED) {
                List<QueueKey> toRemove = consumers.keySet().stream()
                        .filter(queueKey -> queueKey.getTenantId().equals(event.getTenantId()))
                        .toList();
                toRemove.forEach(queueKey -> {
                    removeConsumer(queueKey).ifPresent(consumer -> consumer.delete(false));
                });
            }
        }
    }

    private Optional<TbRuleEngineQueueConsumerManager> getConsumer(QueueKey queueKey) {
        return Optional.ofNullable(consumers.get(queueKey));
    }

    private TbRuleEngineQueueConsumerManager createConsumer(QueueKey queueKey, Queue queue) {
        var consumer = TbRuleEngineQueueConsumerManager.create()
                .ctx(ctx)
                .queueKey(queueKey)
                .consumerExecutor(consumersExecutor)
                .scheduler(scheduler)
                .taskExecutor(mgmtExecutor)
                .build();
        consumers.put(queueKey, consumer);
        consumer.init(queue);
        return consumer;
    }

    private Optional<TbRuleEngineQueueConsumerManager> removeConsumer(QueueKey queueKey) {
        return Optional.ofNullable(consumers.remove(queueKey));
    }

    @Scheduled(fixedDelayString = "${queue.rule-engine.stats.print-interval-ms}")
    public void printStats() {
        if (ctx.isStatsEnabled()) {
            long ts = System.currentTimeMillis();
            consumers.values().forEach(manager -> manager.printStats(ts));
        }
    }

}
