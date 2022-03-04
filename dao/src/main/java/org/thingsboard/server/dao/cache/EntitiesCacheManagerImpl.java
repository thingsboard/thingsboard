/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.server.dao.cache;

import lombok.AllArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.Arrays;

import static org.thingsboard.server.common.data.CacheConstants.ASSET_CACHE;
import static org.thingsboard.server.common.data.CacheConstants.DEVICE_CACHE;
import static org.thingsboard.server.common.data.CacheConstants.EDGE_CACHE;

@Component
@AllArgsConstructor
public class EntitiesCacheManagerImpl implements EntitiesCacheManager {

    private final CacheManager cacheManager;

    @Override
    public void removeDeviceFromCacheByName(TenantId tenantId, String name) {
        Cache cache = cacheManager.getCache(DEVICE_CACHE);
        cache.evict(Arrays.asList(tenantId, name));
    }

    @Override
    public void removeDeviceFromCacheById(TenantId tenantId, DeviceId deviceId) {
        if (deviceId == null) {
            return;
        }
        Cache cache = cacheManager.getCache(DEVICE_CACHE);
        cache.evict(Arrays.asList(tenantId, deviceId));
    }

    @Override
    public void removeAssetFromCacheByName(TenantId tenantId, String name) {
        Cache cache = cacheManager.getCache(ASSET_CACHE);
        cache.evict(Arrays.asList(tenantId, name));
    }

    @Override
    public void removeEdgeFromCacheByName(TenantId tenantId, String name) {
        Cache cache = cacheManager.getCache(EDGE_CACHE);
        cache.evict(Arrays.asList(tenantId, name));
    }

}
