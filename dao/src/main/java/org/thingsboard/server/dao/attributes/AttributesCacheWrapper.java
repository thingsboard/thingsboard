package org.thingsboard.server.dao.attributes;

import org.springframework.cache.Cache;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;

public interface AttributesCacheWrapper {

    Cache.ValueWrapper get(AttributeCacheKey attributeCacheKey);

    void put(AttributeCacheKey attributeCacheKey, AttributeKvEntry attributeKvEntry);

    void putIfAbsent(AttributeCacheKey attributeCacheKey, AttributeKvEntry attributeKvEntry);

    void evict(AttributeCacheKey attributeCacheKey);
}
