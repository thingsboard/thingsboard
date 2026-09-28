// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.cache.agent;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cache.CacheSpecsMap;
import org.thingsboard.server.cache.RedisTbTransactionalCache;
import org.thingsboard.server.cache.TBRedisCacheConfiguration;
import org.thingsboard.server.cache.TbJsonRedisSerializer;
import org.thingsboard.server.common.data.CacheConstants;
import org.thingsboard.server.common.data.agent.AgentAppProfile;

@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "redis")
@Service("AgentAppProfileCache")
public class AgentAppProfileRedisCache extends RedisTbTransactionalCache<AgentAppProfileCacheKey, AgentAppProfile> {

    public AgentAppProfileRedisCache(TBRedisCacheConfiguration configuration, CacheSpecsMap cacheSpecsMap, RedisConnectionFactory connectionFactory) {
        super(CacheConstants.AGENT_APP_PROFILE_CACHE, cacheSpecsMap, connectionFactory, configuration, new TbJsonRedisSerializer<>(AgentAppProfile.class));
    }
}
