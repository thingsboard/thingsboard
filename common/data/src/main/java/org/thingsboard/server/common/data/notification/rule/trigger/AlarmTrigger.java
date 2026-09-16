// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.notification.rule.trigger;

import lombok.Builder;
import lombok.Data;
import org.thingsboard.server.common.data.alarm.AlarmApiCallResult;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.rule.trigger.config.NotificationRuleTriggerType;

import java.io.Serial;

@Data
@Builder
public class AlarmTrigger implements NotificationRuleTrigger {

    @Serial
    private static final long serialVersionUID = -466810297904938644L;

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
