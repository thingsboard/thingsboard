// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.report.configuration.chart;

import lombok.Data;

@Data
public class TimeSeriesChartBarWidthSettings {

    private Double barGap;
    private Double intervalGap;

    public TimeSeriesChartBarWidthSettings() {}

    public TimeSeriesChartBarWidthSettings(TimeSeriesChartBarWidthSettings input) {
        if (input == null) {
            input = new TimeSeriesChartBarWidthSettings();
        }
        this.barGap = input.getBarGap() != null ? input.getBarGap() : 0.3;
        this.intervalGap = input.getIntervalGap() != null ? input.getIntervalGap() : 0.6;
    }

}
