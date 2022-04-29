/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.server.dao.attributes;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;

import static org.thingsboard.server.common.data.CacheConstants.ATTRIBUTES_CACHE;

@Service
@ConditionalOnProperty(prefix = "cache.attributes", value = "enabled", havingValue = "true")
@Primary
@Slf4j
public class DefaultAttributesCacheWrapper implements AttributesCacheWrapper {
    private final Cache attributesCache;

    public DefaultAttributesCacheWrapper(CacheManager cacheManager) {
        this.attributesCache = cacheManager.getCache(ATTRIBUTES_CACHE);
    }

    @Override
    public Cache.ValueWrapper get(AttributeCacheKey attributeCacheKey) {
        var result = attributesCache.get(attributeCacheKey);
        log.warn("[{}] Get = {}", attributeCacheKey, result);
        return result;
    }

    @Override
    public void put(AttributeCacheKey attributeCacheKey, AttributeKvEntry attributeKvEntry) {
        log.warn("[{}] Put = {}", attributeCacheKey, attributeKvEntry);
        attributesCache.put(attributeCacheKey, attributeKvEntry);
    }

    @Override
    public void putIfAbsent(AttributeCacheKey attributeCacheKey, AttributeKvEntry attributeKvEntry) {
        var result = attributesCache.putIfAbsent(attributeCacheKey, attributeKvEntry);
        log.warn("[{}] Put if absent = {}, result = {}", attributeCacheKey, attributeKvEntry, result);
    }

    @Override
    public void evict(AttributeCacheKey attributeCacheKey) {
        log.warn("[{}] Evict", attributeCacheKey);
        attributesCache.evict(attributeCacheKey);
    }
}
