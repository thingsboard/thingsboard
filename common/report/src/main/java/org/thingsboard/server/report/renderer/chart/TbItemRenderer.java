// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.report.renderer.chart;

import org.thingsboard.server.report.renderer.chart.legend.TbLegendItem;
import org.thingsboard.server.report.renderer.chart.legend.TbLegendValuesRequest;
import org.thingsboard.server.report.renderer.chart.legend.TbSeriesLegendValuesGenerator;

public interface TbItemRenderer {

    TbLegendItem getTbLegendItem(TbLegendValuesRequest request, int datasetIndex, int series);

    TbSeriesLegendValuesGenerator getTbSeriesLegendValuesGenerator();

    void setTbSeriesLegendValuesGenerator(TbSeriesLegendValuesGenerator seriesLegendValuesGenerator);

}
