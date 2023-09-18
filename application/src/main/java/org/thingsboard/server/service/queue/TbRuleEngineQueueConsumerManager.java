package org.thingsboard.server.service.queue;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.data.queue.Queue;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.common.stats.StatsFactory;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.QueueKey;
import org.thingsboard.server.queue.discovery.event.PartitionChangeEvent;
import org.thingsboard.server.queue.provider.TbRuleEngineQueueFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Data
@Slf4j
public class TbRuleEngineQueueConsumerManager {

    private final ScheduledExecutorService scheduler;
    private final ExecutorService consumerExecutor;
    private final StatsFactory statsFactory;
    private final TbRuleEngineQueueFactory queueFactory;
    private final QueueKey key;
    private final ReentrantLock lock = new ReentrantLock(); //NonfairSync
    private final ConcurrentMap<TopicPartitionInfo, TbQueueConsumerLauncher> consumers = new ConcurrentHashMap<>();
    private final TbRuleEngineConsumerStats stats;

    private volatile Set<TopicPartitionInfo> partitions = Collections.emptySet();
    private volatile Queue queue;
    private volatile TbQueueConsumerLauncher mainConsumer;

    private final java.util.Queue<TbQueueConsumerManagerTask> tasks = new ConcurrentLinkedQueue<>();

    public TbRuleEngineQueueConsumerManager(ScheduledExecutorService scheduler, ExecutorService consumerExecutor, StatsFactory statsFactory, TbRuleEngineQueueFactory queueFactory, QueueKey key) {
        this.scheduler = scheduler;
        this.consumerExecutor = consumerExecutor;
        this.statsFactory = statsFactory;
        this.queueFactory = queueFactory;
        this.key = key;
        this.stats = new TbRuleEngineConsumerStats(key, statsFactory);
    }

    public void init(Queue queue) {
        processTask(new TbQueueConsumerManagerTask(ComponentLifecycleEvent.CREATED, queue, null));
    }

    private void processTask(TbQueueConsumerManagerTask todo) {
        tasks.add(todo);
        log.info("[{}] Adding task: {}", key, todo);
        tryProcessTasks();
    }

    private void tryProcessTasks() {
        consumerExecutor.submit(() -> {
            if (lock.tryLock()) {
                try {
                    TbQueueConsumerManagerTask lastUpdateTask = null;
                    while (!tasks.isEmpty()) {
                        TbQueueConsumerManagerTask task = tasks.poll();
                        switch (task.getEvent()) {
                            case CREATED:
                                doInit(task.getQueue());
                                break;
                            case UPDATED:
                                lastUpdateTask = task;
                                break;
                            case DELETED:
                                lastUpdateTask = null;
                                doDelete();
                                break;
                        }
                    }
                    if (lastUpdateTask != null) {
                        doUpdate(lastUpdateTask.getQueue(), lastUpdateTask.getPartitions());
                    }
                } finally {
                    lock.unlock();
                }
            } else {
                log.debug("[{}] Failed to acquire lock.", key);
                scheduler.schedule(this::tryProcessTasks, 1, TimeUnit.SECONDS);
            }
        });
    }

    public void doInit(Queue queue) {
        log.info("[{}] Init consumer with queue: {}", key, queue);
        this.queue = queue;
        if (!queue.isConsumerPerPartition()) {
            mainConsumer = new TbQueueConsumerLauncher(queueFactory.createToRuleEngineMsgConsumer(queue));
        }
    }

    private void doUpdate(Queue newQueue, Set<TopicPartitionInfo> partitions) {
        if (newQueue.isConsumerPerPartition()) {

        } else {
            for (var oldConsumer : consumers.values()) {
                oldConsumer.stop();
            }
            for (var oldConsumer : consumers.entrySet()) {
                try {
                    oldConsumer.getValue().awaitStopped();
                } catch (Exception e) {
                    log.info("[{}][{}] Failed to stop the consumer during update", key, oldConsumer.getKey().getPartition().orElse(-1), e);
                }
            }
            if (mainConsumer == null) {
                mainConsumer = new TbQueueConsumerLauncher(queueFactory.createToRuleEngineMsgConsumer(queue));
                //TODO: launch
            }
            mainConsumer.subscribe(partitions);

        }
    }

    private void doDelete() {
    }

    public void subscribe(PartitionChangeEvent event) {
        log.info("[{}] Subscribing to partitions: {}", key, event.getPartitions());
        if (!queue.isConsumerPerPartition()) {
            mainConsumer.subscribe(event.getPartitions());
        } else {
            log.info("[{}] Subscribing consumer per partition: {}", key, event.getPartitions());
            subscribeConsumerPerPartition(event.getQueueKey(), event.getPartitions());
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
                ConcurrentMap<TopicPartitionInfo, TbQueueConsumer<TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>>> consumers = tbTopicWithConsumerPerPartition.getConsumers();
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
                    TbQueueConsumer<TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>> consumer = tbRuleEngineQueueFactory.createToRuleEngineMsgConsumer(configuration);
                    consumers.put(tpi, consumer);
                    launchConsumer(consumer, queueKey, tpi.getFullTopicName(), queueKey + "-" + tpi.getPartition().orElse(-999999));
                    consumer.subscribe(Collections.singleton(tpi));
                });
            } finally {
                tbTopicWithConsumerPerPartition.getLock().unlock();
            }
        } else {
            scheduleTopicRepartition(queueKey); //reschedule later
        }
    }

    public void stop() {
//        consumers.values().forEach(TbQueueConsumer::stop);
//        topicsConsumerPerPartition.values().forEach(tbTopicWithConsumerPerPartition -> tbTopicWithConsumerPerPartition.getConsumers().keySet()
//                .forEach((tpi) -> removeConsumerForTopicByTpi(tbTopicWithConsumerPerPartition.getTopic(), tbTopicWithConsumerPerPartition.getConsumers(), tpi)));
    }

}
