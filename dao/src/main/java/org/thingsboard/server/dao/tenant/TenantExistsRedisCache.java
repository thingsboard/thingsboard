// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.tenant;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cache.CacheSpecsMap;
import org.thingsboard.server.cache.RedisTbTransactionalCache;
import org.thingsboard.server.cache.TBRedisCacheConfiguration;
import org.thingsboard.server.cache.TbJsonRedisSerializer;
import org.thingsboard.server.common.data.CacheConstants;
import org.thingsboard.server.common.data.id.TenantId;

@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "redis")
@Service("TenantExistsCache")
public class TenantExistsRedisCache extends RedisTbTransactionalCache<TenantId, Boolean> {

    public TenantExistsRedisCache(TBRedisCacheConfiguration configuration, CacheSpecsMap cacheSpecsMap, RedisConnectionFactory connectionFactory) {
        super(CacheConstants.TENANTS_EXIST_CACHE, cacheSpecsMap, connectionFactory, configuration, new TbJsonRedisSerializer<>(Boolean.class));
    }
}
