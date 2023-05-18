package org.thingsboard.server.common.msg.notification.trigger;

import lombok.Builder;
import lombok.Data;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.rule.trigger.NotificationRuleTriggerType;

import java.util.concurrent.TimeUnit;

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
        return limitLevel != null ? limitLevel : tenantId;
    }


    private static final long deduplicationDuration = TimeUnit.HOURS.toMillis(1);

    @Override
    public boolean deduplicate() {
        return true;
    }

    @Override
    public String getDeduplicationKey() {
        return String.join(":", NotificationRuleTrigger.super.getDeduplicationKey(), api);
    }

    @Override
    public long getDeduplicationDuration() {
        return deduplicationDuration;
    }

}
