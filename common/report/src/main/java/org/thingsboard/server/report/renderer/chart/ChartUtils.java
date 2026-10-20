// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.report.renderer.chart;

import org.jfree.chart.axis.Axis;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.DateTickUnit;
import org.jfree.chart.axis.DateTickUnitType;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.axis.TickUnitSource;
import org.jfree.chart.axis.TickUnits;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.data.time.SimpleTimePeriod;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.report.configuration.DataKeySettings;
import org.thingsboard.server.common.data.report.configuration.chart.AxisPosition;
import org.thingsboard.server.common.data.report.configuration.chart.ChartFillSettings;
import org.thingsboard.server.common.data.report.configuration.chart.ChartLineType;
import org.thingsboard.server.common.data.report.configuration.chart.ChartShape;
import org.thingsboard.server.common.data.report.configuration.chart.FormatTimeUnit;
import org.thingsboard.server.common.data.report.configuration.chart.TimeSeriesChartAxisSettings;
import org.thingsboard.server.common.data.report.configuration.chart.TimeSeriesChartKeySettings;
import org.thingsboard.server.common.data.report.configuration.chart.TimeSeriesChartNoAggregationBarWidthStrategy;
import org.thingsboard.server.common.data.report.configuration.chart.TimeSeriesChartThreshold;
import org.thingsboard.server.common.data.report.configuration.chart.TimeSeriesChartXAxisSettings;
import org.thingsboard.server.common.data.report.configuration.chart.TimeSeriesChartYAxisSettings;
import org.thingsboard.server.common.data.report.configuration.timewindow.TimeIntervalCalculator;
import org.thingsboard.server.report.context.chart.TsChartSeriesData;
import org.thingsboard.server.report.context.chart.TsChartSeriesEntry;
import org.thingsboard.server.report.context.chart.TsChartThresholdItem;
import org.thingsboard.server.report.context.chart.YAxisScale;
import org.thingsboard.server.report.util.ColorUtils;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Paint;
import java.awt.Polygon;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.stream.Collectors;

import static org.jfree.chart.axis.Axis.DEFAULT_AXIS_LABEL_INSETS;
import static org.jfree.chart.axis.Axis.DEFAULT_TICK_LABEL_INSETS;
import static org.thingsboard.server.report.renderer.chart.ChartUtils.getSeriesSettings;
import static org.thingsboard.server.report.util.AwtFontUtils.ZERO_FONT;
import static org.thingsboard.server.report.util.AwtFontUtils.toAwtFont;
import static org.thingsboard.server.report.util.ColorUtils.safeParseCssColor;

public interface ChartUtils {

    static TimeSeriesChartKeySettings getSeriesSettings(TsChartSeriesData chartSeriesData) {
        DataKeySettings settings = chartSeriesData.getDataKey().getSettings();
        TimeSeriesChartKeySettings keySettings = null;
        if (settings instanceof TimeSeriesChartKeySettings) {
            keySettings = (TimeSeriesChartKeySettings) settings;
        }
        return new TimeSeriesChartKeySettings(keySettings);
    }

    static SimpleTimePeriod calculateBarTimePeriod(TsChartSeriesEntry entry,
                                                    TimeseriesBarRenderCtx barRenderCtx,
                                                    int barsCount, int barIndex) {

        long time = entry.getTs();
        long start = entry.getInterval().startTs;
        long end = entry.getInterval().endTs;
        long interval = end - start;

        if (barRenderCtx.isNoAggregation()) {
            if (barRenderCtx.isNoAggregationWidthRelative()) {
                interval = (long)(barRenderCtx.getTimeWindow() * barRenderCtx.getNoAggregationWidth() / 100f);
            } else {
                interval = (long)barRenderCtx.getNoAggregationWidth();
            }
            start = time - interval / 2;
        }

        double barGapRatio = barRenderCtx.getBarGap();
        double intervalGapRatio = barRenderCtx.getIntervalGap();
        boolean separateBar = barRenderCtx.isNoAggregation() && TimeSeriesChartNoAggregationBarWidthStrategy.separate.equals(barRenderCtx.getNoAggregationBarWidthStrategy());

        long barInterval = separateBar ? interval : (long)((double)interval / (barsCount + barGapRatio * (barsCount - 1) + intervalGapRatio * 2));

        long intervalGap = (long)(barInterval * intervalGapRatio);
        long barGap = (long)(barInterval * barGapRatio);

        long startTime = separateBar ? start : start + intervalGap + (barInterval + barGap) * barIndex;
        long endTime = startTime + barInterval;

        return new SimpleTimePeriod(startTime, endTime);
    }

