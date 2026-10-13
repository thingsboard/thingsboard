// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.report.renderer;

import org.jfree.chart.JFreeChart;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.report.configuration.chart.ReportBarChartWithLabelsSettings;
import org.thingsboard.server.common.data.report.configuration.chart.ReportRangeChartSettings;
import org.thingsboard.server.common.data.report.configuration.chart.ReportTimeSeriesChartSettings;
import org.thingsboard.server.common.data.report.configuration.chart.TimeSeriesChartKeySettings;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponentType;
import org.thingsboard.server.common.data.report.configuration.components.TimeseriesChartComponent;
import org.thingsboard.server.report.context.ComponentData;
import org.thingsboard.server.report.context.chart.TsChartData;
import org.thingsboard.server.report.renderer.chart.TbTimeSeriesChart;
import org.thingsboard.server.report.renderer.chart.TbVisualMap;

import java.awt.Graphics2D;

@Component
public class TimeseriesChartRenderer extends ChartRenderer<TimeseriesChartComponent> {

    @Override
    protected JFreeChart createChart(Graphics2D g2, TimeseriesChartComponent component, ComponentData reportDataSource) {

        ReportTimeSeriesChartSettings chartSettings;
        ReportTimeSeriesChartSettings inputSettings = component.getTimeSeriesChartSettings();
        TbVisualMap visualMap = null;
        String units = null;
        Integer decimals = null;
        if (inputSettings instanceof ReportBarChartWithLabelsSettings inputBarChartWithLabelsSettings) {
            ReportBarChartWithLabelsSettings barChartWithLabelsSettings = new ReportBarChartWithLabelsSettings(inputBarChartWithLabelsSettings);

            TimeSeriesChartKeySettings keySettings = barChartWithLabelsSettings.toTimeSeriesChartKeySettings();

            TsChartData data = reportDataSource.getTsChartData();
            data.getChartData().forEach(chartData -> chartData.getDataKeys().forEach(chartDataKey -> {
                chartDataKey.setSettings(keySettings);
                chartDataKey.setDecimals(barChartWithLabelsSettings.getBarDecimals());
                chartDataKey.setUnits("");
            }));

            units = barChartWithLabelsSettings.getBarUnits();
            decimals = barChartWithLabelsSettings.getBarDecimals();
            chartSettings = barChartWithLabelsSettings;
        } else if (inputSettings instanceof ReportRangeChartSettings inputRangeChartSettings) {
            ReportRangeChartSettings reportRangeChartSettings = new ReportRangeChartSettings(inputRangeChartSettings);
            TimeSeriesChartKeySettings keySettings = reportRangeChartSettings.toTimeSeriesChartKeySettings();
            TsChartData data = reportDataSource.getTsChartData();
            data.getChartData().forEach(chartData -> chartData.getDataKeys().forEach(chartDataKey -> {
                chartDataKey.setSettings(keySettings);
                chartDataKey.setDecimals(reportRangeChartSettings.getRangeDecimals());
                chartDataKey.setUnits(reportRangeChartSettings.getRangeUnits());
            }));

            visualMap = new TbVisualMap(reportRangeChartSettings.getOutOfRangeColor(), reportDataSource.getTsChartData().getRangeItems(),
                    reportRangeChartSettings.getFillArea(), reportRangeChartSettings.getFillAreaOpacity());

            units = reportRangeChartSettings.getRangeUnits();
            decimals = reportRangeChartSettings.getRangeDecimals();
            reportRangeChartSettings.getYAxes().get("default").setUnits(units);
            reportRangeChartSettings.getYAxes().get("default").setDecimals(decimals);
            chartSettings = reportRangeChartSettings;
        } else {
            chartSettings = new ReportTimeSeriesChartSettings(inputSettings);
        }

        TbTimeSeriesChart timeSeriesChart = new TbTimeSeriesChart(chartSettings, reportDataSource.getTsChartData(), reportDataSource.getVariables(),
                                                                  visualMap, units, decimals);

        return timeSeriesChart.createChart(g2);
    }

    @Override
    public ReportComponentType getType() {
        return ReportComponentType.TIME_SERIES_CHART;
    }

}
