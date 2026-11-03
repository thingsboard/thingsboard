// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.notification.info;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

import static org.thingsboard.server.common.data.util.CollectionsUtil.mapOf;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewPlatformVersionNotificationInfo implements RuleOriginatedNotificationInfo {

    private String latestVersion;
    private String latestVersionReleaseNotesUrl;
    private String upgradeInstructionsUrl;

    private String currentVersion;
    private String currentVersionReleaseNotesUrl;

    @Override
    public Map<String, String> getTemplateData() {
        return mapOf(
                "latestVersion", latestVersion,
                "latestVersionReleaseNotesUrl", latestVersionReleaseNotesUrl,
                "upgradeInstructionsUrl", upgradeInstructionsUrl,
                "currentVersion", currentVersion,
                "currentVersionReleaseNotesUrl", currentVersionReleaseNotesUrl
        );
    }

}
