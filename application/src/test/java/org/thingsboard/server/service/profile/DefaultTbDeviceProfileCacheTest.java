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
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.msg.plugin.ComponentLifecycleMsg;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.dao.device.DeviceService;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DefaultTbDeviceProfileCacheTest {

    @Mock
    private DeviceProfileService deviceProfileService;
    @Mock
    private DeviceService deviceService;

    private DefaultTbDeviceProfileCache cache;

    @BeforeEach
    public void setUp() {
        cache = new DefaultTbDeviceProfileCache(deviceProfileService, deviceService);
    }

    @Test
    public void onComponentLifecycleEvent_tenantDeleted_evictsDeviceProfilesForThatTenant() {
        TenantId tenant1 = new TenantId(UUID.randomUUID());
        TenantId tenant2 = new TenantId(UUID.randomUUID());
        DeviceProfileId profileId1 = new DeviceProfileId(UUID.randomUUID());
        DeviceProfileId profileId2 = new DeviceProfileId(UUID.randomUUID());

        loadProfileIntoCache(tenant1, profileId1);
        loadProfileIntoCache(tenant2, profileId2);

        cache.onComponentLifecycleEvent(new ComponentLifecycleMsg(tenant1, tenant1, ComponentLifecycleEvent.DELETED));

        // After deletion tenant1 profile should be reloaded from service on next get
        when(deviceProfileService.findDeviceProfileById(any(), any())).thenReturn(null);
        assertThat(cache.get(tenant1, profileId1)).isNull();
        // tenant2 profile should still be served from cache (no extra service call)
        verify(deviceProfileService, times(1)).findDeviceProfileById(tenant2, profileId2);
    }

    @Test
    public void onComponentLifecycleEvent_tenantDeleted_evictsDeviceMappingsForThatTenant() {
        TenantId tenant = new TenantId(UUID.randomUUID());
        DeviceProfileId profileId = new DeviceProfileId(UUID.randomUUID());
        DeviceId deviceId = new DeviceId(UUID.randomUUID());

        loadProfileIntoCache(tenant, profileId);
        loadDeviceMappingIntoCache(tenant, deviceId, profileId);

        cache.onComponentLifecycleEvent(new ComponentLifecycleMsg(tenant, tenant, ComponentLifecycleEvent.DELETED));

        // After tenant deletion, device-to-profile mapping should be gone; get() should try to reload
        when(deviceService.findDeviceById(any(), any())).thenReturn(null);
        assertThat(cache.get(tenant, deviceId)).isNull();
        verify(deviceService, times(2)).findDeviceById(tenant, deviceId); // once on load, once after eviction
    }

    @Test
    public void onComponentLifecycleEvent_tenantDeleted_removesListenersForThatTenant() {
        TenantId tenant = new TenantId(UUID.randomUUID());
        EntityId listenerId = new DeviceId(UUID.randomUUID());
        AtomicInteger callCount = new AtomicInteger();

        cache.addListener(tenant, listenerId, profile -> callCount.incrementAndGet(), null);

        cache.onComponentLifecycleEvent(new ComponentLifecycleMsg(tenant, tenant, ComponentLifecycleEvent.DELETED));

        // Evicting a profile after tenant deletion should not trigger the removed listener
        DeviceProfileId profileId = new DeviceProfileId(UUID.randomUUID());
        loadProfileIntoCache(tenant, profileId);
        cache.evict(tenant, profileId);

        assertThat(callCount.get()).isZero();
    }

    @Test
    public void onComponentLifecycleEvent_tenantUpdated_doesNotEvictProfiles() {
        TenantId tenant = new TenantId(UUID.randomUUID());
        DeviceProfileId profileId = new DeviceProfileId(UUID.randomUUID());
        loadProfileIntoCache(tenant, profileId);

        cache.onComponentLifecycleEvent(new ComponentLifecycleMsg(tenant, tenant, ComponentLifecycleEvent.UPDATED));

        // Profile should still be served from cache without hitting the service again
        cache.get(tenant, profileId);
        verify(deviceProfileService, times(1)).findDeviceProfileById(tenant, profileId);
    }

    @Test
    public void onComponentLifecycleEvent_differentTenantDeleted_keepsOtherTenantsProfiles() {
        TenantId tenant1 = new TenantId(UUID.randomUUID());
        TenantId tenant2 = new TenantId(UUID.randomUUID());
        DeviceProfileId profileId1 = new DeviceProfileId(UUID.randomUUID());
        DeviceProfileId profileId2 = new DeviceProfileId(UUID.randomUUID());

        DeviceProfile profile1 = loadProfileIntoCache(tenant1, profileId1);
        loadProfileIntoCache(tenant2, profileId2);

        cache.onComponentLifecycleEvent(new ComponentLifecycleMsg(tenant2, tenant2, ComponentLifecycleEvent.DELETED));

        assertThat(cache.get(tenant1, profileId1)).isEqualTo(profile1);
        verify(deviceProfileService, times(1)).findDeviceProfileById(tenant1, profileId1);
    }

    // --- Helpers ---

    private DeviceProfile loadProfileIntoCache(TenantId tenantId, DeviceProfileId profileId) {
        DeviceProfile profile = new DeviceProfile();
        profile.setId(profileId);
        profile.setTenantId(tenantId);
        when(deviceProfileService.findDeviceProfileById(tenantId, profileId)).thenReturn(profile);
        cache.get(tenantId, profileId);
        return profile;
    }

    private void loadDeviceMappingIntoCache(TenantId tenantId, DeviceId deviceId, DeviceProfileId profileId) {
        Device device = new Device();
        device.setId(deviceId);
        device.setDeviceProfileId(profileId);
        when(deviceService.findDeviceById(tenantId, deviceId)).thenReturn(device);
        cache.get(tenantId, deviceId);
    }

}
