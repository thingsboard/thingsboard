// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.report.renderer.chart;

import org.jfree.chart.LegendItem;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.entity.EntityCollection;
import org.jfree.chart.labels.ItemLabelAnchor;
import org.jfree.chart.labels.ItemLabelPosition;
import org.jfree.chart.labels.XYItemLabelGenerator;
import org.jfree.chart.labels.XYSeriesLabelGenerator;
import org.jfree.chart.plot.CrosshairState;
import org.jfree.chart.plot.Marker;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.renderer.xy.XYItemRendererState;
import org.jfree.chart.text.TextUtils;
import org.jfree.chart.ui.GradientPaintTransformer;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.chart.ui.TextAnchor;
import org.jfree.chart.util.Args;
import org.jfree.chart.util.BooleanList;
import org.jfree.chart.util.PaintList;
import org.jfree.data.Range;
import org.jfree.data.general.DatasetUtils;
import org.jfree.data.xy.IntervalXYDataset;
import org.jfree.data.xy.TableXYDataset;
import org.jfree.data.xy.XYDataset;
import org.thingsboard.server.report.renderer.chart.legend.TbLegendItem;
import org.thingsboard.server.report.renderer.chart.legend.TbLegendValues;
import org.thingsboard.server.report.renderer.chart.legend.TbLegendValuesRequest;
import org.thingsboard.server.report.renderer.chart.legend.TbSeriesLegendValuesGenerator;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.Map;

import static org.thingsboard.server.report.util.AwtFontUtils.newFont;
import static org.thingsboard.server.report.util.ColorUtils.safeParseCssColor;

public class TbXYBarRenderer extends XYBarRenderer implements TbItemRenderer {

    private final TbThresholdPainter thresholdPainter;

    private final Map<Integer, Float> itemBorderRadiusMap;
    private Float defaultItemBorderRadius;

    private final BooleanList seriesLabelsVisibleList;
    private boolean defaultSeriesLabelsVisible;
    private Map<Integer, Font> seriesLabelFontMap;
    private Font defaultSeriesLabelFont;
    private PaintList seriesLabelPaintList;
    private transient Paint defaultSeriesLabelPaint;
    private final Map<Integer, XYSeriesLabelGenerator> seriesLabelGeneratorMap;
    private XYSeriesLabelGenerator defaultSeriesLabelGenerator;

    private final BooleanList itemLabelsBackgroundVisibleList;
    private boolean defaultItemLabelsBackgroundVisible;
    private final PaintList itemLabelsBackgroundPaintList;
    private transient Paint defaultItemLabelBackgroundPaint;

    private TbSeriesLegendValuesGenerator seriesLegendValuesGenerator;

    private final boolean stackMode;

    public TbXYBarRenderer(boolean stackMode) {
        super();
        this.stackMode = stackMode;
        itemBorderRadiusMap = new HashMap<>();
        defaultItemBorderRadius = 0f;
        this.itemLabelsBackgroundVisibleList = new BooleanList();
        this.defaultItemLabelsBackgroundVisible = false;
        this.itemLabelsBackgroundPaintList = new PaintList();
        this.defaultItemLabelBackgroundPaint = safeParseCssColor("rgba(255,255,255,0.56)");
        this.thresholdPainter = new TbThresholdPainter();

        this.seriesLabelsVisibleList = new BooleanList();
        this.defaultSeriesLabelsVisible = false;

        this.seriesLabelFontMap = new HashMap<>();
        this.defaultSeriesLabelFont = newFont("sans-serif", Font.PLAIN, 10);

        this.seriesLabelPaintList = new PaintList();
        this.defaultSeriesLabelPaint = Color.BLACK;

        this.seriesLabelGeneratorMap = new HashMap<>();
    }

    public boolean getStackMode() {
        return this.stackMode;
    }

    public boolean isSeriesLabelsVisible(int series) {
        Boolean b = this.seriesLabelsVisibleList.getBoolean(series);
        if (b == null) {
            return this.defaultSeriesLabelsVisible;
        }
        return b;
    }

    public void setSeriesLabelsVisible(int series, boolean visible) {
        setSeriesLabelsVisible(series, Boolean.valueOf(visible));
    }

