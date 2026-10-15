// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.report.renderer.chart.legend;

import org.jfree.chart.LegendItem;
import org.jfree.chart.block.Block;
import org.jfree.chart.block.BlockContainer;
import org.jfree.chart.block.BorderArrangement;
import org.jfree.chart.block.CenterArrangement;
import org.jfree.chart.block.EmptyBlock;
import org.jfree.chart.block.LabelBlock;
import org.jfree.chart.block.RectangleConstraint;
import org.jfree.chart.title.LegendGraphic;
import org.jfree.chart.title.LegendItemBlockContainer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.title.TextTitle;
import org.jfree.chart.title.Title;
import org.jfree.chart.ui.HorizontalAlignment;
import org.jfree.chart.ui.RectangleAnchor;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.chart.ui.Size2D;
import org.jfree.chart.ui.VerticalAlignment;
import org.thingsboard.server.common.data.report.configuration.chart.LegendConfig;
import org.thingsboard.server.report.renderer.chart.layout.TbTableBlockContainer;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.geom.Rectangle2D;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.thingsboard.server.report.util.AwtFontUtils.newFont;
import static org.thingsboard.server.report.util.ColorUtils.safeParseCssColor;

public class TbTableLegendTitle extends Title {

    private final TbLegendItemSource source;
    private final LegendConfig legendConfig;
    private final TbTableBlockContainer legendTable;

    private Comparator<TbLegendItem> legendItemComparator;

    private RectangleInsets legendItemGraphicPadding;
    private RectangleAnchor legendItemGraphicAnchor;
    private RectangleAnchor legendItemGraphicLocation;
    private RectangleEdge legendItemGraphicEdge;

    private Font itemFont;
    private Paint itemPaint;
    private RectangleInsets itemLabelPadding;

    private Font legendColumnTitleFont;
    private Paint legendColumnTitlePaint;

    private Font legendValueFont;
    private Paint legendValuePaint;

    private double maxRelativeWidth = 0;
    private double maxRelativeHeight = 0;

    public TbTableLegendTitle(TbLegendItemSource source, LegendConfig config) {
        this.source = source;
        this.legendConfig = config;
        this.legendTable = new TbTableBlockContainer(16.0, 8.0);

        this.legendItemGraphicPadding = new RectangleInsets(2.0, 2.0, 2.0, 2.0);
        this.legendItemGraphicAnchor = RectangleAnchor.CENTER;
        this.legendItemGraphicLocation = RectangleAnchor.CENTER;
        this.legendItemGraphicEdge = RectangleEdge.LEFT;

        this.itemFont = LegendTitle.DEFAULT_ITEM_FONT;
        this.itemPaint = LegendTitle.DEFAULT_ITEM_PAINT;
        this.itemLabelPadding = new RectangleInsets(2.0, 2.0, 2.0, 2.0);

        this.legendColumnTitleFont = newFont("Roboto", Font.PLAIN, 12);
        this.legendColumnTitlePaint = safeParseCssColor("rgba(0, 0, 0, 0.38)");

        this.legendValueFont = newFont("RobotoMedium", Font.PLAIN, 12);
        this.legendValuePaint = safeParseCssColor("rgba(0, 0, 0, 0.87)");
    }

    public void setLegendItemComparator(Comparator<TbLegendItem> legendItemComparator) {
        this.legendItemComparator = legendItemComparator;
    }

    public void setLegendItemGraphicPadding(RectangleInsets legendItemGraphicPadding) {
        this.legendItemGraphicPadding = legendItemGraphicPadding;
    }

    public void setLegendItemGraphicAnchor(RectangleAnchor legendItemGraphicAnchor) {
        this.legendItemGraphicAnchor = legendItemGraphicAnchor;
    }

    public void setLegendItemGraphicLocation(RectangleAnchor legendItemGraphicLocation) {
        this.legendItemGraphicLocation = legendItemGraphicLocation;
    }

    public void setLegendItemGraphicEdge(RectangleEdge legendItemGraphicEdge) {
        this.legendItemGraphicEdge = legendItemGraphicEdge;
    }

    public void setItemFont(Font itemFont) {
        this.itemFont = itemFont;
    }

