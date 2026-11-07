// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.cache.ota;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStringCommands.SetOption;
import org.springframework.data.redis.core.types.Expiration;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.stats.StatsFactory;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.thingsboard.server.common.data.CacheConstants.OTA_PACKAGE_DATA_CACHE;

/**
 * Redis-backed OTA package data cache with an optional transport-local block cache (L1) in front of it.
 * <p>
 * Empty results are never cached: an absent key (or a read past EOF) must be re-checked in Redis, because
 * the package may be filled by tb-core at any moment.
 * <p>
 * The Redis TTL, when enabled ({@code cache.ota.redis_ttl_minutes > 0}), slides on every read. Only tb-core can
 * refill the key, and MQTT serves chunks without revisiting core, so an absolute TTL expiring mid-download would
 * serve empty chunks to slow devices with no refill path. When both L1 and the Redis TTL are enabled,
 * set {@code redis_ttl_minutes} well above {@code l1_ttl_minutes} plus the slowest device's download time.
 * L1 is expire-after-access, so warm blocks may never be refetched and Redis may see no TTL slides
 * during an active rollout.
 * <p>
 * {@link #put} uses SET NX: package bytes are immutable for a given key, so concurrent fills are idempotent.
 * <p>
 * {@link #has} always checks Redis directly ({@code EXISTS}), bypassing L1: tb-core uses it to decide whether to
 * refill the key, and a warm L1 block must not mask a key lost to TTL, eviction, a flush or a failover.
 * <p>
 * {@link #evict} is only invoked in tb-core (transports have no OTA evict consumer), so after a package is deleted
 * a transport may keep serving its cached blocks for up to {@code l1_ttl_minutes}. Package bytes are immutable and
 * deletion requires the package to be unassigned first, so this only extends downloads already in flight.
 */
@Service
@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "redis")
public class RedisOtaPackageDataCache implements OtaPackageDataCache {

    private final RedisConnectionFactory redisConnectionFactory;
    private final String keyPrefix;
    private final int l1BlockSize;
    private final int redisTtlMinutes;
    private final OtaPackageBlockCache blockCache; // null when L1 is disabled

    public RedisOtaPackageDataCache(RedisConnectionFactory redisConnectionFactory,
                                    StatsFactory statsFactory,
                                    @Value("${cache.key_prefix:}") String keyPrefix,
                                    @Value("${cache.ota.l1_max_bytes:0}") long l1MaxBytes,
                                    @Value("${cache.ota.l1_ttl_minutes:60}") int l1TtlMinutes,
                                    @Value("${cache.ota.l1_block_size_bytes:1048576}") int l1BlockSize,
                                    @Value("${cache.ota.redis_ttl_minutes:0}") int redisTtlMinutes) {
        this.redisConnectionFactory = redisConnectionFactory;
        this.keyPrefix = keyPrefix == null ? "" : keyPrefix;
        this.l1BlockSize = l1BlockSize;
        this.redisTtlMinutes = redisTtlMinutes;
        this.blockCache = l1MaxBytes > 0
                ? new OtaPackageBlockCache(l1MaxBytes, Duration.ofMinutes(l1TtlMinutes), l1BlockSize,
                this::fetchBlock, this::fetchLength, statsFactory)
                : null;
    }

    @Override
    public byte[] get(String key) {
        return get(key, 0, 0);
    }

    @Override
    public byte[] get(String key, int chunkSize, int chunk) {
        if (blockCache != null) {
            return chunkSize == 0 ? blockCache.getAll(key) : blockCache.getChunk(key, chunkSize, chunk);
        }
        byte[] rawKey = toOtaPackageCacheKey(key);
        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            byte[] result;
            if (chunkSize == 0) {
                result = connection.stringCommands().get(rawKey);
            } else {
                int startIndex = chunkSize * chunk;
                int endIndex = startIndex + chunkSize - 1;
                result = connection.stringCommands().getRange(rawKey, startIndex, endIndex);
            }
            slideExpire(connection, rawKey);
            return result;
        }
    }

    @Override
    public boolean has(String key) {
        byte[] rawKey = toOtaPackageCacheKey(key);
        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            Boolean exists = connection.keyCommands().exists(rawKey);
            slideExpire(connection, rawKey);
            return Boolean.TRUE.equals(exists);
        }
    }

    @Override
    public void put(String key, byte[] value) {
        Expiration expiration = redisTtlMinutes > 0 ? Expiration.from(redisTtlMinutes, TimeUnit.MINUTES) : Expiration.persistent();
        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            connection.stringCommands().set(toOtaPackageCacheKey(key), value, expiration, SetOption.SET_IF_ABSENT);
        }
    }

    @Override
    public void evict(String key) {
        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            connection.keyCommands().del(toOtaPackageCacheKey(key));
            if (!keyPrefix.isEmpty()) {
                // also drop the legacy unprefixed blob so pre-upgrade data is not orphaned
                connection.keyCommands().del(String.format("%s::%s", OTA_PACKAGE_DATA_CACHE, key).getBytes());
            }
        }
        if (blockCache != null) {
            blockCache.evict(key);
        }
    }

    private byte[] fetchBlock(String key, int blockIndex) {
        byte[] rawKey = toOtaPackageCacheKey(key);
        long start = (long) blockIndex * l1BlockSize;
        long end = start + l1BlockSize - 1;
        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            byte[] block = connection.stringCommands().getRange(rawKey, start, end);
            slideExpire(connection, rawKey);
            return block;
        }
    }

    private long fetchLength(String key) {
        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            byte[] rawKey = toOtaPackageCacheKey(key);
            Long length = connection.stringCommands().strLen(rawKey);
            slideExpire(connection, rawKey);
            return length == null ? 0 : length;
        }
    }

    private void slideExpire(RedisConnection connection, byte[] rawKey) {
        if (redisTtlMinutes > 0) {
            connection.keyCommands().expire(rawKey, TimeUnit.MINUTES.toSeconds(redisTtlMinutes));
        }
    }

    private byte[] toOtaPackageCacheKey(String key) {
        return String.format("%s%s::%s", keyPrefix, OTA_PACKAGE_DATA_CACHE, key).getBytes();
    }
}
