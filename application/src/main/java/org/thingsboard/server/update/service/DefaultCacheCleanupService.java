/*
Author Ahmet Ertuğrul KAYA
*/
package org.thingsboard.server.update.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.thingsboard.server.service.install.update.CacheCleanupService;

import java.util.Objects;
import java.util.Optional;

import static org.thingsboard.server.common.data.CacheConstants.RESOURCE_INFO_CACHE;
import static org.thingsboard.server.common.data.CacheConstants.SECURITY_SETTINGS_CACHE;

@RequiredArgsConstructor
@Service
@Profile("update")
@Slf4j
public class DefaultCacheCleanupService implements CacheCleanupService {

    private final CacheManager cacheManager;
    private final Optional<RedisTemplate<String, Object>> redisTemplate;


    /**
     * Cleanup caches that can not deserialize anymore due to schema upgrade or data update using sql scripts.
     * Refer to SqlDatabaseUpgradeService and /data/upgrage/*.sql
     * to discover which tables were changed
     * */
    @Override
    public void clearCache(String fromVersion) throws Exception {
        switch (fromVersion) {
            case "3.2.2":
                log.info("Clear cache to upgrade from version 3.2.2 to 3.3.0 ...");
                clearCacheByName("devices");
                clearCacheByName("deviceProfiles");
                clearCacheByName("tenantProfiles");
                clearCacheByName("relations");
                break;
            case "3.3.2":
                log.info("Clear cache to upgrade from version 3.3.2 to 3.3.3 ...");
                clearAll();
                break;
            case "3.3.3":
                log.info("Clear cache to upgrade from version 3.3.3 to 3.3.4 ...");
                clearAll();
                break;
            case "3.6.1":
                log.info("Clearing cache to upgrade from version 3.6.1 to 3.6.2");
                clearCacheByName(SECURITY_SETTINGS_CACHE);
                clearCacheByName(RESOURCE_INFO_CACHE);
                break;
            default:
                //Do nothing, since cache cleanup is optional.
        }
    }

    void clearAllCaches() {
        cacheManager.getCacheNames().forEach(this::clearCacheByName);
    }

    void clearCacheByName(final String cacheName) {
        log.info("Clearing cache [{}]", cacheName);
        Cache cache = cacheManager.getCache(cacheName);
        Objects.requireNonNull(cache, "Cache does not exist for name " + cacheName);
        cache.clear();
    }

    void clearAll() {
        if (redisTemplate.isPresent()) {
            log.info("Flushing all caches");
            redisTemplate.get().execute((RedisCallback<Object>) connection -> {
                connection.flushAll();
                return null;
            });
            return;
        }
        cacheManager.getCacheNames().forEach(this::clearCacheByName);
    }
}
