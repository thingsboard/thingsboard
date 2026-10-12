// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.cache.edge;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cache.CaffeineTbTransactionalCache;
import org.thingsboard.server.common.data.CacheConstants;
import org.thingsboard.server.common.data.edge.EdgeProfile;

@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "caffeine", matchIfMissing = true)
@Service("EdgeProfileCache")
public class EdgeProfileCaffeineCache extends CaffeineTbTransactionalCache<EdgeProfileCacheKey, EdgeProfile> {

    public EdgeProfileCaffeineCache(CacheManager cacheManager) {
        super(cacheManager, CacheConstants.EDGE_PROFILE_CACHE);
    }
}