    public void setSeriesLabelsVisible(int series, Boolean visible) {
        setSeriesLabelsVisible(series, visible, true);
    }

    public void setSeriesLabelsVisible(int series, Boolean visible,
                                           boolean notify) {
        this.seriesLabelsVisibleList.setBoolean(series, visible);
        if (notify) {
            fireChangeEvent();
        }
    }

    public void clearSeriesLabelsVisible(boolean notify) {
        this.seriesLabelsVisibleList.clear();
        if (notify) {
            fireChangeEvent();
        }
    }

    public boolean getDefaultSeriesLabelsVisible() {
        return this.defaultSeriesLabelsVisible;
    }

    public void setDefaultSeriesLabelsVisible(boolean visible) {
        setDefaultSeriesLabelsVisible(visible, true);
    }

    public void setDefaultSeriesLabelsVisible(boolean visible, boolean notify) {
        this.defaultSeriesLabelsVisible = visible;
        if (notify) {
            fireChangeEvent();
        }
    }

    public Font getSeriesLabelFont(int row, int column) {
        Font result = getSeriesLabelFont(row);
        if (result == null) {
            result = this.defaultSeriesLabelFont;
        }
        return result;
    }

    public Font getSeriesLabelFont(int series) {
        return this.seriesLabelFontMap.get(series);
    }

    public void setSeriesLabelFont(int series, Font font) {
        setSeriesLabelFont(series, font, true);
    }

    public void setSeriesLabelFont(int series, Font font, boolean notify) {
        this.seriesLabelFontMap.put(series, font);
        if (notify) {
            fireChangeEvent();
        }
    }

    public void clearSeriesLabelFonts(boolean notify) {
        this.seriesLabelFontMap.clear();
        if (notify) {
            fireChangeEvent();
        }
    }

    public Font getDefaultSeriesLabelFont() {
        return this.defaultSeriesLabelFont;
    }

    public void setDefaultSeriesLabelFont(Font font) {
        Args.nullNotPermitted(font, "font");
        setDefaultSeriesLabelFont(font, true);
    }

    public void setDefaultSeriesLabelFont(Font font, boolean notify) {
        this.defaultSeriesLabelFont = font;
        if (notify) {
            fireChangeEvent();
        }
    }

    public Paint getSeriesLabelPaint(int row, int column) {
        Paint result = getSeriesLabelPaint(row);
        if (result == null) {
            result = this.defaultSeriesLabelPaint;
        }
        return result;
    }

    public Paint getSeriesLabelPaint(int series) {
        return this.seriesLabelPaintList.getPaint(series);
    }

    public void setSeriesLabelPaint(int series, Paint paint) {
        setSeriesLabelPaint(series, paint, true);
    }

    public void setSeriesLabelPaint(int series, Paint paint,
                                    boolean notify) {
        this.seriesLabelPaintList.setPaint(series, paint);
        if (notify) {
            fireChangeEvent();
        }
    }

    public void clearSeriesLabelPaints(boolean notify) {
        this.seriesLabelPaintList.clear();
        if (notify) {
            fireChangeEvent();
        }
    }

    public Paint getDefaultSeriesLabelPaint() {
        return this.defaultSeriesLabelPaint;
    }

    public void setDefaultSeriesLabelPaint(Paint paint) {
        setDefaultSeriesLabelPaint(paint, true);
    }

    public void setDefaultSeriesLabelPaint(Paint paint, boolean notify) {
        Args.nullNotPermitted(paint, "paint");
        this.defaultSeriesLabelPaint = paint;
        if (notify) {
            fireChangeEvent();
        }
    }

    public XYSeriesLabelGenerator getSeriesLabelGenerator(int series) {
        XYSeriesLabelGenerator generator = this.seriesLabelGeneratorMap.get(series);
        if (generator == null) {
            generator = this.defaultSeriesLabelGenerator;
        }
        return generator;
    }

    public void setSeriesLabelGenerator(int series,
                                        XYSeriesLabelGenerator generator) {
        this.seriesLabelGeneratorMap.put(series, generator);
        fireChangeEvent();
    }

