// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.report.renderer.chart.legend;

import org.jfree.chart.block.Arrangement;
import org.jfree.chart.block.Block;
import org.jfree.chart.block.BlockContainer;
import org.jfree.chart.block.BlockFrame;
import org.jfree.chart.block.ColumnArrangement;
import org.jfree.chart.block.FlowArrangement;
import org.jfree.chart.block.LabelBlock;
import org.jfree.chart.block.RectangleConstraint;
import org.jfree.chart.ui.HorizontalAlignment;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.chart.ui.Size2D;
import org.jfree.chart.ui.VerticalAlignment;
import org.thingsboard.server.common.data.report.configuration.chart.LegendConfig;
import org.thingsboard.server.report.renderer.chart.layout.TbColumnArrangement;
import org.thingsboard.server.report.renderer.chart.layout.TbFlowArrangement;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Legend rendered as a set of cards, one card per data key: the key label on top and, when enabled,
 * the legend values (title above value) below it. Cards are placed in a row (wrapping) for a top/bottom legend
 * and stacked in a column of equal-width cards for a left/right legend.
 */
public class TbCardLegendTitle extends TbTableLegendTitle {

    private static final double CARDS_GAP = 8.0;
    private static final double CARD_CONTENT_GAP = 8.0;
    private static final RectangleInsets CARD_PADDING = new RectangleInsets(8.0, 12.0, 8.0, 12.0);
    private static final Color CARD_BORDER_COLOR = new Color(0, 0, 0, 31); // rgba(0, 0, 0, 0.12)
    private static final double CARD_BORDER_RADIUS = 4.0;
    private static final double VALUES_COLUMN_GAP = 16.0;
    private static final double VALUES_ROW_GAP = 4.0;

    private BlockContainer cards = new BlockContainer();

    public TbCardLegendTitle(TbLegendItemSource source, LegendConfig config) {
        super(source, config);
    }

    @Override
    public Size2D arrange(Graphics2D g2, RectangleConstraint constraint) {
        boolean leftOrRight = RectangleEdge.isLeftOrRight(getPosition());
        this.cards = new BlockContainer(this.createCardsArrangement(leftOrRight));
        for (TbLegendItem legendItem : this.loadLegendItems()) {
            this.cards.add(this.createCardBlock(legendItem));
        }
        Size2D size = this.cards.arrange(g2, toContentConstraint(constraint));
        if (leftOrRight) {
            this.equalizeCardWidths();
        }
        Size2D result = new Size2D();
        result.height = calculateTotalHeight(size.height);
        result.width = calculateTotalWidth(size.width);
        return result;
    }

    @Override
    public Object draw(Graphics2D g2, Rectangle2D area, Object params) {
        Rectangle2D target = (Rectangle2D) area.clone();
        target = trimMargin(target);
        target = trimBorder(target);
        target = trimPadding(target);
        return this.cards.draw(g2, target, params);
    }

    private Arrangement createCardsArrangement(boolean leftOrRight) {
        if (leftOrRight) {
            TbColumnArrangement arrangement = new TbColumnArrangement(HorizontalAlignment.LEFT, VerticalAlignment.TOP, CARDS_GAP, CARDS_GAP);
            arrangement.setMaxRelativeWidth(this.maxRelativeWidth);
            return arrangement;
        } else {
            TbFlowArrangement arrangement = new TbFlowArrangement(HorizontalAlignment.LEFT, VerticalAlignment.TOP, CARDS_GAP, CARDS_GAP);
            arrangement.setMaxRelativeHeight(this.maxRelativeHeight);
            return arrangement;
        }
    }

    private Block createCardBlock(TbLegendItem legendItem) {
        CardBlockContainer card = new CardBlockContainer(
                new ColumnArrangement(HorizontalAlignment.LEFT, VerticalAlignment.TOP, 0.0, CARD_CONTENT_GAP));
        card.setFrame(new RoundedBlockBorder(CARD_BORDER_COLOR, CARD_BORDER_RADIUS));
        card.setPadding(CARD_PADDING);
        card.add(this.createLegendItemBlock(legendItem.getLegendItem()));
        BlockContainer values = this.createValuesBlock(legendItem.getLegendValues());
        if (!values.isEmpty()) {
            card.add(values);
        }
        return card;
    }

