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

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.thingsboard.server.actors.ActorSystemContext;
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
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.ToRuleEngineNotificationMsg;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.QueueKey;
import org.thingsboard.server.queue.discovery.event.PartitionChangeEvent;
import org.thingsboard.server.queue.provider.TbRuleEngineQueueFactory;
import org.thingsboard.server.queue.util.AfterStartUp;
import org.thingsboard.server.queue.util.DataDecodingEncodingService;
import org.thingsboard.server.queue.util.TbRuleEngineComponent;
import org.thingsboard.server.service.apiusage.TbApiUsageStateService;
import org.thingsboard.server.service.profile.TbAssetProfileCache;
import org.thingsboard.server.service.profile.TbDeviceProfileCache;
import org.thingsboard.server.service.queue.processing.AbstractConsumerService;
import org.thingsboard.server.service.queue.ruleengine.TbRuleEngineConsumerContext;
import org.thingsboard.server.service.queue.ruleengine.TbRuleEngineQueueConsumerManager;
import org.thingsboard.server.service.rpc.TbRuleEngineDeviceRpcService;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

@Service
@TbRuleEngineComponent
@Slf4j
public class DefaultTbRuleEngineConsumerService extends AbstractConsumerService<ToRuleEngineNotificationMsg> implements TbRuleEngineConsumerService {

    private final TbRuleEngineConsumerContext ctx;
    private final QueueService queueService;
    private final TbRuleEngineDeviceRpcService tbDeviceRpcService;

    private final ConcurrentMap<QueueKey, TbRuleEngineQueueConsumerManager> consumers = new ConcurrentHashMap<>();

    public DefaultTbRuleEngineConsumerService(TbRuleEngineConsumerContext ctx,
                                              TbRuleEngineQueueFactory tbRuleEngineQueueFactory,
                                              ActorSystemContext actorContext,
                                              DataDecodingEncodingService encodingService,
                                              TbRuleEngineDeviceRpcService tbDeviceRpcService,
                                              QueueService queueService,
                                              TbDeviceProfileCache deviceProfileCache,
                                              TbAssetProfileCache assetProfileCache,
                                              TbTenantProfileCache tenantProfileCache,
                                              TbApiUsageStateService apiUsageStateService,
                                              PartitionService partitionService, ApplicationEventPublisher eventPublisher) {
        super(actorContext, encodingService, tenantProfileCache, deviceProfileCache, assetProfileCache, apiUsageStateService, partitionService,
                eventPublisher, tbRuleEngineQueueFactory.createToRuleEngineNotificationsMsgConsumer(), Optional.empty());
        this.ctx = ctx;
        this.tbDeviceRpcService = tbDeviceRpcService;
        this.queueService = queueService;
    }

    @PostConstruct
    public void init() {
        super.init("tb-rule-engine-notifications-consumer");
        List<Queue> queues = queueService.findAllQueues();
        for (Queue configuration : queues) {
            if (partitionService.isManagedByCurrentService(configuration.getTenantId())) {
                initConsumer(configuration);
            }
        }
    }

    private void initConsumer(Queue configuration) {
        getOrCreateConsumer(new QueueKey(ServiceType.TB_RULE_ENGINE, configuration)).init(configuration);
    }

    @Override
    protected void onTbApplicationEvent(PartitionChangeEvent event) {
        if (event.getServiceType().equals(getServiceType())) {
            event.getPartitionsMap().forEach((queueKey, partitions) -> {
                var consumer = consumers.get(queueKey);
                if (consumer != null) {
                    consumer.update(partitions);
                } else {
                    log.warn("Received invalid partition change event for {} that is not managed by this service", queueKey);
                }
            });
        }
    }

    @AfterStartUp(order = AfterStartUp.REGULAR_SERVICE)
    public void onApplicationEvent(ApplicationReadyEvent event) {
        super.onApplicationEvent(event);
        ctx.setReady(true);
    }

    @Override
    protected void launchMainConsumers() {}

    @Override
    protected void stopConsumers() {
        consumers.values().forEach(TbRuleEngineQueueConsumerManager::stop);
        consumers.values().forEach(TbRuleEngineQueueConsumerManager::awaitStop);
        ctx.stop();
    }

