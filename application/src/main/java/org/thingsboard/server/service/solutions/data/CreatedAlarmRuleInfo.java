// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.solutions.data;

import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.cf.configuration.AlarmCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.id.EntityId;

import java.util.UUID;
import java.util.stream.Collectors;

public record CreatedAlarmRuleInfo(EntityId entityId, String entityName, String alarmType,
                                   String severities) implements HasAppliedToEntity {

    public static CreatedAlarmRuleInfo from(EntityId entityId, String entityName, CalculatedField calculatedField) {
        if (calculatedField.getType() != CalculatedFieldType.ALARM) {
            throw new UnsupportedOperationException("Only alarm calculated fields are supported");
        }
        String severities = ((AlarmCalculatedFieldConfiguration) calculatedField.getConfiguration())
                .getCreateRules().keySet().stream()
                .map(AlarmSeverity::getDisplayName)
                .sorted()
                .collect(Collectors.joining(", "));
        return new CreatedAlarmRuleInfo(entityId, entityName, calculatedField.getName(), severities);
    }

    @Override
    public String getCfPageLink(UUID cfId) {
        return "/alarms/alarm-rules/" + cfId;
    }
}
