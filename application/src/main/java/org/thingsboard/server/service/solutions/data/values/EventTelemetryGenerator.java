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
import org.thingsboard.server.service.solutions.data.definition.TelemetryProfile;

import static org.thingsboard.server.service.solutions.data.values.GeneratorTools.randomLong;


public class EventTelemetryGenerator extends TelemetryGenerator {

    private final EventValueStrategyDefinition strategy;
    private final long currentAnomaly;
    private Object value;

    public EventTelemetryGenerator(TelemetryProfile telemetryProfile) {
        super(telemetryProfile);
        this.strategy = (EventValueStrategyDefinition) telemetryProfile.getValueStrategy();
        currentAnomaly = randomLong(0, strategy.getAnomalyChance());
    }

    @Override
    public void addValue(long ts, ObjectNode values) {
        boolean anomaly = false;
        if (randomLong(0, strategy.getAnomalyChance()) == currentAnomaly) {
            anomaly = true;
        }
        this.value = anomaly ? strategy.getAnomalyValue() : strategy.getNormalValue();
        if (value instanceof Boolean) {
            values.put(key, (Boolean) value);
        } else if (value instanceof Double) {
            values.put(key, ((Double) value));
        } else if (value instanceof Integer) {
            values.put(key, ((Integer) value));
        } else {
            throw new RuntimeException("Not supported value for event telemetry generator: " + value);
        }
    }

}
