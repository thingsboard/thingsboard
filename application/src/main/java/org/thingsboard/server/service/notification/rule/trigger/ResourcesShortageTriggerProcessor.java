// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
