// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.notification.rule.trigger;

import lombok.Builder;
import lombok.Data;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.rule.trigger.config.NotificationRuleTriggerType;

@Data
@Builder
public class AlarmAssignmentTrigger implements NotificationRuleTrigger {

    private final TenantId tenantId;
    private final AlarmInfo alarmInfo;
    private final ActionType actionType;
    private final User user;

    @Override
    public EntityId getOriginatorEntityId() {
        return alarmInfo.getOriginator();
    }

    @Override
    public NotificationRuleTriggerType getType() {
        return NotificationRuleTriggerType.ALARM_ASSIGNMENT;
    }

}
