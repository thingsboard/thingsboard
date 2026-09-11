// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.solutions.data.values;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.thingsboard.server.service.solutions.data.definition.TelemetryProfile;

import static org.thingsboard.server.service.solutions.data.values.GeneratorTools.randomDouble;

public class IncrementTelemetryGenerator extends IncDecTelemetryGenerator<IncrementValueStrategyDefinition> {

    public IncrementTelemetryGenerator(TelemetryProfile telemetryProfile) {
        super(telemetryProfile);
    }

    @Override
    public void addValue(long ts, ObjectNode values) {
        double step = randomDouble(strategy.getMinIncrement(), strategy.getMaxIncrement());
        double newValue = value + step;
        value = Math.min(newValue, endValue);
        put(values, value);
    }
}
