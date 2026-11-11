// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.report.renderer.chart.layout;

import org.jfree.chart.block.Block;
import org.jfree.chart.block.BlockContainer;
import org.jfree.chart.block.ColumnArrangement;
import org.jfree.chart.block.LengthConstraintType;
import org.jfree.chart.block.RectangleConstraint;
import org.jfree.chart.title.TextTitle;
import org.jfree.chart.ui.HorizontalAlignment;
import org.jfree.chart.ui.Size2D;
import org.jfree.chart.ui.VerticalAlignment;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

public class TbColumnArrangement extends ColumnArrangement {

    private double maxRelativeWidth = 0;

    public TbColumnArrangement(HorizontalAlignment hAlign, VerticalAlignment vAlign,
                               double hGap, double vGap) {
        super(hAlign, vAlign, hGap, vGap);
    }

    public void setMaxRelativeWidth(double maxRelativeWidth) {
        this.maxRelativeWidth = maxRelativeWidth;
    }

    @Override
    public Size2D arrange(BlockContainer container, Graphics2D g2,
                          RectangleConstraint constraint) {
        if (this.maxRelativeWidth > 0) {
            double maxWidth = constraint.getWidth() * this.maxRelativeWidth;
            constraint = constraint.toFixedWidth(maxWidth);
        }
        LengthConstraintType w = constraint.getWidthConstraintType();
        LengthConstraintType h = constraint.getHeightConstraintType();
        if (w == LengthConstraintType.FIXED && h == LengthConstraintType.RANGE) {
            return this.arrangeFR(container, g2, constraint);
        }
        return super.arrange(container, g2, constraint);
    }

    protected Size2D arrangeFR(BlockContainer container, Graphics2D g2,
                               RectangleConstraint constraint) {
        return arrangeFF(container, g2, constraint);
    }

    protected Size2D arrangeFF(BlockContainer container, Graphics2D g2,
                               RectangleConstraint constraint) {
        Size2D s = super.arrangeNF(container, g2, constraint);
        if (s.width > constraint.getWidth()) {
            List<Block> blocks = (List<Block>) container.getBlocks();
            List<Block> visibleBlocks = new ArrayList<>();
            double maxWidth = 0;
            for (Block b : blocks) {
                Rectangle2D bounds = b.getBounds();
                double left = bounds.getMinX();
                double right = bounds.getMaxX();
                if (left < constraint.getWidth()) {
                    visibleBlocks.add(b);
                    if (right > constraint.getWidth()) {
                        double newWidth = bounds.getWidth() - (right - constraint.getWidth());
                        bounds = new Rectangle2D.Double(bounds.getX(), bounds.getY(), newWidth, bounds.getHeight());
                        b.setBounds(bounds);
                        arrangeTitleWidth(g2, b, bounds.getWidth());
                    }
                    maxWidth = Math.max(maxWidth, bounds.getMaxX());
                }
            }
            container.clear();
            for (Block b : visibleBlocks) {
                container.add(b);
            }
            s.width = maxWidth;
            return s;
        }
        return s;
    }

    private double arrangeTitleWidth(Graphics2D g2, Block block, double maxWidth) {
        if (block instanceof BlockContainer container) {
            List<Block> blocks = (List<Block>) container.getBlocks();
            for (Block b : blocks) {
                maxWidth = arrangeTitleWidth(g2, b, maxWidth);
            }
        } else if (block instanceof TextTitle textTitle) {
            maxWidth -= (textTitle.getPadding().getLeft() + textTitle.getPadding().getRight());
            textTitle.arrange(g2, new RectangleConstraint(maxWidth, 0).toUnconstrainedHeight());
        } else {
            maxWidth -= block.getBounds().getWidth();
        }
        return maxWidth;
    }

}
