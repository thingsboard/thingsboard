// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.report.renderer.chart;

import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.labels.CategoryItemLabelGenerator;
import org.jfree.chart.labels.ItemLabelAnchor;
import org.jfree.chart.labels.ItemLabelPosition;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.chart.ui.TextAnchor;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.thingsboard.server.common.data.report.configuration.chart.BarSeriesSettings;
import org.thingsboard.server.common.data.report.configuration.chart.ChartFillType;
import org.thingsboard.server.common.data.report.configuration.chart.ChartLabelPosition;
import org.thingsboard.server.common.data.report.configuration.chart.ReportBarChartSettings;
import org.thingsboard.server.report.context.chart.LatestChartData;
import org.thingsboard.server.report.context.chart.LatestChartDataItem;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Paint;
import java.util.Map;

import static org.thingsboard.server.report.renderer.chart.ChartUtils.createFillPaint;
import static org.thingsboard.server.report.util.AwtFontUtils.toAwtFont;
import static org.thingsboard.server.report.util.ColorUtils.safeParseCssColor;

public class TbBarChart extends TbLatestChart<ReportBarChartSettings, CategoryPlot> implements CategoryItemLabelGenerator {

    private CategoryAxis categoryAxis;
    private TbNumberAxis valueAxis;
    private TbBarRenderer renderer;
    private DefaultCategoryDataset dataset;

    public TbBarChart(ReportBarChartSettings chartSettings, LatestChartData latestChartData, Map<String, Object> variables) {
        super(chartSettings, latestChartData, variables);
    }

    @Override
    protected CategoryPlot createPlot() {
        this.categoryAxis = new CategoryAxis(null);
        this.valueAxis = new TbNumberAxis(null, null);
        this.renderer = new TbBarRenderer();
        this.dataset = new DefaultCategoryDataset();
        return new CategoryPlot(dataset, categoryAxis, valueAxis, renderer);
    }

    @Override
    protected void setupPlot(CategoryPlot plot) {

        plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);
        plot.setAxisOffset(RectangleInsets.ZERO_INSETS);
        plot.setDomainGridlinesVisible(false);
        plot.setRangeGridlinesVisible(false);

        categoryAxis.setAxisLineVisible(false);
        categoryAxis.setTickLabelsVisible(false);
        categoryAxis.setTickMarksVisible(false);

        ValueMarker zeroMarker = new ValueMarker(
                0.0,
                safeParseCssColor("rgba(0,0,0,0.54)"),
                new BasicStroke(1.0f)
        );

        plot.addRangeMarker(zeroMarker);

        if (this.chartSettings.getAxisMin() != null) {
            valueAxis.setAxisMin(this.chartSettings.getAxisMin());
            if (this.chartSettings.getAxisMin() > 0) {
                zeroMarker.setValue(this.chartSettings.getAxisMin());
            }
        }
        if (this.chartSettings.getAxisMax() != null) {
            valueAxis.setAxisMax(this.chartSettings.getAxisMax());
        }

        valueAxis.setAxisLineVisible(false);
        valueAxis.setTickMarksVisible(false);
        valueAxis.setNumberFormatOverride(this.valueFormatter);
        valueAxis.setTickLabelFont(toAwtFont(this.chartSettings.getAxisTickLabelFont()));
        valueAxis.setTickLabelPaint(safeParseCssColor(this.chartSettings.getAxisTickLabelColor()));
        valueAxis.setAutoRangeIncludesZero(true);

        long visibleItemsCount = this.dataItems.stream().filter(LatestChartDataItem::isHasValue).count();
        if (visibleItemsCount == 0) {
            valueAxis.setVisible(false);
        }

        renderer.setShadowVisible(false);
        renderer.setDrawBarOutline(true);
        renderer.setDefaultOutlineStroke(new BasicStroke(0.0f));

        BarSeriesSettings barSettings = this.chartSettings.getBarSettings();

        if (visibleItemsCount > 0) {
            double barWidth = barSettings.getBarWidth() / 100.0;
            double spacing = (1f / visibleItemsCount) * (1f - barWidth);
            double itemMargin = spacing * (visibleItemsCount - 1);
            double axisMargin = spacing / 2f;
            renderer.setItemMargin(itemMargin);
            categoryAxis.setLowerMargin(axisMargin);
            categoryAxis.setUpperMargin(axisMargin);
        }

        for (LatestChartDataItem dataItem : this.dataItems) {
            dataset.addValue(dataItem.getValue(), dataItem.getSeriesIndex() + "", "Latest");
            Color seriesColor = safeParseCssColor(dataItem.getDataKey().getColor());
            Paint seriesPaint = seriesColor;
            if (!ChartFillType.none.equals(barSettings.getBackgroundSettings().getType())) {
                seriesPaint = createFillPaint(barSettings.getBackgroundSettings(), seriesColor);
            }
            if (!dataItem.isHasValue()) {
                renderer.setSeriesVisible(dataItem.getSeriesIndex(), false);
            }
            renderer.setSeriesPaint(dataItem.getSeriesIndex(), seriesPaint);
            if (barSettings.getShowBorder()) {
                renderer.setSeriesOutlineStroke(dataItem.getSeriesIndex(), new BasicStroke(barSettings.getBorderWidth()));
                renderer.setSeriesOutlinePaint(dataItem.getSeriesIndex(), seriesColor);
            }
            if (barSettings.getShowLabel()) {
                renderer.setSeriesItemLabelsVisible(dataItem.getSeriesIndex(), true);
                renderer.setSeriesItemLabelFont(dataItem.getSeriesIndex(), toAwtFont(barSettings.getLabelFont()));
                renderer.setSeriesItemLabelPaint(dataItem.getSeriesIndex(), safeParseCssColor(barSettings.getLabelColor()));
                renderer.setSeriesItemLabelGenerator(dataItem.getSeriesIndex(), this);
                ItemLabelPosition itemLabelPosition;
                if (ChartLabelPosition.top.equals(barSettings.getLabelPosition())) {
                    itemLabelPosition = new ItemLabelPosition(
                            ItemLabelAnchor.OUTSIDE12, TextAnchor.BOTTOM_CENTER);
                } else {
                    itemLabelPosition = new ItemLabelPosition(
                            ItemLabelAnchor.OUTSIDE6, TextAnchor.TOP_CENTER);
                }
                renderer.setSeriesPositiveItemLabelPosition(dataItem.getSeriesIndex(), itemLabelPosition);
                renderer.setSeriesNegativeItemLabelPosition(dataItem.getSeriesIndex(), itemLabelPosition);
                if (barSettings.getEnableLabelBackground()) {
                    renderer.setSeriesItemLabelsBackgroundVisible(dataItem.getSeriesIndex(), true);
                    renderer.setSeriesItemLabelsBackgroundPaint(dataItem.getSeriesIndex(), safeParseCssColor(barSettings.getLabelBackground()));
                }
            }
            renderer.setSeriesItemBorderRadius(dataItem.getSeriesIndex(), barSettings.getBorderRadius());
        }
    }


    @Override
    public String generateRowLabel(CategoryDataset dataset, int row) {
        return "";
    }

    @Override
    public String generateColumnLabel(CategoryDataset dataset, int column) {
        return "";
    }

    @Override
    public String generateLabel(CategoryDataset dataset, int row, int column) {
        return dataItems.get(row).getLabel();
    }
}
