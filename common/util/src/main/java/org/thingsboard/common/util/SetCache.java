// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.common.util;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.concurrent.TimeUnit;

public class SetCache<K> {

    private static final Object DUMMY_VALUE = Boolean.TRUE;

    private final Cache<K, Object> cache;

    public SetCache(long valueTtlMs) {
        this(Caffeine.newBuilder().expireAfterWrite(valueTtlMs, TimeUnit.MILLISECONDS));
    }

    private SetCache(Caffeine<Object, Object> builder) {
        this.cache = builder.build();
    }

    /**
     * Bounded by entry count with LRU-ish eviction, instead of a TTL - for callers that want to remember
     * up to N recently seen keys rather than expire entries after a fixed duration.
     */
    public static <T> SetCache<T> boundedBySize(int maximumSize) {
        return new SetCache<>(Caffeine.newBuilder().maximumSize(maximumSize));
    }

    public void add(K key) {
        cache.put(key, DUMMY_VALUE);
    }

    public boolean contains(K key) {
        return cache.asMap().containsKey(key);
    }

    public void remove(K key) {
        cache.invalidate(key);
    }

}
