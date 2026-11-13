// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.report.context.chart;

import lombok.Data;
import org.thingsboard.server.common.data.report.configuration.chart.TimeSeriesChartThreshold;

@Data
public class TsChartThresholdItem {

    private final TimeSeriesChartThreshold settings;
    private final Double value;

    public TsChartThresholdItem(TimeSeriesChartThreshold settings, Double value) {
        this.settings = new TimeSeriesChartThreshold(settings);
        this.value = value;
    }

}
