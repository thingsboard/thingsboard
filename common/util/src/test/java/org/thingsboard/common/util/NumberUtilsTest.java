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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class NumberUtilsTest {

    private final Float floatVal = 29.29824f;
    private final double doubleVal = 1729.1729;

    @Test
    public void isNaN() {
        assertThat(NumberUtils.isNaN(doubleVal)).isFalse();
        assertThat(NumberUtils.isNaN(Double.NaN)).isTrue();
    }

    @Test
    public void toFixedFloat() {
        float actualF = NumberUtils.toFixed(floatVal, 3);
        assertThat(Float.compare(floatVal, actualF)).isEqualTo(1);
        assertThat(Float.compare(29.298f, actualF)).isEqualTo(0);
    }

    @Test
    public void toFixedDouble() {
        double actualD = NumberUtils.toFixed(doubleVal, 3);
        assertThat(Double.compare(doubleVal, actualD)).isEqualTo(-1);
        assertThat(Double.compare(1729.173, actualD)).isEqualTo(0);
    }

    @Test
    public void toInt() {
        assertThat(NumberUtils.toInt(doubleVal)).isEqualTo(1729);
        assertThat(NumberUtils.toInt(12.8)).isEqualTo(13);
        assertThat(NumberUtils.toInt(28.0)).isEqualTo(28);
    }

    @Test
    public void roundResult() {
        assertThat(NumberUtils.roundResult(doubleVal, null)).isEqualTo(1729.1729);
        assertThat(NumberUtils.roundResult(doubleVal, 0)).isEqualTo(1729);
        assertThat(NumberUtils.roundResult(doubleVal, 2)).isEqualTo(1729.17);
    }

}
