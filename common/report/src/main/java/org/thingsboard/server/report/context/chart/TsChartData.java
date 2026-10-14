// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.report.context.chart;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.thingsboard.server.common.data.report.configuration.timewindow.TimeIntervalCalculator;

import java.util.List;
import java.util.Map;
import java.util.TimeZone;

@Data
@RequiredArgsConstructor
public class TsChartData {

    private final TimeZone timeZone;
    private final TimeIntervalCalculator.TimeRange timeRange;
    private final boolean noAggregation;
    private final List<TsChartDataSource> chartData;
    private final List<TsChartThresholdItem> thresholdItems;
    private final Map<String, YAxisScale> yAxisScales;
    private final List<TsChartRangeItem> rangeItems;
    private final boolean comparisonEnabled;
    private final TimeIntervalCalculator.TimeRange comparisonTimeRange;

}
