// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.report.renderer.chart;

import org.thingsboard.server.common.data.report.configuration.chart.TimeSeriesChartStateSettings;
import org.thingsboard.server.common.data.report.configuration.chart.TimeSeriesChartStateSourceType;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TbStateValueConverter {

    private final List<TbStateTick> stateTicks;
    private final Map<String, Double> constantsMap;
    private final Map<Double, String> labelsMap;
    private final List<TimeSeriesChartStateSettings> rangeStates;

    public TbStateValueConverter(List<TimeSeriesChartStateSettings> states) {
        List<TimeSeriesChartStateSettings> validStates = states.stream().filter(TimeSeriesChartStateSettings::isValidState).toList();
        this.stateTicks = validStates.stream().map(state -> new TbStateTick(state.getValue(), state.getLabel()))
                .collect(Collectors.toMap(TbStateTick::getValue, Function.identity(), (oldValue, newValue) -> oldValue)).values()
                .stream().sorted(Comparator.comparing(TbStateTick::getValue)).toList();
        this.labelsMap = this.stateTicks.stream().collect(Collectors.toMap(TbStateTick::getValue, TbStateTick::getLabel));
        this.constantsMap = validStates.stream().filter(state -> TimeSeriesChartStateSourceType.constant.equals(state.getSourceType()))
                .collect(Collectors.toMap(TimeSeriesChartStateSettings::sourceValueAsString, TimeSeriesChartStateSettings::getValue,
                        (oldValue, newValue) -> oldValue, LinkedHashMap::new));
        this.rangeStates = validStates.stream().filter(state -> TimeSeriesChartStateSourceType.range.equals(state.getSourceType())).toList();

    }

    public Double convertValue(String value) {
        if (this.constantsMap.containsKey(value)) {
            return this.constantsMap.get(value);
        } else {
            try {
                double doubleValue = Double.parseDouble(value);
                if (!this.rangeStates.isEmpty()) {
                    for (TimeSeriesChartStateSettings state : this.rangeStates) {
                        if (constantRange(state) && state.getSourceRangeFrom() == doubleValue) {
                            return state.getValue();
                        } else if ((state.getSourceRangeFrom() == null || doubleValue >= state.getSourceRangeFrom())
                                && (state.getSourceRangeTo() == null || doubleValue < state.getSourceRangeTo())) {
                            return state.getValue();
                        }
                    }
                }
                return doubleValue;
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }

    public String formatLabel(Double value) {
        return this.labelsMap.get(value);
    }

    public List<TbStateTick> getStateTicks() {
        return stateTicks;
    }

    static boolean constantRange(TimeSeriesChartStateSettings state) {
        return (state.getSourceRangeFrom() != null && state.getSourceRangeTo() != null && state.getSourceRangeFrom().equals(state.getSourceRangeTo()));
    }
}