    static TbDateAxis createXAxis(XYPlot plot, TimeSeriesChartXAxisSettings xAxisSettings, TimeIntervalCalculator.TimeRange timeRange, TimeZone timeZone, int index) {
        Locale locale = Locale.getDefault();
        TbDateAxis xAxis = new TbDateAxis(xAxisSettings.getLabel(), timeZone, locale);
        plot.setDomainAxis(index, xAxis);
        xAxis.setStandardTickUnits(createDateTickUnitsFromTicksFormat(xAxisSettings.getTicksFormat(), timeZone, locale));
        AxisLocation location = AxisPosition.bottom.equals(xAxisSettings.getPosition()) ? AxisLocation.BOTTOM_OR_LEFT : AxisLocation.TOP_OR_RIGHT;
        plot.setDomainAxisLocation(index, location);
        xAxis.setMinimumDate(new Date(timeRange.startTs));
        xAxis.setMaximumDate(new Date(timeRange.endTs));
        xAxis.setGridlinesVisible(xAxisSettings.getShowSplitLines());
        xAxis.setGridlinePaint(safeParseCssColor(xAxisSettings.getSplitLinesColor()));
        xAxis.setTickLabelInsets(new RectangleInsets(DEFAULT_TICK_LABEL_INSETS.getTop(),
                DEFAULT_TICK_LABEL_INSETS.getLeft() + 0.5,
                DEFAULT_TICK_LABEL_INSETS.getBottom(),
                DEFAULT_TICK_LABEL_INSETS.getRight() + 0.5));
        setupAxisAppearance(xAxis, xAxisSettings);
        return xAxis;
    }

    static TbNumberAxis createYAxis(XYPlot plot, TimeSeriesChartYAxisSettings yAxisSettings, List<TbStateTick> stateTicks, String units, Integer decimals, int index, YAxisScale yAxisScale) {
        TbNumberAxis parent = null;
        if (index > 0) {
            parent = (TbNumberAxis) plot.getRangeAxis();
        }
        TbNumberAxis yAxis = new TbNumberAxis(yAxisSettings.getLabel(), parent);
        plot.setRangeAxis(index, yAxis);
        AxisLocation location = AxisPosition.left.equals(yAxisSettings.getPosition()) ? AxisLocation.BOTTOM_OR_LEFT : AxisLocation.TOP_OR_RIGHT;
        plot.setRangeAxisLocation(index, location);
        yAxis.setAutoRangeIncludesZero(false);
        yAxis.setGridlinesVisible(yAxisSettings.getShowSplitLines());
        yAxis.setGridlinePaint(safeParseCssColor(yAxisSettings.getSplitLinesColor()));
        yAxis.setStateTicks(stateTicks);
        if (yAxisSettings.getSplitNumber() != null) {
            yAxis.setSplitNumber(yAxisSettings.getSplitNumber());
        } else if (yAxisSettings.getInterval() != null && yAxisSettings.getInterval() > 0) {
            yAxis.setTickUnit(new NumberTickUnit(yAxisSettings.getInterval()));
        }
        if (yAxisScale != null) {
            if (yAxisScale.min() != null) {
                yAxis.setAxisMin(yAxisScale.min());
            }
            if (yAxisScale.max() != null) {
                yAxis.setAxisMax(yAxisScale.max());
            }
        }
        int axisDecimals = yAxisSettings.getDecimals() != null ? yAxisSettings.getDecimals() : (decimals != null ? decimals : 2);
        String axisUnits = yAxisSettings.getUnits() != null ? yAxisSettings.getUnits() : units;
        yAxis.setNumberFormatOverride(createValueFormatter(axisDecimals, axisUnits));
        setupAxisAppearance(yAxis, yAxisSettings);
        return yAxis;
    }

