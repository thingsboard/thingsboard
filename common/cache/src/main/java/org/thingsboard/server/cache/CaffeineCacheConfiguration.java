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

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.Ticker;
import com.github.benmanes.caffeine.cache.Weigher;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.cache.transaction.TransactionAwareCacheManagerProxy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.thingsboard.common.util.ThingsBoardThreadFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Configuration
@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "caffeine", matchIfMissing = true)
@ConfigurationProperties(prefix = "caffeine")
@EnableCaching
@Data
@Slf4j
public class CaffeineCacheConfiguration {

    private Map<String, CacheSpecs> specs;

    @Value("${cache.stats.enabled:true}")
    private boolean cacheStatsEnabled;

    @Value("${cache.stats.interval:60}")
    private long cacheStatsInterval;

    @Lazy
    @Autowired
    private CacheManager cacheManager;

    private ScheduledExecutorService scheduler = null;

    List<CaffeineCache> caches = Collections.emptyList();

    /**
     * Transaction aware CaffeineCache implementation with TransactionAwareCacheManagerProxy
     * to synchronize cache put/evict operations with ongoing Spring-managed transactions.
     */
    @Bean
    public CacheManager cacheManager() {
        log.trace("Initializing cache: {} specs {}", Arrays.toString(RemovalCause.values()), specs);
        SimpleCacheManager manager = new SimpleCacheManager();
        if (specs != null) {
            caches =
                    specs.entrySet().stream()
                            .map(entry -> buildCache(entry.getKey(),
                                    entry.getValue()))
                            .collect(Collectors.toList());
            manager.setCaches(caches);
        }

        //SimpleCacheManager is not a bean (will be wrapped), so call initializeCaches manually
        manager.initializeCaches();

        return new TransactionAwareCacheManagerProxy(manager);
    }

    @PostConstruct
    public void init() {
        if (cacheStatsEnabled) {
            log.debug("initializing cache stats scheduled job");
            scheduler = Executors.newSingleThreadScheduledExecutor(ThingsBoardThreadFactory.forName("cache-stats"));
            scheduler.scheduleAtFixedRate(this::printCacheStats, cacheStatsInterval, cacheStatsInterval, TimeUnit.SECONDS);
        }
    }

    @PreDestroy
    public void stopActorSystem() {
        if (scheduler != null) {
            scheduler.shutdown();
        }
    }

    void printCacheStats() {
        caches.forEach((cache) -> {
            CacheStats stats = cache.getNativeCache().stats();
            if (stats.hitCount() != 0 || stats.missCount() != 0) {
                log.info("Caffeine [{}] hit [{}] [{}]", cache.getName(), stats.hitRate(), stats);
            }
        });
    }

    private CaffeineCache buildCache(String name, CacheSpecs cacheSpec) {
        final Caffeine<Object, Object> caffeineBuilder
                = Caffeine.newBuilder()
                .weigher(collectionSafeWeigher())
                .maximumWeight(cacheSpec.getMaxSize())
                .expireAfterWrite(cacheSpec.getTimeToLiveInMinutes(), TimeUnit.MINUTES)
                .ticker(ticker());
        if (cacheStatsEnabled) {
            caffeineBuilder.recordStats();
        }
        return new CaffeineCache(name, caffeineBuilder.build());
    }

    @Bean
    public Ticker ticker() {
        return Ticker.systemTicker();
    }

    private Weigher<? super Object, ? super Object> collectionSafeWeigher() {
        return (Weigher<Object, Object>) (key, value) -> {
            if (value instanceof Collection) {
                return ((Collection) value).size();
            }
            return 1;
        };
    }
}
