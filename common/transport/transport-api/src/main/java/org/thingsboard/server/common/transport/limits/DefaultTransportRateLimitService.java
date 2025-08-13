/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.server.common.transport.limits;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.common.data.tenant.profile.TenantProfileData;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.common.transport.TransportTenantProfileCache;
import org.thingsboard.server.common.transport.profile.TenantProfileUpdateResult;
import org.thingsboard.server.queue.util.TbTransportComponent;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static org.thingsboard.server.common.transport.limits.TransportLimitsType.DEVICE_LIMITS;
import static org.thingsboard.server.common.transport.limits.TransportLimitsType.GATEWAY_DEVICE_LIMITS;
import static org.thingsboard.server.common.transport.limits.TransportLimitsType.GATEWAY_LIMITS;
import static org.thingsboard.server.common.transport.limits.TransportLimitsType.TENANT_LIMITS;

@Service
@TbTransportComponent
@Slf4j
public class DefaultTransportRateLimitService implements TransportRateLimitService {

    private final static DummyTransportRateLimit ALLOW = new DummyTransportRateLimit();
    private final ConcurrentMap<TenantId, Boolean> tenantAllowed = new ConcurrentHashMap<>();
    private final ConcurrentMap<TenantId, Set<DeviceId>> tenantDevices = new ConcurrentHashMap<>();
    private final ConcurrentMap<TenantId, Set<DeviceId>> tenantGateways = new ConcurrentHashMap<>();
    private final ConcurrentMap<TenantId, Set<DeviceId>> tenantGatewayDevices = new ConcurrentHashMap<>();
    private final ConcurrentMap<TenantId, EntityTransportRateLimits> perTenantLimits = new ConcurrentHashMap<>();
    private final ConcurrentMap<DeviceId, EntityTransportRateLimits> perDeviceLimits = new ConcurrentHashMap<>();
    private final ConcurrentMap<DeviceId, EntityTransportRateLimits> perGatewayLimits = new ConcurrentHashMap<>();
    private final ConcurrentMap<DeviceId, EntityTransportRateLimits> perGatewayDeviceLimits = new ConcurrentHashMap<>();
    private final Map<InetAddress, InetAddressRateLimitStats> ipMap = new ConcurrentHashMap<>();

    private final TransportTenantProfileCache tenantProfileCache;

    @Value("${transport.rate_limits.ip_limits_enabled:false}")
    private boolean ipRateLimitsEnabled;
    @Value("${transport.rate_limits.max_wrong_credentials_per_ip:10}")
    private int maxWrongCredentialsPerIp;
    @Value("${transport.rate_limits.ip_block_timeout:60000}")
    private long ipBlockTimeout;

    public DefaultTransportRateLimitService(TransportTenantProfileCache tenantProfileCache) {
        this.tenantProfileCache = tenantProfileCache;
    }

    @Override
    public TbPair<EntityType, Boolean> checkLimits(TenantId tenantId, DeviceId gatewayId, DeviceId deviceId, int dataPoints, boolean isGateway) {
        if (!tenantAllowed.getOrDefault(tenantId, Boolean.TRUE)) {
            return TbPair.of(EntityType.API_USAGE_STATE, false);
        }
        if (!checkEntityRateLimit(dataPoints, getTenantRateLimits(tenantId))) {
            return TbPair.of(EntityType.TENANT, false);
        }
        if (isGateway) {
            if (!checkEntityRateLimit(dataPoints, getGatewayDeviceRateLimits(tenantId, deviceId))) {
                return TbPair.of(EntityType.DEVICE, true);
            }
        } else if (gatewayId == null && deviceId != null) {
            if (!checkEntityRateLimit(dataPoints, getDeviceRateLimits(tenantId, deviceId))) {
                return TbPair.of(EntityType.DEVICE, false);
            }
        }
        if (gatewayId != null && !checkEntityRateLimit(dataPoints, getGatewayRateLimits(tenantId, gatewayId))) {
            return TbPair.of(EntityType.DEVICE, true);
        }
        return null;
    }

    private boolean checkEntityRateLimit(int dataPoints, EntityTransportRateLimits limits) {
        if (dataPoints > 0) {
            return limits.getTelemetryMsgRateLimit().tryConsume() && limits.getTelemetryDataPointsRateLimit().tryConsume(dataPoints);
        } else {
            return limits.getRegularMsgRateLimit().tryConsume();
        }
    }

