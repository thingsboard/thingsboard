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

import io.swagger.v3.oas.annotations.media.Schema;
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

    @Schema(description = "JSON object with Tenant Id. Tenant Id of the api key cannot be changed.", accessMode = Schema.AccessMode.READ_ONLY)
    private TenantId tenantId;

    @Schema(description = "JSON object with User Id. User Id of the api key cannot be changed.")
    private UserId userId;

    @Schema(description = "Expiration time of the api key.")
    private long expirationTime;

    @NoXss
    @Length(fieldName = "description")
    @Schema(description = "Api Key description.", example = "Api Key description")
    private String description;

    @Schema(description = "Enabled/disabled api key.", example = "true")
    private boolean enabled;

    @Schema(description = "JSON object with the Api Key Id. " +
            "Specify this field to update the Api Key. " +
            "Referencing non-existing Api Key Id will cause error. " +
            "Omit this field to create new Api Key.")
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
