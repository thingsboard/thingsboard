// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.cache.agent;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cache.CaffeineTbTransactionalCache;
import org.thingsboard.server.common.data.CacheConstants;
import org.thingsboard.server.common.data.agent.AgentApplication;

@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "caffeine", matchIfMissing = true)
@Service("AgentApplicationCache")
public class AgentApplicationCaffeineCache extends CaffeineTbTransactionalCache<AgentApplicationCacheKey, AgentApplication> {

    public AgentApplicationCaffeineCache(CacheManager cacheManager) {
        super(cacheManager, CacheConstants.AGENT_APPLICATION_CACHE);
    }
}
