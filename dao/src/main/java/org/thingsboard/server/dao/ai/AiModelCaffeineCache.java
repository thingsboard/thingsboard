// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.ai;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.thingsboard.server.cache.VersionedCaffeineTbCache;
import org.thingsboard.server.common.data.CacheConstants;
import org.thingsboard.server.common.data.ai.AiModel;

@Component("AiModelCache")
@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "caffeine", matchIfMissing = true)
class AiModelCaffeineCache extends VersionedCaffeineTbCache<AiModelCacheKey, AiModel> {

    AiModelCaffeineCache(CacheManager cacheManager) {
        super(cacheManager, CacheConstants.AI_MODEL_CACHE);
    }

}