    public XYSeriesLabelGenerator getDefaultSeriesLabelGenerator() {
        return this.defaultSeriesLabelGenerator;
    }

    public void setDefaultSeriesLabelGenerator(XYSeriesLabelGenerator generator) {
        this.defaultSeriesLabelGenerator = generator;
        fireChangeEvent();
    }

    public Float getItemBorderRadius(int row, int column) {
        Float result = getSeriesItemBorderRadius(row);
        if (result == null) {
            result = this.defaultItemBorderRadius;
        }
        return result;
    }

    public Float getSeriesItemBorderRadius(int series) {
        return this.itemBorderRadiusMap.get(series);
    }

    public void setSeriesItemBorderRadius(int series, Float borderRadius) {
        setSeriesItemBorderRadius(series, borderRadius, true);
    }

    public void setSeriesItemBorderRadius(int series, Float borderRadius,
                                          boolean notify) {
        this.itemBorderRadiusMap.put(series, borderRadius);
        if (notify) {
            fireChangeEvent();
        }
    }

    public Float getDefaultItemBorderRadius() {
        return this.defaultItemBorderRadius;
    }

    public void setDefaultItemBorderRadius(Float borderRadius) {
        // defer argument checking...
        setDefaultItemBorderRadius(borderRadius, true);
    }

    public void setDefaultItemBorderRadius(Float borderRadius, boolean notify) {
        Args.nullNotPermitted(borderRadius, "borderRadius");
        this.defaultItemBorderRadius = borderRadius;
        if (notify) {
            fireChangeEvent();
        }
    }

    public boolean isItemLabelBackgroundVisible(int row, int column) {
        return isSeriesItemLabelsBackgroundVisible(row);
    }

    public boolean isSeriesItemLabelsBackgroundVisible(int series) {
        Boolean b = this.itemLabelsBackgroundVisibleList.getBoolean(series);
        if (b == null) {
            return this.defaultItemLabelsBackgroundVisible;
        }
        return b;
    }

    public void setSeriesItemLabelsBackgroundVisible(int series, boolean visible) {
        setSeriesItemLabelsBackgroundVisible(series, Boolean.valueOf(visible));
    }

    public void setSeriesItemLabelsBackgroundVisible(int series, Boolean visible) {
        setSeriesItemLabelsBackgroundVisible(series, visible, true);
    }

    public void setSeriesItemLabelsBackgroundVisible(int series, Boolean visible,
                                                     boolean notify) {
        this.itemLabelsBackgroundVisibleList.setBoolean(series, visible);
        if (notify) {
            fireChangeEvent();
        }
    }

    public boolean getDefaultItemLabelsBackgroundVisible() {
        return this.defaultItemLabelsBackgroundVisible;
    }

    public void setDefaultItemLabelsBackgroundVisible(boolean visible) {
        setDefaultItemLabelsBackgroundVisible(visible, true);
    }

    public void setDefaultItemLabelsBackgroundVisible(boolean visible, boolean notify) {
        this.defaultItemLabelsBackgroundVisible = visible;
        if (notify) {
            fireChangeEvent();
        }
    }

    public Paint getItemLabelBackgroundPaint(int row, int column) {
        Paint result = getSeriesItemLabelsBackgroundPaint(row);
        if (result == null) {
            result = this.defaultItemLabelBackgroundPaint;
        }
        return result;
    }

    public Paint getSeriesItemLabelsBackgroundPaint(int series) {
        return this.itemLabelsBackgroundPaintList.getPaint(series);
    }

    public void setSeriesItemLabelsBackgroundPaint(int series, Paint paint) {
        setSeriesItemLabelsBackgroundPaint(series, paint, true);
    }

    public void setSeriesItemLabelsBackgroundPaint(int series, Paint paint,
                                                   boolean notify) {
        this.itemLabelsBackgroundPaintList.setPaint(series, paint);
        if (notify) {
            fireChangeEvent();
        }
    }

    public Paint getDefaultItemLabelsBackgroundPaint() {
        return this.defaultItemLabelBackgroundPaint;
    }

