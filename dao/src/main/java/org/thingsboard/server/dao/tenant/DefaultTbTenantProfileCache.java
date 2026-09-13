// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.tenant;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cache.limits.TenantProfileProvider;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.TenantProfileId;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

@Service
@Slf4j
public class DefaultTbTenantProfileCache implements TbTenantProfileCache, TenantProfileProvider {

    private final Lock tenantProfileFetchLock = new ReentrantLock();
    private final TenantProfileService tenantProfileService;
    private final TenantService tenantService;

    private final ConcurrentMap<TenantProfileId, TenantProfile> tenantProfilesMap = new ConcurrentHashMap<>();
    private final ConcurrentMap<TenantId, TenantProfileId> tenantsMap = new ConcurrentHashMap<>();
    private final ConcurrentMap<TenantId, ConcurrentMap<EntityId, Consumer<TenantProfile>>> profileListeners = new ConcurrentHashMap<>();

    public DefaultTbTenantProfileCache(TenantProfileService tenantProfileService, TenantService tenantService) {
        this.tenantProfileService = tenantProfileService;
        this.tenantService = tenantService;
    }

    @Override
    public TenantProfile get(TenantProfileId tenantProfileId) {
        TenantProfile profile = tenantProfilesMap.get(tenantProfileId);
        if (profile == null) {
            tenantProfileFetchLock.lock();
            try {
                profile = tenantProfilesMap.get(tenantProfileId);
                if (profile == null) {
                    profile = tenantProfileService.findTenantProfileById(TenantId.SYS_TENANT_ID, tenantProfileId);
                    if (profile != null) {
                        tenantProfilesMap.put(tenantProfileId, profile);
                    }
                }
            } finally {
                tenantProfileFetchLock.unlock();
            }
        }
        return profile;
    }

    @Override
    public TenantProfile get(TenantId tenantId) {
        TenantProfileId profileId = tenantsMap.get(tenantId);
        if (profileId == null) {
            Tenant tenant = tenantService.findTenantById(tenantId);
            if (tenant != null) {
                profileId = tenant.getTenantProfileId();
                tenantsMap.put(tenantId, profileId);
            } else {
                return null;
            }
        }
        return get(profileId);
    }

    @Override
    public void put(TenantProfile profile) {
        if (profile.getId() != null) {
            tenantProfilesMap.put(profile.getId(), profile);
            notifyTenantListeners(profile);
        }
    }

    @Override
    public void evict(TenantProfileId profileId) {
        tenantProfilesMap.remove(profileId);
        notifyTenantListeners(get(profileId));
    }

    public void notifyTenantListeners(TenantProfile tenantProfile) {
        if (tenantProfile != null) {
            tenantsMap.forEach(((tenantId, tenantProfileId) -> {
                if (tenantProfileId.equals(tenantProfile.getId())) {
                    ConcurrentMap<EntityId, Consumer<TenantProfile>> tenantListeners = profileListeners.get(tenantId);
                    if (tenantListeners != null) {
                        tenantListeners.forEach((id, listener) -> listener.accept(tenantProfile));
                    }
                }
            }));
        }
    }

    @Override
    public void evict(TenantId tenantId) {
        tenantsMap.remove(tenantId);
        TenantProfile tenantProfile = get(tenantId);
        if (tenantProfile != null) {
            ConcurrentMap<EntityId, Consumer<TenantProfile>> tenantListeners = profileListeners.get(tenantId);
            if (tenantListeners != null) {
                tenantListeners.forEach((id, listener) -> listener.accept(tenantProfile));
            }
        }
    }

    @Override
    public void addListener(TenantId tenantId, EntityId listenerId, Consumer<TenantProfile> profileListener) {
        //Force cache of the tenant id.
        get(tenantId);
        if (profileListener != null) {
            profileListeners.computeIfAbsent(tenantId, id -> new ConcurrentHashMap<>()).put(listenerId, profileListener);
        }
    }

    @Override
    public void removeListener(TenantId tenantId, EntityId listenerId) {
        ConcurrentMap<EntityId, Consumer<TenantProfile>> tenantListeners = profileListeners.get(tenantId);
        if (tenantListeners != null) {
            tenantListeners.remove(listenerId);
        }
    }

}
