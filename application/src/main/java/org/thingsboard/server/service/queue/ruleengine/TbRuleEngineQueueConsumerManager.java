// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.queue.ruleengine;

import com.google.protobuf.ProtocolStringList;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.queue.Queue;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.gen.MsgProtos;
import org.thingsboard.server.common.msg.queue.QueueToRuleEngineMsg;
import org.thingsboard.server.common.msg.queue.RuleEngineException;
import org.thingsboard.server.common.msg.queue.RuleNodeInfo;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TbMsgCallback;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.common.util.ProtoUtils;
import org.thingsboard.server.gen.transport.TransportProtos.ToRuleEngineMsg;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.common.consumer.MainQueueConsumerManager;
import org.thingsboard.server.queue.common.consumer.TbQueueConsumerManagerTask;
import org.thingsboard.server.queue.common.consumer.TbQueueConsumerManagerTask.DeleteQueueTask;
import org.thingsboard.server.queue.common.consumer.TbQueueConsumerTask;
import org.thingsboard.server.queue.common.consumer.TbQueueConsumerTask.ConsumerKey;
import org.thingsboard.server.queue.discovery.QueueKey;
import org.thingsboard.server.service.queue.TbMsgPackCallback;
import org.thingsboard.server.service.queue.TbMsgPackProcessingContext;
import org.thingsboard.server.service.queue.TbMsgPackProcessingContextFactory;
import org.thingsboard.server.service.queue.TbRuleEngineConsumerStats;
import org.thingsboard.server.service.queue.TraceRetryMetadata;
import org.thingsboard.server.service.queue.processing.TbRuleEngineProcessingDecision;
import org.thingsboard.server.service.queue.processing.TbRuleEngineProcessingResult;
import org.thingsboard.server.service.queue.processing.TbRuleEngineProcessingStrategy;
import org.thingsboard.server.service.queue.processing.TbRuleEngineSubmitStrategy;
import org.thingsboard.server.service.ruleenginemonitoring.RuleEngineMonitoringService;
import org.thingsboard.server.service.trace.TraceMessageService;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
public class TbRuleEngineQueueConsumerManager extends MainQueueConsumerManager<TbProtoQueueMsg<ToRuleEngineMsg>, Queue> {

    public static final String SUCCESSFUL_STATUS = "successful";
    public static final String FAILED_STATUS = "failed";

    private final TbRuleEngineConsumerContext ctx;
    private final TbRuleEngineConsumerStats stats;
    private final TbMsgPackProcessingContextFactory packProcessingContextFactory;
    private final TraceMessageService traceMessageService;
    private final RuleEngineMonitoringService ruleEngineMonitoringService;

    @Builder(builderMethodName = "create") // not to conflict with super.builder()
    public TbRuleEngineQueueConsumerManager(TbRuleEngineConsumerContext ctx,
                                            QueueKey queueKey,
                                            ExecutorService consumerExecutor,
                                            ScheduledExecutorService scheduler,
                                            ExecutorService taskExecutor,
                                            TbMsgPackProcessingContextFactory packProcessingContextFactory,
                                            TraceMessageService traceMessageService,
                                            RuleEngineMonitoringService ruleEngineMonitoringService) {
        super(queueKey, null, null,
                (queueConfig, tpi) -> {
                    Integer partitionId = tpi != null ? tpi.getPartition().orElse(-1) : null;
                    return ctx.getQueueFactory().createToRuleEngineMsgConsumer(queueConfig, partitionId);
                },
                consumerExecutor, scheduler, taskExecutor, null);
        this.ctx = ctx;
        this.stats = new TbRuleEngineConsumerStats(queueKey, ctx.getStatsFactory());
        this.packProcessingContextFactory = packProcessingContextFactory;
        this.traceMessageService = traceMessageService;
        this.ruleEngineMonitoringService = ruleEngineMonitoringService;
    }

    public void delete(boolean drainQueue) {
        addTask(new DeleteQueueTask(drainQueue));
    }

    @Override
    protected void processTask(TbQueueConsumerManagerTask task) {
        if (task instanceof DeleteQueueTask deleteQueueTask) {
            doDelete(deleteQueueTask.drainQueue());
        }
    }

    private void doDelete(boolean drainQueue) {
        stopped = true;
        log.info("[{}] Handling queue deletion", queueKey);
        consumerWrapper.getConsumers().forEach(TbQueueConsumerTask::awaitCompletion);

        List<TbQueueConsumer<TbProtoQueueMsg<ToRuleEngineMsg>>> queueConsumers = consumerWrapper.getConsumers().stream()
                .map(TbQueueConsumerTask::getConsumer).collect(Collectors.toList());
        consumerExecutor.submit(() -> {
            if (drainQueue) {
                drainQueue(queueConsumers);
            }

            queueConsumers.forEach(consumer -> {
                for (String topic : consumer.getFullTopicNames()) {
                    try {
                        ctx.getQueueAdmin().deleteTopic(topic);
                        log.info("Deleted topic {}", topic);
                    } catch (Exception e) {
                        log.error("Failed to delete topic {}", topic, e);
                    }
                }
                try {
                    consumer.unsubscribe();
                } catch (Exception e) {
                    log.error("[{}] Failed to unsubscribe consumer", queueKey, e);
                }
            });
        });
    }

