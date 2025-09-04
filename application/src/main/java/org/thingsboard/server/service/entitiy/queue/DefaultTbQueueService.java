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
package org.thingsboard.server.service.entitiy.queue;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.id.QueueId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.queue.Queue;
import org.thingsboard.server.common.data.tenant.profile.TenantProfileQueueConfiguration;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.dao.queue.QueueService;
import org.thingsboard.server.queue.TbQueueAdmin;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@TbCoreComponent
@RequiredArgsConstructor
public class DefaultTbQueueService extends AbstractTbEntityService implements TbQueueService {

    private final QueueService queueService;
    private final TbClusterService tbClusterService;
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

        Queue savedQueue = queueService.saveQueue(queue);
        createTopicsIfNeeded(savedQueue, oldQueue);
        tbClusterService.onQueuesUpdate(List.of(savedQueue));
        return savedQueue;
    }

    @Override
    public void deleteQueue(TenantId tenantId, QueueId queueId) {
        Queue queue = queueService.findQueueById(tenantId, queueId);
        queueService.deleteQueue(tenantId, queueId);
        tbClusterService.onQueuesDelete(List.of(queue));
    }

    @Override
    public void deleteQueueByQueueName(TenantId tenantId, String queueName) {
        Queue queue = queueService.findQueueByTenantIdAndNameInternal(tenantId, queueName);
        queueService.deleteQueue(tenantId, queue.getId());
        tbClusterService.onQueuesDelete(List.of(queue));
    }

    @Override
    public void updateQueuesByTenants(List<TenantId> tenantIds, TenantProfile newTenantProfile, TenantProfile
            oldTenantProfile) {
        boolean oldIsolated = oldTenantProfile != null && oldTenantProfile.isIsolatedTbRuleEngine();
        boolean newIsolated = newTenantProfile.isIsolatedTbRuleEngine();

        if (!oldIsolated && !newIsolated) {
            return;
        }

        if (newTenantProfile.equals(oldTenantProfile)) {
            return;
        }

        Map<String, TenantProfileQueueConfiguration> oldQueues;
        Map<String, TenantProfileQueueConfiguration> newQueues;

        if (oldIsolated) {
            oldQueues = oldTenantProfile.getProfileData().getQueueConfiguration().stream()
                    .collect(Collectors.toMap(TenantProfileQueueConfiguration::getName, q -> q));
        } else {
            oldQueues = Collections.emptyMap();
        }

        if (newIsolated) {
            newQueues = newTenantProfile.getProfileData().getQueueConfiguration().stream()
                    .collect(Collectors.toMap(TenantProfileQueueConfiguration::getName, q -> q));
        } else {
            newQueues = Collections.emptyMap();
        }

        List<String> toRemove = new ArrayList<>();
        List<String> toCreate = new ArrayList<>();
        List<String> toUpdate = new ArrayList<>();

        for (String oldQueue : oldQueues.keySet()) {
            if (!newQueues.containsKey(oldQueue)) {
                toRemove.add(oldQueue);
            }
        }

        for (String newQueue : newQueues.keySet()) {
            if (oldQueues.containsKey(newQueue)) {
                toUpdate.add(newQueue);
            } else {
                toCreate.add(newQueue);
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("[{}] Handling profile queue config update: creating queues {}, updating {}, deleting {}. Affected tenants: {}",
                    newTenantProfile.getUuidId(), toCreate, toUpdate, toRemove, tenantIds);
        }

        List<Queue> updated = new ArrayList<>();
        List<Queue> deleted = new ArrayList<>();
        for (TenantId tenantId : tenantIds) {
            for (String name : toCreate) {
                updated.add(new Queue(tenantId, newQueues.get(name)));
            }

            for (String name : toUpdate) {
                Queue queue = new Queue(tenantId, newQueues.get(name));
                Queue foundQueue = queueService.findQueueByTenantIdAndName(tenantId, name);
                if (foundQueue != null) {
                    queue.setId(foundQueue.getId());
                    queue.setCreatedTime(foundQueue.getCreatedTime());
                }
                if (!queue.equals(foundQueue)) {
                    updated.add(queue);
                    createTopicsIfNeeded(queue, foundQueue);
                }
            }

            for (String name : toRemove) {
                Queue queue = queueService.findQueueByTenantIdAndNameInternal(tenantId, name);
                deleted.add(queue);
            }
        }

        if (!updated.isEmpty()) {
            updated = updated.stream()
                    .map(queueService::saveQueue)
                    .collect(Collectors.toList());
            tbClusterService.onQueuesUpdate(updated);
        }
        if (!deleted.isEmpty()) {
            deleted.forEach(queue -> {
                queueService.deleteQueue(queue.getTenantId(), queue.getId());
            });
            tbClusterService.onQueuesDelete(deleted);
        }
    }

    private void createTopicsIfNeeded(Queue queue, Queue oldQueue) {
        int newPartitions = queue.getPartitions();
        int oldPartitions = oldQueue != null ? oldQueue.getPartitions() : 0;
        for (int i = oldPartitions; i < newPartitions; i++) {
            tbQueueAdmin.createTopicIfNotExists(
                    new TopicPartitionInfo(queue.getTopic(), queue.getTenantId(), i, false).getFullTopicName(),
                    queue.getCustomProperties(),
                    true); // forcing topic creation because the topic may still be cached on some nodes
        }
    }

}
