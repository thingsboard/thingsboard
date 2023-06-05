/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
package org.thingsboard.server.service.apiusage.limits;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.tools.TbRateLimits;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;

import jakarta.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class DefaultRateLimitService implements RateLimitService {

    private final TbTenantProfileCache tenantProfileCache;
    @Value("${cache.rateLimits.timeToLiveInMinutes:60}")
    private int rateLimitsTtl;
    @Value("${cache.rateLimits.maxSize:100000}")
    private int rateLimitsCacheMaxSize;

    private Cache<RateLimitKey, TbRateLimits> rateLimits;

    @PostConstruct
    private void init() {
        rateLimits = Caffeine.newBuilder()
                .expireAfterAccess(rateLimitsTtl, TimeUnit.MINUTES)
                .maximumSize(rateLimitsCacheMaxSize)
                .build();
    }

    @Override
    public boolean checkRateLimit(LimitedApi api, TenantId tenantId) {
        return checkRateLimit(api, tenantId, tenantId);
    }

    @Override
    public boolean checkRateLimit(LimitedApi api, TenantId tenantId, EntityId entityId) {
        if (tenantId.isSysTenantId()) {
            return true;
        }
        RateLimitKey key = new RateLimitKey(api, entityId);

        String rateLimitConfig = tenantProfileCache.get(tenantId).getProfileConfiguration()
                .map(api::getLimitConfig).orElse(null);
        if (StringUtils.isEmpty(rateLimitConfig)) {
            rateLimits.invalidate(key);
            return true;
        }
        log.trace("[{}] Checking rate limit for {} ({})", entityId, api, rateLimitConfig);

        TbRateLimits rateLimit = rateLimits.asMap().compute(key, (k, limit) -> {
            if (limit == null || !limit.getConfiguration().equals(rateLimitConfig)) {
                limit = new TbRateLimits(rateLimitConfig);
                log.trace("[{}] Created new rate limit bucket for {} ({})", entityId, api, rateLimitConfig);
            }
            return limit;
        });
        boolean success = rateLimit.tryConsume();
        if (!success) {
            log.debug("[{}] Rate limit exceeded for {} ({})", entityId, api, rateLimitConfig);
        }
        return success;
    }

    @Data(staticConstructor = "of")
    private static class RateLimitKey {
        private final LimitedApi api;
        private final EntityId entityId;
    }

}
