// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.alarm.rule.condition;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AlarmConditionValue<T> {

    private T staticValue;
    private String dynamicValueArgument;

    @JsonIgnore
    @AssertTrue(message = "Either staticValue or dynamicValueArgument must be set")
    public boolean isValid() {
        return staticValue != null ^ dynamicValueArgument != null;
    }

}