    static TbThresholdMarker createThresholdMarker(TsChartThresholdItem item, String units, Integer decimals) {
        TbThresholdMarker marker = new TbThresholdMarker(item.getValue());
        TimeSeriesChartThreshold threshold = item.getSettings();
        marker.setPaint(safeParseCssColor(threshold.getLineColor()));
        marker.setStroke(createLineStroke(threshold.getLineType(), threshold.getLineWidth()));
        marker.setStartSymbol(threshold.getStartSymbol(), threshold.getStartSymbolSize());
        marker.setEndSymbol(threshold.getEndSymbol(), threshold.getEndSymbolSize());
        if (threshold.getShowLabel()) {
            int thresholdDecimals = threshold.getDecimals() != null ? threshold.getDecimals() : (decimals != null ? decimals : 2);
            String thresholdUnits = threshold.getUnits() != null ? threshold.getUnits() : units;
            NumberFormat formatter = createValueFormatter(thresholdDecimals, thresholdUnits);
            String label = formatter.format(item.getValue());
            marker.setLabel(label);
            marker.setLabelPaint(safeParseCssColor(threshold.getLabelColor()));
            marker.setLabelFont(toAwtFont(threshold.getLabelFont()));
            if (threshold.getEnableLabelBackground()) {
                marker.setDrawLabelBackground(true);
                marker.setLabelBackgroundColor(safeParseCssColor(threshold.getLabelBackground()));
            }
            marker.setLabelPosition(threshold.getLabelPosition());
        }
        return marker;
    }

    static NumberFormat createValueFormatter(int decimals, String units) {
        StringBuilder patternBuilder = new StringBuilder("#");
        if (decimals > 0) {
            patternBuilder.append(".");
        }
        patternBuilder.append("#".repeat(Math.max(0, decimals)));
        if (StringUtils.isNotBlank(units)) {
            patternBuilder.append(" '").append(units).append("'");
        }
        return new DecimalFormat(patternBuilder.toString());
    }

    static void adjustAxisMargins(Axis axis, double top, double left, double bottom, double right) {
        if (StringUtils.isBlank(axis.getLabel())) {
            axis.setLabel(" ");
            axis.setLabelFont(ZERO_FONT);
        }
        axis.setLabelInsets(new RectangleInsets(DEFAULT_AXIS_LABEL_INSETS.getTop() + top,
                DEFAULT_AXIS_LABEL_INSETS.getLeft() + left,
                DEFAULT_AXIS_LABEL_INSETS.getBottom() + bottom,
                DEFAULT_AXIS_LABEL_INSETS.getRight() + right));
    }

    static Shape createSeriesShape(ChartShape chartShape, float size) {
        Shape result = null;
        double delta = size / 2.0;
        int[] xpoints;
        int[] ypoints;
        switch (chartShape) {
            case emptyCircle, circle -> result = new Ellipse2D.Double(-delta, -delta, size, size);
            case rect -> result = new Rectangle2D.Double(-delta, -delta, size, size);
            case roundRect -> result = new RoundRectangle2D.Double(-delta, -delta, size, size, size/4, size/4);
            case triangle -> {
                xpoints = new int[]{0, (int)delta, (int)-delta};
                ypoints = new int[]{(int)-delta, (int)delta, (int)delta};
                result = new Polygon(xpoints, ypoints, 3);
            }
            case diamond -> {
                xpoints = new int[]{0, (int)delta, 0, (int)-delta};
                ypoints = new int[]{(int)-delta, 0, (int)delta, 0};
                result = new Polygon(xpoints, ypoints, 4);
            }
            case pin -> result = createPin(0, 0, size, size);
            case arrow -> result = createArrow(0, 0, size, size);
            case none -> {
            }
        }
        return result;
    }

    static Stroke createLineStroke(ChartLineType lineType, Float lineWidth) {
        float[] dashPattern = null;
        switch (lineType) {
            case solid -> {}
            case dashed -> dashPattern = new float[]{4.0f * lineWidth, 2.0f * lineWidth};
            case dotted -> dashPattern = new float[]{lineWidth};
        }
        return new BasicStroke(
                lineWidth,
                BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_BEVEL,
                10.0f,
                dashPattern,
                0.0f
        );
    }

