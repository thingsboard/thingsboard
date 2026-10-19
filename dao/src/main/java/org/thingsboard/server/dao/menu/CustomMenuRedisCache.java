// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.menu;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cache.CacheSpecsMap;
import org.thingsboard.server.cache.RedisTbTransactionalCache;
import org.thingsboard.server.cache.TBRedisCacheConfiguration;
import org.thingsboard.server.cache.TbJsonRedisSerializer;
import org.thingsboard.server.common.data.CacheConstants;
import org.thingsboard.server.common.data.id.CustomMenuId;
import org.thingsboard.server.common.data.menu.CustomMenu;

@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "redis")
@Service("CustomMenuCache")
public class CustomMenuRedisCache extends RedisTbTransactionalCache<CustomMenuId, CustomMenu> {

    public CustomMenuRedisCache(TBRedisCacheConfiguration configuration, CacheSpecsMap cacheSpecsMap, RedisConnectionFactory connectionFactory) {
        super(CacheConstants.CUSTOM_MENU_CACHE, cacheSpecsMap, connectionFactory, configuration, new TbJsonRedisSerializer<>(CustomMenu.class));
    }
}
