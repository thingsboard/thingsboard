// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.report.renderer.chart;

import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.ui.LengthAdjustmentType;
import org.jfree.chart.ui.RectangleAnchor;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.chart.ui.TextAnchor;
import org.thingsboard.server.common.data.report.configuration.chart.ChartShape;
import org.thingsboard.server.common.data.report.configuration.chart.ThresholdLabelPosition;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;

import static org.thingsboard.server.report.renderer.chart.ChartUtils.createSeriesShape;

public class TbThresholdMarker extends ValueMarker {

    private static final RectangleInsets DEFAULT_LABEL_OFFSET = new RectangleInsets(5,5,5,5);
    private static final RectangleInsets BACKGROUND_LABEL_OFFSET = new RectangleInsets(7,8,7,8);

    private ChartShape startSymbol;
    private float startSymbolSize;
    private Shape startShape;

    private ChartShape endSymbol;
    private float endSymbolSize;
    private Shape endShape;

    private Paint startShapeFillPaint;
    private Stroke startShapeOutlineStroke;

    private Paint endShapeFillPaint;
    private Stroke endShapeOutlineStroke;

    private boolean drawLabelBackground;

    private ThresholdLabelPosition labelPosition;

    public TbThresholdMarker(double value) {
        super(value);
        setLabelOffset(DEFAULT_LABEL_OFFSET);
        setAlpha(1f);
        this.labelPosition = ThresholdLabelPosition.end;
    }

    public Shape getStartShape() {
        return startShape;
    }

    public Shape getEndShape() {
        return endShape;
    }

    public Paint getStartShapeFillPaint() {
        return startShapeFillPaint;
    }

    public Stroke getStartShapeOutlineStroke() {
        return startShapeOutlineStroke;
    }

    public Paint getEndShapeFillPaint() {
        return endShapeFillPaint;
    }

    public Stroke getEndShapeOutlineStroke() {
        return endShapeOutlineStroke;
    }

    public boolean isDrawLabelBackground() {
        return drawLabelBackground;
    }

    public void setDrawLabelBackground(boolean drawLabelBackground) {
        this.drawLabelBackground = drawLabelBackground;
        if (this.drawLabelBackground) {
            setLabelOffset(BACKGROUND_LABEL_OFFSET);
        } else {
            setLabelOffset(DEFAULT_LABEL_OFFSET);
        }
    }

    public void setStartSymbol(ChartShape startSymbol, float startSymbolSize) {
        this.startSymbol = startSymbol;
        this.startSymbolSize = startSymbolSize;
        this.startShape = createSeriesShape(startSymbol, startSymbolSize);
        if (this.startShape != null) {
            if (ChartShape.emptyCircle.equals(this.startSymbol)) {
                this.startShapeFillPaint = Color.WHITE;
                this.startShapeOutlineStroke = new BasicStroke(2.0f);
            }
        }
    }

    public void setEndSymbol(ChartShape endSymbol, float endSymbolSize) {
        this.endSymbol = endSymbol;
        this.endSymbolSize = endSymbolSize;
        this.endShape = createSeriesShape(endSymbol, endSymbolSize);
        if (this.endShape != null) {
            if (ChartShape.emptyCircle.equals(this.endSymbol)) {
                this.endShapeFillPaint = Color.WHITE;
                this.endShapeOutlineStroke = new BasicStroke(2.0f);
            }
        }
    }

    public void setLabelPosition(ThresholdLabelPosition labelPosition) {
        this.labelPosition = labelPosition;
    }

    public ThresholdLabelPosition getLabelPosition() {
        return labelPosition;
    }

    @Override
    public RectangleAnchor getLabelAnchor() {
        switch (labelPosition) {
            case start, insideStart -> {
                return RectangleAnchor.LEFT;
            }
            case middle, insideMiddleTop -> {
                return RectangleAnchor.TOP;
            }
            case end, insideEnd -> {
                return RectangleAnchor.RIGHT;
            }
            case insideStartTop -> {
                return RectangleAnchor.TOP_LEFT;
            }
            case insideStartBottom -> {
                return RectangleAnchor.BOTTOM_LEFT;
            }
            case insideMiddle -> {
                return RectangleAnchor.CENTER;
            }
            case insideMiddleBottom -> {
                return RectangleAnchor.BOTTOM;
            }
            case insideEndTop -> {
                return RectangleAnchor.TOP_RIGHT;
            }
            case insideEndBottom -> {
                return RectangleAnchor.BOTTOM_RIGHT;
            }
        }
        return RectangleAnchor.CENTER;
    }

    @Override
    public TextAnchor getLabelTextAnchor() {
        switch (labelPosition) {
            case start, insideEnd -> {
                return TextAnchor.CENTER_RIGHT;
            }
            case middle, insideMiddleTop -> {
                return TextAnchor.BOTTOM_CENTER;
            }
            case end, insideStart -> {
                return TextAnchor.CENTER_LEFT;
            }
            case insideStartTop -> {
                return TextAnchor.BOTTOM_LEFT;
            }
            case insideStartBottom -> {
                return TextAnchor.TOP_LEFT;
            }
            case insideMiddle -> {
                return TextAnchor.CENTER;
            }
            case insideMiddleBottom -> {
                return TextAnchor.TOP_CENTER;
            }
            case insideEndTop -> {
                return TextAnchor.BOTTOM_RIGHT;
            }
            case insideEndBottom -> {
                return TextAnchor.TOP_RIGHT;
            }
        }
        return TextAnchor.CENTER;
    }

    @Override
    public LengthAdjustmentType getLabelOffsetType() {
        switch (labelPosition) {
            case start, end -> {
                return LengthAdjustmentType.EXPAND;
            }
            case middle, insideStart, insideStartTop, insideStartBottom, insideMiddle, insideMiddleTop,
                 insideMiddleBottom, insideEnd, insideEndTop, insideEndBottom -> {
                return LengthAdjustmentType.CONTRACT;
            }
        }
        return LengthAdjustmentType.CONTRACT;
    }

    public double[] measureOffset(Graphics2D g2) {
        double[] offset = new double[]{0,0};
        if (this.startShape != null) {
            offset[0] = this.symbolOffset(startSymbol, startSymbolSize);
        }
        if (this.endShape != null) {
            offset[1] = this.symbolOffset(endSymbol, endSymbolSize);
        }
        if (this.labelPosition == ThresholdLabelPosition.start || this.labelPosition == ThresholdLabelPosition.end) {
            double width = measureLabelWidth(g2);
            int index = this.labelPosition == ThresholdLabelPosition.start ? 0 : 1;
            offset[index] = Math.max(width, offset[index]);
        }
        return offset;
    }

    private double measureLabelWidth(Graphics2D g2) {
        if (getLabel() != null) {
            FontMetrics metrics = g2.getFontMetrics(getLabelFont());
            double textWidth = metrics.stringWidth(getLabel());
            RectangleInsets labelOffset = getLabelOffset();
            textWidth += labelOffset.getLeft() + labelOffset.getRight();
            return textWidth;
        } else {
            return 0;
        }
    }

    private double symbolOffset(ChartShape symbol, float symbolSize) {
        switch (symbol) {
            case emptyCircle -> {
                return symbolSize / 2 + 1;
            }
            case circle, diamond, triangle, roundRect, rect -> {
                return symbolSize / 2;
            }
            case pin -> {
                return symbolSize;
            }
            case arrow, none -> {
                return 0;
            }
        }
        return symbolSize / 2;
    }
}
