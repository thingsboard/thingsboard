// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.notification.rule.trigger.config;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.alarm.AlarmSearchStatus;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;

import java.io.Serial;
import java.io.Serializable;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema
public class AlarmNotificationRuleTriggerConfig implements NotificationRuleTriggerConfig {

    @Serial
    private static final long serialVersionUID = -7382883720381542344L;

    @ArraySchema(schema = @Schema(implementation = String.class))
    private Set<String> alarmTypes;
    @ArraySchema(schema = @Schema(implementation = AlarmSeverity.class))
    private Set<AlarmSeverity> alarmSeverities;
    @NotEmpty
    @ArraySchema(schema = @Schema(implementation = AlarmAction.class))
    private Set<AlarmAction> notifyOn;
    @Schema
    private ClearRule clearRule;

    @Override
    public NotificationRuleTriggerType getTriggerType() {
        return NotificationRuleTriggerType.ALARM;
    }

    @Data
    public static class ClearRule implements Serializable {
        @Serial
        private static final long serialVersionUID = 7922533150038105124L;
        private Set<AlarmSearchStatus> alarmStatuses;
    }

    @Schema
    public enum AlarmAction {
        CREATED, SEVERITY_CHANGED, ACKNOWLEDGED, CLEARED
    }

}
