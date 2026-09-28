// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.mail;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cache.CacheSpecsMap;
import org.thingsboard.server.cache.RedisTbTransactionalCache;
import org.thingsboard.server.cache.TBRedisCacheConfiguration;
import org.thingsboard.server.cache.TbJsonRedisSerializer;
import org.thingsboard.server.common.data.CacheConstants;
import org.thingsboard.server.common.data.id.CustomMenuId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.menu.CustomMenu;

@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "redis")
@Service("MailOauth2StateCache")
public class MailOauth2StateRedisCache extends RedisTbTransactionalCache<String, TenantId> {

    public MailOauth2StateRedisCache(TBRedisCacheConfiguration configuration, CacheSpecsMap cacheSpecsMap, RedisConnectionFactory connectionFactory) {
        super(CacheConstants.MAIL_OAUTH2_STATE_CACHE, cacheSpecsMap, connectionFactory, configuration, new TbJsonRedisSerializer<>(TenantId.class));
    }
}
