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

import org.thingsboard.server.common.data.queue.QueueConfig;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;

import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

public interface TbQueueConsumerManagerTask {

    QueueTaskType getType();

    record DeleteQueueTask(boolean drainQueue) implements TbQueueConsumerManagerTask {
        @Override
        public QueueTaskType getType() {
            return QueueTaskType.DELETE;
        }
    }

    record UpdateConfigTask(QueueConfig config) implements TbQueueConsumerManagerTask {
        @Override
        public QueueTaskType getType() {
            return QueueTaskType.UPDATE_CONFIG;
        }
    }

    record UpdatePartitionsTask(Set<TopicPartitionInfo> partitions) implements TbQueueConsumerManagerTask {
        @Override
        public QueueTaskType getType() {
            return QueueTaskType.UPDATE_PARTITIONS;
        }
    }

    record AddPartitionsTask(Set<TopicPartitionInfo> partitions,
                             Consumer<TopicPartitionInfo> onStop,
                             Function<String, Long> startOffsetProvider) implements TbQueueConsumerManagerTask {
        @Override
        public QueueTaskType getType() {
            return QueueTaskType.ADD_PARTITIONS;
        }
    }

    record RemovePartitionsTask(Set<TopicPartitionInfo> partitions) implements TbQueueConsumerManagerTask {
        @Override
        public QueueTaskType getType() {
            return QueueTaskType.REMOVE_PARTITIONS;
        }
    }

    record DeletePartitionsTask(Set<TopicPartitionInfo> partitions) implements TbQueueConsumerManagerTask {
        @Override
        public QueueTaskType getType() {
            return QueueTaskType.REMOVE_PARTITIONS;
        }
    }

}
