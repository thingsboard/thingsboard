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
package org.thingsboard.server.queue.common.consumer;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.queue.QueueConfig;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.queue.TbQueueAdmin;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.TbQueueMsg;
import org.thingsboard.server.queue.common.consumer.TbQueueConsumerManagerTask.AddPartitionsTask;
import org.thingsboard.server.queue.common.consumer.TbQueueConsumerManagerTask.DeletePartitionsTask;
import org.thingsboard.server.queue.common.consumer.TbQueueConsumerManagerTask.RemovePartitionsTask;

import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

@Slf4j
public class PartitionedQueueConsumerManager<M extends TbQueueMsg> extends MainQueueConsumerManager<M, QueueConfig> {

    private final ConsumerPerPartitionWrapper consumerWrapper;
    private final TbQueueAdmin queueAdmin;
    @Getter
    private final String topic;

    @Builder(builderMethodName = "create") // not to conflict with super.builder()
    public PartitionedQueueConsumerManager(Object queueKey, String topic, long pollInterval, MsgPackProcessor<M, QueueConfig> msgPackProcessor,
                                           BiFunction<QueueConfig, TopicPartitionInfo, TbQueueConsumer<M>> consumerCreator, TbQueueAdmin queueAdmin,
                                           ExecutorService consumerExecutor, ScheduledExecutorService scheduler,
                                           ExecutorService taskExecutor, Consumer<Throwable> uncaughtErrorHandler) {
        super(queueKey, QueueConfig.of(true, pollInterval), msgPackProcessor, consumerCreator, consumerExecutor, scheduler, taskExecutor, uncaughtErrorHandler);
        this.topic = topic;
        this.consumerWrapper = (ConsumerPerPartitionWrapper) super.consumerWrapper;
        this.queueAdmin = queueAdmin;
    }

    @Override
    protected void processTask(TbQueueConsumerManagerTask task) {
        if (task instanceof AddPartitionsTask addPartitionsTask) {
            log.info("[{}] Added partitions: {}", queueKey, addPartitionsTask.partitions());
            consumerWrapper.addPartitions(addPartitionsTask.partitions(), addPartitionsTask.onStop(), addPartitionsTask.startOffsetProvider());
        } else if (task instanceof RemovePartitionsTask removePartitionsTask) {
            log.info("[{}] Removed partitions: {}", queueKey, removePartitionsTask.partitions());
            consumerWrapper.removePartitions(removePartitionsTask.partitions());
        } else if (task instanceof DeletePartitionsTask deletePartitionsTask) {
            log.info("[{}] Removing partitions and deleting topics: {}", queueKey, deletePartitionsTask.partitions());
            consumerWrapper.removePartitions(deletePartitionsTask.partitions());
            deletePartitionsTask.partitions().forEach(tpi -> {
                String topic = tpi.getFullTopicName();
                try {
                    queueAdmin.deleteTopic(topic);
                } catch (Throwable t) {
                    log.error("Failed to delete topic {}", topic, t);
                }
            });
        }
    }

    public void addPartitions(Set<TopicPartitionInfo> partitions) {
        addPartitions(partitions, null, null);
    }

    public void addPartitions(Set<TopicPartitionInfo> partitions, Consumer<TopicPartitionInfo> onStop, Function<String, Long> startOffsetProvider) {
        addTask(new AddPartitionsTask(partitions, onStop, startOffsetProvider));
    }

    public void removePartitions(Set<TopicPartitionInfo> partitions) {
        addTask(new RemovePartitionsTask(partitions));
    }

    public void delete(Set<TopicPartitionInfo> partitions) {
        addTask(new DeletePartitionsTask(partitions));
    }

}
