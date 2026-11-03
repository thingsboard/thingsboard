// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.notification.rule.trigger.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.EntityType;

import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EntityActionNotificationRuleTriggerConfig implements NotificationRuleTriggerConfig {

    private Set<EntityType> entityTypes; // maybe add name filter ?
    private boolean created;
    private boolean updated;
    private boolean deleted;

    @Override
    public NotificationRuleTriggerType getTriggerType() {
        return NotificationRuleTriggerType.ENTITY_ACTION;
    }

}
