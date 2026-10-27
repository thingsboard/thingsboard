// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.cache.agent;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cache.CaffeineTbTransactionalCache;
import org.thingsboard.server.common.data.CacheConstants;
import org.thingsboard.server.common.data.agent.AgentProfile;

@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "caffeine", matchIfMissing = true)
@Service("AgentProfileCache")
public class AgentProfileCaffeineCache extends CaffeineTbTransactionalCache<AgentProfileCacheKey, AgentProfile> {

    public AgentProfileCaffeineCache(CacheManager cacheManager) {
        super(cacheManager, CacheConstants.AGENT_PROFILE_CACHE);
    }
}
