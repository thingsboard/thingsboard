/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
package org.thingsboard.server.utils;

import org.apache.commons.lang3.math.NumberUtils;
import org.thingsboard.server.common.data.kv.DataType;

import java.math.BigDecimal;
import java.util.Map;

public class TypeCastUtil {

    private TypeCastUtil() {}

    public static Map.Entry<DataType, Object> castValue(String value) {
        if (isNumber(value)) {
            String formattedValue = value.replace(',', '.');
            try {
                BigDecimal bd = new BigDecimal(formattedValue);
                if (bd.stripTrailingZeros().scale() > 0 || isSimpleDouble(formattedValue)) {
                    if (bd.scale() <= 16) {
                        return Map.entry(DataType.DOUBLE, bd.doubleValue());
                    }
                } else {
                    return Map.entry(DataType.LONG, bd.longValueExact());
                }
            } catch (RuntimeException ignored) {}
        } else if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
            return Map.entry(DataType.BOOLEAN, Boolean.parseBoolean(value));
        }
        return Map.entry(DataType.STRING, value);
    }

    private static boolean isNumber(String value) {
        return NumberUtils.isNumber(value.replace(',', '.'));
    }

    private static boolean isSimpleDouble(String valueAsString) {
        return valueAsString.contains(".") && !valueAsString.contains("E") && !valueAsString.contains("e");
    }

}
