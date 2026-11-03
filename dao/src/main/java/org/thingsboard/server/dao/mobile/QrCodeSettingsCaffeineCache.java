// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.mobile;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cache.CaffeineTbTransactionalCache;
import org.thingsboard.server.common.data.CacheConstants;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.mobile.qrCodeSettings.QrCodeSettings;

@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "caffeine", matchIfMissing = true)
@Service("QrCodeSettingsCache")
public class QrCodeSettingsCaffeineCache extends CaffeineTbTransactionalCache<TenantId, QrCodeSettings> {

    public QrCodeSettingsCaffeineCache(CacheManager cacheManager) {
        super(cacheManager, CacheConstants.QR_CODE_SETTINGS_CACHE);
    }

}
