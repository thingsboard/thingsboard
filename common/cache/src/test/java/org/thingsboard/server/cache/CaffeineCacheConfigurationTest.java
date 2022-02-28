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
package org.thingsboard.server.cache;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.transaction.TransactionAwareCacheDecorator;
import org.springframework.cache.transaction.TransactionAwareCacheManagerProxy;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = CaffeineCacheConfiguration.class)
@EnableConfigurationProperties
@TestPropertySource(properties = {
        "cache.type=caffeine",
        "caffeine.specs.relations.timeToLiveInMinutes=1440",
        "caffeine.specs.relations.maxSize=0",
        "caffeine.specs.devices.timeToLiveInMinutes=60",
        "caffeine.specs.devices.maxSize=100"})
@Slf4j
public class CaffeineCacheConfigurationTest {

    @Autowired
    CacheManager cacheManager;

    @Test
    public void verifyTransactionAwareCacheManagerProxy() {
        assertThat(cacheManager).isInstanceOf(TransactionAwareCacheManagerProxy.class);
    }

    @Test
    public void givenCacheConfig_whenCacheManagerReady_thenVerifyExistedCachesWithTransactionAwareCacheDecorator() {
        assertThat(cacheManager.getCache("relations")).isInstanceOf(TransactionAwareCacheDecorator.class);
        assertThat(cacheManager.getCache("devices")).isInstanceOf(TransactionAwareCacheDecorator.class);
    }

    @Test
    public void givenCacheConfig_whenCacheManagerReady_thenVerifyNonExistedCaches() {
        assertThat(cacheManager.getCache("rainbows_and_unicorns")).isNull();
    }
}