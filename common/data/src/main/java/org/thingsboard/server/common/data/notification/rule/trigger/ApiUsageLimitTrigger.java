// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.notification.rule.trigger;

import lombok.Builder;
import lombok.Data;
import org.thingsboard.server.common.data.ApiUsageRecordState;
import org.thingsboard.server.common.data.ApiUsageStateValue;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.rule.trigger.config.NotificationRuleTriggerType;

@Data
@Builder
public class ApiUsageLimitTrigger implements NotificationRuleTrigger {

    private final TenantId tenantId;
    private final ApiUsageRecordState state;
    private final ApiUsageStateValue status;

    @Override
    public NotificationRuleTriggerType getType() {
        return NotificationRuleTriggerType.API_USAGE_LIMIT;
    }

    @Override
    public EntityId getOriginatorEntityId() {
        return tenantId;
    }

}