    public void setDefaultItemLabelsBackgroundPaint(Paint paint) {
        // defer argument checking...
        setDefaultItemLabelsBackgroundPaint(paint, true);
    }

    public void setDefaultItemLabelsBackgroundPaint(Paint paint, boolean notify) {
        Args.nullNotPermitted(paint, "paint");
        this.defaultItemLabelBackgroundPaint = paint;
        if (notify) {
            fireChangeEvent();
        }
    }

    @Override
    public TbSeriesLegendValuesGenerator getTbSeriesLegendValuesGenerator() {
        return seriesLegendValuesGenerator;
    }

    @Override
    public void setTbSeriesLegendValuesGenerator(TbSeriesLegendValuesGenerator seriesLegendValuesGenerator) {
        this.seriesLegendValuesGenerator = seriesLegendValuesGenerator;
    }

    @Override
    public int getPassCount() {
        return 2;
    }

    @Override
    public Range findRangeBounds(XYDataset dataset) {
        if (this.stackMode) {
            if (dataset != null) {
                return DatasetUtils.findStackedRangeBounds(
                        (TableXYDataset) dataset);
            } else {
                return null;
            }
        } else {
            return super.findRangeBounds(dataset);
        }
    }

    @Override
    public void drawItem(Graphics2D g2, XYItemRendererState state,
                         Rectangle2D dataArea, PlotRenderingInfo info, XYPlot plot,
                         ValueAxis domainAxis, ValueAxis rangeAxis, XYDataset dataset,
                         int series, int item, CrosshairState crosshairState, int pass) {

        if (!getItemVisible(series, item)) {
            return;
        }
        IntervalXYDataset intervalDataset = (IntervalXYDataset) dataset;

        double value0;
        double value1;
        if (this.stackMode) {
            double value = intervalDataset.getYValue(series, item);
            if (Double.isNaN(value)) {
                return;
            }
            double positiveBase = 0.0;
            double negativeBase = 0.0;
            for (int i = 0; i < series; i++) {
                double v = dataset.getYValue(i, item);
                if (!Double.isNaN(v) && isSeriesVisible(i)) {
                    if (v > 0) {
                        positiveBase = positiveBase + v;
                    }
                    else {
                        negativeBase = negativeBase + v;
                    }
                }
            }
            if (value > 0.0) {
                value0 = positiveBase;
                value1 = positiveBase + value;
            } else {
                value0 = negativeBase;
                value1 = negativeBase + value;
            }
        } else {
            if (this.getUseYInterval()) {
                value0 = intervalDataset.getStartYValue(series, item);
                value1 = intervalDataset.getEndYValue(series, item);
            } else {
                value0 = this.getBase();
                value1 = intervalDataset.getYValue(series, item);
            }
            if (Double.isNaN(value0) || Double.isNaN(value1)) {
                return;
            }
            if (value0 <= value1) {
                if (!rangeAxis.getRange().intersects(value0, value1)) {
                    return;
                }
            } else {
                if (!rangeAxis.getRange().intersects(value1, value0)) {
                    return;
                }
            }
        }

        double translatedValue0 = rangeAxis.valueToJava2D(value0, dataArea,
                plot.getRangeAxisEdge());
        double translatedValue1 = rangeAxis.valueToJava2D(value1, dataArea,
                plot.getRangeAxisEdge());
        double bottom = Math.min(translatedValue0, translatedValue1);
        double top = Math.max(translatedValue0, translatedValue1);

        double startX = intervalDataset.getStartXValue(series, item);
        if (Double.isNaN(startX)) {
            return;
        }
        double endX = intervalDataset.getEndXValue(series, item);
        if (Double.isNaN(endX)) {
            return;
        }
        if (startX <= endX) {
            if (!domainAxis.getRange().intersects(startX, endX)) {
                return;
            }
        } else {
            if (!domainAxis.getRange().intersects(endX, startX)) {
                return;
            }
        }

        // is there an alignment adjustment to be made?
        if (this.getBarAlignmentFactor() >= 0.0 && this.getBarAlignmentFactor() <= 1.0) {
            double x = intervalDataset.getXValue(series, item);
            double interval = endX - startX;
            startX = x - interval * this.getBarAlignmentFactor();
            endX = startX + interval;
        }

        RectangleEdge location = plot.getDomainAxisEdge();
        double translatedStartX = domainAxis.valueToJava2D(startX, dataArea,
                location);
        double translatedEndX = domainAxis.valueToJava2D(endX, dataArea,
                location);

        double translatedWidth = Math.max(1, Math.abs(translatedEndX
                - translatedStartX));

        double left = Math.min(translatedStartX, translatedEndX);
        if (getMargin() > 0.0) {
            double cut = translatedWidth * getMargin();
            translatedWidth = translatedWidth - cut;
            left = left + cut / 2;
        }


        PlotOrientation orientation = plot.getOrientation();
        boolean positive = (value1 > 0.0);
        boolean inverted = rangeAxis.isInverted();

        Float borderRadius = getItemBorderRadius(series, item);
        double radiusTopLeft = 0.0;
        double radiusTopRight = 0.0;
        double radiusBottomRight = 0.0;
        double radiusBottomLeft = 0.0;

        if (orientation.isHorizontal()) {
            if (positive && inverted || !positive && !inverted) {
                radiusTopLeft = borderRadius;
                radiusBottomLeft = borderRadius;
            } else {
                radiusTopRight = borderRadius;
                radiusBottomRight = borderRadius;
            }
        } else {
            if (positive && !inverted || !positive && inverted) {
                radiusTopLeft = borderRadius;
                radiusTopRight = borderRadius;
            } else {
                radiusBottomLeft = borderRadius;
                radiusBottomRight = borderRadius;
            }
        }

        Shape bar = null;
        if (orientation.isHorizontal()) {
            // clip left and right bounds to data area
            bottom = Math.max(bottom, dataArea.getMinX());
            top = Math.min(top, dataArea.getMaxX());
            bar = ChartUtils.createRectangularShapeWithRoundedCorners(bottom, left, top - bottom, translatedWidth,
                    radiusTopLeft, radiusTopRight, radiusBottomLeft, radiusBottomRight);
        } else if (orientation.isVertical()) {
            // clip top and bottom bounds to data area
            bottom = Math.max(bottom, dataArea.getMinY());
            top = Math.min(top, dataArea.getMaxY());
            bar = ChartUtils.createRectangularShapeWithRoundedCorners(left, bottom, translatedWidth, top - bottom,
                    radiusTopLeft, radiusTopRight, radiusBottomLeft, radiusBottomRight);
        }

        if (pass == 0) {
            if (state.getElementHinting()) {
                beginElementGroup(g2, dataset.getSeriesKey(series), item);
            }
            paintBar(g2, series, item, bar);
            if (state.getElementHinting()) {
                endElementGroup(g2);
            }
        } else if (pass == 1) {
            Shape savedClip = g2.getClip();
            g2.setClip(null);
            try {
                Rectangle2D labelBounds = null;
                if (isItemLabelVisible(series, item)) {
                    XYItemLabelGenerator generator = getItemLabelGenerator(series,
                            item);
                    if (generator != null) {
                        String label = generator.generateLabel(dataset, series, item);
                        if (label != null) {
                            Font labelFont = getItemLabelFont(series, item);
                            Paint labelPaint = getItemLabelPaint(series, item);
                            labelBounds = drawLabel(g2, series, item, plot, label, labelFont, labelPaint, bar.getBounds2D(),
                                    null, value1 < 0.0);
                        }
                    }
                }
                if (isSeriesLabelsVisible(series)) {
                    XYSeriesLabelGenerator generator = getSeriesLabelGenerator(series);
                    if (generator != null) {
                        String label = generator.generateLabel(dataset, series);
                        if (label != null) {
                            Font labelFont = getSeriesLabelFont(series, item);
                            Paint labelPaint = getSeriesLabelPaint(series, item);
                            drawLabel(g2, series, item, plot, label, labelFont, labelPaint, bar.getBounds2D(),
                                    labelBounds, value1 < 0.0);
                        }
                    }
                }

                // update the crosshair point
                double x1 = (startX + endX) / 2.0;
                double y1 = dataset.getYValue(series, item);
                double transX1 = domainAxis.valueToJava2D(x1, dataArea, location);
                int datasetIndex = plot.indexOf(dataset);
                updateCrosshairValues(crosshairState, x1, y1, datasetIndex,
                        transX1, translatedValue1, plot.getOrientation());

                EntityCollection entities = state.getEntityCollection();
                if (entities != null) {
                    addEntity(entities, bar, dataset, series, item, 0.0, 0.0);
                }
            } finally {
                g2.setClip(savedClip);
            }
        }
    }

