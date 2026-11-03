// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.solutions.data.values;


import org.thingsboard.server.service.solutions.data.definition.TelemetryProfile;

public class TelemetryGeneratorFactory {

    public static TelemetryGenerator create(TelemetryProfile tp) {
        switch (tp.getValueStrategy().getStrategyType()) {
            case COUNTER:
                return new CounterTelemetryGenerator(tp);
            case NATURAL:
                return new NaturalTelemetryGenerator(tp);
            case EVENT:
                return new EventTelemetryGenerator(tp);
            case CONSTANT:
                return new ConstantTelemetryGenerator(tp);
            case SEQUENCE:
                return new SequenceValueStrategyGenerator(tp);
            case COMPOSITE:
                return new CompositeValueStrategyGenerator(tp);
            case SCHEDULE:
                return new ScheduleValueStrategyGenerator(tp);
            case INCREMENT:
                return new IncrementTelemetryGenerator(tp);
            case DECREMENT:
                return new DecrementTelemetryGenerator(tp);
            default:
                throw new RuntimeException("Not supported!");
        }
    }

}
