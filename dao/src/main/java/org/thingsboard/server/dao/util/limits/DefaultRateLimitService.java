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
package org.thingsboard.server.dao.util.limits;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.exception.TenantProfileNotFoundException;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.limit.LimitedApi;
import org.thingsboard.server.common.msg.notification.NotificationRuleProcessor;
import org.thingsboard.server.common.msg.notification.trigger.RateLimitsTrigger;
import org.thingsboard.server.common.msg.tools.TbRateLimits;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;

import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class DefaultRateLimitService implements RateLimitService {

    private final TbTenantProfileCache tenantProfileCache;
    private final NotificationRuleProcessor notificationRuleProcessor;

    public DefaultRateLimitService(TbTenantProfileCache tenantProfileCache,
                                   NotificationRuleProcessor notificationRuleProcessor,
                                   @Value("${cache.rateLimits.timeToLiveInMinutes:120}") int rateLimitsTtl,
                                   @Value("${cache.rateLimits.maxSize:200000}") int rateLimitsCacheMaxSize) {
        this.tenantProfileCache = tenantProfileCache;
        this.notificationRuleProcessor = notificationRuleProcessor;
        this.rateLimits = Caffeine.newBuilder()
                .expireAfterAccess(rateLimitsTtl, TimeUnit.MINUTES)
                .maximumSize(rateLimitsCacheMaxSize)
                .build();
    }

    private final Cache<RateLimitKey, TbRateLimits> rateLimits;

    @Override
    public boolean checkRateLimit(LimitedApi api, TenantId tenantId) {
        return checkRateLimit(api, tenantId, tenantId);
    }

    @Override
    public boolean checkRateLimit(LimitedApi api, TenantId tenantId, Object level) {
        if (tenantId.isSysTenantId()) {
            return true;
        }
        TenantProfile tenantProfile = tenantProfileCache.get(tenantId);
        if (tenantProfile == null) {
            throw new TenantProfileNotFoundException(tenantId);
        }

        String rateLimitConfig = tenantProfile.getProfileConfiguration()
                .map(profileConfiguration -> api.getLimitConfig(profileConfiguration, level))
                .orElse(null);
        boolean success = checkRateLimit(api, level, rateLimitConfig);
        if (!success) {
            notificationRuleProcessor.process(RateLimitsTrigger.builder()
                    .tenantId(tenantId).api(api.getLabel())
                    .limitLevel(level instanceof EntityId ? (EntityId) level : null)
                    .limitLevelEntityName(null)
                    .build());
        }
        return success;
    }

    @Override
    public boolean checkRateLimit(LimitedApi api, Object level, String rateLimitConfig) {
        RateLimitKey key = new RateLimitKey(api, level);
        if (StringUtils.isEmpty(rateLimitConfig)) {
            rateLimits.invalidate(key);
            return true;
        }
        log.trace("[{}] Checking rate limit for {} ({})", level, api, rateLimitConfig);

        TbRateLimits rateLimit = rateLimits.asMap().compute(key, (k, limit) -> {
            if (limit == null || !limit.getConfiguration().equals(rateLimitConfig)) {
                limit = new TbRateLimits(rateLimitConfig, api.isRefillRateLimitIntervally());
                log.trace("[{}] Created new rate limit bucket for {} ({})", level, api, rateLimitConfig);
            }
            return limit;
        });
        boolean success = rateLimit.tryConsume();
        if (!success) {
            log.debug("[{}] Rate limit exceeded for {} ({})", level, api, rateLimitConfig);
        }
        return success;
    }

    @Override
    public void cleanUp(LimitedApi api, Object level) {
        RateLimitKey key = new RateLimitKey(api, level);
        rateLimits.invalidate(key);
    }

    @Data(staticConstructor = "of")
    private static class RateLimitKey {
        private final LimitedApi api;
        private final Object level;
    }

}
