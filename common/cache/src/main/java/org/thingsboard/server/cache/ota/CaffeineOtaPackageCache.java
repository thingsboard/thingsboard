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
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import static org.thingsboard.server.common.data.CacheConstants.OTA_PACKAGE_DATA_CACHE;

@Service
@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "caffeine", matchIfMissing = true)
@RequiredArgsConstructor
public class CaffeineOtaPackageCache implements OtaPackageDataCache {

    private final CacheManager cacheManager;

    @Override
    public byte[] get(String key) {
        return get(key, 0, 0);
    }

    @Override
    public byte[] get(String key, int chunkSize, int chunk) {
        byte[] data = cacheManager.getCache(OTA_PACKAGE_DATA_CACHE).get(key, byte[].class);

        if (chunkSize < 1) {
            return data;
        }

        if (data != null && data.length > 0) {
            int startIndex = chunkSize * chunk;

            int size = Math.min(data.length - startIndex, chunkSize);

            if (startIndex < data.length && size > 0) {
                byte[] result = new byte[size];
                System.arraycopy(data, startIndex, result, 0, size);
                return result;
            }
        }
        return new byte[0];
    }

    @Override
    public void put(String key, byte[] value) {
        cacheManager.getCache(OTA_PACKAGE_DATA_CACHE).putIfAbsent(key, value);
    }

    @Override
    public void evict(String key) {
        cacheManager.getCache(OTA_PACKAGE_DATA_CACHE).evict(key);
    }
}
