// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.solutions.data.values;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class IncrementValueStrategyDefinition extends IncDecValueStrategyDefinition {

    private double minIncrement;
    private double maxIncrement;

    @Override
    public ValueStrategyDefinitionType getStrategyType() {
        return ValueStrategyDefinitionType.INCREMENT;
    }

}
