// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.report.renderer.chart.legend;

import org.jfree.data.xy.XYDataset;

public interface TbSeriesLegendValuesGenerator {

    TbLegendValues generateLegendValues(TbLegendValuesRequest request, XYDataset dataset, int series);

}
