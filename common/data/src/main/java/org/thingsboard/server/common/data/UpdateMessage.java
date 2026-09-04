// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Schema
@Data
public class UpdateMessage implements Serializable {

    @Schema(description = "'True' if new platform update is available.")
    private final boolean updateAvailable;
    @Schema(description = "Current ThingsBoard version.")
    private final String currentVersion;
    @Schema(description = "Latest ThingsBoard version.")
    private final String latestVersion;
    @Schema(description = "Upgrade instructions URL.")
    private final String upgradeInstructionsUrl;
    @Schema(description = "Current ThingsBoard version release notes URL.")
    private final String currentVersionReleaseNotesUrl;
    @Schema(description = "Latest ThingsBoard version release notes URL.")
    private final String latestVersionReleaseNotesUrl;

}
