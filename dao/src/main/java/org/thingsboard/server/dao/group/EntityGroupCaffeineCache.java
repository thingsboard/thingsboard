// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.group;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cache.CaffeineTbTransactionalCache;
import org.thingsboard.server.common.data.CacheConstants;
import org.thingsboard.server.common.data.group.EntityGroup;

@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "caffeine", matchIfMissing = true)
@Service("EntityGroupCache")
public class EntityGroupCaffeineCache extends CaffeineTbTransactionalCache<EntityGroupCacheKey, EntityGroup> {

    public EntityGroupCaffeineCache(CacheManager cacheManager) {
        super(cacheManager, CacheConstants.ENTITY_GROUP_CACHE);
    }

}
