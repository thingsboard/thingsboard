/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
package org.thingsboard.server.dao.tenant;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
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
public class DefaultTbTenantProfileCache implements TbTenantProfileCache {

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
