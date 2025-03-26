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
package org.thingsboard.server.service.cf.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.TbApplicationEventListener;
import org.thingsboard.server.queue.discovery.event.PartitionChangeEvent;
import org.thingsboard.server.queue.util.TbRuleEngineComponent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@TbRuleEngineComponent
@Service
@Slf4j
@RequiredArgsConstructor
//TODO ashvayka: remove and use TenantEntityProfileCache in each CalculatedFieldManagerMessageProcessor;
public class DefaultCalculatedFieldEntityProfileCache extends TbApplicationEventListener<PartitionChangeEvent> implements CalculatedFieldEntityProfileCache {

    private static final Integer UNKNOWN = 0;
    private final ConcurrentMap<TenantId, TenantEntityProfileCache> tenantCache = new ConcurrentHashMap<>();
    private final PartitionService partitionService;

    @Override
    protected void onTbApplicationEvent(PartitionChangeEvent event) {
        Map<TenantId, List<Integer>> tenantPartitions = new HashMap<>();
        List<Integer> systemPartitions = new ArrayList<>();

        event.getCfPartitions().stream()
                .filter(TopicPartitionInfo::isMyPartition)
                .forEach(tpi -> {
                    Integer partition = tpi.getPartition().orElse(UNKNOWN);
                    Optional<TenantId> tenantIdOpt = tpi.getTenantId();
                    if (tenantIdOpt.isPresent()) {
                        tenantPartitions.computeIfAbsent(tenantIdOpt.get(), id -> new ArrayList<>()).add(partition);
                    } else {
                        systemPartitions.add(partition);
                    }
                });

        tenantPartitions.forEach((tenantId, partitions) -> {
            var cache = tenantCache.computeIfAbsent(tenantId, id -> new TenantEntityProfileCache());
            cache.setMyPartitions(partitions);
        });

        tenantCache.keySet().stream()
                .filter(tenantId -> !tenantPartitions.containsKey(tenantId))
                .forEach(tenantId -> {
                    var cache = tenantCache.get(tenantId);
                    cache.setMyPartitions(systemPartitions);
                });
    }

    @Override
    public void add(TenantId tenantId, EntityId profileId, EntityId entityId) {
        var tpi = partitionService.resolve(ServiceType.TB_RULE_ENGINE, DataConstants.CF_QUEUE_NAME, tenantId, entityId);
        var partition = tpi.getPartition().orElse(UNKNOWN);
        tenantCache.computeIfAbsent(tenantId, id -> new TenantEntityProfileCache())
                .add(profileId, entityId, partition, tpi.isMyPartition());
    }

    @Override
    public void update(TenantId tenantId, EntityId oldProfileId, EntityId newProfileId, EntityId entityId) {
        var tpi = partitionService.resolve(ServiceType.TB_RULE_ENGINE, DataConstants.CF_QUEUE_NAME, tenantId, entityId);
        var partition = tpi.getPartition().orElse(UNKNOWN);
        var cache = tenantCache.computeIfAbsent(tenantId, id -> new TenantEntityProfileCache());
        //TODO: make this method atomic;
        cache.remove(oldProfileId, entityId);
        cache.add(newProfileId, entityId, partition, tpi.isMyPartition());
    }

    @Override
    public void evict(TenantId tenantId, EntityId entityId) {
        var cache = tenantCache.computeIfAbsent(tenantId, id -> new TenantEntityProfileCache());
        cache.removeEntityId(entityId);
    }

    @Override
    public Collection<EntityId> getMyEntityIdsByProfileId(TenantId tenantId, EntityId profileId) {
        return tenantCache.computeIfAbsent(tenantId, id -> new TenantEntityProfileCache()).getMyEntityIdsByProfileId(profileId);
    }

    @Override
    public int getEntityIdPartition(TenantId tenantId, EntityId entityId) {
        var tpi = partitionService.resolve(ServiceType.TB_RULE_ENGINE, DataConstants.CF_QUEUE_NAME, tenantId, entityId);
        return tpi.getPartition().orElse(UNKNOWN);
    }

}
