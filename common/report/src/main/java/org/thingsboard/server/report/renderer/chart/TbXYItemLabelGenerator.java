// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.report.renderer.chart;

import org.jfree.chart.labels.XYItemLabelGenerator;
import org.jfree.data.xy.XYDataset;

import java.text.NumberFormat;

import static org.thingsboard.server.report.renderer.chart.ChartUtils.createValueFormatter;

public class TbXYItemLabelGenerator implements XYItemLabelGenerator {

    private final NumberFormat formatter;
    private final TbStateValueConverter stateValueConverter;

    public TbXYItemLabelGenerator(Integer decimals, String units, TbStateValueConverter stateValueConverter) {
        int decimalsInt = decimals != null ? decimals : 2;
        this.formatter = createValueFormatter(decimalsInt, units);
        this.stateValueConverter = stateValueConverter;
    }

    @Override
    public String generateLabel(XYDataset dataset, int series, int item) {
        double y = dataset.getYValue(series, item);
        if (Double.isNaN(y) && dataset.getY(series, item) == null) {
            return "";
        } else {
            String label = null;
            if (stateValueConverter != null) {
                label = stateValueConverter.formatLabel(y);
            }
            if (label == null) {
                label = formatter.format(y);
            }
            return label;
        }
    }
}
