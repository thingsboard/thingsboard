// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.report.renderer.chart;

import org.jfree.chart.ChartTheme;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.LegendItem;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.StandardChartTheme;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.ItemLabelAnchor;
import org.jfree.chart.labels.ItemLabelClip;
import org.jfree.chart.labels.ItemLabelPosition;
import org.jfree.chart.labels.XYItemLabelGenerator;
import org.jfree.chart.labels.XYSeriesLabelGenerator;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.SeriesRenderingOrder;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.title.TextTitle;
import org.jfree.chart.title.Title;
import org.jfree.chart.ui.HorizontalAlignment;
import org.jfree.chart.ui.Layer;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.chart.ui.TextAnchor;
import org.jfree.chart.ui.VerticalAlignment;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.SimpleTimePeriod;
import org.jfree.data.time.TimePeriodValues;
import org.jfree.data.time.TimePeriodValuesCollection;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.time.TimeTableXYDataset;
import org.jfree.data.xy.XYDataset;
import org.thingsboard.server.common.data.report.configuration.chart.AxisPosition;
import org.thingsboard.server.common.data.report.configuration.chart.BarSeriesSettings;
import org.thingsboard.server.common.data.report.configuration.chart.BarWithLabelsSeriesSettings;
import org.thingsboard.server.common.data.report.configuration.chart.ChartFillType;
import org.thingsboard.server.common.data.report.configuration.chart.ChartLabelPosition;
import org.thingsboard.server.common.data.report.configuration.chart.ChartShape;
import org.thingsboard.server.common.data.report.configuration.chart.LegendConfig;
import org.thingsboard.server.common.data.report.configuration.chart.LegendPosition;
import org.thingsboard.server.common.data.report.configuration.chart.LineSeriesSettings;
import org.thingsboard.server.common.data.report.configuration.chart.ReportTimeSeriesChartSettings;
import org.thingsboard.server.common.data.report.configuration.chart.TimeSeriesChartBarWidth;
import org.thingsboard.server.common.data.report.configuration.chart.TimeSeriesChartNoAggregationBarWidthSettings;
import org.thingsboard.server.common.data.report.configuration.chart.TimeSeriesChartNoAggregationBarWidthStrategy;
import org.thingsboard.server.common.data.report.configuration.chart.TimeSeriesChartSeriesType;
import org.thingsboard.server.common.data.report.configuration.chart.TimeSeriesChartThreshold;
import org.thingsboard.server.common.data.report.configuration.chart.TimeSeriesChartYAxisSettings;
import org.thingsboard.server.report.context.chart.TsChartData;
import org.thingsboard.server.report.context.chart.TsChartDataSource;
import org.thingsboard.server.report.context.chart.TsChartRangeItem;
import org.thingsboard.server.report.context.chart.TsChartSeriesData;
import org.thingsboard.server.report.context.chart.TsChartSeriesEntry;
import org.thingsboard.server.report.context.chart.TsChartThresholdItem;
import org.thingsboard.server.report.context.chart.YAxisScale;
import org.thingsboard.server.report.renderer.chart.layout.TbColumnArrangement;
import org.thingsboard.server.report.renderer.chart.layout.TbFlowArrangement;
import org.thingsboard.server.report.renderer.chart.legend.TbLegendTitle;
import org.thingsboard.server.report.renderer.chart.legend.TbLegendValues;
import org.thingsboard.server.report.renderer.chart.legend.TbLegendValuesRequest;
import org.thingsboard.server.report.renderer.chart.legend.TbRangeLegendTitle;
import org.thingsboard.server.report.renderer.chart.legend.TbSeriesLegendValuesGenerator;
import org.thingsboard.server.report.renderer.chart.legend.TbTableLegendTitle;
import org.thingsboard.server.report.util.ColorUtils;
import org.thingsboard.server.report.util.ThymeleafUtil;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static org.thingsboard.server.report.renderer.chart.ChartUtils.adjustAxisMargins;
import static org.thingsboard.server.report.renderer.chart.ChartUtils.calculateBarTimePeriod;
import static org.thingsboard.server.report.renderer.chart.ChartUtils.createFillPaint;
import static org.thingsboard.server.report.renderer.chart.ChartUtils.createLineStroke;
import static org.thingsboard.server.report.renderer.chart.ChartUtils.createSeriesShape;
import static org.thingsboard.server.report.renderer.chart.ChartUtils.createThresholdMarker;
import static org.thingsboard.server.report.renderer.chart.ChartUtils.createValueFormatter;
import static org.thingsboard.server.report.renderer.chart.ChartUtils.createXAxis;
import static org.thingsboard.server.report.renderer.chart.ChartUtils.createYAxis;
import static org.thingsboard.server.report.renderer.chart.ChartUtils.datasetGroupsFromSeries;
import static org.thingsboard.server.report.renderer.chart.ChartUtils.getSeriesSettings;
import static org.thingsboard.server.report.util.AwtFontUtils.toAwtFont;
import static org.thingsboard.server.report.util.ColorUtils.safeParseCssColor;

public class TbTimeSeriesChart implements XYSeriesLabelGenerator, TbSeriesLegendValuesGenerator {

    static ChartTheme currentChartTheme = new StandardChartTheme("TbChartTheme");

