/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
package org.thingsboard.server.cache;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.thingsboard.server.common.data.HasVersion;
import org.thingsboard.server.common.data.util.TbPair;

import java.io.Serializable;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@RequiredArgsConstructor
public abstract class VersionedCaffeineTbCache<K extends Serializable, V extends Serializable & HasVersion> implements VersionedTbCache<K, V> {

    private final CacheManager cacheManager;
    private final String cacheName;

    private final Lock lock = new ReentrantLock();

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
        Long version = value != null ? value.getVersion() : 0;
        doPut(key, value, version);
    }

    private void doPut(K key, V value, Long version) {
        if (version == null) {
            return;
        }
        lock.lock();
        try {
            TbPair<Long, V> versionValuePair = doGet(key);
            if (versionValuePair == null || version > versionValuePair.getFirst()) {
                cacheManager.getCache(cacheName).put(key, TbPair.of(version, value));
            }
        } finally {
            lock.unlock();
        }
    }

    private TbPair<Long, V> doGet(K key) {
        Cache.ValueWrapper source = cacheManager.getCache(cacheName).get(key);
        return source == null ? null : (TbPair<Long, V>) source.get();
    }

    @Override
    public void evict(K key) {
        lock.lock();
        try {
            cacheManager.getCache(cacheName).evict(key);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void evict(K key, Long version) {
        if (version == null) {
            return;
        }
        lock.lock();
        try {
            doPut(key, null, version);
        } finally {
            lock.unlock();
        }
    }
}
