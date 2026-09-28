// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.wl;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cache.CacheSpecsMap;
import org.thingsboard.server.cache.RedisTbTransactionalCache;
import org.thingsboard.server.cache.TBRedisCacheConfiguration;
import org.thingsboard.server.cache.TbJsonRedisSerializer;
import org.thingsboard.server.common.data.CacheConstants;
import org.thingsboard.server.common.data.wl.WhiteLabeling;

@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "redis")
@Service("WhiteLabelingCache")
public class WhiteLabelingRedisCache extends RedisTbTransactionalCache<WhiteLabelingCacheKey, WhiteLabeling> {

    public WhiteLabelingRedisCache(TBRedisCacheConfiguration configuration, CacheSpecsMap cacheSpecsMap, RedisConnectionFactory connectionFactory) {
        super(CacheConstants.WHITE_LABELING_CACHE, cacheSpecsMap, connectionFactory, configuration, new TbJsonRedisSerializer<>(WhiteLabeling.class));
    }
}
