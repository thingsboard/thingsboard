// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.service.validator;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.mobile.app.MobileApp;
import org.thingsboard.server.common.data.mobile.app.MobileAppStatus;
import org.thingsboard.server.common.data.oauth2.PlatformType;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.service.DataValidator;

@Component
@AllArgsConstructor
public class MobileAppDataValidator extends DataValidator<MobileApp> {

    @Override
    protected void validateDataImpl(TenantId tenantId, MobileApp mobileApp) {
        if (mobileApp.getStatus() == MobileAppStatus.PUBLISHED) {
            if (mobileApp.getStoreInfo() == null) {
                throw new DataValidationException("Store info is required for published apps");
            }
            if (mobileApp.getPlatformType() == PlatformType.ANDROID &&
                    (mobileApp.getStoreInfo().getSha256CertFingerprints() == null || mobileApp.getStoreInfo().getStoreLink() == null)) {
                throw new DataValidationException("Sha256CertFingerprints and store link are required");
            } else if (mobileApp.getPlatformType() == PlatformType.IOS &&
                    (mobileApp.getStoreInfo().getAppId() == null || mobileApp.getStoreInfo().getStoreLink() == null)) {
                throw new DataValidationException("AppId and store link are required");
            }
        }
    }
}
