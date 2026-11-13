// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.translation;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cache.CaffeineTbTransactionalCache;
import org.thingsboard.server.common.data.CacheConstants;
import org.thingsboard.server.common.data.translation.CustomTranslation;
import org.thingsboard.server.dao.model.sql.CustomTranslationCompositeKey;

@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "caffeine", matchIfMissing = true)
@Service("CustomTranslationCache")
public class CustomTranslationCaffeineCache extends CaffeineTbTransactionalCache<CustomTranslationCompositeKey, CustomTranslation> {

    public CustomTranslationCaffeineCache(CacheManager cacheManager) {
        super(cacheManager, CacheConstants.CUSTOM_TRANSLATION_CACHE);
    }

}
