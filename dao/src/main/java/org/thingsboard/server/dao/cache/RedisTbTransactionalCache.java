/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.server.dao.cache;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.thingsboard.server.cache.TbCacheTransaction;
import org.thingsboard.server.cache.TbCacheValueWrapper;
import org.thingsboard.server.cache.TbTransactionalCache;

import java.io.Serializable;
import java.util.List;

@RequiredArgsConstructor
public abstract class RedisTbTransactionalCache<K extends Serializable, V extends Serializable> implements TbTransactionalCache<K, V> {

    private final CacheManager cacheManager;
    @Getter
    private final String cacheName;
    private final RedisConnectionFactory connectionFactory;

    @Override
    public TbCacheValueWrapper<V> get(K key) {
        return null;
    }

    @Override
    public void putIfAbsent(K key, V value) {

    }

    @Override
    public void evict(K key) {

    }

    @Override
    public TbCacheTransaction<K, V> newTransactionForKey(K key) {
        return null;
    }

    @Override
    public TbCacheTransaction<K, V> newTransactionForKeys(List<K> keys) {
        return null;
    }
}
