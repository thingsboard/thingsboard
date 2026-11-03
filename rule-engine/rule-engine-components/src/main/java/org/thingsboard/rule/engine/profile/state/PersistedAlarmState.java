// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.rule.engine.profile.state;

import lombok.Data;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;

import java.util.Map;

@Data
public class PersistedAlarmState {

    private Map<AlarmSeverity, PersistedAlarmRuleState> createRuleStates;
    private PersistedAlarmRuleState clearRuleState;

}
