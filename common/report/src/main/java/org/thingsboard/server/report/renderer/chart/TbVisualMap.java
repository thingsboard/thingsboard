// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.report.renderer.chart;

import lombok.Data;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.data.Range;
import org.thingsboard.server.report.context.chart.TsChartRangeItem;

import java.awt.Color;
import java.awt.Paint;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.thingsboard.server.report.renderer.chart.ChartUtils.createFillPaint;
import static org.thingsboard.server.report.util.ColorUtils.safeParseCssColor;

@Data
public class TbVisualMap {

    private final Paint outOfRangePaint;
    private final Paint outOfRangeFillPaint;
    private final List<TbVisualMapPiece> pieces;

    public TbVisualMap(String outOfRangeColor, List<TsChartRangeItem> rangeItems, boolean fillArea, float fillAreaOpacity) {
        Color outOfRangePaint = safeParseCssColor(outOfRangeColor);
        this.outOfRangePaint = outOfRangePaint;
        this.outOfRangeFillPaint = createFillPaint(fillArea, fillAreaOpacity, outOfRangePaint);
        this.pieces = rangeItems.stream().map(TsChartRangeItem::getPiece).toList();
        this.pieces.forEach(p -> p.setupPaints(fillArea, fillAreaOpacity));
    }

    public boolean isEmpty() {
        return pieces.isEmpty();
    }

    public List<TbVisualMapArea> calculateAreas(ValueAxis rangeAxis, Rectangle2D dataArea) {
        List<TbVisualMapArea> areas = new ArrayList<>();
        Range range = rangeAxis.getRange();
        double lower = range.getLowerBound();
        double upper = range.getUpperBound();
        double currentLower = lower;
        while (currentLower < upper) {
            double finalCurrentLower = currentLower;
            double currentUpper;
            Paint paint;
            Paint fillPaint;
            Optional<TbVisualMapPiece> piece = pieces.stream().filter(p -> p.matchLower(finalCurrentLower)).findFirst();
            if (piece.isPresent()) {
                TbVisualMapPiece pieceData = piece.get();
                currentUpper = pieceData.getUpper(upper);
                paint = pieceData.getPaint();
                fillPaint = pieceData.getFillPaint();
            } else {
                paint = this.outOfRangePaint;
                fillPaint = this.outOfRangeFillPaint;
                Optional<TbVisualMapPiece> nextPiece = pieces.stream().filter(p -> p.greaterLower(finalCurrentLower)).findFirst();
                if (nextPiece.isPresent()) {
                    TbVisualMapPiece pieceData = nextPiece.get();
                    currentUpper = pieceData.getNearestUpper(upper);
                } else {
                    currentUpper = upper;
                }
            }
            double bottomY = rangeAxis.valueToJava2D(currentLower, dataArea, RectangleEdge.LEFT);
            double topY = rangeAxis.valueToJava2D(currentUpper, dataArea, RectangleEdge.LEFT);
            Rectangle2D area = new Rectangle2D.Double(dataArea.getX(), topY, dataArea.getWidth(), bottomY - topY);
            TbVisualMapArea mapArea = new TbVisualMapArea(paint, fillPaint, area);
            areas.add(mapArea);
            currentLower = currentUpper;
        }
        return areas;
    }

    public Paint lookupPaint(double value) {
        Optional<TbVisualMapPiece> piece = pieces.stream().filter(p -> p.matchValue(value)).findFirst();
        if (piece.isPresent()) {
            TbVisualMapPiece pieceData = piece.get();
            return pieceData.getPaint();
        } else {
            return outOfRangePaint;
        }
    }
}
