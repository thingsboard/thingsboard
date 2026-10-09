// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.role;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cache.VersionedCaffeineTbCache;
import org.thingsboard.server.common.data.CacheConstants;
import org.thingsboard.server.common.data.role.Role;

@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "caffeine", matchIfMissing = true)
@Service("RoleCache")
public class RoleCaffeineCache extends VersionedCaffeineTbCache<RoleCacheKey, Role> {

    public RoleCaffeineCache(CacheManager cacheManager) {
        super(cacheManager, CacheConstants.ROLE_CACHE);
    }

}
