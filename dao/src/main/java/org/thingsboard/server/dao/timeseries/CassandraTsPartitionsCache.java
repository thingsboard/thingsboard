/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.server.dao.timeseries;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class CassandraTsPartitionsCache {

    private final AsyncLoadingCache<CassandraPartitionCacheKey, Boolean> partitionsCache;

    public CassandraTsPartitionsCache(long maxCacheSize, boolean partitionsCacheStats, long partitionsCacheStatsInterval,
                                      ScheduledExecutorService scheduler) {
        Caffeine<Object, Object> caffeineBuilder = Caffeine.newBuilder()
                .maximumSize(maxCacheSize);

        if (partitionsCacheStats) {
            caffeineBuilder.recordStats();
        }

        this.partitionsCache = caffeineBuilder
                .buildAsync(key -> {
                    throw new IllegalStateException("'get' methods calls are not supported!");
                });

        if (partitionsCacheStats) {
            scheduler.scheduleAtFixedRate(this::printCacheStats, partitionsCacheStatsInterval, partitionsCacheStatsInterval, TimeUnit.SECONDS);
        }
    }

    void printCacheStats() {
        CacheStats stats = this.partitionsCache.synchronous().stats();
        if (stats.hitCount() != 0 || stats.missCount() != 0) {
            log.info("ts partitions cache hit [{}] [{}]", stats.hitRate(), stats);
        }
    }

    public boolean has(CassandraPartitionCacheKey key) {
        return partitionsCache.getIfPresent(key) != null;
    }

    public void put(CassandraPartitionCacheKey key) {
        partitionsCache.put(key, CompletableFuture.completedFuture(true));
    }
}
