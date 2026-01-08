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

import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.benmanes.caffeine.cache.Policy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DeduplicateProcessingStrategyTest {

    final int deduplicationIntervalSecs = 10;

    DeduplicateProcessingStrategy strategy;

    @BeforeEach
    void setup() {
        strategy = new DeduplicateProcessingStrategy(deduplicationIntervalSecs);
    }

    @Test
    void shouldThrowWhenDeduplicationIntervalIsLessThanOneSecond() {
        assertThatThrownBy(() -> new DeduplicateProcessingStrategy(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Deduplication interval must be at least 1 second(s) and at most 86400 second(s), was 0 second(s)");
    }

    @Test
    void shouldThrowWhenDeduplicationIntervalIsMoreThan24Hours() {
        assertThatThrownBy(() -> new DeduplicateProcessingStrategy(86401))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Deduplication interval must be at least 1 second(s) and at most 86400 second(s), was 86401 second(s)");
    }

    @Test
    void shouldUseAtLeastTenMinutesForExpireAfterAccess() {
        // GIVEN
        int deduplicationIntervalSecs = 1; // min deduplication interval duration

        // WHEN
        strategy = new DeduplicateProcessingStrategy(deduplicationIntervalSecs);

        // THEN
        var deduplicationCache = (LoadingCache<Long, Set<UUID>>) ReflectionTestUtils.getField(strategy, "deduplicationCache");

        assertThat(deduplicationCache.policy().expireAfterAccess())
                .isPresent()
                .map(Policy.FixedExpiration::getExpiresAfter)
                .hasValue(Duration.ofMinutes(10L));
    }

    @Test
    void shouldCalculateExpireAfterAccessAsIntervalDurationMultipliedByTen() {
        // GIVEN
        int deduplicationIntervalSecs = (int) Duration.ofHours(1L).toSeconds(); // max deduplication interval duration

        // WHEN
        strategy = new DeduplicateProcessingStrategy(deduplicationIntervalSecs);

        // THEN
        var deduplicationCache = (LoadingCache<Long, Set<UUID>>) ReflectionTestUtils.getField(strategy, "deduplicationCache");

        assertThat(deduplicationCache.policy().expireAfterAccess())
                .isPresent()
                .map(Policy.FixedExpiration::getExpiresAfter)
                .hasValue(Duration.ofHours(10L));
    }

    @Test
    void shouldUseAtMostTwoDaysForExpireAfterAccess() {
        // GIVEN
        int deduplicationIntervalSecs = (int) Duration.ofDays(1L).toSeconds(); // max deduplication interval duration

        // WHEN
        strategy = new DeduplicateProcessingStrategy(deduplicationIntervalSecs);

        // THEN
        var deduplicationCache = (LoadingCache<Long, Set<UUID>>) ReflectionTestUtils.getField(strategy, "deduplicationCache");

        assertThat(deduplicationCache.policy().expireAfterAccess())
                .isPresent()
                .map(Policy.FixedExpiration::getExpiresAfter)
                .hasValue(Duration.ofDays(2L));
    }

    @Test
    void shouldNotAllowMoreThan100DeduplicationIntervals() {
        // GIVEN
        int deduplicationIntervalSecs = 1; // min deduplication interval duration

        // WHEN
        strategy = new DeduplicateProcessingStrategy(deduplicationIntervalSecs);

        // THEN
        var deduplicationCache = (LoadingCache<Long, Set<UUID>>) ReflectionTestUtils.getField(strategy, "deduplicationCache");

        assertThat(deduplicationCache.policy().eviction())
                .isPresent()
                .map(Policy.Eviction::getMaximum)
                .hasValue(100L);
    }

    @Test
    void shouldCalculateMaxIntervalsAsTwoDaysDividedByIntervalDuration() {
        // GIVEN
        int deduplicationIntervalSecs = (int) Duration.ofHours(1L).toSeconds();

        // WHEN
        strategy = new DeduplicateProcessingStrategy(deduplicationIntervalSecs);

        // THEN
        var deduplicationCache = (LoadingCache<Long, Set<UUID>>) ReflectionTestUtils.getField(strategy, "deduplicationCache");

        assertThat(deduplicationCache.policy().eviction())
                .isPresent()
                .map(Policy.Eviction::getMaximum)
                .hasValue(48L);
    }

    @Test
    void shouldKeepAtLeastTwoDeduplicationIntervals() {
        // GIVEN
        int deduplicationIntervalSecs = (int) Duration.ofDays(1L).toSeconds(); // max deduplication interval duration

        // WHEN
        strategy = new DeduplicateProcessingStrategy(deduplicationIntervalSecs);

        // THEN
        var deduplicationCache = (LoadingCache<Long, Set<UUID>>) ReflectionTestUtils.getField(strategy, "deduplicationCache");

        assertThat(deduplicationCache.policy().eviction())
                .isPresent()
                .map(Policy.Eviction::getMaximum)
                .hasValue(2L);
    }

    @Test
    void shouldReturnTrueForFirstCallInInterval() {
        long ts = 1_000_000L;
        UUID originator = UUID.randomUUID();

        assertThat(strategy.shouldProcess(ts, originator)).isTrue();
    }

    @Test
    void shouldReturnFalseForSubsequentCallsInInterval() {
        long baseTs = 1_000_000L;
        UUID originator = UUID.randomUUID();

        // Initial call should return true
        assertThat(strategy.shouldProcess(baseTs, originator)).isTrue();

        // Subsequent call within the same interval should return false for the same originator
        long withinSameIntervalTs = baseTs + 1000L;
        assertThat(strategy.shouldProcess(withinSameIntervalTs, originator)).isFalse();
    }

    @Test
    void shouldHandleMultipleOriginatorsIndependently() {
        long baseTs = 1_000_000L;
        UUID originator1 = UUID.randomUUID();
        UUID originator2 = UUID.randomUUID();

        // First call for different originators in the same interval should return true independently
        assertThat(strategy.shouldProcess(baseTs, originator1)).isTrue();
        assertThat(strategy.shouldProcess(baseTs, originator2)).isTrue();

        // Subsequent calls for the same originators within the same interval should return false
        assertThat(strategy.shouldProcess(baseTs + 500L, originator1)).isFalse();
        assertThat(strategy.shouldProcess(baseTs + 500L, originator2)).isFalse();
    }

    @Test
    void shouldHandleEdgeCaseTimestamps() {
        long minTs = Long.MIN_VALUE;
        long maxTs = Long.MAX_VALUE;
        UUID originator = UUID.randomUUID();

        assertThat(strategy.shouldProcess(minTs, originator)).isTrue();
        assertThat(strategy.shouldProcess(minTs + 1L, originator)).isFalse();

        assertThat(strategy.shouldProcess(maxTs, originator)).isTrue();
        assertThat(strategy.shouldProcess(maxTs - 1L, originator)).isFalse();
    }

    @Test
    void shouldResetDeduplicationAtIntervalBoundaries() {
        UUID originator = UUID.randomUUID();

        // check 1st interval
        long firstIntervalStart = 0L;
        long firstIntervalEnd = firstIntervalStart + Duration.ofSeconds(deduplicationIntervalSecs).toMillis() - 1L;
        long firstIntervalMiddle = calculateMiddle(firstIntervalStart, firstIntervalEnd);

        assertThat(strategy.shouldProcess(firstIntervalStart, originator)).isTrue();
        assertThat(strategy.shouldProcess(firstIntervalStart + 1, originator)).isFalse();
        assertThat(strategy.shouldProcess(firstIntervalMiddle, originator)).isFalse();
        assertThat(strategy.shouldProcess(firstIntervalEnd - 1, originator)).isFalse();
        assertThat(strategy.shouldProcess(firstIntervalEnd, originator)).isFalse();

        // check 2nd interval
        long secondIntervalStart = firstIntervalEnd + 1L;
        long secondIntervalEnd = secondIntervalStart + Duration.ofSeconds(deduplicationIntervalSecs).toMillis() - 1L;
        long secondIntervalMiddle = calculateMiddle(secondIntervalStart, secondIntervalEnd);

        assertThat(strategy.shouldProcess(secondIntervalStart, originator)).isTrue();
        assertThat(strategy.shouldProcess(secondIntervalStart + 1, originator)).isFalse();
        assertThat(strategy.shouldProcess(secondIntervalMiddle, originator)).isFalse();
        assertThat(strategy.shouldProcess(secondIntervalEnd - 1, originator)).isFalse();
        assertThat(strategy.shouldProcess(secondIntervalEnd, originator)).isFalse();
    }

    @Test
    void shouldHandleMultipleOriginatorsOverMultipleIntervals() {
        UUID originator1 = UUID.randomUUID();
        UUID originator2 = UUID.randomUUID();
        long baseTs = 0L;

        // First interval for both originators
        assertThat(strategy.shouldProcess(baseTs, originator1)).isTrue();
        assertThat(strategy.shouldProcess(baseTs, originator2)).isTrue();

        // Move to the next interval
        long nextIntervalTs = baseTs + Duration.ofSeconds(10).toMillis();

        // Each originator should be allowed again in the new interval
        assertThat(strategy.shouldProcess(nextIntervalTs, originator1)).isTrue();
        assertThat(strategy.shouldProcess(nextIntervalTs, originator2)).isTrue();

        // Subsequent calls in the same new interval should return false
        assertThat(strategy.shouldProcess(nextIntervalTs + 500L, originator1)).isFalse();
        assertThat(strategy.shouldProcess(nextIntervalTs + 500L, originator2)).isFalse();
    }

    private static long calculateMiddle(long start, long end) {
        return start + (end - start) / 2;
    }

}