    @Override
    public void drawRangeMarker(Graphics2D g2, XYPlot plot, ValueAxis rangeAxis,
                                Marker marker, Rectangle2D dataArea) {
        if (marker instanceof TbThresholdMarker) {
            this.thresholdPainter.paintThresholdMarker(g2, plot, rangeAxis, dataArea, (TbThresholdMarker)marker);
        } else {
            super.drawRangeMarker(g2, plot, rangeAxis, marker, dataArea);
        }
    }

    @Override
    public TbLegendItem getTbLegendItem(TbLegendValuesRequest request, int datasetIndex, int series) {
        if (this.seriesLegendValuesGenerator != null) {
            LegendItem legendItem = this.getLegendItem(datasetIndex, series);
            if (legendItem != null) {
                XYPlot plot = getPlot();
                XYDataset dataset = plot.getDataset(datasetIndex);
                TbLegendValues legendValues = this.seriesLegendValuesGenerator.generateLegendValues(request, dataset, series);
                return new TbLegendItem(legendItem, legendValues);
            }
        }
        return null;
    }

    protected void paintBar(Graphics2D g2, int row, int column, Shape bar) {
        Paint itemPaint = getItemPaint(row, column);
        GradientPaintTransformer t = getGradientPaintTransformer();
        if (t != null && itemPaint instanceof GradientPaint) {
            itemPaint = t.transform((GradientPaint) itemPaint, bar);
        }
        g2.setPaint(itemPaint);
        g2.fill(bar);
        if (isDrawBarOutline()) {
            Stroke stroke = getItemOutlineStroke(row, column);
            Paint paint = getItemOutlinePaint(row, column);
            if (stroke != null && paint != null) {
                g2.setStroke(stroke);
                g2.setPaint(paint);
                g2.draw(bar);
            }
        }
    }