    static Paint createFillPaint(ChartFillSettings fillSettings, Color seriesColor) {
        switch (fillSettings.getType()) {
            case none -> {
                return ColorUtils.TRANSPARENT;
            }
            case opacity -> {
                return ColorUtils.applyOpacity(seriesColor, fillSettings.getOpacity());
            }
            case gradient -> {
                Color startColor = ColorUtils.setOpacity(seriesColor, fillSettings.getGradient().getStart() / 100f);
                Color endColor = ColorUtils.setOpacity(seriesColor, fillSettings.getGradient().getEnd() / 100f);
                return new GradientPaint(0, 0, startColor, 1f, 1f, endColor);
            }
        }
        return ColorUtils.TRANSPARENT;
    }

    static Paint createFillPaint(boolean fillArea, float fillAreaOpacity, Color seriesColor) {
        if (!fillArea) {
            return ColorUtils.TRANSPARENT;
        } else {
            return ColorUtils.applyOpacity(seriesColor, fillAreaOpacity);
        }
    }

    static Map<TbDatasetKey, List<TsChartSeriesData>> datasetGroupsFromSeries(List<TsChartSeriesData> rawSeries) {
        Map<TbDatasetKey, List<TsChartSeriesData>> groupedSeries = rawSeries.stream().
                collect(Collectors.groupingBy(s -> new TbDatasetKey(getSeriesSettings(s), s.getDataSource().isComparison()))).
                entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (oldValue, newValue) -> oldValue,
                        LinkedHashMap::new
                ));
        int datasetIndex = 0;
        for (Map.Entry<TbDatasetKey, List<TsChartSeriesData>> entry : groupedSeries.entrySet()) {
            TbDatasetKey key = entry.getKey();
            List<TsChartSeriesData> series = entry.getValue();
            key.setDatasetIndex(datasetIndex);
            series.sort(Comparator.comparing(TsChartSeriesData::getIndex));
            int seriesIndex = 0;
            for (TsChartSeriesData seriesItem : series) {
                seriesItem.setDatasetIndex(datasetIndex);
                seriesItem.setSeriesIndex(seriesIndex);
                seriesIndex++;
            }
            datasetIndex++;
        }
        return groupedSeries;
    }

    static Shape createRectangularShapeWithRoundedCorners(double x, double y, double width, double height,
                                                          double radiusTopLeft, double radiusTopRight, double radiusBottomRight, double radiusBottomLeft) {
        double maxRadius = Math.min(width, height) / 2f;
        Path2D.Double path = new Path2D.Double();
        path.moveTo(x + radiusTopLeft, y);
        path.lineTo(x + width - radiusTopRight, y);
        if (radiusTopRight > 0) {
            radiusTopRight = Math.min(maxRadius, radiusTopRight);
            path.append(new Arc2D.Double(x + width - radiusTopRight * 2, y, radiusTopRight * 2, radiusTopRight * 2, 90, -90, Arc2D.OPEN), true);
        }
        path.lineTo(x + width, y + height - radiusBottomRight);
        if (radiusBottomRight > 0) {
            radiusBottomRight = Math.min(maxRadius, radiusBottomRight);
            path.append(new Arc2D.Double(x + width - radiusBottomRight * 2, y + height - radiusBottomRight * 2, radiusBottomRight * 2, radiusBottomRight * 2, 0, -90, Arc2D.OPEN), true);
        }
        path.lineTo(x + radiusBottomLeft, y + height);
        if (radiusBottomLeft > 0) {
            radiusBottomLeft = Math.min(maxRadius, radiusBottomLeft);
            path.append(new Arc2D.Double(x, y + height - radiusBottomLeft * 2, radiusBottomLeft * 2, radiusBottomLeft * 2, -90, -90, Arc2D.OPEN), true);
        }
        path.lineTo(x, y + radiusTopLeft);
        if (radiusTopLeft > 0) {
            radiusTopLeft = Math.min(maxRadius, radiusTopLeft);
            path.append(new Arc2D.Double(x, y, radiusTopLeft * 2, radiusTopLeft * 2, 180, -90, Arc2D.OPEN), true);
        }
        path.closePath();
        return path;
    }

    static List<Point2D> interpolateBezier(List<Point2D> points, float smooth, int bezierNumPoints) {
        List<Point2D> interpolatedPoints = new ArrayList<>();
        int i = 0;
        int len = points.size();
        while (i < len) {
            i += calculateBezierSegmentPoints(points, i, smooth, bezierNumPoints, interpolatedPoints) + 1;
        }
        return interpolatedPoints;
    }

    private static int calculateBezierSegmentPoints(List<Point2D> points, int start, float smooth, int bezierNumPoints, List<Point2D> targetPoints) {
        int idx = start;
        double cpx0 = 0;
        double cpy0 = 0;
        double cpx1 = 0;
        double cpy1 = 0;
        double prevX = 0;
        double prevY = 0;
        int k = 0;
        for (; k < points.size(); k++) {
            if (idx >= points.size()) {
                break;
            }
            Point2D point = points.get(idx);
            double x = point.getX();
            double y = point.getY();
            if (idx == start) {
                cpx0 = x;
                cpy0 = y;
                targetPoints.add(new Point2D.Double(x, y));
            } else {
                double dx = x - prevX;
                double dy = y - prevY;

                // Ignore tiny segment.
                if ((dx * dx + dy * dy) < 0.5) {
                    idx += 1;
                    continue;
                }

                if (smooth > 0) {
                    int nextIdx = idx + 1;
                    Point2D nextPoint = nextIdx < points.size() ? points.get(nextIdx) : null;
                    double nextX = nextPoint != null ? nextPoint.getX() : 0;
                    double nextY = nextPoint != null ? nextPoint.getY() : 0;
                    // Ignore duplicate point
                    while (nextX == x && nextY == y && k < points.size()) {
                        k++;
                        nextIdx += 1;
                        idx += 1;
                        nextPoint = nextIdx < points.size() ? points.get(nextIdx) : null;
                        nextX = nextPoint != null ? nextPoint.getX() : 0;
                        nextY = nextPoint != null ? nextPoint.getY() : 0;
                        point = points.get(idx);
                        x = point.getX();
                        y = point.getY();
                        dx = x - prevX;
                        dy = y - prevY;
                    }
                    int tempK = k + 1;
                    double ratioNextSeg = 0.5f;
                    double vx = 0;
                    double vy = 0;
                    double nextCpx0 = 0;
                    double nextCpy0 = 0;
                    // Is last point
                    if (tempK >= points.size()) {
                        cpx1 = x;
                        cpy1 = y;
                    } else {
                        vx = nextX - prevX;
                        vy = nextY - prevY;
                        double dx0 = x - prevX;
                        double dx1 = nextX - x;
                        double dy0 = y - prevY;
                        double dy1 = nextY - y;
                        double lenPrevSeg = Math.sqrt(dx0 * dx0 + dy0 * dy0);
                        double lenNextSeg = Math.sqrt(dx1 * dx1 + dy1 * dy1);
                        ratioNextSeg = lenNextSeg / (lenNextSeg + lenPrevSeg);

                        cpx1 = x - vx * smooth * (1 - ratioNextSeg);
                        cpy1 = y - vy * smooth * (1 - ratioNextSeg);

                        nextCpx0 = x + vx * smooth * ratioNextSeg;
                        nextCpy0 = y + vy * smooth * ratioNextSeg;

                        nextCpx0 = Math.min(nextCpx0, Math.max(nextX, x));
                        nextCpy0 = Math.min(nextCpy0, Math.max(nextY, y));
                        nextCpx0 = Math.min(nextCpx0, Math.max(nextX, x));
                        nextCpy0 = Math.min(nextCpy0, Math.max(nextY, y));

                        vx = nextCpx0 - x;
                        vy = nextCpy0 - y;

                        cpx1 = x - vx * lenPrevSeg / lenNextSeg;
                        cpy1 = y - vy * lenPrevSeg / lenNextSeg;

                        cpx1 = Math.min(cpx1, Math.max(prevX, x));
                        cpy1 = Math.min(cpy1, Math.max(prevY, y));
                        cpx1 = Math.min(cpx1, Math.max(prevX, x));
                        cpy1 = Math.min(cpy1, Math.max(prevY, y));

                        vx = x - cpx1;
                        vy = y - cpy1;
                        nextCpx0 = x + vx * lenNextSeg / lenPrevSeg;
                        nextCpy0 = y + vy * lenNextSeg / lenPrevSeg;
                    }
                    setBezierCurvePoints(cpx0, cpy0, cpx1, cpy1, x, y, bezierNumPoints, targetPoints);
                    cpx0 = nextCpx0;
                    cpy0 = nextCpy0;
                } else {
                    targetPoints.add(new Point2D.Double(x, y));
                }
            }
            prevX = x;
            prevY = y;
            idx += 1;
        }
        return k;
    }

    private static void setBezierCurvePoints(double x1, double y1, double x2, double y2, double x3, double y3, int numPoints, List<Point2D> targetPoints) {
        Point2D lastPoint = targetPoints.get(targetPoints.size() - 1);
        double x0 = lastPoint.getX();
        double y0 = lastPoint.getY();
        float step = 1f / (numPoints - 1);
        for (float t = 0; t <= 1; t += step) {
             double x = Math.pow(1 - t, 3) * x0 +
                    3 * t * Math.pow(1 - t, 2) * x1 +
                    3 * Math.pow(t, 2) * (1 - t) * x2 +
                    Math.pow(t, 3) * x3;
             double y = Math.pow(1 - t, 3) * y0 +
                    3 * t * Math.pow(1 - t, 2) * y1 +
                    3 * Math.pow(t, 2) * (1 - t) * y2 +
                    Math.pow(t, 3) * y3;
             targetPoints.add(new Point2D.Double(x, y));
        }
    }

    private static void setupAxisAppearance(ValueAxis axis, TimeSeriesChartAxisSettings axisSettings) {
        axis.setVisible(axisSettings.getShow());
        axis.setLabelFont(toAwtFont(axisSettings.getLabelFont()));
        axis.setLabelPaint(safeParseCssColor(axisSettings.getLabelColor()));
        axis.setTickLabelsVisible(axisSettings.getShowTickLabels());
        axis.setTickLabelFont(toAwtFont(axisSettings.getTickLabelFont()));
        axis.setTickLabelPaint(safeParseCssColor(axisSettings.getTickLabelColor()));
        axis.setTickMarksVisible(axisSettings.getShowTicks());
        axis.setTickMarkPaint(safeParseCssColor(axisSettings.getTicksColor()));
        axis.setAxisLineVisible(axisSettings.getShowLine());
        axis.setAxisLineStroke(new BasicStroke(1.0f));
        axis.setAxisLinePaint(safeParseCssColor(axisSettings.getLineColor()));
    }

    private static TickUnitSource createDateTickUnitsFromTicksFormat(Map<FormatTimeUnit, String> ticksFormat,
                                                                     TimeZone zone,
                                                                     Locale locale) {
        TickUnits units = new TickUnits();

        // date formatters
        DateFormat f1 = new SimpleDateFormat(ticksFormat.get(FormatTimeUnit.millisecond), locale);
        DateFormat f2 = new SimpleDateFormat(ticksFormat.get(FormatTimeUnit.second), locale);
        DateFormat f3 = new SimpleDateFormat(ticksFormat.get(FormatTimeUnit.minute), locale);
        DateFormat f4 = new SimpleDateFormat(ticksFormat.get(FormatTimeUnit.hour), locale);
        DateFormat f5 = new SimpleDateFormat(ticksFormat.get(FormatTimeUnit.day), locale);
        DateFormat f6 = new SimpleDateFormat(ticksFormat.get(FormatTimeUnit.month), locale);
        DateFormat f7 = new SimpleDateFormat(ticksFormat.get(FormatTimeUnit.year), locale);

        f1.setTimeZone(zone);
        f2.setTimeZone(zone);
        f3.setTimeZone(zone);
        f4.setTimeZone(zone);
        f5.setTimeZone(zone);
        f6.setTimeZone(zone);
        f7.setTimeZone(zone);

        // milliseconds
        units.add(new DateTickUnit(DateTickUnitType.MILLISECOND, 1, f1));
        units.add(new DateTickUnit(DateTickUnitType.MILLISECOND, 5,
                DateTickUnitType.MILLISECOND, 1, f1));
        units.add(new DateTickUnit(DateTickUnitType.MILLISECOND, 10,
                DateTickUnitType.MILLISECOND, 1, f1));
        units.add(new DateTickUnit(DateTickUnitType.MILLISECOND, 25,
                DateTickUnitType.MILLISECOND, 5, f1));
        units.add(new DateTickUnit(DateTickUnitType.MILLISECOND, 50,
                DateTickUnitType.MILLISECOND, 10, f1));
        units.add(new DateTickUnit(DateTickUnitType.MILLISECOND, 100,
                DateTickUnitType.MILLISECOND, 10, f1));
        units.add(new DateTickUnit(DateTickUnitType.MILLISECOND, 250,
                DateTickUnitType.MILLISECOND, 10, f1));
        units.add(new DateTickUnit(DateTickUnitType.MILLISECOND, 500,
                DateTickUnitType.MILLISECOND, 50, f1));

        // seconds
        units.add(new DateTickUnit(DateTickUnitType.SECOND, 1,
                DateTickUnitType.MILLISECOND, 50, f2));
        units.add(new DateTickUnit(DateTickUnitType.SECOND, 5,
                DateTickUnitType.SECOND, 1, f2));
        units.add(new DateTickUnit(DateTickUnitType.SECOND, 10,
                DateTickUnitType.SECOND, 1, f2));
        units.add(new DateTickUnit(DateTickUnitType.SECOND, 30,
                DateTickUnitType.SECOND, 5, f2));

        // minutes
        units.add(new DateTickUnit(DateTickUnitType.MINUTE, 1,
                DateTickUnitType.SECOND, 5, f3));
        units.add(new DateTickUnit(DateTickUnitType.MINUTE, 2,
                DateTickUnitType.SECOND, 10, f3));
        units.add(new DateTickUnit(DateTickUnitType.MINUTE, 5,
                DateTickUnitType.MINUTE, 1, f3));
        units.add(new DateTickUnit(DateTickUnitType.MINUTE, 10,
                DateTickUnitType.MINUTE, 1, f3));
        units.add(new DateTickUnit(DateTickUnitType.MINUTE, 15,
                DateTickUnitType.MINUTE, 5, f3));
        units.add(new DateTickUnit(DateTickUnitType.MINUTE, 20,
                DateTickUnitType.MINUTE, 5, f3));
        units.add(new DateTickUnit(DateTickUnitType.MINUTE, 30,
                DateTickUnitType.MINUTE, 5, f3));

        // hours
        units.add(new DateTickUnit(DateTickUnitType.HOUR, 1,
                DateTickUnitType.MINUTE, 5, f4));
        units.add(new DateTickUnit(DateTickUnitType.HOUR, 2,
                DateTickUnitType.MINUTE, 10, f4));
        units.add(new DateTickUnit(DateTickUnitType.HOUR, 4,
                DateTickUnitType.MINUTE, 30, f4));
        units.add(new DateTickUnit(DateTickUnitType.HOUR, 6,
                DateTickUnitType.HOUR, 1, f4));
        //units.add(new DateTickUnit(DateTickUnitType.HOUR, 12,
          //      DateTickUnitType.HOUR, 1, f5));

        // days
        units.add(new DateTickUnit(DateTickUnitType.DAY, 1,
                DateTickUnitType.HOUR, 1, f5));
        units.add(new DateTickUnit(DateTickUnitType.DAY, 2,
                DateTickUnitType.HOUR, 1, f5));
        units.add(new DateTickUnit(DateTickUnitType.DAY, 4,
                DateTickUnitType.HOUR, 1, f5));
        units.add(new DateTickUnit(DateTickUnitType.DAY, 7,
                DateTickUnitType.DAY, 1, f5));
        units.add(new DateTickUnit(DateTickUnitType.DAY, 15,
                DateTickUnitType.DAY, 1, f5));

        // months
        units.add(new DateTickUnit(DateTickUnitType.MONTH, 1,
                DateTickUnitType.DAY, 1, f6));
        units.add(new DateTickUnit(DateTickUnitType.MONTH, 2,
                DateTickUnitType.DAY, 1, f6));
        units.add(new DateTickUnit(DateTickUnitType.MONTH, 3,
                DateTickUnitType.MONTH, 1, f6));
        units.add(new DateTickUnit(DateTickUnitType.MONTH, 4,
                DateTickUnitType.MONTH, 1, f6));
        units.add(new DateTickUnit(DateTickUnitType.MONTH, 6,
                DateTickUnitType.MONTH, 1, f6));

        // years
        units.add(new DateTickUnit(DateTickUnitType.YEAR, 1,
                DateTickUnitType.MONTH, 1, f7));
        units.add(new DateTickUnit(DateTickUnitType.YEAR, 2,
                DateTickUnitType.MONTH, 3, f7));
        units.add(new DateTickUnit(DateTickUnitType.YEAR, 5,
                DateTickUnitType.YEAR, 1, f7));
        units.add(new DateTickUnit(DateTickUnitType.YEAR, 10,
                DateTickUnitType.YEAR, 1, f7));
        units.add(new DateTickUnit(DateTickUnitType.YEAR, 25,
                DateTickUnitType.YEAR, 5, f7));
        units.add(new DateTickUnit(DateTickUnitType.YEAR, 50,
                DateTickUnitType.YEAR, 10, f7));
        units.add(new DateTickUnit(DateTickUnitType.YEAR, 100,
                DateTickUnitType.YEAR, 20, f7));

        return units;
    }

    private static Shape createPin(double x, double y, double width, double height) {
        double w = width / 5.0 * 3.0;
        double r =  w / 2.0;
        double dy = r * r / (height - r);
        double cy = y - height + r + dy;
        double angle = Math.asin(dy / r);
        double dx = Math.cos(angle) * r;
        double tanX = Math.sin(angle);
        double tanY = Math.cos(angle);
        double cpLen = r * 0.6;
        double cpLen2 = r * 0.7;
        Path2D.Double path = new Path2D.Double();
        path.moveTo(x - dx, cy + dy);
        Arc2D.Double arc = createArc(x, cy, r,
                Math.PI - angle,
                Math.PI * 2 + angle);
        path.append(arc, true);
        path.curveTo(
                x + dx - tanX * cpLen, cy + dy + tanY * cpLen,
                x, y - cpLen2,
                x, y
        );
        path.curveTo(
                x, y - cpLen2,
                x - dx + tanX * cpLen, cy + dy + tanY * cpLen,
                x - dx, cy + dy
        );
        path.closePath();
        return path;
    }

    private static Shape createArrow(double x, double y, double width, double height) {
        double dx = width / 3 * 2;
        Path2D.Double path = new Path2D.Double();
        path.moveTo(x, y);
        path.lineTo(x + dx, y + height);
        path.lineTo(x, y + height / 4 * 3);
        path.lineTo(x - dx, y + height);
        path.lineTo(x, y);
        path.closePath();
        return path;
    }

    private static Arc2D.Double createArc(double centerX, double centerY, double radius, double startAngleRad, double endAngleRad) {
        // Convert radians to degrees
        double startAngleDeg = 360 - Math.toDegrees(startAngleRad);
        double endAngleDeg = 360 - Math.toDegrees(endAngleRad);

        // Calculate the angular extent
        double extent = endAngleDeg - startAngleDeg;
        if (extent > 0) {
            extent = extent - 360;
        }

        // Create the bounding rectangle
        Rectangle2D.Double rect = new Rectangle2D.Double(
                centerX - radius,
                centerY - radius,
                radius * 2,
                radius * 2
        );

        // Create the arc
        return new Arc2D.Double(rect, startAngleDeg, extent, Arc2D.OPEN);
    }
}
