// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.timeseries;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.concurrent.CompletableFuture;

public class CassandraTsPartitionsCache {

    private AsyncLoadingCache<CassandraPartitionCacheKey, Boolean> partitionsCache;

    public CassandraTsPartitionsCache(long maxCacheSize) {
        this.partitionsCache = Caffeine.newBuilder()
                .maximumSize(maxCacheSize)
                .buildAsync(key -> {
                    throw new IllegalStateException("'get' methods calls are not supported!");
                });
    }

    public boolean has(CassandraPartitionCacheKey key) {
        return partitionsCache.getIfPresent(key) != null;
    }

    public void put(CassandraPartitionCacheKey key) {
        partitionsCache.put(key, CompletableFuture.completedFuture(true));
    }

    public void invalidate(CassandraPartitionCacheKey key) {
        partitionsCache.synchronous().invalidate(key);
    }
}