    @Override
    public void update(TenantProfileUpdateResult update) {
        log.info("Received tenant profile update: {}", update.getProfile());
        EntityTransportRateLimits tenantRateLimitPrototype = createRateLimits(update.getProfile(), TENANT_LIMITS);
        EntityTransportRateLimits deviceRateLimitPrototype = createRateLimits(update.getProfile(), DEVICE_LIMITS);
        EntityTransportRateLimits gatewayRateLimitPrototype = createRateLimits(update.getProfile(), GATEWAY_LIMITS);
        EntityTransportRateLimits gatewayDeviceRateLimitPrototype = createRateLimits(update.getProfile(), GATEWAY_DEVICE_LIMITS);
        for (TenantId tenantId : update.getAffectedTenants()) {
            update(tenantId, tenantRateLimitPrototype, deviceRateLimitPrototype, gatewayRateLimitPrototype, gatewayDeviceRateLimitPrototype);
        }
    }

    @Override
    public void update(TenantId tenantId) {
        EntityTransportRateLimits tenantRateLimitPrototype = createRateLimits(tenantProfileCache.get(tenantId), TENANT_LIMITS);
        EntityTransportRateLimits deviceRateLimitPrototype = createRateLimits(tenantProfileCache.get(tenantId), DEVICE_LIMITS);
        EntityTransportRateLimits gatewayRateLimitPrototype = createRateLimits(tenantProfileCache.get(tenantId), GATEWAY_LIMITS);
        EntityTransportRateLimits gatewayDeviceRateLimitPrototype = createRateLimits(tenantProfileCache.get(tenantId), GATEWAY_DEVICE_LIMITS);
        update(tenantId, tenantRateLimitPrototype, deviceRateLimitPrototype, gatewayRateLimitPrototype, gatewayDeviceRateLimitPrototype);
    }

    private void update(TenantId tenantId, EntityTransportRateLimits tenantRateLimitPrototype, EntityTransportRateLimits deviceRateLimitPrototype,
                        EntityTransportRateLimits gatewayRateLimitPrototype, EntityTransportRateLimits gatewayDeviceRateLimitPrototype) {
        mergeLimits(tenantId, tenantRateLimitPrototype, perTenantLimits::get, perTenantLimits::put);
        getTenantDevices(tenantId).forEach(deviceId -> mergeLimits(deviceId, deviceRateLimitPrototype, perDeviceLimits::get, perDeviceLimits::put));
        getTenantGateways(tenantId).forEach(gatewayId -> mergeLimits(gatewayId, gatewayRateLimitPrototype, perGatewayLimits::get, perGatewayLimits::put));
        getTenantGatewayDevices(tenantId).forEach(gatewayId -> mergeLimits(gatewayId, gatewayDeviceRateLimitPrototype, perGatewayDeviceLimits::get, perGatewayDeviceLimits::put));
    }

    @Override
    public void remove(TenantId tenantId) {
        perTenantLimits.remove(tenantId);
        tenantDevices.remove(tenantId);
        tenantGateways.remove(tenantId);
        tenantGatewayDevices.remove(tenantId);
    }

    @Override
    public void remove(DeviceId deviceId) {
        perDeviceLimits.remove(deviceId);
        perGatewayLimits.remove(deviceId);
        perGatewayDeviceLimits.remove(deviceId);
        tenantDevices.values().forEach(set -> set.remove(deviceId));
        tenantGateways.values().forEach(set -> set.remove(deviceId));
        tenantGatewayDevices.values().forEach(set -> set.remove(deviceId));
    }

    @Override
    public void update(TenantId tenantId, boolean allowed) {
        tenantAllowed.put(tenantId, allowed);
    }

    @Override
    public boolean checkAddress(InetSocketAddress address) {
        if (!ipRateLimitsEnabled) {
            return true;
        }
        var stats = ipMap.computeIfAbsent(address.getAddress(), a -> new InetAddressRateLimitStats());
        return !stats.isBlocked() || (stats.getLastActivityTs() + ipBlockTimeout < System.currentTimeMillis());
    }

    @Override
    public void onAuthSuccess(InetSocketAddress address) {
        if (!ipRateLimitsEnabled) {
            return;
        }

        var stats = ipMap.computeIfAbsent(address.getAddress(), a -> new InetAddressRateLimitStats());
        stats.getLock().lock();
        try {
            stats.setLastActivityTs(System.currentTimeMillis());
            stats.setFailureCount(0);
            if (stats.isBlocked()) {
                stats.setBlocked(false);
                log.info("[{}] IP address un-blocked due to correct credentials.", address.getAddress());
            }
        } finally {
            stats.getLock().unlock();
        }
    }