    protected Rectangle2D drawLabel(Graphics2D g,
                                    int series, int item, XYPlot plot, String label,
                                    Font labelFont, Paint labelPaint,
                                    Rectangle2D bar, Rectangle2D prevLabelBounds, boolean negative) {

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setFont(labelFont);

        // find out where to place the label...
        ItemLabelPosition position;
        if (!negative) {
            position = getPositiveItemLabelPosition(series, item);
        } else {
            position = getNegativeItemLabelPosition(series, item);
        }

        Rectangle2D drawBar = bar;

        if (position.getItemLabelAnchor().isInternal()) {
            if (isShowLabelInsideVisibleBar() && g2.getClipBounds() != null) {
                drawBar = drawBar.createIntersection(g2.getClipBounds().getBounds2D());
            }

            Rectangle2D labelBar = getItemLabelInsets().createInsetRectangle(drawBar);
            if (getMinimumLabelSize() != null &&
                    (labelBar.getWidth() < getMinimumLabelSize().getWidth()
                            || labelBar.getHeight() < getMinimumLabelSize().getHeight())) {
                return null; // nothing to do
            }
        }

        // work out the label anchor point...
        Point2D anchorPoint = calculateLabelAnchorPoint(
                position.getItemLabelAnchor(), drawBar, plot.getOrientation());

        String drawLabel = calculateLabeltoDraw(
                label, anchorPoint, position, drawBar, g2);

        if (drawLabel == null) {
            if (!negative) {
                position = getPositiveItemLabelPositionFallback();
            } else {
                position = getNegativeItemLabelPositionFallback();
            }
            if (position != null) {
                g2 = (Graphics2D) g.create();
                g2.setFont(labelFont);

                if (position.getItemLabelAnchor().isInternal()) {
                    if (isShowLabelInsideVisibleBar() && g2.getClipBounds() != null) {
                        drawBar = drawBar.createIntersection(g2.getClipBounds().getBounds2D());
                    }

                    Rectangle2D labelBar = getItemLabelInsets().createInsetRectangle(drawBar);
                    if (getMinimumLabelSize() != null &&
                            (labelBar.getWidth() < getMinimumLabelSize().getWidth()
                                    || labelBar.getHeight() < getMinimumLabelSize().getHeight())) {
                        return null; // nothing to do
                    }
                }

                anchorPoint = calculateLabelAnchorPoint(
                        position.getItemLabelAnchor(), drawBar, plot.getOrientation());

                drawLabel = calculateLabeltoDraw(
                        label, anchorPoint, position, drawBar, g2);
            }
        }

        if (drawLabel != null) {
            float x = (float) anchorPoint.getX();
            float y = (float) anchorPoint.getY();
            boolean drawBackground = isItemLabelBackgroundVisible(series, item);
            if (position.getItemLabelAnchor().isInternal()) {
                y -= 15;
                if (prevLabelBounds != null) {
                    y -= (3 + (float)prevLabelBounds.getHeight());
                }
            } else {
                float distance = drawBackground ? 5 : 3;
                if (position.getTextAnchor() == TextAnchor.BOTTOM_CENTER) {
                    y -= distance;
                } else if (position.getTextAnchor() == TextAnchor.TOP_CENTER) {
                    y += distance;
                }
            }

            Rectangle2D bounds = TextUtils.calculateRotatedStringBounds(drawLabel, g2,
                    x, y,
                    position.getTextAnchor(), position.getAngle(),
                    position.getRotationAnchor()).getBounds2D();
            if (position.getItemLabelAnchor().isInternal()) {
                if (bar.getWidth() - bounds.getWidth() < 0) {
                    return null;
                }
            }
            if (drawBackground) {
                g2.setPaint(getItemLabelBackgroundPaint(series, item));
                g2.setStroke(new BasicStroke(0));
                g2.fillRoundRect((int)bounds.getX()-3, (int)bounds.getY()-1, (int)bounds.getWidth()+6, (int)bounds.getHeight()+4, 4, 4 );
            }

            g2.setPaint(labelPaint);
            TextUtils.drawRotatedString(drawLabel, g2,
                    x, y,
                    position.getTextAnchor(), position.getAngle(),
                    position.getRotationAnchor());
            return bounds;
        }
        return null;
    }

