// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.solutions.data.values;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import org.thingsboard.server.service.solutions.data.definition.TelemetryProfile;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.thingsboard.server.service.solutions.data.values.GeneratorTools.randomDouble;

public abstract class IncDecTelemetryGenerator<T extends IncDecValueStrategyDefinition> extends TelemetryGenerator {

    protected final T strategy;
    @Getter
    protected double value;
    protected double endValue;

    @SuppressWarnings("unchecked")
    public IncDecTelemetryGenerator(TelemetryProfile telemetryProfile) {
        super(telemetryProfile);
        this.strategy = (T) telemetryProfile.getValueStrategy();
        this.value = getRandomStartValue();
        this.endValue = getRandomEndValue();
    }

    public void setValue(double value) {
        this.value = value;
        this.endValue = getRandomEndValue();
    }

    protected void put(ObjectNode values, double value) {
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
