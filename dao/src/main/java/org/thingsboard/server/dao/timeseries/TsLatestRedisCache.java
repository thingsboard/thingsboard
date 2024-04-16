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
package org.thingsboard.server.dao.timeseries;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cache.CacheSpecsMap;
import org.thingsboard.server.cache.RedisTbTransactionalCache;
import org.thingsboard.server.cache.TBRedisCacheConfiguration;
import org.thingsboard.server.cache.TbCacheTransaction;
import org.thingsboard.server.cache.TbCacheValueWrapper;
import org.thingsboard.server.cache.TbJavaRedisSerializer;
import org.thingsboard.server.common.data.CacheConstants;
import org.thingsboard.server.common.data.kv.TsKvEntry;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "redis")
@Service("TsLatestCache")
@Slf4j
public class TsLatestRedisCache<K extends Serializable, V extends Serializable> extends RedisTbTransactionalCache<TsLatestCacheKey, TsKvEntry> {

    public TsLatestRedisCache(TBRedisCacheConfiguration configuration, CacheSpecsMap cacheSpecsMap, RedisConnectionFactory connectionFactory) {
        super(CacheConstants.TS_LATEST_CACHE, cacheSpecsMap, connectionFactory, configuration, new TbJavaRedisSerializer<>());
    }

    @Override
    public void put(TsLatestCacheKey key, TsKvEntry value) {
        log.trace("put [{}][{}]", key, value);
        final byte[] rawKey = getRawKey(key);
        try (var connection = getConnection(rawKey)) {
            put(connection, rawKey, value, RedisStringCommands.SetOption.UPSERT);
        }
    }

    @Override
    public void putIfAbsent(TsLatestCacheKey key, TsKvEntry value) {
        log.trace("putIfAbsent [{}][{}]", key, value);
        throw new NotImplementedException("putIfAbsent is not supported by TsLatestRedisCache");
    }

    @Override
    public TbCacheValueWrapper<TsKvEntry> get(TsLatestCacheKey key) {
        log.debug("get [{}]", key);
        return super.get(key);
    }

    @Override
    protected byte[] doGet(RedisConnection connection, byte[] rawKey) {
        log.trace("doGet [{}][{}]", connection, rawKey);
        return connection.stringCommands().get(rawKey);
    }

    @Override
    public void evict(TsLatestCacheKey key){
        log.trace("evict [{}]", key);
        final byte[] rawKey = getRawKey(key);
        try (var connection = getConnection(rawKey)) {
            connection.keyCommands().del(rawKey);
        }
    }

    @Override
    public void evict(Collection<TsLatestCacheKey> keys) {
        throw new NotImplementedException("evict by many keys is not supported by TsLatestRedisCache");
    }

    @Override
    public void evictOrPut(TsLatestCacheKey key, TsKvEntry value) {
        throw new NotImplementedException("evictOrPut is not supported by TsLatestRedisCache");
    }

    @Override
    public TbCacheTransaction<TsLatestCacheKey, TsKvEntry> newTransactionForKey(TsLatestCacheKey key) {
        throw new NotImplementedException("newTransactionForKey is not supported by TsLatestRedisCache");
    }

    @Override
    public TbCacheTransaction<TsLatestCacheKey, TsKvEntry> newTransactionForKeys(List<TsLatestCacheKey> keys) {
        throw new NotImplementedException("newTransactionForKeys is not supported by TsLatestRedisCache");
    }

}
