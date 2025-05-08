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
package org.thingsboard.server.common.transport.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.ApiUsageState;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.TenantProfileId;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.common.transport.TransportTenantProfileCache;
import org.thingsboard.server.common.transport.limits.TransportRateLimitService;
import org.thingsboard.server.common.transport.profile.TenantProfileUpdateResult;
import org.thingsboard.server.common.util.ProtoUtils;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.util.TbTransportComponent;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Component
@TbTransportComponent
@Slf4j
public class DefaultTransportTenantProfileCache implements TransportTenantProfileCache {

    private final Lock tenantProfileFetchLock = new ReentrantLock();
    private final ConcurrentMap<TenantProfileId, TenantProfile> profiles = new ConcurrentHashMap<>();
    private final ConcurrentMap<TenantId, TenantProfileId> tenantIds = new ConcurrentHashMap<>();
    private final ConcurrentMap<TenantProfileId, Set<TenantId>> tenantProfileIds = new ConcurrentHashMap<>();

    private TransportRateLimitService rateLimitService;
    private TransportService transportService;

    @Lazy
    @Autowired
    public void setRateLimitService(TransportRateLimitService rateLimitService) {
        this.rateLimitService = rateLimitService;
    }

    @Lazy
    @Autowired
    public void setTransportService(TransportService transportService) {
        this.transportService = transportService;
    }

    @Override
    public TenantProfile get(TenantId tenantId) {
        return getTenantProfile(tenantId);
    }

    @Override
    public TenantProfileUpdateResult put(TransportProtos.TenantProfileProto proto) {
        TenantProfile profile = ProtoUtils.fromProto(proto);
        log.trace("[{}] put: {}", profile.getId(), profile);
        profiles.put(profile.getId(), profile);
        Set<TenantId> affectedTenants = tenantProfileIds.get(profile.getId());
        return new TenantProfileUpdateResult(profile, affectedTenants != null ? affectedTenants : Collections.emptySet());
    }

    @Override
    public boolean put(TenantId tenantId, TenantProfileId profileId) {
        log.trace("[{}] put: {}", tenantId, profileId);
        TenantProfileId oldProfileId = tenantIds.get(tenantId);
        if (oldProfileId != null && !oldProfileId.equals(profileId)) {
            tenantProfileIds.computeIfAbsent(oldProfileId, id -> ConcurrentHashMap.newKeySet()).remove(tenantId);
            tenantIds.put(tenantId, profileId);
            tenantProfileIds.computeIfAbsent(profileId, id -> ConcurrentHashMap.newKeySet()).add(tenantId);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public Set<TenantId> remove(TenantProfileId profileId) {
        Set<TenantId> tenants = tenantProfileIds.remove(profileId);
        if (tenants != null) {
            tenants.forEach(tenantIds::remove);
        }
        profiles.remove(profileId);
        return tenants;
    }

    private TenantProfile getTenantProfile(TenantId tenantId) {
        TenantProfile profile = null;
        TenantProfileId tenantProfileId = tenantIds.get(tenantId);
        if (tenantProfileId != null) {
            profile = profiles.get(tenantProfileId);
        }
        if (profile == null) {
            tenantProfileFetchLock.lock();
            try {
                tenantProfileId = tenantIds.get(tenantId);
                if (tenantProfileId != null) {
                    profile = profiles.get(tenantProfileId);
                }
                if (profile == null) {
                    TransportProtos.GetEntityProfileRequestMsg msg = TransportProtos.GetEntityProfileRequestMsg.newBuilder()
                            .setEntityType(EntityType.TENANT.name())
                            .setEntityIdMSB(tenantId.getId().getMostSignificantBits())
                            .setEntityIdLSB(tenantId.getId().getLeastSignificantBits())
                            .build();
                    TransportProtos.GetEntityProfileResponseMsg entityProfileMsg = transportService.getEntityProfile(msg);
                    profile = ProtoUtils.fromProto(entityProfileMsg.getTenantProfile());
                    TenantProfile existingProfile = profiles.get(profile.getId());
                    if (existingProfile != null) {
                        profile = existingProfile;
                    } else {
                        profiles.put(profile.getId(), profile);
                    }
                    tenantProfileIds.computeIfAbsent(profile.getId(), id -> ConcurrentHashMap.newKeySet()).add(tenantId);
                    tenantIds.put(tenantId, profile.getId());
                    ApiUsageState apiUsageState = ProtoUtils.fromProto(entityProfileMsg.getApiState());
                    rateLimitService.update(tenantId, apiUsageState.isTransportEnabled());
                }
            } finally {
                tenantProfileFetchLock.unlock();
            }
        }
        return profile;
    }


}
