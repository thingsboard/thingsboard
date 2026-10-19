// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.report.renderer.chart;

import org.jfree.chart.LegendItem;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.entity.EntityCollection;
import org.jfree.chart.labels.ItemLabelPosition;
import org.jfree.chart.labels.XYItemLabelGenerator;
import org.jfree.chart.plot.CrosshairState;
import org.jfree.chart.plot.Marker;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRendererState;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.text.TextUtils;
import org.jfree.chart.ui.GradientPaintTransformer;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.ui.StandardGradientPaintTransformer;
import org.jfree.chart.ui.TextAnchor;
import org.jfree.chart.util.Args;
import org.jfree.chart.util.BooleanList;
import org.jfree.chart.util.LineUtils;
import org.jfree.chart.util.PaintList;
import org.jfree.chart.util.ShapeUtils;
import org.jfree.data.Range;
import org.jfree.data.xy.TableXYDataset;
import org.jfree.data.xy.XYDataset;
import org.thingsboard.server.report.renderer.chart.legend.TbLegendItem;
import org.thingsboard.server.report.renderer.chart.legend.TbLegendValues;
import org.thingsboard.server.report.renderer.chart.legend.TbLegendValuesRequest;
import org.thingsboard.server.report.renderer.chart.legend.TbSeriesLegendValuesGenerator;

import java.awt.BasicStroke;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static org.thingsboard.server.report.renderer.chart.ChartUtils.interpolateBezier;
import static org.thingsboard.server.report.util.ColorUtils.safeParseCssColor;

public class TbXYLineAndShapeRenderer extends XYLineAndShapeRenderer implements TbItemRenderer, TbVisualMapRenderer {

    public enum LineInterpolationType {
        NONE,
        STEP,
        SMOOTH
    }

    public enum FillType {

        /** No fill. */
        NONE,

        /** Fill down to zero. */
        TO_ZERO,

        /** Fill to the lower bound. */
        TO_LOWER_BOUND,

        /** Fill to the upper bound. */
        TO_UPPER_BOUND
    }

    public static class TbXYItemRenderState extends State {

        /** The area to fill under the curve. */
        public GeneralPath fillArea;

        /** The points. */
        public java.util.List<Point2D> points;

        public List<Point2D> stackPoints;

        /**
         * Creates a new state instance.
         *
         * @param info  the plot rendering info.
         */
        public TbXYItemRenderState(PlotRenderingInfo info) {
            super(info);
            this.fillArea = new GeneralPath();
            this.points = new ArrayList<>();
            this.stackPoints = new ArrayList<>();
        }
    }

    private LineInterpolationType interpolationType;

    private FillType fillType;

    private GradientPaintTransformer gradientPaintTransformer;
    private final TbThresholdPainter thresholdPainter;

    private final PaintList shapeFillPaintList;
    private transient Paint defaultShapeFillPaint;

    private final BooleanList itemLabelsBackgroundVisibleList;
    private boolean defaultItemLabelsBackgroundVisible;
    private final PaintList itemLabelsBackgroundPaintList;
    private transient Paint defaultItemLabelBackgroundPaint;

    private TbSeriesLegendValuesGenerator seriesLegendValuesGenerator;

    private final boolean stackMode;

    private double stepPoint = 1.0d;
    private int precision = 1;
    private float smooth = 0f;

    private Paint currentVisualMapPaint;
    private Paint currentVisualMapFillPaint;
    private TbVisualMap visualMap;

    public TbXYLineAndShapeRenderer() {
        this(LineInterpolationType.NONE, FillType.NONE, false);
    }