    private final ReportTimeSeriesChartSettings chartSettings;
    private final TsChartData chartData;
    private final Map<String, Object> variables;

    private final TbStateValueConverter stateValueConverter;
    private final boolean stackMode;

    private final TbVisualMap visualMap;

    private final String units;
    private final Integer decimals;

    private TbTimeseriesPlot plot;
    private JFreeChart chart;

    private List<DateAxis> xAxisList;

    private List<NumberAxis> yAxisList;
    private Map<String, Integer> yAxisIndexMap;
    private Map<Integer, Boolean> yAxisHasDataMap;

    private List<TsChartSeriesData> seriesList;
    private List<TbDatasetKey> datasetKeys;


    public TbTimeSeriesChart(ReportTimeSeriesChartSettings chartSettings, TsChartData chartData,
                             Map<String, Object> variables, TbVisualMap visualMap, String units, Integer decimals) {
        this.chartSettings = chartSettings;
        this.chartData = chartData;
        this.variables = variables;

        this.visualMap = visualMap;

        if (!this.chartSettings.getStates().isEmpty()) {
            this.stateValueConverter = new TbStateValueConverter(this.chartSettings.getStates());
        } else {
            this.stateValueConverter = null;
        }

        this.stackMode = !this.chartData.isComparisonEnabled() && this.chartSettings.getStack();
        this.units = units;
        this.decimals = decimals;
    }

