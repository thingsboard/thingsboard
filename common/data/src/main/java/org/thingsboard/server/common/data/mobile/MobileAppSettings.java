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

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.id.MobileAppSettingsId;
import org.thingsboard.server.common.data.id.TenantId;

@Schema
@Data
@EqualsAndHashCode(callSuper = true)
public class MobileAppSettings extends BaseData<MobileAppSettingsId> implements HasTenantId {

    private static final long serialVersionUID = 2628323657987010348L;

    @Schema(description = "JSON object with Tenant Id.", accessMode = Schema.AccessMode.READ_ONLY)
    private TenantId tenantId;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Type of application: true means use default Thingsboard app", example = "true")
    private boolean useDefaultApp;
    @Valid
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Android mobile app configuration.")
    private AndroidConfig androidConfig;
    @Valid
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Ios mobile app configuration.")
    private IosConfig iosConfig;
    @Valid
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "QR code config configuration.")
    private QRCodeConfig qrCodeConfig;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String defaultGooglePlayLink;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String defaultAppStoreLink;

    public MobileAppSettings() {
    }

    public MobileAppSettings(MobileAppSettingsId id) {
        super(id);
    }

}