    public void setItemPaint(Paint itemPaint) {
        this.itemPaint = itemPaint;
    }

    public void setItemLabelPadding(RectangleInsets itemLabelPadding) {
        this.itemLabelPadding = itemLabelPadding;
    }

    public void setLegendColumnTitleFont(Font legendColumnTitleFont) {
        this.legendColumnTitleFont = legendColumnTitleFont;
    }

    public void setLegendColumnTitlePaint(Paint legendColumnTitlePaint) {
        this.legendColumnTitlePaint = legendColumnTitlePaint;
    }

    public void setLegendValueFont(Font legendValueFont) {
        this.legendValueFont = legendValueFont;
    }

    public void setLegendValuePaint(Paint legendValuePaint) {
        this.legendValuePaint = legendValuePaint;
    }

    public void setMaxRelativeWidth(double maxRelativeWidth) {
        this.maxRelativeWidth = maxRelativeWidth;
    }

    public void setMaxRelativeHeight(double maxRelativeHeight) {
        this.maxRelativeHeight = maxRelativeHeight;
    }

    @Override
    public Size2D arrange(Graphics2D g2, RectangleConstraint constraint) {
        this.fetchLegendItems();
        RectangleEdge p = getPosition();
        RectangleConstraint targetConstraint = constraint;
        if (RectangleEdge.isTopOrBottom(p)) {
            if (this.maxRelativeHeight > 0) {
                double maxHeight = constraint.getHeight() * this.maxRelativeHeight;
                targetConstraint = toContentConstraint(new RectangleConstraint(constraint.getWidth(), maxHeight));
            }
        } else {
            if (this.maxRelativeWidth > 0) {
                double maxWidth = constraint.getWidth() * this.maxRelativeWidth;
                targetConstraint = toContentConstraint(new RectangleConstraint(maxWidth, constraint.getHeight()));
            }
        }
        Size2D size = this.legendTable.arrange(g2, targetConstraint);
        Size2D result = new Size2D();
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
        return this.legendTable.draw(g2, target, params);
    }

    private void fetchLegendItems() {
        this.legendTable.clear();
        TbLegendValuesRequest request = this.buildLegendValuesRequest();
        List<TbLegendItem> legendItems = this.source.getTbLegendItems(request);
        if (legendItems != null && !legendItems.isEmpty()) {
            if (this.legendItemComparator != null) {
                legendItems.sort(this.legendItemComparator);
            }
            RectangleEdge p = getPosition();
            if (RectangleEdge.isTopOrBottom(p)) {
                this.buildVerticalLegendItems(legendItems);
            } else {
                this.buildHorizontalLegendItems(legendItems);
            }
        }
    }

    private TbLegendValuesRequest buildLegendValuesRequest() {
        TbLegendValuesRequest request = new TbLegendValuesRequest();
        if (legendConfig.getShowMin()) {
            request.setMin(true);
        }
        if (legendConfig.getShowMax()) {
            request.setMax(true);
        }
        if (legendConfig.getShowAvg()) {
            request.setAvg(true);
        }
        if (legendConfig.getShowTotal()) {
            request.setTotal(true);
        }
        if (legendConfig.getShowLatest()) {
            request.setLatest(true);
        }
        return request;
    }

