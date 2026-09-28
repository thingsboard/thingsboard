// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.report.renderer;

import org.jfree.chart.JFreeChart;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.report.configuration.chart.ReportBarChartSettings;
import org.thingsboard.server.common.data.report.configuration.chart.ReportDoughnutChartSettings;
import org.thingsboard.server.common.data.report.configuration.chart.ReportPieChartSettings;
import org.thingsboard.server.common.data.report.configuration.components.LatestChartComponent;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponentType;
import org.thingsboard.server.report.context.ComponentData;
import org.thingsboard.server.report.context.chart.LatestChartData;
import org.thingsboard.server.report.renderer.chart.TbBarChart;
import org.thingsboard.server.report.renderer.chart.TbDoughnutChart;
import org.thingsboard.server.report.renderer.chart.TbLatestChart;
import org.thingsboard.server.report.renderer.chart.TbPieChart;

import java.awt.Graphics2D;
import java.util.Map;

import static org.thingsboard.server.common.data.report.configuration.chart.ReportComponentSubType.DOUGHNUT_CHART;
import static org.thingsboard.server.common.data.report.configuration.chart.ReportComponentSubType.HORIZONTAL_DOUGHNUT_CHART;
import static org.thingsboard.server.common.data.report.configuration.chart.ReportComponentSubType.LATEST_BAR_CHART;
import static org.thingsboard.server.common.data.report.configuration.chart.ReportComponentSubType.PIE_CHART;

@Component
public class LatestChartRenderer extends ChartRenderer<LatestChartComponent> {

    @Override
    protected JFreeChart createChart(Graphics2D g2, LatestChartComponent component, ComponentData reportDataSource) {
        TbLatestChart<?,?> latestChart = createLatestChart(component, reportDataSource.getLatestChartData(), reportDataSource.getVariables());
        return latestChart.createChart(g2);
    }

    private TbLatestChart<?,?> createLatestChart(LatestChartComponent component, LatestChartData latestChartData, Map<String, Object> variables) {
        if (LATEST_BAR_CHART == component.getSubType()) {
            ReportBarChartSettings reportBarChartSettings = new ReportBarChartSettings((ReportBarChartSettings) component.getLatestChartSettings());
            return new TbBarChart(reportBarChartSettings, latestChartData, variables);
        } else if (PIE_CHART == component.getSubType()) {
            ReportPieChartSettings reportPieChartSettings = new ReportPieChartSettings((ReportPieChartSettings) component.getLatestChartSettings());
            return new TbPieChart(reportPieChartSettings, latestChartData, variables);
        } else if (DOUGHNUT_CHART == component.getSubType() || HORIZONTAL_DOUGHNUT_CHART == component.getSubType()) {
            ReportDoughnutChartSettings reportDoughnutChartSettings = new ReportDoughnutChartSettings((ReportDoughnutChartSettings) component.getLatestChartSettings());
            return new TbDoughnutChart(reportDoughnutChartSettings, latestChartData, variables);
        } else {
            throw new IllegalArgumentException("Latest chart with subType '" + component.getSubType() + "' is not supported");
        }
    }

    @Override
    public ReportComponentType getType() {
        return ReportComponentType.LATEST_CHART;
    }
}
