// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.report.renderer.chart.layout;

import org.jfree.chart.block.Block;
import org.jfree.chart.block.BlockContainer;
import org.jfree.chart.block.FlowArrangement;
import org.jfree.chart.block.RectangleConstraint;
import org.jfree.chart.ui.HorizontalAlignment;
import org.jfree.chart.ui.Size2D;
import org.jfree.chart.ui.VerticalAlignment;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

public class TbFlowArrangement extends FlowArrangement {

    private final HorizontalAlignment horizontalAlignment;
    private final VerticalAlignment verticalAlignment;
    private final TbContentJustify horizontalJustify;

    private final double horizontalGap;
    private final double verticalGap;

    private double maxRelativeHeight = 0;

    public TbFlowArrangement() {
        this(HorizontalAlignment.CENTER, VerticalAlignment.CENTER, 2.0, 2.0);
    }

    public TbFlowArrangement(HorizontalAlignment hAlign, VerticalAlignment vAlign,
                             double hGap, double vGap) {
        this(hAlign, vAlign, TbContentJustify.NONE, hGap, vGap);
    }

    public TbFlowArrangement(HorizontalAlignment hAlign, VerticalAlignment vAlign,
                             TbContentJustify hJustify, double hGap, double vGap) {
        super(hAlign, vAlign, hGap, vGap);
        this.horizontalAlignment = hAlign;
        this.verticalAlignment = vAlign;
        this.horizontalJustify = hJustify;
        this.horizontalGap = hGap;
        this.verticalGap = vGap;
    }

    public void setMaxRelativeHeight(double maxRelativeHeight) {
        this.maxRelativeHeight = maxRelativeHeight;
    }

    @Override
    public Size2D arrange(BlockContainer container, Graphics2D g2,
                          RectangleConstraint constraint) {
        if (this.maxRelativeHeight > 0) {
            double maxHeight = constraint.getHeight() * this.maxRelativeHeight;
            constraint = constraint.toFixedHeight(maxHeight);
        }
        return super.arrange(container, g2, constraint);
    }

    @Override
    protected Size2D arrangeFN(BlockContainer container, Graphics2D g2,
                               RectangleConstraint constraint) {

        List<Block> blocks = (List<Block>) container.getBlocks();
        double width = constraint.getWidth();

        double x = 0.0;
        double y = 0.0;
        double maxHeight = 0.0;
        List<Block> itemsInRow = new ArrayList<>();
        for (int i = 0; i < blocks.size(); i++) {
            Block block = blocks.get(i);
            Size2D size = block.arrange(g2, RectangleConstraint.NONE);
            if (x + size.width <= width) {
                itemsInRow.add(block);
                block.setBounds(
                        new Rectangle2D.Double(x, y, size.width, size.height)
                );
                x = x + size.width + this.horizontalGap;
                maxHeight = Math.max(maxHeight, size.height);
            }
            else {
                if (itemsInRow.isEmpty()) {
                    // place in this row (truncated) anyway
                    block.setBounds(
                            new Rectangle2D.Double(
                                    x, y, Math.min(size.width, width - x), size.height
                            )
                    );
                    x = 0.0;
                    y = y + size.height + this.verticalGap;
                }
                else {
                    alignItemsH(itemsInRow, width);
                    // start new row
                    itemsInRow.clear();
                    x = 0.0;
                    y = y + maxHeight + this.verticalGap;
                    maxHeight = size.height;
                    block.setBounds(
                            new Rectangle2D.Double(
                                    x, y, Math.min(size.width, width), size.height
                            )
                    );
                    x = size.width + this.horizontalGap;
                    itemsInRow.add(block);
                }
            }
        }
        alignItemsH(itemsInRow, width);
        return new Size2D(constraint.getWidth(), y + maxHeight);
    }

    protected Size2D arrangeFF(BlockContainer container, Graphics2D g2,
                               RectangleConstraint constraint) {
        Size2D s = arrangeFN(container, g2, constraint);
        if (s.height > constraint.getHeight()) {
            List<Block> blocks = (List<Block>) container.getBlocks();
            List<Block> visibleBlocks = new ArrayList<>();
            for (Block b : blocks) {
                Rectangle2D bounds = b.getBounds();
                double bottom = bounds.getMaxY();
                if (bottom <= constraint.getHeight()) {
                    visibleBlocks.add(b);
                }
            }
            container.clear();
            for (Block b : visibleBlocks) {
                container.add(b);
            }
            s = arrangeFN(container, g2, constraint);
        }
        alignItemsV((List<Block>)container.getBlocks(), s.height, constraint.getHeight());
        return s;
    }

    private void alignItemsH(List<Block> items, double width) {
        if (items.isEmpty()) {
            return;
        }
        double itemsWidth = this.horizontalGap * (items.size() - 1);
        for (Block item : items) {
            itemsWidth += item.getBounds().getWidth();
        }
        if (itemsWidth < width) {
            if (this.horizontalJustify == TbContentJustify.SPACE_AROUND) {
                itemsWidth -= this.horizontalGap * (items.size() - 1);
                double space = width - itemsWidth;
                double itemMargin = space / items.size();
                if (itemMargin > this.horizontalGap) {
                    double x = items.get(0).getBounds().getX() + itemMargin / 2;
                    for (Block item : items) {
                        this.setX(item, x);
                        x += item.getBounds().getWidth() + itemMargin;
                    }
                }
            } else {
                double movement = 0;
                if (horizontalAlignment == HorizontalAlignment.CENTER) {
                    movement = (width - itemsWidth) / 2;
                } else if (horizontalAlignment == HorizontalAlignment.RIGHT) {
                    movement = width - itemsWidth;
                }
                if (movement > 0) {
                    for (Block item : items) {
                        this.moveRight(item, movement);
                    }
                }
            }
        }
    }

    private void alignItemsV(List<Block> items, double itemsHeight, double height) {
        if (itemsHeight < height) {
            double movement = 0;
            if (verticalAlignment == VerticalAlignment.CENTER) {
                movement = (height - itemsHeight) / 2;
            } else if (verticalAlignment == VerticalAlignment.BOTTOM) {
                movement = height - itemsHeight;
            }
            if (movement > 0) {
                for (Block item : items) {
                    this.moveBottom(item, movement);
                }
            }
        }
    }

    private void moveRight(Block item, double movement) {
        Rectangle2D bounds = item.getBounds();
        item.setBounds(new Rectangle2D.Double(bounds.getX() + movement, bounds.getY(), bounds.getWidth(), bounds.getHeight()));
    }

    private void setX(Block item, double x) {
        Rectangle2D bounds = item.getBounds();
        item.setBounds(new Rectangle2D.Double(x, bounds.getY(), bounds.getWidth(), bounds.getHeight()));
    }

    private void moveBottom(Block item, double movement) {
        Rectangle2D bounds = item.getBounds();
        item.setBounds(new Rectangle2D.Double(bounds.getX(), bounds.getY() + movement, bounds.getWidth(), bounds.getHeight()));
    }



    }