    @Override
    public void onAuthFailure(InetSocketAddress address) {
        if (!ipRateLimitsEnabled) {
            return;
        }

        var stats = ipMap.computeIfAbsent(address.getAddress(), a -> new InetAddressRateLimitStats());
        stats.getLock().lock();
        try {
            stats.setLastActivityTs(System.currentTimeMillis());
            int failureCount = stats.getFailureCount() + 1;
            stats.setFailureCount(failureCount);
            if (failureCount >= maxWrongCredentialsPerIp) {
                log.info("[{}] IP address blocked due to constantly wrong credentials.", address.getAddress());
                stats.setBlocked(true);
            }
        } finally {
            stats.getLock().unlock();
        }
    }

    @Override
    public void invalidateRateLimitsIpTable(long sessionInactivityTimeout) {
        if (!ipRateLimitsEnabled) {
            return;
        }
        long currentTime = System.currentTimeMillis();
        long expTime = currentTime - Math.max(sessionInactivityTimeout, ipBlockTimeout);
        for (var entry : ipMap.entrySet()) {
            var stats = entry.getValue();
            if (stats.getLastActivityTs() < expTime) {
                log.debug("[{}] IP address removed due to session inactivity timeout.", entry.getKey());
                ipMap.remove(entry.getKey());
            } else if (stats.isBlocked() && (stats.getLastActivityTs() + ipBlockTimeout < currentTime)) {
                log.info("[{}] IP address unblocked due ip block timeout.", entry.getKey());
                stats.setBlocked(false);
            }
        }
    }

    private <T extends EntityId> void mergeLimits(T entityId, EntityTransportRateLimits newRateLimits,
                                                  Function<T, EntityTransportRateLimits> getFunction,
                                                  BiConsumer<T, EntityTransportRateLimits> putFunction) {
        EntityTransportRateLimits oldRateLimits = getFunction.apply(entityId);
        if (oldRateLimits == null) {
            if (EntityType.TENANT.equals(entityId.getEntityType())) {
                log.info("[{}] New rate limits: {}", entityId, newRateLimits);
            } else {
                log.debug("[{}] New rate limits: {}", entityId, newRateLimits);
            }
            putFunction.accept(entityId, newRateLimits);
        } else {
            EntityTransportRateLimits updated = merge(oldRateLimits, newRateLimits);
            if (updated != null) {
                if (EntityType.TENANT.equals(entityId.getEntityType())) {
                    log.info("[{}] Updated rate limits: {}", entityId, updated);
                } else {
                    log.debug("[{}] Updated rate limits: {}", entityId, updated);
                }
                putFunction.accept(entityId, updated);
            }
        }
    }

    private EntityTransportRateLimits merge(EntityTransportRateLimits oldRateLimits, EntityTransportRateLimits newRateLimits) {
        boolean regularUpdate = !oldRateLimits.getRegularMsgRateLimit().getConfiguration().equals(newRateLimits.getRegularMsgRateLimit().getConfiguration());
        boolean telemetryMsgRateUpdate = !oldRateLimits.getTelemetryMsgRateLimit().getConfiguration().equals(newRateLimits.getTelemetryMsgRateLimit().getConfiguration());
        boolean telemetryDataPointUpdate = !oldRateLimits.getTelemetryDataPointsRateLimit().getConfiguration().equals(newRateLimits.getTelemetryDataPointsRateLimit().getConfiguration());
        if (regularUpdate || telemetryMsgRateUpdate || telemetryDataPointUpdate) {
            return new EntityTransportRateLimits(
                    regularUpdate ? newLimit(newRateLimits.getRegularMsgRateLimit().getConfiguration()) : oldRateLimits.getRegularMsgRateLimit(),
                    telemetryMsgRateUpdate ? newLimit(newRateLimits.getTelemetryMsgRateLimit().getConfiguration()) : oldRateLimits.getTelemetryMsgRateLimit(),
                    telemetryDataPointUpdate ? newLimit(newRateLimits.getTelemetryDataPointsRateLimit().getConfiguration()) : oldRateLimits.getTelemetryDataPointsRateLimit());
        } else {
            return null;
        }
    }

