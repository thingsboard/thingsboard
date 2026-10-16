// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.relation;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cache.CaffeineTbTransactionalCache;
import org.thingsboard.server.common.data.CacheConstants;

@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "caffeine", matchIfMissing = true)
@Service("RelationCache")
public class RelationCaffeineCache extends CaffeineTbTransactionalCache<RelationCacheKey, RelationCacheValue> {

    public RelationCaffeineCache(CacheManager cacheManager) {
        super(cacheManager, CacheConstants.RELATIONS_CACHE);
    }

}
