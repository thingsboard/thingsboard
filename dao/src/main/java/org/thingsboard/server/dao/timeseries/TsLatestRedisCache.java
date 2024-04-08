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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cache.CacheSpecsMap;
import org.thingsboard.server.cache.RedisTbTransactionalCache;
import org.thingsboard.server.cache.TBRedisCacheConfiguration;
import org.thingsboard.server.cache.TbJavaRedisSerializer;
import org.thingsboard.server.common.data.CacheConstants;
import org.thingsboard.server.common.data.kv.TsKvEntry;

import java.io.Serializable;

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

}
