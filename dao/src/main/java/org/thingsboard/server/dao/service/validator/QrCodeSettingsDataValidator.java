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
package org.thingsboard.server.dao.service.validator;

import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.id.MobileAppBundleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.mobile.app.MobileApp;
import org.thingsboard.server.common.data.mobile.qrCodeSettings.QRCodeConfig;
import org.thingsboard.server.common.data.mobile.qrCodeSettings.QrCodeSettings;
import org.thingsboard.server.common.data.mobile.app.StoreInfo;
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
        QRCodeConfig qrCodeConfig = qrCodeSettings.getQrCodeConfig();
        if (!qrCodeSettings.isUseDefaultApp() && (mobileAppBundleId == null)) {
            throw new DataValidationException("Mobile app bundle is required to use custom application!");
        }
        if (!qrCodeSettings.isUseDefaultApp()){
            MobileApp androidApp = mobileAppDao.findByBundleIdAndPlatformType(tenantId, mobileAppBundleId, PlatformType.ANDROID);
            StoreInfo androidStoreInfo = androidApp.getStoreInfo();
            if (androidStoreInfo == null) {
                throw new DataValidationException("Android app store info is empty! ");
            }

            MobileApp iosApp = mobileAppDao.findByBundleIdAndPlatformType(tenantId, mobileAppBundleId, PlatformType.IOS);
            StoreInfo iosStoreInfo = iosApp.getStoreInfo();
            if (iosStoreInfo == null) {
                throw new DataValidationException("IOS app store info is empty! ");
            }
        }
        if (qrCodeConfig == null) {
            throw new DataValidationException("Qr code configuration is required!");
        }
    }
}
