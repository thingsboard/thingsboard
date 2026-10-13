// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.report.renderer.chart;

import org.jfree.chart.labels.PieSectionLabelGenerator;
import org.jfree.chart.plot.PieLabelLinkStyle;
import org.jfree.chart.plot.PieLabelRecord;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.PiePlotState;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.text.TextBlock;
import org.jfree.chart.text.TextBox;
import org.jfree.chart.text.TextUtils;
import org.jfree.chart.ui.HorizontalAlignment;
import org.jfree.chart.ui.RectangleAnchor;
import org.jfree.data.general.DatasetUtils;
import org.jfree.data.general.PieDataset;

import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Arc2D;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.Line2D;
import java.awt.geom.QuadCurve2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

public class TbPiePlot<K extends Comparable<K>> extends PiePlot<K> {

    private Double pieRadius = null;

    public TbPiePlot(PieDataset<K> dataset) {
        super(dataset);
    }

    public void setPieRadius(Double pieRadius) {
        this.pieRadius = pieRadius;
    }

    @Override
    protected double getLabelLinkDepth() {
        return 0.0;
    }

    @Override
    protected void drawPie(Graphics2D g2, Rectangle2D plotArea,
                           PlotRenderingInfo info) {

        PiePlotState state = initialise(g2, plotArea, this, null, info);

        // adjust the plot area for interior spacing and labels...
        double labelReserve = 0.0;
        if (this.getLabelGenerator() != null && !this.getSimpleLabels()) {
            labelReserve = this.getLabelGap() + this.getMaximumLabelWidth();
        }
        double gapHorizontal = plotArea.getWidth() * labelReserve * 2.0;
        double gapVertical = plotArea.getHeight() * this.getInteriorGap() * 2.0;


        double linkX = plotArea.getX() + gapHorizontal / 2;
        double linkY = plotArea.getY() + gapVertical / 2;
        double linkW = plotArea.getWidth() - gapHorizontal;
        double linkH = plotArea.getHeight() - gapVertical;

        // make the link area a square if the pie chart is to be circular...
        if (this.isCircular()) {
            double min = Math.min(linkW, linkH) / 2;
            linkX = (linkX + linkX + linkW) / 2 - min;
            linkY = (linkY + linkY + linkH) / 2 - min;
            linkW = 2 * min;
            linkH = 2 * min;
        }

        // the link area defines the dog leg points for the linking lines to
        // the labels
        Rectangle2D linkArea = new Rectangle2D.Double(linkX, linkY, linkW,
                linkH);
        state.setLinkArea(linkArea);

        // the explode area defines the max circle/ellipse for the exploded
        // pie sections.  it is defined by shrinking the linkArea by the
        // linkMargin factor.
        double lm = 0.0;
        if (!this.getSimpleLabels()) {
            lm = this.getLabelLinkMargin();
        }
        double hh = linkArea.getWidth() * lm * 2.0;
        double vv = linkArea.getHeight() * lm * 2.0;
        Rectangle2D explodeArea = new Rectangle2D.Double(linkX + hh / 2.0,
                linkY + vv / 2.0, linkW - hh, linkH - vv);

        state.setExplodedPieArea(explodeArea);

        Rectangle2D pieArea;
        if (this.pieRadius != null) {
            double min = Math.min(plotArea.getWidth(), plotArea.getHeight());
            double size = min * this.pieRadius / 100;
            double x = plotArea.getCenterX() - size / 2;
            double y = plotArea.getCenterY() - size / 2;
            pieArea = new Rectangle2D.Double(x, y, size, size);

            double horizontalSpace = plotArea.getWidth() - pieArea.getWidth();
            double verticalSpace = plotArea.getHeight() - pieArea.getHeight();
            double hLabelSpace = Math.min(horizontalSpace, gapHorizontal);
            double vLabelSpace = Math.min(verticalSpace, gapVertical);
            double maxLinkLength = 30;
            double maxLinkHeight = 15;
            if (horizontalSpace - hLabelSpace > maxLinkLength) {
                hLabelSpace += horizontalSpace - hLabelSpace - maxLinkLength;
            }
            if (verticalSpace - vLabelSpace > maxLinkHeight) {
                vLabelSpace += verticalSpace - vLabelSpace - maxLinkHeight;
            }
            linkX = plotArea.getX() + hLabelSpace / 2;
            linkY = plotArea.getY() + vLabelSpace / 2;
            linkW = plotArea.getWidth() - hLabelSpace;
            linkH = plotArea.getHeight() - vLabelSpace;
            linkArea = new Rectangle2D.Double(linkX, linkY, linkW,
                    linkH);
            state.setLinkArea(linkArea);
        } else {
            // the pie area defines the circle/ellipse for regular pie sections.
            // it is defined by shrinking the explodeArea by the explodeMargin
            // factor.
            double maximumExplodePercent = getMaximumExplodePercent();
            double percent = maximumExplodePercent / (1.0 + maximumExplodePercent);

            double h1 = explodeArea.getWidth() * percent;
            double v1 = explodeArea.getHeight() * percent;
            pieArea = new Rectangle2D.Double(explodeArea.getX()
                    + h1 / 2.0, explodeArea.getY() + v1 / 2.0,
                    explodeArea.getWidth() - h1, explodeArea.getHeight() - v1);
        }
        state.setPieArea(pieArea);
        state.setPieCenterX(pieArea.getCenterX());
        state.setPieCenterY(pieArea.getCenterY());
        state.setPieWRadius(pieArea.getWidth() / 2.0);
        state.setPieHRadius(pieArea.getHeight() / 2.0);

        // plot the data (unless the dataset is null)...
        if ((this.getDataset() != null) && (this.getDataset().getKeys().size() > 0)) {

            List<K> keys = this.getDataset().getKeys();
            double totalValue = DatasetUtils.calculatePieDatasetTotal(
                    this.getDataset());

            int passesRequired = state.getPassesRequired();
            for (int pass = 0; pass < passesRequired; pass++) {
                double runningTotal = 0.0;
                for (int section = 0; section < keys.size(); section++) {
                    Number n = this.getDataset().getValue(section);
                    if (n != null) {
                        double value = n.doubleValue();
                        if (value > 0.0) {
                            runningTotal += value;
                            drawItem(g2, section, explodeArea, state, pass);
                        }
                    }
                }
            }
            if (this.getSimpleLabels()) {
                drawSimpleLabels(g2, keys, totalValue, plotArea, linkArea,
                        state);
            }
            else {
                drawLabels(g2, keys, totalValue, plotArea, linkArea, state);
            }

        }
        else {
            drawNoDataMessage(g2, plotArea);
        }
    }

