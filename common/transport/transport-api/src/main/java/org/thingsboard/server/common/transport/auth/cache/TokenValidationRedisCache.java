// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.transport.auth.cache;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cache.CacheSpecsMap;
import org.thingsboard.server.cache.RedisTbTransactionalCache;
import org.thingsboard.server.cache.TBRedisCacheConfiguration;
import org.thingsboard.server.common.data.CacheConstants;
import org.thingsboard.server.gen.transport.TransportProtos.TokenValidationCacheEntryProto;

@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "redis")
@Service("TokenValidationCache")
public class TokenValidationRedisCache extends RedisTbTransactionalCache<String, TokenValidationCacheEntryProto> {

    public TokenValidationRedisCache(TBRedisCacheConfiguration configuration, CacheSpecsMap cacheSpecsMap, RedisConnectionFactory connectionFactory) {
        super(CacheConstants.TRANSPORT_TOKEN_VALIDATION_CACHE, cacheSpecsMap, connectionFactory, configuration, new TokenValidationCacheEntrySerializer());
    }

}
