// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.role;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cache.CacheSpecsMap;
import org.thingsboard.server.cache.TBRedisCacheConfiguration;
import org.thingsboard.server.cache.TbJsonRedisSerializer;
import org.thingsboard.server.cache.VersionedRedisTbCache;
import org.thingsboard.server.common.data.CacheConstants;
import org.thingsboard.server.common.data.role.Role;

@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "redis")
@Service("RoleCache")
public class RoleRedisCache extends VersionedRedisTbCache<RoleCacheKey, Role> {

    public RoleRedisCache(TBRedisCacheConfiguration configuration, CacheSpecsMap cacheSpecsMap, RedisConnectionFactory connectionFactory) {
        super(CacheConstants.ROLE_CACHE, cacheSpecsMap, connectionFactory, configuration, new TbJsonRedisSerializer<>(Role.class));
    }

}
