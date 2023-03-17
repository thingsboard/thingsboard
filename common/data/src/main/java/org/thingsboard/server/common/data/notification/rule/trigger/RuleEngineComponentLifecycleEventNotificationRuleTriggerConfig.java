/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
package org.thingsboard.server.common.data.notification.rule.trigger;

import lombok.Data;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;

import java.util.Set;
import java.util.UUID;

@Data
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
