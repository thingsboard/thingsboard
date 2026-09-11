// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.session;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cache.CaffeineTbTransactionalCache;
import org.thingsboard.server.common.data.CacheConstants;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.gen.transport.TransportProtos;

@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "caffeine", matchIfMissing = true)
@Service("SessionCache")
public class SessionCaffeineCache extends CaffeineTbTransactionalCache<DeviceId, TransportProtos.DeviceSessionsCacheEntry> {

    public SessionCaffeineCache(CacheManager cacheManager) {
        super(cacheManager, CacheConstants.SESSIONS_CACHE);
    }

}
