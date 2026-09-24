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
package org.thingsboard.server.transport.mqtt.util.sparkplug;

import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.adaptor.AdaptorException;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MetricDataTypeTest {

    @Test
    void checkType_noExceptionWhenValueMatchesExpectedType() {
        assertThatNoException().isThrownBy(() -> MetricDataType.String.checkType("hello"));
    }

    @Test
    void checkType_noExceptionWhenValueIsNull() {
        assertThatNoException().isThrownBy(() -> MetricDataType.String.checkType(null));
    }

    @Test
    void checkType_throwsAdaptorExceptionWithActualClassNameWhenTypeMismatch() {
        assertThatThrownBy(() -> MetricDataType.String.checkType(42))
                .isInstanceOf(AdaptorException.class)
                .hasMessageContaining(Integer.class.toString());
    }

}
