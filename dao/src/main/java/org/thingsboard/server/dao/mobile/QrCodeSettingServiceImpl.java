/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
import org.thingsboard.server.common.data.mobile.qrCodeSettings.BadgePosition;
import org.thingsboard.server.common.data.mobile.app.MobileApp;
import org.thingsboard.server.common.data.mobile.qrCodeSettings.QrCodeSettings;
import org.thingsboard.server.common.data.mobile.qrCodeSettings.QRCodeConfig;
import org.thingsboard.server.common.data.oauth2.PlatformType;
import org.thingsboard.server.dao.entity.AbstractCachedEntityService;
import org.thingsboard.server.dao.service.DataValidator;

import java.util.Map;

import static org.thingsboard.server.common.data.oauth2.PlatformType.ANDROID;
import static org.thingsboard.server.common.data.oauth2.PlatformType.IOS;
import static org.thingsboard.server.dao.service.Validator.validateId;

@Service
@Slf4j
@RequiredArgsConstructor
public class QrCodeSettingServiceImpl extends AbstractCachedEntityService<TenantId, QrCodeSettings, QrCodeSettingsEvictEvent> implements QrCodeSettingService {

    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    private static final String DEFAULT_QR_CODE_LABEL = "Scan to connect or download mobile app";

    @Value("${mobileApp.googlePlayLink:https://play.google.com/store/apps/details?id=org.thingsboard.demo.app}")
    private String googlePlayLink;
    @Value("${mobileApp.appStoreLink:https://apps.apple.com/us/app/thingsboard-live/id1594355695}")
    private String appStoreLink;

    private final QrCodeSettingsDao qrCodeSettingsDao;
    private final MobileAppService mobileAppService;
    private final DataValidator<QrCodeSettings> mobileAppSettingsDataValidator;

    @Override
    public QrCodeSettings saveQrCodeSettings(TenantId tenantId, QrCodeSettings qrCodeSettings) {
        mobileAppSettingsDataValidator.validate(qrCodeSettings, s -> tenantId);
        try {
            QrCodeSettings savedQrCodeSettings = qrCodeSettingsDao.save(tenantId, qrCodeSettings);
            publishEvictEvent(new QrCodeSettingsEvictEvent(tenantId));
            return constructMobileAppSettings(savedQrCodeSettings);
        } catch (Exception e) {
            handleEvictEvent(new QrCodeSettingsEvictEvent(tenantId));
            checkConstraintViolation(e, Map.of(
                    "qr_code_settings_tenant_id_unq_key", "Mobile application for specified tenant already exists!"
            ));
            throw e;
        }
    }

    @Override
    public QrCodeSettings findQrCodeSettings(TenantId tenantId) {
        log.trace("Executing getMobileAppSettings for tenant [{}] ", tenantId);
        QrCodeSettings qrCodeSettings = cache.getAndPutInTransaction(tenantId,
                () -> qrCodeSettingsDao.findByTenantId(tenantId), true);
        return constructMobileAppSettings(qrCodeSettings);
    }

    @Override
    public MobileApp findAppFromQrCodeSettings(TenantId tenantId, PlatformType platformType) {
        log.trace("Executing findAppQrCodeConfig for tenant [{}] ", tenantId);
        QrCodeSettings qrCodeSettings = findQrCodeSettings(tenantId);
        return qrCodeSettings.getMobileAppBundleId() != null ? mobileAppService.findByBundleIdAndPlatformType(tenantId, qrCodeSettings.getMobileAppBundleId(), platformType) : null;
    }

    @Override
    public void deleteByTenantId(TenantId tenantId) {
        log.trace("Executing deleteByTenantId, tenantId [{}]", tenantId);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        qrCodeSettingsDao.removeByTenantId(tenantId);
    }

    @TransactionalEventListener(classes = QrCodeSettingsEvictEvent.class)
    @Override
    public void handleEvictEvent(QrCodeSettingsEvictEvent event) {
        cache.evict(event.getTenantId());
    }

    private QrCodeSettings constructMobileAppSettings(QrCodeSettings qrCodeSettings) {
        if (qrCodeSettings == null) {
            qrCodeSettings = new QrCodeSettings();
            qrCodeSettings.setUseDefaultApp(true);
            qrCodeSettings.setAndroidEnabled(true);
            qrCodeSettings.setIosEnabled(true);

            QRCodeConfig qrCodeConfig = QRCodeConfig.builder()
                    .showOnHomePage(true)
                    .qrCodeLabelEnabled(true)
                    .qrCodeLabel(DEFAULT_QR_CODE_LABEL)
                    .badgeEnabled(true)
                    .badgePosition(BadgePosition.RIGHT)
                    .badgeEnabled(true)
                    .build();

            qrCodeSettings.setQrCodeConfig(qrCodeConfig);
            qrCodeSettings.setMobileAppBundleId(qrCodeSettings.getMobileAppBundleId());
        }
        if (qrCodeSettings.isUseDefaultApp() || qrCodeSettings.getMobileAppBundleId() == null) {
            qrCodeSettings.setGooglePlayLink(googlePlayLink);
            qrCodeSettings.setAppStoreLink(appStoreLink);
        } else {
            MobileApp androidApp = mobileAppService.findByBundleIdAndPlatformType(qrCodeSettings.getTenantId(), qrCodeSettings.getMobileAppBundleId(), ANDROID);
            MobileApp iosApp = mobileAppService.findByBundleIdAndPlatformType(qrCodeSettings.getTenantId(), qrCodeSettings.getMobileAppBundleId(), IOS);
            if (androidApp != null && androidApp.getStoreInfo() != null) {
                qrCodeSettings.setGooglePlayLink(androidApp.getStoreInfo().getStoreLink());
            }
            if (iosApp != null && iosApp.getStoreInfo() != null) {
                qrCodeSettings.setAppStoreLink(iosApp.getStoreInfo().getStoreLink());
            }
        }
        return qrCodeSettings;
    }

}
