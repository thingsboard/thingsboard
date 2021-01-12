/**
 * Copyright © 2016-2021 The Thingsboard Authors
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

import com.google.protobuf.ProtocolStringList;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.thingsboard.rule.engine.api.RpcError;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.queue.Queue;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.queue.QueueToRuleEngineMsg;
import org.thingsboard.server.common.msg.queue.RuleEngineException;
import org.thingsboard.server.common.msg.queue.RuleNodeInfo;
import org.thingsboard.server.common.msg.queue.ServiceQueue;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.common.msg.queue.TbMsgCallback;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.common.stats.StatsFactory;
import org.thingsboard.server.common.transport.util.DataDecodingEncodingService;
import org.thingsboard.server.dao.queue.QueueService;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.ToRuleEngineMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToRuleEngineNotificationMsg;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.PartitionChangeEvent;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.queue.provider.TbRuleEngineQueueFactory;
import org.thingsboard.server.queue.util.TbRuleEngineComponent;
import org.thingsboard.server.service.apiusage.TbApiUsageStateService;
import org.thingsboard.server.service.profile.TbDeviceProfileCache;
import org.thingsboard.server.service.queue.processing.AbstractConsumerService;
import org.thingsboard.server.service.queue.processing.TbRuleEngineProcessingDecision;
import org.thingsboard.server.service.queue.processing.TbRuleEngineProcessingResult;
import org.thingsboard.server.service.queue.processing.TbRuleEngineProcessingStrategy;
import org.thingsboard.server.service.queue.processing.TbRuleEngineProcessingStrategyFactory;
import org.thingsboard.server.service.queue.processing.TbRuleEngineSubmitStrategy;
import org.thingsboard.server.service.queue.processing.TbRuleEngineSubmitStrategyFactory;
import org.thingsboard.server.service.rpc.FromDeviceRpcResponse;
import org.thingsboard.server.service.rpc.TbRuleEngineDeviceRpcService;
import org.thingsboard.server.service.stats.RuleEngineStatisticsService;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Service
@TbRuleEngineComponent
@Slf4j
public class DefaultTbRuleEngineConsumerService extends AbstractConsumerService<ToRuleEngineNotificationMsg> implements TbRuleEngineConsumerService {

    public static final String SUCCESSFUL_STATUS = "successful";
    public static final String FAILED_STATUS = "failed";
    @Value("${queue.rule-engine.poll-interval}")
    private long pollDuration;
    @Value("${queue.rule-engine.pack-processing-timeout}")
    private long packProcessingTimeout;
    @Value("${queue.rule-engine.stats.enabled:true}")
    private boolean statsEnabled;

    private final StatsFactory statsFactory;
    private final TbRuleEngineSubmitStrategyFactory submitStrategyFactory;
    private final TbRuleEngineProcessingStrategyFactory processingStrategyFactory;
    private final TbRuleEngineQueueFactory tbRuleEngineQueueFactory;
    private final RuleEngineStatisticsService statisticsService;
    private final TbRuleEngineDeviceRpcService tbDeviceRpcService;
    private final ConcurrentMap<String, ConsumerManager> consumerManagers = new ConcurrentHashMap<>();
    private ExecutorService submitExecutor;
    private final QueueService queueService;
    private final PartitionService partitionService;
    private final TbServiceInfoProvider serviceInfoProvider;
    private final TenantId tenantId;

    private final Lock consumerLock;

    public DefaultTbRuleEngineConsumerService(TbRuleEngineProcessingStrategyFactory processingStrategyFactory,
                                              TbRuleEngineSubmitStrategyFactory submitStrategyFactory,
                                              TbRuleEngineQueueFactory tbRuleEngineQueueFactory,
                                              RuleEngineStatisticsService statisticsService,
                                              ActorSystemContext actorContext,
                                              DataDecodingEncodingService encodingService,
                                              TbRuleEngineDeviceRpcService tbDeviceRpcService,
                                              StatsFactory statsFactory,
                                              TbDeviceProfileCache deviceProfileCache,
                                              TbTenantProfileCache tenantProfileCache,
                                              TbApiUsageStateService apiUsageStateService,
                                              QueueService queueService,
                                              PartitionService partitionService,
                                              TbServiceInfoProvider serviceInfoProvider) {
        super(actorContext, encodingService, tenantProfileCache, deviceProfileCache, apiUsageStateService, tbRuleEngineQueueFactory.createToRuleEngineNotificationsMsgConsumer());
        this.statisticsService = statisticsService;
        this.tbRuleEngineQueueFactory = tbRuleEngineQueueFactory;
        this.submitStrategyFactory = submitStrategyFactory;
        this.processingStrategyFactory = processingStrategyFactory;
        this.tbDeviceRpcService = tbDeviceRpcService;
        this.statsFactory = statsFactory;
        this.queueService = queueService;
        this.partitionService = partitionService;
        this.serviceInfoProvider = serviceInfoProvider;
        this.tenantId = actorContext.getServiceInfoProvider().getIsolatedTenant().orElse(TenantId.SYS_TENANT_ID);
        this.consumerLock = new ReentrantLock();
    }

    @PostConstruct
    public void init() {
        super.init("tb-rule-engine-consumer", "tb-rule-engine-notifications-consumer");
        queueService.findQueues(tenantId).forEach(this::initQueue);
        submitExecutor = Executors.newSingleThreadExecutor();
    }

    private ConsumerManager initQueue(Queue queue) {
        ConsumerManager manager =
                new ConsumerManager(tbRuleEngineQueueFactory.createToRuleEngineMsgConsumer(queue), new TbRuleEngineConsumerStats(queue.getName(), statsFactory), queue);
        consumerManagers.put(queue.getName(), manager);
        return manager;
    }

    @PreDestroy
    public void stop() {
        super.destroy();
        if (submitExecutor != null) {
            submitExecutor.shutdownNow();
        }
    }

    @Override
    public void onApplicationEvent(PartitionChangeEvent partitionChangeEvent) {
        if (partitionChangeEvent.getServiceType().equals(getServiceType())) {
            ServiceQueue serviceQueue = partitionChangeEvent.getServiceQueueKey().getServiceQueue();
            log.info("[{}] Subscribing to partitions: {}", serviceQueue.getQueue(), partitionChangeEvent.getPartitions());
            ConsumerManager manager = consumerManagers.get(serviceQueue.getQueue());
            manager.subscribe(partitionChangeEvent.getPartitions());
        }
    }

    @Override
    protected void launchMainConsumers() {
        consumerManagers.values().forEach(ConsumerManager::start);
    }

    @Override
    protected void stopMainConsumers() {
        consumerManagers.values().forEach(ConsumerManager::unsubscribe);
//        consumerManagers.values().forEach(ConsumerManager::stop);
    }

    private void printFirstOrAll(Queue queue, TbMsgPackProcessingContext
            ctx, Map<UUID, TbProtoQueueMsg<ToRuleEngineMsg>> map, String prefix) {
        boolean printAll = log.isTraceEnabled();
        log.info("{} to process [{}] messages", prefix, map.size());
        for (Map.Entry<UUID, TbProtoQueueMsg<ToRuleEngineMsg>> pending : map.entrySet()) {
            ToRuleEngineMsg tmp = pending.getValue().getValue();
            TbMsg tmpMsg = TbMsg.fromBytes(queue.getName(), tmp.getTbMsg().toByteArray(), TbMsgCallback.EMPTY);
            RuleNodeInfo ruleNodeInfo = ctx.getLastVisitedRuleNode(pending.getKey());
            if (printAll) {
                log.trace("[{}] {} to process message: {}, Last Rule Node: {}", new TenantId(new UUID(tmp.getTenantIdMSB(), tmp.getTenantIdLSB())), prefix, tmpMsg, ruleNodeInfo);
            } else {
                log.info("[{}] {} to process message: {}, Last Rule Node: {}", new TenantId(new UUID(tmp.getTenantIdMSB(), tmp.getTenantIdLSB())), prefix, tmpMsg, ruleNodeInfo);
                break;
            }
        }
    }

    @Override
    protected ServiceType getServiceType() {
        return ServiceType.TB_RULE_ENGINE;
    }

    @Override
    protected long getNotificationPollDuration() {
        return pollDuration;
    }

    @Override
    protected long getNotificationPackProcessingTimeout() {
        return packProcessingTimeout;
    }

    @Override
    protected void handleNotification(UUID id, TbProtoQueueMsg<ToRuleEngineNotificationMsg> msg, TbCallback
            callback) throws Exception {
        ToRuleEngineNotificationMsg nfMsg = msg.getValue();
        if (nfMsg.getComponentLifecycleMsg() != null && !nfMsg.getComponentLifecycleMsg().isEmpty()) {
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
        try {
            consumerLock.lock();
            String queueName = queueUpdateMsg.getQueueName();
            ConsumerManager manager = consumerManagers.get(queueName);
            Queue queue = queueService.findQueueByTenantIdAndName(tenantId, queueName);
            if (manager == null) {
                manager = initQueue(queue);
                manager.start();
                partitionService.addNewQueue(queueUpdateMsg);
                partitionService.recalculatePartitions(serviceInfoProvider.getServiceInfo(), new ArrayList<>(partitionService.getOtherServices(ServiceType.TB_RULE_ENGINE)));
            } else {
                manager.stop();
                manager.setQueue(queue);
                manager.start();
            }
        } finally {
            consumerLock.unlock();
        }
    }

    private void deleteQueue(TransportProtos.QueueDeleteMsg queueDeleteMsg) {
        try {
            consumerLock.lock();
            partitionService.removeQueue(queueDeleteMsg);
            String queueName = queueDeleteMsg.getQueueName();
            ConsumerManager manager = consumerManagers.remove(queueName);
            manager.unsubscribe();
            manager.stop();
        } finally {
            consumerLock.unlock();
        }
    }

    private void forwardToRuleEngineActor(String queueName, TenantId tenantId, ToRuleEngineMsg
            toRuleEngineMsg, TbMsgCallback callback) {
        TbMsg tbMsg = TbMsg.fromBytes(queueName, toRuleEngineMsg.getTbMsg().toByteArray(), callback);
        QueueToRuleEngineMsg msg;
        ProtocolStringList relationTypesList = toRuleEngineMsg.getRelationTypesList();
        Set<String> relationTypes = null;
        if (relationTypesList != null) {
            if (relationTypesList.size() == 1) {
                relationTypes = Collections.singleton(relationTypesList.get(0));
            } else {
                relationTypes = new HashSet<>(relationTypesList);
            }
        }
        msg = new QueueToRuleEngineMsg(tenantId, tbMsg, relationTypes, toRuleEngineMsg.getFailureMessage());
        actorContext.tell(msg);
    }

    @Scheduled(fixedDelayString = "${queue.rule-engine.stats.print-interval-ms}")
    public void printStats() {
        if (statsEnabled) {
            long ts = System.currentTimeMillis();
            consumerManagers.forEach((queue, manager) -> {
                manager.printStats();
                statisticsService.reportQueueStats(ts, manager.getStats());
                manager.resetStats();
            });
        }
    }

    private class ConsumerManager {
        private final TbQueueConsumer<TbProtoQueueMsg<ToRuleEngineMsg>> consumer;
        @Getter
        private final TbRuleEngineConsumerStats stats;
        @Getter
        private Queue queue;
        private Future<?> future;

        public ConsumerManager(TbQueueConsumer<TbProtoQueueMsg<ToRuleEngineMsg>> consumer, TbRuleEngineConsumerStats stats, Queue queue) {
            this.consumer = consumer;
            this.stats = stats;
            this.queue = queue;
        }

        public void start() {
            future = consumersExecutor.submit(() -> {
                log.info("TB Rule Engine Consumer started, [{}] queue.", queue.getName());
                while (!stopped && future != null && !future.isCancelled()) {
                    try {
                        List<TbProtoQueueMsg<ToRuleEngineMsg>> msgs = consumer.poll(pollDuration);
                        if (msgs.isEmpty()) {
                            continue;
                        }
                        TbRuleEngineSubmitStrategy submitStrategy = submitStrategyFactory.newInstance(queue.getName(), queue.getSubmitStrategy());
                        TbRuleEngineProcessingStrategy ackStrategy = processingStrategyFactory.newInstance(queue.getName(), queue.getProcessingStrategy());

                        submitStrategy.init(msgs);

                        while (!stopped && future != null && !future.isCancelled()) {
                            TbMsgPackProcessingContext ctx = new TbMsgPackProcessingContext(queue.getName(), submitStrategy);
                            submitStrategy.submitAttempt((id, msg) -> submitExecutor.submit(() -> {
                                log.trace("[{}] Creating callback for message: {}", id, msg.getValue());
                                ToRuleEngineMsg toRuleEngineMsg = msg.getValue();
                                TenantId tenantId = new TenantId(new UUID(toRuleEngineMsg.getTenantIdMSB(), toRuleEngineMsg.getTenantIdLSB()));
                                TbMsgCallback callback = statsEnabled ?
                                        new TbMsgPackCallback(id, tenantId, ctx, stats.getTimer(tenantId, SUCCESSFUL_STATUS), stats.getTimer(tenantId, FAILED_STATUS)) :
                                        new TbMsgPackCallback(id, tenantId, ctx);
                                try {
                                    if (toRuleEngineMsg.getTbMsg() != null && !toRuleEngineMsg.getTbMsg().isEmpty()) {
                                        forwardToRuleEngineActor(queue.getName(), tenantId, toRuleEngineMsg, callback);
                                    } else {
                                        callback.onSuccess();
                                    }
                                } catch (Exception e) {
                                    callback.onFailure(new RuleEngineException(e.getMessage()));
                                }
                            }));

                            boolean timeout = false;
                            if (!ctx.await(queue.getPackProcessingTimeout(), TimeUnit.MILLISECONDS)) {
                                timeout = true;
                            }

                            TbRuleEngineProcessingResult result = new TbRuleEngineProcessingResult(queue.getName(), timeout, ctx);
                            if (timeout) {
                                printFirstOrAll(queue, ctx, ctx.getPendingMap(), "Timeout");
                            }
                            if (!ctx.getFailedMap().isEmpty()) {
                                printFirstOrAll(queue, ctx, ctx.getFailedMap(), "Failed");
                            }
                            ctx.printProfilerStats();

                            TbRuleEngineProcessingDecision decision = ackStrategy.analyze(result);
                            if (statsEnabled) {
                                stats.log(result, decision.isCommit());
                            }
                            if (decision.isCommit()) {
                                submitStrategy.stop();
                                break;
                            } else {
                                submitStrategy.update(decision.getReprocessMap());
                            }
                        }
                        consumer.commit();
                    } catch (Exception e) {
                        if (!stopped && future != null && !future.isCancelled()) {
                            log.warn("Failed to process messages from queue.", e);
                            try {
                                Thread.sleep(pollDuration);
                            } catch (InterruptedException e2) {
                                log.trace("Failed to wait until the server has capacity to handle new requests", e2);
                            }
                        }
                    }
                }
                log.info("TB Rule Engine Consumer stopped, [{}] queue.", queue.getName());
            });
        }

        public void stop() {
            if (future != null) {
                future.cancel(true);
            }
        }

        public void subscribe(Set<TopicPartitionInfo> partitions) {
            consumer.subscribe(partitions);
        }

        public void unsubscribe() {
            consumer.unsubscribe();
        }

        public void resetStats() {
            stats.reset();
        }

        public void printStats() {
            stats.printStats();
        }

        public void setQueue(Queue queue) {
            this.queue = queue;
        }
    }

}
