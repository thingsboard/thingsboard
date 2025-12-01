/**
 * Copyright © 2016-2025 The Thingsboard Authors
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

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.kv.DataType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TypeCastUtilTest {

    // 1. Standard valid input case – integer string parsed as LONG
    @Test
    void castValue_withValidIntegerString_returnsLong() {
        Pair<DataType, Object> result = TypeCastUtil.castValue("42");

        assertThat(result.getLeft()).isEqualTo(DataType.LONG);
        assertThat(result.getRight()).isEqualTo(42L);
    }

    // 2. Null input case – castValue should throw NullPointerException
    @Test
    void castValue_withNullInput_throwsNullPointerException() {
        assertThatThrownBy(() -> TypeCastUtil.castValue(null))
                .isInstanceOf(NullPointerException.class);
    }

    // 3. Empty string / invalid input case – non-numeric string returns STRING type
    @Test
    void castValue_withEmptyString_returnsString() {
        Pair<DataType, Object> result = TypeCastUtil.castValue("");

        assertThat(result.getLeft()).isEqualTo(DataType.STRING);
        assertThat(result.getRight()).isEqualTo("");
    }

    // 4. Edge case – comma as decimal separator is converted and parsed as DOUBLE
    @Test
    void castValue_withCommaDecimalSeparator_returnsDouble() {
        Pair<DataType, Object> result = TypeCastUtil.castValue("3,14");

        assertThat(result.getLeft()).isEqualTo(DataType.DOUBLE);
        assertThat((Double) result.getRight()).isEqualTo(3.14);
    }

}

