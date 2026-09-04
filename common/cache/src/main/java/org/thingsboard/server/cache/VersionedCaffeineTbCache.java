// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.cache;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.thingsboard.server.common.data.HasVersion;
import org.thingsboard.server.common.data.util.TbPair;

import java.io.Serializable;

public abstract class VersionedCaffeineTbCache<K extends VersionedCacheKey, V extends Serializable & HasVersion> extends CaffeineTbTransactionalCache<K, V> implements VersionedTbCache<K, V> {

    public VersionedCaffeineTbCache(CacheManager cacheManager, String cacheName) {
        super(cacheManager, cacheName);
    }

    @Override
    public TbCacheValueWrapper<V> get(K key) {
        TbPair<Long, V> versionValuePair = doGet(key);
        if (versionValuePair != null) {
            return SimpleTbCacheValueWrapper.wrap(versionValuePair.getSecond());
        }
        return null;
    }

    @Override
    public void put(K key, V value) {
        Long version = getVersion(value);
        if (version == null) {
            return;
        }
        doPut(key, value, version);
    }

    private void doPut(K key, V value, Long version) {
        lock.lock();
        try {
            TbPair<Long, V> versionValuePair = doGet(key);
            if (versionValuePair == null || version > versionValuePair.getFirst()) {
                failAllTransactionsByKey(key);
                cache.put(key, wrapValue(value, version));
            }
        } finally {
            lock.unlock();
        }
    }

    private TbPair<Long, V> doGet(K key) {
        Cache.ValueWrapper source = cache.get(key);
        return source == null ? null : (TbPair<Long, V>) source.get();
    }

    @Override
    public void evict(K key) {
        lock.lock();
        try {
            failAllTransactionsByKey(key);
            cache.evict(key);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void evict(K key, Long version) {
        if (version == null) {
            return;
        }
        doPut(key, null, version);
    }

    @Override
    void doPutIfAbsent(K key, V value) {
        cache.putIfAbsent(key, wrapValue(value, getVersion(value)));
    }

    private TbPair<Long, V> wrapValue(V value, Long version) {
        return TbPair.of(version, value);
    }

}
