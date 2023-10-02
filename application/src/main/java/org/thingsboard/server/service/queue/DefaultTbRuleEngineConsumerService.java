/**
 * Copyright © 2016-2023 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.service.queue;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.common.data.id.QueueId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.queue.Queue;
import org.thingsboard.server.common.data.rpc.RpcError;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.common.msg.rpc.FromDeviceRpcResponse;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.ToRuleEngineNotificationMsg;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.QueueKey;
import org.thingsboard.server.queue.discovery.event.PartitionChangeEvent;
import org.thingsboard.server.queue.provider.TbRuleEngineQueueFactory;
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

@Service
@TbRuleEngineComponent
@Slf4j
public class DefaultTbRuleEngineConsumerService extends AbstractConsumerService<ToRuleEngineNotificationMsg> implements TbRuleEngineConsumerService {

    private final TbRuleEngineConsumerContext ctx;
    private final TbRuleEngineDeviceRpcService tbDeviceRpcService;

    private final ConcurrentMap<QueueKey, TbRuleEngineQueueConsumerManager> consumers = new ConcurrentHashMap<>();

    public DefaultTbRuleEngineConsumerService(TbRuleEngineConsumerContext ctx,
                                              TbRuleEngineQueueFactory tbRuleEngineQueueFactory,
                                              ActorSystemContext actorContext,
                                              DataDecodingEncodingService encodingService,
                                              TbRuleEngineDeviceRpcService tbDeviceRpcService,
                                              TbDeviceProfileCache deviceProfileCache,
                                              TbAssetProfileCache assetProfileCache,
                                              TbTenantProfileCache tenantProfileCache,
                                              TbApiUsageStateService apiUsageStateService,
                                              PartitionService partitionService, ApplicationEventPublisher eventPublisher) {
        super(actorContext, encodingService, tenantProfileCache, deviceProfileCache, assetProfileCache, apiUsageStateService, partitionService,
                eventPublisher, tbRuleEngineQueueFactory.createToRuleEngineNotificationsMsgConsumer(), Optional.empty());
        this.ctx = ctx;
        this.tbDeviceRpcService = tbDeviceRpcService;
    }

    @PostConstruct
    public void init() {
        super.init("tb-rule-engine-notifications-consumer"); // TODO: restore init of the main consumer?
        List<Queue> queues = ctx.findAllQueues();
        for (Queue configuration : queues) {
            if (partitionService.isManagedByCurrentService(configuration.getTenantId())) {
                initConsumer(configuration);
            }
        }
    }

    private void initConsumer(Queue configuration) {
        consumers.computeIfAbsent(new QueueKey(ServiceType.TB_RULE_ENGINE, configuration),
                key -> new TbRuleEngineQueueConsumerManager(ctx, key)).init(configuration);
    }

    @Override
    protected void onTbApplicationEvent(PartitionChangeEvent event) {
        if (event.getServiceType().equals(getServiceType())) {
            var consumer = consumers.get(event.getQueueKey());
            if (consumer != null) {
                consumer.subscribe(event);
            } else {
                log.warn("Received invalid partition change event for {} that is not managed by this service", event.getQueueKey());
            }
        }
    }

    @Override
    protected void launchMainConsumers() {
        consumers.values().forEach(TbRuleEngineQueueConsumerManager::launchMainConsumer);
    }

    @Override
    protected void stopMainConsumers() {
        consumers.values().forEach(TbRuleEngineQueueConsumerManager::stop);
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
        if (!nfMsg.getComponentLifecycleMsg().isEmpty()) {
            handleComponentLifecycleMsg(id, nfMsg.getComponentLifecycleMsg());
            callback.onSuccess();
        } else if (nfMsg.hasFromDeviceRpcResponse()) {
            TransportProtos.FromDeviceRPCResponseProto proto = nfMsg.getFromDeviceRpcResponse();
            RpcError error = proto.getError() > 0 ? RpcError.values()[proto.getError()] : null;
            FromDeviceRpcResponse response = new FromDeviceRpcResponse(new UUID(proto.getRequestIdMSB(), proto.getRequestIdLSB())
                    , proto.getResponse(), error);
            tbDeviceRpcService.processRpcResponseFromDevice(response);
            callback.onSuccess();
        } else if (nfMsg.hasQueueUpdateMsg()) {
            ctx.getScheduler().execute(() -> updateQueue(nfMsg.getQueueUpdateMsg()));
            callback.onSuccess();
        } else if (nfMsg.hasQueueDeleteMsg()) {
            ctx.getScheduler().execute(() -> deleteQueue(nfMsg.getQueueDeleteMsg()));
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
            Queue queue = ctx.getQueueService().findQueueById(tenantId, queueId);
            consumers.computeIfAbsent(queueKey, key -> new TbRuleEngineQueueConsumerManager(ctx, key)).update(queue);
        }

        partitionService.updateQueue(queueUpdateMsg);
        partitionService.recalculatePartitions(ctx.getServiceInfoProvider().getServiceInfo(),
                new ArrayList<>(partitionService.getOtherServices(ServiceType.TB_RULE_ENGINE)));
    }

    private void deleteQueue(TransportProtos.QueueDeleteMsg queueDeleteMsg) {
        log.info("Received queue delete msg: [{}]", queueDeleteMsg);
        TenantId tenantId = new TenantId(new UUID(queueDeleteMsg.getTenantIdMSB(), queueDeleteMsg.getTenantIdLSB()));
        QueueKey queueKey = new QueueKey(ServiceType.TB_RULE_ENGINE, queueDeleteMsg.getQueueName(), tenantId);

        partitionService.removeQueue(queueDeleteMsg);
        var manager = consumers.remove(queueKey);
        if (manager != null) {
            manager.delete();
        }
        partitionService.recalculatePartitions(ctx.getServiceInfoProvider().getServiceInfo(), new ArrayList<>(partitionService.getOtherServices(ServiceType.TB_RULE_ENGINE)));
    }

    @Scheduled(fixedDelayString = "${queue.rule-engine.stats.print-interval-ms}")
    public void printStats() {
        if (ctx.isStatsEnabled()) {
            long ts = System.currentTimeMillis();
            consumers.values().forEach(manager -> manager.printStats(ts));
        }
    }

}
