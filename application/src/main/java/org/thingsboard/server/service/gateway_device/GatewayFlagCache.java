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
package org.thingsboard.server.service.gateway_device;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.plugin.ComponentLifecycleMsg;
import org.thingsboard.server.dao.device.DeviceService;

import java.time.Duration;

/**
 * Local in-memory cache of the device gateway flag. Provides two levels of the check:
 * <ul>
 *     <li>{@link #isPotentialGateway(EntityId)} - non-blocking, safe to call on any hot path:
 *     consults the local cache only and treats a device with the unknown flag as a potential gateway;</li>
 *     <li>{@link #isGateway(TenantId, DeviceId)} - resolves the flag from the device and caches it.
 *     BLOCKING: performs a device lookup that may be a distributed cache call or a DB read,
 *     so it must not be called on hot paths - offload it to a dedicated pool.</li>
 * </ul>
 * The gateway flag lives in the device additional info, so any change of it is a device UPDATED
 * lifecycle event, that is broadcast to all core and rule engine nodes and republished
 * as an application event on each of them (see DefaultTbClusterService#broadcast and
 * AbstractConsumerService#handleComponentLifecycleMsg) - the cached flag is invalidated on it.
 * <p>
 * The expiration stays as a backstop even though the flag is invalidated on the lifecycle events:
 * a broadcast may be missed (e.g. around a node restart, or on a device save path that bypasses
 * the entity service layer), and DEVICE DELETED events are broadcast to the rule engine service nodes only
 * (see the core node entity type filter in DefaultTbClusterService#broadcast), so on the dedicated core nodes
 * the entries of the deleted devices are cleaned up by the expiration only.
 */
@Component
@RequiredArgsConstructor
public class GatewayFlagCache {

    private final DeviceService deviceService;

    @Value("${cache.gateway_flag.max_size:100000}")
    private int maxSize;
    @Value("${cache.gateway_flag.time_to_live_in_minutes:5}")
    private int timeToLiveInMinutes;

    private Cache<DeviceId, Boolean> gatewayFlagByDeviceId;

    @PostConstruct
    public void init() {
        gatewayFlagByDeviceId = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(Duration.ofMinutes(timeToLiveInMinutes))
                .build();
    }

    @EventListener(ComponentLifecycleMsg.class)
    public void onDeviceLifecycleEvent(ComponentLifecycleMsg event) {
        if (event.getEntityId().getEntityType() == EntityType.DEVICE) {
            gatewayFlagByDeviceId.invalidate(new DeviceId(event.getEntityId().getId()));
        }
    }

    /**
     * Non-blocking check against the local cache only: never performs a device lookup,
     * a device with the unknown flag is treated as a potential gateway.
     */
    public boolean isPotentialGateway(EntityId entityId) {
        return !Boolean.FALSE.equals(gatewayFlagByDeviceId.getIfPresent(new DeviceId(entityId.getId())));
    }

    /**
     * Returns the cached gateway flag, resolving it from the device on a cache miss.
     * BLOCKING on a miss: the device lookup may be a distributed cache call or a DB read -
     * do not call on hot paths.
     */
    public boolean isGateway(TenantId tenantId, DeviceId deviceId) {
        return gatewayFlagByDeviceId.get(deviceId, id -> resolveGatewayFlag(tenantId, id));
    }

    private boolean resolveGatewayFlag(TenantId tenantId, DeviceId deviceId) {
        Device device = deviceService.findDeviceById(tenantId, deviceId);
        return device != null && device.getAdditionalInfo() != null
                && device.getAdditionalInfo().has(DataConstants.GATEWAY_PARAMETER)
                && device.getAdditionalInfo().get(DataConstants.GATEWAY_PARAMETER).asBoolean();
    }
}
