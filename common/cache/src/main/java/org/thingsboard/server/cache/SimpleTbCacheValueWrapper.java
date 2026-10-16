// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.cache;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.springframework.cache.Cache;

@ToString
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class SimpleTbCacheValueWrapper<T> implements TbCacheValueWrapper<T> {

    private final T value;

    @Override
    public T get() {
        return value;
    }

    public static <T> SimpleTbCacheValueWrapper<T> empty() {
        return new SimpleTbCacheValueWrapper<>(null);
    }

    public static <T> SimpleTbCacheValueWrapper<T> wrap(T value) {
        return new SimpleTbCacheValueWrapper<>(value);
    }

    @SuppressWarnings("unchecked")
    public static <T> SimpleTbCacheValueWrapper<T> wrap(Cache.ValueWrapper source) {
        return source == null ? null : new SimpleTbCacheValueWrapper<>((T) source.get());
    }
}
