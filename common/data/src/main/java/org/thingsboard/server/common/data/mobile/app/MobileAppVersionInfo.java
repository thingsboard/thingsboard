// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
