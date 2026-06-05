/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.transport.lwm2m.server.store;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2MClientState;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClient;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.thingsboard.server.transport.lwm2m.server.store.util.LwM2MClientSerDes.serialize;

/**
 * Verifies that {@link TbRedisLwM2MClientStore#getAll()} uses separate connections for
 * SCAN and GET operations to prevent Jedis 5.x response-ordering corruption that occurs
 * when both commands share the same connection.
 */
@ExtendWith(MockitoExtension.class)
class TbRedisLwM2MClientStoreTest {

    @Mock
    RedisConnectionFactory connectionFactory;

    @Mock
    RedisConnection scanConnection;

    @Mock
    RedisConnection getConnection;

    TbRedisLwM2MClientStore store;

    @BeforeEach
    void setUp() {
        // First getConnection() call → scanConnection, second → getConnection
        when(connectionFactory.getConnection())
                .thenReturn(scanConnection)
                .thenReturn(getConnection);
        store = new TbRedisLwM2MClientStore(connectionFactory);
    }

    @Test
    void getAll_returnsSingleClient() {
        LwM2mClient client = new LwM2mClient("nodeId", "testEndpoint");
        client.setState(LwM2MClientState.REGISTERED);
        byte[] key = "CLIENT#EP#testEndpoint".getBytes();
        byte[] value = serialize(client);

        // Cursor created before thenReturn to avoid Mockito unfinished-stubbing error
        Cursor<byte[]> cursor = cursorOf(key);
        when(scanConnection.scan(any(ScanOptions.class))).thenReturn(cursor);
        when(getConnection.get(key)).thenReturn(value);

        Set<LwM2mClient> result = store.getAll();

        assertThat(result).hasSize(1);
        assertThat(result.iterator().next().getEndpoint()).isEqualTo("testEndpoint");
    }

    @Test
    void getAll_getIsNeverCalledOnScanConnection() {
        Cursor<byte[]> cursor = cursorOf();
        when(scanConnection.scan(any(ScanOptions.class))).thenReturn(cursor);

        store.getAll();

        verify(scanConnection, never()).get(any(byte[].class));
    }

    @Test
    void getAll_scanIsNeverCalledOnGetConnection() {
        Cursor<byte[]> cursor = cursorOf();
        when(scanConnection.scan(any(ScanOptions.class))).thenReturn(cursor);

        store.getAll();

        verify(getConnection, never()).scan(any(ScanOptions.class));
    }

    @Test
    void getAll_skipsKeyWhenValueIsNull() {
        byte[] key = "CLIENT#EP#gone".getBytes();
        Cursor<byte[]> cursor = cursorOf(key);
        when(scanConnection.scan(any(ScanOptions.class))).thenReturn(cursor);
        // getConnection.get(key) returns null by default — no stubbing needed

        Set<LwM2mClient> result = store.getAll();

        assertThat(result).isEmpty();
    }

    /**
     * Creates a mock {@link Cursor} that iterates over the given keys via {@code forEachRemaining}.
     * The cursor is created separately (not inside a {@code thenReturn()} argument) to avoid
     * Mockito's "unfinished stubbing" error caused by nested {@code when()} calls.
     */
    @SuppressWarnings("unchecked")
    private static Cursor<byte[]> cursorOf(byte[]... keys) {
        Cursor<byte[]> cursor = mock(Cursor.class);
        List<byte[]> keyList = List.of(keys);
        doAnswer(inv -> {
            Consumer<byte[]> action = inv.getArgument(0);
            keyList.forEach(action);
            return null;
        }).when(cursor).forEachRemaining(any(Consumer.class));
        return cursor;
    }
}
