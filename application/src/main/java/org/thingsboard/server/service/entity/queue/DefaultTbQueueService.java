/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.server.service.entity.queue;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.id.QueueId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.queue.Queue;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.dao.queue.QueueService;
import org.thingsboard.server.queue.TbQueueAdmin;
import org.thingsboard.server.queue.TbQueueClusterService;
import org.thingsboard.server.queue.util.TbCoreComponent;

@Slf4j
@Service
@TbCoreComponent
@AllArgsConstructor
public class DefaultTbQueueService implements TbQueueService {
    private final QueueService queueService;
    private final TbQueueClusterService queueClusterService;
    private final TbQueueAdmin tbQueueAdmin;

    @Override
    public Queue saveQueue(Queue queue) {
        boolean create = queue.getId() == null;
        Queue oldQueue;

        if (create) {
            oldQueue = null;
        } else {
            oldQueue = queueService.findQueueById(queue.getTenantId(), queue.getId());
        }

        //TODO: add checkNotNull
        Queue savedQueue = queueService.saveQueue(queue);

        if (create) {
            onQueueCreated(savedQueue);
        } else {
            onQueueUpdated(savedQueue, oldQueue);
        }

        return savedQueue;
    }

    @Override
    public void deleteQueue(TenantId tenantId, QueueId queueId) {
        Queue queue = queueService.findQueueById(tenantId, queueId);
        queueService.deleteQueue(tenantId, queueId);
        onQueueDeleted(tenantId, queue);
    }

    @Override
    public void deleteQueueByQueueName(TenantId tenantId, String queueName) {
        Queue queue = queueService.findQueueByTenantIdAndNameInternal(tenantId, queueName);
        queueService.deleteQueue(tenantId, queue.getId());
        onQueueDeleted(tenantId, queue);
    }

    private void onQueueCreated(Queue queue) {
        if (tbQueueAdmin != null) {
            for (int i = 0; i < queue.getPartitions(); i++) {
                tbQueueAdmin.createTopicIfNotExists(
                        new TopicPartitionInfo(queue.getTopic(), queue.getTenantId(), i, false).getFullTopicName());
            }
        }

        if (queueClusterService != null) {
            queueClusterService.onQueueChange(queue);
        }
    }

    private void onQueueUpdated(Queue queue, Queue oldQueue) {
        int oldPartitions = oldQueue.getPartitions();
        int currentPartitions = queue.getPartitions();

        if (currentPartitions != oldPartitions && tbQueueAdmin != null) {
            if (currentPartitions > oldPartitions) {
                log.info("Added [{}] new partitions to [{}] queue", currentPartitions - oldPartitions, queue.getName());
                for (int i = oldPartitions; i < currentPartitions; i++) {
                    tbQueueAdmin.createTopicIfNotExists(
                            new TopicPartitionInfo(queue.getTopic(), queue.getTenantId(), i, false).getFullTopicName());
                }
                if (queueClusterService != null) {
                    queueClusterService.onQueueChange(queue);
                }
            } else {
                log.info("Removed [{}] partitions from [{}] queue", oldPartitions - currentPartitions, queue.getName());
                if (queueClusterService != null) {
                    queueClusterService.onQueueChange(queue);
                }
                await();
                for (int i = currentPartitions; i < oldPartitions; i++) {
                    tbQueueAdmin.deleteTopic(
                            new TopicPartitionInfo(queue.getTopic(), queue.getTenantId(), i, false).getFullTopicName());
                }
            }
        } else if (!oldQueue.equals(queue) && queueClusterService != null) {
            queueClusterService.onQueueChange(queue);
        }
    }

    private void onQueueDeleted(TenantId tenantId, Queue queue) {
        if (queueClusterService != null) {
            queueClusterService.onQueueDelete(queue);
            await();
        }
//        queueStatsService.deleteQueueStatsByQueueId(tenantId, queueId);
        if (tbQueueAdmin != null) {
            for (int i = 0; i < queue.getPartitions(); i++) {
                String fullTopicName = new TopicPartitionInfo(queue.getTopic(), queue.getTenantId(), i, false).getFullTopicName();
                log.debug("Deleting queue [{}]", fullTopicName);
                try {
                    tbQueueAdmin.deleteTopic(fullTopicName);
                } catch (Exception e) {
                    log.error("Failed to delete queue [{}]", fullTopicName);
                }
            }
        }
    }

    @SneakyThrows
    private void await() {
        Thread.sleep(3000);
    }

}
