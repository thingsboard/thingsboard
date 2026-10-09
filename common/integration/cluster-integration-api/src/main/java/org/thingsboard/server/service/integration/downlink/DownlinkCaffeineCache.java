// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.integration.downlink;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.thingsboard.integration.api.data.DownLinkMsg;
import org.thingsboard.server.cache.CaffeineTbTransactionalCache;
import org.thingsboard.server.common.data.CacheConstants;

@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "caffeine", matchIfMissing = true)
@Service("DownlinkCache")
public class DownlinkCaffeineCache extends CaffeineTbTransactionalCache<DownlinkCacheKey, DownLinkMsg> {

    public DownlinkCaffeineCache(CacheManager cacheManager) {
        super(cacheManager, CacheConstants.DOWNLINK_CACHE);
    }

}