    @Override
    protected void processMsgs(List<TbProtoQueueMsg<ToRuleEngineMsg>> msgs,
                               TbQueueConsumer<TbProtoQueueMsg<ToRuleEngineMsg>> consumer,
                               ConsumerKey consumerKey,
                               Queue queue) throws Exception {
        TbRuleEngineSubmitStrategy submitStrategy = getSubmitStrategy(queue);
        TbRuleEngineProcessingStrategy ackStrategy = getProcessingStrategy(queue);
        submitStrategy.init(msgs);
        ConcurrentMap<UUID, TraceRetryMetadata> msgIdToTraceId = null;
        if (traceMessageService != null) {
            msgIdToTraceId = new ConcurrentHashMap<>();
        }
        // Each attempt gets a new pack context, so the retry flag has to outlive it: a retry must not re-sample traces.
        boolean retry = false;
        while (!stopped && !consumer.isStopped()) {
            TbMsgPackProcessingContext packCtx = packProcessingContextFactory.create(queue.getName(), submitStrategy, ackStrategy.isSkipTimeoutMsgs());
            packCtx.setMsgIdAndTraceIdMap(msgIdToTraceId);
            packCtx.setHasRetry(retry);
            // Decide trace-id coverage for the whole pack once, before per-message submission.
            if (traceMessageService != null) {
                traceMessageService.onMessagePack(queue.getTenantId(), packCtx);
            }
            submitStrategy.submitAttempt((id, msg) -> submitMessage(packCtx, id, msg));

            final boolean timeout = !packCtx.await(queue.getPackProcessingTimeout(), TimeUnit.MILLISECONDS);

            TbRuleEngineProcessingResult result = new TbRuleEngineProcessingResult(queue.getName(), timeout, packCtx);
            if (timeout) {
                printFirstOrAll(packCtx, packCtx.getPendingMap(), "Timeout");
                if (traceMessageService != null) {
                    traceMessageService.onTimeout(packCtx);
                }
            }
            if (!packCtx.getFailedMap().isEmpty()) {
                printFirstOrAll(packCtx, packCtx.getFailedMap(), "Failed");
            }
            packCtx.printProfilerStats();

            TbRuleEngineProcessingDecision decision = ackStrategy.analyze(result);
            if (ctx.isStatsEnabled()) {
                stats.log(result, decision.isCommit());
            }

            reportToRuleEngineMonitoring(packCtx, queue, decision.isCommit() ? Set.of() : decision.getReprocessMap().keySet());

            packCtx.cleanup();

            if (decision.isCommit()) {
                submitStrategy.stop();
                consumer.commit();
                break;
            } else {
                retry = true;
                submitStrategy.update(decision.getReprocessMap());
            }
        }
    }

    private void reportToRuleEngineMonitoring(TbMsgPackProcessingContext packCtx, Queue queue, Set<UUID> toReprocess) {
        try {
            if (ruleEngineMonitoringService == null) {
                return;
            }
            ruleEngineMonitoringService.recordQueueOutcomes(packCtx, queue, ctx.getServiceInfoProvider().getServiceId(), toReprocess);
        } catch (Exception e) {
            log.error("Failed to report queue outcome to monitoring service", e);
        }
    }

    private TbRuleEngineSubmitStrategy getSubmitStrategy(Queue queue) {
        return ctx.getSubmitStrategyFactory().newInstance(queue.getName(), queue.getSubmitStrategy());
    }

    private TbRuleEngineProcessingStrategy getProcessingStrategy(Queue queue) {
        return ctx.getProcessingStrategyFactory().newInstance(queue.getName(), queue.getProcessingStrategy());
    }

    private void submitMessage(TbMsgPackProcessingContext packCtx, UUID id, TbProtoQueueMsg<ToRuleEngineMsg> msg) {
        log.trace("[{}] Creating callback for topic {} message: {}", id, config.getName(), msg.getValue());
        ToRuleEngineMsg toRuleEngineMsg = msg.getValue();
        TenantId tenantId = TenantId.fromUUID(new UUID(toRuleEngineMsg.getTenantIdMSB(), toRuleEngineMsg.getTenantIdLSB()));
        TbMsgCallback callback = ctx.isPrometheusStatsEnabled() ?
                new TbMsgPackCallback(id, tenantId, packCtx, stats.getTimer(tenantId, SUCCESSFUL_STATUS), stats.getTimer(tenantId, FAILED_STATUS)) :
                new TbMsgPackCallback(id, tenantId, packCtx);
        try {
            if (!toRuleEngineMsg.getTbMsg().isEmpty() || toRuleEngineMsg.hasTbMsgProto()) {
                forwardToRuleEngineActor(packCtx, config.getName(), tenantId, toRuleEngineMsg, callback);
            } else {
                callback.onSuccess();
            }
        } catch (Exception e) {
            callback.onFailure(new RuleEngineException(e.getMessage(), e));
        }
    }

