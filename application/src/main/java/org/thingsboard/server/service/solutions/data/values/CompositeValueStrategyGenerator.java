// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.solutions.data.values;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.thingsboard.server.service.solutions.data.definition.TelemetryProfile;

public class CompositeValueStrategyGenerator extends TelemetryGenerator {

    TelemetryGenerator defaultGenerator;
    TelemetryGenerator whGenerator;
    TelemetryGenerator nhGenerator;
    TelemetryGenerator hhGenerator;

    public CompositeValueStrategyGenerator(TelemetryProfile tp) {
        super(tp);
        var def = (CompositeValueStrategyDefinition) tp.getValueStrategy();
        defaultGenerator = TelemetryGeneratorFactory.create(new TelemetryProfile(tp.getKey(), def.getDefaultHours()));
        if (def.getWorkHours() != null) {
            whGenerator = TelemetryGeneratorFactory.create(new TelemetryProfile(tp.getKey(), def.getWorkHours()));
        }
        if (def.getNightHours() != null) {
            nhGenerator = TelemetryGeneratorFactory.create(new TelemetryProfile(tp.getKey(), def.getNightHours()));
        }
        if (def.getHolidayHours() != null) {
            hhGenerator = TelemetryGeneratorFactory.create(new TelemetryProfile(tp.getKey(), def.getHolidayHours()));
        }
    }

    @Override
    public void addValue(long ts, ObjectNode values) {
        if (hhGenerator != null && GeneratorTools.isHoliday(ts)) {
            hhGenerator.addValue(ts, values);
        } else if (nhGenerator != null && GeneratorTools.isNightHour(ts)) {
            nhGenerator.addValue(ts, values);
        } else if (whGenerator != null && GeneratorTools.isWorkHour(ts)) {
            whGenerator.addValue(ts, values);
        } else {
            defaultGenerator.addValue(ts, values);
        }
    }
}
