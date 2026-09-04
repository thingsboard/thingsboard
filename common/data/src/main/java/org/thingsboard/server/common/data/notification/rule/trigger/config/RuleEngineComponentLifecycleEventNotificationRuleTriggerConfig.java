// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.notification.rule.trigger.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;

import java.util.Set;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RuleEngineComponentLifecycleEventNotificationRuleTriggerConfig implements NotificationRuleTriggerConfig {

    private Set<UUID> ruleChains; // if empty - all rule chains

    private Set<ComponentLifecycleEvent> ruleChainEvents; // available options: STARTED, UPDATED, STOPPED. if empty - all events
    private boolean onlyRuleChainLifecycleFailures;

    private boolean trackRuleNodeEvents;
    private Set<ComponentLifecycleEvent> ruleNodeEvents; // available options: STARTED, UPDATED, STOPPED. if empty - all events
    private boolean onlyRuleNodeLifecycleFailures;

    @Override
    public NotificationRuleTriggerType getTriggerType() {
        return NotificationRuleTriggerType.RULE_ENGINE_COMPONENT_LIFECYCLE_EVENT;
    }

}
