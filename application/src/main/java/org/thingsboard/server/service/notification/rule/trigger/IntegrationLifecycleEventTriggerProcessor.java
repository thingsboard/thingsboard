// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.notification.rule.trigger;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.notification.info.IntegrationLifecycleEventNotificationInfo;
import org.thingsboard.server.common.data.notification.info.RuleOriginatedNotificationInfo;
import org.thingsboard.server.common.data.notification.rule.trigger.IntegrationLifecycleEventTrigger;
import org.thingsboard.server.common.data.notification.rule.trigger.config.IntegrationLifecycleEventNotificationRuleTriggerConfig;
import org.thingsboard.server.common.data.notification.rule.trigger.config.NotificationRuleTriggerType;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;

@Service
@RequiredArgsConstructor
public class IntegrationLifecycleEventTriggerProcessor implements NotificationRuleTriggerProcessor<IntegrationLifecycleEventTrigger, IntegrationLifecycleEventNotificationRuleTriggerConfig> {

    @Override
    public boolean matchesFilter(IntegrationLifecycleEventTrigger trigger, IntegrationLifecycleEventNotificationRuleTriggerConfig triggerConfig) {
        return (isEmpty(triggerConfig.getIntegrationTypes()) || triggerConfig.getIntegrationTypes().contains(trigger.getIntegrationType())) &&
                (isEmpty(triggerConfig.getIntegrations()) || triggerConfig.getIntegrations().contains(trigger.getIntegrationId().getId())) &&
                (isEmpty(triggerConfig.getNotifyOn()) || triggerConfig.getNotifyOn().contains(trigger.getEvent())) &&
                (!triggerConfig.isOnlyOnError() || trigger.getError() != null);
    }

    @Override
    public RuleOriginatedNotificationInfo constructNotificationInfo(IntegrationLifecycleEventTrigger trigger) {
        return IntegrationLifecycleEventNotificationInfo.builder()
                .integrationId(trigger.getIntegrationId())
                .integrationType(trigger.getIntegrationType().name())
                .integrationName(trigger.getIntegrationName())
                .action(trigger.getEvent() == ComponentLifecycleEvent.STARTED ? "start"
                        : trigger.getEvent() == ComponentLifecycleEvent.UPDATED ? "update"
                        : trigger.getEvent() == ComponentLifecycleEvent.STOPPED ? "stop" : null)
                .eventType(trigger.getEvent())
                .error(trigger.getError() != null ? StringUtils.abbreviate(trigger.getError().getMessage(), 50) : null)
                .build();
    }

    @Override
    public NotificationRuleTriggerType getTriggerType() {
        return NotificationRuleTriggerType.INTEGRATION_LIFECYCLE_EVENT;
    }

}
