// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.notification.rule.trigger.config;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.alarm.AlarmSearchStatus;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;

import java.io.Serial;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema
public class AlarmCommentNotificationRuleTriggerConfig implements NotificationRuleTriggerConfig {

    @Serial
    private static final long serialVersionUID = -9164282098882339645L;

    @ArraySchema(schema = @Schema(implementation = String.class))
    private Set<String> alarmTypes;
    @ArraySchema(schema = @Schema(implementation = AlarmSeverity.class))
    private Set<AlarmSeverity> alarmSeverities;
    @ArraySchema(schema = @Schema(implementation = AlarmSearchStatus.class))
    private Set<AlarmSearchStatus> alarmStatuses;
    @Schema
    private boolean onlyUserComments;
    @Schema
    private boolean notifyOnCommentUpdate;

    @Override
    public NotificationRuleTriggerType getTriggerType() {
        return NotificationRuleTriggerType.ALARM_COMMENT;
    }

}
