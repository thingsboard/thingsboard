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
