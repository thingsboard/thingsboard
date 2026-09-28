// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.integration.rpc;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cache.CacheSpecsMap;
import org.thingsboard.server.cache.RedisTbTransactionalCache;
import org.thingsboard.server.cache.TBRedisCacheConfiguration;
import org.thingsboard.server.cache.TbJsonRedisSerializer;
import org.thingsboard.server.common.data.CacheConstants;
import org.thingsboard.server.common.data.id.IntegrationId;

@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "redis")
@Service("RemoteIntegrationCache")
public class IntegrationSessionRedisCache extends RedisTbTransactionalCache<IntegrationId, IntegrationSession> {

    public IntegrationSessionRedisCache(TBRedisCacheConfiguration configuration, CacheSpecsMap cacheSpecsMap, RedisConnectionFactory connectionFactory) {
        super(CacheConstants.REMOTE_INTEGRATIONS_CACHE, cacheSpecsMap, connectionFactory, configuration, new TbJsonRedisSerializer<>(IntegrationSession.class));
    }
}
