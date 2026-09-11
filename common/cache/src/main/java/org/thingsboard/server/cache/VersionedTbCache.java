// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.cache;

import org.thingsboard.server.common.data.HasVersion;

import java.io.Serializable;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Supplier;

public interface VersionedTbCache<K extends VersionedCacheKey, V extends Serializable & HasVersion> extends TbTransactionalCache<K, V> {

    TbCacheValueWrapper<V> get(K key);

    default V get(K key, Supplier<V> supplier) {
        return get(key, supplier, true);
    }

    default V get(K key, Supplier<V> supplier, boolean putToCache) {
        return Optional.ofNullable(get(key))
                .map(TbCacheValueWrapper::get)
                .orElseGet(() -> {
                    V value = supplier.get();
                    if (putToCache) {
                        put(key, value);
                    }
                    return value;
                });
    }

    void put(K key, V value);

    void evict(K key);

    void evict(Collection<K> keys);

    void evict(K key, Long version);

    default Long getVersion(V value) {
        if (value == null) {
            return 0L;
        } else if (value.getVersion() != null) {
            return value.getVersion();
        } else {
            return null;
        }
    }

}
