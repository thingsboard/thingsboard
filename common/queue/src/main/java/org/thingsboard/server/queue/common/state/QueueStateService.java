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

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.queue.TbQueueMsg;
import org.thingsboard.server.queue.common.consumer.PartitionedQueueConsumerManager;
import org.thingsboard.server.queue.discovery.QueueKey;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static org.thingsboard.server.common.msg.queue.TopicPartitionInfo.withTopic;

@Slf4j
public abstract class QueueStateService<E extends TbQueueMsg, S extends TbQueueMsg> {

    protected final PartitionedQueueConsumerManager<E> eventConsumer;

    @Getter
    protected final Map<QueueKey, Set<TopicPartitionInfo>> partitions = new HashMap<>();
    protected final Set<TopicPartitionInfo> partitionsInProgress = ConcurrentHashMap.newKeySet();
    protected boolean initialized;

    protected final ReadWriteLock partitionsLock = new ReentrantReadWriteLock();

    protected QueueStateService(PartitionedQueueConsumerManager<E> eventConsumer) {
        this.eventConsumer = eventConsumer;
    }

    public void update(QueueKey queueKey, Set<TopicPartitionInfo> newPartitions) {
        newPartitions = withTopic(newPartitions, eventConsumer.getTopic());
        var writeLock = partitionsLock.writeLock();
        writeLock.lock();
        Set<TopicPartitionInfo> oldPartitions = this.partitions.getOrDefault(queueKey, Collections.emptySet());
        Set<TopicPartitionInfo> addedPartitions;
        Set<TopicPartitionInfo> removedPartitions;
        try {
            addedPartitions = new HashSet<>(newPartitions);
            addedPartitions.removeAll(oldPartitions);
            removedPartitions = new HashSet<>(oldPartitions);
            removedPartitions.removeAll(newPartitions);
            this.partitions.put(queueKey, newPartitions);
        } finally {
            writeLock.unlock();
        }

        if (!removedPartitions.isEmpty()) {
            removePartitions(queueKey, removedPartitions);
        }

        if (!addedPartitions.isEmpty()) {
            addPartitions(queueKey, addedPartitions);
        }
        initialized = true;
    }

    protected void addPartitions(QueueKey queueKey, Set<TopicPartitionInfo> partitions) {
        eventConsumer.addPartitions(partitions);
    }

    protected void removePartitions(QueueKey queueKey, Set<TopicPartitionInfo> partitions) {
        eventConsumer.removePartitions(partitions);
    }

    public void delete(Set<TopicPartitionInfo> partitions) {
        if (partitions.isEmpty()) {
            return;
        }
        var writeLock = partitionsLock.writeLock();
        writeLock.lock();
        try {
            this.partitions.values().forEach(tpis -> tpis.removeAll(partitions));
        } finally {
            writeLock.unlock();
        }
        deletePartitions(partitions);
    }

    protected void deletePartitions(Set<TopicPartitionInfo> partitions) {
        eventConsumer.delete(withTopic(partitions, eventConsumer.getTopic()));
    }

    public Set<TopicPartitionInfo> getPartitionsInProgress() {
        return initialized ? partitionsInProgress : null;
    }

    public void stop() {
        eventConsumer.stop();
        eventConsumer.awaitStop();
    }

}
