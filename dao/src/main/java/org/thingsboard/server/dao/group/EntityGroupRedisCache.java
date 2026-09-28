// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.group;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cache.CacheSpecsMap;
import org.thingsboard.server.cache.RedisTbTransactionalCache;
import org.thingsboard.server.cache.TBRedisCacheConfiguration;
import org.thingsboard.server.cache.TbJsonRedisSerializer;
import org.thingsboard.server.common.data.CacheConstants;
import org.thingsboard.server.common.data.group.EntityGroup;

@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "redis")
@Service("EntityGroupCache")
public class EntityGroupRedisCache extends RedisTbTransactionalCache<EntityGroupCacheKey, EntityGroup> {

    public EntityGroupRedisCache(CacheSpecsMap cacheSpecsMap, RedisConnectionFactory connectionFactory, TBRedisCacheConfiguration configuration) {
        super(CacheConstants.ENTITY_GROUP_CACHE, cacheSpecsMap, connectionFactory, configuration, new TbJsonRedisSerializer<>(EntityGroup.class));
    }

}