    public TbXYLineAndShapeRenderer(LineInterpolationType interpolationType, FillType fillType, boolean stackMode) {
        super();
        Args.nullNotPermitted(interpolationType, "interpolationType");
        Args.nullNotPermitted(fillType, "fillType");
        this.interpolationType = interpolationType;
        this.fillType = fillType;
        this.stackMode = stackMode;
        this.shapeFillPaintList = new PaintList();
        this.itemLabelsBackgroundVisibleList = new BooleanList();
        this.defaultItemLabelsBackgroundVisible = false;
        this.itemLabelsBackgroundPaintList = new PaintList();
        this.defaultItemLabelBackgroundPaint = safeParseCssColor("rgba(255,255,255,0.56)");
        this.gradientPaintTransformer = new StandardGradientPaintTransformer();
        this.thresholdPainter = new TbThresholdPainter();
    }

    public LineInterpolationType getInterpolationType() {
        return this.interpolationType;
    }

    public void setInterpolationType(LineInterpolationType interpolationType) {
        this.interpolationType = interpolationType;
        fireChangeEvent();
    }

    public FillType getFillType() {
        return this.fillType;
    }

    public void setFillType(FillType fillType) {
        this.fillType = fillType;
        fireChangeEvent();
    }

    public GradientPaintTransformer getGradientPaintTransformer() {
        return this.gradientPaintTransformer;
    }

    public void setGradientPaintTransformer(GradientPaintTransformer gpt) {
        this.gradientPaintTransformer = gpt;
        fireChangeEvent();
    }

    public boolean getStackMode() {
        return this.stackMode;
    }

    public double getStepPoint() {
        return this.stepPoint;
    }

    public void setStepPoint(double stepPoint) {
        if (stepPoint < 0.0d || stepPoint > 1.0d) {
            throw new IllegalArgumentException(
                    "Requires stepPoint in [0.0;1.0]");
        }
        this.stepPoint = stepPoint;
        fireChangeEvent();
    }

    public int getPrecision() {
        return this.precision;
    }

    public void setPrecision(int p) {
        if (p <= 0) {
            throw new IllegalArgumentException("Requires p > 0.");
        }
        this.precision = p;
        fireChangeEvent();
    }
    public float getSmooth() {
        return this.smooth;
    }

    public void setSmooth(float s) {
        if (s < 0 || s > 1) {
            throw new IllegalArgumentException("Requires smooth in [0.0;1.0]");
        }
        this.smooth = s;
        fireChangeEvent();
    }

    @Override
    public int getAreaPass() {
        return 0;
    }

    @Override
    public int getItemPass() {
        return 1;
    }

    @Override
    public void setCurrentVisualMapPaint(Paint paint) {
        this.currentVisualMapPaint = paint;
    }

    @Override
    public void setCurrentVisualMapFillPaint(Paint paint) {
        this.currentVisualMapFillPaint = paint;
    }

    @Override
    public void setVisualMap(TbVisualMap visualMap) {
        this.visualMap = visualMap;
    }

    @Override
    public Paint getSeriesPaint(int series) {
        if (currentVisualMapPaint != null) {
            return currentVisualMapPaint;
        } else {
            return super.getSeriesPaint(series);
        }
    }

    @Override
    public Paint getSeriesFillPaint(int series) {
        if (this.currentVisualMapFillPaint != null) {
            return this.currentVisualMapFillPaint;
        } else {
            return super.getSeriesFillPaint(series);
        }
    }

    @Override
    public Range findRangeBounds(XYDataset dataset) {
        if (this.stackMode) {
            if (dataset == null) {
                return null;
            }
            double min = Double.POSITIVE_INFINITY;
            double max = Double.NEGATIVE_INFINITY;
            TableXYDataset d = (TableXYDataset) dataset;
            int itemCount = d.getItemCount();
            for (int i = 0; i < itemCount; i++) {
                double[] stackValues = getStackValues((TableXYDataset) dataset,
                        d.getSeriesCount(), i);
                min = Math.min(min, stackValues[0]);
                max = Math.max(max, stackValues[1]);
            }
            if (min == Double.POSITIVE_INFINITY) {
                return null;
            }
            return new Range(min, max);
        } else {
            return super.findRangeBounds(dataset);
        }
    }