    private EntityTransportRateLimits createRateLimits(TenantProfile tenantProfile, TransportLimitsType limitsType) {
        TenantProfileData profileData = tenantProfile.getProfileData();
        DefaultTenantProfileConfiguration profile = (DefaultTenantProfileConfiguration) profileData.getConfiguration();
        if (profile == null) {
            return new EntityTransportRateLimits(ALLOW, ALLOW, ALLOW);
        } else {
            TransportRateLimit regularMsgRateLimit;
            TransportRateLimit telemetryMsgRateLimit;
            TransportRateLimit telemetryDpRateLimit;
            switch (limitsType) {
                case TENANT_LIMITS -> {
                    regularMsgRateLimit = newLimit(profile.getTransportTenantMsgRateLimit());
                    telemetryMsgRateLimit = newLimit(profile.getTransportTenantTelemetryMsgRateLimit());
                    telemetryDpRateLimit = newLimit(profile.getTransportTenantTelemetryDataPointsRateLimit());
                }
                case DEVICE_LIMITS -> {
                    regularMsgRateLimit = newLimit(profile.getTransportDeviceMsgRateLimit());
                    telemetryMsgRateLimit = newLimit(profile.getTransportDeviceTelemetryMsgRateLimit());
                    telemetryDpRateLimit = newLimit(profile.getTransportDeviceTelemetryDataPointsRateLimit());
                }
                case GATEWAY_LIMITS -> {
                    regularMsgRateLimit = newLimit(profile.getTransportGatewayMsgRateLimit());
                    telemetryMsgRateLimit = newLimit(profile.getTransportGatewayTelemetryMsgRateLimit());
                    telemetryDpRateLimit = newLimit(profile.getTransportGatewayTelemetryDataPointsRateLimit());
                }
                case GATEWAY_DEVICE_LIMITS -> {
                    regularMsgRateLimit = newLimit(profile.getTransportGatewayDeviceMsgRateLimit());
                    telemetryMsgRateLimit = newLimit(profile.getTransportGatewayDeviceTelemetryMsgRateLimit());
                    telemetryDpRateLimit = newLimit(profile.getTransportGatewayDeviceTelemetryDataPointsRateLimit());
                }
                default -> throw new IllegalStateException("Unknown limits type: " + limitsType);
            }

            return new EntityTransportRateLimits(regularMsgRateLimit, telemetryMsgRateLimit, telemetryDpRateLimit);
        }
    }

    private static TransportRateLimit newLimit(String config) {
        return StringUtils.isEmpty(config) ? ALLOW : new SimpleTransportRateLimit(config);
    }

    private EntityTransportRateLimits getTenantRateLimits(TenantId tenantId) {
        return perTenantLimits.computeIfAbsent(tenantId, k -> createRateLimits(tenantProfileCache.get(tenantId), TENANT_LIMITS));
    }

    private EntityTransportRateLimits getDeviceRateLimits(TenantId tenantId, DeviceId deviceId) {
        return perDeviceLimits.computeIfAbsent(deviceId, k -> {
            EntityTransportRateLimits limits = createRateLimits(tenantProfileCache.get(tenantId), DEVICE_LIMITS);
            getTenantDevices(tenantId).add(deviceId);
            return limits;
        });
    }

    private EntityTransportRateLimits getGatewayRateLimits(TenantId tenantId, DeviceId gatewayId) {
        return perGatewayLimits.computeIfAbsent(gatewayId, k -> {
            EntityTransportRateLimits limits = createRateLimits(tenantProfileCache.get(tenantId), GATEWAY_LIMITS);
            getTenantGateways(tenantId).add(gatewayId);
            return limits;
        });
    }

    private EntityTransportRateLimits getGatewayDeviceRateLimits(TenantId tenantId, DeviceId gatewayId) {
        return perGatewayDeviceLimits.computeIfAbsent(gatewayId, k -> {
            EntityTransportRateLimits limits = createRateLimits(tenantProfileCache.get(tenantId), GATEWAY_DEVICE_LIMITS);
            getTenantGatewayDevices(tenantId).add(gatewayId);
            return limits;
        });
    }

    private Set<DeviceId> getTenantDevices(TenantId tenantId) {
        return tenantDevices.computeIfAbsent(tenantId, id -> ConcurrentHashMap.newKeySet());
    }

    private Set<DeviceId> getTenantGateways(TenantId tenantId) {
        return tenantGateways.computeIfAbsent(tenantId, id -> ConcurrentHashMap.newKeySet());
    }

    private Set<DeviceId> getTenantGatewayDevices(TenantId tenantId) {
        return tenantGatewayDevices.computeIfAbsent(tenantId, id -> ConcurrentHashMap.newKeySet());
    }

}
