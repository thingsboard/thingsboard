// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
