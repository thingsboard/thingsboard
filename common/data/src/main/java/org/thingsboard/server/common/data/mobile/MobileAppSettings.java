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
package org.thingsboard.server.common.data.mobile;

import jakarta.validation.Valid;
import lombok.Data;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.id.MobileAppSettingsId;
import org.thingsboard.server.common.data.id.TenantId;

@Data
public class MobileAppSettings extends BaseData<MobileAppSettingsId> implements HasTenantId {

    private static final long serialVersionUID = 2628323657987010348L;

    private TenantId tenantId;
    private boolean useDefaultApp;
    @Valid
    private AndroidConfig androidConfig;
    @Valid
    private IosConfig iosConfig;
    @Valid
    private QRCodeConfig qrCodeConfig;

    public MobileAppSettings() {
    }
    public MobileAppSettings(MobileAppSettingsId id) {
        super(id);
    }

}
