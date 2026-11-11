// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.notification.rule.trigger;

import lombok.Builder;
import lombok.Data;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.common.data.notification.rule.trigger.config.NotificationRuleTriggerType;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;

@Data
@Builder
public class IntegrationLifecycleEventTrigger implements NotificationRuleTrigger {

    private final TenantId tenantId;
    private final IntegrationId integrationId;
    private final IntegrationType integrationType;
    private final String integrationName;
    private final ComponentLifecycleEvent event;
    private final Throwable error;

    @Override
    public NotificationRuleTriggerType getType() {
        return NotificationRuleTriggerType.INTEGRATION_LIFECYCLE_EVENT;
    }

    @Override
    public EntityId getOriginatorEntityId() {
        return integrationId;
    }

}
