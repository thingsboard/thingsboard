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

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.TbApplicationEventListener;
import org.thingsboard.server.queue.discovery.event.PartitionChangeEvent;
import org.thingsboard.server.queue.util.TbRuleEngineComponent;

import java.util.Collection;
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
    private final AssetService assetService;
    private final DeviceService deviceService;

    @Value("${calculated_fields.init_fetch_pack_size:50000}")
    @Getter
    private int initFetchPackSize;

    @Override
    protected void onTbApplicationEvent(PartitionChangeEvent event) {
        event.getCfPartitions().forEach(tpi -> tpi.getTenantId().ifPresent(this::initCacheForNewTenant));
    }

    @Override
    public void add(TenantId tenantId, EntityId profileId, EntityId entityId) {
        tenantCache.computeIfAbsent(tenantId, id -> new TenantEntityProfileCache()).add(profileId, entityId);
    }

    @Override
    public void update(TenantId tenantId, EntityId oldProfileId, EntityId newProfileId, EntityId entityId) {
        tenantCache.compute(tenantId, (id, cache) -> {
            if (cache == null) {
                cache = new TenantEntityProfileCache();
            }
            cache.remove(oldProfileId, entityId);
            cache.add(newProfileId, entityId);
            return cache;
        });
    }

    @Override
    public void evict(TenantId tenantId, EntityId entityId) {
        var cache = tenantCache.computeIfAbsent(tenantId, id -> new TenantEntityProfileCache());
        cache.removeEntityId(entityId);
    }

    @Override
    public void evictProfile(TenantId tenantId, EntityId profileId) {
        tenantCache.computeIfAbsent(tenantId, id -> new TenantEntityProfileCache()).removeProfileId(profileId);
    }

    @Override
    public void removeTenant(TenantId tenantId) {
        tenantCache.remove(tenantId);
    }

    @Override
    public Collection<EntityId> getEntityIdsByProfileId(TenantId tenantId, EntityId profileId) {
        return tenantCache.computeIfAbsent(tenantId, id -> new TenantEntityProfileCache()).getEntityIdsByProfileId(profileId);
    }

    @Override
    public int getEntityIdPartition(TenantId tenantId, EntityId entityId) {
        var tpi = partitionService.resolve(ServiceType.TB_RULE_ENGINE, DataConstants.CF_QUEUE_NAME, tenantId, entityId);
        return tpi.getPartition().orElse(UNKNOWN);
    }

    private void initCacheForNewTenant(TenantId tenantId) {
        PageDataIterable<Device> devices = new PageDataIterable<>(pageLink -> deviceService.findDevicesByTenantId(tenantId, pageLink), initFetchPackSize);
        for (Device device : devices) {
            log.trace("Processing device record: {}", device);
            try {
                if (partitionService.isManagedByCurrentService(device.getTenantId())) {
                    add(device.getTenantId(), device.getDeviceProfileId(), device.getId());
                }
            } catch (Exception e) {
                log.error("Failed to process device record: {}", device, e);
            }
        }
        PageDataIterable<Asset> assets = new PageDataIterable<>(pageLink -> assetService.findAssetsByTenantId(tenantId, pageLink), initFetchPackSize);
        for (Asset asset : assets) {
            log.trace("Processing asset record: {}", asset);
            try {
                if (partitionService.isManagedByCurrentService(asset.getTenantId())) {
                    add(asset.getTenantId(), asset.getAssetProfileId(), asset.getId());
                }
            } catch (Exception e) {
                log.error("Failed to process asset record: {}", asset, e);
            }
        }
    }

}
