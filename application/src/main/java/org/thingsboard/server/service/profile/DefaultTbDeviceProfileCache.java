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

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
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

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DefaultTbDeviceProfileCache implements TbDeviceProfileCache {

    private final Lock deviceProfileFetchLock = new ReentrantLock();
    private final DeviceProfileService deviceProfileService;
    private final DeviceService deviceService;

    private final ConcurrentMap<DeviceProfileId, DeviceProfile> deviceProfilesMap = new ConcurrentHashMap<>();
    private final ConcurrentMap<DeviceId, DeviceProfileId> devicesMap = new ConcurrentHashMap<>();
    private final ConcurrentMap<TenantId, ConcurrentMap<EntityId, Consumer<DeviceProfile>>> profileListeners = new ConcurrentHashMap<>();
    private final ConcurrentMap<TenantId, ConcurrentMap<EntityId, BiConsumer<DeviceId, DeviceProfile>>> deviceProfileListeners = new ConcurrentHashMap<>();

    public DefaultTbDeviceProfileCache(DeviceProfileService deviceProfileService, DeviceService deviceService) {
        this.deviceProfileService = deviceProfileService;
        this.deviceService = deviceService;
    }

    @Override
    public DeviceProfile get(TenantId tenantId, DeviceProfileId deviceProfileId) {
        DeviceProfile profile = deviceProfilesMap.get(deviceProfileId);
        if (profile == null) {
            deviceProfileFetchLock.lock();
            try {
                profile = deviceProfilesMap.get(deviceProfileId);
                if (profile == null) {
                    profile = deviceProfileService.findDeviceProfileById(tenantId, deviceProfileId);
                    if (profile != null) {
                        deviceProfilesMap.put(deviceProfileId, profile);
                        log.debug("[{}] Fetch device profile into cache: {}", profile.getId(), profile);
                    }
                }
            } finally {
                deviceProfileFetchLock.unlock();
            }
        }
        log.trace("[{}] Found device profile in cache: {}", deviceProfileId, profile);
        return profile;
    }

    @Override
    public DeviceProfile get(TenantId tenantId, DeviceId deviceId) {
        DeviceProfileId profileId = devicesMap.get(deviceId);
        if (profileId == null) {
            Device device = deviceService.findDeviceById(tenantId, deviceId);
            if (device != null) {
                profileId = device.getDeviceProfileId();
                devicesMap.put(deviceId, profileId);
            } else {
                return null;
            }
        }
        return get(tenantId, profileId);
    }

    @Override
    public void evict(TenantId tenantId, DeviceProfileId profileId) {
        DeviceProfile oldProfile = deviceProfilesMap.remove(profileId);
        log.debug("[{}] evict device profile from cache: {}", profileId, oldProfile);
        DeviceProfile newProfile = get(tenantId, profileId);
        if (newProfile != null) {
            notifyProfileListeners(newProfile);
        }
    }

    @Override
    public void evict(TenantId tenantId, DeviceId deviceId) {
        DeviceProfileId old = devicesMap.remove(deviceId);
        if (old != null) {
            DeviceProfile newProfile = get(tenantId, deviceId);
            if (newProfile == null || !old.equals(newProfile.getId())) {
                notifyDeviceListeners(tenantId, deviceId, newProfile);
            }
        }
    }

    @Override
    public void addListener(TenantId tenantId, EntityId listenerId,
                            Consumer<DeviceProfile> profileListener,
                            BiConsumer<DeviceId, DeviceProfile> deviceListener) {
        if (profileListener != null) {
            profileListeners.computeIfAbsent(tenantId, id -> new ConcurrentHashMap<>()).put(listenerId, profileListener);
        }
        if (deviceListener != null) {
            deviceProfileListeners.computeIfAbsent(tenantId, id -> new ConcurrentHashMap<>()).put(listenerId, deviceListener);
        }
    }

    @Override
    public DeviceProfile find(DeviceProfileId deviceProfileId) {
        return deviceProfileService.findDeviceProfileById(TenantId.SYS_TENANT_ID, deviceProfileId);
    }

    @Override
    public DeviceProfile findOrCreateDeviceProfile(TenantId tenantId, String profileName) {
        return deviceProfileService.findOrCreateDeviceProfile(tenantId, profileName);
    }

    @Override
    public void removeListener(TenantId tenantId, EntityId listenerId) {
        ConcurrentMap<EntityId, Consumer<DeviceProfile>> tenantListeners = profileListeners.get(tenantId);
        if (tenantListeners != null) {
            tenantListeners.remove(listenerId);
        }
        ConcurrentMap<EntityId, BiConsumer<DeviceId, DeviceProfile>> deviceListeners = deviceProfileListeners.get(tenantId);
        if (deviceListeners != null) {
            deviceListeners.remove(listenerId);
        }
    }

    @EventListener(ComponentLifecycleMsg.class)
    public void onComponentLifecycleEvent(ComponentLifecycleMsg event) {
        switch (event.getEntityId().getEntityType()) {
            case TENANT:
                if (event.getEvent() == ComponentLifecycleEvent.DELETED) {
                    TenantId tenantId = event.getTenantId();
                    Set<DeviceProfileId> toRemove = deviceProfilesMap.values().stream()
                            .filter(deviceProfile -> deviceProfile.getTenantId().equals(tenantId))
                            .map(DeviceProfile::getId)
                            .collect(Collectors.toSet());
                    deviceProfilesMap.keySet().removeAll(toRemove);
                    devicesMap.entrySet().removeIf(entry -> toRemove.contains(entry.getValue()));
                    profileListeners.remove(tenantId);
                    deviceProfileListeners.remove(tenantId);
                }
                break;
        }
    }

    private void notifyProfileListeners(DeviceProfile profile) {
        ConcurrentMap<EntityId, Consumer<DeviceProfile>> tenantListeners = profileListeners.get(profile.getTenantId());
        if (tenantListeners != null) {
            tenantListeners.forEach((id, listener) -> listener.accept(profile));
        }
    }

    private void notifyDeviceListeners(TenantId tenantId, DeviceId deviceId, DeviceProfile profile) {
        if (profile != null) {
            ConcurrentMap<EntityId, BiConsumer<DeviceId, DeviceProfile>> tenantListeners = deviceProfileListeners.get(tenantId);
            if (tenantListeners != null) {
                tenantListeners.forEach((id, listener) -> listener.accept(deviceId, profile));
            }
        }
    }

}
