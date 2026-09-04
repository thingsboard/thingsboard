// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisConnection;

import java.io.Serializable;
import java.util.Objects;

@Slf4j
@RequiredArgsConstructor
public class RedisTbCacheTransaction<K extends Serializable, V extends Serializable> implements TbCacheTransaction<K, V> {

    private final RedisTbTransactionalCache<K, V> cache;
    private final RedisConnection connection;

    @Override
    public void put(K key, V value) {
        cache.put(key, value, connection);
    }

    @Override
    public boolean commit() {
        try {
            var execResult = connection.exec();
            var result = execResult != null && execResult.stream().anyMatch(Objects::nonNull);
            return result;
        } finally {
            connection.close();
        }
    }

    @Override
    public void rollback() {
        try {
            connection.discard();
        } finally {
            connection.close();
        }
    }

}
