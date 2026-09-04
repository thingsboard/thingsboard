// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.solutions.data.values;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CompositeValueStrategyDefinition implements ValueStrategyDefinition {

    private ValueStrategyDefinition defaultHours;
    private ValueStrategyDefinition workHours;
    private ValueStrategyDefinition nightHours;
    private ValueStrategyDefinition holidayHours;

    @Override
    public ValueStrategyDefinitionType getStrategyType() {
        return ValueStrategyDefinitionType.COMPOSITE;
    }
}
