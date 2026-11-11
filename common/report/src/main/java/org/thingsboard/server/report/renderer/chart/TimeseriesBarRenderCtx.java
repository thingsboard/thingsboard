// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.report.renderer.chart;

import lombok.Builder;
import lombok.Data;
import org.thingsboard.server.common.data.report.configuration.chart.TimeSeriesChartNoAggregationBarWidthStrategy;

@Data
@Builder
public class TimeseriesBarRenderCtx {

    private double barGap;
    private double intervalGap;
    private TimeSeriesChartNoAggregationBarWidthStrategy noAggregationBarWidthStrategy;
    private boolean noAggregationWidthRelative;
    private double noAggregationWidth;
    private long timeWindow;
    private boolean noAggregation;

}
