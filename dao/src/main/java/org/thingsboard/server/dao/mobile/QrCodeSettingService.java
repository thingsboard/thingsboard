// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.mobile;

import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.mobile.app.MobileApp;
import org.thingsboard.server.common.data.mobile.qrCodeSettings.QrCodeSettings;
import org.thingsboard.server.common.data.oauth2.PlatformType;

public interface QrCodeSettingService {

    QrCodeSettings saveQrCodeSettings(TenantId tenantId, QrCodeSettings qrCodeSettings);

    QrCodeSettings findQrCodeSettings(TenantId tenantId);

    MobileApp findAppFromQrCodeSettings(TenantId sysTenantId, PlatformType platformType);

    void deleteByTenantId(TenantId tenantId);

}
