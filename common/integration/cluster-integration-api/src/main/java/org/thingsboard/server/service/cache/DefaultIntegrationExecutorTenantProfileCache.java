// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.TenantProfileId;
import org.thingsboard.server.queue.util.TbIntegrationExecutorComponent;
import org.thingsboard.server.service.integration.IntegrationConfigurationService;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Service
@TbIntegrationExecutorComponent
@Slf4j
@RequiredArgsConstructor
public class DefaultIntegrationExecutorTenantProfileCache implements IntegrationExecutorTenantProfileCache {

    private final IntegrationConfigurationService integrationConfigurationService;

    private final ConcurrentMap<TenantProfileId, TenantProfile> profiles = new ConcurrentHashMap<>();
    private final ConcurrentMap<TenantId, TenantProfileId> tenantsProfiles = new ConcurrentHashMap<>();
    private final Lock tenantProfileFetchLock = new ReentrantLock();

    @Override
    public TenantProfile get(TenantId tenantId) {
        TenantProfile profile = null;
        TenantProfileId tenantProfileId = tenantsProfiles.get(tenantId);
        if (tenantProfileId != null) {
            profile = profiles.get(tenantProfileId);
        }
        if (profile == null) {
            tenantProfileFetchLock.lock();
            try {
                tenantProfileId = tenantsProfiles.get(tenantId);
                if (tenantProfileId != null) {
                    profile = profiles.get(tenantProfileId);
                }
                if (profile == null) {
                    profile = integrationConfigurationService.getTenantProfile(tenantId);
                    log.trace("Fetched tenant profile for tenant {}: {}", tenantId, profile);
                    if (profile != null) {
                        profiles.put(profile.getId(), profile);
                        tenantsProfiles.put(tenantId, profile.getId());
                    }
                }
            } finally {
                tenantProfileFetchLock.unlock();
            }
        }
        return profile;
    }

    @Override
    public void evict(TenantProfileId profileId) {
        tenantsProfiles.values().removeIf(profileId::equals);
        profiles.remove(profileId);
        log.debug("Evicted tenant profile by id {}", profileId);
    }

    @Override
    public void evict(TenantId tenantId) {
        tenantsProfiles.remove(tenantId);
        log.debug("Evicted tenant profile for tenant {}", tenantId);
    }

}
