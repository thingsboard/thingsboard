package org.thingsboard.server.cache;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.List;

@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "redis")
@Service
@RequiredArgsConstructor
public class RedisTbTransactionalCacheManager implements TbTransactionalCache {

    private final CacheManager cacheManager;

    @Override
    public <K extends Serializable> Cache.ValueWrapper get(String cacheName, K key) {
        return cacheManager.getCache(cacheName).get(key);
    }

    @Override
    public <K extends Serializable, V extends Serializable> void putIfAbsent(String cacheName, K key, V value) {
    }

    @Override
    public <K extends Serializable> void evict(String cacheName, K key) {
    }

    @Override
    public <K extends Serializable> TbCacheTransaction newTransactionForKey(String cacheName, K key) {
        return null;
    }

    @Override
    public <K extends Serializable> TbCacheTransaction newTransactionForKeys(String cacheName, List<K> keys) {
        return null;
    }

}
