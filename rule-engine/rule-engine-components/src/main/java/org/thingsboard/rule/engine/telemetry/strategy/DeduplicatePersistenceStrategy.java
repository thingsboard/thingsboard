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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.common.collect.Sets;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;

final class DeduplicatePersistenceStrategy implements PersistenceStrategy {

    private static final int MIN_DEDUPLICATION_INTERVAL_SECS = 1;
    private static final int MAX_DEDUPLICATION_INTERVAL_SECS = (int) Duration.ofDays(1L).toSeconds();

    private final long deduplicationIntervalMillis;
    private final LoadingCache<Long, Set<UUID>> deduplicationCache;

    @JsonCreator
    public DeduplicatePersistenceStrategy(@JsonProperty("deduplicationIntervalSecs") int deduplicationIntervalSecs) {
        if (deduplicationIntervalSecs < MIN_DEDUPLICATION_INTERVAL_SECS || deduplicationIntervalSecs > MAX_DEDUPLICATION_INTERVAL_SECS) {
            throw new IllegalArgumentException("Deduplication interval must be at least " + MIN_DEDUPLICATION_INTERVAL_SECS + " second(s) " +
                    "and at most " + MAX_DEDUPLICATION_INTERVAL_SECS + " second(s), was " + deduplicationIntervalSecs + " second(s)");
        }
        deduplicationIntervalMillis = Duration.ofSeconds(deduplicationIntervalSecs).toMillis();
        deduplicationCache = Caffeine.newBuilder()
                .softValues()
                .expireAfterAccess(Duration.ofSeconds(deduplicationIntervalSecs * 10L))
                .maximumSize(20L)
                .build(__ -> Sets.newConcurrentHashSet());
    }

    @JsonProperty("deduplicationIntervalSecs")
    public long getDeduplicationIntervalSecs() {
        return Duration.ofMillis(deduplicationIntervalMillis).toSeconds();
    }

    @Override
    public boolean shouldPersist(long ts, UUID originatorUuid) {
        long intervalNumber = ts / deduplicationIntervalMillis;
        return deduplicationCache.get(intervalNumber).add(originatorUuid);
    }

}
