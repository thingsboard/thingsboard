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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.common.collect.Sets;
import com.google.common.primitives.Longs;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;

final class DeduplicateProcessingStrategy implements ProcessingStrategy {

    private static final int MIN_DEDUPLICATION_INTERVAL_SECS = 1;
    private static final int MAX_DEDUPLICATION_INTERVAL_SECS = (int) Duration.ofDays(1L).toSeconds();

    private static final long MIN_INTERVAL_EXPIRY_MILLIS = Duration.ofMinutes(10L).toMillis();
    private static final int INTERVAL_EXPIRY_FACTOR = 10;
    private static final long MAX_INTERVAL_EXPIRY_MILLIS = Duration.ofDays(2L).toMillis();

    private static final int MAX_TOTAL_INTERVALS_DURATION_SECS = (int) Duration.ofDays(2L).toSeconds();
    private static final int MAX_NUMBER_OF_INTERVALS = 100;

    private final long deduplicationIntervalMillis;
    private final LoadingCache<Long, Set<UUID>> deduplicationCache;

    @JsonCreator
    public DeduplicateProcessingStrategy(@JsonProperty("deduplicationIntervalSecs") int deduplicationIntervalSecs) {
        if (deduplicationIntervalSecs < MIN_DEDUPLICATION_INTERVAL_SECS || deduplicationIntervalSecs > MAX_DEDUPLICATION_INTERVAL_SECS) {
            throw new IllegalArgumentException("Deduplication interval must be at least " + MIN_DEDUPLICATION_INTERVAL_SECS + " second(s) " +
                    "and at most " + MAX_DEDUPLICATION_INTERVAL_SECS + " second(s), was " + deduplicationIntervalSecs + " second(s)");
        }
        deduplicationIntervalMillis = Duration.ofSeconds(deduplicationIntervalSecs).toMillis();
        deduplicationCache = Caffeine.newBuilder()
                .softValues()
                .expireAfterAccess(calculateExpireAfterAccess(deduplicationIntervalSecs))
                .maximumSize(calculateMaxNumberOfDeduplicationIntervals(deduplicationIntervalSecs))
                .build(__ -> Sets.newConcurrentHashSet());
    }

    /**
     * Calculates the expire-after-access duration. By default, we keep each deduplication interval
     * alive for 10 “iterations” (interval duration × 10). However, we never let this drop below
     * 10 minutes to ensure adequate retention for small intervals, nor exceed 48 hours to prevent
     * storing stale data in memory.
     */
    private static Duration calculateExpireAfterAccess(int deduplicationIntervalSecs) {
        long desiredExpiryMillis = Duration.ofSeconds(deduplicationIntervalSecs).toMillis() * INTERVAL_EXPIRY_FACTOR;
        return Duration.ofMillis(Longs.constrainToRange(desiredExpiryMillis, MIN_INTERVAL_EXPIRY_MILLIS, MAX_INTERVAL_EXPIRY_MILLIS));
    }

    /**
     * Calculates the maximum number of deduplication intervals we will store in the cache.
     * We limit retention to two days to avoid stale data and cap it at 100 intervals to manage memory usage.
     */
    private static long calculateMaxNumberOfDeduplicationIntervals(int deduplicationIntervalSecs) {
        int numberOfDeduplicationIntervals = MAX_TOTAL_INTERVALS_DURATION_SECS / deduplicationIntervalSecs;
        return Math.min(numberOfDeduplicationIntervals, MAX_NUMBER_OF_INTERVALS);
    }

    @JsonProperty("deduplicationIntervalSecs")
    public long getDeduplicationIntervalSecs() {
        return Duration.ofMillis(deduplicationIntervalMillis).toSeconds();
    }

    @Override
    public boolean shouldProcess(long ts, UUID originatorUuid) {
        long intervalNumber = ts / deduplicationIntervalMillis;
        return deduplicationCache.get(intervalNumber).add(originatorUuid);
    }

}
