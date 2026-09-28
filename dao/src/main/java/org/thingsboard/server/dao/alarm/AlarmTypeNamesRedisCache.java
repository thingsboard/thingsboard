// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.alarm;

import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cache.CacheSpecsMap;
import org.thingsboard.server.cache.RedisTbTransactionalCache;
import org.thingsboard.server.cache.TBRedisCacheConfiguration;
import org.thingsboard.server.cache.TbTypedJsonRedisSerializer;
import org.thingsboard.server.common.data.CacheConstants;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.HashSet;

@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "redis")
@Service("AlarmTypeNamesCache")
public class AlarmTypeNamesRedisCache extends RedisTbTransactionalCache<TenantId, HashSet<String>> {

    public AlarmTypeNamesRedisCache(TBRedisCacheConfiguration configuration, CacheSpecsMap cacheSpecsMap, RedisConnectionFactory connectionFactory) {
        super(CacheConstants.ALARM_TYPE_NAMES_CACHE, cacheSpecsMap, connectionFactory, configuration, new TbTypedJsonRedisSerializer<>(new TypeReference<>() {}));
    }
}