    protected void drawSimpleLabels(Graphics2D g2, List<K> keys,
                                    double totalValue, Rectangle2D plotArea, Rectangle2D pieArea,
                                    PiePlotState state) {
        Shape savedClip = g2.getClip();
        g2.setClip(null);
        try {
            Composite originalComposite = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                    1.0f));

            Rectangle2D labelsArea = this.getSimpleLabelOffset().createInsetRectangle(
                    pieArea);
            double runningTotal = 0.0;
            for (K key : keys) {
                boolean include;
                double v = 0.0;
                Number n = getDataset().getValue(key);
                if (n == null) {
                    include = !getIgnoreNullValues();
                } else {
                    v = n.doubleValue();
                    include = getIgnoreZeroValues() ? v > 0.0 : v >= 0.0;
                }

                if (include) {
                    runningTotal = runningTotal + v;
                    // work out the mid angle (0 - 90 and 270 - 360) = right,
                    // otherwise left
                    double mid = getStartAngle() + (getDirection().getFactor()
                            * ((runningTotal - v / 2.0) * 360) / totalValue);

                    Arc2D arc = new Arc2D.Double(labelsArea, getStartAngle(),
                            mid - getStartAngle(), Arc2D.OPEN);
                    int x = (int) arc.getEndPoint().getX();
                    int y = (int) arc.getEndPoint().getY();

                    PieSectionLabelGenerator myLabelGenerator = getLabelGenerator();
                    if (myLabelGenerator == null) {
                        continue;
                    }
                    String label = myLabelGenerator.generateSectionLabel(
                            this.getDataset(), key);
                    if (label == null) {
                        continue;
                    }
                    TextBlock block = TextUtils.createTextBlock(label,
                            this.getLabelFont(), this.getLabelPaint());
                    TextBox labelBox = new TextBox(block);
                    labelBox.setBackgroundPaint(this.getLabelBackgroundPaint());
                    labelBox.setOutlinePaint(this.getLabelOutlinePaint());
                    labelBox.setOutlineStroke(this.getLabelOutlineStroke());
                    if (this.getShadowGenerator() == null) {
                        labelBox.setShadowPaint(this.getLabelShadowPaint());
                    } else {
                        labelBox.setShadowPaint(null);
                    }
                    labelBox.setInteriorGap(this.getLabelPadding());
                    labelBox.draw(g2, x, y, RectangleAnchor.CENTER);
                }
            }

