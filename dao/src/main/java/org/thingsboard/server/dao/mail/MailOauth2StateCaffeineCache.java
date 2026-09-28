// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.mail;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cache.CaffeineTbTransactionalCache;
import org.thingsboard.server.common.data.CacheConstants;
import org.thingsboard.server.common.data.id.TenantId;

@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "caffeine", matchIfMissing = true)
@Service("MailOauth2StateCache")
public class MailOauth2StateCaffeineCache extends CaffeineTbTransactionalCache<String, TenantId> {

    public MailOauth2StateCaffeineCache(CacheManager cacheManager) {
        super(cacheManager, CacheConstants.MAIL_OAUTH2_STATE_CACHE);
    }

}