    private void forwardToRuleEngineActor(TbMsgPackProcessingContext packCtx, String queueName, TenantId tenantId, ToRuleEngineMsg toRuleEngineMsg, TbMsgCallback callback) {
        TbMsg tbMsg = ProtoUtils.fromTbMsgProto(queueName, toRuleEngineMsg, callback);
        if (traceMessageService != null) {
            traceMessageService.onForwardToRuleEngineActor(tenantId, packCtx, tbMsg);
        }
        QueueToRuleEngineMsg msg;
        ProtocolStringList relationTypesList = toRuleEngineMsg.getRelationTypesList();
        Set<String> relationTypes;
        if (relationTypesList.size() == 1) {
            relationTypes = Collections.singleton(relationTypesList.get(0));
        } else {
            relationTypes = new HashSet<>(relationTypesList);
        }
        msg = new QueueToRuleEngineMsg(tenantId, tbMsg, relationTypes, toRuleEngineMsg.getFailureMessage());
        ctx.getActorContext().tell(msg);
    }

    private void printFirstOrAll(TbMsgPackProcessingContext ctx, Map<UUID, TbProtoQueueMsg<ToRuleEngineMsg>> map, String prefix) {
        boolean printAll = log.isTraceEnabled();
        log.info("[{}] {} to process [{}] messages", queueKey, prefix, map.size());
        for (Map.Entry<UUID, TbProtoQueueMsg<ToRuleEngineMsg>> pending : map.entrySet()) {
            ToRuleEngineMsg tmp = pending.getValue().getValue();
            TbMsg tmpMsg = ProtoUtils.fromTbMsgProto(config.getName(), tmp, TbMsgCallback.EMPTY);
            RuleNodeInfo ruleNodeInfo = ctx.getLastVisitedRuleNode(pending.getKey());
            if (printAll) {
                log.trace("[{}][{}] {} to process message: {}, Last Rule Node: {}", queueKey, TenantId.fromUUID(new UUID(tmp.getTenantIdMSB(), tmp.getTenantIdLSB())), prefix, tmpMsg, ruleNodeInfo);
            } else {
                log.info("[{}] {} to process message: {}, Last Rule Node: {}", TenantId.fromUUID(new UUID(tmp.getTenantIdMSB(), tmp.getTenantIdLSB())), prefix, tmpMsg, ruleNodeInfo);
                break;
            }
        }
    }

    public void printStats(long ts) {
        stats.printStats();
        ctx.getStatisticsService().reportQueueStats(ts, stats);
        stats.reset();
    }

    private void drainQueue(List<TbQueueConsumer<TbProtoQueueMsg<ToRuleEngineMsg>>> consumers) {
        long finishTs = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(ctx.getTopicDeletionDelayInSec());
        try {
            int n = 0;
            while (System.currentTimeMillis() <= finishTs) {
                for (TbQueueConsumer<TbProtoQueueMsg<ToRuleEngineMsg>> consumer : consumers) {
                    List<TbProtoQueueMsg<ToRuleEngineMsg>> msgs = consumer.poll(config.getPollInterval());
                    if (msgs.isEmpty()) {
                        continue;
                    }
                    for (TbProtoQueueMsg<ToRuleEngineMsg> msg : msgs) {
                        try {
                            MsgProtos.TbMsgProto tbMsgProto = ProtoUtils.getTbMsgProto(msg.getValue());
                            EntityId originator = EntityIdFactory.getByTypeAndUuid(tbMsgProto.getEntityType(), new UUID(tbMsgProto.getEntityIdMSB(), tbMsgProto.getEntityIdLSB()));

                            TopicPartitionInfo tpi = ctx.getPartitionService().resolve(ServiceType.TB_RULE_ENGINE, config.getName(), TenantId.SYS_TENANT_ID, originator);
                            ctx.getProducerProvider().getRuleEngineMsgProducer().send(tpi, msg, null);
                            n++;
                        } catch (Throwable e) {
                            log.warn("Failed to move message to system {}: {}", consumer.getTopic(), msg, e);
                        }
                    }
                    consumer.commit();
                }
            }
            if (n > 0) {
                log.info("Moved {} messages from {} to system {}", n, queueKey, config.getName());
            }
        } catch (Exception e) {
            log.error("[{}] Failed to drain queue", queueKey, e);
        }
    }

}
