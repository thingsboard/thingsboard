/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
package org.thingsboard.server.service.cf;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.script.api.tbel.TbelInvokeService;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.cf.CalculatedFieldLink;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.cf.CalculatedFieldService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.service.cf.ctx.state.CalculatedFieldCtx;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Service
@Slf4j
@RequiredArgsConstructor
public class DefaultCalculatedFieldCache implements CalculatedFieldCache {

    private final Lock calculatedFieldFetchLock = new ReentrantLock();

    private final CalculatedFieldService calculatedFieldService;
    private final AssetService assetService;
    private final DeviceService deviceService;

    private final ConcurrentMap<CalculatedFieldId, CalculatedField> calculatedFields = new ConcurrentHashMap<>();
    private final ConcurrentMap<CalculatedFieldId, List<CalculatedFieldLink>> calculatedFieldLinks = new ConcurrentHashMap<>();
    private final ConcurrentMap<EntityId, List<CalculatedFieldLink>> entityIdCalculatedFieldLinks = new ConcurrentHashMap<>();
    private final ConcurrentMap<CalculatedFieldId, CalculatedFieldCtx> calculatedFieldsCtx = new ConcurrentHashMap<>();
    private final ConcurrentMap<EntityId, Set<EntityId>> profileEntities = new ConcurrentHashMap<>();

    @Value("${calculatedField.initFetchPackSize:50000}")
    @Getter
    private int initFetchPackSize;


    @PostConstruct
    public void init() {
        // to discuss: fetch on start or fetch on demand
        PageDataIterable<CalculatedField> cfs = new PageDataIterable<>(calculatedFieldService::findAllCalculatedFields, initFetchPackSize);
        cfs.forEach(cf -> calculatedFields.putIfAbsent(cf.getId(), cf));
        PageDataIterable<CalculatedFieldLink> cfls = new PageDataIterable<>(calculatedFieldService::findAllCalculatedFieldLinks, initFetchPackSize);
        cfls.forEach(link -> calculatedFieldLinks.computeIfAbsent(link.getCalculatedFieldId(), id -> new ArrayList<>()).add(link));
    }

    @Override
    public CalculatedField getCalculatedField(TenantId tenantId, CalculatedFieldId calculatedFieldId) {
        CalculatedField calculatedField = calculatedFields.get(calculatedFieldId);
        if (calculatedField == null) {
            calculatedFieldFetchLock.lock();
            try {
                calculatedField = calculatedFields.get(calculatedFieldId);
                if (calculatedField == null) {
                    calculatedField = calculatedFieldService.findById(tenantId, calculatedFieldId);
                    if (calculatedField != null) {
                        calculatedFields.put(calculatedFieldId, calculatedField);
                        log.debug("[{}] Fetch calculated field into cache: {}", calculatedFieldId, calculatedField);
                    }
                }
            } finally {
                calculatedFieldFetchLock.unlock();
            }
        }
        log.trace("[{}] Found calculated field in cache: {}", calculatedFieldId, calculatedField);
        return calculatedField;
    }

    @Override
    public List<CalculatedFieldLink> getCalculatedFieldLinks(TenantId tenantId, CalculatedFieldId calculatedFieldId) {
        List<CalculatedFieldLink> cfLinks = calculatedFieldLinks.get(calculatedFieldId);
        if (cfLinks == null || cfLinks.isEmpty()) {
            calculatedFieldFetchLock.lock();
            try {
                cfLinks = calculatedFieldLinks.get(calculatedFieldId);
                if (cfLinks == null || cfLinks.isEmpty()) {
                    cfLinks = calculatedFieldService.findAllCalculatedFieldLinksById(tenantId, calculatedFieldId);
                    if (cfLinks != null) {
                        calculatedFieldLinks.put(calculatedFieldId, cfLinks);
                        log.debug("[{}] Fetch calculated field links into cache: {}", calculatedFieldId, cfLinks);
                    }
                }
            } finally {
                calculatedFieldFetchLock.unlock();
            }
        }
        log.trace("[{}] Found calculated field links in cache: {}", calculatedFieldId, cfLinks);
        return cfLinks;
    }

