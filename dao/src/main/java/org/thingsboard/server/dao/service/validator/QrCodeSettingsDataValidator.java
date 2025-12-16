/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
import org.thingsboard.server.exception.DataValidationException;
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
