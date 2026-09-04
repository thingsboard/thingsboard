// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.tenant;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cache.CaffeineTbTransactionalCache;
import org.thingsboard.server.common.data.CacheConstants;
import org.thingsboard.server.common.data.TenantProfile;

@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "caffeine", matchIfMissing = true)
@Service("TenantProfileCache")
public class TenantProfileCaffeineCache extends CaffeineTbTransactionalCache<TenantProfileCacheKey, TenantProfile> {

    public TenantProfileCaffeineCache(CacheManager cacheManager) {
        super(cacheManager, CacheConstants.TENANT_PROFILE_CACHE);
    }

}
