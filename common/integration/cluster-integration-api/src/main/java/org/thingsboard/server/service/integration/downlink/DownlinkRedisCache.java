// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.integration.downlink;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Service;
import org.thingsboard.integration.api.data.DownLinkMsg;
import org.thingsboard.server.cache.CacheSpecsMap;
import org.thingsboard.server.cache.RedisTbTransactionalCache;
import org.thingsboard.server.cache.TBRedisCacheConfiguration;
import org.thingsboard.server.cache.TbJsonRedisSerializer;
import org.thingsboard.server.common.data.CacheConstants;

@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "redis")
@Service("DownlinkCache")
public class DownlinkRedisCache extends RedisTbTransactionalCache<DownlinkCacheKey, DownLinkMsg> {

    public DownlinkRedisCache(TBRedisCacheConfiguration configuration, CacheSpecsMap cacheSpecsMap, RedisConnectionFactory connectionFactory) {
        super(CacheConstants.DOWNLINK_CACHE, cacheSpecsMap, connectionFactory, configuration, new TbJsonRedisSerializer<>(DownLinkMsg.class));
    }
}
