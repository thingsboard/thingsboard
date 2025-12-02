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
package org.thingsboard.server.common.data.pat;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.id.ApiKeyId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

import java.io.Serial;

@Schema
@Data
@EqualsAndHashCode(callSuper = true)
public class ApiKeyInfo extends BaseData<ApiKeyId> implements HasTenantId {

    @Serial
    private static final long serialVersionUID = -2313196723950490263L;

    @Schema(description = "JSON object with Tenant Id. Tenant Id of the API key cannot be changed.", accessMode = Schema.AccessMode.READ_ONLY)
    private TenantId tenantId;

    @Schema(description = "JSON object with User Id. User Id of the API key cannot be changed.")
    private UserId userId;

    @Schema(description = "Expiration time of the API key.")
    private long expirationTime;

    @NoXss
    @NotBlank
    @Length(fieldName = "description")
    @Schema(description = "API Key description.", example = "API Key description")
    private String description;

    @Schema(description = "Enabled/disabled API key.", example = "true")
    private boolean enabled;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Schema(description = "Indicates if the API key is expired based on current time. Returns false if expirationTime is 0 (no expiry).",
            example = "false",
            accessMode = Schema.AccessMode.READ_ONLY)
    public boolean isExpired() {
        if (expirationTime == 0) {
            return false;
        }
        return System.currentTimeMillis() > expirationTime;
    }

    @Schema(description = "JSON object with the API Key Id. " +
            "Specify this field to update the API Key. " +
            "Referencing non-existing API Key Id will cause error. " +
            "Omit this field to create new API Key.")
    @Override
    public ApiKeyId getId() {
        return super.getId();
    }

    public ApiKeyInfo() {
        super();
    }

    public ApiKeyInfo(ApiKeyId id) {
        super(id);
    }

    public ApiKeyInfo(ApiKeyInfo apiKeyInfo) {
        super(apiKeyInfo);
        this.tenantId = apiKeyInfo.getTenantId();
        this.userId = apiKeyInfo.getUserId();
        this.expirationTime = apiKeyInfo.getExpirationTime();
        this.enabled = apiKeyInfo.isEnabled();
        this.description = apiKeyInfo.getDescription();
    }

}
