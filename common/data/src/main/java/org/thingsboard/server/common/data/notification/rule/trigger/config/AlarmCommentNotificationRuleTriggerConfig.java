// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.notification.rule.trigger.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.alarm.AlarmSearchStatus;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;

import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AlarmCommentNotificationRuleTriggerConfig implements NotificationRuleTriggerConfig {

    private Set<String> alarmTypes;
    private Set<AlarmSeverity> alarmSeverities;
    private Set<AlarmSearchStatus> alarmStatuses;
    private boolean onlyUserComments;
    private boolean notifyOnCommentUpdate;

    @Override
    public NotificationRuleTriggerType getTriggerType() {
        return NotificationRuleTriggerType.ALARM_COMMENT;
    }

}
