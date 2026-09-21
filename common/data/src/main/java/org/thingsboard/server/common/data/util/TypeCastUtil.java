// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.util;

import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.thingsboard.server.common.data.kv.DataType;

import java.math.BigDecimal;

public class TypeCastUtil {

    private TypeCastUtil() {}

    public static Pair<DataType, Object> castValue(String value) {
        if (isNumber(value)) {
            String formattedValue = value.replace(',', '.');
            try {
                BigDecimal bd = new BigDecimal(formattedValue);
                if (bd.stripTrailingZeros().scale() > 0 || isSimpleDouble(formattedValue)) {
                    if (bd.scale() <= 16) {
                        return Pair.of(DataType.DOUBLE, bd.doubleValue());
                    }
                } else {
                    return Pair.of(DataType.LONG, bd.longValueExact());
                }
            } catch (RuntimeException ignored) {}
        } else if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
            return Pair.of(DataType.BOOLEAN, Boolean.parseBoolean(value));
        }
        return Pair.of(DataType.STRING, value);
    }

    public static Pair<DataType, Number> castToNumber(String value) {
        if (isNumber(value)) {
            String formattedValue = value.replace(',', '.');
            BigDecimal bd = new BigDecimal(formattedValue);
            if (bd.stripTrailingZeros().scale() > 0 || isSimpleDouble(formattedValue)) {
                if (bd.scale() <= 16) {
                    return Pair.of(DataType.DOUBLE, bd.doubleValue());
                } else {
                    return Pair.of(DataType.DOUBLE, bd);
                }
            } else {
                return Pair.of(DataType.LONG, bd.longValueExact());
            }
        } else {
            throw new IllegalArgumentException("'" + value + "' can't be parsed as number");
        }
    }

    private static boolean isNumber(String value) {
        return NumberUtils.isCreatable(value.replace(',', '.'));
    }

    private static boolean isSimpleDouble(String valueAsString) {
        return valueAsString.contains(".") && !valueAsString.contains("E") && !valueAsString.contains("e");
    }

}
