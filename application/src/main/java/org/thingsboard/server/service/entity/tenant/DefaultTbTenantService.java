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
package org.thingsboard.server.service.entity.tenant;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.queue.Queue;
import org.thingsboard.server.common.data.tenant.profile.TenantProfileQueueConfiguration;
import org.thingsboard.server.dao.queue.QueueService;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.dao.tenant.TenantProfileService;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entity.queue.TbQueueService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@TbCoreComponent
@AllArgsConstructor
public class DefaultTbTenantService implements TbTenantService {

    private final TenantService tenantService;
    private final TbQueueService tbQueueService;
    private final QueueService queueService;
    private final TenantProfileService tenantProfileService;
    private final TbTenantProfileCache tenantProfileCache;

    @Override
    public Tenant saveTenant(Tenant tenant) {
        boolean updated = tenant.getId() != null;
        Tenant oldTenant = updated ? tenantService.findTenantById(tenant.getId()) : null;
        List<Queue> queues;
        if (updated) {

        }

        Tenant savedTenant = tenantService.saveTenant(tenant);
        tenantProfileCache.evict(tenant.getId());
        updateQueuesForTenant(oldTenant, savedTenant);
        return savedTenant;
    }

    public void updateQueuesForTenant(Tenant oldTenant, Tenant newTenant) {
        TenantProfile oldTenantProfile = oldTenant != null ? tenantProfileService.findTenantProfileById(TenantId.SYS_TENANT_ID, oldTenant.getTenantProfileId()) : null;
        TenantProfile newTenantProfile = tenantProfileService.findTenantProfileById(TenantId.SYS_TENANT_ID, newTenant.getTenantProfileId());

        TenantId tenantId = newTenant.getId();

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

        toRemove.forEach(q -> tbQueueService.deleteQueueByQueueName(tenantId, q));

        toCreate.forEach(key -> tbQueueService.saveQueue(new Queue(tenantId, newQueues.get(key))));

        toUpdate.forEach(key -> {
            Queue queueToUpdate = new Queue(tenantId, newQueues.get(key));
            Queue foundQueue = queueService.findQueueByTenantIdAndName(tenantId, key);
            queueToUpdate.setId(foundQueue.getId());
            queueToUpdate.setCreatedTime(foundQueue.getCreatedTime());

            if (queueToUpdate.equals(foundQueue)) {
                //Queue not changed
            } else {
                tbQueueService.saveQueue(queueToUpdate);
            }
        });
    }
}