    public JFreeChart createChart(Graphics2D g2) {

        this.plot = new TbTimeseriesPlot();

        this.chart = new JFreeChart(
                null,
                null,
                plot,
                false);
        currentChartTheme.apply(chart);

        plot.setVisualMap(this.visualMap);
        plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);
        plot.setSeriesRenderingOrder(SeriesRenderingOrder.FORWARD);
        plot.setAxisOffset(RectangleInsets.ZERO_INSETS);
        plot.setInsets(new RectangleInsets(2.0, 0.0, 2.0, 0.0));
        chart.setBackgroundPaint(ColorUtils.TRANSPARENT);

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
            if (!chartSettings.getShowLegend() || chartSettings.getLegendConfig().getPosition() != LegendPosition.top) {
                title.setPadding(new RectangleInsets(1.0, 1.0, 8.0, 1.0));
            }
            chart.setTitle(title);
        }

        this.setupGrid();
        this.setupXAxes();
        this.setupYAxes();
        this.setupData();
        this.postConfigureXAxes();
        this.postConfigureYAxes();
        this.setupThresholds();
        this.setupLegend();

        return chart;
    }

    private void setupGrid() {
        if (chartSettings.getGrid().getShow()) {
            String gridBackgroundColor = chartSettings.getGrid().getBackgroundColor();
            plot.setBackgroundPaint(safeParseCssColor(gridBackgroundColor, null));
            plot.setOutlineStroke(new BasicStroke(chartSettings.getGrid().getBorderWidth()));
            plot.setOutlinePaint(safeParseCssColor(chartSettings.getGrid().getBorderColor()));
        } else {
            plot.setBackgroundPaint(null);
            plot.setOutlinePaint(null);
        }
        plot.setDomainGridlinesVisible(false);
        plot.setRangeGridlinesVisible(false);
    }

    private void setupXAxes() {
        this.xAxisList = new ArrayList<>();
        this.xAxisList.add(createXAxis(plot, chartSettings.getXAxis(), chartData.getTimeRange(), chartData.getTimeZone(), 0));
        if (this.chartData.isComparisonEnabled()) {
            this.xAxisList.add(createXAxis(plot, chartSettings.getComparisonXAxis(), chartData.getComparisonTimeRange(), chartData.getTimeZone(), 1));
        }
    }

    private void setupYAxes() {
        this.yAxisList = new ArrayList<>();
        this.yAxisIndexMap = new HashMap<>();
        this.yAxisHasDataMap = new HashMap<>();

        List<TimeSeriesChartYAxisSettings> yAxisSettingsList = chartSettings.getYAxes().values().stream().sorted(Comparator.comparingInt(TimeSeriesChartYAxisSettings::getOrder)).toList();

        List<TbStateTick> stateTicks = this.stateValueConverter != null ? this.stateValueConverter.getStateTicks() : null;

        for (int index = 0; index < yAxisSettingsList.size(); index++) {
            TimeSeriesChartYAxisSettings yAxisSettings = yAxisSettingsList.get(index);
            YAxisScale yAxisScale = chartData.getYAxisScales().get(yAxisSettings.getId());
            this.yAxisList.add(createYAxis(plot, yAxisSettings, stateTicks, this.units, this.decimals, index, yAxisScale));
            this.yAxisIndexMap.put(yAxisSettingsList.get(index).getId(), index);
            this.yAxisHasDataMap.put(index, false);
        }
    }

    private void setupData() {

        List<TsChartSeriesData> allSeries = new ArrayList<>();
        for (TsChartDataSource dataSource : chartData.getChartData()) {
            allSeries.addAll(dataSource.getData());
        }

        this.seriesList = allSeries.stream().sorted(Comparator.comparingInt(TsChartSeriesData::getIndex)).toList();

        Map<TbDatasetKey, List<TsChartSeriesData>> groupedSeries = datasetGroupsFromSeries(this.seriesList);

        this.datasetKeys = new ArrayList<>(groupedSeries.keySet());
        if (groupedSeries.isEmpty()) {
            this.plot.setRenderer(0, new TbXYLineAndShapeRenderer());
        } else {
            groupedSeries.forEach(this::setupDataset);
        }
    }

    private void setupThresholds() {
        List<TsChartThresholdItem> thresholdItems = this.chartData.getThresholdItems();
        thresholdItems.forEach(thresholdItem -> {
            TimeSeriesChartThreshold threshold = thresholdItem.getSettings();
            String yAxisId = threshold.getYAxisId();
            int datasetIndex = -1;
            int axisIndex = this.yAxisIndexMap.get(yAxisId);
            if (axisIndex >= 0 && this.yAxisHasDataMap.get(axisIndex)) {
                Optional<TbDatasetKey> foundKey = this.datasetKeys.stream().filter(key -> key.getYAxisId().equals(yAxisId)).findFirst();
                if (foundKey.isPresent()) {
                    datasetIndex = foundKey.get().getDatasetIndex();
                }
            }
            if (datasetIndex >= 0) {
                TbThresholdMarker marker = createThresholdMarker(thresholdItem, this.units, this.decimals);
                plot.addRangeMarker(datasetIndex, marker, Layer.FOREGROUND);
            }
        });
    }

    private void postConfigureXAxes() {
        if (this.xAxisList.size() > 1) {
            if (chartSettings.getXAxis().getPosition().equals(AxisPosition.top) && chartSettings.getComparisonXAxis().getPosition().equals(AxisPosition.top)) {
                adjustAxisMargins(xAxisList.get(0), 0.0, 0.0, 4.0, 0.0);
            } else if (chartSettings.getXAxis().getPosition().equals(AxisPosition.bottom) && chartSettings.getComparisonXAxis().getPosition().equals(AxisPosition.bottom)) {
                adjustAxisMargins(xAxisList.get(0), 4.0, 0.0, 0.0, 0.0);
            }
        }
    }

    private void postConfigureYAxes() {

        // Update axes visibility

        for (Map.Entry<Integer, Boolean> yAxisHasDataEntry : this.yAxisHasDataMap.entrySet()) {
            if (!yAxisHasDataEntry.getValue()) {
                this.yAxisList.get(yAxisHasDataEntry.getKey()).setVisible(false);
            }
        }

        // Adjust scale for bar series

        for (Map.Entry<String, Integer> yAxisEntry : this.yAxisIndexMap.entrySet()) {
            boolean includeZeros = this.datasetKeys.stream().anyMatch(entry ->
                    entry.getYAxisId().equals(yAxisEntry.getKey()) && TimeSeriesChartSeriesType.bar.equals(entry.getSeriesType()));
            this.yAxisList.get(yAxisEntry.getValue()).setAutoRangeIncludesZero(includeZeros);
        }

        // Adjust axes horizontal position

        List<TimeSeriesChartYAxisSettings> yAxisSettingsList = chartSettings.getYAxes().values().stream().sorted(Comparator.comparingInt(TimeSeriesChartYAxisSettings::getOrder)).toList();
        List<TimeSeriesChartYAxisSettings> leftAxes = yAxisSettingsList.stream().filter(
                axis -> {
                    if (AxisPosition.left.equals(axis.getPosition())) {
                        int index = yAxisIndexMap.get(axis.getId());
                        return this.yAxisHasDataMap.get(index);
                    } else {
                        return false;
                    }
                }
        ).toList();

        if (leftAxes.size() > 1) {
            for (int i = 0; i < leftAxes.size()-1; i++) {
                TimeSeriesChartYAxisSettings leftAxis = leftAxes.get(i);
                int index = yAxisSettingsList.indexOf(leftAxis);
                NumberAxis axis = this.yAxisList.get(index);
                adjustAxisMargins(axis, 0.0, 4.0, 0.0, 0.0);
            }
        }

        List<TimeSeriesChartYAxisSettings> rightAxes = yAxisSettingsList.stream().filter(
                axis -> {
                    if (AxisPosition.right.equals(axis.getPosition())) {
                        int index = yAxisIndexMap.get(axis.getId());
                        return this.yAxisHasDataMap.get(index);
                    } else {
                        return false;
                    }
                }
        ).toList();

        if (rightAxes.size() > 1) {
            for (int i = 0; i < rightAxes.size()-1; i++) {
                TimeSeriesChartYAxisSettings rightAxis = rightAxes.get(i);
                int index = yAxisSettingsList.indexOf(rightAxis);
                NumberAxis axis = this.yAxisList.get(index);
                adjustAxisMargins(axis, 0.0, 0.0, 0.0, 4.0);
            }
        }
    }

    private void setupLegend() {
        if (chartSettings.getShowLegend()) {

            LegendConfig legendConfig = chartSettings.getLegendConfig();
            Title legendTitle = null;

            double hPadding = 8.0;

            if (this.visualMap != null) {
                List<TsChartRangeItem> visibleRangeItems = this.chartData.getRangeItems().stream().filter(TsChartRangeItem::isVisible).toList();
                if (!visibleRangeItems.isEmpty()) {
                    TbRangeLegendTitle legend = new TbRangeLegendTitle(visibleRangeItems);
                    legend.setLegendLabelFont(toAwtFont(chartSettings.getLegendLabelFont()));
                    legend.setLegendLabelPaint(safeParseCssColor(chartSettings.getLegendLabelColor()));
                    legendTitle = legend;
                    hPadding = 16.0;
                }
            } else if (legendConfig.isSimpleLegend()) {
                TbFlowArrangement hLayout = new TbFlowArrangement(HorizontalAlignment.CENTER, VerticalAlignment.CENTER, 16.0, 8.0);
                hLayout.setMaxRelativeHeight(0.35);
                TbColumnArrangement vLayout = new TbColumnArrangement(HorizontalAlignment.LEFT, VerticalAlignment.TOP, 16.0, 8.0);
                vLayout.setMaxRelativeWidth(0.35);
                LegendTitle legend = new TbLegendTitle(plot, hLayout, vLayout);
                legend.setBackgroundPaint(ColorUtils.TRANSPARENT);

                legend.setItemLabelPadding(new RectangleInsets(0.0, 4.0, 0.0, 0.0));
                legend.setItemFont(toAwtFont(chartSettings.getLegendLabelFont()));
                legend.setItemPaint(safeParseCssColor(chartSettings.getLegendLabelColor()));

                boolean sortAlphabetically = legendConfig.getSortDataKeys();
                LegendItemCollection legendItems = plot.getLegendItems();
                List<LegendItem> items = new ArrayList<>();

                for (int i = 0; i < legendItems.getItemCount(); i++) {
                    items.add(legendItems.get(i));
                }

                if (sortAlphabetically) {
                    items.sort(Comparator.comparing(LegendItem::getLabel, String.CASE_INSENSITIVE_ORDER));
                } else {
                    items.sort((item1, item2) -> {
                        int dataIndex1 = findSeriesIndex(item1.getDatasetIndex(), item1.getSeriesIndex());
                        int dataIndex2 = findSeriesIndex(item2.getDatasetIndex(), item2.getSeriesIndex());
                        return dataIndex1 - dataIndex2;
                    });
                }
                LegendItemCollection sortedCollection = new LegendItemCollection();
                for (LegendItem item : items) {
                    sortedCollection.add(item);
                }
                plot.setFixedLegendItems(sortedCollection);

                legendTitle = legend;

            } else {
                TbTableLegendTitle legend = new TbTableLegendTitle(plot, legendConfig);
                legend.setMaxRelativeWidth(0.35);
                legend.setMaxRelativeHeight(0.35);

                legend.setItemLabelPadding(new RectangleInsets(0.0, 4.0, 0.0, 0.0));
                legend.setItemFont(toAwtFont(chartSettings.getLegendLabelFont()));
                legend.setItemPaint(safeParseCssColor(chartSettings.getLegendLabelColor()));

                legend.setLegendColumnTitleFont(toAwtFont(chartSettings.getLegendColumnTitleFont()));
                legend.setLegendColumnTitlePaint(safeParseCssColor(chartSettings.getLegendColumnTitleColor()));

                legend.setLegendValueFont(toAwtFont(chartSettings.getLegendValueFont()));
                legend.setLegendValuePaint(safeParseCssColor(chartSettings.getLegendValueColor()));

                boolean sortAlphabetically = legendConfig.getSortDataKeys();
                if (sortAlphabetically) {
                    legend.setLegendItemComparator(Comparator.comparing(item -> item.getLegendItem().getLabel(), String.CASE_INSENSITIVE_ORDER));
                } else {
                    legend.setLegendItemComparator((item1, item2) -> {
                        int dataIndex1 = findSeriesIndex(item1.getLegendItem().getDatasetIndex(), item1.getLegendItem().getSeriesIndex());
                        int dataIndex2 = findSeriesIndex(item2.getLegendItem().getDatasetIndex(), item2.getLegendItem().getSeriesIndex());
                        return dataIndex1 - dataIndex2;
                    });
                }
                legendTitle = legend;
            }

            if (legendTitle != null) {
                RectangleEdge position = RectangleEdge.TOP;
                LegendPosition legendPosition = legendConfig.getPosition();
                switch (legendPosition) {
                    case bottom -> position = RectangleEdge.BOTTOM;
                    case left -> position = RectangleEdge.LEFT;
                    case right -> position = RectangleEdge.RIGHT;
                }
                legendTitle.setPosition(position);
                if (RectangleEdge.isLeftOrRight(position)) {
                    legendTitle.setVerticalAlignment(VerticalAlignment.TOP);
                    if (position == RectangleEdge.LEFT) {
                        legendTitle.setPadding(new RectangleInsets(0.0, 0.0, 0.0, hPadding));
                    } else {
                        legendTitle.setPadding(new RectangleInsets(0.0, hPadding, 0.0, 0.0));
                    }
                } else {
                    legendTitle.setPadding(new RectangleInsets(8.0, 0.0, 8.0, 0.0));
                }

                chart.addSubtitle(legendTitle);
                legendTitle.addChangeListener(chart);
            }
        }
    }

    private void setupDataset(TbDatasetKey datasetKey, List<TsChartSeriesData> seriesList) {
        createDataset(datasetKey, seriesList);
        createDatasetRenderer(datasetKey, seriesList);
    }

    private void createDataset(TbDatasetKey datasetKey, List<TsChartSeriesData> seriesList) {
        int xAxisIndex = datasetKey.isComparison() ? 1 : 0;
        int yAxisIndex = this.yAxisIndexMap.get(datasetKey.getYAxisId());
        if (datasetKey.getSeriesType() == TimeSeriesChartSeriesType.bar) {
            List<TsChartSeriesData> barsList = this.seriesList.stream().
                    filter(barSeries -> !barSeries.isEmpty() && getSeriesSettings(barSeries).getSeriesType() == TimeSeriesChartSeriesType.bar).toList();
            TimeSeriesChartNoAggregationBarWidthSettings noAggregationBarWidthSettings = chartSettings.getNoAggregationBarWidthSettings();
            TimeSeriesChartBarWidth targetBarWidth = TimeSeriesChartNoAggregationBarWidthStrategy.group.equals(noAggregationBarWidthSettings.getStrategy()) ?
                    noAggregationBarWidthSettings.getGroupWidth() : noAggregationBarWidthSettings.getBarWidth();
            TimeseriesBarRenderCtx barRenderCtx = TimeseriesBarRenderCtx.builder()
                    .barGap(chartSettings.getBarWidthSettings().getBarGap())
                    .intervalGap(chartSettings.getBarWidthSettings().getIntervalGap())
                    .timeWindow(chartData.getTimeRange().endTs - chartData.getTimeRange().startTs)
                    .noAggregation(chartData.isNoAggregation())
                    .noAggregationBarWidthStrategy(noAggregationBarWidthSettings.getStrategy())
                    .noAggregationWidthRelative(targetBarWidth.getRelative())
                    .noAggregationWidth(targetBarWidth.getRelative() ? targetBarWidth.getRelativeWidth() : targetBarWidth.getAbsoluteWidth())
                    .build();
            this.createBarsDataset(datasetKey.getDatasetIndex(), xAxisIndex, yAxisIndex, seriesList, barsList, barRenderCtx);
        } else {
            this.createLinesDataset(datasetKey.getDatasetIndex(), xAxisIndex, yAxisIndex, seriesList);
        }
    }

    private void createDatasetRenderer(TbDatasetKey datasetKey, List<TsChartSeriesData> seriesList) {
        if (datasetKey.getSeriesType() == TimeSeriesChartSeriesType.bar) {
            this.createBarsRenderer(datasetKey, seriesList);
        } else {
            this.createLinesRenderer(datasetKey, seriesList);
        }
    }

    private void createBarsDataset(int datasetIndex, int xAxisIndex, int yAxisIndex,
                                   List<TsChartSeriesData> seriesList,
                                   List<TsChartSeriesData> allBarsList,
                                   TimeseriesBarRenderCtx barRenderCtx) {
        XYDataset dataset;
        boolean hasData = false;
        if (this.stackMode) {
            List<Integer> barDatasets = allBarsList.stream().map(TsChartSeriesData::getDatasetIndex).distinct().sorted().toList();
            int barsCount = barDatasets.size();
            int barIndex = barDatasets.indexOf(datasetIndex);
            TimeTableXYDataset tableDataset = new TimeTableXYDataset(chartData.getTimeZone());
            for (TsChartSeriesData series : seriesList) {
                String seriesName = datasetIndex + "_" + series.getSeriesIndex();
                for (TsChartSeriesEntry tsValue : series.getData()) {
                    Double converted = this.convertValue(tsValue);
                    if (converted != null) {
                        SimpleTimePeriod timePeriod = calculateBarTimePeriod(tsValue, barRenderCtx, barsCount, barIndex);
                        tableDataset.add(timePeriod, converted, seriesName);
                        hasData = true;
                    }
                }
            }

            dataset = tableDataset;
        } else {
            int barsCount = allBarsList.size();
            TimePeriodValuesCollection tpvDataset = new TimePeriodValuesCollection();
            for (TsChartSeriesData series : seriesList) {
                int barIndex = allBarsList.indexOf(series);
                TimePeriodValues timePeriods = new TimePeriodValues(datasetIndex + "_" + series.getSeriesIndex());
                for (TsChartSeriesEntry tsValue : series.getData()) {
                    Double converted = this.convertValue(tsValue);
                    if (converted != null) {
                        SimpleTimePeriod timePeriod = calculateBarTimePeriod(tsValue, barRenderCtx, barsCount, barIndex);
                        timePeriods.add(timePeriod, converted);
                        hasData = true;
                    }
                }
                tpvDataset.addSeries(timePeriods);
            }
            dataset = tpvDataset;
        }
        this.yAxisHasDataMap.put(yAxisIndex, hasData);
        plot.setDataset(datasetIndex, dataset);
        plot.mapDatasetToDomainAxis(datasetIndex, xAxisIndex);
        plot.mapDatasetToRangeAxis(datasetIndex, yAxisIndex);
    }

    private void createLinesDataset(int datasetIndex, int xAxisIndex, int yAxisIndex, List<TsChartSeriesData> seriesList) {
        Locale locale = Locale.getDefault();
        XYDataset dataset;
        boolean hasData = false;
        if (this.stackMode) {
            TimeTableXYDataset tableDataset = new TimeTableXYDataset(chartData.getTimeZone());
            for (TsChartSeriesData series : seriesList) {
                String seriesName = datasetIndex + "_" + series.getSeriesIndex();
                for (TsChartSeriesEntry tsValue : series.getData()) {
                    Double converted = this.convertValue(tsValue);
                    if (converted != null) {
                        Millisecond millisecond = new Millisecond(new Date(tsValue.getTs()), chartData.getTimeZone(), locale);
                        tableDataset.add(millisecond, converted, seriesName);
                        hasData = true;
                    }
                }
            }
            dataset = tableDataset;
        } else {
            TimeSeriesCollection tsDataset = new TimeSeriesCollection(chartData.getTimeZone());
            for (TsChartSeriesData series : seriesList) {
                TimeSeries timeSeries = new TimeSeries(datasetIndex + "_" + series.getSeriesIndex());
                for (TsChartSeriesEntry tsValue : series.getData()) {
                    Double converted = this.convertValue(tsValue);
                    if (converted != null) {
                        Millisecond millisecond = new Millisecond(new Date(tsValue.getTs()), chartData.getTimeZone(), locale);
                        timeSeries.add(millisecond, converted);
                        hasData = true;
                    }
                }
                tsDataset.addSeries(timeSeries);
            }
            dataset = tsDataset;
        }
        this.yAxisHasDataMap.put(yAxisIndex, hasData);
        plot.setDataset(datasetIndex, dataset);
        plot.mapDatasetToDomainAxis(datasetIndex, xAxisIndex);
        plot.mapDatasetToRangeAxis(datasetIndex, yAxisIndex);
    }

    private Double convertValue(TsChartSeriesEntry tsValue) {
        if (this.stateValueConverter != null) {
            return this.stateValueConverter.convertValue(tsValue.getValue());
        } else {
            return tsValue.getDoubleValue();
        }
    }

    private void createBarsRenderer(TbDatasetKey datasetKey, List<TsChartSeriesData> seriesList) {
        TbXYBarRenderer renderer = this.createBarRenderer();
        for (TsChartSeriesData series : seriesList) {
            BarSeriesSettings barSettings = getSeriesSettings(series).getBarSettings();
            Color seriesColor = safeParseCssColor(series.getDataKey().getColor());
            Paint seriesPaint = seriesColor;
            if (!ChartFillType.none.equals(barSettings.getBackgroundSettings().getType())) {
                seriesPaint = createFillPaint(barSettings.getBackgroundSettings(), seriesColor);
            }
            renderer.setSeriesPaint(series.getSeriesIndex(), seriesPaint);
            if (barSettings.getShowBorder()) {
                renderer.setSeriesOutlineStroke(series.getSeriesIndex(), new BasicStroke(barSettings.getBorderWidth()));
                renderer.setSeriesOutlinePaint(series.getSeriesIndex(), seriesColor);
            }
            renderer.setSeriesItemBorderRadius(series.getSeriesIndex(), barSettings.getBorderRadius());

            if (barSettings instanceof BarWithLabelsSeriesSettings barWithLabelsSettings) {
                if (barWithLabelsSettings.getShowLabel() || barWithLabelsSettings.getShowSeriesLabel()) {
                    renderer.setItemLabelInsets(RectangleInsets.ZERO_INSETS);
                    renderer.setSeriesPositiveItemLabelPosition(series.getSeriesIndex(), new ItemLabelPosition(
                            ItemLabelAnchor.INSIDE6, TextAnchor.CENTER_LEFT, TextAnchor.CENTER_LEFT, -Math.PI / 2, ItemLabelClip.NONE));
                    renderer.setSeriesNegativeItemLabelPosition(series.getSeriesIndex(), new ItemLabelPosition(
                            ItemLabelAnchor.INSIDE6, TextAnchor.CENTER_LEFT, TextAnchor.CENTER_LEFT, -Math.PI / 2, ItemLabelClip.NONE));
                    if (barWithLabelsSettings.getShowLabel()) {
                        renderer.setSeriesItemLabelsVisible(series.getSeriesIndex(), true);
                        renderer.setSeriesItemLabelFont(series.getSeriesIndex(), toAwtFont(barWithLabelsSettings.getLabelFont()));
                        renderer.setSeriesItemLabelPaint(series.getSeriesIndex(), safeParseCssColor(barWithLabelsSettings.getLabelColor()));
                        XYItemLabelGenerator labelGenerator = new TbXYItemLabelGenerator(series.getDataKey().getDecimals(), series.getDataKey().getUnits(),
                                this.stateValueConverter);
                        renderer.setSeriesItemLabelGenerator(series.getSeriesIndex(), labelGenerator);
                    }
                    if (barWithLabelsSettings.getShowSeriesLabel()) {
                        renderer.setSeriesLabelsVisible(series.getSeriesIndex(), true);
                        renderer.setSeriesLabelFont(series.getSeriesIndex(), toAwtFont(barWithLabelsSettings.getSeriesLabelFont()));
                        renderer.setSeriesLabelPaint(series.getSeriesIndex(), safeParseCssColor(barWithLabelsSettings.getSeriesLabelColor()));
                        renderer.setSeriesLabelGenerator(series.getSeriesIndex(),this);
                    }
                }
            } else {
                if (barSettings.getShowLabel()) {
                    renderer.setSeriesItemLabelsVisible(series.getSeriesIndex(), true);
                    renderer.setSeriesItemLabelFont(series.getSeriesIndex(), toAwtFont(barSettings.getLabelFont()));
                    renderer.setSeriesItemLabelPaint(series.getSeriesIndex(), safeParseCssColor(barSettings.getLabelColor()));
                    XYItemLabelGenerator labelGenerator = new TbXYItemLabelGenerator(series.getDataKey().getDecimals(), series.getDataKey().getUnits(),
                            this.stateValueConverter);
                    renderer.setSeriesItemLabelGenerator(series.getSeriesIndex(), labelGenerator);
                    ItemLabelPosition positiveItemLabelPosition;
                    ItemLabelPosition negativeItemLabelPosition;
                    if (ChartLabelPosition.top.equals(barSettings.getLabelPosition())) {
                        positiveItemLabelPosition = new ItemLabelPosition(
                                ItemLabelAnchor.OUTSIDE12, TextAnchor.BOTTOM_CENTER);
                        negativeItemLabelPosition = new ItemLabelPosition(
                                ItemLabelAnchor.OUTSIDE6, TextAnchor.TOP_CENTER);
                    } else {
                        positiveItemLabelPosition = new ItemLabelPosition(
                                ItemLabelAnchor.OUTSIDE6, TextAnchor.TOP_CENTER);
                        negativeItemLabelPosition = new ItemLabelPosition(
                                ItemLabelAnchor.OUTSIDE12, TextAnchor.BOTTOM_CENTER);
                    }
                    renderer.setSeriesPositiveItemLabelPosition(series.getSeriesIndex(), positiveItemLabelPosition);
                    renderer.setSeriesNegativeItemLabelPosition(series.getSeriesIndex(), negativeItemLabelPosition);
                    if (barSettings.getEnableLabelBackground()) {
                        renderer.setSeriesItemLabelsBackgroundVisible(series.getSeriesIndex(), true);
                        renderer.setSeriesItemLabelsBackgroundPaint(series.getSeriesIndex(), safeParseCssColor(barSettings.getLabelBackground()));
                    }
                }
            }
        }
        plot.setRenderer(datasetKey.getDatasetIndex(), renderer);
    }

    private void createLinesRenderer(TbDatasetKey datasetKey, List<TsChartSeriesData> seriesList) {
        TbXYLineAndShapeRenderer renderer = createLineRenderer(datasetKey);
        for (TsChartSeriesData series : seriesList) {
            LineSeriesSettings lineSettings = getSeriesSettings(series).getLineSettings();
            Color seriesColor = safeParseCssColor(series.getDataKey().getColor());
            Paint fillPaint = createFillPaint(lineSettings.getFillAreaSettings(), seriesColor);
            renderer.setSeriesPaint(series.getSeriesIndex(), seriesColor);
            renderer.setSeriesFillPaint(series.getSeriesIndex(), fillPaint);

            if (lineSettings.getShowLine()) {
                renderer.setSeriesLinesVisible(series.getSeriesIndex(), true);
                Stroke lineStroke = createLineStroke(lineSettings.getLineType(), lineSettings.getLineWidth());
                renderer.setSeriesStroke(series.getSeriesIndex(), lineStroke);
            } else {
                renderer.setSeriesLinesVisible(series.getSeriesIndex(), false);
            }
            if (lineSettings.getShowPoints() || series.getData().size() == 1) {
                Shape seriesShape = createSeriesShape(lineSettings.getPointShape(), lineSettings.getPointSize());
                if (seriesShape != null) {
                    renderer.setSeriesShapesVisible(series.getSeriesIndex(), true);
                    renderer.setSeriesShape(series.getSeriesIndex(), seriesShape);
                    if (ChartShape.emptyCircle.equals(lineSettings.getPointShape())) {
                        renderer.setSeriesShapesFillPaint(series.getSeriesIndex(), Color.WHITE);
                        renderer.setSeriesOutlineStroke(series.getSeriesIndex(), new BasicStroke(2.0f));
                    }
                    if (lineSettings.getShowPointLabel()) {
                        renderer.setSeriesItemLabelsVisible(series.getSeriesIndex(), true);
                        renderer.setSeriesItemLabelFont(series.getSeriesIndex(), toAwtFont(lineSettings.getPointLabelFont()));
                        renderer.setSeriesItemLabelPaint(series.getSeriesIndex(), safeParseCssColor(lineSettings.getPointLabelColor()));
                        XYItemLabelGenerator labelGenerator = new TbXYItemLabelGenerator(series.getDataKey().getDecimals(), series.getDataKey().getUnits(),
                                this.stateValueConverter);
                        renderer.setSeriesItemLabelGenerator(series.getSeriesIndex(), labelGenerator);
                        ItemLabelPosition itemLabelPosition;
                        if (ChartLabelPosition.top.equals(lineSettings.getPointLabelPosition())) {
                            itemLabelPosition = new ItemLabelPosition(
                                    ItemLabelAnchor.OUTSIDE12, TextAnchor.BOTTOM_CENTER);
                        } else {
                            itemLabelPosition = new ItemLabelPosition(
                                    ItemLabelAnchor.OUTSIDE6, TextAnchor.TOP_CENTER);
                        }
                        renderer.setItemLabelInsets(RectangleInsets.ZERO_INSETS);
                        renderer.setSeriesPositiveItemLabelPosition(series.getSeriesIndex(), itemLabelPosition);
                        renderer.setSeriesNegativeItemLabelPosition(series.getSeriesIndex(), itemLabelPosition);
                        if (lineSettings.getEnablePointLabelBackground()) {
                            renderer.setSeriesItemLabelsBackgroundVisible(series.getSeriesIndex(), true);
                            renderer.setSeriesItemLabelsBackgroundPaint(series.getSeriesIndex(), safeParseCssColor(lineSettings.getPointLabelBackground()));
                        }
                    }
                } else {
                    renderer.setSeriesShapesVisible(series.getSeriesIndex(), false);
                }
            } else {
                renderer.setSeriesShapesVisible(series.getSeriesIndex(), false);
            }
        }
        plot.setRenderer(datasetKey.getDatasetIndex(), renderer);
    }

    private TbXYBarRenderer createBarRenderer() {
        TbXYBarRenderer barRenderer = new TbXYBarRenderer(this.stackMode);
        barRenderer.setShadowVisible(false);
        barRenderer.setDrawBarOutline(true);
        barRenderer.setDefaultOutlineStroke(new BasicStroke(0.0f));
        barRenderer.setLegendItemLabelGenerator(this);
        barRenderer.setTbSeriesLegendValuesGenerator(this);
        return barRenderer;
    }

    private TbXYLineAndShapeRenderer createLineRenderer(TbDatasetKey datasetKey) {
        TbXYLineAndShapeRenderer.LineInterpolationType interpolationType = TbXYLineAndShapeRenderer.LineInterpolationType.NONE;
        if (datasetKey.isStepLine()) {
            interpolationType = TbXYLineAndShapeRenderer.LineInterpolationType.STEP;
        } else if (datasetKey.isSmoothLine()) {
            interpolationType = TbXYLineAndShapeRenderer.LineInterpolationType.SMOOTH;
        }
        TbXYLineAndShapeRenderer lineRenderer = new TbXYLineAndShapeRenderer(interpolationType,
                datasetKey.isFillArea() ? TbXYLineAndShapeRenderer.FillType.TO_ZERO : TbXYLineAndShapeRenderer.FillType.NONE,
                this.stackMode);
        if (datasetKey.isStepLine()) {
            double stepPoint = 0.0;
            switch (datasetKey.getStepType()) {
                case start -> stepPoint = 0.0;
                case middle -> stepPoint = 0.5;
                case end -> stepPoint = 1.0;
            }
            lineRenderer.setStepPoint(stepPoint);
        } else if (datasetKey.isSmoothLine()) {
            lineRenderer.setPrecision(100);
            lineRenderer.setSmooth(0.25f);
        }
        lineRenderer.setLegendItemLabelGenerator(this);
        lineRenderer.setTbSeriesLegendValuesGenerator(this);
        return lineRenderer;
    }

    @Override
    public String generateLabel(XYDataset dataset, int seriesIndex) {
        int datasetIndex = this.plot.indexOf(dataset);
        Optional<TsChartSeriesData> seriesOpt = findSeries(datasetIndex, seriesIndex);
        if (seriesOpt.isPresent()) {
            TsChartSeriesData series = seriesOpt.get();
            return ThymeleafUtil.renderFromTextString(series.getDataKey().getLabel(), series.getDataSource().getVariables());
        } else {
            return "Undefined";
        }
    }

    @Override
    public TbLegendValues generateLegendValues(TbLegendValuesRequest request, XYDataset dataset, int seriesIndex) {
        int datasetIndex = this.plot.indexOf(dataset);
        Optional<TsChartSeriesData> seriesOpt = findSeries(datasetIndex, seriesIndex);
        if (seriesOpt.isPresent()) {
            TsChartSeriesData series = seriesOpt.get();
            int decimals = series.getDataKey().getDecimals() != null ? series.getDataKey().getDecimals() : 2;
            String units = series.getDataKey().getUnits();
            NumberFormat valueFormatter = createValueFormatter(decimals, units);
            TbLegendValues result = new TbLegendValues();
            if (request.isMin()) {
                Double min = series.calcMin();
                result.setMin(min != null ? valueFormatter.format(min) : "");
            }
            if (request.isMax()) {
                Double max = series.calcMax();
                result.setMax(max != null ? valueFormatter.format(max) : "");
            }
            if (request.isAvg()) {
                Double avg = series.calcAvg();
                result.setAvg(avg != null ? valueFormatter.format(avg) : "");
            }
            if (request.isTotal()) {
                Double total = series.calcTotal();
                result.setTotal(total != null ? valueFormatter.format(total) : "");
            }
            if (request.isLatest()) {
                Double latest = series.calcLatest();
                result.setLatest(latest != null ? valueFormatter.format(latest) : "");
            }
            return result;
        } else {
            return null;
        }
    }

    private int findSeriesIndex(int datasetIndex, int seriesIndex) {
        Optional<TsChartSeriesData> series = findSeries(datasetIndex, seriesIndex);
        return series.map(TsChartSeriesData::getIndex).orElse(-1);
    }

    private Optional<TsChartSeriesData> findSeries(int datasetIndex, int seriesIndex) {
        return this.seriesList.stream()
                .filter(s -> s.getDatasetIndex() == datasetIndex && s.getSeriesIndex() == seriesIndex)
                .findFirst();
    }
}
