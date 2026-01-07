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
package org.thingsboard.server.common.data.mobile.bundle;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.id.MobileAppBundleId;
import org.thingsboard.server.common.data.id.MobileAppId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.mobile.layout.MobileLayoutConfig;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

@EqualsAndHashCode(callSuper = true)
@Data
@ToString
public class MobileAppBundle extends BaseData<MobileAppBundleId> implements HasTenantId, HasName {

    @Schema(description = "JSON object with Tenant Id")
    private TenantId tenantId;
    @Schema(description = "Application bundle title. Cannot be empty", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @NoXss
    @Length(fieldName = "title")
    private String title;
    @Schema(description = "Application bundle description.")
    @NoXss
    @Length(fieldName = "description")
    private String description;
    @Schema(description = "Android application id")
    private MobileAppId androidAppId;
    @Schema(description = "IOS application id")
    private MobileAppId iosAppId;
    @Schema(description = "Application layout configuration")
    @Valid
    private MobileLayoutConfig layoutConfig;
    @Schema(description = "Whether OAuth2 settings are enabled or not")
    private Boolean oauth2Enabled;

    public MobileAppBundle() {
        super();
    }

    public MobileAppBundle(MobileAppBundleId id) {
        super(id);
    }

    public MobileAppBundle(MobileAppBundle mobile) {
        super(mobile);
        this.tenantId = mobile.tenantId;
        this.title = mobile.title;
        this.description = mobile.description;
        this.androidAppId = mobile.androidAppId;
        this.iosAppId = mobile.iosAppId;
        this.layoutConfig = mobile.layoutConfig;
        this.oauth2Enabled = mobile.oauth2Enabled;
    }

    @Override
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Schema(description = "Mobile app bundle title", example = "My main application", accessMode = Schema.AccessMode.READ_ONLY)
    public String getName() {
        return title;
    }
}
