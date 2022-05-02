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
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.id.QueueId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.data.queue.Queue;
import org.thingsboard.server.common.data.tenant.profile.TenantProfileQueueConfiguration;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.dao.queue.QueueService;
import org.thingsboard.server.queue.TbQueueAdmin;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@TbCoreComponent
@AllArgsConstructor
public class DefaultTbQueueService implements TbQueueService {
    private static final String MAIN = "Main";

    private final QueueService queueService;
    private final TbClusterService tbClusterService;
    private final TbQueueAdmin tbQueueAdmin;
    private final DeviceProfileService deviceProfileService;

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

        if (tbClusterService != null) {
            tbClusterService.onQueueChange(queue);
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
                if (tbClusterService != null) {
                    tbClusterService.onQueueChange(queue);
                }
            } else {
                log.info("Removed [{}] partitions from [{}] queue", oldPartitions - currentPartitions, queue.getName());
                if (tbClusterService != null) {
                    tbClusterService.onQueueChange(queue);
                }
                await();
                for (int i = currentPartitions; i < oldPartitions; i++) {
                    tbQueueAdmin.deleteTopic(
                            new TopicPartitionInfo(queue.getTopic(), queue.getTenantId(), i, false).getFullTopicName());
                }
            }
        } else if (!oldQueue.equals(queue) && tbClusterService != null) {
            tbClusterService.onQueueChange(queue);
        }
    }

    private void onQueueDeleted(TenantId tenantId, Queue queue) {
        if (tbClusterService != null) {
            tbClusterService.onQueueDelete(queue);
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

    @Override
    public void updateQueuesByTenants(List<TenantId> tenantIds, TenantProfile newTenantProfile, TenantProfile oldTenantProfile) {
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

        tenantIds.forEach(tenantId -> {
            Map<QueueId, List<DeviceProfile>> deviceProfileQueues;

            if (oldTenantProfile != null && !newTenantProfile.getId().equals(oldTenantProfile.getId()) || !toRemove.isEmpty()) {
                List<DeviceProfile> deviceProfiles = deviceProfileService.findDeviceProfiles(tenantId, new PageLink(Integer.MAX_VALUE)).getData();
                deviceProfileQueues = deviceProfiles.stream()
                        .filter(dp -> dp.getDefaultQueueId() != null)
                        .collect(Collectors.groupingBy(DeviceProfile::getDefaultQueueId));
            } else {
                deviceProfileQueues = Collections.emptyMap();
            }

            Map<String, QueueId> createdQueues = toCreate.stream()
                    .map(key -> saveQueue(new Queue(tenantId, newQueues.get(key))))
                    .collect(Collectors.toMap(Queue::getName, Queue::getId));

            // assigning created queues to device profiles instead of system queues
            if (oldTenantProfile != null && !oldTenantProfile.isIsolatedTbRuleEngine()) {
                deviceProfileQueues.forEach((queueId, list) -> {
                    Queue queue = queueService.findQueueById(TenantId.SYS_TENANT_ID, queueId);
                    QueueId queueIdToAssign = createdQueues.get(queue.getName());
                    if (queueIdToAssign == null) {
                        queueIdToAssign = createdQueues.get(MAIN);
                    }
                    for (DeviceProfile deviceProfile : list) {
                        deviceProfile.setDefaultQueueId(queueIdToAssign);
                        saveDeviceProfile(deviceProfile);
                    }
                });
            }

            toUpdate.forEach(key -> {
                Queue queueToUpdate = new Queue(tenantId, newQueues.get(key));
                Queue foundQueue = queueService.findQueueByTenantIdAndName(tenantId, key);
                queueToUpdate.setId(foundQueue.getId());
                queueToUpdate.setCreatedTime(foundQueue.getCreatedTime());

                if (queueToUpdate.equals(foundQueue)) {
                    //Queue not changed
                } else {
                    saveQueue(queueToUpdate);
                }
            });

            toRemove.forEach(q -> {
                Queue queue = queueService.findQueueByTenantIdAndNameInternal(tenantId, q);
                QueueId queueIdForRemove = queue.getId();
                if (deviceProfileQueues.containsKey(queueIdForRemove)) {
                    Queue foundQueue = queueService.findQueueByTenantIdAndName(tenantId, q);
                    if (foundQueue == null) {
                        foundQueue = queueService.findQueueByTenantIdAndName(tenantId, MAIN);
                    }
                    QueueId newQueueId = foundQueue.getId();
                    deviceProfileQueues.get(queueIdForRemove).stream()
                            .peek(dp -> dp.setDefaultQueueId(newQueueId))
                            .forEach(this::saveDeviceProfile);
                }
                deleteQueue(tenantId, queueIdForRemove);
            });
        });
    }

    //TODO: remove after implementing TbDeviceProfileService
    private void saveDeviceProfile(DeviceProfile deviceProfile) {
        DeviceProfile savedDeviceProfile = deviceProfileService.saveDeviceProfile(deviceProfile);
        tbClusterService.onDeviceProfileChange(savedDeviceProfile, null);
        tbClusterService.broadcastEntityStateChangeEvent(deviceProfile.getTenantId(), savedDeviceProfile.getId(), ComponentLifecycleEvent.UPDATED);
    }

}
