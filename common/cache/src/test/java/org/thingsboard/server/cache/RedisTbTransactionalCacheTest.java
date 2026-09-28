// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.cache;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisScriptingCommands;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pins the raw Redis key layout of {@link RedisTbTransactionalCache}: with the empty default
 * {@code cache.key_prefix} the layout must stay exactly {@code cacheName + key} (production key compatibility on
 * upgrade), a configured prefix must be prepended, and {@link RedisTbTransactionalCache#evictByPrefix} must build
 * its scan prefix from the same {@code rawKeyPrefix} as {@link RedisTbTransactionalCache#getRawKey} — if the two
 * drift, prefix eviction silently stops matching written keys.
 */
class RedisTbTransactionalCacheTest {

    private static final String CACHE_NAME = "testCache";

    private JedisConnectionFactory connectionFactory;
    private RedisScriptingCommands scriptingCommands;

    @Test
    void givenEmptyKeyPrefix_whenGetRawKey_thenProductionKeyLayoutIsUnchanged() {
        TestRedisCache cache = createCache("");
        assertThat(rawKeyString(cache, "myKey")).isEqualTo(CACHE_NAME + "myKey");
    }

    @Test
    void givenNullKeyPrefix_whenGetRawKey_thenProductionKeyLayoutIsUnchanged() {
        TestRedisCache cache = createCache(null);
        assertThat(rawKeyString(cache, "myKey")).isEqualTo(CACHE_NAME + "myKey");
    }

    @Test
    void givenConfiguredKeyPrefix_whenGetRawKey_thenPrefixIsPrepended() {
        TestRedisCache cache = createCache("citus:");
        assertThat(rawKeyString(cache, "myKey")).isEqualTo("citus:" + CACHE_NAME + "myKey");
    }

    @Test
    void givenEmptyKeyPrefix_whenEvictByPrefix_thenScanPrefixMatchesRawKeyLayout() {
        TestRedisCache cache = createCache("");

        String scanPrefix = evictByPrefixScanPrefix(cache, "device_");

        assertThat(scanPrefix).isEqualTo(CACHE_NAME + "device_");
        // keys written via getRawKey must be matched by the eviction scan
        assertThat(rawKeyString(cache, "device_1")).startsWith(scanPrefix);
    }

    @Test
    void givenConfiguredKeyPrefix_whenEvictByPrefix_thenScanPrefixMatchesRawKeyLayout() {
        TestRedisCache cache = createCache("citus:");

        String scanPrefix = evictByPrefixScanPrefix(cache, "device_");

        assertThat(scanPrefix).isEqualTo("citus:" + CACHE_NAME + "device_");
        assertThat(rawKeyString(cache, "device_1")).startsWith(scanPrefix);
    }

    private TestRedisCache createCache(String keyPrefix) {
        connectionFactory = mock(JedisConnectionFactory.class);
        RedisConnection connection = mock(RedisConnection.class);
        scriptingCommands = mock(RedisScriptingCommands.class);
        when(connectionFactory.isRedisClusterAware()).thenReturn(false);
        when(connectionFactory.getConnection()).thenReturn(connection);
        when(connection.scriptingCommands()).thenReturn(scriptingCommands);

        TBRedisCacheConfiguration configuration = new TBRedisCacheConfiguration() {
            @Override
            protected JedisConnectionFactory loadFactory() {
                return connectionFactory;
            }
        };
        configuration.setKeyPrefix(keyPrefix);
        configuration.setEvictTtlInMs(60000);

        CacheSpecs cacheSpecs = new CacheSpecs();
        cacheSpecs.setMaxSize(100);
        CacheSpecsMap cacheSpecsMap = new CacheSpecsMap();
        cacheSpecsMap.setSpecs(Map.of(CACHE_NAME, cacheSpecs));

        return new TestRedisCache(cacheSpecsMap, connectionFactory, configuration);
    }

    private static String rawKeyString(TestRedisCache cache, String key) {
        return new String(cache.getRawKey(key), StandardCharsets.UTF_8);
    }

    private String evictByPrefixScanPrefix(TestRedisCache cache, String prefix) {
        cache.evictByPrefix(prefix);
        ArgumentCaptor<byte[]> scriptArgs = ArgumentCaptor.forClass(byte[].class);
        verify(scriptingCommands).evalSha(any(byte[].class), eq(ReturnType.INTEGER), eq(0), scriptArgs.capture(), scriptArgs.capture());
        // varargs are (scanPrefix, batchSize); the first captured value is the scan prefix
        return new String(scriptArgs.getAllValues().get(0), StandardCharsets.UTF_8);
    }

    private static class TestRedisCache extends RedisTbTransactionalCache<String, String> {

        TestRedisCache(CacheSpecsMap cacheSpecsMap, RedisConnectionFactory connectionFactory, TBRedisCacheConfiguration configuration) {
            super(CACHE_NAME, cacheSpecsMap, connectionFactory, configuration, new TbRedisSerializer<>() {
                @Override
                public byte[] serialize(String value) {
                    return value == null ? null : value.getBytes(StandardCharsets.UTF_8);
                }

                @Override
                public String deserialize(String key, byte[] bytes) {
                    return bytes == null ? null : new String(bytes, StandardCharsets.UTF_8);
                }
            });
        }

    }

}
