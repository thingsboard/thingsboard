/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.TbQueueMsg;
import org.thingsboard.server.queue.common.consumer.TbQueueConsumerManagerTask.AddPartitionsTask;
import org.thingsboard.server.queue.common.consumer.TbQueueConsumerManagerTask.RemovePartitionsTask;
import org.thingsboard.server.queue.discovery.QueueKey;

import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.BiFunction;
import java.util.function.Consumer;

@Slf4j
public class PartitionedQueueConsumerManager<M extends TbQueueMsg> extends MainQueueConsumerManager<M, QueueConfig> {

    private final ConsumerPerPartitionWrapper consumerWrapper;
    @Getter
    private final String topic;

    @Builder(builderMethodName = "create") // not to conflict with super.builder()
    public PartitionedQueueConsumerManager(QueueKey queueKey, String topic, long pollInterval, MsgPackProcessor<M, QueueConfig> msgPackProcessor,
                                           BiFunction<QueueConfig, Integer, TbQueueConsumer<M>> consumerCreator,
                                           ExecutorService consumerExecutor, ScheduledExecutorService scheduler,
                                           ExecutorService taskExecutor, Consumer<Throwable> uncaughtErrorHandler) {
        super(queueKey, QueueConfig.of(true, pollInterval), msgPackProcessor, consumerCreator, consumerExecutor, scheduler, taskExecutor, uncaughtErrorHandler);
        this.topic = topic;
        this.consumerWrapper = (ConsumerPerPartitionWrapper) super.consumerWrapper;
    }

    @Override
    protected void processTask(TbQueueConsumerManagerTask task) {
        if (task instanceof AddPartitionsTask addPartitionsTask) {
            log.info("[{}] Added partitions: {}", queueKey, addPartitionsTask.partitions());
            consumerWrapper.addPartitions(addPartitionsTask.partitions(), addPartitionsTask.onStop());
        } else if (task instanceof RemovePartitionsTask removePartitionsTask) {
            log.info("[{}] Removed partitions: {}", queueKey, removePartitionsTask.partitions());
            consumerWrapper.removePartitions(removePartitionsTask.partitions());
        }
    }

    public void addPartitions(Set<TopicPartitionInfo> partitions) {
        addPartitions(partitions, null);
    }

    public void addPartitions(Set<TopicPartitionInfo> partitions, Consumer<TopicPartitionInfo> onStop) {
        addTask(new AddPartitionsTask(partitions, onStop));
    }

    public void removePartitions(Set<TopicPartitionInfo> partitions) {
        addTask(new RemovePartitionsTask(partitions));
    }

}
