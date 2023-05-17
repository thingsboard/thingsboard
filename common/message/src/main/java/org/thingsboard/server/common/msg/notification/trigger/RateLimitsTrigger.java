package org.thingsboard.server.common.msg.notification.trigger;

import lombok.Builder;
import lombok.Data;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.rule.trigger.NotificationRuleTriggerType;

@Data
@Builder
public class RateLimitsTrigger implements NotificationRuleTrigger {

    private final TenantId tenantId;
    private final String api;
    private final EntityId limitLevel;
    private final String limitLevelEntityName;

    @Override
    public NotificationRuleTriggerType getType() {
        return NotificationRuleTriggerType.RATE_LIMITS;
    }

    @Override
    public EntityId getOriginatorEntityId() {
        return tenantId;
    }

}
