// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.alarm.rule.condition;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
public class SimpleAlarmCondition extends AlarmCondition {

    @Override
    public AlarmConditionType getType() {
        return AlarmConditionType.SIMPLE;
    }

}
