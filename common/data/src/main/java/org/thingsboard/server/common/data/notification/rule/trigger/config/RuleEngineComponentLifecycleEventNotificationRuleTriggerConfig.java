// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.notification.rule.trigger.config;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema
public class RuleEngineComponentLifecycleEventNotificationRuleTriggerConfig implements NotificationRuleTriggerConfig {

    @ArraySchema(schema = @Schema(implementation = UUID.class))
    private Set<UUID> ruleChains; // if empty - all rule chains
    @ArraySchema(schema = @Schema(implementation = ComponentLifecycleEvent.class))
    private Set<ComponentLifecycleEvent> ruleChainEvents; // available options: STARTED, UPDATED, STOPPED. if empty - all events
    private boolean onlyRuleChainLifecycleFailures;

    private boolean trackRuleNodeEvents;
    @ArraySchema(schema = @Schema(implementation = ComponentLifecycleEvent.class))
    private Set<ComponentLifecycleEvent> ruleNodeEvents; // available options: STARTED, UPDATED, STOPPED. if empty - all events
    private boolean onlyRuleNodeLifecycleFailures;

    @Override
    public NotificationRuleTriggerType getTriggerType() {
        return NotificationRuleTriggerType.RULE_ENGINE_COMPONENT_LIFECYCLE_EVENT;
    }

}
