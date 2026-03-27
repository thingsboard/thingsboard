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
package org.thingsboard.server.service.profile;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.msg.plugin.ComponentLifecycleMsg;
import org.thingsboard.server.dao.asset.AssetProfileService;
import org.thingsboard.server.dao.asset.AssetService;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DefaultTbAssetProfileCacheTest {

    @Mock
    private AssetProfileService assetProfileService;
    @Mock
    private AssetService assetService;

    private DefaultTbAssetProfileCache cache;

    @BeforeEach
    public void setUp() {
        cache = new DefaultTbAssetProfileCache(assetProfileService, assetService);
    }

    @Test
    public void onComponentLifecycleEvent_tenantDeleted_evictsAssetProfilesForThatTenant() {
        TenantId tenant1 = new TenantId(UUID.randomUUID());
        TenantId tenant2 = new TenantId(UUID.randomUUID());
        AssetProfileId profileId1 = new AssetProfileId(UUID.randomUUID());
        AssetProfileId profileId2 = new AssetProfileId(UUID.randomUUID());

        loadProfileIntoCache(tenant1, profileId1);
        loadProfileIntoCache(tenant2, profileId2);

        cache.onComponentLifecycleEvent(new ComponentLifecycleMsg(tenant1, tenant1, ComponentLifecycleEvent.DELETED));

        // After deletion tenant1 profile should be reloaded from service on next get
        when(assetProfileService.findAssetProfileById(any(), any())).thenReturn(null);
        assertThat(cache.get(tenant1, profileId1)).isNull();
        verify(assetProfileService, times(1)).findAssetProfileById(tenant2, profileId2);
    }

    @Test
    public void onComponentLifecycleEvent_tenantDeleted_evictsAssetMappingsForThatTenant() {
        TenantId tenant = new TenantId(UUID.randomUUID());
        AssetProfileId profileId = new AssetProfileId(UUID.randomUUID());
        AssetId assetId = new AssetId(UUID.randomUUID());

        loadProfileIntoCache(tenant, profileId);
        loadAssetMappingIntoCache(tenant, assetId, profileId);

        cache.onComponentLifecycleEvent(new ComponentLifecycleMsg(tenant, tenant, ComponentLifecycleEvent.DELETED));

        // After tenant deletion, asset-to-profile mapping should be gone; get() should try to reload
        when(assetService.findAssetById(any(), any())).thenReturn(null);
        assertThat(cache.get(tenant, assetId)).isNull();
        verify(assetService, times(2)).findAssetById(tenant, assetId); // once on load, once after eviction
    }

    @Test
    public void onComponentLifecycleEvent_tenantDeleted_removesListenersForThatTenant() {
        TenantId tenant = new TenantId(UUID.randomUUID());
        EntityId listenerId = new AssetId(UUID.randomUUID());
        AtomicInteger callCount = new AtomicInteger();

        cache.addListener(tenant, listenerId, profile -> callCount.incrementAndGet(), null);

        cache.onComponentLifecycleEvent(new ComponentLifecycleMsg(tenant, tenant, ComponentLifecycleEvent.DELETED));

        // Evicting a profile after tenant deletion should not trigger the removed listener
        AssetProfileId profileId = new AssetProfileId(UUID.randomUUID());
        loadProfileIntoCache(tenant, profileId);
        cache.evict(tenant, profileId);

        assertThat(callCount.get()).isZero();
    }

    @Test
    public void onComponentLifecycleEvent_tenantUpdated_doesNotEvictProfiles() {
        TenantId tenant = new TenantId(UUID.randomUUID());
        AssetProfileId profileId = new AssetProfileId(UUID.randomUUID());
        loadProfileIntoCache(tenant, profileId);

        cache.onComponentLifecycleEvent(new ComponentLifecycleMsg(tenant, tenant, ComponentLifecycleEvent.UPDATED));

        // Profile should still be served from cache without hitting the service again
        cache.get(tenant, profileId);
        verify(assetProfileService, times(1)).findAssetProfileById(tenant, profileId);
    }

    @Test
    public void onComponentLifecycleEvent_differentTenantDeleted_keepsOtherTenantsProfiles() {
        TenantId tenant1 = new TenantId(UUID.randomUUID());
        TenantId tenant2 = new TenantId(UUID.randomUUID());
        AssetProfileId profileId1 = new AssetProfileId(UUID.randomUUID());
        AssetProfileId profileId2 = new AssetProfileId(UUID.randomUUID());

        AssetProfile profile1 = loadProfileIntoCache(tenant1, profileId1);
        loadProfileIntoCache(tenant2, profileId2);

        cache.onComponentLifecycleEvent(new ComponentLifecycleMsg(tenant2, tenant2, ComponentLifecycleEvent.DELETED));

        assertThat(cache.get(tenant1, profileId1)).isEqualTo(profile1);
        verify(assetProfileService, times(1)).findAssetProfileById(tenant1, profileId1);
    }

    // --- Helpers ---

    private AssetProfile loadProfileIntoCache(TenantId tenantId, AssetProfileId profileId) {
        AssetProfile profile = new AssetProfile();
        profile.setId(profileId);
        profile.setTenantId(tenantId);
        when(assetProfileService.findAssetProfileById(tenantId, profileId)).thenReturn(profile);
        cache.get(tenantId, profileId);
        return profile;
    }

    private void loadAssetMappingIntoCache(TenantId tenantId, AssetId assetId, AssetProfileId profileId) {
        Asset asset = new Asset();
        asset.setId(assetId);
        asset.setAssetProfileId(profileId);
        when(assetService.findAssetById(tenantId, assetId)).thenReturn(asset);
        cache.get(tenantId, assetId);
    }

}
