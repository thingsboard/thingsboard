// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.report.renderer.chart;

import org.jfree.chart.entity.EntityCollection;
import org.jfree.chart.entity.PieSectionEntity;
import org.jfree.chart.labels.PieToolTipGenerator;
import org.jfree.chart.plot.CenterTextMode;
import org.jfree.chart.plot.PiePlotState;
import org.jfree.chart.plot.RingPlot;
import org.jfree.chart.text.TextBlock;
import org.jfree.chart.text.TextBlockAnchor;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.chart.urls.PieURLGenerator;
import org.jfree.chart.util.LineUtils;
import org.jfree.chart.util.Rotation;
import org.jfree.chart.util.ShapeUtils;
import org.jfree.chart.util.UnitType;
import org.jfree.data.general.PieDataset;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import static org.thingsboard.server.report.util.AwtFontUtils.ZERO_FONT;

public class TbRingPlot extends RingPlot {

    private double segmentsPadAngle = 0f;
    private boolean roundSegmentCorners = false;
    private double absoluteSectionDepth = 0f;
    private double scaleBase = 0f;

    private String centerLabel;
    private Font centerLabelFont;
    private Color centerLabelColor;


    public TbRingPlot(PieDataset dataset) {
        super(dataset);
        this.setInteriorGap(0);
        this.setLabelLinkMargin(0);
        this.centerLabel = null;
        this.centerLabelFont = DEFAULT_LABEL_FONT;
        this.centerLabelColor = Color.BLACK;
    }

    public void setSegmentsPadAngle(double segmentsPadAngle) {
        this.segmentsPadAngle = segmentsPadAngle;
    }

    public void setRoundSegmentCorners(boolean roundSegmentCorners) {
        this.roundSegmentCorners = roundSegmentCorners;
    }

    public void setAbsoluteSectionDepth(double absoluteSectionDepth) {
        this.absoluteSectionDepth = absoluteSectionDepth;
    }

    public void setScaleBase(double scaleBase) {
        this.scaleBase = scaleBase;
    }

    public void setCenterLabel(String centerLabel) {
        this.centerLabel = centerLabel;
    }

    public void setCenterLabelFont(Font centerLabelFont) {
        this.centerLabelFont = centerLabelFont;
    }

    public void setCenterLabelColor(Color centerLabelColor) {
        this.centerLabelColor = centerLabelColor;
    }

