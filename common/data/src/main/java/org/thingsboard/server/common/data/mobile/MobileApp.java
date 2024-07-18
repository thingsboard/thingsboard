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
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.id.MobileAppId;
import org.thingsboard.server.common.data.id.TenantId;

@EqualsAndHashCode(callSuper = true)
@Data
@ToString
@NoArgsConstructor
public class MobileApp extends BaseData<MobileAppId> implements HasTenantId, HasName {

    @Schema(description = "JSON object with Tenant Id")
    private TenantId tenantId;
    @Schema(description = "Application package name. Cannot be empty", requiredMode = Schema.RequiredMode.REQUIRED)
    private String pkgName;
    @Schema(description = "Application secret. The length must be at least 16 characters", requiredMode = Schema.RequiredMode.REQUIRED)
    private String appSecret;
    @Schema(description = "Whether OAuth2 settings are enabled or not")
    private boolean oauth2Enabled;

    public MobileApp(MobileApp mobile) {
        super(mobile);
        this.tenantId = mobile.tenantId;
        this.pkgName = mobile.pkgName;
        this.appSecret = mobile.appSecret;
        this.oauth2Enabled = mobile.oauth2Enabled;
    }

    @Override
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Schema(description = "Mobile app package name", example = "my.mobile.app", accessMode = Schema.AccessMode.READ_ONLY)
    public String getName() {
        return pkgName;
    }
}
