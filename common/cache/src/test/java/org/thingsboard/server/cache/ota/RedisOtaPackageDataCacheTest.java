// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.cache.ota;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisKeyCommands;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.connection.RedisStringCommands.SetOption;
import org.springframework.data.redis.core.types.Expiration;
import org.thingsboard.server.cache.CacheSpecs;
import org.thingsboard.server.cache.CacheSpecsMap;
import org.thingsboard.server.common.stats.StatsFactory;

import java.util.Arrays;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.thingsboard.server.common.data.CacheConstants.OTA_PACKAGE_DATA_BLOCKS_CACHE;

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

    private static CacheSpecs blockSpecs(int maxBlocks) {
        CacheSpecs specs = new CacheSpecs();
        specs.setTimeToLiveInMinutes(60);
        specs.setMaxSize(maxBlocks);
        return specs;
    }

    private RedisOtaPackageDataCache cache(String prefix, int maxBlocks, int blockSize) {
        return new RedisOtaPackageDataCache(connectionFactory, mock(StatsFactory.class), prefix, blockSpecs(maxBlocks), blockSize);
    }

    private RedisOtaPackageDataCache cacheFromSpecsMap(Map<String, CacheSpecs> specs) {
        CacheSpecsMap specsMap = new CacheSpecsMap();
        specsMap.setSpecs(specs);
        return new RedisOtaPackageDataCache(connectionFactory, mock(StatsFactory.class), specsMap, "");
    }

    @Test
    void blockCacheOff_chunkRead_issuesLegacyGetRange() {
        byte[] data = {1, 2, 3};
        when(stringCommands.getRange(RAW_KEY, 128, 191)).thenReturn(data);

        assertThat(cache("", 0, MIB).get("id", 64, 2)).isEqualTo(data);

        verify(stringCommands).getRange(RAW_KEY, 128, 191);
        verifyNoMoreInteractions(stringCommands);
        verifyNoInteractions(keyCommands);
    }

    @Test
    void blockCacheOff_fullRead_issuesLegacyGet() {
        byte[] data = {1, 2, 3};
        when(stringCommands.get(RAW_KEY)).thenReturn(data);

        assertThat(cache("", 0, MIB).get("id")).isEqualTo(data);

        verify(stringCommands).get(RAW_KEY);
        verifyNoMoreInteractions(stringCommands);
        verifyNoInteractions(keyCommands);
    }

    @Test
    void put_usesSetNxWithoutExpiration() {
        byte[] value = {1, 2, 3};

        cache("", 0, MIB).put("id", value);

        verify(stringCommands).set(RAW_KEY, value, Expiration.persistent(), SetOption.SET_IF_ABSENT);
        verifyNoMoreInteractions(stringCommands);
        verifyNoInteractions(keyCommands);
    }

    @Test
    void keyPrefix_isPrependedToRawKey() {
        cache("citus:", 0, MIB).get("id", 64, 0);

        verify(stringCommands).getRange("citus:otaPackagesData::id".getBytes(), 0, 63);
    }

    @Test
    void emptyKeyPrefix_keepsLegacyRawKey() {
        cache("", 0, MIB).get("id", 64, 0);
        cache(null, 0, MIB).get("id", 64, 0);

        verify(stringCommands, times(2)).getRange("otaPackagesData::id".getBytes(), 0, 63);
    }

    @Test
    void specsMap_withBlocksEntry_enablesBlockCacheWithOneMiBBlocks() {
        byte[] block = new byte[MIB];
        when(stringCommands.getRange(RAW_KEY, 0, MIB - 1)).thenReturn(block);

        cacheFromSpecsMap(Map.of(OTA_PACKAGE_DATA_BLOCKS_CACHE, blockSpecs(2))).get("id", 64, 0);

        verify(stringCommands).getRange(RAW_KEY, 0, MIB - 1);
        verifyNoMoreInteractions(stringCommands);
    }

    @Test
    void specsMap_withZeroMaxSize_keepsBlockCacheOff() {
        cacheFromSpecsMap(Map.of(OTA_PACKAGE_DATA_BLOCKS_CACHE, blockSpecs(0))).get("id", 64, 0);

        verify(stringCommands).getRange(RAW_KEY, 0, 63);
        verifyNoMoreInteractions(stringCommands);
    }

    @Test
    void specsMap_withoutBlocksEntry_keepsBlockCacheOff() {
        cacheFromSpecsMap(null).get("id", 64, 0);
        cacheFromSpecsMap(Map.of()).get("id", 64, 0);

        verify(stringCommands, times(2)).getRange(RAW_KEY, 0, 63);
        verifyNoMoreInteractions(stringCommands);
    }

    @Test
    void blockCacheOn_chunkRead_fetchesWholeBlockViaGetRange() {
        byte[] block = new byte[MIB];
        Arrays.fill(block, (byte) 7);
        when(stringCommands.getRange(RAW_KEY, 0, MIB - 1)).thenReturn(block);
        RedisOtaPackageDataCache cache = cache("", 10, MIB);

        assertThat(cache.get("id", 64, 0)).isEqualTo(Arrays.copyOf(block, 64));
        verify(stringCommands).getRange(RAW_KEY, 0, MIB - 1);
        verifyNoMoreInteractions(stringCommands);

        assertThat(cache.get("id", 64, 1)).isEqualTo(Arrays.copyOf(block, 64));
        verifyNoMoreInteractions(stringCommands); // served from the block cache, no new Redis interaction
        verifyNoInteractions(keyCommands);
    }

    @Test
    void blockCacheOn_fullRead_usesStrLenThenBlocks() {
        byte[] data = new byte[10];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) (i + 1);
        }
        when(stringCommands.strLen(RAW_KEY)).thenReturn(10L);
        when(stringCommands.getRange(RAW_KEY, 0, 3)).thenReturn(Arrays.copyOfRange(data, 0, 4));
        when(stringCommands.getRange(RAW_KEY, 4, 7)).thenReturn(Arrays.copyOfRange(data, 4, 8));
        when(stringCommands.getRange(RAW_KEY, 8, 11)).thenReturn(Arrays.copyOfRange(data, 8, 10));

        assertThat(cache("", 256, 4).get("id")).isEqualTo(data);

        verify(stringCommands).strLen(RAW_KEY);
        verify(stringCommands).getRange(RAW_KEY, 0, 3);
        verify(stringCommands).getRange(RAW_KEY, 4, 7);
        verify(stringCommands).getRange(RAW_KEY, 8, 11);
        verifyNoMoreInteractions(stringCommands);
        verifyNoInteractions(keyCommands);
    }

    @Test
    void evict_deletesRedisKeyAndCachedBlocks() {
        byte[] block = {1, 2, 3, 4};
        when(stringCommands.getRange(RAW_KEY, 0, 3)).thenReturn(block);
        RedisOtaPackageDataCache cache = cache("", 256, 4);

        cache.get("id", 4, 0);
        cache.get("id", 4, 0);
        verify(stringCommands, times(1)).getRange(RAW_KEY, 0, 3);

        cache.evict("id");
        verify(keyCommands).del(RAW_KEY);

        cache.get("id", 4, 0);
        verify(stringCommands, times(2)).getRange(RAW_KEY, 0, 3);
    }

    @Test
    void evict_nonEmptyPrefix_deletesPrefixedAndLegacyKeys() {
        cache("citus:", 0, MIB).evict("id");

        verify(keyCommands).del("citus:otaPackagesData::id".getBytes());
        verify(keyCommands).del(RAW_KEY);
        verifyNoMoreInteractions(keyCommands);
    }

    @Test
    void evict_emptyPrefix_issuesSingleDel() {
        cache("", 0, MIB).evict("id");

        verify(keyCommands, times(1)).del(RAW_KEY);
        verifyNoMoreInteractions(keyCommands);
    }

    @Test
    void has_usesRedisExists() {
        when(keyCommands.exists(RAW_KEY)).thenReturn(true);
        RedisOtaPackageDataCache cache = cache("", 0, MIB);
        assertThat(cache.has("id")).isTrue();

        when(keyCommands.exists(RAW_KEY)).thenReturn(false);
        assertThat(cache.has("id")).isFalse();
        verifyNoInteractions(stringCommands);
    }

    @Test
    void has_blockCacheOn_bypassesCachedBlocksWhenRedisKeyIsLost() {
        byte[] block = {1, 2, 3, 4};
        when(stringCommands.getRange(RAW_KEY, 0, 3)).thenReturn(block);
        RedisOtaPackageDataCache cache = cache("", 256, 4);

        cache.get("id", 4, 0); // caches block 0

        when(keyCommands.exists(RAW_KEY)).thenReturn(false); // key lost: eviction, flush or failover
        assertThat(cache.has("id")).isFalse();
    }
}
