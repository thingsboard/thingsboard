// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.transport.auth.cache;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cache.CaffeineTbTransactionalCache;
import org.thingsboard.server.common.data.CacheConstants;
import org.thingsboard.server.gen.transport.TransportProtos.TokenValidationCacheEntryProto;

@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "caffeine", matchIfMissing = true)
@Service("TokenValidationCache")
public class TokenValidationCaffeineCache extends CaffeineTbTransactionalCache<String, TokenValidationCacheEntryProto> {

    public TokenValidationCaffeineCache(CacheManager cacheManager) {
        super(cacheManager, CacheConstants.TRANSPORT_TOKEN_VALIDATION_CACHE);
    }

}
