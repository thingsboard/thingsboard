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

    public static Object roundResult(double value, Integer precision) {
        if (precision == null) {
            return value;
        }
        if (precision.equals(0)) {
            return toInt(value);
        }
        return toFixed(value, precision);
    }

}
