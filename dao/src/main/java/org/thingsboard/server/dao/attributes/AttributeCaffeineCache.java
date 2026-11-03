// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.attributes;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cache.VersionedCaffeineTbCache;
import org.thingsboard.server.common.data.CacheConstants;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;

@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "caffeine", matchIfMissing = true)
@Service("AttributeCache")
public class AttributeCaffeineCache extends VersionedCaffeineTbCache<AttributeCacheKey, AttributeKvEntry> {

    public AttributeCaffeineCache(CacheManager cacheManager) {
        super(cacheManager, CacheConstants.ATTRIBUTES_CACHE);
    }

}
