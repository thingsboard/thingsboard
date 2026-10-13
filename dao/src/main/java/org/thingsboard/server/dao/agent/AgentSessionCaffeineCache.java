// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.agent;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cache.CaffeineTbTransactionalCache;
import org.thingsboard.server.common.data.CacheConstants;
import org.thingsboard.server.common.data.id.AgentId;

@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "caffeine", matchIfMissing = true)
@Service("AgentSessionCache")
public class AgentSessionCaffeineCache extends CaffeineTbTransactionalCache<AgentId, String> {

    public AgentSessionCaffeineCache(CacheManager cacheManager) {
        super(cacheManager, CacheConstants.AGENT_SESSIONS_CACHE);
    }

}
