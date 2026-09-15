// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
