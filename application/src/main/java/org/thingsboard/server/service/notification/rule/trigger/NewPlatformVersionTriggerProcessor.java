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
