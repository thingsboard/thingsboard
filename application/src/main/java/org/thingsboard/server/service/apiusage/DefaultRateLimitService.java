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
package org.thingsboard.server.service.apiusage;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.common.msg.tools.TbRateLimits;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class DefaultRateLimitService implements RateLimitService {

    private final TbTenantProfileCache tenantProfileCache;

    private final Map<String, Map<TenantId, TbRateLimits>> rateLimits = new ConcurrentHashMap<>();

    @Override
    public boolean checkEntityExportLimit(TenantId tenantId) {
        return checkLimit(tenantId, "entityExport", DefaultTenantProfileConfiguration::getTenantEntityExportRateLimit);
    }

    @Override
    public boolean checkEntityImportLimit(TenantId tenantId) {
        return checkLimit(tenantId, "entityImport", DefaultTenantProfileConfiguration::getTenantEntityImportRateLimit);
    }

    private boolean checkLimit(TenantId tenantId, String rateLimitsKey, Function<DefaultTenantProfileConfiguration, String> rateLimitConfigExtractor) {
        String rateLimitConfig = tenantProfileCache.get(tenantId).getProfileConfiguration()
                .map(rateLimitConfigExtractor).orElse(null);

        Map<TenantId, TbRateLimits> rateLimits = this.rateLimits.get(rateLimitsKey);
        if (StringUtils.isEmpty(rateLimitConfig)) {
            if (rateLimits != null) {
                rateLimits.remove(tenantId);
                if (rateLimits.isEmpty()) {
                    this.rateLimits.remove(rateLimitsKey);
                }
            }
            return true;
        }

        if (rateLimits == null) {
            rateLimits = new ConcurrentHashMap<>();
            this.rateLimits.put(rateLimitsKey, rateLimits);
        }
        TbRateLimits rateLimit = rateLimits.get(tenantId);
        if (rateLimit == null || !rateLimit.getConfiguration().equals(rateLimitConfig)) {
            rateLimit = new TbRateLimits(rateLimitConfig);
            rateLimits.put(tenantId, rateLimit);
        }

        return rateLimit.tryConsume();
    }

}
