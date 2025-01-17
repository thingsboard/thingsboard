/**
 * Copyright © 2016-2024 The Thingsboard Authors
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DeduplicatePersistenceStrategyTest {

    final int deduplicationIntervalSecs = 10;

    DeduplicatePersistenceStrategy strategy;

    @BeforeEach
    void setup() {
        strategy = new DeduplicatePersistenceStrategy(deduplicationIntervalSecs);
    }

    @Test
    void shouldThrowWhenDeduplicationIntervalIsLessThanOneSecond() {
        assertThatThrownBy(() -> new DeduplicatePersistenceStrategy(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Deduplication interval must be at least 1 second(s), was 0 second(s)");
    }

    @Test
    void shouldReturnTrueForFirstCallInInterval() {
        long ts = 1_000_000L;
        UUID originator = UUID.randomUUID();

        assertThat(strategy.shouldPersist(ts, originator)).isTrue();
    }

    @Test
    void shouldReturnFalseForSubsequentCallsInInterval() {
        long baseTs = 1_000_000L;
        UUID originator = UUID.randomUUID();

        // Initial call should return true
        assertThat(strategy.shouldPersist(baseTs, originator)).isTrue();

        // Subsequent call within the same interval should return false for the same originator
        long withinSameIntervalTs = baseTs + 1000L;
        assertThat(strategy.shouldPersist(withinSameIntervalTs, originator)).isFalse();
    }

    @Test
    void shouldHandleMultipleOriginatorsIndependently() {
        long baseTs = 1_000_000L;
        UUID originator1 = UUID.randomUUID();
        UUID originator2 = UUID.randomUUID();

        // First call for different originators in the same interval should return true independently
        assertThat(strategy.shouldPersist(baseTs, originator1)).isTrue();
        assertThat(strategy.shouldPersist(baseTs, originator2)).isTrue();

        // Subsequent calls for the same originators within the same interval should return false
        assertThat(strategy.shouldPersist(baseTs + 500L, originator1)).isFalse();
        assertThat(strategy.shouldPersist(baseTs + 500L, originator2)).isFalse();
    }

    @Test
    void shouldHandleEdgeCaseTimestamps() {
        long minTs = Long.MIN_VALUE;
        long maxTs = Long.MAX_VALUE;
        UUID originator = UUID.randomUUID();

        assertThat(strategy.shouldPersist(minTs, originator)).isTrue();
        assertThat(strategy.shouldPersist(minTs + 1L, originator)).isFalse();

        assertThat(strategy.shouldPersist(maxTs, originator)).isTrue();
        assertThat(strategy.shouldPersist(maxTs - 1L, originator)).isFalse();
    }

    @Test
    void shouldResetDeduplicationAtIntervalBoundaries() {
        UUID originator = UUID.randomUUID();

        // check 1st interval
        long firstIntervalStart = 0L;
        long firstIntervalEnd = firstIntervalStart + Duration.ofSeconds(deduplicationIntervalSecs).toMillis() - 1L;
        long firstIntervalMiddle = calculateMiddle(firstIntervalStart, firstIntervalEnd);

        assertThat(strategy.shouldPersist(firstIntervalStart, originator)).isTrue();
        assertThat(strategy.shouldPersist(firstIntervalStart + 1, originator)).isFalse();
        assertThat(strategy.shouldPersist(firstIntervalMiddle, originator)).isFalse();
        assertThat(strategy.shouldPersist(firstIntervalEnd - 1, originator)).isFalse();
        assertThat(strategy.shouldPersist(firstIntervalEnd, originator)).isFalse();

        // check 2nd interval
        long secondIntervalStart = firstIntervalEnd + 1L;
        long secondIntervalEnd = secondIntervalStart + Duration.ofSeconds(deduplicationIntervalSecs).toMillis() - 1L;
        long secondIntervalMiddle = calculateMiddle(secondIntervalStart, secondIntervalEnd);

        assertThat(strategy.shouldPersist(secondIntervalStart, originator)).isTrue();
        assertThat(strategy.shouldPersist(secondIntervalStart + 1, originator)).isFalse();
        assertThat(strategy.shouldPersist(secondIntervalMiddle, originator)).isFalse();
        assertThat(strategy.shouldPersist(secondIntervalEnd - 1, originator)).isFalse();
        assertThat(strategy.shouldPersist(secondIntervalEnd, originator)).isFalse();
    }

    @Test
    void shouldHandleMultipleOriginatorsOverMultipleIntervals() {
        UUID originator1 = UUID.randomUUID();
        UUID originator2 = UUID.randomUUID();
        long baseTs = 0L;

        // First interval for both originators
        assertThat(strategy.shouldPersist(baseTs, originator1)).isTrue();
        assertThat(strategy.shouldPersist(baseTs, originator2)).isTrue();

        // Move to the next interval
        long nextIntervalTs = baseTs + Duration.ofSeconds(10).toMillis();

        // Each originator should be allowed again in the new interval
        assertThat(strategy.shouldPersist(nextIntervalTs, originator1)).isTrue();
        assertThat(strategy.shouldPersist(nextIntervalTs, originator2)).isTrue();

        // Subsequent calls in the same new interval should return false
        assertThat(strategy.shouldPersist(nextIntervalTs + 500L, originator1)).isFalse();
        assertThat(strategy.shouldPersist(nextIntervalTs + 500L, originator2)).isFalse();
    }

    private static long calculateMiddle(long start, long end) {
        return start + (end - start) / 2;
    }

}
