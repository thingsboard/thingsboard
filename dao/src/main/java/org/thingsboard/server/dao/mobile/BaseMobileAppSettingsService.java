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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.mobile.AndroidConfig;
import org.thingsboard.server.common.data.mobile.BadgePosition;
import org.thingsboard.server.common.data.mobile.IosConfig;
import org.thingsboard.server.common.data.mobile.MobileAppSettings;
import org.thingsboard.server.common.data.mobile.QRCodeConfig;
import org.thingsboard.server.dao.entity.AbstractCachedEntityService;
import org.thingsboard.server.dao.service.DataValidator;

import java.util.Map;

import static org.thingsboard.server.dao.service.Validator.validateId;

@Service
@Slf4j
@RequiredArgsConstructor
public class BaseMobileAppSettingsService extends AbstractCachedEntityService<TenantId, MobileAppSettings, MobileAppSettingsEvictEvent> implements MobileAppSettingsService {

    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    private static final String DEFAULT_QR_CODE_LABEL = "Scan to connect or download mobile app";

    @Value("${mobileApp.googlePlayLink:https://play.google.com/store/apps/details?id=org.thingsboard.demo.app}")
    private String googlePlayLink;
    @Value("${mobileApp.appStoreLink:https://apps.apple.com/us/app/thingsboard-live/id1594355695}")
    private String appStoreLink;

    private final MobileAppSettingsDao mobileAppSettingsDao;
    private final DataValidator<MobileAppSettings> mobileAppSettingsDataValidator;

    @Override
    public MobileAppSettings saveMobileAppSettings(TenantId tenantId, MobileAppSettings mobileAppSettings) {
        mobileAppSettingsDataValidator.validate(mobileAppSettings, s -> tenantId);
        try {
            MobileAppSettings savedMobileAppSettings = mobileAppSettingsDao.save(tenantId, mobileAppSettings);
            publishEvictEvent(new MobileAppSettingsEvictEvent(tenantId));
            return constructMobileAppSettings(savedMobileAppSettings);
        } catch (Exception e) {
            handleEvictEvent(new MobileAppSettingsEvictEvent(tenantId));
            checkConstraintViolation(e, Map.of(
                    "mobile_app_settings_tenant_id_unq_key", "Mobile application for specified tenant already exists!"
            ));
            throw e;
        }
    }

    @Override
    public MobileAppSettings getMobileAppSettings(TenantId tenantId) {
        log.trace("Executing getMobileAppSettings for tenant [{}] ", tenantId);
        MobileAppSettings mobileAppSettings = cache.getAndPutInTransaction(tenantId,
                () -> mobileAppSettingsDao.findByTenantId(tenantId), true);
        return constructMobileAppSettings(mobileAppSettings);
    }

    @Override
    public void deleteByTenantId(TenantId tenantId) {
        log.trace("Executing deleteByTenantId, tenantId [{}]", tenantId);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        mobileAppSettingsDao.removeByTenantId(tenantId);
    }

    @TransactionalEventListener(classes = MobileAppSettingsEvictEvent.class)
    @Override
    public void handleEvictEvent(MobileAppSettingsEvictEvent event) {
        cache.evict(event.getTenantId());
    }

    private MobileAppSettings constructMobileAppSettings(MobileAppSettings mobileAppSettings) {
        if (mobileAppSettings == null) {
            mobileAppSettings = new MobileAppSettings();
            mobileAppSettings.setUseDefaultApp(true);

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
                    .badgeEnabled(true)
                    .build();

            mobileAppSettings.setQrCodeConfig(qrCodeConfig);
            mobileAppSettings.setAndroidConfig(androidConfig);
            mobileAppSettings.setIosConfig(iosConfig);
        }
        if (mobileAppSettings.isUseDefaultApp()) {
            mobileAppSettings.setDefaultGooglePlayLink(googlePlayLink);
            mobileAppSettings.setDefaultAppStoreLink(appStoreLink);
        }
        return mobileAppSettings;
    }

}
