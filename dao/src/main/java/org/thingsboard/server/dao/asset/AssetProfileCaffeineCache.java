// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.asset;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cache.VersionedCaffeineTbCache;
import org.thingsboard.server.common.data.CacheConstants;
import org.thingsboard.server.common.data.asset.AssetProfile;

@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "caffeine", matchIfMissing = true)
@Service("AssetProfileCache")
public class AssetProfileCaffeineCache extends VersionedCaffeineTbCache<AssetProfileCacheKey, AssetProfile> {

    public AssetProfileCaffeineCache(CacheManager cacheManager) {
        super(cacheManager, CacheConstants.ASSET_PROFILE_CACHE);
    }

}
