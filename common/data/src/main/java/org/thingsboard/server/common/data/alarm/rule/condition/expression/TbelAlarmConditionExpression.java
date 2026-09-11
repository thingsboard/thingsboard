// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.alarm.rule.condition.expression;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TbelAlarmConditionExpression implements AlarmConditionExpression {

    @NotBlank
    private String expression;

    @Override
    public AlarmConditionExpressionType getType() {
        return AlarmConditionExpressionType.TBEL;
    }

}
