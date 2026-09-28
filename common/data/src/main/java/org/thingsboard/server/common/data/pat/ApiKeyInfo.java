// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.common.data.pat;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.TenantEntity;
import org.thingsboard.server.common.data.id.ApiKeyId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.permission.AuthorityPermissionsInfo;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

import java.io.Serial;

@Schema
@Data
@EqualsAndHashCode(callSuper = true)
public class ApiKeyInfo extends BaseData<ApiKeyId> implements TenantEntity {

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

    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    @Schema(description = "Internal API key flag. Internal keys allow user impersonation via headers and cannot be updated/deleted, only rotated. " +
            "This field is read-only and can only be set when creating internal API keys via special service method.",
            example = "false",
            accessMode = Schema.AccessMode.READ_ONLY,
            hidden = true)
    private boolean internal;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Authority-specific permissions for this API key. " +
            "For internal API keys: specify permissions for all authorities (SYS_ADMIN, TENANT_ADMIN, CUSTOMER_USER). " +
            "This field is read-only and can only be set when creating internal API keys via special service method.",
            accessMode = Schema.AccessMode.READ_ONLY,
            hidden = true)
    private AuthorityPermissionsInfo permissions;

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
        this.internal = apiKeyInfo.isInternal();
        this.permissions = apiKeyInfo.getPermissions();
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.API_KEY;
    }

}
