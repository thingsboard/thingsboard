// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.profile;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.msg.plugin.ComponentLifecycleMsg;
import org.thingsboard.server.dao.asset.AssetProfileService;
import org.thingsboard.server.dao.asset.AssetService;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DefaultTbAssetProfileCache implements TbAssetProfileCache {

    private final Lock assetProfileFetchLock = new ReentrantLock();
    private final AssetProfileService assetProfileService;
    private final AssetService assetService;

    private final ConcurrentMap<AssetProfileId, AssetProfile> assetProfilesMap = new ConcurrentHashMap<>();
    private final ConcurrentMap<AssetId, AssetProfileId> assetsMap = new ConcurrentHashMap<>();
    private final ConcurrentMap<TenantId, ConcurrentMap<EntityId, Consumer<AssetProfile>>> profileListeners = new ConcurrentHashMap<>();
    private final ConcurrentMap<TenantId, ConcurrentMap<EntityId, BiConsumer<AssetId, AssetProfile>>> assetProfileListeners = new ConcurrentHashMap<>();

    public DefaultTbAssetProfileCache(AssetProfileService assetProfileService, AssetService assetService) {
        this.assetProfileService = assetProfileService;
        this.assetService = assetService;
    }

    @Override
    public AssetProfile get(TenantId tenantId, AssetProfileId assetProfileId) {
        AssetProfile profile = assetProfilesMap.get(assetProfileId);
        if (profile == null) {
            assetProfileFetchLock.lock();
            try {
                profile = assetProfilesMap.get(assetProfileId);
                if (profile == null) {
                    profile = assetProfileService.findAssetProfileById(tenantId, assetProfileId);
                    if (profile != null) {
                        assetProfilesMap.put(assetProfileId, profile);
                        log.debug("[{}] Fetch asset profile into cache: {}", profile.getId(), profile);
                    }
                }
            } finally {
                assetProfileFetchLock.unlock();
            }
        }
        log.trace("[{}] Found asset profile in cache: {}", assetProfileId, profile);
        return profile;
    }

    @Override
    public AssetProfile get(TenantId tenantId, AssetId assetId) {
        AssetProfileId profileId = assetsMap.get(assetId);
        if (profileId == null) {
            Asset asset = assetService.findAssetById(tenantId, assetId);
            if (asset != null) {
                profileId = asset.getAssetProfileId();
                assetsMap.put(assetId, profileId);
            } else {
                return null;
            }
        }
        return get(tenantId, profileId);
    }

    @Override
    public void evict(TenantId tenantId, AssetProfileId profileId) {
        AssetProfile oldProfile = assetProfilesMap.remove(profileId);
        log.debug("[{}] evict asset profile from cache: {}", profileId, oldProfile);
        AssetProfile newProfile = get(tenantId, profileId);
        if (newProfile != null) {
            notifyProfileListeners(newProfile);
        }
    }

    @Override
    public void evict(TenantId tenantId, AssetId assetId) {
        AssetProfileId old = assetsMap.remove(assetId);
        if (old != null) {
            AssetProfile newProfile = get(tenantId, assetId);
            if (newProfile == null || !old.equals(newProfile.getId())) {
                notifyAssetListeners(tenantId, assetId, newProfile);
            }
        }
    }

    @Override
    public void addListener(TenantId tenantId, EntityId listenerId,
                            Consumer<AssetProfile> profileListener,
                            BiConsumer<AssetId, AssetProfile> assetListener) {
        if (profileListener != null) {
            profileListeners.computeIfAbsent(tenantId, id -> new ConcurrentHashMap<>()).put(listenerId, profileListener);
        }
        if (assetListener != null) {
            assetProfileListeners.computeIfAbsent(tenantId, id -> new ConcurrentHashMap<>()).put(listenerId, assetListener);
        }
    }

    @Override
    public AssetProfile find(AssetProfileId assetProfileId) {
        return assetProfileService.findAssetProfileById(TenantId.SYS_TENANT_ID, assetProfileId);
    }

    @Override
    public AssetProfile findOrCreateAssetProfile(TenantId tenantId, String profileName) {
        return assetProfileService.findOrCreateAssetProfile(tenantId, profileName);
    }

    @Override
    public void removeListener(TenantId tenantId, EntityId listenerId) {
        ConcurrentMap<EntityId, Consumer<AssetProfile>> tenantListeners = profileListeners.get(tenantId);
        if (tenantListeners != null) {
            tenantListeners.remove(listenerId);
        }
        ConcurrentMap<EntityId, BiConsumer<AssetId, AssetProfile>> assetListeners = assetProfileListeners.get(tenantId);
        if (assetListeners != null) {
            assetListeners.remove(listenerId);
        }
    }

    @EventListener(ComponentLifecycleMsg.class)
    public void onComponentLifecycleEvent(ComponentLifecycleMsg event) {
        switch (event.getEntityId().getEntityType()) {
            case TENANT:
                if (event.getEvent() == ComponentLifecycleEvent.DELETED) {
                    TenantId tenantId = event.getTenantId();
                    Set<AssetProfileId> toRemove = assetProfilesMap.values().stream()
                            .filter(assetProfile -> assetProfile.getTenantId().equals(tenantId))
                            .map(AssetProfile::getId)
                            .collect(Collectors.toSet());
                    assetProfilesMap.keySet().removeAll(toRemove);
                    assetsMap.entrySet().removeIf(entry -> toRemove.contains(entry.getValue()));
                    profileListeners.remove(tenantId);
                    assetProfileListeners.remove(tenantId);
                }
                break;
        }
    }

    private void notifyProfileListeners(AssetProfile profile) {
        ConcurrentMap<EntityId, Consumer<AssetProfile>> tenantListeners = profileListeners.get(profile.getTenantId());
        if (tenantListeners != null) {
            tenantListeners.forEach((id, listener) -> listener.accept(profile));
        }
    }

    private void notifyAssetListeners(TenantId tenantId, AssetId assetId, AssetProfile profile) {
        if (profile != null) {
            ConcurrentMap<EntityId, BiConsumer<AssetId, AssetProfile>> tenantListeners = assetProfileListeners.get(tenantId);
            if (tenantListeners != null) {
                tenantListeners.forEach((id, listener) -> listener.accept(assetId, profile));
            }
        }
    }

}
