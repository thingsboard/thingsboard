// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.sync.vc;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cache.CaffeineTbTransactionalCache;
import org.thingsboard.server.common.data.CacheConstants;

import java.util.UUID;

@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "caffeine", matchIfMissing = true)
@Service("VersionControlTaskCache")
public class VersionControlTaskCaffeineCache extends CaffeineTbTransactionalCache<UUID, VersionControlTaskCacheEntry> {

    public VersionControlTaskCaffeineCache(CacheManager cacheManager) {
        super(cacheManager, CacheConstants.VERSION_CONTROL_TASK_CACHE);
    }

}
