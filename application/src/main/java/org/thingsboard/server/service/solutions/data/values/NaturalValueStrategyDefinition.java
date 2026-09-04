// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.solutions.data.values;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class NaturalValueStrategyDefinition implements ValueStrategyDefinition {
    private int precision;
    private double minStartValue;
    private double maxStartValue;
    private double minLowValue;
    private double maxLowValue;
    private double minHighValue;
    private double maxHighValue;
    private double minIncrement;
    private double maxIncrement;
    private double minDecrement;
    private double maxDecrement;
    private double holidayMultiplier;
    private double workHoursMultiplier;
    private double nightHoursMultiplier;
    private boolean decrementOnStart;

    @Override
    public ValueStrategyDefinitionType getStrategyType() {
        return ValueStrategyDefinitionType.NATURAL;
    }

}
