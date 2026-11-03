// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
