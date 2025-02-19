/**
 * Copyright © 2016-2024 The Thingsboard Authors
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

import lombok.Getter;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.queue.TbQueueMsg;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class QueueStateService<E extends TbQueueMsg, S extends TbQueueMsg> {

    private PartitionedQueueConsumerManager<S> stateConsumer;
    private PartitionedQueueConsumerManager<E> eventConsumer;

    @Getter
    private Set<TopicPartitionInfo> partitions;
    private final Lock lock = new ReentrantLock();

    public void init(PartitionedQueueConsumerManager<S> stateConsumer, PartitionedQueueConsumerManager<E> eventConsumer) {
        this.stateConsumer = stateConsumer;
        this.eventConsumer = eventConsumer;
    }

    public void update(Set<TopicPartitionInfo> newPartitions) {
        newPartitions = withTopic(newPartitions, stateConsumer.getTopic());
        lock.lock();
        Set<TopicPartitionInfo> oldPartitions = this.partitions != null ? this.partitions : Collections.emptySet();
        Set<TopicPartitionInfo> addedPartitions;
        Set<TopicPartitionInfo> removedPartitions;
        try {
            addedPartitions = new HashSet<>(newPartitions);
            addedPartitions.removeAll(oldPartitions);
            removedPartitions = new HashSet<>(oldPartitions);
            removedPartitions.removeAll(newPartitions);
            this.partitions = newPartitions;
        } finally {
            lock.unlock();
        }

        if (!removedPartitions.isEmpty()) {
            stateConsumer.removePartitions(removedPartitions);
            eventConsumer.removePartitions(withTopic(removedPartitions, eventConsumer.getTopic()));
        }

        if (!addedPartitions.isEmpty()) {
            stateConsumer.addPartitions(addedPartitions, partition -> {
                lock.lock();
                try {
                    if (this.partitions.contains(partition)) {
                        eventConsumer.addPartitions(Set.of(partition.newByTopic(eventConsumer.getTopic())));
                    }
                } finally {
                    lock.unlock();
                }
            });
        }
    }

    private Set<TopicPartitionInfo> withTopic(Set<TopicPartitionInfo> partitions, String topic) {
        return partitions.stream().map(tpi -> tpi.newByTopic(topic)).collect(Collectors.toSet());
    }

}
