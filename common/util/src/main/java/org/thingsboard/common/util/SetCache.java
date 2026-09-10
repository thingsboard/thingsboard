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
        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(valueTtlMs, TimeUnit.MILLISECONDS)
                .build();
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
