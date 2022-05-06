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
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.support.NullValue;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.types.Expiration;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.thingsboard.server.cache.CacheSpecs;
import org.thingsboard.server.cache.CacheSpecsMap;
import org.thingsboard.server.cache.TBRedisCacheConfiguration;
import org.thingsboard.server.cache.TbCacheTransaction;
import org.thingsboard.server.cache.TbCacheValueWrapper;
import org.thingsboard.server.cache.TbTransactionalCache;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
public abstract class RedisTbTransactionalCache<K extends Serializable, V extends Serializable> implements TbTransactionalCache<K, V> {

    private static final byte[] BINARY_NULL_VALUE = RedisSerializer.java().serialize(NullValue.INSTANCE);

    @Getter
    private final String cacheName;
    private final RedisConnectionFactory connectionFactory;
    private final RedisSerializer<String> keySerializer = new StringRedisSerializer();
    private final RedisSerializer<V> valueSerializer;
    private final Expiration evictExpiration;
    private final Expiration cacheTtl;

    public RedisTbTransactionalCache(String cacheName,
                                     CacheSpecsMap cacheSpecsMap,
                                     RedisConnectionFactory connectionFactory,
                                     TBRedisCacheConfiguration configuration,
                                     RedisSerializer<V> valueSerializer) {
        this.cacheName = cacheName;
        this.connectionFactory = connectionFactory;
        this.valueSerializer = valueSerializer;
        this.evictExpiration = Expiration.from(configuration.getEvictTtlInMs(), TimeUnit.MILLISECONDS);
        CacheSpecs cacheSpecs = cacheSpecsMap.getSpecs().get(cacheName);
        if (cacheSpecs == null) {
            throw new RuntimeException("Missing cache specs for " + cacheSpecs);
        }
        this.cacheTtl = Expiration.from(cacheSpecs.getTimeToLiveInMinutes(), TimeUnit.MINUTES);
    }

    @Override
    public TbCacheValueWrapper<V> get(K key) {
        try (var connection = connectionFactory.getConnection()) {
            byte[] rawKey = getRawKey(key);
            byte[] rawValue = connection.get(rawKey);
            if (rawValue == null) {
                return null;
            } else if (Arrays.equals(rawValue, BINARY_NULL_VALUE)) {
                return SimpleTbCacheValueWrapper.empty();
            } else {
                V value = valueSerializer.deserialize(rawValue);
                return SimpleTbCacheValueWrapper.wrap(value);
            }
        }
    }

    @Override
    public void putIfAbsent(K key, V value) {
        try (var connection = connectionFactory.getConnection()) {
            putIfAbsent(connection, key, value);
        }
    }

    @Override
    public void evict(K key) {
        try (var connection = connectionFactory.getConnection()) {
            connection.del(getRawKey(key));
        }
    }

    @Override
    public void evictOrPut(K key, V value) {
        try (var connection = connectionFactory.getConnection()) {
            var rawKey = getRawKey(key);
            var records = connection.del(rawKey);
            if (records == null || records == 0) {
                //We need to put the value in case of Redis, because evict will NOT cancel concurrent transaction used to "get" the missing value from cache.
                connection.set(rawKey, getRawValue(value), evictExpiration, RedisStringCommands.SetOption.UPSERT);
            }
        }
    }

    @Override
    public TbCacheTransaction<K, V> newTransactionForKey(K key) {
        byte[][] rawKey = new byte[][]{getRawKey(key)};
        RedisConnection connection = watch(rawKey);
        return new RedisTbCacheTransaction<>(this, connection);
    }

    @Override
    public TbCacheTransaction<K, V> newTransactionForKeys(List<K> keys) {
        byte[][] rawKeysList = keys.stream().map(this::getRawKey).toArray(byte[][]::new);
        RedisConnection connection = watch(rawKeysList);
        return new RedisTbCacheTransaction<>(this, connection);
    }

    private RedisConnection watch(byte[][] rawKeysList) {
        var connection = connectionFactory.getConnection();
        try {
            connection.watch(rawKeysList);
            connection.multi();
        } catch (Exception e) {
            connection.close();
        }
        return connection;
    }

    private byte[] getRawKey(K key) {
        String keyString = cacheName + key.toString();
        byte[] rawKey;
        try {
            rawKey = keySerializer.serialize(keyString);
        } catch (Exception e) {
            log.warn("Failed to serialize the cache key: {}", key, e);
            throw new RuntimeException(e);
        }
        if (rawKey == null) {
            log.warn("Failed to serialize the cache key: {}", key);
            throw new IllegalArgumentException("Failed to serialize the cache key!");
        }
        return rawKey;
    }

    private byte[] getRawValue(V value) {
        if (value == null) {
            return BINARY_NULL_VALUE;
        } else {
            try {
                return valueSerializer.serialize(value);
            } catch (Exception e) {
                log.warn("Failed to serialize the cache value: {}", value, e);
                throw new RuntimeException(e);
            }
        }
    }

    public void putIfAbsent(RedisConnection connection, K key, V value) {
        byte[] rawKey = getRawKey(key);
        byte[] rawValue = getRawValue(value);
        connection.set(rawKey, rawValue, cacheTtl, RedisStringCommands.SetOption.SET_IF_ABSENT);
    }

}
