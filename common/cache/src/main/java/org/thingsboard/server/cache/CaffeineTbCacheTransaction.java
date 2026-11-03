// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.cache;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class CaffeineTbCacheTransaction<K extends Serializable, V extends Serializable> implements TbCacheTransaction<K, V> {
    @Getter
    private final UUID id = UUID.randomUUID();
    private final CaffeineTbTransactionalCache<K, V> cache;
    @Getter
    private final List<K> keys;
    @Getter
    @Setter
    private boolean failed;

    private final Map<K, V> pendingPuts = new LinkedHashMap<>();

    @Override
    public void put(K key, V value) {
        pendingPuts.put(key, value);
    }

    @Override
    public boolean commit() {
        return cache.commit(id, pendingPuts);
    }

    @Override
    public void rollback() {
        cache.rollback(id);
    }


}
