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
                return connection.get(toOtaPackageCacheKey(key));
            }

            int startIndex = chunkSize * chunk;
            int endIndex = startIndex + chunkSize - 1;
            return connection.getRange(toOtaPackageCacheKey(key), startIndex, endIndex);
        }
    }

    @Override
    public void put(String key, byte[] value) {
        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            connection.set(toOtaPackageCacheKey(key), value);
        }
    }

    @Override
    public void evict(String key) {
        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            connection.del(toOtaPackageCacheKey(key));
        }
    }

    private byte[] toOtaPackageCacheKey(String key) {
        return String.format("%s::%s", OTA_PACKAGE_DATA_CACHE, key).getBytes();
    }
}
