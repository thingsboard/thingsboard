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
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.id.MobileAppBundleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.mobile.QRCodeConfig;
import org.thingsboard.server.common.data.mobile.QrCodeSettings;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.service.DataValidator;

@Component
@AllArgsConstructor
public class QrCodeSettingsDataValidator extends DataValidator<QrCodeSettings> {

    @Override
    protected void validateDataImpl(TenantId tenantId, QrCodeSettings qrCodeSettings) {
        MobileAppBundleId mobileAppBundleId = qrCodeSettings.getMobileAppBundleId();
        QRCodeConfig qrCodeConfig = qrCodeSettings.getQrCodeConfig();
        if (!qrCodeSettings.isUseDefaultApp() && (mobileAppBundleId == null)) {
            throw new DataValidationException("Mobile app bundle is required to use custom application!");
        }
        if (qrCodeConfig == null) {
            throw new DataValidationException("Qr code configuration is required!");
        }
    }
}
