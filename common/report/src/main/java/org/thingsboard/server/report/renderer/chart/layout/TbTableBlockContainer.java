// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.report.renderer.chart.layout;

import org.jfree.chart.block.AbstractBlock;
import org.jfree.chart.block.Block;
import org.jfree.chart.block.BlockContainer;
import org.jfree.chart.block.RectangleConstraint;
import org.jfree.chart.ui.HorizontalAlignment;
import org.jfree.chart.ui.Size2D;
import org.jfree.chart.ui.VerticalAlignment;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TbTableBlockContainer extends AbstractBlock implements Block {

    private final double horizontalGap;
    private final double verticalGap;
    private final List<TbTableBlockColumn> columns;
    private final Map<Integer, Double> columnWidths;
    private double headerHeight = 0;
    private double rowHeight = 0;

    public TbTableBlockContainer(double horizontalGap, double verticalGap) {
        this.horizontalGap = horizontalGap;
        this.verticalGap = verticalGap;
        this.columns = new ArrayList<>();
        this.columnWidths = new HashMap<>();
    }

    public int addColumn(HorizontalAlignment headerAlign, HorizontalAlignment cellAlign, boolean fitWidth) {
        TbTableBlockColumn column = new TbTableBlockColumn(headerAlign, cellAlign, fitWidth);
        this.columns.add(column);
        int index = this.columns.size() - 1;
        this.columnWidths.put(index, 0.0);
        return index;
    }

    public void setColumnHeader(Block header, int column) {
        this.columns.get(column).setHeader(header);
    }

    public void addColumnCell(Block cell, int column) {
        this.columns.get(column).addCell(cell);
    }

    public void clear() {
        this.columns.clear();
        this.columnWidths.clear();
        this.headerHeight = 0;
        this.rowHeight = 0;
    }

    @Override
    public Size2D arrange(Graphics2D g2, RectangleConstraint constraint) {
        if (this.columns.isEmpty()) {
            return new Size2D(0, 0);
        }
        double maxWidth = constraint.getWidth();
        double maxHeight = constraint.getHeight();
        if (maxWidth <= 0 && maxHeight <= 0) {
            maxWidth = Double.MAX_VALUE;
            maxHeight = Double.MAX_VALUE;
        }
        List<Size2D> headerSizes = new ArrayList<>();
        List<Size2D> maxCellSizes = new ArrayList<>();
        for (int i = 0; i < this.columns.size(); i++) {
            TbTableBlockColumn column = this.columns.get(i);
            Size2D[] sizes = column.calculateMaxCellDimensions(g2);
            headerSizes.add(sizes[0]);
            maxCellSizes.add(sizes[1]);
            if (column.fixedWidth()) {
                this.columnWidths.put(i, sizes[1].width);
            }
        }
        for (Size2D header : headerSizes) {
            headerHeight = Math.max(headerHeight, header.getHeight());
        }
        for (Size2D maxCell : maxCellSizes) {
            rowHeight = Math.max(rowHeight, maxCell.getHeight());
        }

        int fitWidthColumnsCount = (int)this.columns.stream().filter(c -> !c.fixedWidth()).count();
        if (fitWidthColumnsCount > 0) {
            int fixedColumnsCount = this.columns.size() - fitWidthColumnsCount;
            double fixedColumnsWidth = 0;
            if (fixedColumnsCount > 0) {
                fixedColumnsWidth = this.columnWidths.values().stream().mapToDouble(Double::doubleValue).sum() + (fixedColumnsCount - 1) * horizontalGap;
            }
            double availableColumnsWidth = Math.max(0, maxWidth - fixedColumnsWidth);
            if (availableColumnsWidth > 0) {
                double fitWidthColumnWidth = (availableColumnsWidth - fitWidthColumnsCount * horizontalGap) / fitWidthColumnsCount;
                this.columns.stream().filter(c -> !c.fixedWidth()).forEach(column -> {
                    int index = this.columns.indexOf(column);
                    this.columnWidths.put(index, fitWidthColumnWidth);
                });
            }
        }

        boolean heightOverflow = false;
        boolean widthOverflow = false;
        double x = 0;
        double y = 0;
        double cellHeight = this.headerHeight;
        if (maxHeight < this.headerHeight) {
            heightOverflow = true;
            cellHeight = 0;
        }
        for (int i = 0; i < this.columns.size(); i++) {
            double leftWidth = widthOverflow ? 0 : Math.max(maxWidth - x, 0);
            double horizontalGap = 0;
            if (i > 0) {
                horizontalGap = this.horizontalGap;
                if (leftWidth < this.horizontalGap) {
                    widthOverflow = true;
                    horizontalGap = 0;
                }
            }
            x += horizontalGap;
            leftWidth = widthOverflow ? 0 : Math.max(maxWidth - x, 0);
            TbTableBlockColumn column = this.columns.get(i);
            double cellWidth = this.columnWidths.get(i);
            if (leftWidth < cellWidth) {
                widthOverflow = true;
                cellWidth = 0;
            }
            column.setHeaderBounds(g2, new Rectangle2D.Double(x, y, cellWidth, cellHeight));
            x += cellWidth;
        }
        y += cellHeight;

        int rows = this.columns.get(0).getCellsCount();
        for (int row = 0; row < rows; row++) {
            double leftHeight = heightOverflow ? 0 : Math.max(maxHeight - y, 0);
            double verticalGap = this.verticalGap;
            if (leftHeight < this.verticalGap) {
                heightOverflow = true;
                verticalGap = 0;
            }
            y += verticalGap;
            leftHeight = heightOverflow ? 0 : Math.max(maxHeight - y, 0);
            cellHeight = this.rowHeight;
            if (leftHeight < this.rowHeight) {
                heightOverflow = true;
                cellHeight = 0;
            }
            x = 0;
            widthOverflow = false;
            for (int c = 0; c < this.columns.size(); c++) {
                double leftWidth = widthOverflow ? 0 : Math.max(maxWidth - x, 0);
                double horizontalGap = 0;
                if (c > 0) {
                    horizontalGap = this.horizontalGap;
                    if (leftWidth < this.horizontalGap) {
                        widthOverflow = true;
                        horizontalGap = 0;
                    }
                }
                x += horizontalGap;
                leftWidth = widthOverflow ? 0 : Math.max(maxWidth - x, 0);
                TbTableBlockColumn column = this.columns.get(c);
                double cellWidth = this.columnWidths.get(c);
                if (leftWidth < cellWidth) {
                    widthOverflow = true;
                    cellWidth = 0;
                }
                column.setCellBounds(g2, new Rectangle2D.Double(x, y, cellWidth, cellHeight), row);
                x += cellWidth;
            }
            y += cellHeight;
        }
        return new Size2D(x, y);
    }

    @Override
    public void draw(Graphics2D g2, Rectangle2D area) {
        draw(g2, area, null);
    }

    @Override
    public Object draw(Graphics2D g2, Rectangle2D area, Object params) {
        Rectangle2D target = (Rectangle2D) area.clone();
        for (TbTableBlockColumn column : this.columns) {
            column.draw(g2, target);
        }
        return null;
    }

    public static class TbTableBlockColumn {

        private final BlockContainer header;
        private final List<BlockContainer> cells;

        private final HorizontalAlignment cellAlign;
        private final boolean fitWidth;

        public TbTableBlockColumn(HorizontalAlignment headerAlign, HorizontalAlignment cellAlign, boolean fitWidth) {
            this.cellAlign = cellAlign;
            this.fitWidth = fitWidth;
            this.header = new BlockContainer(new TbFlowArrangement(headerAlign, VerticalAlignment.CENTER, 0, 0));
            this.cells = new ArrayList<>();
        }

        public void setHeader(Block header) {
            this.header.add(header);
        }

        public void addCell(Block cell) {
            BlockContainer cellContainer = new BlockContainer(new TbFlowArrangement(cellAlign, VerticalAlignment.CENTER, 0, 0));
            cellContainer.add(cell);
            this.cells.add(cellContainer);
        }

        public Size2D[] calculateMaxCellDimensions(Graphics2D g2) {
            Size2D[] sizes = new Size2D[2];
            RectangleConstraint constraint = RectangleConstraint.NONE;
            double maxWidth = 0;
            double maxHeight = 0;
            Size2D size = this.header.arrange(g2, constraint);
            sizes[0] = size;
            maxWidth = Math.max(maxWidth, size.getWidth());
            maxHeight = Math.max(maxHeight, size.getHeight());
            for (BlockContainer cell : cells) {
                size = cell.arrange(g2, constraint);
                maxWidth = Math.max(maxWidth, size.getWidth());
                maxHeight = Math.max(maxHeight, size.getHeight());
            }
            sizes[1] = new Size2D(maxWidth, maxHeight);
            return sizes;
        }

        public int getCellsCount() {
            return cells.size();
        }

        public void setHeaderBounds(Graphics2D g2, Rectangle2D bounds) {
            this.header.arrange(g2, new RectangleConstraint(bounds.getWidth(), bounds.getHeight()));
            this.header.setBounds(bounds);
        }

        public void setCellBounds(Graphics2D g2, Rectangle2D bounds, int row) {
            BlockContainer cell = this.cells.get(row);
            cell.arrange(g2, new RectangleConstraint(bounds.getWidth(), bounds.getHeight()));
            cell.setBounds(bounds);
        }

        public boolean fixedWidth() {
            return !fitWidth;
        }

        public void draw(Graphics2D g2, Rectangle2D area) {
            double x = area.getX();
            double y = area.getY();
            Rectangle2D headerBounds = this.header.getBounds();
            if (!headerBounds.isEmpty()) {
                Rectangle2D headerArea = new Rectangle2D.Double(x + headerBounds.getX(), y + headerBounds.getY(), headerBounds.getWidth(), headerBounds.getHeight());
                this.header.draw(g2, headerArea);
            }
            for (BlockContainer cell : cells) {
                Rectangle2D cellBounds = cell.getBounds();
                if (!cellBounds.isEmpty()) {
                    Rectangle2D cellArea = new Rectangle2D.Double(x + cellBounds.getX(), y + cellBounds.getY(), cellBounds.getWidth(), cellBounds.getHeight());
                    cell.draw(g2, cellArea);
                }
            }
        }

    }
}
