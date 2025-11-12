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
