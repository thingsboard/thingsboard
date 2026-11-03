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

import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.transaction.TransactionAwareCacheDecorator;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.thingsboard.server.common.data.CacheConstants;
import org.thingsboard.server.common.data.id.TenantId;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class TBRedisCacheConfigurationTest {

    private static final String CACHE_WITH_TTL = CacheConstants.RELATIONS_CACHE;
    private static final String CACHE_WITHOUT_TTL = CacheConstants.EDGE_SESSIONS_CACHE;

    @Test
    void givenCacheSpecs_whenCacheManagerCreated_thenTimeToLiveIsApplied() {
        CacheManager cacheManager = cacheManager(Map.of(
                CACHE_WITH_TTL, cacheSpecs(1440),
                CACHE_WITHOUT_TTL, cacheSpecs(0)));

        assertThat(cacheConfiguration(cacheManager, CACHE_WITH_TTL).getTtl()).isEqualTo(Duration.ofMinutes(1440));
        assertThat(cacheConfiguration(cacheManager, CACHE_WITHOUT_TTL).getTtl()).isEqualTo(Duration.ZERO);
    }

    @Test
    void givenNoCacheSpecs_whenCacheManagerCreated_thenNoTimeToLiveIsApplied() {
        CacheManager cacheManager = cacheManager(null);

        assertThat(cacheConfiguration(cacheManager, CACHE_WITH_TTL).getTtl()).isEqualTo(Duration.ZERO);
    }

    @Test
    void givenCacheSpecs_whenCacheManagerCreated_thenEntityIdKeyConverterIsPreserved() {
        CacheManager cacheManager = cacheManager(Map.of(CACHE_WITH_TTL, cacheSpecs(1440)));

        assertThat(cacheConfiguration(cacheManager, CACHE_WITH_TTL).getConversionService()
                .canConvert(TenantId.class, String.class)).isTrue();
    }

    private static CacheManager cacheManager(Map<String, CacheSpecs> specs) {
        CacheSpecsMap cacheSpecsMap = new CacheSpecsMap();
        cacheSpecsMap.setSpecs(specs);
        TBRedisCacheConfiguration configuration = new TBRedisCacheConfiguration() {
            @Override
            protected JedisConnectionFactory loadFactory() {
                return null;
            }
        };
        RedisCacheManager cacheManager = (RedisCacheManager) configuration.cacheManager(mock(RedisConnectionFactory.class), cacheSpecsMap);
        cacheManager.afterPropertiesSet();
        return cacheManager;
    }

    private static RedisCacheConfiguration cacheConfiguration(CacheManager cacheManager, String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache instanceof TransactionAwareCacheDecorator decorator) {
            cache = decorator.getTargetCache();
        }
        return ((RedisCache) cache).getCacheConfiguration();
    }

    private static CacheSpecs cacheSpecs(int timeToLiveInMinutes) {
        CacheSpecs cacheSpecs = new CacheSpecs();
        cacheSpecs.setTimeToLiveInMinutes(timeToLiveInMinutes);
        return cacheSpecs;
    }

}
