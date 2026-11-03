// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.sync.vc.autocommit;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cache.CacheSpecsMap;
import org.thingsboard.server.cache.RedisTbTransactionalCache;
import org.thingsboard.server.cache.TBRedisCacheConfiguration;
import org.thingsboard.server.cache.TbJsonRedisSerializer;
import org.thingsboard.server.common.data.CacheConstants;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.sync.vc.AutoCommitSettings;

@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "redis")
@Service("AutoCommitSettingsCache")
public class AutoCommitSettingsRedisCache extends RedisTbTransactionalCache<TenantId, AutoCommitSettings> {

    public AutoCommitSettingsRedisCache(TBRedisCacheConfiguration configuration, CacheSpecsMap cacheSpecsMap, RedisConnectionFactory connectionFactory) {
        super(CacheConstants.AUTO_COMMIT_SETTINGS_CACHE, cacheSpecsMap, connectionFactory, configuration, new TbJsonRedisSerializer<>(AutoCommitSettings.class));
    }
}
