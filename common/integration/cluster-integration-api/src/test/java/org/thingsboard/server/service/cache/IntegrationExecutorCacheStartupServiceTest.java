// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.common.data.AssetCacheInfo;
import org.thingsboard.server.common.data.AssetProfileCacheInfo;
import org.thingsboard.server.common.data.DeviceCacheInfo;
import org.thingsboard.server.common.data.DeviceProfileCacheInfo;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.service.data.EntityCacheKey;
import org.thingsboard.server.service.integration.IntegrationConfigurationService;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IntegrationExecutorCacheStartupServiceTest {

    @Mock
    private IntegrationConfigurationService configurationService;
    @Mock
    private IntegrationExecutorDeviceProfileCache deviceProfileCache;
    @Mock
    private IntegrationExecutorAssetProfileCache assetProfileCache;
    @Mock
    private IntegrationExecutorDeviceCache deviceCache;
    @Mock
    private IntegrationExecutorAssetCache assetCache;

    private IntegrationExecutorCacheStartupService startupService;

    @BeforeEach
    void setUp() {
        startupService = new IntegrationExecutorCacheStartupService(configurationService, deviceProfileCache, assetProfileCache, deviceCache, assetCache);
        ReflectionTestUtils.setField(startupService, "preload", true);
        ReflectionTestUtils.setField(startupService, "relationType", "ManagedByIntegration");
        ReflectionTestUtils.setField(startupService, "retryAttempts", 3);
        ReflectionTestUtils.setField(startupService, "retryDelayMs", 10L);
        ReflectionTestUtils.setField(startupService, "timeoutSec", 5L);
        ReflectionTestUtils.setField(startupService, "batchSize", 2);
    }

    @Test
    void preloadCaches_shouldLoadAllProfilesAndEntities() {
        TenantId tenantId = TenantId.fromUUID(UUID.randomUUID());
        UUID profileId = UUID.randomUUID();

        DeviceProfileCacheInfo deviceProfile = new DeviceProfileCacheInfo(profileId, tenantId.getId(), "defaultProfile", null, null);
        AssetProfileCacheInfo assetProfile = new AssetProfileCacheInfo(UUID.randomUUID(), tenantId.getId(), "assetProfile", null, null);
        DeviceCacheInfo device = new DeviceCacheInfo(UUID.randomUUID(), tenantId, null, "device1", "default", new DeviceProfileId(profileId));
        AssetCacheInfo asset = new AssetCacheInfo(UUID.randomUUID(), tenantId, null, "asset1", "default", null);

        when(configurationService.getAllDeviceProfileCacheInfos(null, 2)).thenReturn(List.of(deviceProfile));
        when(configurationService.getAllAssetProfileCacheInfos(null, 2)).thenReturn(List.of(assetProfile));
        when(configurationService.getDeviceCacheInfosByRelation(null, 2)).thenReturn(List.of(device));
        when(configurationService.getAssetCacheInfosByRelation(null, 2)).thenReturn(List.of(asset));

        startupService.preloadCaches();

        verify(deviceProfileCache).put(any(EntityCacheKey.class), eq(deviceProfile));
        verify(assetProfileCache).put(any(EntityCacheKey.class), eq(assetProfile));
        verify(deviceCache).put(any(EntityCacheKey.class), eq(device));
        verify(assetCache).put(any(EntityCacheKey.class), eq(asset));
    }

    @Test
    void preloadCaches_withBatchPagination_shouldFetchMultipleBatches() {
        TenantId tenantId = TenantId.fromUUID(UUID.randomUUID());
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        UUID id3 = UUID.randomUUID();

        DeviceProfileCacheInfo p1 = new DeviceProfileCacheInfo(id1, tenantId.getId(), "p1", null, null);
        DeviceProfileCacheInfo p2 = new DeviceProfileCacheInfo(id2, tenantId.getId(), "p2", null, null);
        DeviceProfileCacheInfo p3 = new DeviceProfileCacheInfo(id3, tenantId.getId(), "p3", null, null);

        when(configurationService.getAllDeviceProfileCacheInfos(null, 2)).thenReturn(List.of(p1, p2));
        when(configurationService.getAllDeviceProfileCacheInfos(id2, 2)).thenReturn(List.of(p3));

        when(configurationService.getAllAssetProfileCacheInfos(any(), anyInt())).thenReturn(Collections.emptyList());
        when(configurationService.getDeviceCacheInfosByRelation(any(), anyInt())).thenReturn(Collections.emptyList());
        when(configurationService.getAssetCacheInfosByRelation(any(), anyInt())).thenReturn(Collections.emptyList());

        startupService.preloadCaches();

        verify(deviceProfileCache).put(any(EntityCacheKey.class), eq(p1));
        verify(deviceProfileCache).put(any(EntityCacheKey.class), eq(p2));
        verify(deviceProfileCache).put(any(EntityCacheKey.class), eq(p3));
        verify(configurationService, times(2)).getAllDeviceProfileCacheInfos(any(), eq(2));
    }

    @Test
    void preloadCaches_retryLogic_shouldRetryOnFailureAndSucceed() {
        when(configurationService.getAllDeviceProfileCacheInfos(null, 2))
                .thenThrow(new RuntimeException("transient error"))
                .thenThrow(new RuntimeException("transient error again"))
                .thenReturn(Collections.emptyList());

        when(configurationService.getAllAssetProfileCacheInfos(any(), anyInt())).thenReturn(Collections.emptyList());
        when(configurationService.getDeviceCacheInfosByRelation(any(), anyInt())).thenReturn(Collections.emptyList());
        when(configurationService.getAssetCacheInfosByRelation(any(), anyInt())).thenReturn(Collections.emptyList());

        assertDoesNotThrow(() -> startupService.preloadCaches());

        verify(configurationService, times(3)).getAllDeviceProfileCacheInfos(null, 2);
    }

    @Test
    void preloadCaches_retryExhausted_shouldNotCrash() {
        when(configurationService.getAllDeviceProfileCacheInfos(null, 2))
                .thenThrow(new RuntimeException("persistent error"));

        assertDoesNotThrow(() -> startupService.preloadCaches());

        verify(configurationService, times(3)).getAllDeviceProfileCacheInfos(null, 2);
    }

    @Test
    void preloadCaches_disabled_shouldSkipLoading() {
        ReflectionTestUtils.setField(startupService, "preload", false);

        startupService.preloadCaches();

        verify(configurationService, never()).getAllDeviceProfileCacheInfos(any(), anyInt());
        verify(configurationService, never()).getDeviceCacheInfosByRelation(any(), anyInt());
    }

    @Test
    void awaitCacheReady_shouldBlockUntilPreloadCompletes() throws Exception {
        CountDownLatch awaitStarted = new CountDownLatch(1);
        boolean[] awaitResult = {false};

        when(configurationService.getAllDeviceProfileCacheInfos(any(), anyInt())).thenReturn(Collections.emptyList());
        when(configurationService.getAllAssetProfileCacheInfos(any(), anyInt())).thenReturn(Collections.emptyList());
        when(configurationService.getDeviceCacheInfosByRelation(any(), anyInt())).thenReturn(Collections.emptyList());
        when(configurationService.getAssetCacheInfosByRelation(any(), anyInt())).thenReturn(Collections.emptyList());

        Thread awaiter = new Thread(() -> {
            try {
                awaitStarted.countDown();
                startupService.awaitCacheReady();
                awaitResult[0] = true;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        awaiter.start();

        assertTrue(awaitStarted.await(5, TimeUnit.SECONDS));
        Thread.sleep(50);

        startupService.preloadCaches();

        awaiter.join(5000);
        assertTrue(awaitResult[0], "awaitCacheReady should have returned after preloadCaches completed");
    }

    @Test
    void awaitCacheReady_afterPreloadAlreadyDone_shouldReturnImmediately() {
        when(configurationService.getAllDeviceProfileCacheInfos(any(), anyInt())).thenReturn(Collections.emptyList());
        when(configurationService.getAllAssetProfileCacheInfos(any(), anyInt())).thenReturn(Collections.emptyList());
        when(configurationService.getDeviceCacheInfosByRelation(any(), anyInt())).thenReturn(Collections.emptyList());
        when(configurationService.getAssetCacheInfosByRelation(any(), anyInt())).thenReturn(Collections.emptyList());

        startupService.preloadCaches();

        assertDoesNotThrow(() -> startupService.awaitCacheReady());
    }

    @Test
    void awaitCacheReady_timeout_shouldNotBlock() {
        ReflectionTestUtils.setField(startupService, "timeoutSec", 1L);

        long start = System.currentTimeMillis();
        assertDoesNotThrow(() -> startupService.awaitCacheReady());
        long elapsed = System.currentTimeMillis() - start;

        assertTrue(elapsed >= 900 && elapsed < 5000, "awaitCacheReady should timeout after ~1 second, took " + elapsed + "ms");
    }

    @Test
    void preloadCaches_partialPutFailure_shouldContinueWithRemainingItems() {
        TenantId tenantId = TenantId.fromUUID(UUID.randomUUID());
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        DeviceProfileCacheInfo p1 = new DeviceProfileCacheInfo(id1, tenantId.getId(), "p1", null, null);
        DeviceProfileCacheInfo p2 = new DeviceProfileCacheInfo(id2, tenantId.getId(), "p2", null, null);

        when(configurationService.getAllDeviceProfileCacheInfos(null, 2)).thenReturn(List.of(p1, p2));
        when(configurationService.getAllAssetProfileCacheInfos(any(), anyInt())).thenReturn(Collections.emptyList());
        when(configurationService.getDeviceCacheInfosByRelation(any(), anyInt())).thenReturn(Collections.emptyList());
        when(configurationService.getAssetCacheInfosByRelation(any(), anyInt())).thenReturn(Collections.emptyList());

        Mockito.doThrow(new RuntimeException("put failed"))
                .doNothing()
                .when(deviceProfileCache).put(any(EntityCacheKey.class), any());

        assertDoesNotThrow(() -> startupService.preloadCaches());

        verify(deviceProfileCache, times(2)).put(any(EntityCacheKey.class), any());
    }

}
