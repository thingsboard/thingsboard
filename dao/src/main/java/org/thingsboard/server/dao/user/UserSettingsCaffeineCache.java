// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.user;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cache.CaffeineTbTransactionalCache;
import org.thingsboard.server.common.data.CacheConstants;
import org.thingsboard.server.common.data.settings.UserSettings;
import org.thingsboard.server.common.data.settings.UserSettingsCompositeKey;

@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "caffeine", matchIfMissing = true)
@Service("UserSettingsCache")
public class UserSettingsCaffeineCache extends CaffeineTbTransactionalCache<UserSettingsCompositeKey, UserSettings> {

    public UserSettingsCaffeineCache(CacheManager cacheManager) {
        super(cacheManager, CacheConstants.USER_SETTINGS_CACHE);
    }

}
