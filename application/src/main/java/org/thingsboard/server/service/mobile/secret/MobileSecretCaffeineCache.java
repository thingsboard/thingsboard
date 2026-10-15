// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.mobile.secret;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cache.CaffeineTbTransactionalCache;
import org.thingsboard.server.common.data.CacheConstants;
import org.thingsboard.server.common.data.security.model.JwtPair;

@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "caffeine", matchIfMissing = true)
@Service("MobileSecretCache")
public class MobileSecretCaffeineCache extends CaffeineTbTransactionalCache<String, JwtPair> {

    public MobileSecretCaffeineCache(CacheManager cacheManager) {
        super(cacheManager, CacheConstants.MOBILE_SECRET_KEY_CACHE);
    }

}
