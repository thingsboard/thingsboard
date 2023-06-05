/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.common.data.id.QueueId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.queue.Queue;
import org.thingsboard.server.common.data.rpc.RpcError;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.queue.QueueToRuleEngineMsg;
import org.thingsboard.server.common.msg.queue.RuleEngineException;
import org.thingsboard.server.common.msg.queue.RuleNodeInfo;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.common.msg.queue.TbMsgCallback;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.common.msg.rpc.FromDeviceRpcResponse;
import org.thingsboard.server.common.stats.StatsFactory;
import org.thingsboard.server.dao.queue.QueueService;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.ToRuleEngineMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToRuleEngineNotificationMsg;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.QueueKey;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.queue.discovery.event.PartitionChangeEvent;
import org.thingsboard.server.queue.provider.TbRuleEngineQueueFactory;
import org.thingsboard.server.queue.util.DataDecodingEncodingService;
import org.thingsboard.server.queue.util.TbRuleEngineComponent;
import org.thingsboard.server.service.apiusage.TbApiUsageStateService;
import org.thingsboard.server.service.profile.TbAssetProfileCache;
import org.thingsboard.server.service.profile.TbDeviceProfileCache;
import org.thingsboard.server.service.queue.processing.AbstractConsumerService;
import org.thingsboard.server.service.queue.processing.TbRuleEngineProcessingDecision;
import org.thingsboard.server.service.queue.processing.TbRuleEngineProcessingResult;
import org.thingsboard.server.service.queue.processing.TbRuleEngineProcessingStrategy;
import org.thingsboard.server.service.queue.processing.TbRuleEngineProcessingStrategyFactory;
import org.thingsboard.server.service.queue.processing.TbRuleEngineSubmitStrategy;
import org.thingsboard.server.service.queue.processing.TbRuleEngineSubmitStrategyFactory;
import org.thingsboard.server.service.rpc.TbRuleEngineDeviceRpcService;
import org.thingsboard.server.service.stats.RuleEngineStatisticsService;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Service
@TbRuleEngineComponent
@Slf4j
public class DefaultTbRuleEngineConsumerService extends AbstractConsumerService<ToRuleEngineNotificationMsg> implements TbRuleEngineConsumerService {

    public static final String SUCCESSFUL_STATUS = "successful";
    public static final String FAILED_STATUS = "failed";
    public static final String THREAD_TOPIC_SEPARATOR = " | ";
    @Value("${queue.rule-engine.poll-interval}")
    private long pollDuration;
    @Value("${queue.rule-engine.pack-processing-timeout}")
    private long packProcessingTimeout;
    @Value("${queue.rule-engine.stats.enabled:true}")
    private boolean statsEnabled;
    @Value("${queue.rule-engine.prometheus-stats.enabled:false}")
    boolean prometheusStatsEnabled;

