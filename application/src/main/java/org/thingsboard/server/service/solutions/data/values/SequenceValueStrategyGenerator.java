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

import java.util.Random;

public class SequenceValueStrategyGenerator extends TelemetryGenerator {

    private final SequenceValueStrategyDefinition strategy;
    private int max;
    private int index;

    public SequenceValueStrategyGenerator(TelemetryProfile telemetryProfile) {
        super(telemetryProfile);
        this.strategy = (SequenceValueStrategyDefinition) telemetryProfile.getValueStrategy();
        max = strategy.getTelemetry().fields().next().getValue().size() - 1;
        index = strategy.isRandom() ? new Random().nextInt(max + 1) : 0;

    }

    @Override
    public void addValue(long ts, ObjectNode values) {
        strategy.getTelemetry().fields().forEachRemaining(entry -> {
            String key = entry.getKey();
            values.set(key, entry.getValue().get(index));
        });
        index++;
        if (index > max) {
            index = 0;
        }
    }
}
