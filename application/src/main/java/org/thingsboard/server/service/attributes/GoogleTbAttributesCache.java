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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class GoogleTbAttributesCache implements TbAttributesCache {
    private final Map<TenantId, Cache<AttributesKey, Optional<AttributeKvEntry>>> tenantsCache = new ConcurrentHashMap<>();

    @Autowired
    private AttributesCacheConfiguration cacheConfiguration;

    private Cache<AttributesKey, Optional<AttributeKvEntry>> getTenantCache(TenantId tenantId) {
        return tenantsCache.computeIfAbsent(tenantId,
                id -> CacheBuilder.newBuilder()
                        .maximumSize(cacheConfiguration.getMaxSize())
                        .expireAfterAccess(cacheConfiguration.getExpireAfterAccessInMinutes(), TimeUnit.MINUTES)
                        .build()
        );
    }

    @Override
    public Optional<AttributeKvEntry> find(TenantId tenantId, EntityId entityId, String scope, String key) {
        return getTenantCache(tenantId).getIfPresent(new AttributesKey(scope, entityId, key));
    }

    @Override
    public void put(TenantId tenantId, EntityId entityId, String scope, String key, AttributeKvEntry entry) {
        getTenantCache(tenantId).put(new AttributesKey(scope, entityId, key), Optional.ofNullable(entry));
    }

    @Override
    public void evict(TenantId tenantId, EntityId entityId, String scope, List<String> attributeKeys) {
        List<AttributesKey> keys = attributeKeys.stream().map(key -> new AttributesKey(scope, entityId, key)).collect(Collectors.toList());
        getTenantCache(tenantId).invalidateAll(keys);
    }
}
