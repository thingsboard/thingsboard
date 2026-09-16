// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.entity.count;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cache.CaffeineTbTransactionalCache;
import org.thingsboard.server.common.data.CacheConstants;
import org.thingsboard.server.dao.entity.EntityCountCacheKey;

@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "caffeine", matchIfMissing = true)
@Service("EntityCountCache")
public class EntityCountCaffeineCache extends CaffeineTbTransactionalCache<EntityCountCacheKey, Long> {

    public EntityCountCaffeineCache(CacheManager cacheManager) {
        super(cacheManager, CacheConstants.ENTITY_COUNT_CACHE);
    }

}
