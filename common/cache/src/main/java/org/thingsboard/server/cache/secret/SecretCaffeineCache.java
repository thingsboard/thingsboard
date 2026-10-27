// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.cache.secret;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cache.CaffeineTbTransactionalCache;
import org.thingsboard.server.common.data.CacheConstants;
import org.thingsboard.server.common.data.secret.Secret;

@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "caffeine", matchIfMissing = true)
@Service("SecretCache")
public class SecretCaffeineCache extends CaffeineTbTransactionalCache<SecretCacheKey, Secret> {

    public SecretCaffeineCache(CacheManager cacheManager) {
        super(cacheManager, CacheConstants.SECRETS_CACHE);
    }

}
