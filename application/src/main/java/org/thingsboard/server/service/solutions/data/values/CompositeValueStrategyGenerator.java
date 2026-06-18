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
