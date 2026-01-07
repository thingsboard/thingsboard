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
package org.thingsboard.server.common.data.mobile.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.validation.Length;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class MobileAppVersionInfo {

    @Schema(description = "Minimum supported version")
    @Length(fieldName = "minVersion", max = 20)
    private String minVersion;

    @Schema(description = "Release notes of minimum supported version")
    @Length(fieldName = "minVersionReleaseNotes", max = 40000)
    private String minVersionReleaseNotes;

    @Schema(description = "Latest supported version")
    @Length(fieldName = "latestVersion", max = 20)
    private String latestVersion;

    @Schema(description = "Release notes of latest supported version")
    @Length(fieldName = "latestVersionReleaseNotes", max = 40000)
    private String latestVersionReleaseNotes;

}
