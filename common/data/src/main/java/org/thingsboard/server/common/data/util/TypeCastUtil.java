/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.common.data.util;

import com.google.gson.JsonParser;
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
        } else if (looksLikeJson(value)) {
            try {
                return Pair.of(DataType.JSON, JsonParser.parseString(value));
            } catch (Exception ignored) {
            }
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
        return NumberUtils.isNumber(value.replace(',', '.'));
    }

    private static boolean isSimpleDouble(String valueAsString) {
        return valueAsString.contains(".") && !valueAsString.contains("E") && !valueAsString.contains("e");
    }

    private static boolean looksLikeJson(String value) {
        String trimmed = value.trim();
        return (trimmed.startsWith("{") && trimmed.endsWith("}")) ||
                (trimmed.startsWith("[") && trimmed.endsWith("]"));
    }

}
