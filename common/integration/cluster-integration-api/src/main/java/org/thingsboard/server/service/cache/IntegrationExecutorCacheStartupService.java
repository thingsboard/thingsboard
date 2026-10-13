// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.AssetCacheInfo;
import org.thingsboard.server.common.data.AssetProfileCacheInfo;
import org.thingsboard.server.common.data.DeviceCacheInfo;
import org.thingsboard.server.common.data.DeviceProfileCacheInfo;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.queue.util.TbIntegrationExecutorComponent;
import org.thingsboard.server.service.data.EntityCacheKey;
import org.thingsboard.server.service.integration.IntegrationConfigurationService;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Service responsible for preloading Integration Executor caches on startup.
 *
 * Blocks message processing until:
 * 1. All device and asset profiles are cached
 * 2. Devices/assets with the 'ManagedByIntegration' relation are cached
 *
 * This ensures the Integration Executor can process messages without waiting for tb-core.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@TbIntegrationExecutorComponent
public class IntegrationExecutorCacheStartupService {

    private final IntegrationConfigurationService configurationService;
    private final IntegrationExecutorDeviceProfileCache deviceProfileCache;
    private final IntegrationExecutorAssetProfileCache assetProfileCache;
    private final IntegrationExecutorDeviceCache deviceCache;
    private final IntegrationExecutorAssetCache assetCache;

    private final CountDownLatch cacheReadyLatch = new CountDownLatch(1);

    @Value("${integrations.executor.cache.startup.preload:true}")
    private boolean preload;

    @Value("${integrations.executor.cache.startup.relation_type:ManagedByIntegration}")
    private String relationType;

    @Value("${integrations.executor.cache.startup.retry_attempts:5}")
    private int retryAttempts;

    @Value("${integrations.executor.cache.startup.retry_delay_ms:5000}")
    private long retryDelayMs;

    @Value("${integrations.executor.cache.startup.timeout_sec:300}")
    private long timeoutSec;

    @Value("${integrations.executor.cache.startup.batch_size:1000}")
    private int batchSize;

    /**
     * Preload caches synchronously. Blocks until preloading is complete.
     * Must be called explicitly from DefaultClusterIntegrationService.doStartup()
     * which runs in the IE-startup executor thread.
     */
    public void preloadCaches() {
        log.info("Starting Integration Executor cache preloading...");
        try {
            if (preload) {
                preloadAllProfiles();
                preloadManagedEntities();
            } else {
                log.debug("Cache preloading is disabled");
            }
            log.info("Integration Executor cache preloading completed successfully");
        } catch (Exception e) {
            log.error("Failed to pre-load caches, but continuing startup", e);
        } finally {
            cacheReadyLatch.countDown();
        }
    }

    /**
     * Blocks the calling thread until caches are ready or timeout occurs.
     * Should be called by message processing threads before processing messages.
     */
    public void awaitCacheReady() throws InterruptedException {
        if (!cacheReadyLatch.await(timeoutSec, TimeUnit.SECONDS)) {
            log.warn("Cache preloading timed out after {} seconds, proceeding anyway", timeoutSec);
        }
    }

    private void preloadAllProfiles() {
        log.debug("Preloading all device and asset profiles in batches...");
        preloadEntities("device profile", configurationService::getAllDeviceProfileCacheInfos, deviceProfileCache,
                DeviceProfileCacheInfo::tenantId, DeviceProfileCacheInfo::name, DeviceProfileCacheInfo::getId);
        preloadEntities("asset profile", configurationService::getAllAssetProfileCacheInfos, assetProfileCache,
                AssetProfileCacheInfo::tenantId, AssetProfileCacheInfo::name, AssetProfileCacheInfo::getId);
    }

    private void preloadManagedEntities() {
        log.debug("Preloading devices and assets with '{}' relation...", relationType);
        preloadEntities("managed device", configurationService::getDeviceCacheInfosByRelation, deviceCache,
                DeviceCacheInfo::tenantId, DeviceCacheInfo::name, DeviceCacheInfo::getId);
        preloadEntities("managed asset", configurationService::getAssetCacheInfosByRelation, assetCache,
                AssetCacheInfo::tenantId, AssetCacheInfo::name, AssetCacheInfo::getId);
    }

    private <T extends HasName> void preloadEntities(String entityTypeName, BatchFetcher<T> fetcher, IntegrationExecutorCache<T> cache,
                                                     Function<T, TenantId> tenantIdExtractor, Function<T, String> nameExtractor, Function<T, UUID> idExtractor) {
        int totalCached = 0;
        try {
            UUID lastId = null;
            boolean hasMore = true;
            int batchNum = 0;

            while (hasMore && !Thread.currentThread().isInterrupted()) {
                final UUID currentLastId = lastId;
                List<T> batch = retryOperation(() -> fetcher.fetch(currentLastId, batchSize));

                if (batch.isEmpty()) {
                    hasMore = false;
                } else {
                    log.debug("Fetched {} batch {} with {} entities", entityTypeName, batchNum, batch.size());

                    for (T item : batch) {
                        try {
                            EntityCacheKey key = EntityCacheKey.builder()
                                    .tenantId(tenantIdExtractor.apply(item))
                                    .name(nameExtractor.apply(item))
                                    .build();
                            cache.put(key, item);
                            totalCached++;
                            log.debug("preloaded {}: {}", entityTypeName, nameExtractor.apply(item));
                        } catch (Exception e) {
                            log.warn("Failed to pre-load {}: {}", entityTypeName, nameExtractor.apply(item), e);
                        }
                    }

                    lastId = idExtractor.apply(batch.get(batch.size() - 1));

                    hasMore = batch.size() >= batchSize;
                    if (hasMore) {
                        batchNum++;
                    }
                }
            }
            log.debug("Successfully cached {} {}s in total", totalCached, entityTypeName);
        } catch (Exception e) {
            log.error("Failed to pre-load {}s after {} attempts. Cached {} entities before failure.",
                    entityTypeName, retryAttempts, totalCached, e);
        }
    }

    private <T> T retryOperation(SupplierWithException<T> operation) throws Exception {
        Exception lastException = null;
        for (int attempt = 1; attempt <= retryAttempts; attempt++) {
            try {
                return operation.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw e;
            } catch (Exception e) {
                lastException = e;
                if (attempt < retryAttempts) {
                    log.warn("Attempt {}/{} failed, retrying in {}ms...", attempt, retryAttempts, retryDelayMs);
                    Thread.sleep(retryDelayMs);
                }
            }
        }
        throw lastException != null ? lastException : new IllegalStateException("Retry failed with 0 attempts configured");
    }

    @FunctionalInterface
    private interface SupplierWithException<T> {
        T get() throws Exception;
    }

    @FunctionalInterface
    private interface BatchFetcher<T> {
        List<T> fetch(UUID lastId, int batchSize) throws Exception;
    }

}
