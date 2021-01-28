/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
package org.thingsboard.server.service.attributes;

import com.github.benmanes.caffeine.cache.Cache;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.id.TenantId;


@Service
@Qualifier("CaffeineCacheStats")
public class CaffeineAttributesTbCacheStatsService extends AbstractAttributesTbCacheStatsService<Cache<AttributesKey, AttributeCacheEntry>> {
    private static final String CACHE_MANAGER = "CaffeineCacheManager";

    public CaffeineAttributesTbCacheStatsService(MeterRegistry meterRegistry) {
        super(meterRegistry);
    }

    @Override
    public void registerCacheStats(Cache<AttributesKey, AttributeCacheEntry> cache, TenantId tenantId) {
        CaffeineCacheMetrics.monitor(meterRegistry, cache, CACHE_STATS_NAME,
                "cacheManager", CACHE_MANAGER,
                "name", tenantId.getId().toString());
    }
}
