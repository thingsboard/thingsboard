// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.cache;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.common.data.DeviceCacheInfo;
import org.thingsboard.server.common.data.DeviceProfileCacheInfo;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.gen.integration.IntegrationInfoProto;
import org.thingsboard.server.service.data.EntityCacheKey;
import org.thingsboard.server.service.data.EntityUplinkData;

import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AbstractIntegrationExecutorCacheTest {

    private static final TenantId TENANT_ID = TenantId.fromUUID(UUID.randomUUID());

    private TestEntityCache entityCache;
    private TestProfileCache profileCache;

    @BeforeEach
    void setUp() {
        entityCache = new TestEntityCache();
        ReflectionTestUtils.setField(entityCache, "cacheTtlMs", 1000L);
        ReflectionTestUtils.setField(entityCache, "cleanupIntervalMs", 60000L);
        entityCache.init();

        profileCache = new TestProfileCache();
        ReflectionTestUtils.setField(profileCache, "cacheTtlMs", 86400000L);
        ReflectionTestUtils.setField(profileCache, "cleanupIntervalMs", 3600000L);
        profileCache.init();
    }

    @AfterEach
    void tearDown() {
        entityCache.destroy();
        profileCache.destroy();
    }

    @Test
    void putAndGetEntity_shouldReturnCachedEntity() throws Exception {
        DeviceCacheInfo device = createDevice("device1");
        EntityCacheKey key = EntityCacheKey.builder().tenantId(TENANT_ID).name("device1").build();

        entityCache.put(key, device);

        IntegrationInfoProto proto = buildProto(TENANT_ID);
        EntityUplinkData uplinkData = EntityUplinkData.builder().name("device1").type("default").build();

        DeviceCacheInfo result = entityCache.getEntity(proto, uplinkData).get(5, TimeUnit.SECONDS);
        assertSame(device, result);
    }

    @Test
    void getEntity_cacheMiss_shouldFetchAndCache() throws Exception {
        DeviceCacheInfo device = createDevice("device2");
        entityCache.setFetchResult(Futures.immediateFuture(device));

        IntegrationInfoProto proto = buildProto(TENANT_ID);
        EntityUplinkData uplinkData = EntityUplinkData.builder().name("device2").type("default").build();

        DeviceCacheInfo result = entityCache.getEntity(proto, uplinkData).get(5, TimeUnit.SECONDS);
        assertSame(device, result);

        entityCache.setFetchResult(Futures.immediateFailedFuture(new RuntimeException("should not be called")));
        DeviceCacheInfo cached = entityCache.getEntity(proto, uplinkData).get(5, TimeUnit.SECONDS);
        assertSame(device, cached);
    }

    @Test
    void getEntity_concurrentFetch_shouldDeduplicateRequests() throws Exception {
        SettableFuture<DeviceCacheInfo> delayedFuture = SettableFuture.create();
        entityCache.setFetchResult(delayedFuture);

        IntegrationInfoProto proto = buildProto(TENANT_ID);
        EntityUplinkData uplinkData = EntityUplinkData.builder().name("device3").type("default").build();

        ListenableFuture<DeviceCacheInfo> future1 = entityCache.getEntity(proto, uplinkData);
        ListenableFuture<DeviceCacheInfo> future2 = entityCache.getEntity(proto, uplinkData);

        DeviceCacheInfo device = createDevice("device3");
        delayedFuture.set(device);

        assertSame(device, future1.get(5, TimeUnit.SECONDS));
        assertSame(device, future2.get(5, TimeUnit.SECONDS));
    }

    @Test
    void getEntity_fetchFailure_shouldPropagateException() {
        RuntimeException error = new RuntimeException("fetch failed");
        entityCache.setFetchResult(Futures.immediateFailedFuture(error));

        IntegrationInfoProto proto = buildProto(TENANT_ID);
        EntityUplinkData uplinkData = EntityUplinkData.builder().name("device-fail").type("default").build();

        ListenableFuture<DeviceCacheInfo> future = entityCache.getEntity(proto, uplinkData);
        ExecutionException ex = assertThrows(ExecutionException.class, () -> future.get(5, TimeUnit.SECONDS));
        assertSame(error, ex.getCause());
    }

    @Test
    void evict_entityCache_shouldRemoveById() throws Exception {
        DeviceCacheInfo device = createDevice("device4");
        EntityCacheKey key = EntityCacheKey.builder().tenantId(TENANT_ID).name("device4").build();

        entityCache.put(key, device);

        IntegrationInfoProto proto = buildProto(TENANT_ID);
        EntityUplinkData uplinkData = EntityUplinkData.builder().name("device4").type("default").build();
        assertNotNull(entityCache.getEntity(proto, uplinkData).get(5, TimeUnit.SECONDS));

        entityCache.evict(device.getId());

        entityCache.setFetchResult(Futures.immediateFuture(null));
        DeviceCacheInfo result = entityCache.getEntity(proto, uplinkData).get(5, TimeUnit.SECONDS);
        assertNull(result);
    }

    @Test
    void ttlEviction_shouldRemoveExpiredEntries() throws Exception {
        DeviceCacheInfo device = createDevice("device-ttl");
        EntityCacheKey key = EntityCacheKey.builder().tenantId(TENANT_ID).name("device-ttl").build();

        entityCache.put(key, device);

        ReflectionTestUtils.setField(entityCache, "cacheTtlMs", 1L);
        Thread.sleep(50);

        ReflectionTestUtils.invokeMethod(entityCache, "cleanupExpiredEntries");

        entityCache.setFetchResult(Futures.immediateFuture(null));
        IntegrationInfoProto proto = buildProto(TENANT_ID);
        EntityUplinkData uplinkData = EntityUplinkData.builder().name("device-ttl").type("default").build();
        DeviceCacheInfo result = entityCache.getEntity(proto, uplinkData).get(5, TimeUnit.SECONDS);
        assertNull(result);
    }

    @Test
    void profileCache_putAndGetByUUID() throws Exception {
        DeviceProfileCacheInfo profile = createProfile("profile1");
        EntityCacheKey key = EntityCacheKey.builder().tenantId(TENANT_ID).name("profile1").build();

        profileCache.put(key, profile);

        DeviceProfileCacheInfo result = profileCache.getProfile(profile.getId(), key).get(5, TimeUnit.SECONDS);
        assertSame(profile, result);
    }

    @Test
    void profileCache_getByKey_whenNotInProfilesById() throws Exception {
        DeviceProfileCacheInfo profile = createProfile("profile2");
        EntityCacheKey key = EntityCacheKey.builder().tenantId(TENANT_ID).name("profile2").build();

        profileCache.put(key, profile);

        UUID differentId = UUID.randomUUID();
        DeviceProfileCacheInfo result = profileCache.getProfile(differentId, key).get(5, TimeUnit.SECONDS);
        assertSame(profile, result);
    }

    @Test
    void profileCache_evict_shouldRemoveFromBothMaps() throws Exception {
        DeviceProfileCacheInfo profile = createProfile("profile3");
        EntityCacheKey key = EntityCacheKey.builder().tenantId(TENANT_ID).name("profile3").build();

        profileCache.put(key, profile);

        assertNotNull(profileCache.getProfile(profile.getId(), key).get(5, TimeUnit.SECONDS));

        profileCache.evict(profile.getId());

        profileCache.setFetchResult(Futures.immediateFuture(null));
        DeviceProfileCacheInfo result = profileCache.getProfile(profile.getId(), key).get(5, TimeUnit.SECONDS);
        assertNull(result);
    }

    @Test
    void entityCache_getProfile_withNullProfilesById_shouldNotNPE() throws Exception {
        EntityCacheKey key = EntityCacheKey.builder().tenantId(TENANT_ID).name("test").build();

        entityCache.setFetchResult(Futures.immediateFuture(null));
        DeviceCacheInfo result = entityCache.getProfile(UUID.randomUUID(), key).get(5, TimeUnit.SECONDS);
        assertNull(result);
    }

    @Test
    void profileCache_ttlEviction_shouldRemoveExpiredProfiles() throws Exception {
        DeviceProfileCacheInfo profile = createProfile("profile-ttl");
        EntityCacheKey key = EntityCacheKey.builder().tenantId(TENANT_ID).name("profile-ttl").build();

        profileCache.put(key, profile);

        ReflectionTestUtils.setField(profileCache, "cacheTtlMs", 1L);
        Thread.sleep(50);

        ReflectionTestUtils.invokeMethod(profileCache, "cleanupExpiredEntries");

        profileCache.setFetchResult(Futures.immediateFuture(null));
        DeviceProfileCacheInfo result = profileCache.getProfile(profile.getId(), key).get(5, TimeUnit.SECONDS);
        assertNull(result);
    }

    @Test
    void putNull_shouldBeNoOp() throws Exception {
        EntityCacheKey key = EntityCacheKey.builder().tenantId(TENANT_ID).name("null-device").build();

        entityCache.put(key, null);

        entityCache.setFetchResult(Futures.immediateFuture(null));
        IntegrationInfoProto proto = buildProto(TENANT_ID);
        EntityUplinkData uplinkData = EntityUplinkData.builder().name("null-device").type("default").build();
        DeviceCacheInfo result = entityCache.getEntity(proto, uplinkData).get(5, TimeUnit.SECONDS);
        assertNull(result);
    }

    @Test
    void fetchReturningNull_shouldNotCacheAndShouldRefetchNextTime() throws Exception {
        entityCache.setFetchResult(Futures.immediateFuture(null));

        IntegrationInfoProto proto = buildProto(TENANT_ID);
        EntityUplinkData uplinkData = EntityUplinkData.builder().name("missing-device").type("default").build();

        DeviceCacheInfo result1 = entityCache.getEntity(proto, uplinkData).get(5, TimeUnit.SECONDS);
        assertNull(result1);

        DeviceCacheInfo device = createDevice("missing-device");
        entityCache.setFetchResult(Futures.immediateFuture(device));
        DeviceCacheInfo result2 = entityCache.getEntity(proto, uplinkData).get(5, TimeUnit.SECONDS);
        assertSame(device, result2);
    }

    @Test
    void evictThenPut_shouldAllowRepopulation() throws Exception {
        DeviceCacheInfo device = createDevice("device-repop");
        EntityCacheKey key = EntityCacheKey.builder().tenantId(TENANT_ID).name("device-repop").build();

        entityCache.put(key, device);
        entityCache.evict(device.getId());

        DeviceCacheInfo newDevice = createDevice("device-repop");
        entityCache.put(key, newDevice);

        IntegrationInfoProto proto = buildProto(TENANT_ID);
        EntityUplinkData uplinkData = EntityUplinkData.builder().name("device-repop").type("default").build();
        DeviceCacheInfo result = entityCache.getEntity(proto, uplinkData).get(5, TimeUnit.SECONDS);
        assertSame(newDevice, result);
    }

    @Test
    @SuppressWarnings("unchecked")
    void cleanupStuckFutures_shouldRemoveExpiredFutures() {
        SettableFuture<DeviceCacheInfo> stuckFuture = SettableFuture.create();
        entityCache.setFetchResult(stuckFuture);

        IntegrationInfoProto proto = buildProto(TENANT_ID);
        EntityUplinkData uplinkData = EntityUplinkData.builder().name("stuck-device").type("default").build();

        ListenableFuture<DeviceCacheInfo> future = entityCache.getEntity(proto, uplinkData);

        ConcurrentMap<EntityCacheKey, ?> futures = (ConcurrentMap<EntityCacheKey, ?>) ReflectionTestUtils.getField(entityCache, "futures");
        assertNotNull(futures);

        EntityCacheKey key = EntityCacheKey.builder().tenantId(TENANT_ID).name("stuck-device").build();
        Object futureEntry = futures.get(key);
        assertNotNull(futureEntry);

        // Simulate stuck future by backdating createdAt past the 60s timeout
        ReflectionTestUtils.setField(futureEntry, "createdAt", System.currentTimeMillis() - 120_000L);

        ReflectionTestUtils.invokeMethod(entityCache, "cleanupStuckFutures");

        ExecutionException ex = assertThrows(ExecutionException.class, () -> future.get(1, TimeUnit.SECONDS));
        assertThat(ex.getCause()).isInstanceOf(RuntimeException.class);
        assertThat(ex.getCause().getMessage()).contains("Future timed out");
        assertTrue(futures.isEmpty());
    }

    @Test
    void tenantIsolation_shouldNotReturnCrosstenantData() throws Exception {
        TenantId tenant1 = TenantId.fromUUID(UUID.randomUUID());
        TenantId tenant2 = TenantId.fromUUID(UUID.randomUUID());

        DeviceCacheInfo device1 = createDevice("shared-name");
        DeviceCacheInfo device2 = createDevice("shared-name");

        entityCache.put(EntityCacheKey.builder().tenantId(tenant1).name("shared-name").build(), device1);
        entityCache.put(EntityCacheKey.builder().tenantId(tenant2).name("shared-name").build(), device2);

        IntegrationInfoProto proto1 = buildProto(tenant1);
        IntegrationInfoProto proto2 = buildProto(tenant2);
        EntityUplinkData uplinkData = EntityUplinkData.builder().name("shared-name").type("default").build();

        DeviceCacheInfo result1 = entityCache.getEntity(proto1, uplinkData).get(5, TimeUnit.SECONDS);
        DeviceCacheInfo result2 = entityCache.getEntity(proto2, uplinkData).get(5, TimeUnit.SECONDS);

        assertSame(device1, result1);
        assertSame(device2, result2);
    }

    private DeviceCacheInfo createDevice(String name) {
        return new DeviceCacheInfo(
                UUID.randomUUID(), TENANT_ID,
                null, name, "default",
                new DeviceProfileId(UUID.randomUUID())
        );
    }

    private DeviceProfileCacheInfo createProfile(String name) {
        return new DeviceProfileCacheInfo(
                UUID.randomUUID(), TENANT_ID.getId(),
                name, null, null
        );
    }

    private IntegrationInfoProto buildProto(TenantId tenantId) {
        return IntegrationInfoProto.newBuilder()
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .build();
    }

    private static class TestEntityCache extends AbstractIntegrationExecutorCache<DeviceCacheInfo> {

        private ListenableFuture<DeviceCacheInfo> fetchResult = Futures.immediateFuture(null);

        void setFetchResult(ListenableFuture<DeviceCacheInfo> result) {
            this.fetchResult = result;
        }

        @Override
        protected ListenableFuture<DeviceCacheInfo> fetchEntity(TenantId tenantId, IntegrationInfoProto proto, EntityUplinkData entityUplinkData) {
            return fetchResult;
        }

    }

    private static class TestProfileCache extends AbstractIntegrationExecutorCache<DeviceProfileCacheInfo> {

        private ListenableFuture<DeviceProfileCacheInfo> fetchResult = Futures.immediateFuture(null);

        void setFetchResult(ListenableFuture<DeviceProfileCacheInfo> result) {
            this.fetchResult = result;
        }

        @Override
        protected ListenableFuture<DeviceProfileCacheInfo> fetchProfile(EntityCacheKey key) {
            return fetchResult;
        }

        @Override
        protected boolean isProfileCache() {
            return true;
        }

    }

}
