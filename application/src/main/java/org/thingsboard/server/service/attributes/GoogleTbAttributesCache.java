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
package org.thingsboard.server.service.attributes;

import com.google.common.base.Ticker;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@ConditionalOnProperty(prefix = "cache.attributes", value = "enabled", havingValue = "true")
@Service
public class GoogleTbAttributesCache implements TbAttributesCache {
    private final Map<TenantId, Cache<AttributesKey, AttributeCacheEntry>> tenantsCache = new ConcurrentHashMap<>();

    private final AttributesCacheConfiguration cacheConfiguration;
    private final TbCacheStatsService<Cache<AttributesKey, AttributeCacheEntry>> cacheStatsService;

    public GoogleTbAttributesCache(AttributesCacheConfiguration cacheConfiguration, TbCacheStatsService<Cache<AttributesKey, AttributeCacheEntry>> cacheStatsService) {
        this.cacheConfiguration = cacheConfiguration;
        this.cacheStatsService = cacheStatsService;
    }

    private Cache<AttributesKey, AttributeCacheEntry> getTenantCache(TenantId tenantId) {
        return tenantsCache.computeIfAbsent(tenantId,
                id -> {
                    CacheBuilder<Object, Object> cacheBuilder = CacheBuilder.newBuilder()
                            .maximumSize(cacheConfiguration.getMaxSizePerTenant())
                            .expireAfterAccess(cacheConfiguration.getExpireAfterAccessInMinutes(), TimeUnit.MINUTES);
                    if (customTicker != null){
                        cacheBuilder.ticker(customTicker);
                    }
                    if (cacheStatsService.areCacheStatsEnabled()) {
                        cacheBuilder.recordStats();
                    }
                    Cache<AttributesKey, AttributeCacheEntry> cache = cacheBuilder.build();

                    if (cacheStatsService.areCacheStatsEnabled()) {
                        cacheStatsService.registerCacheStats(cache, tenantId);
                    }
                    return cache;
                }
        );
    }

    @Override
    public AttributeCacheEntry find(TenantId tenantId, EntityId entityId, String scope, String key) {
        return getTenantCache(tenantId).getIfPresent(new AttributesKey(scope, entityId, key));
    }

    @Override
    public void put(TenantId tenantId, EntityId entityId, String scope, String key, AttributeKvEntry entry) {
        getTenantCache(tenantId).put(new AttributesKey(scope, entityId, key), entry != null ?
                new AttributeCacheEntry(entry) : AttributeCacheEntry.empty());
    }

    @Override
    public void evict(TenantId tenantId, EntityId entityId, String scope, List<String> attributeKeys) {
        List<AttributesKey> keys = attributeKeys.stream().map(key -> new AttributesKey(scope, entityId, key)).collect(Collectors.toList());
        getTenantCache(tenantId).invalidateAll(keys);
    }

    @Override
    public void invalidateAll() {
        tenantsCache.forEach((tenantId, cache) -> {
            cache.invalidateAll();
        });
    }

    private Ticker customTicker;

    public void setCustomTicker(Ticker customTicker) {
        this.customTicker = customTicker;
    }

}
