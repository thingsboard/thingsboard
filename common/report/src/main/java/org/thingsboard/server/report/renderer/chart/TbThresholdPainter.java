// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.report.renderer.chart;

import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.text.TextUtils;
import org.jfree.chart.ui.LengthAdjustmentType;
import org.jfree.chart.ui.RectangleAnchor;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.chart.util.ShapeUtils;
import org.jfree.data.Range;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Composite;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

public class TbThresholdPainter {

    public void paintThresholdMarker(Graphics2D g2, XYPlot plot, ValueAxis rangeAxis,
                                     Rectangle2D dataArea, TbThresholdMarker marker) {
        Shape savedClip = g2.getClip();
        g2.setClip(null);
        try {
            double value = marker.getValue();
            Range range = rangeAxis.getRange();
            if (!range.contains(value)) {
                return;
            }

            double v = rangeAxis.valueToJava2D(value, dataArea,
                    plot.getRangeAxisEdge());
            PlotOrientation orientation = plot.getOrientation();
            Line2D line = null;
            if (orientation == PlotOrientation.HORIZONTAL) {
                line = new Line2D.Double(v, dataArea.getMinY(), v,
                        dataArea.getMaxY());
            } else if (orientation == PlotOrientation.VERTICAL) {
                line = new Line2D.Double(dataArea.getMinX(), v,
                        dataArea.getMaxX(), v);
            } else {
                throw new IllegalStateException("Unrecognised orientation.");
            }

            final Composite originalComposite = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(
                    AlphaComposite.SRC_OVER, marker.getAlpha()));
            g2.setPaint(marker.getPaint());
            g2.setStroke(marker.getStroke());
            g2.draw(line);

            if (marker.getStartShape() != null || marker.getEndShape() != null) {
                double startY;
                double endY;
                double startX;
                double endX;
                if (orientation == PlotOrientation.HORIZONTAL) {
                    startX = v;
                    endX = v;
                    startY = dataArea.getMinY();
                    endY = dataArea.getMaxY();
                } else {
                    startX = dataArea.getMinX();
                    endX = dataArea.getMaxX();
                    startY = v;
                    endY = v;
                }
                if (marker.getStartShape() != null) {
                    Shape startShape = marker.getStartShape();
                    double shapeX = startX;
                    double shapeY = startY;
                    double angle;
                    if (orientation == PlotOrientation.HORIZONTAL) {
                        angle = 0;
                    } else {
                        angle = -Math.PI / 2;
                    }
                    startShape = ShapeUtils.createTranslatedShape(startShape, shapeX, shapeY);
                    startShape = ShapeUtils.rotateShape(startShape, angle, (float) shapeX, (float) shapeY);
                    this.drawThresholdShape(g2, startShape, marker.getPaint(), marker.getStartShapeFillPaint(), marker.getStartShapeOutlineStroke());
                }
                if (marker.getEndShape() != null) {
                    Shape endShape = marker.getEndShape();
                    double shapeX = endX;
                    double shapeY = endY;
                    double angle;
                    if (orientation == PlotOrientation.HORIZONTAL) {
                        angle = Math.PI;
                    } else {
                        angle = Math.PI / 2;
                    }
                    endShape = ShapeUtils.createTranslatedShape(endShape, shapeX, shapeY);
                    endShape = ShapeUtils.rotateShape(endShape, angle, (float) shapeX, (float) shapeY);
                    this.drawThresholdShape(g2, endShape, marker.getPaint(), marker.getEndShapeFillPaint(), marker.getEndShapeOutlineStroke());
                }
            }
            String label = marker.getLabel();
            RectangleAnchor anchor = marker.getLabelAnchor();
            if (label != null) {
                Font labelFont = marker.getLabelFont();
                g2.setFont(labelFont);
                Point2D coords = calculateThresholdMarkerTextAnchorPoint(
                        g2, orientation, dataArea, line.getBounds2D(),
                        marker.getLabelOffset(),
                        marker.getLabelOffsetType(), anchor);
                if (marker.isDrawLabelBackground()) {
                    Rectangle2D r = TextUtils.calcAlignedStringBounds(label,
                            g2, (float) coords.getX(), (float) coords.getY(),
                            marker.getLabelTextAnchor());
                    g2.setPaint(marker.getLabelBackgroundColor());
                    g2.setStroke(new BasicStroke(0));
                    g2.fillRoundRect((int)r.getX()-3, (int)r.getY()-2, (int)r.getWidth()+6, (int)r.getHeight()+4, 4, 4 );
                }
                g2.setPaint(marker.getLabelPaint());
                TextUtils.drawAlignedString(label, g2,
                        (float) coords.getX(), (float) coords.getY(),
                        marker.getLabelTextAnchor());
            }
            g2.setComposite(originalComposite);
        } finally {
            g2.setClip(savedClip);
        }
    }

    private void drawThresholdShape(Graphics2D g2, Shape shape, Paint paint, Paint fillPaint, Stroke outlineStroke) {
        if (fillPaint != null) {
            g2.setPaint(fillPaint);
        } else {
            g2.setPaint(paint);
        }
        g2.fill(shape);
        if (outlineStroke != null) {
            g2.setPaint(paint);
            g2.setStroke(outlineStroke);
            g2.draw(shape);
        }
    }

    private Point2D calculateThresholdMarkerTextAnchorPoint(Graphics2D g2,
                                                            PlotOrientation orientation, Rectangle2D dataArea,
                                                            Rectangle2D markerArea, RectangleInsets markerOffset,
                                                            LengthAdjustmentType labelOffsetForRange, RectangleAnchor anchor) {

        Rectangle2D anchorRect = null;
        if (orientation == PlotOrientation.HORIZONTAL) {
            anchorRect = markerOffset.createAdjustedRectangle(markerArea,
                    LengthAdjustmentType.EXPAND, labelOffsetForRange);
        }
        else if (orientation == PlotOrientation.VERTICAL) {
            anchorRect = markerOffset.createAdjustedRectangle(markerArea,
                    labelOffsetForRange, LengthAdjustmentType.EXPAND);
        }
        return anchor.getAnchorPoint(anchorRect);

    }

}