    @Override
    public List<CalculatedFieldLink> getCalculatedFieldLinksByEntityId(TenantId tenantId, EntityId entityId) {
        List<CalculatedFieldLink> cfLinks = entityIdCalculatedFieldLinks.get(entityId);
        if (cfLinks == null || cfLinks.isEmpty()) {
            calculatedFieldFetchLock.lock();
            try {
                cfLinks = entityIdCalculatedFieldLinks.get(entityId);
                if (cfLinks == null || cfLinks.isEmpty()) {
                    cfLinks = calculatedFieldService.findAllCalculatedFieldLinksByEntityId(tenantId, entityId);
                    if (cfLinks != null) {
                        entityIdCalculatedFieldLinks.put(entityId, cfLinks);
                        log.debug("[{}] Fetch calculated field links by entity id into cache: {}", entityId, cfLinks);
                    }
                }
            } finally {
                calculatedFieldFetchLock.unlock();
            }
        }
        log.trace("[{}] Found calculated field links by entity id in cache: {}", entityId, cfLinks);
        return cfLinks;
    }

    @Override
    public CalculatedFieldCtx getCalculatedFieldCtx(TenantId tenantId, CalculatedFieldId calculatedFieldId, TbelInvokeService tbelInvokeService) {
        CalculatedFieldCtx ctx = calculatedFieldsCtx.get(calculatedFieldId);
        if (ctx == null) {
            calculatedFieldFetchLock.lock();
            try {
                ctx = calculatedFieldsCtx.get(calculatedFieldId);
                if (ctx == null) {
                    CalculatedField calculatedField = getCalculatedField(tenantId, calculatedFieldId);
                    if (calculatedField != null) {
                        ctx = new CalculatedFieldCtx(calculatedField, tbelInvokeService);
                        calculatedFieldsCtx.put(calculatedFieldId, ctx);
                        log.debug("[{}] Put calculated field ctx into cache: {}", calculatedFieldId, ctx);
                    }
                }
            } finally {
                calculatedFieldFetchLock.unlock();
            }
        }
        log.trace("[{}] Found calculated field ctx in cache: {}", calculatedFieldId, ctx);
        return ctx;
    }

    @Override
    public Set<EntityId> getEntitiesByProfile(TenantId tenantId, EntityId entityProfileId) {
        Set<EntityId> entities = profileEntities.get(entityProfileId);
        if (entities == null) {
            calculatedFieldFetchLock.lock();
            try {
                entities = profileEntities.get(entityProfileId);
                if (entities == null) {
                    entities = switch (entityProfileId.getEntityType()) {
                        case ASSET_PROFILE -> profileEntities.computeIfAbsent(entityProfileId, profileId -> {
                            Set<EntityId> assetIds = new HashSet<>();
                            (new PageDataIterable<>(pageLink ->
                                    assetService.findAssetIdsByTenantIdAndAssetProfileId(tenantId, (AssetProfileId) profileId, pageLink), initFetchPackSize)).forEach(assetIds::add);
                            return assetIds;
                        });
                        case DEVICE_PROFILE -> profileEntities.computeIfAbsent(entityProfileId, profileId -> {
                            Set<EntityId> deviceIds = new HashSet<>();
                            (new PageDataIterable<>(pageLink ->
                                    deviceService.findDeviceIdsByTenantIdAndDeviceProfileId(tenantId, (DeviceProfileId) entityProfileId, pageLink), initFetchPackSize)).forEach(deviceIds::add);
                            return deviceIds;
                        });
                        default ->
                                throw new IllegalArgumentException("Entity type should be ASSET_PROFILE or DEVICE_PROFILE.");
                    };
                }
            } finally {
                calculatedFieldFetchLock.unlock();
            }
        }
        log.trace("[{}] Found entities by profile in cache: {}", entityProfileId, entities);
        return entities;
    }

    @Override
    public void evict(CalculatedFieldId calculatedFieldId) {
        CalculatedField oldCalculatedField = calculatedFields.remove(calculatedFieldId);
        log.debug("[{}] evict calculated field from cache: {}", calculatedFieldId, oldCalculatedField);
        calculatedFieldLinks.remove(calculatedFieldId);
        log.debug("[{}] evict calculated field links from cache: {}", calculatedFieldId, oldCalculatedField);
        calculatedFieldsCtx.remove(calculatedFieldId);
        log.debug("[{}] evict calculated field ctx from cache: {}", calculatedFieldId, oldCalculatedField);
        entityIdCalculatedFieldLinks.forEach((entityId, calculatedFieldLinks) -> calculatedFieldLinks.removeIf(link -> link.getCalculatedFieldId().equals(calculatedFieldId)));
        log.debug("[{}] evict calculated field links from cached links by entity id: {}", calculatedFieldId, oldCalculatedField);
    }

}
