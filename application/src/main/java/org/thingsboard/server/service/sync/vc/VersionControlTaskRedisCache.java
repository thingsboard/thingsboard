// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.sync.vc;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cache.CacheSpecsMap;
import org.thingsboard.server.cache.RedisTbTransactionalCache;
import org.thingsboard.server.cache.TBRedisCacheConfiguration;
import org.thingsboard.server.cache.TbJsonRedisSerializer;
import org.thingsboard.server.common.data.CacheConstants;

import java.util.UUID;

@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "redis")
@Service("VersionControlTaskCache")
public class VersionControlTaskRedisCache extends RedisTbTransactionalCache<UUID, VersionControlTaskCacheEntry> {

    public VersionControlTaskRedisCache(TBRedisCacheConfiguration configuration, CacheSpecsMap cacheSpecsMap, RedisConnectionFactory connectionFactory) {
        super(CacheConstants.VERSION_CONTROL_TASK_CACHE, cacheSpecsMap, connectionFactory, configuration, new TbJsonRedisSerializer<>(VersionControlTaskCacheEntry.class));
    }
}
