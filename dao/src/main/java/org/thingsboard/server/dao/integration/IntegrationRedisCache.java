// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.integration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cache.CacheSpecsMap;
import org.thingsboard.server.cache.TBRedisCacheConfiguration;
import org.thingsboard.server.cache.TbJsonRedisSerializer;
import org.thingsboard.server.cache.VersionedRedisTbCache;
import org.thingsboard.server.common.data.CacheConstants;
import org.thingsboard.server.common.data.integration.Integration;

@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "redis")
@Service("IntegrationCache")
public class IntegrationRedisCache extends VersionedRedisTbCache<IntegrationCacheKey, Integration> {

    public IntegrationRedisCache(TBRedisCacheConfiguration configuration, CacheSpecsMap cacheSpecsMap, RedisConnectionFactory connectionFactory) {
        super(CacheConstants.INTEGRATIONS_CACHE, cacheSpecsMap, connectionFactory, configuration, new TbJsonRedisSerializer<>(Integration.class));
    }

}
