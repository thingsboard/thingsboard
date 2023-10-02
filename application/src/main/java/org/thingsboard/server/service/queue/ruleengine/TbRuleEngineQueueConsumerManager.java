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
package org.thingsboard.server.service.queue.ruleengine;

import com.google.protobuf.ProtocolStringList;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
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
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.QueueKey;
import org.thingsboard.server.queue.discovery.event.PartitionChangeEvent;
import org.thingsboard.server.service.queue.TbMsgPackCallback;
import org.thingsboard.server.service.queue.TbMsgPackProcessingContext;
import org.thingsboard.server.service.queue.TbRuleEngineConsumerStats;
import org.thingsboard.server.service.queue.processing.TbRuleEngineProcessingDecision;
import org.thingsboard.server.service.queue.processing.TbRuleEngineProcessingResult;
import org.thingsboard.server.service.queue.processing.TbRuleEngineProcessingStrategy;
import org.thingsboard.server.service.queue.processing.TbRuleEngineSubmitStrategy;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Data
@Slf4j
public class TbRuleEngineQueueConsumerManager {

    public static final String SUCCESSFUL_STATUS = "successful";
    public static final String FAILED_STATUS = "failed";

    private final TbRuleEngineConsumerContext ctx;
    private final QueueKey key;

    private final ReentrantLock lock = new ReentrantLock(); //NonfairSync
    private final ConcurrentMap<TopicPartitionInfo, TbQueueConsumerTask> consumersPerPartition = new ConcurrentHashMap<>();
    private final TbRuleEngineConsumerStats stats;

    private volatile Set<TopicPartitionInfo> partitions = Collections.emptySet();
    private volatile Queue queue;
    private volatile TbQueueConsumerTask mainConsumer;

    private final java.util.Queue<TbQueueConsumerManagerTask> tasks = new ConcurrentLinkedQueue<>();

    public TbRuleEngineQueueConsumerManager(TbRuleEngineConsumerContext ctx, QueueKey key) {
        this.ctx = ctx;
        this.key = key;
        this.stats = new TbRuleEngineConsumerStats(key, ctx.getStatsFactory());
    }

    public void init(Queue queue) {
        processTask(new TbQueueConsumerManagerTask(QueueEvent.CREATED, queue, null));
    }

    public void update(Queue queue) {
        processTask(new TbQueueConsumerManagerTask(QueueEvent.UPDATED, queue, null));
    }

    public void subscribe(PartitionChangeEvent event) {
        processTask(new TbQueueConsumerManagerTask(QueueEvent.PARTITION_CHANGE, queue, event.getPartitions()));
    }

    public void launchMainConsumer() {
        processTask(new TbQueueConsumerManagerTask(QueueEvent.LAUNCHED, null, null));
    }

    public void stop() {
        processTask(new TbQueueConsumerManagerTask(QueueEvent.STOP, null, null));
    }

    public void delete() {
        processTask(new TbQueueConsumerManagerTask(QueueEvent.DELETED, null, null));
    }

    private void processTask(TbQueueConsumerManagerTask todo) {
        tasks.add(todo);
        log.info("[{}] Adding task: {}", key, todo);
        tryProcessTasks();
    }

    private void tryProcessTasks() {
        ctx.getMgmtExecutor().submit(() -> {
            if (lock.tryLock()) {
                try {
                    Queue newConfiguration = null;
                    Set<TopicPartitionInfo> newPartitions = null;
                    while (!tasks.isEmpty()) {
                        TbQueueConsumerManagerTask task = tasks.poll();
                        switch (task.getEvent()) {
                            case CREATED:
                                doInit(task.getQueue());
                                break;
                            case LAUNCHED:
                                if (!queue.isConsumerPerPartition()) {
                                    doLaunchMainConsumer();
                                }
                                break;
                            case UPDATED:
                                newConfiguration = task.getQueue();
                                break;
                            case PARTITION_CHANGE:
                                newPartitions = task.getPartitions();
                                break;
                            case STOP:
                                newConfiguration = null;
                                newPartitions = null;
                                doStop();
                                break;
                            case DELETED:
                                newConfiguration = null;
                                newPartitions = null;
                                doDelete();
                                break;
                        }
                    }
                    if (newConfiguration != null) {
                        doUpdate(newConfiguration);
                    }
                    if (newPartitions != null) {
                        doUpdate(newPartitions);
                    }
                } finally {
                    lock.unlock();
                }
            } else {
                log.debug("[{}] Failed to acquire lock.", key);
                ctx.getScheduler().schedule(this::tryProcessTasks, 1, TimeUnit.SECONDS);
            }
        });
    }

