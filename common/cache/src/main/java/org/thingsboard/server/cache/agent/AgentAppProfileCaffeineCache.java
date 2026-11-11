// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.cache.agent;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cache.CaffeineTbTransactionalCache;
import org.thingsboard.server.common.data.CacheConstants;
import org.thingsboard.server.common.data.agent.AgentAppProfile;

@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "caffeine", matchIfMissing = true)
@Service("AgentAppProfileCache")
public class AgentAppProfileCaffeineCache extends CaffeineTbTransactionalCache<AgentAppProfileCacheKey, AgentAppProfile> {

    public AgentAppProfileCaffeineCache(CacheManager cacheManager) {
        super(cacheManager, CacheConstants.AGENT_APP_PROFILE_CACHE);
    }
}
