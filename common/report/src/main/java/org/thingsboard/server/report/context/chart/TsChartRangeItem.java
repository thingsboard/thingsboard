// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.report.context.chart;

import lombok.Data;
import org.thingsboard.server.common.data.report.configuration.chart.ColorRange;
import org.thingsboard.server.report.renderer.chart.TbVisualMapPiece;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
public class TsChartRangeItem {

    private int index;
    private Double from;
    private Double to;
    private String color;
    private String label;
    private boolean visible;
    private TbVisualMapPiece piece;

    public static List<TsChartRangeItem> toRangeItems(List<ColorRange> colorRanges, NumberFormat valueFormat) {
        List<TsChartRangeItem> rangeItems = new ArrayList<>();
        int counter = 0;

        List<ColorRange> ranges =
                ColorRange.filterIncludingColorRanges(colorRanges).stream().sorted(ColorRange.COLOR_RANGE_COMPARATOR).filter(r -> isNumber(r.getFrom()) || isNumber(r.getTo())).toList();

        for (int i = 0; i < ranges.size(); i++) {
            ColorRange range = ranges.get(i);
            Double from = range.getFrom();
            Double to = range.getTo();
            if (i > 0) {
                ColorRange prevRange = ranges.get(i - 1);
                if (isNumber(prevRange.getTo()) && isNumber(from) && from < prevRange.getTo()) {
                    from = prevRange.getTo();
                }
            }
            Double formatToValue = to != null ? Double.valueOf(valueFormat.format(to)) : null;
            Double formatFromValue = from != null ? Double.valueOf(valueFormat.format(from)) : null;

            TsChartRangeItem rangeItem = new TsChartRangeItem();
            rangeItem.setIndex(counter++);
            rangeItem.setColor(range.getColor());
            rangeItem.setVisible(true);
            rangeItem.setFrom(from);
            rangeItem.setTo(to);
            rangeItem.setLabel(rangeItemLabel(formatFromValue, formatToValue, valueFormat));
            rangeItem.setPiece(TbVisualMapPiece.fromRange(range.getColor(), formatFromValue, formatToValue));

            rangeItems.add(rangeItem);
        }

        return rangeItems;
    }

    public static List<Double> toMarkPoints(List<TsChartRangeItem> ranges) {
        Set<Double> points = new HashSet<>();
        for (TsChartRangeItem range : ranges) {
            if (range.isVisible()) {
                if (isNumber(range.getFrom())) {
                    points.add(range.getFrom());
                }
                if (isNumber(range.getTo())) {
                    points.add(range.getTo());
                }
            }
        }
        return points.stream().sorted(Double::compareTo).toList();
    }

    private static boolean isNumber(Double number) {
        return number != null && Double.isFinite(number);
    }

    private static String rangeItemLabel(Double from, Double to, NumberFormat valueFormat) {
        if (isNumber(from) && isNumber(to)) {
            if (from.compareTo(to) == 0) {
                return valueFormat.format(from);
            } else {
                return valueFormat.format(from) + " - " + valueFormat.format(to);
            }
        } else if (isNumber(from)) {
            return "≥ " + valueFormat.format(from);
        } else if (isNumber(to)) {
            return "< " + valueFormat.format(to);
        } else {
            return null;
        }
    }

}
