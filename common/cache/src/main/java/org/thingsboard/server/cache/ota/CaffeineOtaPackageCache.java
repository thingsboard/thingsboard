// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