    private String calculateLabeltoDraw(String label, Point2D anchorPoint,
                                        ItemLabelPosition position, Rectangle2D bar, Graphics2D g2) {
        if (!position.getItemLabelAnchor().isInternal()) {
            return label;
        }

        Rectangle2D labelBar = getItemLabelInsets().createInsetRectangle(bar).getBounds();

        switch (position.getItemLabelClip()) {
            case CLIP :
                Shape currentClip = g2.getClip();
                if (currentClip == null) {
                    g2.setClip(labelBar);
                } else {
                    g2.setClip(labelBar
                            .createIntersection(currentClip.getBounds2D()));
                }
                return label;
            case NONE :
                return label;
            default :
        }

        String result = label;
        while (result != null && !result.isEmpty()) {
            Rectangle2D labelBounds = TextUtils.calculateRotatedStringBounds(result,
                    g2, (float) anchorPoint.getX(), (float) anchorPoint.getY(),
                    position.getTextAnchor(), position.getAngle(),
                    position.getRotationAnchor()).getBounds2D();

            if (labelBar.getHeight() >= labelBounds.getHeight()
                    && labelBar.getWidth() >= labelBounds.getWidth()) {
                // Label fits
                return result;
            } else if (labelBar.getHeight() < labelBounds.getHeight()) {
                // Optimization: label will never fit due to insufficient height
                return null;
            } else {
                switch (position.getItemLabelClip()) {
                    case FIT :
                        return null;
                    case TRUNCATE : {
                        String nextResult = result.replaceFirst(".(\\.{3})?$",
                                "...");
                        if ("...".equals(nextResult) || result.equals(nextResult)) {
                            return null;
                        } else {
                            result = nextResult;
                        }
                        break;
                    }
                    case TRUNCATE_WORD : {
                        String nextResult = result
                                .replaceFirst("\\W+\\w*(\\.{3})?$", "...");
                        if ("...".equals(nextResult) || result.equals(nextResult)) {
                            return null;
                        } else {
                            result = nextResult;
                        }
                        break;
                    }
                    default :
                        throw new IllegalStateException("Should never happen");
                }
            }
        }
        return null;
    }

