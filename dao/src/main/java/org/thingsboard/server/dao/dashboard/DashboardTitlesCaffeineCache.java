// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.dashboard;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cache.CaffeineTbTransactionalCache;
import org.thingsboard.server.common.data.CacheConstants;
import org.thingsboard.server.common.data.id.DashboardId;

@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "caffeine", matchIfMissing = true)
@Service("DashboardTitlesCache")
public class DashboardTitlesCaffeineCache extends CaffeineTbTransactionalCache<DashboardId, String> {

    public DashboardTitlesCaffeineCache(CacheManager cacheManager) {
        super(cacheManager, CacheConstants.DASHBOARD_TITLES_CACHE);
    }

}
