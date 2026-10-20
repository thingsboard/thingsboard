// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.report.renderer.chart.legend;

import org.jfree.chart.LegendItem;
import org.jfree.chart.LegendItemSource;
import org.jfree.chart.block.Arrangement;
import org.jfree.chart.block.Block;
import org.jfree.chart.block.BlockContainer;
import org.jfree.chart.block.BorderArrangement;
import org.jfree.chart.block.CenterArrangement;
import org.jfree.chart.title.LegendGraphic;
import org.jfree.chart.title.LegendItemBlockContainer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.title.TextTitle;
import org.jfree.chart.title.Title;
import org.jfree.chart.ui.HorizontalAlignment;
import org.jfree.chart.ui.VerticalAlignment;

import java.awt.Font;
import java.awt.Paint;

public class TbLegendTitle extends LegendTitle {


    public TbLegendTitle(LegendItemSource source, Arrangement hLayout, Arrangement vLayout) {
        super(source, hLayout, vLayout);
    }

    @Override
    protected Block createLegendItemBlock(LegendItem item) {
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
        lg.setPadding(this.getLegendItemGraphicPadding());

        LegendItemBlockContainer legendItem = new LegendItemBlockContainer(
                new BorderArrangement(), item.getDataset(),
                item.getSeriesKey());
        lg.setShapeAnchor(getLegendItemGraphicAnchor());
        lg.setShapeLocation(getLegendItemGraphicLocation());
        legendItem.add(lg, this.getLegendItemGraphicEdge());
        Font textFont = item.getLabelFont();
        if (textFont == null) {
            textFont = this.getItemFont();
        }
        Paint textPaint = item.getLabelPaint();
        if (textPaint == null) {
            textPaint = this.getItemPaint();
        }
        TextTitle labelBlock = new TextTitle(item.getLabel(), textFont,
                textPaint, Title.DEFAULT_POSITION,
                HorizontalAlignment.LEFT,
                VerticalAlignment.CENTER, this.getItemLabelPadding());
        labelBlock.setMaximumLinesToDisplay(1);
        legendItem.add(labelBlock);
        legendItem.setToolTipText(item.getToolTipText());
        legendItem.setURLText(item.getURLText());

        result = new BlockContainer(new CenterArrangement());
        result.add(legendItem);

        return result;
    }
}
