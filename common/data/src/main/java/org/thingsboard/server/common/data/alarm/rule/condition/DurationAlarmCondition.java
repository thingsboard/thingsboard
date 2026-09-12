// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.alarm.rule.condition;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.concurrent.TimeUnit;

@Schema
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class DurationAlarmCondition extends AlarmCondition {

    @NotNull
    private TimeUnit unit;
    @Valid
    @NotNull
    private AlarmConditionValue<Long> value;

    @Override
    public AlarmConditionType getType() {
        return AlarmConditionType.DURATION;
    }

}
