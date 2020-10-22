/**
 * Copyright © 2016-2020 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.common.transport.limits;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.TenantProfileData;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.transport.TransportTenantProfileCache;
import org.thingsboard.server.common.transport.profile.TenantProfileUpdateResult;
import org.thingsboard.server.queue.util.TbTransportComponent;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
@TbTransportComponent
@Slf4j
public class DefaultTransportRateLimitService implements TransportRateLimitService {

    private final ConcurrentMap<TenantId, Boolean> tenantAllowed = new ConcurrentHashMap<>();
    private final ConcurrentMap<TenantId, TransportRateLimit[]> perTenantLimits = new ConcurrentHashMap<>();
    private final ConcurrentMap<DeviceId, TransportRateLimit[]> perDeviceLimits = new ConcurrentHashMap<>();

    private final TransportRateLimitFactory rateLimitFactory;
    private final TransportTenantProfileCache tenantProfileCache;

    public DefaultTransportRateLimitService(TransportRateLimitFactory rateLimitFactory, TransportTenantProfileCache tenantProfileCache) {
        this.rateLimitFactory = rateLimitFactory;
        this.tenantProfileCache = tenantProfileCache;
    }

    @Override
    public TransportRateLimitType checkLimits(TenantId tenantId, DeviceId deviceId, int dataPoints, TransportRateLimitType... limits) {
        if (!tenantAllowed.getOrDefault(tenantId, Boolean.TRUE)) {
            return TransportRateLimitType.TENANT_ADDED_TO_DISABLED_LIST;
        }
        TransportRateLimit[] tenantLimits = getTenantRateLimits(tenantId);
        TransportRateLimit[] deviceLimits = getDeviceRateLimits(tenantId, deviceId);
        for (TransportRateLimitType limitType : limits) {
            TransportRateLimit rateLimit;
            if (limitType.isTenantLevel()) {
                rateLimit = tenantLimits[limitType.ordinal()];
            } else {
                rateLimit = deviceLimits[limitType.ordinal()];
            }
            if (!rateLimit.tryConsume(limitType.isMessageLevel() ? 1L : dataPoints)) {
                return limitType;
            }
        }
        return null;
    }

    @Override
    public void update(TenantProfileUpdateResult update) {
        TransportRateLimit[] newLimits = createTransportRateLimits(update.getProfile());
        for (TenantId tenantId : update.getAffectedTenants()) {
            mergeLimits(tenantId, newLimits);
        }
    }

    @Override
    public void update(TenantId tenantId) {
        mergeLimits(tenantId, fetchProfileAndInit(tenantId));
    }

    @Override
    public void remove(TenantId tenantId) {
        perTenantLimits.remove(tenantId);
    }

    @Override
    public void remove(DeviceId deviceId) {
        perDeviceLimits.remove(deviceId);
    }

    @Override
    public void update(TenantId tenantId, boolean allowed) {
        tenantAllowed.put(tenantId, allowed);
    }

    private void mergeLimits(TenantId tenantId, TransportRateLimit[] newRateLimits) {
        TransportRateLimit[] oldRateLimits = perTenantLimits.get(tenantId);
        if (oldRateLimits == null) {
            perTenantLimits.put(tenantId, newRateLimits);
        } else {
            for (int i = 0; i < TransportRateLimitType.values().length; i++) {
                TransportRateLimit newLimit = newRateLimits[i];
                TransportRateLimit oldLimit = oldRateLimits[i];
                if (newLimit != null && (oldLimit == null || !oldLimit.getConfiguration().equals(newLimit.getConfiguration()))) {
                    oldRateLimits[i] = newLimit;
                }
            }
        }
    }

    private TransportRateLimit[] fetchProfileAndInit(TenantId tenantId) {
        return perTenantLimits.computeIfAbsent(tenantId, tmp -> createTransportRateLimits(tenantProfileCache.get(tenantId)));
    }

    private TransportRateLimit[] createTransportRateLimits(TenantProfile tenantProfile) {
        TenantProfileData profileData = tenantProfile.getProfileData();
        TransportRateLimit[] rateLimits = new TransportRateLimit[TransportRateLimitType.values().length];
        for (TransportRateLimitType type : TransportRateLimitType.values()) {
            rateLimits[type.ordinal()] = rateLimitFactory.create(type, profileData.getProperties().get(type.getConfigurationKey()));
        }
        return rateLimits;
    }

    private TransportRateLimit[] getTenantRateLimits(TenantId tenantId) {
        TransportRateLimit[] limits = perTenantLimits.get(tenantId);
        if (limits == null) {
            limits = fetchProfileAndInit(tenantId);
            perTenantLimits.put(tenantId, limits);
        }
        return limits;
    }

    private TransportRateLimit[] getDeviceRateLimits(TenantId tenantId, DeviceId deviceId) {
        TransportRateLimit[] limits = perDeviceLimits.get(deviceId);
        if (limits == null) {
            limits = fetchProfileAndInit(tenantId);
            perDeviceLimits.put(deviceId, limits);
        }
        return limits;
    }
}
