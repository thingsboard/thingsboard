// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.report.renderer.chart.legend;

import org.jfree.chart.block.Arrangement;
import org.jfree.chart.block.Block;
import org.jfree.chart.block.BlockContainer;
import org.jfree.chart.block.LabelBlock;
import org.jfree.chart.block.RectangleConstraint;
import org.jfree.chart.title.LegendGraphic;
import org.jfree.chart.title.Title;
import org.jfree.chart.ui.HorizontalAlignment;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.chart.ui.Size2D;
import org.jfree.chart.ui.VerticalAlignment;
import org.thingsboard.server.report.context.chart.TsChartRangeItem;
import org.thingsboard.server.report.renderer.chart.layout.TbColumnArrangement;
import org.thingsboard.server.report.renderer.chart.layout.TbFlowArrangement;
import org.thingsboard.server.report.renderer.chart.layout.TbTableBlockContainer;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.thingsboard.server.report.util.AwtFontUtils.newFont;
import static org.thingsboard.server.report.util.ColorUtils.safeParseCssColor;

public class TbRangeLegendTitle extends Title {

    private static final Shape LEGEND_ITEM_SHAPE = new Ellipse2D.Double(-4f, -4f, 8f, 8f);

    private final List<TsChartRangeItem> rangeItems;

    private Block legendContainer;

    private Font legendLabelFont;
    private Paint legendLabelPaint;

    private double maxRelativeWidth = 0;
    private double maxRelativeHeight = 0;

    public TbRangeLegendTitle(List<TsChartRangeItem> rangeItems) {
        this.rangeItems = rangeItems;

        this.legendLabelFont = newFont("Roboto", Font.PLAIN, 12);
        this.legendLabelPaint = safeParseCssColor("rgba(0, 0, 0, 0.76)");
    }

    public void setLegendLabelFont(Font legendLabelFont) {
        this.legendLabelFont = legendLabelFont;
    }

    public void setLegendLabelPaint(Paint legendLabelPaint) {
        this.legendLabelPaint = legendLabelPaint;
    }

    public void setMaxRelativeWidth(double maxRelativeWidth) {
        this.maxRelativeWidth = maxRelativeWidth;
    }

    public void setMaxRelativeHeight(double maxRelativeHeight) {
        this.maxRelativeHeight = maxRelativeHeight;
    }

    @Override
    public Size2D arrange(Graphics2D g2, RectangleConstraint constraint) {
        this.buildLegendItems();
        RectangleEdge p = getPosition();
        RectangleConstraint targetConstraint = constraint;
        if (RectangleEdge.isTopOrBottom(p)) {
            if (this.maxRelativeHeight > 0) {
                double maxHeight = constraint.getHeight() * this.maxRelativeHeight;
                targetConstraint = toContentConstraint(new RectangleConstraint(constraint.getWidth(), maxHeight));
            } else {
                targetConstraint = toContentConstraint(constraint.toFixedWidth(constraint.getWidth()));
            }
        } else {
            if (this.maxRelativeWidth > 0) {
                double maxWidth = constraint.getWidth() * this.maxRelativeWidth;
                targetConstraint = toContentConstraint(new RectangleConstraint(maxWidth, constraint.getHeight()));
            }
        }
        Size2D size = this.legendContainer.arrange(g2, targetConstraint);Size2D result = new Size2D();
        result.height = calculateTotalHeight(size.height);
        result.width = calculateTotalWidth(size.width);
        return result;
    }

    @Override
    public void draw(Graphics2D g2, Rectangle2D area) {
        draw(g2, area, null);
    }

    @Override
    public Object draw(Graphics2D g2, Rectangle2D area, Object params) {
        Rectangle2D target = (Rectangle2D) area.clone();
        target = trimMargin(target);
        target = trimBorder(target);
        target = trimPadding(target);
        return this.legendContainer.draw(g2, target, params);
    }

    private void buildLegendItems() {
        Arrangement arrangement;
        List<TsChartRangeItem> items;
        if (RectangleEdge.isTopOrBottom(getPosition())) {
            items = rangeItems;
            arrangement = new TbFlowArrangement(HorizontalAlignment.CENTER, VerticalAlignment.CENTER, 24.0, 8.0);
        } else {
            items = new ArrayList<>(rangeItems);
            Collections.reverse(items);
            arrangement = new TbColumnArrangement(HorizontalAlignment.LEFT, VerticalAlignment.TOP, 24.0, 8.0);
        }
        this.legendContainer = new BlockContainer(arrangement);
        for (TsChartRangeItem item : items) {
            Block itemBlock = this.buildLegendItem(item);
            ((BlockContainer) this.legendContainer).add(itemBlock);
        }
    }

    private Block buildLegendItem(TsChartRangeItem item) {
        TbTableBlockContainer legendBlock = new TbTableBlockContainer(4.0, 0.0);

        int shape = legendBlock.addColumn(HorizontalAlignment.LEFT, HorizontalAlignment.LEFT, false);
        Block shapeBlock = this.buildLegendShape(item);
        legendBlock.setColumnHeader(shapeBlock, shape);

        int label = legendBlock.addColumn(HorizontalAlignment.LEFT, HorizontalAlignment.LEFT, false);
        Block labelBlock = this.buildLegendLabel(item);
        legendBlock.setColumnHeader(labelBlock, label);

        return legendBlock;
    }

    private Block buildLegendShape(TsChartRangeItem item) {
        Paint shapeFill = safeParseCssColor(item.getColor());
        LegendGraphic lg = new LegendGraphic(LEGEND_ITEM_SHAPE, shapeFill);
        lg.setPadding(RectangleInsets.ZERO_INSETS);
        return lg;
    }

    private Block buildLegendLabel(TsChartRangeItem item) {
        String label = item.getLabel();
        Paint labelPaint = this.legendLabelPaint;
        return new LabelBlock(label, this.legendLabelFont, labelPaint);
    }

}
