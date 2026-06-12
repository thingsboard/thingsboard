/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.service.solutions.data.values;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.Setter;
import org.thingsboard.server.service.solutions.data.definition.TelemetryProfile;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.thingsboard.server.service.solutions.data.values.GeneratorTools.getMultiplier;
import static org.thingsboard.server.service.solutions.data.values.GeneratorTools.randomDouble;

public class NaturalTelemetryGenerator extends TelemetryGenerator {

    private final NaturalValueStrategyDefinition strategy;
    @Getter @Setter
    private double value;
    private boolean isIncrement;
    private double lowValue;
    private double highValue;

    public NaturalTelemetryGenerator(TelemetryProfile telemetryProfile) {
        super(telemetryProfile);
        this.strategy = (NaturalValueStrategyDefinition) telemetryProfile.getValueStrategy();
        this.value = getRandomStartValue();
        this.lowValue = getRandomLowValue();
        this.highValue = getRandomHighValue();
        isIncrement = !strategy.isDecrementOnStart();
    }

    @Override
    public void addValue(long ts, ObjectNode values) {

        double multiplier = getMultiplier(ts, strategy.getHolidayMultiplier(), strategy.getWorkHoursMultiplier(), strategy.getNightHoursMultiplier());

        if (isIncrement) {
            double step = randomDouble(strategy.getMinIncrement(), strategy.getMaxIncrement());
            value += step * multiplier;
            if (value > highValue) {
                value = highValue;
                highValue = getRandomHighValue();
                isIncrement = false;
            }
        } else {
            double step = randomDouble(strategy.getMinDecrement(), strategy.getMaxDecrement());
            value -= step * multiplier;
            if (value < lowValue) {
                value = lowValue;
                lowValue = getRandomLowValue();
                isIncrement = true;
            }
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

    public double getRandomLowValue() {
        return randomDouble(strategy.getMinLowValue(), strategy.getMaxLowValue());
    }

    public double getRandomHighValue() {
        return randomDouble(strategy.getMinHighValue(), strategy.getMaxHighValue());
    }
}
