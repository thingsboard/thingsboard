// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.pat;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cache.CaffeineTbTransactionalCache;
import org.thingsboard.server.common.data.CacheConstants;
import org.thingsboard.server.common.data.pat.ApiKey;

@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "caffeine", matchIfMissing = true)
@Service("ApiKeyCache")
public class ApiKeyCaffeineCache extends CaffeineTbTransactionalCache<ApiKeyCacheKey, ApiKey> {

    public ApiKeyCaffeineCache(CacheManager cacheManager) {
        super(cacheManager, CacheConstants.API_KEYS_CACHE);
    }

}
