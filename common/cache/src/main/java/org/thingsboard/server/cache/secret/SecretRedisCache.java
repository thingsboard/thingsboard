// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.cache.secret;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cache.CacheSpecsMap;
import org.thingsboard.server.cache.RedisTbTransactionalCache;
import org.thingsboard.server.cache.TBRedisCacheConfiguration;
import org.thingsboard.server.cache.TbJsonRedisSerializer;
import org.thingsboard.server.common.data.CacheConstants;
import org.thingsboard.server.common.data.secret.Secret;

@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "redis")
@Service("SecretCache")
public class SecretRedisCache extends RedisTbTransactionalCache<SecretCacheKey, Secret> {

    public SecretRedisCache(TBRedisCacheConfiguration configuration, CacheSpecsMap cacheSpecsMap, RedisConnectionFactory connectionFactory) {
        super(CacheConstants.SECRETS_CACHE, cacheSpecsMap, connectionFactory, configuration, new TbJsonRedisSerializer<>(Secret.class));
    }

}
