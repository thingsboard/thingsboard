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

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.queue.TbQueueMsg;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static org.thingsboard.server.common.msg.queue.TopicPartitionInfo.withTopic;

@Slf4j
public class QueueStateService<E extends TbQueueMsg, S extends TbQueueMsg> {

    private PartitionedQueueConsumerManager<S> stateConsumer;
    private PartitionedQueueConsumerManager<E> eventConsumer;

    @Getter
    private Set<TopicPartitionInfo> partitions;
    private final Set<TopicPartitionInfo> partitionsInProgress = ConcurrentHashMap.newKeySet();
    private boolean initialized;

    private final ReadWriteLock partitionsLock = new ReentrantReadWriteLock();

    public void init(PartitionedQueueConsumerManager<S> stateConsumer, PartitionedQueueConsumerManager<E> eventConsumer) {
        this.stateConsumer = stateConsumer;
        this.eventConsumer = eventConsumer;
    }

    public void update(Set<TopicPartitionInfo> newPartitions) {
        newPartitions = withTopic(newPartitions, stateConsumer.getTopic());
        var writeLock = partitionsLock.writeLock();
        writeLock.lock();
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
            writeLock.unlock();
        }

        if (!removedPartitions.isEmpty()) {
            stateConsumer.removePartitions(removedPartitions);
            eventConsumer.removePartitions(withTopic(removedPartitions, eventConsumer.getTopic()));
        }

        if (!addedPartitions.isEmpty()) {
            partitionsInProgress.addAll(addedPartitions);
            stateConsumer.addPartitions(addedPartitions, partition -> {
                var readLock = partitionsLock.readLock();
                readLock.lock();
                try {
                    partitionsInProgress.remove(partition);
                    log.info("Finished partition {} (still in progress: {})", partition, partitionsInProgress);
                    if (partitionsInProgress.isEmpty()) {
                        log.info("All partitions processed");
                    }
                    if (this.partitions.contains(partition)) {
                        eventConsumer.addPartitions(Set.of(partition.withTopic(eventConsumer.getTopic())));
                    }
                } finally {
                    readLock.unlock();
                }
            });
        }
        initialized = true;
    }

    public Set<TopicPartitionInfo> getPartitionsInProgress() {
        return initialized ? partitionsInProgress : null;
    }

}
