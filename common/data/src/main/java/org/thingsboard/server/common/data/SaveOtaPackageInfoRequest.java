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
