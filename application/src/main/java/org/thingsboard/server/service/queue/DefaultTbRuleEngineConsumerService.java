/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.thingsboard.rule.engine.api.RpcError;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.TbActorMsg;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.queue.*;
import org.thingsboard.server.common.stats.StatsFactory;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.ToRuleEngineMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToRuleEngineNotificationMsg;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.PartitionChangeEvent;
import org.thingsboard.server.queue.provider.TbRuleEngineQueueFactory;
import org.thingsboard.server.queue.settings.TbQueueRuleEngineSettings;
import org.thingsboard.server.queue.settings.TbRuleEngineQueueConfiguration;
import org.thingsboard.server.queue.util.TbRuleEngineComponent;
import org.thingsboard.server.common.transport.util.DataDecodingEncodingService;
import org.thingsboard.server.service.queue.processing.*;
import org.thingsboard.server.service.rpc.FromDeviceRpcResponse;
import org.thingsboard.server.service.rpc.TbRuleEngineDeviceRpcService;
import org.thingsboard.server.service.stats.RuleEngineStatisticsService;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.*;

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
    private final TbQueueRuleEngineSettings ruleEngineSettings;
    private final RuleEngineStatisticsService statisticsService;
    private final TbRuleEngineDeviceRpcService tbDeviceRpcService;
    private final ConcurrentMap<String, TbQueueConsumer<TbProtoQueueMsg<ToRuleEngineMsg>>> consumers = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, TbRuleEngineQueueConfiguration> consumerConfigurations = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, TbRuleEngineConsumerStats> consumerStats = new ConcurrentHashMap<>();
    private ExecutorService submitExecutor;

    public DefaultTbRuleEngineConsumerService(TbRuleEngineProcessingStrategyFactory processingStrategyFactory,
                                              TbRuleEngineSubmitStrategyFactory submitStrategyFactory,
                                              TbQueueRuleEngineSettings ruleEngineSettings,
                                              TbRuleEngineQueueFactory tbRuleEngineQueueFactory, RuleEngineStatisticsService statisticsService,
                                              ActorSystemContext actorContext, DataDecodingEncodingService encodingService,
                                              TbRuleEngineDeviceRpcService tbDeviceRpcService,
                                              StatsFactory statsFactory) {
        super(actorContext, encodingService, tbRuleEngineQueueFactory.createToRuleEngineNotificationsMsgConsumer());
        this.statisticsService = statisticsService;
        this.ruleEngineSettings = ruleEngineSettings;
        this.tbRuleEngineQueueFactory = tbRuleEngineQueueFactory;
        this.submitStrategyFactory = submitStrategyFactory;
        this.processingStrategyFactory = processingStrategyFactory;
        this.tbDeviceRpcService = tbDeviceRpcService;
        this.statsFactory = statsFactory;
    }

    @PostConstruct
    public void init() {
        super.init("tb-rule-engine-consumer", "tb-rule-engine-notifications-consumer");
        for (TbRuleEngineQueueConfiguration configuration : ruleEngineSettings.getQueues()) {
            consumerConfigurations.putIfAbsent(configuration.getName(), configuration);
            consumers.computeIfAbsent(configuration.getName(), queueName -> tbRuleEngineQueueFactory.createToRuleEngineMsgConsumer(configuration));
            consumerStats.put(configuration.getName(), new TbRuleEngineConsumerStats(configuration.getName(), statsFactory));
        }
        submitExecutor = Executors.newSingleThreadExecutor();
    }

    @PreDestroy
    public void stop() {
        super.destroy();
        if (submitExecutor != null) {
            submitExecutor.shutdownNow();
        }
        ruleEngineSettings.getQueues().forEach(config -> consumerConfigurations.put(config.getName(), config));
    }

    @Override
    public void onApplicationEvent(PartitionChangeEvent partitionChangeEvent) {
        if (partitionChangeEvent.getServiceType().equals(getServiceType())) {
            ServiceQueue serviceQueue = partitionChangeEvent.getServiceQueueKey().getServiceQueue();
            log.info("[{}] Subscribing to partitions: {}", serviceQueue.getQueue(), partitionChangeEvent.getPartitions());
            consumers.get(serviceQueue.getQueue()).subscribe(partitionChangeEvent.getPartitions());
        }
    }

    @Override
    protected void launchMainConsumers() {
        consumers.forEach((queue, consumer) -> launchConsumer(consumer, consumerConfigurations.get(queue), consumerStats.get(queue)));
    }

    @Override
    protected void stopMainConsumers() {
        consumers.values().forEach(TbQueueConsumer::unsubscribe);
    }

    private void launchConsumer(TbQueueConsumer<TbProtoQueueMsg<ToRuleEngineMsg>> consumer, TbRuleEngineQueueConfiguration configuration, TbRuleEngineConsumerStats stats) {
        consumersExecutor.execute(() -> {
            while (!stopped) {
                try {
                    List<TbProtoQueueMsg<ToRuleEngineMsg>> msgs = consumer.poll(pollDuration);
                    if (msgs.isEmpty()) {
                        continue;
                    }
                    TbRuleEngineSubmitStrategy submitStrategy = submitStrategyFactory.newInstance(configuration.getName(), configuration.getSubmitStrategy());
                    TbRuleEngineProcessingStrategy ackStrategy = processingStrategyFactory.newInstance(configuration.getName(), configuration.getProcessingStrategy());

                    submitStrategy.init(msgs);

                    while (!stopped) {
                        TbMsgPackProcessingContext ctx = new TbMsgPackProcessingContext(submitStrategy);
                        submitStrategy.submitAttempt((id, msg) -> submitExecutor.submit(() -> {
                            log.trace("[{}] Creating callback for message: {}", id, msg.getValue());
                            ToRuleEngineMsg toRuleEngineMsg = msg.getValue();
                            TenantId tenantId = new TenantId(new UUID(toRuleEngineMsg.getTenantIdMSB(), toRuleEngineMsg.getTenantIdLSB()));
                            TbMsgCallback callback = statsEnabled ?
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
                        }));

                        boolean timeout = false;
                        if (!ctx.await(configuration.getPackProcessingTimeout(), TimeUnit.MILLISECONDS)) {
                            timeout = true;
                        }

                        TbRuleEngineProcessingResult result = new TbRuleEngineProcessingResult(configuration.getName(), timeout, ctx);
                        if (timeout) {
                            printFirstOrAll(configuration, ctx, ctx.getPendingMap(), "Timeout");
                        }
                        if (!ctx.getFailedMap().isEmpty()) {
                            printFirstOrAll(configuration, ctx, ctx.getFailedMap(), "Failed");
                        }
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
        });
    }

    private void printFirstOrAll(TbRuleEngineQueueConfiguration configuration, TbMsgPackProcessingContext ctx, Map<UUID, TbProtoQueueMsg<ToRuleEngineMsg>> map, String prefix) {
        boolean printAll = log.isTraceEnabled();
        log.info("{} to process [{}] messages", prefix, map.size());
        for (Map.Entry<UUID, TbProtoQueueMsg<ToRuleEngineMsg>> pending : map.entrySet()) {
            ToRuleEngineMsg tmp = pending.getValue().getValue();
            TbMsg tmpMsg = TbMsg.fromBytes(configuration.getName(), tmp.getTbMsg().toByteArray(), TbMsgCallback.EMPTY);
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
    protected void handleNotification(UUID id, TbProtoQueueMsg<ToRuleEngineNotificationMsg> msg, TbCallback callback) throws Exception {
        ToRuleEngineNotificationMsg nfMsg = msg.getValue();
        if (nfMsg.getComponentLifecycleMsg() != null && !nfMsg.getComponentLifecycleMsg().isEmpty()) {
            Optional<TbActorMsg> actorMsg = encodingService.decode(nfMsg.getComponentLifecycleMsg().toByteArray());
            if (actorMsg.isPresent()) {
                log.trace("[{}] Forwarding message to App Actor {}", id, actorMsg.get());
                actorContext.tellWithHighPriority(actorMsg.get());
            }
            callback.onSuccess();
        } else if (nfMsg.hasFromDeviceRpcResponse()) {
            TransportProtos.FromDeviceRPCResponseProto proto = nfMsg.getFromDeviceRpcResponse();
            RpcError error = proto.getError() > 0 ? RpcError.values()[proto.getError()] : null;
            FromDeviceRpcResponse response = new FromDeviceRpcResponse(new UUID(proto.getRequestIdMSB(), proto.getRequestIdLSB())
                    , proto.getResponse(), error);
            tbDeviceRpcService.processRpcResponseFromDevice(response);
            callback.onSuccess();
        } else {
            log.trace("Received notification with missing handler");
            callback.onSuccess();
        }
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