    private final StatsFactory statsFactory;
    private final TbRuleEngineSubmitStrategyFactory submitStrategyFactory;
    private final TbRuleEngineProcessingStrategyFactory processingStrategyFactory;
    private final TbRuleEngineQueueFactory tbRuleEngineQueueFactory;
    private final RuleEngineStatisticsService statisticsService;
    private final TbRuleEngineDeviceRpcService tbDeviceRpcService;
    private final TbServiceInfoProvider serviceInfoProvider;
    private final QueueService queueService;
    private final ConcurrentMap<QueueKey, TbQueueConsumer<TbProtoQueueMsg<ToRuleEngineMsg>>> consumers = new ConcurrentHashMap<>();
    private final ConcurrentMap<QueueKey, Queue> consumerConfigurations = new ConcurrentHashMap<>();
    private final ConcurrentMap<QueueKey, TbRuleEngineConsumerStats> consumerStats = new ConcurrentHashMap<>();
    private final ConcurrentMap<QueueKey, TbTopicWithConsumerPerPartition> topicsConsumerPerPartition = new ConcurrentHashMap<>();
    final ExecutorService submitExecutor = Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName("tb-rule-engine-consumer-submit"));
    final ScheduledExecutorService repartitionExecutor = Executors.newScheduledThreadPool(1, ThingsBoardThreadFactory.forName("tb-rule-engine-consumer-repartition"));

    public DefaultTbRuleEngineConsumerService(TbRuleEngineProcessingStrategyFactory processingStrategyFactory,
                                              TbRuleEngineSubmitStrategyFactory submitStrategyFactory,
                                              TbRuleEngineQueueFactory tbRuleEngineQueueFactory,
                                              RuleEngineStatisticsService statisticsService,
                                              ActorSystemContext actorContext,
                                              DataDecodingEncodingService encodingService,
                                              TbRuleEngineDeviceRpcService tbDeviceRpcService,
                                              StatsFactory statsFactory,
                                              TbDeviceProfileCache deviceProfileCache,
                                              TbAssetProfileCache assetProfileCache,
                                              TbTenantProfileCache tenantProfileCache,
                                              TbApiUsageStateService apiUsageStateService,
                                              PartitionService partitionService, ApplicationEventPublisher eventPublisher,
                                              TbServiceInfoProvider serviceInfoProvider, QueueService queueService) {
        super(actorContext, encodingService, tenantProfileCache, deviceProfileCache, assetProfileCache, apiUsageStateService, partitionService, eventPublisher, tbRuleEngineQueueFactory.createToRuleEngineNotificationsMsgConsumer(), Optional.empty());
        this.statisticsService = statisticsService;
        this.tbRuleEngineQueueFactory = tbRuleEngineQueueFactory;
        this.submitStrategyFactory = submitStrategyFactory;
        this.processingStrategyFactory = processingStrategyFactory;
        this.tbDeviceRpcService = tbDeviceRpcService;
        this.statsFactory = statsFactory;
        this.serviceInfoProvider = serviceInfoProvider;
        this.queueService = queueService;
    }

    @PostConstruct
    public void init() {
        super.init("tb-rule-engine-consumer", "tb-rule-engine-notifications-consumer");
        List<Queue> queues = queueService.findAllQueues();
        for (Queue configuration : queues) {
            initConsumer(configuration);
        }
    }

    private void initConsumer(Queue configuration) {
        QueueKey queueKey = new QueueKey(ServiceType.TB_RULE_ENGINE, configuration);
        consumerConfigurations.putIfAbsent(queueKey, configuration);
        consumerStats.putIfAbsent(queueKey, new TbRuleEngineConsumerStats(configuration.getName(), statsFactory));
        if (!configuration.isConsumerPerPartition()) {
            consumers.computeIfAbsent(queueKey, queueName -> tbRuleEngineQueueFactory.createToRuleEngineMsgConsumer(configuration));
        } else {
            topicsConsumerPerPartition.computeIfAbsent(queueKey, k -> new TbTopicWithConsumerPerPartition(k.getQueueName()));
        }
    }

    @PreDestroy
    public void stop() {
        super.destroy();
        submitExecutor.shutdownNow();
        repartitionExecutor.shutdownNow();
    }

    @Override
    protected void onTbApplicationEvent(PartitionChangeEvent event) {
        if (event.getServiceType().equals(getServiceType())) {
            String serviceQueue = event.getQueueKey().getQueueName();
            log.info("[{}] Subscribing to partitions: {}", serviceQueue, event.getPartitions());
            if (!consumerConfigurations.get(event.getQueueKey()).isConsumerPerPartition()) {
                consumers.get(event.getQueueKey()).subscribe(event.getPartitions());
            } else {
                log.info("[{}] Subscribing consumer per partition: {}", serviceQueue, event.getPartitions());
                subscribeConsumerPerPartition(event.getQueueKey(), event.getPartitions());
            }
        }
    }

    void subscribeConsumerPerPartition(QueueKey queue, Set<TopicPartitionInfo> partitions) {
        topicsConsumerPerPartition.get(queue).getSubscribeQueue().add(partitions);
        scheduleTopicRepartition(queue);
    }

    private void scheduleTopicRepartition(QueueKey queue) {
        repartitionExecutor.schedule(() -> repartitionTopicWithConsumerPerPartition(queue), 1, TimeUnit.SECONDS);
    }

    void repartitionTopicWithConsumerPerPartition(final QueueKey queueKey) {
        if (stopped) {
            return;
        }
        TbTopicWithConsumerPerPartition tbTopicWithConsumerPerPartition = topicsConsumerPerPartition.get(queueKey);
        java.util.Queue<Set<TopicPartitionInfo>> subscribeQueue = tbTopicWithConsumerPerPartition.getSubscribeQueue();
        if (subscribeQueue.isEmpty()) {
            return;
        }
        if (tbTopicWithConsumerPerPartition.getLock().tryLock()) {
            try {
                Set<TopicPartitionInfo> partitions = null;
                while (!subscribeQueue.isEmpty()) {
                    partitions = subscribeQueue.poll();
                }
                if (partitions == null) {
                    return;
                }

                Set<TopicPartitionInfo> addedPartitions = new HashSet<>(partitions);
                ConcurrentMap<TopicPartitionInfo, TbQueueConsumer<TbProtoQueueMsg<ToRuleEngineMsg>>> consumers = tbTopicWithConsumerPerPartition.getConsumers();
                addedPartitions.removeAll(consumers.keySet());
                log.info("calculated addedPartitions {}", addedPartitions);

                Set<TopicPartitionInfo> removedPartitions = new HashSet<>(consumers.keySet());
                removedPartitions.removeAll(partitions);
                log.info("calculated removedPartitions {}", removedPartitions);

                removedPartitions.forEach((tpi) -> {
                    removeConsumerForTopicByTpi(queueKey.getQueueName(), consumers, tpi);
                });

                addedPartitions.forEach((tpi) -> {
                    log.info("[{}] Adding consumer for topic: {}", queueKey, tpi);
                    Queue configuration = consumerConfigurations.get(queueKey);
                    TbQueueConsumer<TbProtoQueueMsg<ToRuleEngineMsg>> consumer = tbRuleEngineQueueFactory.createToRuleEngineMsgConsumer(configuration);
                    consumers.put(tpi, consumer);
                    launchConsumer(consumer, consumerConfigurations.get(queueKey), consumerStats.get(queueKey), "" + queueKey + "-" + tpi.getPartition().orElse(-999999));
                    consumer.subscribe(Collections.singleton(tpi));
                });

            } finally {
                tbTopicWithConsumerPerPartition.getLock().unlock();
            }
        } else {
            scheduleTopicRepartition(queueKey); //reschedule later
        }

    }

    void removeConsumerForTopicByTpi(String queue, ConcurrentMap<TopicPartitionInfo, TbQueueConsumer<TbProtoQueueMsg<ToRuleEngineMsg>>> consumers, TopicPartitionInfo tpi) {
        log.info("[{}] Removing consumer for topic: {}", queue, tpi);
        consumers.get(tpi).unsubscribe();
        consumers.remove(tpi);
    }

    @Override
    protected void launchMainConsumers() {
        consumers.forEach((queue, consumer) -> launchConsumer(consumer, consumerConfigurations.get(queue), consumerStats.get(queue), queue.getQueueName()));
    }

    @Override
    protected void stopMainConsumers() {
        consumers.values().forEach(TbQueueConsumer::unsubscribe);
        topicsConsumerPerPartition.values().forEach(tbTopicWithConsumerPerPartition -> tbTopicWithConsumerPerPartition.getConsumers().keySet()
                .forEach((tpi) -> removeConsumerForTopicByTpi(tbTopicWithConsumerPerPartition.getTopic(), tbTopicWithConsumerPerPartition.getConsumers(), tpi)));
    }

    void launchConsumer(TbQueueConsumer<TbProtoQueueMsg<ToRuleEngineMsg>> consumer, Queue configuration, TbRuleEngineConsumerStats stats, String threadSuffix) {
        consumersExecutor.execute(() -> consumerLoop(consumer, configuration, stats, threadSuffix));
    }

    void consumerLoop(TbQueueConsumer<TbProtoQueueMsg<ToRuleEngineMsg>> consumer, org.thingsboard.server.common.data.queue.Queue configuration, TbRuleEngineConsumerStats stats, String threadSuffix) {
        updateCurrentThreadName(threadSuffix);
        while (!stopped && !consumer.isStopped()) {
            try {
                List<TbProtoQueueMsg<ToRuleEngineMsg>> msgs = consumer.poll(pollDuration);
                if (msgs.isEmpty()) {
                    continue;
                }
                final TbRuleEngineSubmitStrategy submitStrategy = getSubmitStrategy(configuration);
                final TbRuleEngineProcessingStrategy ackStrategy = getAckStrategy(configuration);
                submitStrategy.init(msgs);
                while (!stopped && !consumer.isStopped()) {
                    TbMsgPackProcessingContext ctx = new TbMsgPackProcessingContext(configuration.getName(), submitStrategy, ackStrategy.isSkipTimeoutMsgs());
                    submitStrategy.submitAttempt((id, msg) -> submitExecutor.submit(() -> submitMessage(configuration, stats, ctx, id, msg)));

                    final boolean timeout = !ctx.await(configuration.getPackProcessingTimeout(), TimeUnit.MILLISECONDS);

                    TbRuleEngineProcessingResult result = new TbRuleEngineProcessingResult(configuration.getName(), timeout, ctx);
                    if (timeout) {
                        printFirstOrAll(configuration, ctx, ctx.getPendingMap(), "Timeout");
                    }
                    if (!ctx.getFailedMap().isEmpty()) {
                        printFirstOrAll(configuration, ctx, ctx.getFailedMap(), "Failed");
                    }
                    ctx.printProfilerStats();

                    TbRuleEngineProcessingDecision decision = ackStrategy.analyze(result);
                    if (statsEnabled) {
                        stats.log(result, decision.isCommit());
                    }

                    ctx.cleanup();

                    if (decision.isCommit()) {
                        submitStrategy.stop();
                        break;
                    } else {
                        submitStrategy.update(decision.getReprocessMap());
                    }
                }
                consumer.commit();
            } catch (Exception e) {
                if (!stopped) {
                    log.warn("Failed to process messages from queue.", e);
                    try {
                        Thread.sleep(pollDuration);
                    } catch (InterruptedException e2) {
                        log.trace("Failed to wait until the server has capacity to handle new requests", e2);
                    }
                }
            }
        }
        log.info("TB Rule Engine Consumer stopped.");
    }

    void updateCurrentThreadName(String threadSuffix) {
        String name = Thread.currentThread().getName();
        int spliteratorIndex = name.indexOf(THREAD_TOPIC_SEPARATOR);
        if (spliteratorIndex > 0) {
            name = name.substring(0, spliteratorIndex);
        }
        name = name + THREAD_TOPIC_SEPARATOR + threadSuffix;
        Thread.currentThread().setName(name);
    }

    TbRuleEngineProcessingStrategy getAckStrategy(Queue configuration) {
        return processingStrategyFactory.newInstance(configuration.getName(), configuration.getProcessingStrategy());
    }

    TbRuleEngineSubmitStrategy getSubmitStrategy(Queue configuration) {
        return submitStrategyFactory.newInstance(configuration.getName(), configuration.getSubmitStrategy());
    }

    void submitMessage(Queue configuration, TbRuleEngineConsumerStats stats, TbMsgPackProcessingContext ctx, UUID id, TbProtoQueueMsg<ToRuleEngineMsg> msg) {
        log.trace("[{}] Creating callback for topic {} message: {}", id, configuration.getName(), msg.getValue());
        ToRuleEngineMsg toRuleEngineMsg = msg.getValue();
        TenantId tenantId = TenantId.fromUUID(new UUID(toRuleEngineMsg.getTenantIdMSB(), toRuleEngineMsg.getTenantIdLSB()));
        TbMsgCallback callback = prometheusStatsEnabled ?
                new TbMsgPackCallback(id, tenantId, ctx, stats.getTimer(tenantId, SUCCESSFUL_STATUS), stats.getTimer(tenantId, FAILED_STATUS)) :
                new TbMsgPackCallback(id, tenantId, ctx);
        try {
            if (toRuleEngineMsg.getTbMsg() != null && !toRuleEngineMsg.getTbMsg().isEmpty()) {
                forwardToRuleEngineActor(configuration.getName(), tenantId, toRuleEngineMsg, callback);
            } else {
                callback.onSuccess();
            }
        } catch (Exception e) {
            callback.onFailure(new RuleEngineException(e.getMessage()));
        }
    }

    private void printFirstOrAll(Queue configuration, TbMsgPackProcessingContext ctx, Map<UUID, TbProtoQueueMsg<ToRuleEngineMsg>> map, String prefix) {
        boolean printAll = log.isTraceEnabled();
        log.info("{} to process [{}] messages", prefix, map.size());
        for (Map.Entry<UUID, TbProtoQueueMsg<ToRuleEngineMsg>> pending : map.entrySet()) {
            ToRuleEngineMsg tmp = pending.getValue().getValue();
            TbMsg tmpMsg = TbMsg.fromBytes(configuration.getName(), tmp.getTbMsg().toByteArray(), TbMsgCallback.EMPTY);
            RuleNodeInfo ruleNodeInfo = ctx.getLastVisitedRuleNode(pending.getKey());
            if (printAll) {
                log.trace("[{}] {} to process message: {}, Last Rule Node: {}", TenantId.fromUUID(new UUID(tmp.getTenantIdMSB(), tmp.getTenantIdLSB())), prefix, tmpMsg, ruleNodeInfo);
            } else {
                log.info("[{}] {} to process message: {}, Last Rule Node: {}", TenantId.fromUUID(new UUID(tmp.getTenantIdMSB(), tmp.getTenantIdLSB())), prefix, tmpMsg, ruleNodeInfo);
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
    protected void handleNotification(UUID id, TbProtoQueueMsg<ToRuleEngineNotificationMsg> msg, TbCallback callback) throws Exception {
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
            repartitionExecutor.execute(() -> updateQueue(nfMsg.getQueueUpdateMsg()));
            callback.onSuccess();
        } else if (nfMsg.hasQueueDeleteMsg()) {
            repartitionExecutor.execute(() -> deleteQueue(nfMsg.getQueueDeleteMsg()));
            callback.onSuccess();
        } else {
            log.trace("Received notification with missing handler");
            callback.onSuccess();
        }
    }

    private void updateQueue(TransportProtos.QueueUpdateMsg queueUpdateMsg) {
        log.info("Received queue update msg: [{}]", queueUpdateMsg);
        String queueName = queueUpdateMsg.getQueueName();
        TenantId tenantId = new TenantId(new UUID(queueUpdateMsg.getTenantIdMSB(), queueUpdateMsg.getTenantIdLSB()));
        QueueId queueId = new QueueId(new UUID(queueUpdateMsg.getQueueIdMSB(), queueUpdateMsg.getQueueIdLSB()));
        QueueKey queueKey = new QueueKey(ServiceType.TB_RULE_ENGINE, queueUpdateMsg.getQueueName(), tenantId);
        Queue queue = queueService.findQueueById(tenantId, queueId);
        Queue oldQueue = consumerConfigurations.remove(queueKey);
        if (oldQueue != null) {
            if (oldQueue.isConsumerPerPartition()) {
                TbTopicWithConsumerPerPartition consumerPerPartition = topicsConsumerPerPartition.remove(queueKey);
                ReentrantLock lock = consumerPerPartition.getLock();
                try {
                    lock.lock();
                    consumerPerPartition.getConsumers().values().forEach(TbQueueConsumer::unsubscribe);
                } finally {
                    lock.unlock();
                }
            } else {
                TbQueueConsumer<TbProtoQueueMsg<ToRuleEngineMsg>> consumer = consumers.remove(queueKey);
                consumer.unsubscribe();
            }
        }

        initConsumer(queue);

        if (!queue.isConsumerPerPartition()) {
            launchConsumer(consumers.get(queueKey), consumerConfigurations.get(queueKey), consumerStats.get(queueKey), queueName);
        }

        partitionService.updateQueue(queueUpdateMsg);
        partitionService.recalculatePartitions(serviceInfoProvider.getServiceInfo(), new ArrayList<>(partitionService.getOtherServices(ServiceType.TB_RULE_ENGINE)));
    }

    private void deleteQueue(TransportProtos.QueueDeleteMsg queueDeleteMsg) {
        log.info("Received queue delete msg: [{}]", queueDeleteMsg);
        TenantId tenantId = new TenantId(new UUID(queueDeleteMsg.getTenantIdMSB(), queueDeleteMsg.getTenantIdLSB()));
        QueueKey queueKey = new QueueKey(ServiceType.TB_RULE_ENGINE, queueDeleteMsg.getQueueName(), tenantId);

        Queue queue = consumerConfigurations.remove(queueKey);
        if (queue != null) {
            if (queue.isConsumerPerPartition()) {
                TbTopicWithConsumerPerPartition tbTopicWithConsumerPerPartition = topicsConsumerPerPartition.remove(queueKey);
                if (tbTopicWithConsumerPerPartition != null) {
                    tbTopicWithConsumerPerPartition.getConsumers().values().forEach(TbQueueConsumer::unsubscribe);
                    tbTopicWithConsumerPerPartition.getConsumers().clear();
                }
            } else {
                TbQueueConsumer<TbProtoQueueMsg<ToRuleEngineMsg>> consumer = consumers.remove(queueKey);
                if (consumer != null) {
                    consumer.unsubscribe();
                }
            }
        }
        partitionService.removeQueue(queueDeleteMsg);
    }

    private void forwardToRuleEngineActor(String queueName, TenantId tenantId, ToRuleEngineMsg toRuleEngineMsg, TbMsgCallback callback) {
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
            consumerStats.forEach((queue, stats) -> {
                stats.printStats();
                statisticsService.reportQueueStats(ts, stats);
                stats.reset();
            });
        }
    }

}
