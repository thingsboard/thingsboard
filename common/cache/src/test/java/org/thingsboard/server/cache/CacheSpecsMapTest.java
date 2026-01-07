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
package org.thingsboard.server.cache;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.thingsboard.server.common.data.CacheConstants;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {CacheSpecsMap.class, TbCaffeineCacheConfiguration.class})
@EnableConfigurationProperties
@TestPropertySource(properties = {
        "cache.type=caffeine",
        "cache.specs.relations.timeToLiveInMinutes=1440",
        "cache.specs.relations.maxSize=0",
        "cache.specs.devices.timeToLiveInMinutes=60",
        "cache.specs.devices.maxSize=100"})
@Slf4j
public class CacheSpecsMapTest {

    @Autowired
    CacheManager cacheManager;

    @Test
    public void verifyNotTransactionAwareCacheManagerProxy() {
        // We no longer use built-in transaction support for the caches, because we have our own cache cleanup and transaction logic that implements CAS.
        assertThat(cacheManager).isInstanceOf(SimpleCacheManager.class);
    }

    @Test
    public void givenCacheConfig_whenCacheManagerReady_thenVerifyExistedCachesWithNoTransactionAwareCacheDecorator() {
        // We no longer use built-in transaction support for the caches, because we have our own cache cleanup and transaction logic that implements CAS.
        assertThat(cacheManager.getCache(CacheConstants.RELATIONS_CACHE)).isInstanceOf(CaffeineCache.class);
        assertThat(cacheManager.getCache(CacheConstants.DEVICE_CACHE)).isInstanceOf(CaffeineCache.class);
    }

    @Test
    public void givenCacheConfig_whenCacheManagerReady_thenVerifyNonExistedCaches() {
        assertThat(cacheManager.getCache("rainbows_and_unicorns")).isNull();
    }

}
