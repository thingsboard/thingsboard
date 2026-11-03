// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.cache.usersUpdateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cache.CaffeineTbTransactionalCache;
import org.thingsboard.server.common.data.CacheConstants;


@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "caffeine", matchIfMissing = true)
@Service("UsersSessionInvalidation")
public class UsersSessionInvalidationCaffeineCache extends CaffeineTbTransactionalCache<String, Long> {

    @Autowired
    public UsersSessionInvalidationCaffeineCache(CacheManager cacheManager) {
        super(cacheManager, CacheConstants.USERS_SESSION_INVALIDATION_CACHE);
    }
}