            g2.setComposite(originalComposite);
        } finally {
            g2.setClip(savedClip);
        }
    }

    @Override
    protected void drawLeftLabel(Graphics2D g2, PiePlotState state,
                                 PieLabelRecord record) {
        Shape savedClip = g2.getClip();
        g2.setClip(null);
        try {
            double anchorX = state.getLinkArea().getMinX();
            double targetX = anchorX - record.getGap();
            double targetY = record.getAllocatedY();

            if (this.getLabelLinksVisible()) {
                double theta = record.getAngle();
                double linkX = state.getPieCenterX() + Math.cos(theta)
                        * state.getPieWRadius() * record.getLinkPercent();
                double linkY = state.getPieCenterY() - Math.sin(theta)
                        * state.getPieHRadius() * record.getLinkPercent();
                double elbowX = state.getPieCenterX() + Math.cos(theta)
                        * state.getLinkArea().getWidth() / 2.0;
                double elbowY = state.getPieCenterY() - Math.sin(theta)
                        * state.getLinkArea().getHeight() / 2.0;
                double anchorY = elbowY;
                g2.setPaint(this.getSectionPaint(record.getKey()));
                g2.setStroke(this.getLabelLinkStroke());
                PieLabelLinkStyle style = getLabelLinkStyle();
                if (style.equals(PieLabelLinkStyle.STANDARD)) {
                    g2.draw(new Line2D.Double(linkX, linkY, elbowX, elbowY));
                    g2.draw(new Line2D.Double(anchorX, anchorY, elbowX, elbowY));
                    g2.draw(new Line2D.Double(anchorX, anchorY, targetX, targetY));
                } else if (style.equals(PieLabelLinkStyle.QUAD_CURVE)) {
                    QuadCurve2D q = new QuadCurve2D.Float();
                    q.setCurve(targetX, targetY, anchorX, anchorY, elbowX, elbowY);
                    g2.draw(q);
                    g2.draw(new Line2D.Double(elbowX, elbowY, linkX, linkY));
                } else if (style.equals(PieLabelLinkStyle.CUBIC_CURVE)) {
                    CubicCurve2D c = new CubicCurve2D.Float();
                    c.setCurve(targetX, targetY, anchorX, anchorY, elbowX, elbowY,
                            linkX, linkY);
                    g2.draw(c);
                }
            }
            TextBox tb = record.getLabel();
            tb.getTextBlock().setLineAlignment(HorizontalAlignment.RIGHT);
            tb.draw(g2, (float) targetX, (float) targetY, RectangleAnchor.RIGHT);
        } finally {
            g2.setClip(savedClip);
        }
    }

    @Override
    protected void drawRightLabel(Graphics2D g2, PiePlotState state,
                                  PieLabelRecord record) {
        Shape savedClip = g2.getClip();
        g2.setClip(null);
        try {
            double anchorX = state.getLinkArea().getMaxX();
            double targetX = anchorX + record.getGap();
            double targetY = record.getAllocatedY();

            if (this.getLabelLinksVisible()) {
                double theta = record.getAngle();
                double linkX = state.getPieCenterX() + Math.cos(theta)
                        * state.getPieWRadius() * record.getLinkPercent();
                double linkY = state.getPieCenterY() - Math.sin(theta)
                        * state.getPieHRadius() * record.getLinkPercent();
                double elbowX = state.getPieCenterX() + Math.cos(theta)
                        * state.getLinkArea().getWidth() / 2.0;
                double elbowY = state.getPieCenterY() - Math.sin(theta)
                        * state.getLinkArea().getHeight() / 2.0;
                double anchorY = elbowY;
                g2.setPaint(this.getSectionPaint(record.getKey()));
                g2.setStroke(this.getLabelLinkStroke());
                PieLabelLinkStyle style = getLabelLinkStyle();
                if (style.equals(PieLabelLinkStyle.STANDARD)) {
                    g2.draw(new Line2D.Double(linkX, linkY, elbowX, elbowY));
                    g2.draw(new Line2D.Double(anchorX, anchorY, elbowX, elbowY));
                    g2.draw(new Line2D.Double(anchorX, anchorY, targetX, targetY));
                } else if (style.equals(PieLabelLinkStyle.QUAD_CURVE)) {
                    QuadCurve2D q = new QuadCurve2D.Float();
                    q.setCurve(targetX, targetY, anchorX, anchorY, elbowX, elbowY);
                    g2.draw(q);
                    g2.draw(new Line2D.Double(elbowX, elbowY, linkX, linkY));
                } else if (style.equals(PieLabelLinkStyle.CUBIC_CURVE)) {
                    CubicCurve2D c = new CubicCurve2D.Float();
                    c.setCurve(targetX, targetY, anchorX, anchorY, elbowX, elbowY,
                            linkX, linkY);
                    g2.draw(c);
                }
            }

            TextBox tb = record.getLabel();
            tb.getTextBlock().setLineAlignment(HorizontalAlignment.LEFT);
            tb.draw(g2, (float) targetX, (float) targetY, RectangleAnchor.LEFT);
        }
        finally {
            g2.setClip(savedClip);
        }
    }
}
