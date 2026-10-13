// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.alarm;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cache.CaffeineTbTransactionalCache;
import org.thingsboard.server.common.data.CacheConstants;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.HashSet;

@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "caffeine", matchIfMissing = true)
@Service("AlarmTypeNamesCache")
public class AlarmTypeNamesCaffeineCache extends CaffeineTbTransactionalCache<TenantId, HashSet<String>> {

    public AlarmTypeNamesCaffeineCache(CacheManager cacheManager) {
        super(cacheManager, CacheConstants.ALARM_TYPE_NAMES_CACHE);
    }

}
