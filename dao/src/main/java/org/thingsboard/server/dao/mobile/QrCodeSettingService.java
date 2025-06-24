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
