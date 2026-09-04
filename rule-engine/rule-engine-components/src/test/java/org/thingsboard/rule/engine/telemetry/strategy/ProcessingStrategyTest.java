// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.rule.engine.telemetry.strategy;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class ProcessingStrategyTest {

    @Test
    void testOnEveryMessageReturnsCorrectInstance() {
        ProcessingStrategy strategy = ProcessingStrategy.onEveryMessage();
        assertThat(strategy)
                .isNotNull()
                .isInstanceOf(OnEveryMessageProcessingStrategy.class);
    }

    @Test
    void testDeduplicateReturnsCorrectInstance() {
        int validDeduplicationIntervalSecs = 5;
        ProcessingStrategy strategy = ProcessingStrategy.deduplicate(validDeduplicationIntervalSecs);
        assertThat(strategy)
                .isNotNull()
                .isInstanceOf(DeduplicateProcessingStrategy.class);

        long actualDeduplicationIntervalMillis = (long) ReflectionTestUtils.getField(strategy, "deduplicationIntervalMillis");
        assertThat(actualDeduplicationIntervalMillis).isEqualTo(Duration.ofSeconds(validDeduplicationIntervalSecs).toMillis());
    }

    @Test
    void testSkipReturnsCorrectInstance() {
        ProcessingStrategy strategy = ProcessingStrategy.skip();
        assertThat(strategy)
                .isNotNull()
                .isInstanceOf(SkipProcessingStrategy.class);
    }

}
