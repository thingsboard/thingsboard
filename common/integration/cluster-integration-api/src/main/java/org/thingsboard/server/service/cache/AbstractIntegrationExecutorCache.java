// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.cache;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.id.HasUUID;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.gen.integration.IntegrationInfoProto;
import org.thingsboard.server.service.data.EntityCacheKey;
import org.thingsboard.server.service.data.EntityUplinkData;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
public abstract class AbstractIntegrationExecutorCache<E extends HasUUID & HasName> implements IntegrationExecutorCache<E> {

    private static final long FUTURE_TIMEOUT_MS = 60_000;

    @Value("${integrations.executor.cache.ttl_ms:86400000}")
    private long cacheTtlMs;

    @Value("${integrations.executor.cache.cleanup_interval_ms:3600000}")
    private long cleanupIntervalMs;

    private final ConcurrentMap<EntityCacheKey, CachedEntity<E>> entities = new ConcurrentHashMap<>();
    private final ConcurrentMap<EntityCacheKey, FutureEntry<E>> futures = new ConcurrentHashMap<>();

    private ConcurrentMap<UUID, CachedEntity<E>> profilesById;

    private ScheduledExecutorService cleanupExecutor;

    @PostConstruct
    public void init() {
        if (isProfileCache()) {
            profilesById = new ConcurrentHashMap<>();
        }
        cleanupExecutor = Executors.newSingleThreadScheduledExecutor(ThingsBoardThreadFactory.forName("integration-executor-cache-cleanup-%d"));
        cleanupExecutor.scheduleAtFixedRate(this::cleanupStuckFutures, FUTURE_TIMEOUT_MS, FUTURE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        cleanupExecutor.scheduleAtFixedRate(this::cleanupExpiredEntries, cleanupIntervalMs, cleanupIntervalMs, TimeUnit.MILLISECONDS);
        log.info("Initialized {}{} with TTL: {}ms, cleanup interval: {}ms", this.getClass().getSimpleName(),
                isProfileCache() ? " (profile cache)" : "", cacheTtlMs, cleanupIntervalMs);
    }

    @PreDestroy
    public void destroy() {
        if (cleanupExecutor != null) {
            cleanupExecutor.shutdown();
        }
    }

    private void cleanupExpiredEntries() {
        try {
            int removedCount = 0;
            for (Map.Entry<EntityCacheKey, CachedEntity<E>> entry : entities.entrySet()) {
                CachedEntity<E> cachedEntity = entry.getValue();
                if (cachedEntity.isExpired(cacheTtlMs)) {
                    if (entities.remove(entry.getKey(), cachedEntity)) {
                        if (profilesById != null) {
                            E entity = cachedEntity.getEntity();
                            if (entity != null) {
                                profilesById.remove(entity.getId());
                            }
                        }
                        removedCount++;
                    }
                }
            }
            if (removedCount > 0) {
                log.debug("Cleaned up {} expired entries from {}", removedCount, this.getClass().getSimpleName());
            }
        } catch (Exception e) {
            log.error("Failed to cleanup expired entries from {}", this.getClass().getSimpleName(), e);
        }
    }

    private void cleanupStuckFutures() {
        try {
            int stuckFuturesRemoved = 0;
            for (Map.Entry<EntityCacheKey, FutureEntry<E>> entry : futures.entrySet()) {
                FutureEntry<E> futureEntry = entry.getValue();
                if (futureEntry.isExpired()) {
                    if (futures.remove(entry.getKey(), futureEntry)) {
                        futureEntry.future.setException(new RuntimeException("Future timed out for key: " + entry.getKey()));
                        stuckFuturesRemoved++;
                    }
                }
            }
            if (stuckFuturesRemoved > 0) {
                log.warn("Cleaned up {} stuck futures from {}", stuckFuturesRemoved, this.getClass().getSimpleName());
            }
        } catch (Exception e) {
            log.error("Failed to cleanup stuck futures from {}", this.getClass().getSimpleName(), e);
        }
    }

    @Override
    public ListenableFuture<E> getProfile(UUID id, EntityCacheKey key) {
        if (profilesById != null) {
            CachedEntity<E> cachedEntity = profilesById.get(id);
            if (cachedEntity != null) {
                cachedEntity.updateAccessTime();
                return Futures.immediateFuture(cachedEntity.getEntity());
            }
        }
        CachedEntity<E> cachedEntity = entities.get(key);
        if (cachedEntity != null) {
            cachedEntity.updateAccessTime();
            return Futures.immediateFuture(cachedEntity.getEntity());
        }
        return findOrCreateEntityFuture(key, null, null);
    }

    @Override
    public ListenableFuture<E> getEntity(IntegrationInfoProto proto, EntityUplinkData entityUplinkData) {
        TenantId tenantId = TenantId.fromUUID(new UUID(proto.getTenantIdMSB(), proto.getTenantIdLSB()));
        EntityCacheKey key = EntityCacheKey.builder().tenantId(tenantId).name(entityUplinkData.getName()).build();
        CachedEntity<E> cachedEntity = entities.get(key);
        if (cachedEntity == null) {
            return findOrCreateEntityFuture(key, proto, entityUplinkData);
        } else {
            cachedEntity.updateAccessTime();
            return Futures.immediateFuture(cachedEntity.getEntity());
        }
    }

    protected ListenableFuture<E> findOrCreateEntityFuture(EntityCacheKey key, IntegrationInfoProto proto, EntityUplinkData entityUplinkData) {
        final SettableFuture<E> futureToSet = SettableFuture.create();
        FutureEntry<E> newEntry = new FutureEntry<>(futureToSet);
        var existingEntry = futures.putIfAbsent(key, newEntry);
        if (existingEntry != null) {
            return existingEntry.future;
        }
        try {
            var future = isProfileCache() ? fetchProfile(key) : fetchEntity(key.tenantId(), proto, entityUplinkData);
            Futures.addCallback(future, new FutureCallback<>() {
                @Override
                public void onSuccess(E result) {
                    if (result != null) {
                        CachedEntity<E> cachedEntity = new CachedEntity<>(result);
                        entities.put(key, cachedEntity);
                        if (profilesById != null) {
                            profilesById.put(result.getId(), cachedEntity);
                        }
                    }
                    futures.remove(key);
                    futureToSet.set(result);
                }

                @Override
                public void onFailure(Throwable t) {
                    futures.remove(key);
                    futureToSet.setException(t);
                }
            }, MoreExecutors.directExecutor());
            return futureToSet;
        } catch (Throwable e) {
            log.error("Failed to get entity {}", key, e);
            futures.remove(key);
            futureToSet.setException(e);
            return futureToSet;
        }
    }

    @Override
    public void put(EntityCacheKey key, E entity) {
        if (entity == null) {
            return;
        }
        CachedEntity<E> cachedEntity = new CachedEntity<>(entity);
        entities.put(key, cachedEntity);
        if (profilesById != null) {
            profilesById.put(entity.getId(), cachedEntity);
        }
    }

    @Override
    public void evict(UUID id) {
        if (profilesById != null) {
            profilesById.remove(id);
        }
        // O(n) scan — a reverse-lookup map (UUID → EntityCacheKey) would make this O(1), but would double memory for entity caches that can hold 400k+ entries.
        // Eviction is rare (only on entity deleted from tb-core), so the trade-off favors lower memory.
        entities.values().removeIf(cachedEntity -> {
            E entity = cachedEntity.getEntity();
            return entity != null && id.equals(entity.getId());
        });
    }

    protected ListenableFuture<E> fetchProfile(EntityCacheKey key) {
        return Futures.immediateFuture(null);
    }

    protected ListenableFuture<E> fetchEntity(TenantId tenantId, IntegrationInfoProto proto, EntityUplinkData entityUplinkData) {
        return Futures.immediateFuture(null);
    }

    protected boolean isProfileCache() {
        return false;
    }

    @Getter
    private static class CachedEntity<E> {

        private final E entity;
        private final AtomicLong lastAccessTime;

        public CachedEntity(E entity) {
            this.entity = entity;
            this.lastAccessTime = new AtomicLong(System.currentTimeMillis());
        }

        public void updateAccessTime() {
            lastAccessTime.set(System.currentTimeMillis());
        }

        public boolean isExpired(long ttlMs) {
            return System.currentTimeMillis() - lastAccessTime.get() > ttlMs;
        }

    }

    private static class FutureEntry<E> {

        private final SettableFuture<E> future;
        private final long createdAt;

        FutureEntry(SettableFuture<E> future) {
            this.future = future;
            this.createdAt = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - createdAt > AbstractIntegrationExecutorCache.FUTURE_TIMEOUT_MS;
        }

    }

}
