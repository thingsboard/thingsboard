package org.thingsboard.server.cache;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "caffeine", matchIfMissing = true)
@Service
@RequiredArgsConstructor
public class CaffeineTbTransactionalCacheManager implements TbTransactionalCache {

    private final CacheManager cacheManager;
    private final ConcurrentMap<String, CaffeineCacheTransactionStorage> caches = new ConcurrentHashMap<>();

    @Override
    public <K extends Serializable> Cache.ValueWrapper get(String cacheName, K key) {
        return cacheManager.getCache(cacheName).get(key);
    }

    @Override
    public <K extends Serializable, V extends Serializable> void putIfAbsent(String cacheName, K key, V value) {
        getCache(cacheName).putIfAbsent(key, value);
    }

    @Override
    public <K extends Serializable> void evict(String cacheName, K key) {
        getCache(cacheName).evict(key);
    }

    @Override
    public <K extends Serializable> TbCacheTransaction newTransactionForKey(String cacheName, K key) {
        return getCache(cacheName).newTransaction(Collections.singletonList(key));
    }

    @Override
    public <K extends Serializable> TbCacheTransaction newTransactionForKeys(String cacheName, List<K> keys) {
        return getCache(cacheName).newTransaction(keys);
    }

    private CaffeineCacheTransactionStorage getCache(String cacheName) {
        return caches.computeIfAbsent(cacheName, cn -> new CaffeineCacheTransactionStorage(cacheName, this));
    }

    <K extends Serializable, V extends Serializable> void doPutIfAbsent(String cacheName, Object key, Object value) {
        cacheManager.getCache(cacheName).putIfAbsent(key, value);
    }

    <K extends Serializable> void doEvict(String cacheName, K key) {
        cacheManager.getCache(cacheName).evict(key);
    }
}
