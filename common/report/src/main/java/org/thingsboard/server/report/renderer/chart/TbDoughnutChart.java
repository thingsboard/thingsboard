// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.report.renderer.chart;

import org.jfree.chart.plot.CenterTextMode;
import org.jfree.chart.util.Rotation;
import org.jfree.data.general.DefaultPieDataset;
import org.thingsboard.server.common.data.report.configuration.chart.DoughnutLayout;
import org.thingsboard.server.common.data.report.configuration.chart.ReportDoughnutChartSettings;
import org.thingsboard.server.common.data.report.configuration.style.Font;
import org.thingsboard.server.common.data.report.configuration.style.FontStyle;
import org.thingsboard.server.common.data.report.configuration.style.FontWeight;
import org.thingsboard.server.report.context.chart.LatestChartData;
import org.thingsboard.server.report.context.chart.LatestChartDataItem;

import java.util.List;
import java.util.Map;

import static org.thingsboard.server.report.util.AwtFontUtils.toAwtFont;
import static org.thingsboard.server.report.util.ColorUtils.safeParseCssColor;

public class TbDoughnutChart extends TbLatestChart<ReportDoughnutChartSettings, TbRingPlot> {

    private DefaultPieDataset<Integer> doughnutDataset;

    public TbDoughnutChart(ReportDoughnutChartSettings settings, LatestChartData latestChartData, Map<String, Object> variables) {
        super(settings, latestChartData, variables);
    }

    @Override
    protected TbRingPlot createPlot() {
        this.doughnutDataset = new DefaultPieDataset<>();
        return new TbRingPlot(this.doughnutDataset);
    }

    @Override
    protected void setupPlot(TbRingPlot plot) {
        plot.setSegmentsPadAngle(2);
        plot.setRoundSegmentCorners(true);
        plot.setAbsoluteSectionDepth(27.0);
        if (this.chartSettings.getAutoScale()) {
            plot.setScaleBase(134.0);
        }
        plot.setSeparatorsVisible(false);
        plot.setLabelGenerator(null);
        plot.setShadowPaint(null);
        plot.setDirection(this.chartSettings.getClockwise() ? Rotation.CLOCKWISE : Rotation.ANTICLOCKWISE);
        plot.setSectionOutlinesVisible(false);
        List<LatestChartDataItem> visibleDataItems = this.dataItems.stream().filter(item -> item.isHasValue() && item.getValue() >= 0).toList();
        double total = 0;
        for (LatestChartDataItem dataItem : visibleDataItems) {
            Integer key = dataItem.getSeriesIndex();
            this.doughnutDataset.setValue(key, dataItem.getValue());
            plot.setSectionPaint(key, safeParseCssColor(dataItem.getDataKey().getColor()));
            total += dataItem.getValue();
        }
        if (visibleDataItems.isEmpty()) {
            Integer key = 0;
            this.doughnutDataset.setValue(key, 1.0);
            plot.setSectionPaint(key, safeParseCssColor("lightgray"));
        }
        for (LatestChartDataItem dataItem : visibleDataItems) {
            Integer key = dataItem.getSeriesIndex();
            if (total == 0) {
                this.doughnutDataset.setValue(key, 1);
            }
        }
        if (this.chartSettings.getLayout() == DoughnutLayout.WITH_TOTAL) {
            plot.setCenterLabel("Total");
            plot.setCenterLabelFont(toAwtFont(Font.builder().family("Roboto")
                    .size(12f)
                    .weight(FontWeight.NORMAL)
                    .style(FontStyle.NORMAL)
                    .build()));
            plot.setCenterLabelColor(safeParseCssColor("rgba(0, 0, 0, 0.38)"));
            plot.setCenterTextMode(CenterTextMode.FIXED);
            plot.setCenterText(totalText);
            plot.setCenterTextFont(toAwtFont(this.chartSettings.getTotalValueFont()));
            plot.setCenterTextColor(safeParseCssColor(this.chartSettings.getTotalValueColor()));
        } else {
            plot.setCenterTextMode(CenterTextMode.NONE);
        }
    }
}