    private Point2D calculateLabelAnchorPoint(ItemLabelAnchor anchor,
                                              Rectangle2D bar, PlotOrientation orientation) {

        Point2D result = null;
        RectangleInsets labelInsets = getItemLabelInsets();
        Rectangle2D insideBar = labelInsets.createInsetRectangle(bar);
        Rectangle2D outsideBar = labelInsets.createOutsetRectangle(bar);

        if (anchor == ItemLabelAnchor.CENTER) {
            result = new Point2D.Double(bar.getCenterX(), bar.getCenterY());
        } else if (anchor == ItemLabelAnchor.INSIDE1 || anchor == ItemLabelAnchor.INSIDE2) {
            result = new Point2D.Double(insideBar.getMaxX(), insideBar.getMinY());
        } else if (anchor == ItemLabelAnchor.INSIDE3) {
            result = new Point2D.Double(insideBar.getMaxX(), bar.getCenterY());
        } else if (anchor == ItemLabelAnchor.INSIDE4 || anchor == ItemLabelAnchor.INSIDE5) {
            result = new Point2D.Double(insideBar.getMaxX(), insideBar.getMaxY());
        } else if (anchor == ItemLabelAnchor.INSIDE6) {
            result = new Point2D.Double(bar.getCenterX(), insideBar.getMaxY());
        } else if (anchor == ItemLabelAnchor.INSIDE7 || anchor == ItemLabelAnchor.INSIDE8) {
            result = new Point2D.Double(insideBar.getMinX(), insideBar.getMaxY());
        } else if (anchor == ItemLabelAnchor.INSIDE9) {
            result = new Point2D.Double(insideBar.getMinX(), bar.getCenterY());
        } else if (anchor == ItemLabelAnchor.INSIDE10 || anchor == ItemLabelAnchor.INSIDE11) {
            result = new Point2D.Double(insideBar.getMinX(), insideBar.getMinY());
        } else if (anchor == ItemLabelAnchor.INSIDE12) {
            result = new Point2D.Double(bar.getCenterX(), insideBar.getMinY());
        } else if (anchor == ItemLabelAnchor.OUTSIDE1 || anchor == ItemLabelAnchor.OUTSIDE2) {
            result = new Point2D.Double(outsideBar.getMaxX(), outsideBar.getMinY());
        } else if (anchor == ItemLabelAnchor.OUTSIDE3) {
            result = new Point2D.Double(outsideBar.getMaxX(), bar.getCenterY());
        } else if (anchor == ItemLabelAnchor.OUTSIDE4 || anchor == ItemLabelAnchor.OUTSIDE5) {
            result = new Point2D.Double(outsideBar.getMaxX(), outsideBar.getMaxY());
        } else if (anchor == ItemLabelAnchor.OUTSIDE6) {
            result = new Point2D.Double(bar.getCenterX(), outsideBar.getMaxY());
        } else if (anchor == ItemLabelAnchor.OUTSIDE7 || anchor == ItemLabelAnchor.OUTSIDE8) {
            result = new Point2D.Double(outsideBar.getMinX(), outsideBar.getMaxY());
        } else if (anchor == ItemLabelAnchor.OUTSIDE9) {
            result = new Point2D.Double(outsideBar.getMinX(), bar.getCenterY());
        } else if (anchor == ItemLabelAnchor.OUTSIDE10 || anchor == ItemLabelAnchor.OUTSIDE11) {
            result = new Point2D.Double(outsideBar.getMinX(), outsideBar.getMinY());
        } else if (anchor == ItemLabelAnchor.OUTSIDE12) {
            result = new Point2D.Double(bar.getCenterX(), outsideBar.getMinY());
        }

        return result;

    }

}
