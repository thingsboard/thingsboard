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
package org.thingsboard.server.service.profile;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.TenantProfileId;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.tenant.TenantProfileService;
import org.thingsboard.server.dao.tenant.TenantService;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Service
@Slf4j
public class DefaultTbTenantProfileCache implements TbTenantProfileCache {

    private final Lock tenantProfileFetchLock = new ReentrantLock();
    private final TenantProfileService tenantProfileService;
    private final TenantService tenantService;

    private final ConcurrentMap<TenantProfileId, TenantProfile> tenantProfilesMap = new ConcurrentHashMap<>();
    private final ConcurrentMap<TenantId, TenantProfileId> tenantsMap = new ConcurrentHashMap<>();

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
        }
    }

    @Override
    public void evict(TenantProfileId profileId) {
        tenantProfilesMap.remove(profileId);
    }

    @Override
    public void evict(TenantId tenantId) {
        tenantsMap.remove(tenantId);
    }

}
