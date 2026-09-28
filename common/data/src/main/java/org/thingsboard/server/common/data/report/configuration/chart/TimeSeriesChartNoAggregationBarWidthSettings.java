// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.report.configuration.chart;

import lombok.Data;

@Data
public class TimeSeriesChartNoAggregationBarWidthSettings {

    private TimeSeriesChartNoAggregationBarWidthStrategy strategy;
    private TimeSeriesChartBarWidth groupWidth;
    private TimeSeriesChartBarWidth barWidth;

    public TimeSeriesChartNoAggregationBarWidthSettings() {}

    public TimeSeriesChartNoAggregationBarWidthSettings(TimeSeriesChartNoAggregationBarWidthSettings input) {
        if (input == null) {
            input = new TimeSeriesChartNoAggregationBarWidthSettings();
        }
        this.strategy = input.getStrategy() != null ? input.strategy : TimeSeriesChartNoAggregationBarWidthStrategy.group;
        this.groupWidth = new TimeSeriesChartBarWidth(input.getGroupWidth());
        this.barWidth = new TimeSeriesChartBarWidth(input.getBarWidth());
    }

}