    public void doInit(Queue queue) {
        log.info("[{}] Init consumer with queue: {}", key, queue);
        this.queue = queue;
        if (queue.isConsumerPerPartition()) {
            log.debug("[{}] Ignore init event since isConsumerPerPartition is enabled.", key);
        } else {
            mainConsumer = new TbQueueConsumerTask(key, "main", ctx.getQueueFactory().createToRuleEngineMsgConsumer(queue));
        }
    }

    private void doLaunchMainConsumer() {
        if (mainConsumer != null) {
            launchConsumer(mainConsumer, queue, mainConsumer.getId(), queue.getName());
        } else {
            log.warn("[{}] Can't launch main consumer since it is empty!", key);
        }
    }

    private void doUpdate(Queue newQueue) {
        log.info("[{}] Processing queue update: {}", key, newQueue);
        var oldQueue = queue;
        if (log.isTraceEnabled()) {
            log.trace("[{}] Old queue configuration: {}", key, oldQueue);
            log.trace("[{}] New queue configuration: {}", key, newQueue);
        }
        if (oldQueue != null) {
            doStop(oldQueue);
        }
        doInit(newQueue);
        if (!newQueue.isConsumerPerPartition()) {
            doLaunchMainConsumer();
        }
    }

    private void doUpdate(Set<TopicPartitionInfo> partitions) {
        log.info("[{}] Subscribing to partitions: {}", key, partitions);
        if (queue.isConsumerPerPartition()) {
            log.debug("[{}] Subscribing consumers per partition separately: {}", key, partitions);
            Set<TopicPartitionInfo> addedPartitions = new HashSet<>(partitions);
            addedPartitions.removeAll(consumersPerPartition.keySet());
            log.info("calculated addedPartitions {}", addedPartitions);

            Set<TopicPartitionInfo> removedPartitions = new HashSet<>(consumersPerPartition.keySet());
            removedPartitions.removeAll(partitions);
            log.info("calculated removedPartitions {}", removedPartitions);

            removedPartitions.forEach((tpi) -> {
                log.info("[{}] Unsubscribing from topic: {}", queue, tpi);
                consumersPerPartition.get(tpi).unsubscribe();
            });

            removedPartitions.forEach((tpi) -> {
                log.info("[{}] Removing consumer for topic: {}", queue, tpi);
                consumersPerPartition.get(tpi).stopAndAwait();
                consumersPerPartition.remove(tpi);
            });

            addedPartitions.forEach((tpi) -> {
                log.info("[{}] Adding consumer for topic: {}", key, tpi);
                TbQueueConsumerTask consumerTask = new TbQueueConsumerTask(key, tpi, ctx.getQueueFactory().createToRuleEngineMsgConsumer(queue));
                consumersPerPartition.put(tpi, consumerTask);
                //TODO: Is it ok to subscribe first?
                consumerTask.subscribe(Collections.singleton(tpi));
                launchConsumer(consumerTask, queue, mainConsumer.getId(), key + "-" + tpi.getPartition().orElse(-999999));
            });
        } else {
            mainConsumer.subscribe(partitions);
        }
    }

    private void doStop() {
        doStop(queue);
    }

    private void doStop(Queue queue) {
        if (queue.isConsumerPerPartition()) {
            consumersPerPartition.values().forEach(TbQueueConsumerTask::unsubscribe);
            consumersPerPartition.values().forEach(TbQueueConsumerTask::stopAndAwait);
        } else if (mainConsumer != null) {
            mainConsumer.unsubscribe();
            mainConsumer.stopAndAwait();
        }
    }

