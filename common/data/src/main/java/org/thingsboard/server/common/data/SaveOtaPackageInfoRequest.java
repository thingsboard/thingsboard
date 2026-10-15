// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Schema
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class SaveOtaPackageInfoRequest extends OtaPackageInfo {
    @Schema(description = "Indicates OTA Package uses url. Should be 'true' if uses url or 'false' if will be used data.", example = "true", accessMode = Schema.AccessMode.READ_ONLY)
    boolean usesUrl;

    public SaveOtaPackageInfoRequest(OtaPackageInfo otaPackageInfo, boolean usesUrl) {
        super(otaPackageInfo);
        this.usesUrl = usesUrl;
    }

    public SaveOtaPackageInfoRequest(SaveOtaPackageInfoRequest saveOtaPackageInfoRequest) {
        super(saveOtaPackageInfoRequest);
        this.usesUrl = saveOtaPackageInfoRequest.isUsesUrl();
    }
}
