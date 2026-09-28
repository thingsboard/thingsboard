// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.integration.rpc;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cache.CaffeineTbTransactionalCache;
import org.thingsboard.server.common.data.CacheConstants;
import org.thingsboard.server.common.data.id.IntegrationId;

@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "caffeine", matchIfMissing = true)
@Service("RemoteIntegrationCache")
public class IntegrationSessionCaffeineCache extends CaffeineTbTransactionalCache<IntegrationId, IntegrationSession> {

    public IntegrationSessionCaffeineCache(CacheManager cacheManager) {
        super(cacheManager, CacheConstants.REMOTE_INTEGRATIONS_CACHE);
    }

}
