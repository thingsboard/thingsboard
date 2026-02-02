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
import org.thingsboard.server.common.data.notification.info.ResourcesShortageNotificationInfo;
import org.thingsboard.server.common.data.notification.info.RuleOriginatedNotificationInfo;
import org.thingsboard.server.common.data.notification.rule.trigger.ResourcesShortageTrigger;
import org.thingsboard.server.common.data.notification.rule.trigger.config.NotificationRuleTriggerType;
import org.thingsboard.server.common.data.notification.rule.trigger.config.ResourcesShortageNotificationRuleTriggerConfig;

@Service
@RequiredArgsConstructor
public class ResourcesShortageTriggerProcessor implements NotificationRuleTriggerProcessor<ResourcesShortageTrigger, ResourcesShortageNotificationRuleTriggerConfig> {

    @Override
    public boolean matchesFilter(ResourcesShortageTrigger trigger, ResourcesShortageNotificationRuleTriggerConfig triggerConfig) {
        float usagePercent = trigger.getUsage() / 100.0f;
        return switch (trigger.getResource()) {
            case CPU -> usagePercent >= triggerConfig.getCpuThreshold();
            case RAM -> usagePercent >= triggerConfig.getRamThreshold();
            case STORAGE -> usagePercent >= triggerConfig.getStorageThreshold();
        };
    }

    @Override
    public RuleOriginatedNotificationInfo constructNotificationInfo(ResourcesShortageTrigger trigger) {
        return ResourcesShortageNotificationInfo.builder()
                .resource(trigger.getResource().name())
                .usage(trigger.getUsage())
                .serviceId(trigger.getServiceId())
                .serviceType(trigger.getServiceType())
                .build();
    }

    @Override
    public NotificationRuleTriggerType getTriggerType() {
        return NotificationRuleTriggerType.RESOURCES_SHORTAGE;
    }

}