    public Paint getShapeFillPaint(int series, int item, double value) {
        return lookupSeriesShapeFillPaint(series, value);
    }

    public Paint lookupSeriesShapeFillPaint(int series, double value) {
        Paint seriesShapesFillPaint = getSeriesShapesFillPaint(series);
        if (seriesShapesFillPaint == null) {
            if (this.visualMap != null) {
                seriesShapesFillPaint = this.visualMap.lookupPaint(value);
            }
            if (seriesShapesFillPaint == null) {
                seriesShapesFillPaint = defaultShapeFillPaint;
                if (seriesShapesFillPaint == null) {
                    seriesShapesFillPaint = lookupSeriesPaint(series);
                }
            }
        }
        return seriesShapesFillPaint;
    }

    public Paint getSeriesShapesFillPaint(int series) {
        return this.shapeFillPaintList.getPaint(series);
    }

    public void setSeriesShapesFillPaint(int series, Paint paint) {
        setSeriesShapesFillPaint(series, paint, true);
    }

    public void setSeriesShapesFillPaint(int series, Paint paint, boolean notify) {
        this.shapeFillPaintList.setPaint(series, paint);
        if (notify) {
            fireChangeEvent();
        }
    }

    public void setDefaultShapeFillPaint(Paint paint) {
        this.defaultShapeFillPaint = paint;
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
    public TbXYItemRenderState initialise(Graphics2D g2, Rectangle2D dataArea,
                                          XYPlot plot, XYDataset data, PlotRenderingInfo info) {

        TbXYItemRenderState state = new TbXYItemRenderState(info);
        state.setProcessVisibleItemsOnly(false);
        return state;
    }

    @Override
    public void drawItem(Graphics2D g2, XYItemRendererState state,
                         Rectangle2D dataArea, PlotRenderingInfo info, XYPlot plot,
                         ValueAxis domainAxis, ValueAxis rangeAxis, XYDataset dataset,
                         int series, int item, CrosshairState crosshairState, int pass) {

        // do nothing if item is not visible
        if (!getItemVisible(series, item)) {
            return;
        }

        // first pass draws the background (lines, for instance)
        if (isLinePass(pass)) {
            if (getItemLineVisible(series, item) || this.fillType != FillType.NONE) {
                this.drawLineAndArea(state, g2, plot, dataset, pass,
                        series, item, domainAxis, rangeAxis, dataArea);
            }
        } else {
            super.drawItem(g2, state, dataArea, info, plot, domainAxis, rangeAxis, dataset, series, item, crosshairState, pass);
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

    private void drawLineAndArea(XYItemRendererState state,
                                 Graphics2D g2, XYPlot plot, XYDataset dataset, int pass,
                                 int series, int item, ValueAxis xAxis, ValueAxis yAxis,
                                 Rectangle2D dataArea) {
        TbXYItemRenderState s = (TbXYItemRenderState) state;
        RectangleEdge xAxisLocation = plot.getDomainAxisEdge();
        RectangleEdge yAxisLocation = plot.getRangeAxisEdge();

        // get the data points
        double x1 = dataset.getXValue(series, item);
        double y1 = dataset.getYValue(series, item);

        if (!this.stackMode) {
            double transX1 = xAxis.valueToJava2D(x1, dataArea, xAxisLocation);
            double transY1 = yAxis.valueToJava2D(y1, dataArea, yAxisLocation);

            // Collect points
            if (!Double.isNaN(transX1) && !Double.isNaN(transY1)) {
                Point2D p = plot.getOrientation() == PlotOrientation.HORIZONTAL
                        ? new Point2D.Float((float) transY1, (float) transX1)
                        : new Point2D.Float((float) transX1, (float) transY1);
                if (!s.points.contains(p))
                    s.points.add(p);
            }
        } else {
            this.prepareStackModePoints(s, plot, dataset, series, item, xAxis, yAxis, dataArea);
        }

        if (item == dataset.getItemCount(series) - 1) {     // construct path
            if (s.points.size() > 1) {
                this.sortPoints(s, plot.getOrientation());
                List<Point2D> interpolated = this.interpolatePoints(s, dataArea, s.points);
                this.drawPointsToPath(interpolated, s.seriesPath, true);
                if (this.fillType != FillType.NONE) {
                    List<Point2D> stackPoints = null;
                    if (this.stackMode) {
                        stackPoints = this.interpolatePoints(s, dataArea, s.stackPoints);
                    }
                    this.drawFillArea(plot, xAxis, yAxis, dataArea, s.fillArea, interpolated, stackPoints);
                    Paint fp = getSeriesFillPaint(series);
                    if (this.gradientPaintTransformer != null
                            && fp instanceof GradientPaint) {
                        GradientPaint gp = this.gradientPaintTransformer
                                .transform((GradientPaint) fp, s.fillArea);
                        g2.setPaint(gp);
                    } else {
                        g2.setPaint(fp);
                    }
                    g2.fill(s.fillArea);
                    s.fillArea.reset();
                }
                if (getItemLineVisible(series, item)) {
                    // then draw the line...
                    drawFirstPassShape(g2, pass, series, item, s.seriesPath);
                }
            }
            // reset points vector
            s.points = new ArrayList<>();
            s.stackPoints = new ArrayList<>();
        }
    }

    @Override
    protected void drawSecondaryPass(Graphics2D g2, XYPlot plot,
                                     XYDataset dataset, int pass, int series, int item,
                                     ValueAxis domainAxis, Rectangle2D dataArea, ValueAxis rangeAxis,
                                     CrosshairState crosshairState, EntityCollection entities) {

        Shape savedClip = g2.getClip();
        g2.setClip(null);
        try {
            Shape entityArea = null;

            // get the data point...
            double x1 = dataset.getXValue(series, item);
            double y1 = dataset.getYValue(series, item);
            if (Double.isNaN(y1) || Double.isNaN(x1)) {
                return;
            }

            PlotOrientation orientation = plot.getOrientation();
            RectangleEdge xAxisLocation = plot.getDomainAxisEdge();
            RectangleEdge yAxisLocation = plot.getRangeAxisEdge();
            double transX1 = domainAxis.valueToJava2D(x1, dataArea, xAxisLocation);
            double transY1;
            if (this.stackMode) {
                RectangleEdge edge1 = plot.getRangeAxisEdge();
                TableXYDataset tdataset = (TableXYDataset) dataset;
                double[] stack = getStackValues(tdataset, series, item);
                transY1 = (float) rangeAxis.valueToJava2D(y1 + (y1 >= 0.0 ? stack[1] : stack[0]), dataArea,
                        edge1);
            } else {
                transY1 = rangeAxis.valueToJava2D(y1, dataArea, yAxisLocation);
            }

            if (getItemShapeVisible(series, item)) {
                Shape shape = getItemShape(series, item);
                if (orientation == PlotOrientation.HORIZONTAL) {
                    shape = ShapeUtils.createTranslatedShape(shape, transY1, transX1);
                } else if (orientation == PlotOrientation.VERTICAL) {
                    shape = ShapeUtils.createTranslatedShape(shape, transX1, transY1);
                }
                entityArea = shape;
                if (shape.intersects(dataArea)) {
                    if (getItemShapeFilled(series, item)) {
                        if (this.getUseFillPaint()) {
                            g2.setPaint(getItemFillPaint(series, item));
                        } else {
                            g2.setPaint(getShapeFillPaint(series, item, y1));
                        }
                        g2.fill(shape);
                    }
                    if (this.getDrawOutlines()) {
                        Paint outlinePaint = null;
                        if (this.visualMap != null) {
                            outlinePaint = this.visualMap.lookupPaint(y1);
                        }
                        if (outlinePaint == null) {
                            if (getUseOutlinePaint()) {
                                outlinePaint = getItemOutlinePaint(series, item);
                            } else {
                                outlinePaint = getItemPaint(series, item);
                            }
                        }
                        g2.setPaint(outlinePaint);
                        g2.setStroke(getItemOutlineStroke(series, item));
                        g2.draw(shape);
                    }
                }
            }

            double xx = transX1;
            double yy = transY1;
            if (orientation == PlotOrientation.HORIZONTAL) {
                xx = transY1;
                yy = transX1;
            }

            // draw the item label if there is one...
            if (isItemLabelVisible(series, item)) {
                drawItemLabel(g2, orientation, dataset, series, item, xx, yy,
                        entityArea != null ? entityArea.getBounds2D() : null,
                        (y1 < 0.0));
            }

            int datasetIndex = plot.indexOf(dataset);
            updateCrosshairValues(crosshairState, x1, y1, datasetIndex,
                    transX1, transY1, orientation);

            // add an entity for the item, but only if it falls within the data
            // area...
            if (entities != null && ShapeUtils.isPointInRect(dataArea, xx, yy)) {
                addEntity(entities, entityArea, dataset, series, item, xx, yy);
            }
        } finally {
            g2.setClip(savedClip);
        }
    }

    protected void drawItemLabel(Graphics2D g2, PlotOrientation orientation,
                                 XYDataset dataset, int series, int item, double x, double y,
                                 Rectangle2D itemShapeBounds,
                                 boolean negative) {

        XYItemLabelGenerator generator = getItemLabelGenerator(series, item);
        if (generator != null) {
            Font labelFont = getItemLabelFont(series, item);
            g2.setFont(labelFont);
            String label = generator.generateLabel(dataset, series, item);

            // get the label position..
            ItemLabelPosition position;
            if (!negative) {
                position = getPositiveItemLabelPosition(series, item);
            }
            else {
                position = getNegativeItemLabelPosition(series, item);
            }

            double shapeHeight = itemShapeBounds != null ? itemShapeBounds.getHeight() : 0.0;
            boolean drawBackground = isItemLabelBackgroundVisible(series, item);

            if (position.getTextAnchor() == TextAnchor.BOTTOM_CENTER) {
                y -= (5 + shapeHeight / 2);
                if (drawBackground) {
                    y -= 4;
                }
            } else if (position.getTextAnchor() == TextAnchor.TOP_CENTER) {
                y += (5 + shapeHeight / 2);
                if (drawBackground) {
                    y += 4;
                }
            }

            // work out the label anchor point...
            Point2D anchorPoint = calculateLabelAnchorPoint(
                    position.getItemLabelAnchor(), x, y, orientation);
            if (drawBackground) {
                Rectangle2D bounds = TextUtils.calculateRotatedStringBounds(label, g2,
                        (float) anchorPoint.getX(), (float) anchorPoint.getY(),
                        position.getTextAnchor(), position.getAngle(),
                        position.getRotationAnchor()).getBounds2D();
                g2.setPaint(getItemLabelBackgroundPaint(series, item));
                g2.setStroke(new BasicStroke(0));
                g2.fillRoundRect((int)bounds.getX()-3, (int)bounds.getY()-1, (int)bounds.getWidth()+6, (int)bounds.getHeight()+4, 4, 4 );
            }
            Paint paint = getItemLabelPaint(series, item);
            g2.setPaint(paint);
            TextUtils.drawRotatedString(label, g2,
                    (float) anchorPoint.getX(), (float) anchorPoint.getY(),
                    position.getTextAnchor(), position.getAngle(),
                    position.getRotationAnchor());
        }

    }

    @Override
    public LegendItem getLegendItem(int datasetIndex, int series) {
        XYPlot plot = getPlot();
        if (plot == null) {
            return null;
        }

        XYDataset dataset = plot.getDataset(datasetIndex);
        if (dataset == null) {
            return null;
        }

        if (!getItemVisible(series, 0)) {
            return null;
        }
        String label = getLegendItemLabelGenerator().generateLabel(dataset,
                series);
        String description = label;
        String toolTipText = null;
        if (getLegendItemToolTipGenerator() != null) {
            toolTipText = getLegendItemToolTipGenerator().generateLabel(
                    dataset, series);
        }
        String urlText = null;
        if (getLegendItemURLGenerator() != null) {
            urlText = getLegendItemURLGenerator().generateLabel(dataset,
                    series);
        }
        boolean shapeIsVisible = getItemShapeVisible(series, 0);
        Shape shape = lookupLegendShape(series);
        boolean shapeIsFilled = getItemShapeFilled(series, 0);
        Paint fillPaint = (this.getUseFillPaint() ? lookupSeriesFillPaint(series)
                : lookupSeriesShapeFillPaint(series, 0));
        boolean shapeOutlineVisible = this.getDrawOutlines();
        Paint outlinePaint = (this.getUseOutlinePaint() ? lookupSeriesOutlinePaint(
                series) : lookupSeriesPaint(series));
        Stroke outlineStroke = lookupSeriesOutlineStroke(series);
        boolean lineVisible = getItemLineVisible(series, 0);
        Stroke lineStroke = lookupSeriesStroke(series);
        Paint linePaint = lookupSeriesPaint(series);
        if (shape != null) {
            if (shape.getBounds().getHeight() >= 10) {
                double scaleFactor = 10.0 / shape.getBounds().getHeight();
                AffineTransform scaleTransform = AffineTransform.getScaleInstance(scaleFactor, scaleFactor);
                shape = scaleTransform.createTransformedShape(shape);
            }
        }
        LegendItem result = new LegendItem(label, description, toolTipText,
                urlText, shapeIsVisible, shape, shapeIsFilled, fillPaint,
                shapeOutlineVisible, outlinePaint, outlineStroke, lineVisible,
                this.getLegendLine(), lineStroke, linePaint);
        result.setLabelFont(lookupLegendTextFont(series));
        Paint labelPaint = lookupLegendTextPaint(series);
        if (labelPaint != null) {
            result.setLabelPaint(labelPaint);
        }
        result.setSeriesKey(dataset.getSeriesKey(series));
        result.setSeriesIndex(series);
        result.setDataset(dataset);
        result.setDatasetIndex(datasetIndex);

        return result;
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

    protected double[] getStackValues(TableXYDataset dataset,
                                    int series, int index) {
        double[] result = new double[2];
        for (int i = 0; i < series; i++) {
            double v = dataset.getYValue(i, index);
            if (!Double.isNaN(v)) {
                if (v >= 0.0) {
                    result[1] += v;
                }
                else {
                    result[0] += v;
                }
            }
        }
        return result;
    }

    protected List<Point2D> interpolatePoints(TbXYItemRenderState state, Rectangle2D dataArea, List<Point2D> points) {
        switch (this.interpolationType) {
            case STEP -> {
                return this.interpolateStep(state, dataArea, points);
            }
            case SMOOTH -> {
                if (this.smooth > 0) {
                    return interpolateBezier(points, this.smooth, this.precision);
                } else {
                    return points;
                }
            }
            default -> {
                return points;
            }
        }
    }

    private List<Point2D> interpolateStep(TbXYItemRenderState state, Rectangle2D dataArea, List<Point2D> points) {
        List<Point2D> interpolated = new ArrayList<>();
        if (points.size() > 1) {
            for (int item = 0; item < points.size(); item++) {
                double transX1 = points.get(item).getX();
                double transY1 = points.get(item).getY();
                if (item > 0) {
                    // get the previous data point...
                    double transX0 = points.get(item - 1).getX();
                    double transY0 = points.get(item - 1).getY();
                    if (transY0 == transY1) {
                        // for drawing a horizontal bar.
                        storeLine(state, dataArea, transX0, transY0, transX1,
                                transY1, interpolated);
                    } else { //this handles the need to perform a 'step'.
                        // calculate the step point
                        double transXs = transX0 + (this.stepPoint
                                * (transX1 - transX0));
                        storeLine(state, dataArea, transX0, transY0, transXs,
                                transY0, interpolated);
                        storeLine(state, dataArea, transXs, transY0, transXs,
                                transY1, interpolated);
                        storeLine(state, dataArea, transXs, transY1, transX1,
                                transY1, interpolated);
                    }
                }
            }
        }
        return interpolated;
    }

    private void prepareStackModePoints(TbXYItemRenderState s, XYPlot plot, XYDataset dataset, int series, int item,
                                          ValueAxis xAxis, ValueAxis yAxis, Rectangle2D dataArea) {
        TableXYDataset tdataset = (TableXYDataset) dataset;
        double x1 = dataset.getXValue(series, item);
        double y1 = dataset.getYValue(series, item);
        if (Double.isNaN(y1)) {
            return;
        }
        PlotOrientation orientation = plot.getOrientation();
        double[] stack1 = getStackValues(tdataset, series, item);
        RectangleEdge edge0 = plot.getDomainAxisEdge();
        float transX1 = (float) xAxis.valueToJava2D(x1, dataArea, edge0);
        RectangleEdge edge1 = plot.getRangeAxisEdge();
        double stackY1 = y1 >= 0 ? stack1[1] : stack1[0];
        float transY1 = (float) yAxis.valueToJava2D(y1 + stackY1, dataArea,
                edge1);
        float transStack1 = (float) yAxis.valueToJava2D(stackY1,
                dataArea, edge1);
        if (orientation == PlotOrientation.VERTICAL) {
            storePoint(s, transX1, transY1);
            storeStackPoint(s, transX1, transStack1);
        } else {
            storePoint(s, transY1, transX1);
            storeStackPoint(s, transStack1, transX1);
        }
    }

    private void storePoint(TbXYItemRenderState s, double x, double y) {
        Point2D p = new Point2D.Float((float)x, (float)y);
        if (!s.points.contains(p)) {
            s.points.add(p);
        }
    }

    private void storeStackPoint(TbXYItemRenderState s, double x, double y) {
        Point2D p = new Point2D.Float((float)x, (float)y);
        if (!s.stackPoints.contains(p)) {
            s.stackPoints.add(p);
        }
    }

    private void storeLine(TbXYItemRenderState state, Rectangle2D dataArea, double x0, double y0, double x1, double y1, List<Point2D> targetPoints) {
        if (Double.isNaN(x0) || Double.isNaN(x1) || Double.isNaN(y0)
                || Double.isNaN(y1)) {
            return;
        }
        state.workingLine.setLine(x0, y0, x1, y1);
        boolean visible = LineUtils.clipLine(state.workingLine, dataArea);
        if (visible) {
            if (!targetPoints.contains(state.workingLine.getP1())) {
                targetPoints.add(state.workingLine.getP1());
            }
            if (!targetPoints.contains(state.workingLine.getP2())) {
                targetPoints.add(state.workingLine.getP2());
            }
        }
    }

    private void sortPoints(TbXYItemRenderState s, PlotOrientation orientation) {
        if (orientation == PlotOrientation.VERTICAL) {
            s.points.sort((p1, p2) -> (int)(p1.getX() - p2.getX()));
        } else {
            s.points.sort((p1, p2) -> (int)(p1.getY() - p2.getY()));
        }
        if (this.getStackMode()) {
            if (orientation == PlotOrientation.VERTICAL) {
                s.stackPoints.sort((p1, p2) -> (int)(p1.getX() - p2.getX()));
            } else {
                s.stackPoints.sort((p1, p2) -> (int)(p1.getY() - p2.getY()));
            }
        }
    }

    private void drawPointsToPath(List<Point2D> points, GeneralPath targetPath, boolean firstPointMove) {
        if (points.size() > 1) {
            Point2D cp0 = points.get(0);
            if (firstPointMove) {
                targetPath.moveTo(cp0.getX(), cp0.getY());
            } else {
                targetPath.lineTo(cp0.getX(), cp0.getY());
            }
            for (int i = 1; i < points.size(); i++) {
                Point2D cp = points.get(i);
                targetPath.lineTo(cp.getX(), cp.getY());
            }
        }
    }

    private void drawFillArea(XYPlot plot, ValueAxis xAxis,
                              ValueAxis yAxis,
                              Rectangle2D dataArea,
                              GeneralPath fillArea,
                              List<Point2D> seriesPoints,
                              List<Point2D> stackPoints) {
        if (this.stackMode) {
            if (seriesPoints.size() > 1) {
                this.drawPointsToPath(seriesPoints, fillArea, true);
                Collections.reverse(stackPoints);
                this.drawPointsToPath(stackPoints, fillArea, false);
                fillArea.closePath();
            }
        } else {
            RectangleEdge xAxisLocation = plot.getDomainAxisEdge();
            RectangleEdge yAxisLocation = plot.getRangeAxisEdge();
            if (seriesPoints.size() > 1) {
                Point2D origin;
                if (this.fillType == FillType.TO_ZERO) {
                    float xz = (float) xAxis.valueToJava2D(0, dataArea,
                            yAxisLocation);
                    float yz = (float) yAxis.valueToJava2D(0, dataArea,
                            yAxisLocation);
                    origin = plot.getOrientation() == PlotOrientation.HORIZONTAL
                            ? new Point2D.Float(yz, xz)
                            : new Point2D.Float(xz, yz);
                } else if (this.fillType == FillType.TO_LOWER_BOUND) {
                    float xlb = (float) xAxis.valueToJava2D(
                            xAxis.getLowerBound(), dataArea, xAxisLocation);
                    float ylb = (float) yAxis.valueToJava2D(
                            yAxis.getLowerBound(), dataArea, yAxisLocation);
                    origin = plot.getOrientation() == PlotOrientation.HORIZONTAL
                            ? new Point2D.Float(ylb, xlb)
                            : new Point2D.Float(xlb, ylb);
                } else {// fillType == TO_UPPER_BOUND
                    float xub = (float) xAxis.valueToJava2D(
                            xAxis.getUpperBound(), dataArea, xAxisLocation);
                    float yub = (float) yAxis.valueToJava2D(
                            yAxis.getUpperBound(), dataArea, yAxisLocation);
                    origin = plot.getOrientation() == PlotOrientation.HORIZONTAL
                            ? new Point2D.Float(yub, xub)
                            : new Point2D.Float(xub, yub);
                }
                Point2D cp0 = seriesPoints.get(0);
                if (plot.getOrientation() == PlotOrientation.HORIZONTAL) {
                    fillArea.moveTo(origin.getX(), cp0.getY());
                } else {
                    fillArea.moveTo(cp0.getX(), origin.getY());
                }
                this.drawPointsToPath(seriesPoints, fillArea, false);
                if (plot.getOrientation() == PlotOrientation.HORIZONTAL) {
                    fillArea.lineTo(origin.getX(), seriesPoints.get(
                            seriesPoints.size() - 1).getY());
                } else {
                    fillArea.lineTo(seriesPoints.get(
                            seriesPoints.size() - 1).getX(), origin.getY());
                }
                fillArea.closePath();
            }
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof TbXYLineAndShapeRenderer)) {
            return false;
        }
        TbXYLineAndShapeRenderer that = (TbXYLineAndShapeRenderer) obj;
        if (this.fillType != that.fillType) {
            return false;
        }
        if (!Objects.equals(this.gradientPaintTransformer, that.gradientPaintTransformer)) {
            return false;
        }
        return super.equals(obj);
    }

}
