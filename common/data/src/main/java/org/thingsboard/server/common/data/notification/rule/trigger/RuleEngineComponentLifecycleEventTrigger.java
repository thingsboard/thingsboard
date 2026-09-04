// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.notification.rule.trigger;

import lombok.Builder;
import lombok.Data;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.rule.trigger.config.NotificationRuleTriggerType;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;

@Data
@Builder
public class RuleEngineComponentLifecycleEventTrigger implements NotificationRuleTrigger {

    private final TenantId tenantId;
    private final RuleChainId ruleChainId;
    private final String ruleChainName;
    private final EntityId componentId;
    private final String componentName;
    private final ComponentLifecycleEvent eventType;
    private final Throwable error;

    @Override
    public NotificationRuleTriggerType getType() {
        return NotificationRuleTriggerType.RULE_ENGINE_COMPONENT_LIFECYCLE_EVENT;
    }

    @Override
    public EntityId getOriginatorEntityId() {
        return componentId;
    }

}
