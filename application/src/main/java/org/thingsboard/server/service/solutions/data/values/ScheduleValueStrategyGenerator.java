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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TimeZone;

public class ScheduleValueStrategyGenerator extends TelemetryGenerator {

    private TelemetryGenerator defaultGenerator;
    private TimeZone timeZone;
    private Map<ValueStrategySchedule, TelemetryGenerator> scheduleGenerators;
    private TelemetryGenerator prevGenerator;

    public ScheduleValueStrategyGenerator(TelemetryProfile tp) {
        super(tp);
        var def = (ScheduleValueStrategyDefinition) tp.getValueStrategy();
        timeZone = TimeZone.getTimeZone(def.getTimeZone());
        defaultGenerator = TelemetryGeneratorFactory.create(new TelemetryProfile(tp.getKey(), def.getDefaultDefinition()));
        scheduleGenerators = new LinkedHashMap<>();
        for (ValueStrategySchedule scheduleItem : def.getSchedule()) {
            scheduleGenerators.put(scheduleItem, TelemetryGeneratorFactory.create(new TelemetryProfile(tp.getKey(), scheduleItem.getDefinition())));
        }
    }

    @Override
    public void addValue(long ts, ObjectNode values) {
        int hour = GeneratorTools.getHour(timeZone, ts);
        int minute = GeneratorTools.getMinute(timeZone, ts);
        TelemetryGenerator generator = scheduleGenerators.entrySet().stream().filter(pair -> {
            var schedule = pair.getKey();
            if (hour == schedule.getStartHour() && hour == schedule.getEndHour()) {
                return schedule.getStartMinute() <= minute && minute <= schedule.getEndMinute();
            } else if (hour == schedule.getStartHour() && hour < schedule.getEndHour()) {
                return schedule.getStartMinute() <= minute;
            } else if (hour > schedule.getStartHour() && hour < schedule.getEndHour()) {
                return true;
            } else if (hour > schedule.getStartHour() && hour == schedule.getEndHour()) {
                return minute <= schedule.getEndHour();
            } else {
                return false;
            }
        }).map(Map.Entry::getValue).findFirst().orElse(defaultGenerator);

        if (prevGenerator != null && !prevGenerator.equals(generator)) {
            generator.setValue(prevGenerator.getValue());
        }

        generator.addValue(ts, values);

        prevGenerator = generator;
    }
}
