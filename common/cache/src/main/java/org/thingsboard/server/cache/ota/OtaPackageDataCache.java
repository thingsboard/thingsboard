// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.cache.ota;

public interface OtaPackageDataCache {

    byte[] get(String key);

    byte[] get(String key, int chunkSize, int chunk);

    void put(String key, byte[] value);

    void evict(String key);

    default boolean has(String otaPackageId) {
        byte[] data = get(otaPackageId, 1, 0);
        return data != null && data.length > 0;
    }
}
