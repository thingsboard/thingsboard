// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.redis.connection.RedisConnection;
import redis.clients.jedis.exceptions.JedisNoScriptException;

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
        } catch (InvalidDataAccessApiUsageException e) {
            if (!(e.getCause() instanceof JedisNoScriptException)) {
                throw e;
            }
            // The node has not loaded the script yet, e.g. a replica that a failover just promoted, and a script
            // queued in MULTI cannot fall back to loading it. Load it now so the next transaction on this node works;
            // this one is not applied, like a transaction aborted by WATCH.
            log.debug("[{}] Transaction failed with NOSCRIPT, loading the cache scripts on the node", cache.getCacheName());
            cache.loadScripts(connection);
            return false;
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
