// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.report.context.chart;

import lombok.Data;
import org.thingsboard.server.common.data.report.configuration.DataKey;

@Data
public class LatestChartDataItem {

    private LatestChartDataSource dataSource;
    private DataKey dataKey;
    private String label;

    private long ts;
    private double value;
    private boolean hasValue;

    private int index;

    private int keyIndex;

    private int seriesIndex;

    @Override
    public String toString() {
        return "LatestChartDataItem{" +
                "dataKey=" + dataKey +
                ", index=" + index +
                ", label=" + label +
                ", ts=" + ts +
                ", value=" + value +
                ", hasValue=" + hasValue +
                ", keyIndex=" + keyIndex +
                ", seriesIndex=" + seriesIndex +
                '}';
    }
}
