// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.cache.ota;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStringCommands.SetOption;
import org.springframework.data.redis.core.types.Expiration;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cache.CacheSpecs;
import org.thingsboard.server.cache.CacheSpecsMap;
import org.thingsboard.server.common.stats.StatsFactory;

import java.time.Duration;

import static org.thingsboard.server.common.data.CacheConstants.OTA_PACKAGE_DATA_BLOCKS_CACHE;
import static org.thingsboard.server.common.data.CacheConstants.OTA_PACKAGE_DATA_CACHE;

/**
 * Redis-backed OTA package data cache with an optional block cache in each node's memory in front of it,
 * configured by {@code cache.specs.otaPackagesDataBlocks}: {@code maxSize} is the number of 1 MiB blocks
 * (0 disables it, the default) and {@code timeToLiveInMinutes} expires each block after it was loaded.
 * <p>
 * Empty results are never cached: an absent key (or a read past EOF) must be re-checked in Redis, because
 * the package may be filled by tb-core at any moment.
 * <p>
 * {@link #put} uses SET NX: package bytes are immutable for a given key, so concurrent fills are idempotent.
 * <p>
 * {@link #has} always checks Redis directly ({@code EXISTS}), bypassing the block cache: tb-core uses it to decide
 * whether to refill the key, and a cached block must not mask a key lost to eviction, a flush or a failover.
 * <p>
 * {@link #evict} is only invoked in tb-core (transports have no OTA evict consumer), so after a package is deleted
 * a transport may keep serving its cached blocks for up to {@code timeToLiveInMinutes}. Package bytes are immutable
 * and deletion requires the package to be unassigned first, so this only extends downloads already in flight.
 */
@Service
@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "redis")
public class RedisOtaPackageDataCache implements OtaPackageDataCache {

    static final int BLOCK_SIZE_BYTES = 1024 * 1024;

    private final RedisConnectionFactory redisConnectionFactory;
    private final String keyPrefix;
    private final int blockSize;
    private final OtaPackageBlockCache blockCache; // null when the block cache is disabled

    @Autowired
    public RedisOtaPackageDataCache(RedisConnectionFactory redisConnectionFactory,
                                    StatsFactory statsFactory,
                                    CacheSpecsMap cacheSpecsMap,
                                    @Value("${cache.key_prefix:}") String keyPrefix) {
        this(redisConnectionFactory, statsFactory, keyPrefix,
                cacheSpecsMap.getSpecs() != null ? cacheSpecsMap.getSpecs().get(OTA_PACKAGE_DATA_BLOCKS_CACHE) : null,
                BLOCK_SIZE_BYTES);
    }

    RedisOtaPackageDataCache(RedisConnectionFactory redisConnectionFactory,
                             StatsFactory statsFactory,
                             String keyPrefix,
                             CacheSpecs blockCacheSpecs,
                             int blockSize) {
        this.redisConnectionFactory = redisConnectionFactory;
        this.keyPrefix = keyPrefix == null ? "" : keyPrefix;
        this.blockSize = blockSize;
        int maxBlocks = blockCacheSpecs != null && blockCacheSpecs.getMaxSize() != null ? blockCacheSpecs.getMaxSize() : 0;
        int ttlMinutes = blockCacheSpecs != null && blockCacheSpecs.getTimeToLiveInMinutes() != null ? blockCacheSpecs.getTimeToLiveInMinutes() : 0;
        this.blockCache = maxBlocks > 0
                ? new OtaPackageBlockCache((long) maxBlocks * blockSize, Duration.ofMinutes(ttlMinutes), blockSize,
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
        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            if (chunkSize == 0) {
                return connection.stringCommands().get(toOtaPackageCacheKey(key));
            }
            int startIndex = chunkSize * chunk;
            int endIndex = startIndex + chunkSize - 1;
            return connection.stringCommands().getRange(toOtaPackageCacheKey(key), startIndex, endIndex);
        }
    }

    @Override
    public boolean has(String key) {
        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            return Boolean.TRUE.equals(connection.keyCommands().exists(toOtaPackageCacheKey(key)));
        }
    }

    @Override
    public void put(String key, byte[] value) {
        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            connection.stringCommands().set(toOtaPackageCacheKey(key), value, Expiration.persistent(), SetOption.SET_IF_ABSENT);
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
        long start = (long) blockIndex * blockSize;
        long end = start + blockSize - 1;
        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            return connection.stringCommands().getRange(toOtaPackageCacheKey(key), start, end);
        }
    }

    private long fetchLength(String key) {
        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            Long length = connection.stringCommands().strLen(toOtaPackageCacheKey(key));
            return length == null ? 0 : length;
        }
    }

    private byte[] toOtaPackageCacheKey(String key) {
        return String.format("%s%s::%s", keyPrefix, OTA_PACKAGE_DATA_CACHE, key).getBytes();
    }
}
