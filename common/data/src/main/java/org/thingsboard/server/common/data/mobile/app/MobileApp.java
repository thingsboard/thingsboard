// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.mobile.app;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.id.MobileAppId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.oauth2.PlatformType;
import org.thingsboard.server.common.data.validation.Length;

@EqualsAndHashCode(callSuper = true)
@Data
@ToString
public class MobileApp extends BaseData<MobileAppId> implements HasTenantId, HasName {

    @Schema(description = "JSON object with Tenant Id")
    private TenantId tenantId;
    @Schema(description = "Application package name. Cannot be empty", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Length(fieldName = "pkgName")
    private String pkgName;
    @Schema(description = "Application title")
    @Length(fieldName = "title")
    private String title;
    @Schema(description = "Application secret. The length must be at least 16 characters", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty
    @Length(fieldName = "appSecret", min = 16, max = 2048, message = "must be at least 16 and max 2048 characters")
    private String appSecret;
    @Schema(description = "Application platform type: ANDROID or IOS", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private PlatformType platformType;
    @Schema(description = "Application status: PUBLISHED, DEPRECATED, SUSPENDED, DRAFT", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private MobileAppStatus status;
    @Schema(description = "Application version info")
    @Valid
    private MobileAppVersionInfo versionInfo;
    @Schema(description = "Application store information")
    @Valid
    private StoreInfo storeInfo;

    public MobileApp() {
        super();
    }

    public MobileApp(MobileAppId id) {
        super(id);
    }

    public MobileApp(MobileApp mobile) {
        super(mobile);
        this.tenantId = mobile.tenantId;
        this.pkgName = mobile.pkgName;
        this.title = mobile.title;
        this.appSecret = mobile.appSecret;
        this.platformType = mobile.platformType;
        this.status = mobile.status;
        this.versionInfo = mobile.versionInfo;
        this.storeInfo = mobile.storeInfo;
    }

    @Override
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Schema(description = "Mobile app package name", example = "my.mobile.app", accessMode = Schema.AccessMode.READ_ONLY)
    public String getName() {
        return pkgName;
    }
}
