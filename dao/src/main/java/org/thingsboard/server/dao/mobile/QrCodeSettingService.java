// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.dao.mobile;

import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.mobile.app.MobileApp;
import org.thingsboard.server.common.data.mobile.qrCodeSettings.QrCodeSettings;
import org.thingsboard.server.common.data.oauth2.PlatformType;

public interface QrCodeSettingService {

    QrCodeSettings saveQrCodeSettings(TenantId tenantId, QrCodeSettings qrCodeSettings);

    QrCodeSettings findQrCodeSettings(TenantId tenantId);

    MobileApp findAppFromQrCodeSettings(TenantId sysTenantId, PlatformType platformType);

    QrCodeSettings getMergedQrCodeSettings(TenantId tenantId);

    void deleteByTenantId(TenantId tenantId);

}
