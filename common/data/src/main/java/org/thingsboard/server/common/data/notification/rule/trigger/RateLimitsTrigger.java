// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.notification.rule.trigger;

import lombok.Builder;
import lombok.Data;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.limit.LimitedApi;
import org.thingsboard.server.common.data.notification.rule.trigger.config.NotificationRuleTriggerType;

import java.io.Serial;
import java.util.concurrent.TimeUnit;

@Data
@Builder
public class RateLimitsTrigger implements NotificationRuleTrigger {

    @Serial
    private static final long serialVersionUID = -4423112145409424886L;

    private final TenantId tenantId;
    private final LimitedApi api;
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


    @Override
    public DeduplicationStrategy getDeduplicationStrategy() {
        return DeduplicationStrategy.ALL;
    }

    @Override
    public String getDeduplicationKey() {
        return String.join(":", NotificationRuleTrigger.super.getDeduplicationKey(), api.toString());
    }

    @Override
    public long getDefaultDeduplicationDuration() {
        return TimeUnit.HOURS.toMillis(4);
    }

}