    private void buildVerticalLegendItems(List<TbLegendItem> legendItems) {
        Map<String, Integer> legendTableColumns = new HashMap<>();
        int legend = this.legendTable.addColumn(HorizontalAlignment.LEFT, HorizontalAlignment.LEFT, true);
        this.legendTable.setColumnHeader(new EmptyBlock(0,0), legend);
        legendTableColumns.put("legend", legend);

        if (legendConfig.getShowMin()) {
            int min = this.legendTable.addColumn(HorizontalAlignment.RIGHT, HorizontalAlignment.RIGHT, false);
            this.legendTable.setColumnHeader(new LabelBlock("Min", this.legendColumnTitleFont, this.legendColumnTitlePaint), min);
            legendTableColumns.put("min", min);
        }
        if (legendConfig.getShowMax()) {
            int max = this.legendTable.addColumn(HorizontalAlignment.RIGHT, HorizontalAlignment.RIGHT, false);
            this.legendTable.setColumnHeader(new LabelBlock("Max", this.legendColumnTitleFont, this.legendColumnTitlePaint), max);
            legendTableColumns.put("max", max);
        }
        if (legendConfig.getShowAvg()) {
            int avg = this.legendTable.addColumn(HorizontalAlignment.RIGHT, HorizontalAlignment.RIGHT, false);
            this.legendTable.setColumnHeader(new LabelBlock("Avg", this.legendColumnTitleFont, this.legendColumnTitlePaint), avg);
            legendTableColumns.put("avg", avg);
        }
        if (legendConfig.getShowTotal()) {
            int total = this.legendTable.addColumn(HorizontalAlignment.RIGHT, HorizontalAlignment.RIGHT, false);
            this.legendTable.setColumnHeader(new LabelBlock("Total", this.legendColumnTitleFont, this.legendColumnTitlePaint), total);
            legendTableColumns.put("total", total);
        }
        if (legendConfig.getShowLatest()) {
            int latest = this.legendTable.addColumn(HorizontalAlignment.RIGHT, HorizontalAlignment.RIGHT, false);
            this.legendTable.setColumnHeader(new LabelBlock("Latest", this.legendColumnTitleFont, this.legendColumnTitlePaint), latest);
            legendTableColumns.put("latest", latest);
        }
        for (TbLegendItem legendItem : legendItems) {
            this.addVerticalItemBlock(legendItem, legendTableColumns);
        }
    }

    private void buildHorizontalLegendItems(List<TbLegendItem> legendItems) {
        int legendValuesTitles = this.legendTable.addColumn(HorizontalAlignment.LEFT, HorizontalAlignment.LEFT, false);
        this.legendTable.setColumnHeader(new EmptyBlock(0,0), legendValuesTitles);
        if (legendConfig.getShowMin()) {
            this.legendTable.addColumnCell(new LabelBlock("Min", this.legendColumnTitleFont, this.legendColumnTitlePaint), legendValuesTitles);
        }
        if (legendConfig.getShowMax()) {
            this.legendTable.addColumnCell(new LabelBlock("Max", this.legendColumnTitleFont, this.legendColumnTitlePaint), legendValuesTitles);
        }
        if (legendConfig.getShowAvg()) {
            this.legendTable.addColumnCell(new LabelBlock("Avg", this.legendColumnTitleFont, this.legendColumnTitlePaint), legendValuesTitles);
        }
        if (legendConfig.getShowTotal()) {
            this.legendTable.addColumnCell(new LabelBlock("Total", this.legendColumnTitleFont, this.legendColumnTitlePaint), legendValuesTitles);
        }
        if (legendConfig.getShowLatest()) {
            this.legendTable.addColumnCell(new LabelBlock("Latest", this.legendColumnTitleFont, this.legendColumnTitlePaint), legendValuesTitles);
        }
        for (TbLegendItem legendItem : legendItems) {
            this.addHorizontalItemBlock(legendItem);
        }
    }

    private void addVerticalItemBlock(TbLegendItem item, Map<String, Integer> legendTableColumns) {
        this.legendTable.addColumnCell(createLegendItemBlock(item.getLegendItem()), legendTableColumns.get("legend"));
        TbLegendValues values = item.getLegendValues();
        if (this.legendConfig.getShowMin()) {
            LabelBlock valueBlock = new LabelBlock(values.getMin(), this.legendValueFont, this.legendValuePaint);
            this.legendTable.addColumnCell(valueBlock, legendTableColumns.get("min"));
        }
        if (this.legendConfig.getShowMax()) {
            LabelBlock valueBlock = new LabelBlock(values.getMax(), this.legendValueFont, this.legendValuePaint);
            this.legendTable.addColumnCell(valueBlock, legendTableColumns.get("max"));
        }
        if (this.legendConfig.getShowAvg()) {
            LabelBlock valueBlock = new LabelBlock(values.getAvg(), this.legendValueFont, this.legendValuePaint);
            this.legendTable.addColumnCell(valueBlock, legendTableColumns.get("avg"));
        }
        if (this.legendConfig.getShowTotal()) {
            LabelBlock valueBlock = new LabelBlock(values.getTotal(), this.legendValueFont, this.legendValuePaint);
            this.legendTable.addColumnCell(valueBlock, legendTableColumns.get("total"));
        }
        if (this.legendConfig.getShowLatest()) {
            LabelBlock valueBlock = new LabelBlock(values.getLatest(), this.legendValueFont, this.legendValuePaint);
            this.legendTable.addColumnCell(valueBlock, legendTableColumns.get("latest"));
        }
    }

