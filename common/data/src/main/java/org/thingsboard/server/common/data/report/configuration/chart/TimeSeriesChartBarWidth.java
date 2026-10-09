// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.report.configuration.chart;

import lombok.Data;

@Data
public class TimeSeriesChartBarWidth {

    private Boolean relative;
    private Double relativeWidth;
    private Double absoluteWidth;

    public TimeSeriesChartBarWidth() {}

    public TimeSeriesChartBarWidth(TimeSeriesChartBarWidth input) {
        if (input == null) {
            input = new TimeSeriesChartBarWidth();
        }
        this.relative = input.getRelative() != null ? input.getRelative() : Boolean.TRUE;
        this.relativeWidth = input.getRelativeWidth() != null ? input.getRelativeWidth() : 2;
        this.absoluteWidth = input.getAbsoluteWidth() != null ? input.getAbsoluteWidth() : 1000;
    }

}
