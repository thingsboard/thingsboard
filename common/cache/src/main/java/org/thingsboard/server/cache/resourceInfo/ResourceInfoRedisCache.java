// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.cache.resourceInfo;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cache.CacheSpecsMap;
import org.thingsboard.server.cache.RedisTbTransactionalCache;
import org.thingsboard.server.cache.TBRedisCacheConfiguration;
import org.thingsboard.server.cache.TbJsonRedisSerializer;
import org.thingsboard.server.common.data.CacheConstants;
import org.thingsboard.server.common.data.TbResourceInfo;

@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "redis")
@Service("ResourceInfoCache")
public class ResourceInfoRedisCache extends RedisTbTransactionalCache<ResourceInfoCacheKey, TbResourceInfo> {

    public ResourceInfoRedisCache(TBRedisCacheConfiguration configuration, CacheSpecsMap cacheSpecsMap, RedisConnectionFactory connectionFactory) {
        super(CacheConstants.RESOURCE_INFO_CACHE, cacheSpecsMap, connectionFactory, configuration, new TbJsonRedisSerializer<>(TbResourceInfo.class));
    }
}
