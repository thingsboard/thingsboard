/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.common.data.mobile.qrCodeSettings;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.id.MobileAppBundleId;
import org.thingsboard.server.common.data.id.QrCodeSettingsId;
import org.thingsboard.server.common.data.id.TenantId;

@Schema
@Data
@EqualsAndHashCode(callSuper = true)
public class QrCodeSettings extends BaseData<QrCodeSettingsId> implements HasTenantId {

    private static final long serialVersionUID = 2628323657987010348L;

    @Schema(description = "JSON object with Tenant Id.", accessMode = Schema.AccessMode.READ_ONLY)
    private TenantId tenantId;
    @Schema(description = "Use settings from system level", example = "true")
    private boolean useSystemSettings;
    @Schema(description = "Type of application: true means use default Thingsboard app", example = "true")
    private boolean useDefaultApp;
    @Schema(description = "Mobile app bundle.")
    private MobileAppBundleId mobileAppBundleId;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "QR code config configuration.")
    @Valid
    @NotNull
    private QRCodeConfig qrCodeConfig;
    @Schema(description = "Indicates if google play link is available", example = "true")
    private boolean androidEnabled;
    @Schema(description = "Indicates if apple store link is available", example = "true")
    private boolean iosEnabled;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String googlePlayLink;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String appStoreLink;

    public QrCodeSettings() {
    }

    public QrCodeSettings(QrCodeSettingsId id) {
        super(id);
    }

}
