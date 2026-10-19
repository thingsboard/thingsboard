// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.notification.rule.trigger.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;

import java.util.Set;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class IntegrationLifecycleEventNotificationRuleTriggerConfig implements NotificationRuleTriggerConfig {

    private Set<IntegrationType> integrationTypes;
    private Set<UUID> integrations; // set either integrationTypes or integrations or none
    private Set<ComponentLifecycleEvent> notifyOn; // STARTED, UPDATED or STOPPED
    private boolean onlyOnError;

    @Override
    public NotificationRuleTriggerType getTriggerType() {
        return NotificationRuleTriggerType.INTEGRATION_LIFECYCLE_EVENT;
    }

}
