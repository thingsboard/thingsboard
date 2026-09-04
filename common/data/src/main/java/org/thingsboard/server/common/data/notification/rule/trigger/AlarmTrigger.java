// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.notification.rule.trigger;

import lombok.Builder;
import lombok.Data;
import org.thingsboard.server.common.data.alarm.AlarmApiCallResult;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.rule.trigger.config.NotificationRuleTriggerType;

@Data
@Builder
public class AlarmTrigger implements NotificationRuleTrigger {

    private final TenantId tenantId;
    private final AlarmApiCallResult alarmUpdate;

    @Override
    public NotificationRuleTriggerType getType() {
        return NotificationRuleTriggerType.ALARM;
    }

    @Override
    public EntityId getOriginatorEntityId() {
        return alarmUpdate.getAlarm().getId();
    }

}
