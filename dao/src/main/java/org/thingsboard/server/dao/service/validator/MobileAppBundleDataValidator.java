// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.service.validator;

import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.id.MobileAppId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.mobile.app.MobileApp;
import org.thingsboard.server.common.data.mobile.bundle.MobileAppBundle;
import org.thingsboard.server.common.data.oauth2.PlatformType;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.mobile.MobileAppDao;
import org.thingsboard.server.dao.service.DataValidator;

@Component
@AllArgsConstructor
public class MobileAppBundleDataValidator extends DataValidator<MobileAppBundle> {

    @Autowired
    private MobileAppDao mobileAppDao;

    @Override
    protected void validateDataImpl(TenantId tenantId, MobileAppBundle mobileAppBundle) {
        MobileAppId androidAppId = mobileAppBundle.getAndroidAppId();
        if (androidAppId != null) {
            MobileApp androidApp = mobileAppDao.findById(tenantId, androidAppId.getId());
            if (androidApp == null) {
                throw new DataValidationException("Mobile app bundle refers to non-existing android app!");
            }
            if (androidApp.getPlatformType() != PlatformType.ANDROID) {
                throw new DataValidationException("Mobile app bundle refers to wrong android app! Platform type of specified app is " + androidApp.getPlatformType());
            }
        }
        MobileAppId iosAppId = mobileAppBundle.getIosAppId();
        if (iosAppId != null) {
            MobileApp iosApp = mobileAppDao.findById(tenantId, iosAppId.getId());
            if (iosApp == null) {
                throw new DataValidationException("Mobile app bundle refers to non-existing ios app!");
            }
            if (iosApp.getPlatformType() != PlatformType.IOS) {
                throw new DataValidationException("Mobile app bundle refers to wrong ios app! Platform type of specified app is " + iosApp.getPlatformType());
            }
        }
    }
}