    private void doDelete() {
        doStop();
        //TODO: repack messages
    }

    @SneakyThrows
    void launchConsumer(TbQueueConsumerTask consumerTask, Queue configuration, Object consumerKey, String threadSuffix) {
        log.info("[{}] Launching consumer: [{}]", key, consumerKey);
        while (!ctx.isReady) {
            //TODO: Remember this task. Cancel previous task if needed.
            log.debug("[{}][{}] Waiting for consumer to get ready..", key, consumerKey);
            Thread.sleep(1000);
        }
        consumerTask.setTask(ctx.getConsumersExecutor().submit(() -> consumerLoop(consumerTask.getConsumer(), configuration, threadSuffix)));
    }

    void consumerLoop(TbQueueConsumer<TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>> consumer, Queue configuration, String threadSuffix) {
        ThingsBoardThreadFactory.updateCurrentThreadName(threadSuffix);
        while (!ctx.stopped && !consumer.isStopped()
                //TODO: remove this.
                && !consumer.isQueueDeleted()) {
            try {
                List<TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>> msgs = consumer.poll(queue.getPollInterval());
                if (msgs.isEmpty()) {
                    continue;
                }
                final TbRuleEngineSubmitStrategy submitStrategy = getSubmitStrategy(queue);
                final TbRuleEngineProcessingStrategy ackStrategy = getProcessingStrategy(queue);
                submitStrategy.init(msgs);
                while (!ctx.isStopped() && !consumer.isStopped()) {
                    TbMsgPackProcessingContext packCtx = new TbMsgPackProcessingContext(queue.getName(), submitStrategy, ackStrategy.isSkipTimeoutMsgs());
                    submitStrategy.submitAttempt((id, msg) -> ctx.getSubmitExecutor().submit(() -> submitMessage(configuration, stats, packCtx, id, msg)));

                    final boolean timeout = !packCtx.await(configuration.getPackProcessingTimeout(), TimeUnit.MILLISECONDS);

                    TbRuleEngineProcessingResult result = new TbRuleEngineProcessingResult(configuration.getName(), timeout, packCtx);
                    if (timeout) {
                        printFirstOrAll(configuration, packCtx, packCtx.getPendingMap(), "Timeout");
                    }
                    if (!packCtx.getFailedMap().isEmpty()) {
                        printFirstOrAll(configuration, packCtx, packCtx.getFailedMap(), "Failed");
                    }
                    packCtx.printProfilerStats();

                    TbRuleEngineProcessingDecision decision = ackStrategy.analyze(result);
                    if (ctx.isStatsEnabled()) {
                        stats.log(result, decision.isCommit());
                    }

                    packCtx.cleanup();

                    if (decision.isCommit()) {
                        submitStrategy.stop();
                        break;
                    } else {
                        submitStrategy.update(decision.getReprocessMap());
                    }
                }
                consumer.commit();
            } catch (Exception e) {
                if (!ctx.stopped) {
                    log.warn("Failed to process messages from queue.", e);
                    try {
                        Thread.sleep(ctx.getPollDuration());
                    } catch (InterruptedException e2) {
                        log.trace("Failed to wait until the server has capacity to handle new requests", e2);
                    }
                }
            }
        }
        //TODO: refactor and move to the "doDelete" method. Use separate consumer if needed (it is still synchronous).
        if (consumer.isQueueDeleted()) {
            processQueueDeletion(configuration, consumer);
        }
        log.info("TB Rule Engine Consumer stopped.");
    }

