// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.entityview;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cache.VersionedCaffeineTbCache;
import org.thingsboard.server.common.data.CacheConstants;

@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "caffeine", matchIfMissing = true)
@Service("EntityViewCache")
public class EntityViewCaffeineCache extends VersionedCaffeineTbCache<EntityViewCacheKey, EntityViewCacheValue> {

    public EntityViewCaffeineCache(CacheManager cacheManager) {
        super(cacheManager, CacheConstants.ENTITY_VIEW_CACHE);
    }

}
