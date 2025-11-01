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
package org.thingsboard.server.queue.common.state;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.queue.TbQueueMsg;
import org.thingsboard.server.queue.common.consumer.PartitionedQueueConsumerManager;
import org.thingsboard.server.queue.discovery.QueueKey;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import static org.thingsboard.server.common.msg.queue.TopicPartitionInfo.withTopic;

@Slf4j
public class KafkaQueueStateService<E extends TbQueueMsg, S extends TbQueueMsg> extends QueueStateService<E, S> {

    private final PartitionedQueueConsumerManager<S> stateConsumer;
    private final Supplier<Map<String, Long>> eventsStartOffsetsProvider;

    private final Set<TopicPartitionInfo> partitionsInProgress = ConcurrentHashMap.newKeySet();

    @Builder
    public KafkaQueueStateService(PartitionedQueueConsumerManager<E> eventConsumer,
                                  PartitionedQueueConsumerManager<S> stateConsumer,
                                  List<PartitionedQueueConsumerManager<?>> otherConsumers,
                                  Supplier<Map<String, Long>> eventsStartOffsetsProvider) {
        super(eventConsumer, otherConsumers != null ? otherConsumers : Collections.emptyList());
        this.stateConsumer = stateConsumer;
        this.eventsStartOffsetsProvider = eventsStartOffsetsProvider;
    }

    @Override
    protected void addPartitions(QueueKey queueKey, Set<TopicPartitionInfo> partitions, RestoreCallback callback) {
        Map<String, Long> eventsStartOffsets = eventsStartOffsetsProvider != null ? eventsStartOffsetsProvider.get() : null; // remembering the offsets before subscribing to states

        Set<TopicPartitionInfo> statePartitions = withTopic(partitions, stateConsumer.getTopic());
        partitionsInProgress.addAll(statePartitions);
        stateConsumer.addPartitions(statePartitions, statePartition -> {
            var readLock = partitionsLock.readLock();
            readLock.lock();
            try {
                partitionsInProgress.remove(statePartition);
                log.info("Finished partition {} (still in progress: {})", statePartition, partitionsInProgress);
                if (callback != null) {
                    callback.onPartitionRestored(statePartition);
                }
                if (partitionsInProgress.isEmpty()) {
                    log.info("All partitions processed");
                    if (callback != null) {
                        callback.onAllPartitionsRestored();
                    }
                }

                TopicPartitionInfo eventPartition = statePartition.withTopic(eventConsumer.getTopic());
                if (this.partitions.get(queueKey).contains(eventPartition)) {
                    eventConsumer.addPartitions(Set.of(eventPartition), null, eventsStartOffsets != null ? eventsStartOffsets::get : null);
                    for (PartitionedQueueConsumerManager<?> consumer : otherConsumers) {
                        consumer.addPartitions(Set.of(statePartition.withTopic(consumer.getTopic())));
                    }
                }
            } finally {
                readLock.unlock();
            }
        }, null);
    }

    @Override
    protected void removePartitions(QueueKey queueKey, Set<TopicPartitionInfo> partitions) {
        super.removePartitions(queueKey, partitions);
        stateConsumer.removePartitions(withTopic(partitions, stateConsumer.getTopic()));
    }

    @Override
    protected void deletePartitions(Set<TopicPartitionInfo> partitions) {
        super.deletePartitions(partitions);
        stateConsumer.delete(withTopic(partitions, stateConsumer.getTopic()));
    }

    @Override
    public void stop() {
        super.stop();
        stateConsumer.stop();
        stateConsumer.awaitStop();
    }

}
