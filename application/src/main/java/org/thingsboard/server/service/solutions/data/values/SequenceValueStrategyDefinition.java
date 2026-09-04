// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.solutions.data.values;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Data;


@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SequenceValueStrategyDefinition implements ValueStrategyDefinition {
    private boolean random;
    private ObjectNode telemetry;

    @Override
    public ValueStrategyDefinitionType getStrategyType() {
        return ValueStrategyDefinitionType.SEQUENCE;
    }

}
