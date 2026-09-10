// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.alarm;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cache.CaffeineTbTransactionalCache;
import org.thingsboard.server.common.data.CacheConstants;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;

@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "caffeine", matchIfMissing = true)
@Service("AlarmTypesCache")
public class AlarmTypesCaffeineCache extends CaffeineTbTransactionalCache<TenantId, PageData<EntitySubtype>> {

    public AlarmTypesCaffeineCache(CacheManager cacheManager) {
        super(cacheManager, CacheConstants.ALARM_TYPES_CACHE);
    }

}
