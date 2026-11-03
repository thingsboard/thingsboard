// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.solutions.data.values;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConstantValueStrategyDefinition implements ValueStrategyDefinition {
    private JsonNode value;

    @Override
    public ValueStrategyDefinitionType getStrategyType() {
        return ValueStrategyDefinitionType.CONSTANT;
    }
}
