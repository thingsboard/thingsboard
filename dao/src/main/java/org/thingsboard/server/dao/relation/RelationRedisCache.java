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
package org.thingsboard.server.dao.relation;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cache.CacheSpecsMap;
import org.thingsboard.server.cache.TBRedisCacheConfiguration;
import org.thingsboard.server.common.data.CacheConstants;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.dao.attributes.AttributeCacheKey;
import org.thingsboard.server.dao.cache.RedisTbTransactionalCache;

@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "redis")
@Service("RelationCache")
public class RelationRedisCache extends RedisTbTransactionalCache<RelationCacheKey, RelationCacheValue> {

    public RelationRedisCache(TBRedisCacheConfiguration configuration, CacheSpecsMap cacheSpecsMap, RedisConnectionFactory connectionFactory) {
        super(CacheConstants.RELATIONS_CACHE, cacheSpecsMap, connectionFactory, configuration, new RedisSerializer<>() {

            private final RedisSerializer<Object> java = RedisSerializer.java();

            @Override
            public byte[] serialize(RelationCacheValue attributeKvEntry) throws SerializationException {
                return java.serialize(attributeKvEntry);
            }

            @Override
            public RelationCacheValue deserialize(byte[] bytes) throws SerializationException {
                return (RelationCacheValue) java.deserialize(bytes);
            }
        });
    }
}
