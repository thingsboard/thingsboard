// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.notification.rule.trigger;

import lombok.Builder;
import lombok.Data;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.rule.trigger.config.NotificationRuleTriggerType;

@Data
@Builder
public class EntityActionTrigger implements NotificationRuleTrigger {

    private final TenantId tenantId;
    private final EntityId entityId;
    private final HasName entity;
    private final ActionType actionType;
    private final User user;

    @Override
    public NotificationRuleTriggerType getType() {
        return NotificationRuleTriggerType.ENTITY_ACTION;
    }

    @Override
    public EntityId getOriginatorEntityId() {
        return entityId;
    }

}
