// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.report.renderer.chart;

import org.jfree.chart.ChartTheme;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.StandardChartTheme;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.title.TextTitle;
import org.jfree.chart.ui.HorizontalAlignment;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.chart.ui.VerticalAlignment;
import org.thingsboard.server.common.data.report.configuration.chart.LegendPosition;
import org.thingsboard.server.common.data.report.configuration.chart.ReportLatestChartSettings;
import org.thingsboard.server.report.context.chart.LatestChartData;
import org.thingsboard.server.report.context.chart.LatestChartDataItem;
import org.thingsboard.server.report.renderer.chart.legend.TbLatestChartLegendItem;
import org.thingsboard.server.report.renderer.chart.legend.TbLatestLegendTitle;
import org.thingsboard.server.report.util.ColorUtils;
import org.thingsboard.server.report.util.ThymeleafUtil;

import java.awt.Font;
import java.awt.Graphics2D;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static org.thingsboard.server.report.renderer.chart.ChartUtils.createValueFormatter;
import static org.thingsboard.server.report.util.AwtFontUtils.toAwtFont;
import static org.thingsboard.server.report.util.ColorUtils.safeParseCssColor;

public abstract class TbLatestChart<S extends ReportLatestChartSettings, P extends Plot> {

    static ChartTheme currentChartTheme = new StandardChartTheme("TbChartTheme");

    protected final S chartSettings;
    protected final LatestChartData latestChartData;
    private final Map<String, Object> variables;

    protected final List<LatestChartDataItem> dataItems;

    protected final NumberFormat valueFormatter;

    protected List<TbLatestChartLegendItem> legendItems;

    protected double total = 0;
    protected String totalText = "N/A";

    protected JFreeChart chart;

    public TbLatestChart(S chartSettings, LatestChartData latestChartData, Map<String, Object> variables) {
        this.chartSettings = chartSettings;
        this.latestChartData = latestChartData;
        this.variables = variables;

        Comparator<LatestChartDataItem> comparator = chartSettings.getSortSeries() ? Comparator.comparing(LatestChartDataItem::getLabel, String.CASE_INSENSITIVE_ORDER)
                : Comparator.comparing(LatestChartDataItem::getIndex);
        this.dataItems = latestChartData.getChartData().stream().flatMap(ds ->
                ds.getItems().stream()).sorted(comparator).toList();

        for (int i=0; i<dataItems.size(); i++) {
            this.dataItems.get(i).setSeriesIndex(i);
        }

        int decimals = chartSettings.getDecimals() != null ? chartSettings.getDecimals() : 2;
        String units = chartSettings.getUnits();
        this.valueFormatter = createValueFormatter(decimals, units);

        List<LatestChartDataItem> dataItemsWithValue = this.dataItems.stream().filter(LatestChartDataItem::isHasValue).toList();

        boolean hasTotalValue = !dataItemsWithValue.isEmpty();

        for (LatestChartDataItem item : dataItemsWithValue) {
            this.total += item.getValue();
        }
        if (hasTotalValue) {
            this.totalText = valueFormatter.format(this.total);
        }

        if (chartSettings.getShowLegend()) {
            this.legendItems = this.dataItems.stream().map(item -> {
                TbLatestChartLegendItem legendItem = new TbLatestChartLegendItem();
                legendItem.setLabel(item.getLabel());
                legendItem.setColor(item.getDataKey().getColor());
                if (item.isHasValue()) {
                    legendItem.setHasValue(true);
                    legendItem.setValue(this.valueFormatter.format(item.getValue()));
                } else {
                    legendItem.setHasValue(false);
                    legendItem.setValue("--");
                }
                return legendItem;
            }).toList();
            if (!chartSettings.getShowTotal() && chartSettings.getLegendShowTotal()) {
                TbLatestChartLegendItem legendItem = new TbLatestChartLegendItem();
                legendItem.setLabel("Total");
                legendItem.setHasValue(hasTotalValue);
                if (hasTotalValue) {
                    legendItem.setValue(this.totalText);
                } else {
                    legendItem.setValue("--");
                }
                legendItem.setTotal(true);
                legendItem.setColor("rgba(0, 0, 0, 0.06)");
                this.legendItems = new ArrayList<>(legendItems);
                this.legendItems.add(legendItem);
            }
        }
    }

    public JFreeChart createChart(Graphics2D g2) {
        P plot = this.createPlot();

        this.chart = new JFreeChart(
                null,
                null,
                plot,
                false);
        currentChartTheme.apply(chart);

        plot.setBackgroundPaint(null);
        plot.setOutlinePaint(null);
        plot.setInsets(new RectangleInsets(2.0, 0.0, 2.0, 0.0));
        chart.setBackgroundPaint(ColorUtils.TRANSPARENT);

        this.setupPlot(plot);

        if (chartSettings.getShowTitle()) {
            Font titleFont = toAwtFont(chartSettings.getTitleFont());
            String titleText = ThymeleafUtil.renderFromTextString(chartSettings.getTitle(), this.variables);
            TextTitle title = new TextTitle(titleText, titleFont);
            title.setPaint(safeParseCssColor(chartSettings.getTitleColor()));
            HorizontalAlignment alignment = HorizontalAlignment.CENTER;
            switch (chartSettings.getTitleAlignment()) {
                case RIGHT -> alignment = HorizontalAlignment.RIGHT;
                case LEFT -> alignment = HorizontalAlignment.LEFT;
            }
            title.setHorizontalAlignment(alignment);
            if (!chartSettings.getShowLegend() || chartSettings.getLegendPosition() != LegendPosition.top) {
                title.setPadding(new RectangleInsets(1.0, 1.0, 8.0, 1.0));
            }
            chart.setTitle(title);
        }

        if (chartSettings.getShowLegend()) {
            this.setupLegend();
        }

        return chart;
    }

    protected abstract P createPlot();

    protected abstract void setupPlot(P plot);

    private void setupLegend() {
        TbLatestLegendTitle legend = new TbLatestLegendTitle(this.legendItems);
        legend.setLegendLabelFont(toAwtFont(chartSettings.getLegendLabelFont()));
        legend.setLegendLabelPaint(safeParseCssColor(chartSettings.getLegendLabelColor()));
        legend.setLegendValueFont(toAwtFont(chartSettings.getLegendValueFont()));
        legend.setLegendValuePaint(safeParseCssColor(chartSettings.getLegendValueColor()));

        RectangleEdge position = RectangleEdge.TOP;
        LegendPosition legendPosition = chartSettings.getLegendPosition();
        switch (legendPosition) {
            case bottom -> position = RectangleEdge.BOTTOM;
            case left -> position = RectangleEdge.LEFT;
            case right -> position = RectangleEdge.RIGHT;
        }

        legend.setPosition(position);

        if (RectangleEdge.isLeftOrRight(position)) {
            legend.setVerticalAlignment(VerticalAlignment.CENTER);
            if (position == RectangleEdge.LEFT) {
                legend.setPadding(new RectangleInsets(0.0, 0.0, 0.0, 24.0));
            } else {
                legend.setPadding(new RectangleInsets(0.0, 24.0, 0.0, 0.0));
            }
        } else {
            if (position == RectangleEdge.TOP) {
                legend.setPadding(new RectangleInsets(8.0, 0.0, 16.0, 0.0));
            } else {
                legend.setPadding(new RectangleInsets(16.0, 0.0, 8.0, 0.0));
            }
        }
        chart.addSubtitle(legend);
        legend.addChangeListener(chart);
    }
}
