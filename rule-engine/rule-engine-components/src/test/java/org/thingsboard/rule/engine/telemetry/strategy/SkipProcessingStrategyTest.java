// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
