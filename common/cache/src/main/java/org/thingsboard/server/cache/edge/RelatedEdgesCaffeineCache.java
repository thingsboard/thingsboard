// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.cache.edge;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cache.CaffeineTbTransactionalCache;
import org.thingsboard.server.common.data.CacheConstants;

@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "caffeine", matchIfMissing = true)
@Service("RelatedEdgeIdsCache")
public class RelatedEdgesCaffeineCache extends CaffeineTbTransactionalCache<RelatedEdgesCacheKey, RelatedEdgesCacheValue> {

    public RelatedEdgesCaffeineCache(CacheManager cacheManager) {
        super(cacheManager, CacheConstants.RELATED_EDGES_CACHE);
    }

}
