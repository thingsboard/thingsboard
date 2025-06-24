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
package org.thingsboard.server.service.sync.tenant.util;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Slf4j
public class ResultStore<K> {

    private final String name;
    private final int persistFrequency;

    private final Cache<UUID, Result<K>> results;
    private final org.springframework.cache.Cache resultsCache;

    @Builder
    public ResultStore(String name, int ttlInMinutes, int persistFrequency, BiConsumer<UUID, Result<K>> removalListener,
                       String cacheName, CacheManager cacheManager) {
        this.name = name;
        this.persistFrequency = persistFrequency;

        this.resultsCache = Optional.ofNullable(cacheManager.getCache(cacheName))
                .orElseThrow(() -> new IllegalArgumentException(cacheName + " cache is missing"));
        this.results = Caffeine.newBuilder()
                .expireAfterAccess(ttlInMinutes, TimeUnit.MINUTES)
                .<UUID, Result<K>>removalListener((key, result, removalCause) -> {
                    if (key != null) {
                        if (removalListener != null) {
                            removalListener.accept(key, result);
                        }
                        resultsCache.evict(key);
                    }
                })
                .build();
    }

    public void report(UUID id, K key) {
        Result<K> result = getOrCreateResult(id);
        int count = result.report(key);
        if (count % persistFrequency == 0) {
            resultsCache.put(id, result);
            print(id, key, count);
        }
    }

    public int get(UUID id, K key) {
        Result<K> result = getOrCreateResult(id);
        return result.getCount(key);
    }

    public void update(UUID id, Consumer<Result<K>> updater) {
        Result<K> result = getOrCreateResult(id);
        updater.accept(result);
        flush(id);
    }

    public void flush(UUID id, K... keys) {
        Result<K> result = getOrCreateResult(id);
        resultsCache.put(id, result);
        for (K key : keys) {
            int count = result.getCount(key);
            if (count % persistFrequency != 0) { // if we didn't print it before
                print(id, key, count);
            }
        }
    }

    private void print(UUID id, K key, int count) {
        log.info("[{}][{}][{}] {} processed", name, id, key, count);
    }

    private Result<K> getOrCreateResult(UUID id) {
        return results.get(id, k -> new Result<K>());
    }

    public Result<K> getStoredResult(UUID id) {
        return resultsCache.get(id, Result.class);
    }

}
