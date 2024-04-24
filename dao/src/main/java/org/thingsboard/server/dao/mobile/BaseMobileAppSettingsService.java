/**
 * Copyright © 2016-2024 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.dao.mobile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.mobile.AndroidConfig;
import org.thingsboard.server.common.data.mobile.BadgePosition;
import org.thingsboard.server.common.data.mobile.BadgeStyle;
import org.thingsboard.server.common.data.mobile.IosConfig;
import org.thingsboard.server.common.data.mobile.MobileAppSettings;
import org.thingsboard.server.common.data.mobile.QRCodeConfig;
import org.thingsboard.server.dao.entity.AbstractCachedService;

@Service
@Slf4j
@RequiredArgsConstructor
public class BaseMobileAppSettingsService extends AbstractCachedService<TenantId, MobileAppSettings, MobileAppSettingsEvictEvent> implements MobileAppSettingsService {

    private static final String DEFAULT_QR_CODE_LABEL = "Scan to connect or download mobile app";
    private final MobileAppSettingsDao mobileAppSettingsDao;

    @Override
    public MobileAppSettings saveMobileAppSettings(TenantId tenantId, MobileAppSettings settings) {
        MobileAppSettings mobileAppSettings = mobileAppSettingsDao.save(tenantId, settings);
        publishEvictEvent(new MobileAppSettingsEvictEvent(tenantId));
        return mobileAppSettings;
    }

    @Override
    public MobileAppSettings getMobileAppSettings(TenantId tenantId) {
        log.trace("Executing getMobileAppSettings for tenant [{}] ", tenantId);
        MobileAppSettings mobileAppSettings = cache.getAndPutInTransaction(tenantId,
                () -> mobileAppSettingsDao.findByTenantId(tenantId), true);
        return constructMobileAppSettings(mobileAppSettings);
    }

    @TransactionalEventListener(classes = MobileAppSettingsEvictEvent.class)
    @Override
    public void handleEvictEvent(MobileAppSettingsEvictEvent event) {
        cache.evict(event.getTenantId());
    }

    private MobileAppSettings constructMobileAppSettings(MobileAppSettings mobileAppSettings) {
        if (mobileAppSettings == null) {
            mobileAppSettings = new MobileAppSettings();
            mobileAppSettings.setUseDefault(true);

            AndroidConfig androidConfig = AndroidConfig.builder()
                    .enabled(true)
                    .build();
            IosConfig iosConfig = IosConfig.builder()
                    .enabled(true)
                    .build();
            QRCodeConfig qrCodeConfig = QRCodeConfig.builder()
                    .showOnHomePage(true)
                    .qrCodeLabelEnabled(true)
                    .qrCodeLabel(DEFAULT_QR_CODE_LABEL)
                    .badgeEnabled(true)
                    .badgePosition(BadgePosition.RIGHT)
                    .badgeStyle(BadgeStyle.ORIGINAL)
                    .badgeEnabled(true)
                    .build();

            mobileAppSettings.setQrCodeConfig(qrCodeConfig);
            mobileAppSettings.setAndroidConfig(androidConfig);
            mobileAppSettings.setIosConfig(iosConfig);
        }
        return mobileAppSettings;
    }

}
