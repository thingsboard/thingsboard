// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.notification.rule.trigger;

import lombok.Builder;
import lombok.Data;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmComment;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.rule.trigger.config.NotificationRuleTriggerType;

import java.io.Serial;

@Data
@Builder
public class AlarmCommentTrigger implements NotificationRuleTrigger {

    @Serial
    private static final long serialVersionUID = -8614770559491757202L;

    private final TenantId tenantId;
    private final AlarmComment comment;
    private final Alarm alarm;
    private final ActionType actionType;
    private final User user;

    @Override
    public NotificationRuleTriggerType getType() {
        return NotificationRuleTriggerType.ALARM_COMMENT;
    }

    @Override
    public EntityId getOriginatorEntityId() {
        return alarm.getId();
    }

}
