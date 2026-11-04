// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.report.context.chart;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.thingsboard.server.common.data.report.configuration.timewindow.TimeIntervalCalculator;

@Data
@RequiredArgsConstructor
public class TsChartSeriesEntry {

    private final long ts;
    private final TimeIntervalCalculator.TimeRange interval;
    private final String value;
    private final Double doubleValue;

}
