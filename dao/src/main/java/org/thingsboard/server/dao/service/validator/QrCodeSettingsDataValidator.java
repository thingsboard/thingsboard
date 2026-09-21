// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.service.validator;

import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.id.MobileAppBundleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.mobile.app.MobileApp;
import org.thingsboard.server.common.data.mobile.app.MobileAppStatus;
import org.thingsboard.server.common.data.mobile.qrCodeSettings.QrCodeSettings;
import org.thingsboard.server.common.data.oauth2.PlatformType;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.mobile.MobileAppDao;
import org.thingsboard.server.dao.service.DataValidator;

@Component
@AllArgsConstructor
public class QrCodeSettingsDataValidator extends DataValidator<QrCodeSettings> {

    @Autowired
    MobileAppDao mobileAppDao;

    @Override
    protected void validateDataImpl(TenantId tenantId, QrCodeSettings qrCodeSettings) {
        MobileAppBundleId mobileAppBundleId = qrCodeSettings.getMobileAppBundleId();
        if (!qrCodeSettings.isUseDefaultApp() && (mobileAppBundleId == null)) {
            throw new DataValidationException("Mobile app bundle is required to use custom application!");
        }
        if (!qrCodeSettings.isUseDefaultApp()) {
            if (qrCodeSettings.isAndroidEnabled()) {
                MobileApp androidApp = mobileAppDao.findByBundleIdAndPlatformType(tenantId, mobileAppBundleId, PlatformType.ANDROID);
                if (androidApp != null && androidApp.getStatus() != MobileAppStatus.PUBLISHED) {
                    throw new DataValidationException("The mobile app bundle references an Android app that has not been published!");
                }
            }
            if (qrCodeSettings.isIosEnabled()) {
                MobileApp iosApp = mobileAppDao.findByBundleIdAndPlatformType(tenantId, mobileAppBundleId, PlatformType.IOS);
                if (iosApp != null && iosApp.getStatus() != MobileAppStatus.PUBLISHED) {
                    throw new DataValidationException("The mobile app bundle references an iOS app that has not been published!");
                }
            }
        }
    }
}
