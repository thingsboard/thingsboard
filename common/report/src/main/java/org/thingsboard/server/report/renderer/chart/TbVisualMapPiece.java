// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.report.renderer.chart;

import lombok.Data;

import java.awt.Color;
import java.awt.Paint;

import static org.thingsboard.server.report.renderer.chart.ChartUtils.createFillPaint;
import static org.thingsboard.server.report.util.ColorUtils.safeParseCssColor;

@Data
public class TbVisualMapPiece {

    private Double lt;
    private Double gte;
    private Double value;
    private String color;

    private Paint paint;
    private Paint fillPaint;

    public static TbVisualMapPiece fromRange(String color, Double from, Double to) {
        TbVisualMapPiece piece = new TbVisualMapPiece();
        piece.color = color;
        if (isNumber(from) && isNumber(to)) {
            if (from.compareTo(to) == 0) {
                piece.value = from;
            } else {
                piece.gte = from;
                piece.lt = to;
            }
        } else if (isNumber(from)) {
            piece.gte = from;
        } else if (isNumber(to)) {
            piece.lt = to;
        }
        return piece;
    }

    public void setupPaints(boolean fillArea, float fillAreaOpacity) {
        Color paint = safeParseCssColor(color);
        this.paint = paint;
        this.fillPaint = createFillPaint(fillArea, fillAreaOpacity, paint);
    }

    public boolean matchValue(double value) {
        if (isNumber(gte) && isNumber(lt)) {
            return value >= gte && value < lt;
        } else if (isNumber(gte)) {
            return value >= gte;
        } else if (isNumber(lt)) {
            return value < lt;
        } else if (isNumber(this.value)) {
            return this.value == value;
        } else {
            return false;
        }
    }

    public boolean matchLower(double lower) {
        if (isNumber(gte) && isNumber(lt)) {
            return lower >= gte && lower < lt;
        } else if (isNumber(gte)) {
            return lower >= gte;
        } else if (isNumber(lt)) {
            return lower < lt;
        } else {
            return false;
        }
    }

    public boolean greaterLower(double lower) {
        if (isNumber(gte)) {
            return gte > lower;
        }
        if (isNumber(lt)) {
            return lower < lt;
        }
        return false;
    }

    public double getUpper(double maxUpper) {
        if (isNumber(lt)) {
            return Math.min(maxUpper, lt);
        } else {
            return maxUpper;
        }
    }

    public double getNearestUpper(double maxUpper) {
        if (isNumber(gte)) {
            return Math.min(maxUpper, gte);
        }
        if (isNumber(lt)) {
            return Math.min(maxUpper, lt);
        } else {
            return maxUpper;
        }
    }

    private static boolean isNumber(Double number) {
        return number != null && Double.isFinite(number);
    }

}
