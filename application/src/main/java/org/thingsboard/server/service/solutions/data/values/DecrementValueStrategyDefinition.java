// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.solutions.data.values;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DecrementValueStrategyDefinition extends IncDecValueStrategyDefinition {

    private double minDecrement;
    private double maxDecrement;

    @Override
    public ValueStrategyDefinitionType getStrategyType() {
        return ValueStrategyDefinitionType.DECREMENT;
    }

}
