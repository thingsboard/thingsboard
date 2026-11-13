// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.cache;

import org.junit.jupiter.api.Test;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.redis.connection.RedisConnection;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.exceptions.JedisNoScriptException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedisTbCacheTransactionTest {

    @SuppressWarnings("unchecked")
    private final RedisTbTransactionalCache<String, String> cache = mock(RedisTbTransactionalCache.class);
    private final RedisConnection connection = mock(RedisConnection.class);
    private final RedisTbCacheTransaction<String, String> transaction = new RedisTbCacheTransaction<>(cache, connection);

    @Test
    void givenNoScriptOnExec_whenCommit_thenLoadsScriptsAndReportsNotApplied() {
        when(connection.exec()).thenThrow(new InvalidDataAccessApiUsageException("NOSCRIPT No matching script.",
                new JedisNoScriptException("NOSCRIPT No matching script.")));

        assertThat(transaction.commit()).isFalse();

        verify(cache).loadScripts(connection);
        verify(connection).close();
    }

    @Test
    void givenOtherErrorOnExec_whenCommit_thenThrows() {
        when(connection.exec()).thenThrow(new InvalidDataAccessApiUsageException("WRONGTYPE", new JedisDataException("WRONGTYPE")));

        assertThatThrownBy(transaction::commit).isInstanceOf(InvalidDataAccessApiUsageException.class);

        verify(cache, never()).loadScripts(connection);
        verify(connection).close();
    }

}
