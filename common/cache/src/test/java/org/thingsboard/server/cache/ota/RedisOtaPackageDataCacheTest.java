// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.cache.ota;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisKeyCommands;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.connection.RedisStringCommands.SetOption;
import org.springframework.data.redis.core.types.Expiration;
import org.thingsboard.server.common.stats.StatsFactory;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class RedisOtaPackageDataCacheTest {

    private static final int MIB = 1024 * 1024;
    private static final byte[] RAW_KEY = "otaPackagesData::id".getBytes();

    private RedisConnectionFactory connectionFactory;
    private RedisConnection connection;
    private RedisStringCommands stringCommands;
    private RedisKeyCommands keyCommands;

    @BeforeEach
    void setUp() {
        connectionFactory = mock(RedisConnectionFactory.class);
        connection = mock(RedisConnection.class);
        stringCommands = mock(RedisStringCommands.class);
        keyCommands = mock(RedisKeyCommands.class);
        when(connectionFactory.getConnection()).thenReturn(connection);
        when(connection.stringCommands()).thenReturn(stringCommands);
        when(connection.keyCommands()).thenReturn(keyCommands);
    }

    private RedisOtaPackageDataCache cache(String prefix, long l1MaxBytes, int blockSize, int redisTtlMinutes) {
        return new RedisOtaPackageDataCache(connectionFactory, mock(StatsFactory.class), prefix,
                l1MaxBytes, 60, blockSize, redisTtlMinutes);
    }

    @Test
    void l1Off_chunkRead_issuesLegacyGetRange() {
        byte[] data = {1, 2, 3};
        when(stringCommands.getRange(RAW_KEY, 128, 191)).thenReturn(data);

        assertThat(cache("", 0, MIB, 0).get("id", 64, 2)).isEqualTo(data);

        verify(stringCommands).getRange(RAW_KEY, 128, 191);
        verifyNoMoreInteractions(stringCommands);
        verifyNoInteractions(keyCommands);
    }

    @Test
    void l1Off_fullRead_issuesLegacyGet() {
        byte[] data = {1, 2, 3};
        when(stringCommands.get(RAW_KEY)).thenReturn(data);

        assertThat(cache("", 0, MIB, 0).get("id")).isEqualTo(data);

        verify(stringCommands).get(RAW_KEY);
        verifyNoMoreInteractions(stringCommands);
        verifyNoInteractions(keyCommands);
    }

    @Test
    void l1Off_put_usesSetNx() {
        byte[] value = {1, 2, 3};

        cache("", 0, MIB, 0).put("id", value);

        verify(stringCommands).set(RAW_KEY, value, Expiration.persistent(), SetOption.SET_IF_ABSENT);
        verifyNoMoreInteractions(stringCommands);
    }

    @Test
    void keyPrefix_isPrependedToRawKey() {
        cache("citus:", 0, MIB, 0).get("id", 64, 0);

        verify(stringCommands).getRange("citus:otaPackagesData::id".getBytes(), 0, 63);
    }

    @Test
    void emptyKeyPrefix_keepsLegacyRawKey() {
        cache("", 0, MIB, 0).get("id", 64, 0);
        cache(null, 0, MIB, 0).get("id", 64, 0);

        verify(stringCommands, times(2)).getRange("otaPackagesData::id".getBytes(), 0, 63);
    }

    @Test
    void redisTtlOn_readSlidesExpire() {
        cache("", 0, MIB, 30).get("id", 64, 2);

        verify(stringCommands).getRange(RAW_KEY, 128, 191);
        verify(keyCommands).expire(RAW_KEY, 1800);
    }

    @Test
    void redisTtlOn_put_setsNxWithExpiration() {
        byte[] value = {1, 2, 3};

        cache("", 0, MIB, 30).put("id", value);

        verify(stringCommands).set(RAW_KEY, value, Expiration.from(30, TimeUnit.MINUTES), SetOption.SET_IF_ABSENT);
    }

    @Test
    void redisTtlOff_noExpireIssued() {
        RedisOtaPackageDataCache cache = cache("", 0, MIB, 0);
        cache.get("id", 64, 0);
        cache.get("id");

        verify(connection, never()).keyCommands();
        verifyNoInteractions(keyCommands);
    }

    @Test
    void l1On_chunkRead_fetchesWholeBlockViaGetRange() {
        byte[] block = new byte[MIB];
        Arrays.fill(block, (byte) 7);
        when(stringCommands.getRange(RAW_KEY, 0, MIB - 1)).thenReturn(block);
        RedisOtaPackageDataCache cache = cache("", 10L * MIB, MIB, 0);

        assertThat(cache.get("id", 64, 0)).isEqualTo(Arrays.copyOf(block, 64));
        verify(stringCommands).getRange(RAW_KEY, 0, MIB - 1);
        verifyNoMoreInteractions(stringCommands);

        assertThat(cache.get("id", 64, 1)).isEqualTo(Arrays.copyOf(block, 64));
        verifyNoMoreInteractions(stringCommands); // served from L1, no new Redis interaction
        verifyNoInteractions(keyCommands);
    }

    @Test
    void l1On_fullRead_usesStrLenThenBlocks() {
        byte[] data = new byte[10];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) (i + 1);
        }
        when(stringCommands.strLen(RAW_KEY)).thenReturn(10L);
        when(stringCommands.getRange(RAW_KEY, 0, 3)).thenReturn(Arrays.copyOfRange(data, 0, 4));
        when(stringCommands.getRange(RAW_KEY, 4, 7)).thenReturn(Arrays.copyOfRange(data, 4, 8));
        when(stringCommands.getRange(RAW_KEY, 8, 11)).thenReturn(Arrays.copyOfRange(data, 8, 10));

        assertThat(cache("", 1024, 4, 0).get("id")).isEqualTo(data);

        verify(stringCommands).strLen(RAW_KEY);
        verify(stringCommands).getRange(RAW_KEY, 0, 3);
        verify(stringCommands).getRange(RAW_KEY, 4, 7);
        verify(stringCommands).getRange(RAW_KEY, 8, 11);
        verifyNoMoreInteractions(stringCommands);
    }

    @Test
    void evict_deletesRedisKeyAndLocalBlocks() {
        byte[] block = {1, 2, 3, 4};
        when(stringCommands.getRange(RAW_KEY, 0, 3)).thenReturn(block);
        RedisOtaPackageDataCache cache = cache("", 1024, 4, 0);

        cache.get("id", 4, 0);
        cache.get("id", 4, 0);
        verify(stringCommands, times(1)).getRange(RAW_KEY, 0, 3);

        cache.evict("id");
        verify(keyCommands).del(RAW_KEY);

        cache.get("id", 4, 0);
        verify(stringCommands, times(2)).getRange(RAW_KEY, 0, 3);
    }
}
