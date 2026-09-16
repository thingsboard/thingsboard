// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.common.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class NumberUtils {

    public static boolean isNaN(double value) {
        return Double.isNaN(value);
    }

    public static double toFixed(double value, int precision) {
        return BigDecimal.valueOf(value).setScale(precision, RoundingMode.HALF_UP).doubleValue();
    }

    public static float toFixed(float value, int precision) {
        return BigDecimal.valueOf(value).setScale(precision, RoundingMode.HALF_UP).floatValue();
    }

    public static int toInt(double value) {
        return BigDecimal.valueOf(value).setScale(0, RoundingMode.HALF_UP).intValue();
    }

    public static long toLong(double value) {
        return BigDecimal.valueOf(value).setScale(0, RoundingMode.HALF_UP).longValue();
    }

    public static Object roundResult(double value, Integer precision) {
        if (precision == null) {
            return value;
        }
        if (precision.equals(0)) {
            return toLong(value);
        }
        return toFixed(value, precision);
    }

}
