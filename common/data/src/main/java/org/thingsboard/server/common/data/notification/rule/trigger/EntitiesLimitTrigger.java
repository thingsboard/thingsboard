// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.notification.rule.trigger;

import lombok.Builder;
import lombok.Data;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.rule.trigger.config.NotificationRuleTriggerType;

@Data
@Builder
public class EntitiesLimitTrigger implements NotificationRuleTrigger {

    private final TenantId tenantId;
    private final EntityType entityType;

    private long limit;
    private long currentCount;

    @Override
    public NotificationRuleTriggerType getType() {
        return NotificationRuleTriggerType.ENTITIES_LIMIT;
    }

    @Override
    public EntityId getOriginatorEntityId() {
        return tenantId;
    }

}
