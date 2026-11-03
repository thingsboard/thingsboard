// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.cache.edge;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cache.CacheSpecsMap;
import org.thingsboard.server.cache.RedisTbTransactionalCache;
import org.thingsboard.server.cache.TBRedisCacheConfiguration;
import org.thingsboard.server.cache.TbJsonRedisSerializer;
import org.thingsboard.server.common.data.CacheConstants;

@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "redis")
@Service("RelatedEdgeIdsCache")
public class RelatedEdgesRedisCache extends RedisTbTransactionalCache<RelatedEdgesCacheKey, RelatedEdgesCacheValue> {

    public RelatedEdgesRedisCache(TBRedisCacheConfiguration configuration, CacheSpecsMap cacheSpecsMap, RedisConnectionFactory connectionFactory) {
        super(CacheConstants.RELATED_EDGES_CACHE, cacheSpecsMap, connectionFactory, configuration, new TbJsonRedisSerializer<>(RelatedEdgesCacheValue.class));
    }

}
