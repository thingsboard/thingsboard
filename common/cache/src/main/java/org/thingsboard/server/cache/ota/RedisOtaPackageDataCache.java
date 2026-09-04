// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.cache.ota;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Service;

import static org.thingsboard.server.common.data.CacheConstants.OTA_PACKAGE_DATA_CACHE;

@Service
@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "redis")
@RequiredArgsConstructor
public class RedisOtaPackageDataCache implements OtaPackageDataCache {

    private final RedisConnectionFactory redisConnectionFactory;

    @Override
    public byte[] get(String key) {
        return get(key, 0, 0);
    }

    @Override
    public byte[] get(String key, int chunkSize, int chunk) {
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
    public void put(String key, byte[] value) {
        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            connection.stringCommands().set(toOtaPackageCacheKey(key), value);
        }
    }

    @Override
    public void evict(String key) {
        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            connection.keyCommands().del(toOtaPackageCacheKey(key));
        }
    }

    private byte[] toOtaPackageCacheKey(String key) {
        return String.format("%s::%s", OTA_PACKAGE_DATA_CACHE, key).getBytes();
    }
}