    @Override
    protected ServiceType getServiceType() {
        return ServiceType.TB_RULE_ENGINE;
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
    protected void handleNotification(UUID id, TbProtoQueueMsg<ToRuleEngineNotificationMsg> msg, TbCallback callback) throws Exception {
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
        } else if (nfMsg.hasQueueUpdateMsg()) {
            updateQueue(nfMsg.getQueueUpdateMsg());
            callback.onSuccess();
        } else if (nfMsg.hasQueueDeleteMsg()) {
            deleteQueue(nfMsg.getQueueDeleteMsg());
            callback.onSuccess();
        } else {
            log.trace("Received notification with missing handler");
            callback.onSuccess();
        }
    }

    private void updateQueue(TransportProtos.QueueUpdateMsg queueUpdateMsg) {
        log.info("Received queue update msg: [{}]", queueUpdateMsg);
        TenantId tenantId = new TenantId(new UUID(queueUpdateMsg.getTenantIdMSB(), queueUpdateMsg.getTenantIdLSB()));
        if (partitionService.isManagedByCurrentService(tenantId)) {
            QueueId queueId = new QueueId(new UUID(queueUpdateMsg.getQueueIdMSB(), queueUpdateMsg.getQueueIdLSB()));
            String queueName = queueUpdateMsg.getQueueName();
            QueueKey queueKey = new QueueKey(ServiceType.TB_RULE_ENGINE, queueName, tenantId);
            Queue queue = queueService.findQueueById(tenantId, queueId);

            TbRuleEngineQueueConsumerManager consumerManager = getOrCreateConsumer(queueKey);
            Queue oldQueue = consumerManager.getQueue();
            consumerManager.update(queue);

            if (oldQueue != null && queue.getPartitions() == oldQueue.getPartitions()) {
                return;
            }
        }

        partitionService.updateQueue(queueUpdateMsg);
        partitionService.recalculatePartitions(ctx.getServiceInfoProvider().getServiceInfo(),
                new ArrayList<>(partitionService.getOtherServices(ServiceType.TB_RULE_ENGINE)));
    }

    private void deleteQueue(TransportProtos.QueueDeleteMsg queueDeleteMsg) {
        log.info("Received queue delete msg: [{}]", queueDeleteMsg);
        TenantId tenantId = new TenantId(new UUID(queueDeleteMsg.getTenantIdMSB(), queueDeleteMsg.getTenantIdLSB()));
        QueueKey queueKey = new QueueKey(ServiceType.TB_RULE_ENGINE, queueDeleteMsg.getQueueName(), tenantId);
        var consumerManager = consumers.remove(queueKey);
        if (consumerManager != null) {
            consumerManager.delete(true);
        }

        partitionService.removeQueue(queueDeleteMsg);
        partitionService.recalculatePartitions(ctx.getServiceInfoProvider().getServiceInfo(), new ArrayList<>(partitionService.getOtherServices(ServiceType.TB_RULE_ENGINE)));
    }

    @EventListener
    public void handleComponentLifecycleEvent(ComponentLifecycleMsg event) {
        if (event.getEntityId().getEntityType() == EntityType.TENANT) {
            if (event.getEvent() == ComponentLifecycleEvent.DELETED) {
                List<QueueKey> toRemove = consumers.keySet().stream()
                        .filter(queueKey -> queueKey.getTenantId().equals(event.getTenantId()))
                        .collect(Collectors.toList());
                toRemove.forEach(queueKey -> {
                    var consumerManager = consumers.remove(queueKey);
                    if (consumerManager != null) {
                        consumerManager.delete(false);
                    }
                });
            }
        }
    }

    private TbRuleEngineQueueConsumerManager getOrCreateConsumer(QueueKey queueKey) {
        return consumers.computeIfAbsent(queueKey, key -> new TbRuleEngineQueueConsumerManager(ctx, key));
    }

    @Scheduled(fixedDelayString = "${queue.rule-engine.stats.print-interval-ms}")
    public void printStats() {
        if (ctx.isStatsEnabled()) {
            long ts = System.currentTimeMillis();
            consumers.values().forEach(manager -> manager.printStats(ts));
        }
    }

}
