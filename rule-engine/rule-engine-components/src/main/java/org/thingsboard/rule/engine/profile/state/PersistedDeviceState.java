// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.rule.engine.profile.state;

import lombok.Data;

import java.util.Map;

@Data
public class PersistedDeviceState {

    Map<String, PersistedAlarmState> alarmStates;

}
