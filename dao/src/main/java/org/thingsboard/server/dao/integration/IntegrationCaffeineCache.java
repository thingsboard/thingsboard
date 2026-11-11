// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.integration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cache.VersionedCaffeineTbCache;
import org.thingsboard.server.common.data.CacheConstants;
import org.thingsboard.server.common.data.integration.Integration;

@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "caffeine", matchIfMissing = true)
@Service("IntegrationCache")
public class IntegrationCaffeineCache extends VersionedCaffeineTbCache<IntegrationCacheKey, Integration> {

    public IntegrationCaffeineCache(CacheManager cacheManager) {
        super(cacheManager, CacheConstants.INTEGRATIONS_CACHE);
    }

}
