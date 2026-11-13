// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.report.configuration.chart;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

@Data
public class ColorRange {

    private Double from;
    private Double to;
    private String color;

    @JsonIgnore
    public boolean includes(ColorRange other) {
        if (isNumber(from) && isNumber(to)) {
            if (isNumber(other.from) && isNumber(other.to)) {
                return other.from >= from && other.to < to;
            } else {
                return false;
            }
        } else if (isNumber(from)) {
            if (isNumber(other.from)) {
                return other.from >= from;
            } else {
                return false;
            }
        } else if (isNumber(to)) {
            if (isNumber(other.to)) {
                return other.to < to;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public static List<ColorRange> filterIncludingColorRanges(List<ColorRange> colorRanges) {
        List<ColorRange> result = new ArrayList<>(colorRanges);
        boolean includes = true;
        while (includes) {
            int index = -1;
            for (int i = 0; i < result.size(); i++) {
                ColorRange range = result.get(i);
                int currentIndex = i;
                if (IntStream.range(0, result.size())
                        .anyMatch(i1 -> {
                            ColorRange value = result.get(i1);
                            return i1 != currentIndex && value.includes(range);
                        })) {
                    index = i;
                    break;
                }
            }
            if (index > -1) {
                result.remove(index);
            } else {
                includes = false;
            }
        }
        return result;
    }

    public static final Comparator<ColorRange> COLOR_RANGE_COMPARATOR = (a, b) -> {
        if (isNumber(a.from) && isNumber(a.to) && isNumber(b.from) && isNumber(b.to)) {
            if (b.from >= a.from && b.to < a.to) {
                return 1;
            } else if (a.from >= b.from && a.to < b.to) {
                return -1;
            } else {
                return (int)(a.from - b.from);
            }
        } else if (isNumber(a.from) && isNumber(b.from)) {
            return (int)(a.from - b.from);
        } else if (isNumber(a.to) && isNumber(b.to)) {
            return (int)(a.to - b.to);
        } else if (isNumber(a.from) && !isNumber(b.from)) {
            return 1;
        } else if (!isNumber(a.from) && isNumber(b.from)) {
            return -1;
        } else if (isNumber(a.to) && !isNumber(b.to)) {
            return 1;
        } else if (!isNumber(a.to) && isNumber(b.to)) {
            return -1;
        } else {
            return 0;
        }
    };

    private static boolean isNumber(Double number) {
        return number != null && Double.isFinite(number);
    }

}