    private void processQueueDeletion(Queue queue, TbQueueConsumer<TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>> consumer) {
//        long finishTs = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(topicDeletionDelayInSec);
//        try {
//            int n = 0;
//            while (System.currentTimeMillis() <= finishTs) {
//                List<TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>> msgs = consumer.poll(queue.getPollInterval());
//                if (msgs.isEmpty()) {
//                    continue;
//                }
//                for (TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg> msg : msgs) {
//                    try {
//                        MsgProtos.TbMsgProto tbMsgProto = MsgProtos.TbMsgProto.parseFrom(msg.getValue().getTbMsg().toByteArray());
//                        EntityId originator = EntityIdFactory.getByTypeAndUuid(tbMsgProto.getEntityType(), new UUID(tbMsgProto.getEntityIdMSB(), tbMsgProto.getEntityIdLSB()));
//
//                        TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_RULE_ENGINE, queue.getName(), TenantId.SYS_TENANT_ID, originator);
//                        producerProvider.getRuleEngineMsgProducer().send(tpi, msg, null);
//                        n++;
//                    } catch (Throwable e) {
//                        log.debug("Failed to move message to system {}: {}", consumer.getTopic(), msg, e);
//                    }
//                }
//                consumer.commit();
//            }
//            if (n > 0) {
//                log.info("Moved {} messages from {} to system {}", n, consumer.getFullTopicNames(), consumer.getTopic());
//            }
//
//            consumer.unsubscribe();
//            for (String topic : consumer.getFullTopicNames()) {
//                try {
//                    queueAdmin.deleteTopic(topic);
//                    log.info("Deleted topic {}", topic);
//                } catch (Exception e) {
//                    log.error("Failed to delete topic {} after unsubscribing", topic, e);
//                }
//            }
//        } catch (Exception e) {
//            log.error("Failed to process deletion of {} ({})", consumer.getTopic(), queue.getTenantId(), e);
//        }
    }

    TbRuleEngineSubmitStrategy getSubmitStrategy(Queue configuration) {
        return ctx.getSubmitStrategyFactory().newInstance(configuration.getName(), configuration.getSubmitStrategy());
    }

    TbRuleEngineProcessingStrategy getProcessingStrategy(Queue configuration) {
        return ctx.getProcessingStrategyFactory().newInstance(configuration.getName(), configuration.getProcessingStrategy());
    }

    void submitMessage(Queue configuration, TbRuleEngineConsumerStats stats, TbMsgPackProcessingContext packCtx, UUID id, TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg> msg) {
        log.trace("[{}] Creating callback for topic {} message: {}", id, configuration.getName(), msg.getValue());
        TransportProtos.ToRuleEngineMsg toRuleEngineMsg = msg.getValue();
        TenantId tenantId = TenantId.fromUUID(new UUID(toRuleEngineMsg.getTenantIdMSB(), toRuleEngineMsg.getTenantIdLSB()));
        TbMsgCallback callback = ctx.prometheusStatsEnabled ?
                new TbMsgPackCallback(id, tenantId, packCtx, stats.getTimer(tenantId, SUCCESSFUL_STATUS), stats.getTimer(tenantId, FAILED_STATUS)) :
                new TbMsgPackCallback(id, tenantId, packCtx);
        try {
            if (!toRuleEngineMsg.getTbMsg().isEmpty()) {
                forwardToRuleEngineActor(configuration.getName(), tenantId, toRuleEngineMsg, callback);
            } else {
                callback.onSuccess();
            }
        } catch (Exception e) {
            callback.onFailure(new RuleEngineException(e.getMessage(), e));
        }
    }

    private void forwardToRuleEngineActor(String queueName, TenantId tenantId, TransportProtos.ToRuleEngineMsg toRuleEngineMsg, TbMsgCallback callback) {
        TbMsg tbMsg = TbMsg.fromBytes(queueName, toRuleEngineMsg.getTbMsg().toByteArray(), callback);
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


    private void printFirstOrAll(Queue configuration, TbMsgPackProcessingContext ctx, Map<UUID, TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>> map, String prefix) {
        boolean printAll = log.isTraceEnabled();
        log.info("{} to process [{}] messages", prefix, map.size());
        for (Map.Entry<UUID, TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>> pending : map.entrySet()) {
            TransportProtos.ToRuleEngineMsg tmp = pending.getValue().getValue();
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

    public void printStats(long ts) {
        stats.printStats();
        ctx.getStatisticsService().reportQueueStats(ts, stats);
        stats.reset();
    }
}
