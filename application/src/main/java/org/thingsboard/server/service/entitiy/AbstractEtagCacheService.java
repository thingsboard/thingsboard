// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.entitiy;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.thingsboard.server.common.data.HasTenantId;

import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public abstract class AbstractEtagCacheService<K extends HasTenantId> {

    public final Cache<K, String> etagCache;

    public AbstractEtagCacheService(int cacheTtl, int cacheMaxSize) {
        this.etagCache = Caffeine.newBuilder()
                .expireAfterAccess(cacheTtl, TimeUnit.MINUTES)
                .maximumSize(cacheMaxSize)
                .build();
    }

    public String getETag(K cacheKey) {
        return etagCache.getIfPresent(cacheKey);
    }

    public void putETag(K cacheKey, String etag) {
        etagCache.put(cacheKey, etag);
    }

    public void invalidateByFilter(Predicate<K> predicate) {
        Set<K> keysToInvalidate = etagCache.asMap().keySet().stream()
                .filter(predicate)
                .collect(Collectors.toSet());
        etagCache.invalidateAll(keysToInvalidate);
    }

}
