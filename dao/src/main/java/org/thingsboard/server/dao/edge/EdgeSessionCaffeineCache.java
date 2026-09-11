// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.edge;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cache.CaffeineTbTransactionalCache;
import org.thingsboard.server.common.data.CacheConstants;
import org.thingsboard.server.common.data.id.EdgeId;

@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "caffeine", matchIfMissing = true)
@Service("EdgeSessionCache")
public class EdgeSessionCaffeineCache extends CaffeineTbTransactionalCache<EdgeId, String> {

    public EdgeSessionCaffeineCache(CacheManager cacheManager) {
        super(cacheManager, CacheConstants.EDGE_SESSIONS_CACHE);
    }

}
