// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.report.renderer.chart;

import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.entity.EntityCollection;
import org.jfree.chart.labels.CategoryItemLabelGenerator;
import org.jfree.chart.labels.ItemLabelAnchor;
import org.jfree.chart.labels.ItemLabelPosition;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.CategoryItemRendererState;
import org.jfree.chart.text.TextUtils;
import org.jfree.chart.ui.GradientPaintTransformer;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.chart.ui.TextAnchor;
import org.jfree.chart.util.Args;
import org.jfree.chart.util.BooleanList;
import org.jfree.chart.util.PaintList;
import org.jfree.data.KeyedValues2DItemKey;
import org.jfree.data.category.CategoryDataset;

import java.awt.BasicStroke;
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

import static org.thingsboard.server.report.util.ColorUtils.safeParseCssColor;

public class TbBarRenderer extends BarRenderer {

    private final Map<Integer, Float> itemBorderRadiusMap;
    private Float defaultItemBorderRadius;

    private final BooleanList itemLabelsBackgroundVisibleList;
    private boolean defaultItemLabelsBackgroundVisible;
    private final PaintList itemLabelsBackgroundPaintList;
    private transient Paint defaultItemLabelBackgroundPaint;

    public TbBarRenderer() {
        super();
        itemBorderRadiusMap = new HashMap<>();
        defaultItemBorderRadius = 0f;
        this.itemLabelsBackgroundVisibleList = new BooleanList();
        this.defaultItemLabelsBackgroundVisible = false;
        this.itemLabelsBackgroundPaintList = new PaintList();
        this.defaultItemLabelBackgroundPaint = safeParseCssColor("rgba(255,255,255,0.56)");
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
    public int getPassCount() {
        return 2;
    }

    @Override
    public void drawItem(Graphics2D g2, CategoryItemRendererState state,
                         Rectangle2D dataArea, CategoryPlot plot, CategoryAxis domainAxis,
                         ValueAxis rangeAxis, CategoryDataset dataset, int row,
                         int column, int pass) {

        // nothing is drawn if the row index is not included in the list with
        // the indices of the visible rows...
        int visibleRow = state.getVisibleSeriesIndex(row);
        if (visibleRow < 0) {
            return;
        }
        // nothing is drawn for null values...
        Number dataValue = dataset.getValue(row, column);
        if (dataValue == null) {
            return;
        }

        final double value = dataValue.doubleValue();
        PlotOrientation orientation = plot.getOrientation();
        double barW0 = calculateBarW0(plot, orientation, dataArea, domainAxis,
                state, visibleRow, column);
        double[] barL0L1 = calculateBarL0L1(value);
        if (barL0L1 == null) {
            return;  // the bar is not visible
        }

        RectangleEdge edge = plot.getRangeAxisEdge();
        double transL0 = rangeAxis.valueToJava2D(barL0L1[0], dataArea, edge);
        double transL1 = rangeAxis.valueToJava2D(barL0L1[1], dataArea, edge);

        // in the following code, barL0 is (in Java2D coordinates) the LEFT
        // end of the bar for a horizontal bar chart, and the TOP end of the
        // bar for a vertical bar chart.  Whether this is the BASE of the bar
        // or not depends also on (a) whether the data value is 'negative'
        // relative to the base value and (b) whether the range axis is
        // inverted.  This only matters if/when we apply the minimumBarLength
        // attribute, because we should extend the non-base end of the bar
        boolean positive = (value >= this.getBase());
        boolean inverted = rangeAxis.isInverted();
        double barL0 = Math.min(transL0, transL1);
        double barLength = Math.abs(transL1 - transL0);
        double barLengthAdj = 0.0;
        if (barLength > 0.0 && barLength < getMinimumBarLength()) {
            barLengthAdj = getMinimumBarLength() - barLength;
        }
        double barL0Adj = 0.0;
        RectangleEdge barBase;
        if (orientation == PlotOrientation.HORIZONTAL) {
            if (positive && inverted || !positive && !inverted) {
                barL0Adj = barLengthAdj;
                barBase = RectangleEdge.RIGHT;
            }
            else {
                barBase = RectangleEdge.LEFT;
            }
        }
        else {
            if (positive && !inverted || !positive && inverted) {
                barL0Adj = barLengthAdj;
                barBase = RectangleEdge.BOTTOM;
            }
            else {
                barBase = RectangleEdge.TOP;
            }
        }

        Float borderRadius = getItemBorderRadius(row, column);
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

        // draw the bar...
        Shape bar;
        if (orientation == PlotOrientation.HORIZONTAL) {
            bar = ChartUtils.createRectangularShapeWithRoundedCorners(barL0 - barL0Adj, barW0, barLength + barLengthAdj, state.getBarWidth(),
                    radiusTopLeft, radiusTopRight, radiusBottomLeft, radiusBottomRight);
        }
        else {
            bar = ChartUtils.createRectangularShapeWithRoundedCorners(barW0, barL0 - barL0Adj, state.getBarWidth(), barLength + barLengthAdj,
                    radiusTopLeft, radiusTopRight, radiusBottomLeft, radiusBottomRight);
        }
        if (pass == 0) {
            if (state.getElementHinting()) {
                KeyedValues2DItemKey key = new KeyedValues2DItemKey(
                        dataset.getRowKey(row), dataset.getColumnKey(column));
                beginElementGroup(g2, key);
            }
            paintBar(g2, row, column, bar);
            if (state.getElementHinting()) {
                endElementGroup(g2);
            }
        } else if (pass == 1) {
            Shape savedClip = g2.getClip();
            g2.setClip(null);
            try {
                if (isItemLabelVisible(row, column)) {
                    CategoryItemLabelGenerator generator = getItemLabelGenerator(row,
                            column);
                    if (generator != null) {
                        String label = generator.generateLabel(dataset, row, column);
                        if (label != null) {
                            Font labelFont = getItemLabelFont(row, column);
                            Paint labelPaint = getItemLabelPaint(row, column);
                            drawLabel(g2, row, column, plot, label, labelFont, labelPaint, bar.getBounds2D(), value < 0.0);
                        }
                    }
                }
                // submit the current data point as a crosshair candidate
                int datasetIndex = plot.indexOf(dataset);
                updateCrosshairValues(state.getCrosshairState(),
                        dataset.getRowKey(row), dataset.getColumnKey(column), value,
                        datasetIndex, barW0, barL0, orientation);

                // add an item entity, if this information is being collected
                EntityCollection entities = state.getEntityCollection();
                if (entities != null) {
                    addItemEntity(entities, dataset, row, column, bar);
                }
            } finally {
                g2.setClip(savedClip);
            }
        }
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

    protected void drawLabel(Graphics2D g,
                             int series, int item, CategoryPlot plot, String label,
                             Font labelFont, Paint labelPaint,
                             Rectangle2D bar, boolean negative) {

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
            float distance = drawBackground ? 5 : 3;
            if (position.getTextAnchor() == TextAnchor.BOTTOM_CENTER) {
                y -= distance;
            } else if (position.getTextAnchor() == TextAnchor.TOP_CENTER) {
                y += distance;
            }

            Rectangle2D bounds = TextUtils.calculateRotatedStringBounds(drawLabel, g2,
                    x, y,
                    position.getTextAnchor(), position.getAngle(),
                    position.getRotationAnchor()).getBounds2D();
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
        }
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
