// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.solutions.data.values;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.Setter;
import org.thingsboard.server.service.solutions.data.definition.TelemetryProfile;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.thingsboard.server.service.solutions.data.values.GeneratorTools.getMultiplier;
import static org.thingsboard.server.service.solutions.data.values.GeneratorTools.randomDouble;

public class CounterTelemetryGenerator extends TelemetryGenerator {

    private final CounterValueStrategyDefinition strategy;
    @Getter @Setter
    private double value;

    public CounterTelemetryGenerator(TelemetryProfile telemetryProfile) {
        super(telemetryProfile);
        this.strategy = (CounterValueStrategyDefinition) telemetryProfile.getValueStrategy();
        this.value = getRandomStartValue();
    }

    @Override
    public void addValue(long ts, ObjectNode values) {
        double step = randomDouble(strategy.getMinIncrement(), strategy.getMaxIncrement());
        double multiplier = getMultiplier(ts, strategy.getHolidayMultiplier(), strategy.getWorkHoursMultiplier(), strategy.getNightHoursMultiplier());
        value += step * multiplier;
        if (value > getRandomEndValue()) {
            value = getRandomStartValue();
        }
        put(values, value);
    }

    private void put(ObjectNode values, double value) {
        if (strategy.getPrecision() == 0) {
            values.put(key, (int) value);
        } else {
            values.put(key, BigDecimal.valueOf(value)
                    .setScale(strategy.getPrecision(), RoundingMode.HALF_UP)
                    .doubleValue());
        }
    }

    public double getRandomStartValue() {
        return randomDouble(strategy.getMinStartValue(), strategy.getMaxStartValue());
    }

    public double getRandomEndValue() {
        return randomDouble(strategy.getMinEndValue(), strategy.getMaxEndValue());
    }
}
