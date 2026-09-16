// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.ai;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;
import org.thingsboard.server.cache.CacheSpecsMap;
import org.thingsboard.server.cache.TBRedisCacheConfiguration;
import org.thingsboard.server.cache.TbJsonRedisSerializer;
import org.thingsboard.server.cache.VersionedRedisTbCache;
import org.thingsboard.server.common.data.CacheConstants;
import org.thingsboard.server.common.data.ai.AiModel;

@Component("AiModelCache")
@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "redis")
class AiModelRedisCache extends VersionedRedisTbCache<AiModelCacheKey, AiModel> {

    AiModelRedisCache(TBRedisCacheConfiguration configuration, CacheSpecsMap cacheSpecsMap, RedisConnectionFactory connectionFactory) {
        super(CacheConstants.AI_MODEL_CACHE, cacheSpecsMap, connectionFactory, configuration, new TbJsonRedisSerializer<>(AiModel.class));
    }

}
