// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.user;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cache.CacheSpecsMap;
import org.thingsboard.server.cache.RedisTbTransactionalCache;
import org.thingsboard.server.cache.TBRedisCacheConfiguration;
import org.thingsboard.server.cache.TbJsonRedisSerializer;
import org.thingsboard.server.common.data.CacheConstants;
import org.thingsboard.server.common.data.settings.UserSettings;
import org.thingsboard.server.common.data.settings.UserSettingsCompositeKey;

@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "redis")
@Service("UserSettingsCache")
public class UserSettingsRedisCache extends RedisTbTransactionalCache<UserSettingsCompositeKey, UserSettings> {

    public UserSettingsRedisCache(TBRedisCacheConfiguration configuration, CacheSpecsMap cacheSpecsMap, RedisConnectionFactory connectionFactory) {
        super(CacheConstants.USER_SETTINGS_CACHE, cacheSpecsMap, connectionFactory, configuration, new TbJsonRedisSerializer<>(UserSettings.class));
    }
}
