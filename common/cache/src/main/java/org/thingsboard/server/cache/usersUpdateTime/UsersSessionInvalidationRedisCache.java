// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.cache.usersUpdateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cache.CacheSpecsMap;
import org.thingsboard.server.cache.RedisTbTransactionalCache;
import org.thingsboard.server.cache.TBRedisCacheConfiguration;
import org.thingsboard.server.cache.TbJsonRedisSerializer;
import org.thingsboard.server.common.data.CacheConstants;

@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "redis")
@Service("UsersSessionInvalidation")
public class UsersSessionInvalidationRedisCache extends RedisTbTransactionalCache<String, Long> {

    @Autowired
    public UsersSessionInvalidationRedisCache(TBRedisCacheConfiguration configuration, CacheSpecsMap cacheSpecsMap, RedisConnectionFactory connectionFactory) {
        super(CacheConstants.USERS_SESSION_INVALIDATION_CACHE, cacheSpecsMap, connectionFactory, configuration, new TbJsonRedisSerializer<>(Long.class));
    }
}
