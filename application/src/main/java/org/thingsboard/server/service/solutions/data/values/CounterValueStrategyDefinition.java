// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.solutions.data.values;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CounterValueStrategyDefinition implements ValueStrategyDefinition {

    private int precision;
    private double minStartValue;
    private double maxStartValue;
    private double minEndValue;
    private double maxEndValue;
    private double minIncrement;
    private double maxIncrement;
    private double holidayMultiplier;
    private double workHoursMultiplier;
    private double nightHoursMultiplier;

    @Override
    public ValueStrategyDefinitionType getStrategyType() {
        return ValueStrategyDefinitionType.COUNTER;
    }

}
