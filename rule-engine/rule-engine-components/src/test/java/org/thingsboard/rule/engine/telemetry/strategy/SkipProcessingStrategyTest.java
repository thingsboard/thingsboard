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
package org.thingsboard.rule.engine.telemetry.strategy;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class SkipProcessingStrategyTest {

    @ParameterizedTest
    @MethodSource("edgeCaseProvider")
    void shouldAlwaysReturnFalseForAnyInput(long timestamp, UUID originator) {
        var skipStrategy = SkipProcessingStrategy.getInstance();
        assertThat(skipStrategy.shouldProcess(timestamp, originator)).isFalse();
    }

    private static Stream<Arguments> edgeCaseProvider() {
        return Stream.of(
                Arguments.of(Long.MIN_VALUE, new UUID(0L, 0L)),
                Arguments.of(Long.MAX_VALUE, new UUID(Long.MAX_VALUE, Long.MAX_VALUE)),
                Arguments.of(0L, new UUID(0L, 0L)),
                Arguments.of(-1L, new UUID(-1L, -1L)),
                Arguments.of(1L, new UUID(1L, 1L)),
                Arguments.of(42L, UUID.randomUUID()),
                Arguments.of(1000L, null)
        );
    }

}
