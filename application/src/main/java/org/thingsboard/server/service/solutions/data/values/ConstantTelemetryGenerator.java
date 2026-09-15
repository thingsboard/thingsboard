// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.solutions.data.values;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.thingsboard.server.service.solutions.data.definition.TelemetryProfile;

public class ConstantTelemetryGenerator extends TelemetryGenerator {

    private final ConstantValueStrategyDefinition strategy;
    private JsonNode value;

    public ConstantTelemetryGenerator(TelemetryProfile telemetryProfile) {
        super(telemetryProfile);
        this.strategy = (ConstantValueStrategyDefinition) telemetryProfile.getValueStrategy();
        this.value = strategy.getValue();
    }

    @Override
    public void addValue(long ts, ObjectNode values) {
        values.set(key, value);
    }

    @Override
    public double getValue() {
        if (value != null && value.isNumber()) {
            return value.doubleValue();
        } else {
            return super.getValue();
        }
    }

    @Override
    public void setValue(double value) {
        // do nothing;
    }
}
