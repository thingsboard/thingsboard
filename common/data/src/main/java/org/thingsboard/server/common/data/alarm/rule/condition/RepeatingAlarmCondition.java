// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.alarm.rule.condition;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class RepeatingAlarmCondition extends AlarmCondition {

    @Valid
    @NotNull
    private AlarmConditionValue<Integer> count;

    @Override
    public AlarmConditionType getType() {
        return AlarmConditionType.REPEATING;
    }

}
