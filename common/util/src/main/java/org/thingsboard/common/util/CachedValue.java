// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.common.util;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class CachedValue<V> {

    private final LoadingCache<Object, V> cache;

    public CachedValue(Supplier<V> supplier, long valueTtlMs) {
        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(valueTtlMs, TimeUnit.MILLISECONDS)
                .build(__ -> supplier.get());
    }

    public V get() {
        return cache.get(this);
    }

}
