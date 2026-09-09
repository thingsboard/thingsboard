// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.pat;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cache.CacheSpecsMap;
import org.thingsboard.server.cache.RedisTbTransactionalCache;
import org.thingsboard.server.cache.TBRedisCacheConfiguration;
import org.thingsboard.server.cache.TbJsonRedisSerializer;
import org.thingsboard.server.common.data.CacheConstants;
import org.thingsboard.server.common.data.pat.ApiKey;

@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "redis")
@Service("ApiKeyCache")
public class ApiKeyRedisCache extends RedisTbTransactionalCache<ApiKeyCacheKey, ApiKey> {

    public ApiKeyRedisCache(TBRedisCacheConfiguration configuration, CacheSpecsMap cacheSpecsMap, RedisConnectionFactory connectionFactory) {
        super(CacheConstants.API_KEYS_CACHE, cacheSpecsMap, connectionFactory, configuration, new TbJsonRedisSerializer<>(ApiKey.class));
    }

}
