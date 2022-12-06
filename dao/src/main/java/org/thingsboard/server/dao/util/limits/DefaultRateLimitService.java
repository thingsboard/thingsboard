/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.server.dao.util.limits;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.common.msg.tools.TbRateLimits;
import org.thingsboard.server.dao.exception.MissingProfileException;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class DefaultRateLimitService implements RateLimitService {

    private final TbTenantProfileCache tenantProfileCache;

    private final ConcurrentMap<LimitedApi, ConcurrentMap<String, TbRateLimits>> apiRateLimits = new ConcurrentHashMap<>();

    @Override
    public boolean checkRateLimit(LimitedApi api, TenantId tenantId) {
        return checkRateLimit(api, tenantId, true);
    }

    @Override
    public boolean checkRateLimit(LimitedApi api, TenantId tenantId, boolean ignoreMissingProfileConfig) {
        return checkRateLimit(tenantId, tenantId.toString(), api, LimitLevel.TENANT, ignoreMissingProfileConfig);
    }

    @Override
    public boolean checkRateLimit(LimitedApi api, TenantId tenantId, EntityId entityId) {
        LimitLevel limitLevel = LimitLevel.forEntityType(entityId.getEntityType());
        return checkRateLimit(tenantId, entityId.toString(), api, limitLevel, true);
    }

    @Override
    public boolean checkRateLimit(LimitedApi api, TenantId tenantId, String key) {
        return checkRateLimit(tenantId, key, api, LimitLevel.GENERAL, true);
    }

    private boolean checkRateLimit(TenantId tenantId, String key, LimitedApi api, LimitLevel level, boolean ignoreMissingProfileConfig) {
        if (tenantId.equals(TenantId.SYS_TENANT_ID)) {
            return true;
        }
        DefaultTenantProfileConfiguration profileConfiguration = Optional.ofNullable(tenantProfileCache.get(tenantId))
                .map(TenantProfile::getDefaultProfileConfiguration).orElse(null);
        if (profileConfiguration == null) {
            if (ignoreMissingProfileConfig) {
                return true;
            } else {
                throw new MissingProfileException("Profile configuration for tenant [" + tenantId + "] is missing");
            }
        }

        String rateLimitConfig = api.getLimitConfig(profileConfiguration, level);
        log.trace("Checking rate limit for {} on {} level ([{}], tenantId: {})", api, level, key, tenantId);
        return checkRateLimit(api, rateLimitConfig, key);
    }

    @Override
    public boolean checkRateLimit(LimitedApi api, String rateLimitConfig, Object... keyParts) {
        String key = newKey(keyParts);
        Map<String, TbRateLimits> rateLimits = apiRateLimits.computeIfAbsent(api, k -> new ConcurrentHashMap<>());
        if (StringUtils.isBlank(rateLimitConfig)) {
            rateLimits.remove(key);
            return true;
        }

        TbRateLimits rateLimit = rateLimits.get(key);
        if (rateLimit == null || !rateLimit.getConfiguration().equals(rateLimitConfig)) {
            rateLimit = new TbRateLimits(rateLimitConfig, api.isRefillRateLimitIntervally());
            rateLimits.put(key, rateLimit);
        }
        return rateLimit.tryConsume();
    }

    @Override
    public void cleanUp(LimitedApi api, Object... keyParts) {
        String key = newKey(keyParts);
        Map<String, TbRateLimits> rateLimits = apiRateLimits.get(api);
        if (rateLimits != null) {
            rateLimits.remove(key);
        }
    }

    @Override
    public void cleanUpAll(LimitedApi api, Object keyPart) {
        Map<String, TbRateLimits> rateLimits = apiRateLimits.get(api);
        if (rateLimits != null) {
            rateLimits.keySet().removeIf(key -> key.startsWith(keyPart.toString()));
        }
    }

    private static String newKey(Object... keyParts) {
        return StringUtils.join(keyParts, ":");
    }

}
