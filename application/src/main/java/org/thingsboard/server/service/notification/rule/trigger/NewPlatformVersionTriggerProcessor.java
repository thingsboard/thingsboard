// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.notification.rule.trigger;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.UpdateMessage;
import org.thingsboard.server.common.data.notification.info.NewPlatformVersionNotificationInfo;
import org.thingsboard.server.common.data.notification.info.RuleOriginatedNotificationInfo;
import org.thingsboard.server.common.data.notification.rule.trigger.NewPlatformVersionTrigger;
import org.thingsboard.server.common.data.notification.rule.trigger.config.NewPlatformVersionNotificationRuleTriggerConfig;
import org.thingsboard.server.common.data.notification.rule.trigger.config.NotificationRuleTriggerType;

@Service
@RequiredArgsConstructor
public class NewPlatformVersionTriggerProcessor implements NotificationRuleTriggerProcessor<NewPlatformVersionTrigger, NewPlatformVersionNotificationRuleTriggerConfig> {

    @Override
    public boolean matchesFilter(NewPlatformVersionTrigger trigger, NewPlatformVersionNotificationRuleTriggerConfig triggerConfig) {
        return trigger.getUpdateInfo().isUpdateAvailable();
    }

    @Override
    public RuleOriginatedNotificationInfo constructNotificationInfo(NewPlatformVersionTrigger trigger) {
        UpdateMessage updateInfo = trigger.getUpdateInfo();
        return NewPlatformVersionNotificationInfo.builder()
                .latestVersion(updateInfo.getLatestVersion())
                .latestVersionReleaseNotesUrl(updateInfo.getLatestVersionReleaseNotesUrl())
                .upgradeInstructionsUrl(updateInfo.getUpgradeInstructionsUrl())
                .currentVersion(updateInfo.getCurrentVersion())
                .currentVersionReleaseNotesUrl(updateInfo.getCurrentVersionReleaseNotesUrl())
                .build();
    }

    @Override
    public NotificationRuleTriggerType getTriggerType() {
        return NotificationRuleTriggerType.NEW_PLATFORM_VERSION;
    }

}