    private void addHorizontalItemBlock(TbLegendItem item) {
        int column = this.legendTable.addColumn(HorizontalAlignment.CENTER, HorizontalAlignment.RIGHT, false);
        this.legendTable.setColumnHeader(createLegendItemBlock(item.getLegendItem()), column);
        TbLegendValues values = item.getLegendValues();
        if (this.legendConfig.getShowMin()) {
            LabelBlock valueBlock = new LabelBlock(values.getMin(), this.legendValueFont, this.legendValuePaint);
            this.legendTable.addColumnCell(valueBlock, column);
        }
        if (this.legendConfig.getShowMax()) {
            LabelBlock valueBlock = new LabelBlock(values.getMax(), this.legendValueFont, this.legendValuePaint);
            this.legendTable.addColumnCell(valueBlock, column);
        }
        if (this.legendConfig.getShowAvg()) {
            LabelBlock valueBlock = new LabelBlock(values.getAvg(), this.legendValueFont, this.legendValuePaint);
            this.legendTable.addColumnCell(valueBlock, column);
        }
        if (this.legendConfig.getShowTotal()) {
            LabelBlock valueBlock = new LabelBlock(values.getTotal(), this.legendValueFont, this.legendValuePaint);
            this.legendTable.addColumnCell(valueBlock, column);
        }
        if (this.legendConfig.getShowLatest()) {
            LabelBlock valueBlock = new LabelBlock(values.getLatest(), this.legendValueFont, this.legendValuePaint);
            this.legendTable.addColumnCell(valueBlock, column);
        }
    }


    private Block createLegendItemBlock(LegendItem item) {
        BlockContainer result;
        LegendGraphic lg = new LegendGraphic(item.getShape(),
                item.getFillPaint());
        lg.setFillPaintTransformer(item.getFillPaintTransformer());
        lg.setShapeFilled(item.isShapeFilled());
        lg.setLine(item.getLine());
        lg.setLineStroke(item.getLineStroke());
        lg.setLinePaint(item.getLinePaint());
        lg.setLineVisible(item.isLineVisible());
        lg.setShapeVisible(item.isShapeVisible());
        lg.setShapeOutlineVisible(item.isShapeOutlineVisible());
        lg.setOutlinePaint(item.getOutlinePaint());
        lg.setOutlineStroke(item.getOutlineStroke());
        lg.setPadding(this.legendItemGraphicPadding);

        LegendItemBlockContainer legendItem = new LegendItemBlockContainer(
                new BorderArrangement(), item.getDataset(),
                item.getSeriesKey());
        lg.setShapeAnchor(this.legendItemGraphicAnchor);
        lg.setShapeLocation(this.legendItemGraphicLocation);
        legendItem.add(lg, this.legendItemGraphicEdge);
        Font textFont = item.getLabelFont();
        if (textFont == null) {
            textFont = this.itemFont;
        }
        Paint textPaint = item.getLabelPaint();
        if (textPaint == null) {
            textPaint = this.itemPaint;
        }
        TextTitle labelBlock = new TextTitle(item.getLabel(), textFont,
                textPaint, Title.DEFAULT_POSITION,
                HorizontalAlignment.LEFT,
                VerticalAlignment.CENTER, this.itemLabelPadding);
        labelBlock.setMaximumLinesToDisplay(1);
        legendItem.add(labelBlock);
        legendItem.setToolTipText(item.getToolTipText());
        legendItem.setURLText(item.getURLText());

        result = new BlockContainer(new CenterArrangement());
        result.add(legendItem);

        return result;
    }
}