    private BlockContainer createValuesBlock(TbLegendValues values) {
        BlockContainer result = new BlockContainer(
                new FlowArrangement(HorizontalAlignment.LEFT, VerticalAlignment.TOP, VALUES_COLUMN_GAP, VALUES_ROW_GAP));
        if (this.legendConfig.getShowMin()) {
            result.add(this.createValueItemBlock("Min", values.getMin()));
        }
        if (this.legendConfig.getShowMax()) {
            result.add(this.createValueItemBlock("Max", values.getMax()));
        }
        if (this.legendConfig.getShowAvg()) {
            result.add(this.createValueItemBlock("Avg", values.getAvg()));
        }
        if (this.legendConfig.getShowTotal()) {
            result.add(this.createValueItemBlock("Total", values.getTotal()));
        }
        if (this.legendConfig.getShowLatest()) {
            result.add(this.createValueItemBlock("Latest", values.getLatest()));
        }
        return result;
    }

    private Block createValueItemBlock(String title, String value) {
        BlockContainer result = new BlockContainer(new ColumnArrangement(HorizontalAlignment.LEFT, VerticalAlignment.TOP, 0.0, 0.0));
        result.add(new LabelBlock(title, this.legendColumnTitleFont, this.legendColumnTitlePaint));
        result.add(new LabelBlock(value != null ? value : "", this.legendValueFont, this.legendValuePaint));
        return result;
    }

    /**
     * Stretches the cards of each column to the width of the widest card in that column.
     */
    private void equalizeCardWidths() {
        List<Block> blocks = (List<Block>) this.cards.getBlocks();
        Map<Double, Double> columnWidths = new HashMap<>();
        for (Block block : blocks) {
            Rectangle2D bounds = block.getBounds();
            columnWidths.merge(bounds.getX(), bounds.getWidth(), Math::max);
        }
        for (Block block : blocks) {
            Rectangle2D bounds = block.getBounds();
            block.setBounds(new Rectangle2D.Double(bounds.getX(), bounds.getY(), columnWidths.get(bounds.getX()), bounds.getHeight()));
        }
    }

    /**
     * {@link BlockContainer} ignores its own border and padding both in {@link BlockContainer#arrange} and in
     * {@link BlockContainer#draw}. Cards need them, so the size includes them and children are drawn inside them.
     */
    private static class CardBlockContainer extends BlockContainer {

        CardBlockContainer(Arrangement arrangement) {
            super(arrangement);
        }

        @Override
        public Size2D arrange(Graphics2D g2, RectangleConstraint constraint) {
            Size2D contentSize = getArrangement().arrange(this, g2, toContentConstraint(constraint));
            return new Size2D(calculateTotalWidth(contentSize.getWidth()), calculateTotalHeight(contentSize.getHeight()));
        }

        @Override
        public Object draw(Graphics2D g2, Rectangle2D area, Object params) {
            Rectangle2D contentArea = trimMargin((Rectangle2D) area.clone());
            drawBorder(g2, contentArea);
            contentArea = trimPadding(trimBorder(contentArea));
            for (Object object : getBlocks()) {
                Block block = (Block) object;
                Rectangle2D bounds = block.getBounds();
                block.draw(g2, new Rectangle2D.Double(bounds.getX() + contentArea.getX(), bounds.getY() + contentArea.getY(),
                        bounds.getWidth(), bounds.getHeight()), params);
            }
            return null;
        }
    }

    private static class RoundedBlockBorder implements BlockFrame {

        private static final RectangleInsets INSETS = new RectangleInsets(1.0, 1.0, 1.0, 1.0);

        private final Paint paint;
        private final double radius;

        RoundedBlockBorder(Paint paint, double radius) {
            this.paint = paint;
            this.radius = radius;
        }

        @Override
        public RectangleInsets getInsets() {
            return INSETS;
        }

        @Override
        public void draw(Graphics2D g2, Rectangle2D area) {
            Paint savedPaint = g2.getPaint();
            Stroke savedStroke = g2.getStroke();
            Object savedAntialiasing = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
            g2.setPaint(this.paint);
            g2.setStroke(new BasicStroke(1.0f));
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.draw(new RoundRectangle2D.Double(area.getX() + 0.5, area.getY() + 0.5,
                    area.getWidth() - 1.0, area.getHeight() - 1.0, this.radius * 2, this.radius * 2));
            g2.setPaint(savedPaint);
            g2.setStroke(savedStroke);
            if (savedAntialiasing != null) {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, savedAntialiasing);
            }
        }
    }
}
