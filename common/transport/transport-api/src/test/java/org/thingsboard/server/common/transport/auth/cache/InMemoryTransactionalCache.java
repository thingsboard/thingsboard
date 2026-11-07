// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.transport.auth.cache;

import org.thingsboard.server.cache.TbCacheTransaction;
import org.thingsboard.server.cache.TbCacheValueWrapper;
import org.thingsboard.server.cache.TbTransactionalCache;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Map-backed stand-in for a remote {@link TbTransactionalCache} with Redis SET / SET NX / DEL semantics.
 */
public class InMemoryTransactionalCache<K extends Serializable, V extends Serializable> implements TbTransactionalCache<K, V> {

    public final Map<K, V> map = new ConcurrentHashMap<>();
    public final AtomicInteger getCount = new AtomicInteger();

    @Override
    public String getCacheName() {
        return "in-memory";
    }

    @Override
    public TbCacheValueWrapper<V> get(K key) {
        getCount.incrementAndGet();
        V value = map.get(key);
        return value != null ? () -> value : null;
    }

    @Override
    public void put(K key, V value) {
        map.put(key, value);
    }

    @Override
    public void putIfAbsent(K key, V value) {
        map.putIfAbsent(key, value);
    }

    @Override
    public void evict(K key) {
        map.remove(key);
    }

    @Override
    public void evict(Collection<K> keys) {
        keys.forEach(map::remove);
    }

    @Override
    public void evictOrPut(K key, V value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void evictByPrefix(String prefix) {
        throw new UnsupportedOperationException();
    }

    @Override
    public TbCacheTransaction<K, V> newTransactionForKey(K key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public TbCacheTransaction<K, V> newTransactionForKeys(List<K> keys) {
        throw new UnsupportedOperationException();
    }

}
