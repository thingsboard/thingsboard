/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
package org.thingsboard.server.cache.firmware;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import static org.thingsboard.server.common.data.CacheConstants.FIRMWARE_CACHE;

@Service
@ConditionalOnExpression("(('${service.type:null}'=='monolith' && '${transport.api_enabled:true}'=='true') || '${service.type:null}'=='core') && ('${cache.type:null}'=='caffeine' || '${cache.type:null}'=='caffeine')")
public class CaffeineFirmwareCacheWriter implements FirmwareCacheWriter {

    private final CacheManager cacheManager;

    public CaffeineFirmwareCacheWriter(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @Override
    public void put(String key, byte[] value) {
        cacheManager.getCache(FIRMWARE_CACHE).putIfAbsent(key, value);
    }
}
