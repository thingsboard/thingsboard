// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.user;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cache.CaffeineTbTransactionalCache;
import org.thingsboard.server.common.data.CacheConstants;
import org.thingsboard.server.common.data.permission.MergedUserPermissions;

@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "caffeine", matchIfMissing = true)
@Service("PermissionCache")
public class UserPermissionCaffeineCache extends CaffeineTbTransactionalCache<UserPermissionCacheKey, MergedUserPermissions> {

    public UserPermissionCaffeineCache(CacheManager cacheManager) {
        super(cacheManager, CacheConstants.USER_PERMISSIONS_CACHE);
    }

}