    @Override
    protected void drawItem(Graphics2D g2, int section, Rectangle2D dataArea,
                            PiePlotState state, int currentPass) {

        PieDataset dataset = getDataset();
        Number n = dataset.getValue(section);
        if (n == null) {
            return;
        }
        double value = n.doubleValue();
        double angle1 = 0.0;
        double angle2 = 0.0;

        Rotation direction = getDirection();
        if (direction == Rotation.CLOCKWISE) {
            angle1 = state.getLatestAngle();
            angle2 = angle1 - value / state.getTotal() * 360.0;
        }
        else if (direction == Rotation.ANTICLOCKWISE) {
            angle1 = state.getLatestAngle();
            angle2 = angle1 + value / state.getTotal() * 360.0;
        }
        else {
            throw new IllegalStateException("Rotation type not recognised.");
        }

        double angle = (angle2 - angle1);
        if (Math.abs(angle) > getMinimumArcAngleToDraw()) {
            Comparable key = getSectionKey(section);
            double ep = 0.0;
            double mep = getMaximumExplodePercent();
            if (mep > 0.0) {
                ep = getExplodePercent(key) / mep;
            }
            Rectangle2D arcBounds = getArcBounds(state.getPieArea(),
                    state.getExplodedPieArea(), angle1, angle, ep);


            double padAngle = dataset.getItemCount() > 1 ? this.segmentsPadAngle : 0;
            boolean roundedCorners = dataset.getItemCount() > 1 && this.roundSegmentCorners;
            double angleOffset = padAngle / 2;

            double sectionDepth;
            if (this.absoluteSectionDepth > 0) {
                sectionDepth = this.absoluteSectionDepth / this.computeSize(arcBounds);
            } else {
                sectionDepth = this.getSectionDepth();
            }

            // create the bounds for the inner arc
            double depth = sectionDepth / 2.0;

            if (roundedCorners) {
                float absDepth = (float) (depth * arcBounds.getHeight());
                float depthOffsetAngle = (float) ((absDepth / 2) / ((arcBounds.getHeight() - absDepth) / 2) * 180 / Math.PI);
                angleOffset += depthOffsetAngle;
            }
            angleOffset = angleOffset * direction.getFactor();

            Arc2D.Double arc = new Arc2D.Double(arcBounds, angle1 + angleOffset, angle - angleOffset*2,
                    Arc2D.OPEN);

            RectangleInsets s = new RectangleInsets(UnitType.RELATIVE,
                    depth, depth, depth, depth);
            Rectangle2D innerArcBounds = new Rectangle2D.Double();
            innerArcBounds.setRect(arcBounds);
            s.trim(innerArcBounds);
            // calculate inner arc in reverse direction, for later
            // GeneralPath construction

            Arc2D.Double arc2 = new Arc2D.Double(innerArcBounds, angle1
                    + angle - angleOffset, -(angle - angleOffset*2), Arc2D.OPEN);
            GeneralPath path = new GeneralPath();
            path.moveTo((float) arc.getStartPoint().getX(),
                    (float) arc.getStartPoint().getY());
            path.append(arc.getPathIterator(null), false);

            if (roundedCorners) {
                path.append(this.roundJoin(arc.getEndPoint(), arc2.getStartPoint()).getPathIterator(null), true);
            }

            path.append(arc2.getPathIterator(null), true);

            if (roundedCorners) {
                path.append(this.roundJoin(arc2.getEndPoint(), arc.getStartPoint()).getPathIterator(null), true);
            }

            path.closePath();

            Line2D separator = new Line2D.Double(arc2.getEndPoint(),
                    arc.getStartPoint());

            if (currentPass == 0) {
                Paint shadowPaint = getShadowPaint();
                double shadowXOffset = getShadowXOffset();
                double shadowYOffset = getShadowYOffset();
                if (shadowPaint != null && getShadowGenerator() == null) {
                    Shape shadowArc = ShapeUtils.createTranslatedShape(
                            path, (float) shadowXOffset, (float) shadowYOffset);
                    g2.setPaint(shadowPaint);
                    g2.fill(shadowArc);
                }
            }
            else if (currentPass == 1) {
                Paint paint = lookupSectionPaint(key);
                g2.setPaint(paint);
                g2.fill(path);

                Paint outlinePaint = lookupSectionOutlinePaint(key);
                Stroke outlineStroke = lookupSectionOutlineStroke(key);
                if (getSectionOutlinesVisible() && outlinePaint != null
                        && outlineStroke != null) {
                    g2.setPaint(outlinePaint);
                    g2.setStroke(outlineStroke);
                    g2.draw(path);
                }

                if (section == 0) {
                    AffineTransform currentTransform = g2.getTransform();
                    double scale = this.computeScaleFactor(arcBounds);
                    float factor = 1f;
                    if (scale > 0.0) {
                        g2.scale(scale, scale);
                        factor = (float) scale;
                    }
                    String nstr = null;
                    if (this.getCenterTextMode().equals(CenterTextMode.VALUE)) {
                        nstr = this.getCenterTextFormatter().format(n);
                    } else if (this.getCenterTextMode().equals(CenterTextMode.FIXED)) {
                        nstr = this.getCenterText();
                    }
                    if (this.centerLabel != null || nstr != null) {
                        TextBlock centerBlock = new TextBlock();
                        if (this.centerLabel != null) {
                            centerBlock.addLine(this.centerLabel, this.centerLabelFont, this.centerLabelColor);
                        }
                        if (nstr != null) {
                            if (this.centerLabel != null) {
                                centerBlock.addLine("", ZERO_FONT.deriveFont(1f), Color.BLACK);
                            }
                            centerBlock.addLine(nstr, this.getCenterTextFont(), this.getCenterTextColor());
                        }
                        centerBlock.draw(g2, (float) dataArea.getCenterX() / factor,
                                (float) dataArea.getCenterY() / factor, TextBlockAnchor.CENTER);
                    }
                   g2.setTransform(currentTransform);
                }

                // add an entity for the pie section
                if (state.getInfo() != null) {
                    EntityCollection entities = state.getEntityCollection();
                    if (entities != null) {
                        String tip = null;
                        PieToolTipGenerator toolTipGenerator
                                = getToolTipGenerator();
                        if (toolTipGenerator != null) {
                            tip = toolTipGenerator.generateToolTip(dataset,
                                    key);
                        }
                        String url = null;
                        PieURLGenerator urlGenerator = getURLGenerator();
                        if (urlGenerator != null) {
                            url = urlGenerator.generateURL(dataset, key,
                                    getPieIndex());
                        }
                        PieSectionEntity entity = new PieSectionEntity(path,
                                dataset, getPieIndex(), section, key, tip,
                                url);
                        entities.add(entity);
                    }
                }
            }
            else if (currentPass == 2) {
                if (this.getSeparatorsVisible()) {
                    Line2D extendedSeparator = LineUtils.extendLine(
                            separator, this.getInnerSeparatorExtension(),
                            this.getOuterSeparatorExtension());
                    g2.setStroke(this.getSeparatorStroke());
                    g2.setPaint(this.getSeparatorPaint());
                    g2.draw(extendedSeparator);
                }
            }
        }
        state.setLatestAngle(angle2);
    }

    private double computeSize(Rectangle2D arcBounds) {
        if (this.scaleBase > 0) {
            return this.scaleBase;
        } else {
            return Math.min(arcBounds.getWidth(), arcBounds.getHeight());
        }
    }

    private double computeScaleFactor(Rectangle2D arcBounds) {
        if (this.scaleBase > 0) {
            double size = Math.min(arcBounds.getWidth(), arcBounds.getHeight());
            return size / this.scaleBase;
        } else {
            return 0.0;
        }
    }

    private Arc2D.Double roundJoin(Point2D startPoint, Point2D endPoint) {
        float joinCenterX = (float) (startPoint.getX() + endPoint.getX()) / 2;
        float joinCenterY = (float) (startPoint.getY() + endPoint.getY()) / 2;
        double startAngle = 360 - Math.toDegrees(Math.atan2(startPoint.getY() - joinCenterY, startPoint.getX() - joinCenterX));
        double distance = startPoint.distance(endPoint);
        return new Arc2D.Double(joinCenterX - distance / 2, joinCenterY - distance / 2, distance, distance, startAngle, 180 * getDirection().getFactor(), Arc2D.OPEN);
    }
}
