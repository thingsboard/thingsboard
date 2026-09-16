// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.ota;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cache.CaffeineTbTransactionalCache;
import org.thingsboard.server.common.data.CacheConstants;
import org.thingsboard.server.common.data.OtaPackageInfo;

@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "caffeine", matchIfMissing = true)
@Service("OtaPackageCache")
public class OtaPackageCaffeineCache extends CaffeineTbTransactionalCache<OtaPackageCacheKey, OtaPackageInfo> {

    public OtaPackageCaffeineCache(CacheManager cacheManager) {
        super(cacheManager, CacheConstants.OTA_PACKAGE_CACHE);
    }

}
