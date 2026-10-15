// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.queue.common.state;

import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.queue.TbQueueMsg;
import org.thingsboard.server.queue.common.consumer.PartitionedQueueConsumerManager;
import org.thingsboard.server.queue.discovery.QueueKey;

import java.util.Collections;
import java.util.Set;

import static org.thingsboard.server.common.msg.queue.TopicPartitionInfo.withTopic;

public class DefaultQueueStateService<E extends TbQueueMsg, S extends TbQueueMsg> extends QueueStateService<E, S> {

    public DefaultQueueStateService(PartitionedQueueConsumerManager<E> eventConsumer) {
        super(eventConsumer, Collections.emptyList());
    }

    @Override
    protected void addPartitions(QueueKey queueKey, Set<TopicPartitionInfo> partitions, RestoreCallback callback) {
        if (callback != null) {
            for (TopicPartitionInfo partition : partitions) {
                callback.onPartitionRestored(partition);
            }
            callback.onAllPartitionsRestored();
        }
        eventConsumer.addPartitions(partitions);
        for (PartitionedQueueConsumerManager<?> consumer : otherConsumers) {
            consumer.addPartitions(withTopic(partitions, consumer.getTopic()));
        }
    }

}
