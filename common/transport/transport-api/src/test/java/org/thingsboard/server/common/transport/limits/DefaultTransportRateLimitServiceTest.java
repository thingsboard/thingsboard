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
package org.thingsboard.server.common.transport.limits;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.TenantProfileId;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.common.data.tenant.profile.TenantProfileData;
import org.thingsboard.server.common.transport.TransportTenantProfileCache;
import org.thingsboard.server.common.transport.profile.TenantProfileUpdateResult;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultTransportRateLimitServiceTest {

    private TransportTenantProfileCache tenantProfileCache;
    private ExecutorService executor;

    private final TenantId tenant = TenantId.fromUUID(UUID.randomUUID());

    @BeforeEach
    void setUp() {
        tenantProfileCache = mock(TransportTenantProfileCache.class);
        executor = Executors.newCachedThreadPool();
    }

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    @Test
    void checkLimitsDoesNotHoldMapBinLockAcrossProfileFetch() throws Exception {
        // Two concurrent rate-limit checks for the SAME tenant must both be able to reach
        // the (blocking) tenant-profile fetch concurrently. If the blocking fetch runs inside
        // ConcurrentHashMap.computeIfAbsent, the second caller is stuck on the bin reservation
        // node and never reaches the fetch -> the latch never reaches zero.
        CountDownLatch bothCallersReachedFetch = new CountDownLatch(2);
        CountDownLatch releaseFetch = new CountDownLatch(1);

        when(tenantProfileCache.get(tenant)).thenAnswer(invocation -> {
            bothCallersReachedFetch.countDown();
            releaseFetch.await(5, TimeUnit.SECONDS);
            return tenantProfile();
        });

        DefaultTransportRateLimitService service = new DefaultTransportRateLimitService(tenantProfileCache);

        Runnable check = () -> service.checkLimits(tenant, null, null, 1, false);
        executor.submit(check);
        executor.submit(check);

        boolean bothReached = bothCallersReachedFetch.await(3, TimeUnit.SECONDS);
        releaseFetch.countDown();

        assertThat(bothReached)
                .as("both checkLimits calls should reach the profile fetch concurrently (no bin lock across I/O)")
                .isTrue();
    }

    @ParameterizedTest
    @EnumSource(TransportLimitsType.class)
    void eachLimitsTypeReadsItsOwnProfileFields(TransportLimitsType type) {
        // Distinct sentinel per profile field so a transposed method reference (e.g. GATEWAY_DEVICE_LIMITS
        // wired to the plain gateway getters) resolves to the wrong value and fails the assertion.
        DefaultTenantProfileConfiguration config = new DefaultTenantProfileConfiguration();
        config.setTransportTenantMsgRateLimit("tenant-msg");
        config.setTransportTenantTelemetryMsgRateLimit("tenant-tele-msg");
        config.setTransportTenantTelemetryDataPointsRateLimit("tenant-tele-dp");
        config.setTransportDeviceMsgRateLimit("device-msg");
        config.setTransportDeviceTelemetryMsgRateLimit("device-tele-msg");
        config.setTransportDeviceTelemetryDataPointsRateLimit("device-tele-dp");
        config.setTransportGatewayMsgRateLimit("gateway-msg");
        config.setTransportGatewayTelemetryMsgRateLimit("gateway-tele-msg");
        config.setTransportGatewayTelemetryDataPointsRateLimit("gateway-tele-dp");
        config.setTransportGatewayDeviceMsgRateLimit("gateway-device-msg");
        config.setTransportGatewayDeviceTelemetryMsgRateLimit("gateway-device-tele-msg");
        config.setTransportGatewayDeviceTelemetryDataPointsRateLimit("gateway-device-tele-dp");

        String prefix = switch (type) {
            case TENANT_LIMITS -> "tenant";
            case DEVICE_LIMITS -> "device";
            case GATEWAY_LIMITS -> "gateway";
            case GATEWAY_DEVICE_LIMITS -> "gateway-device";
        };

        assertThat(type.getRegularMsgRateLimit().apply(config)).isEqualTo(prefix + "-msg");
        assertThat(type.getTelemetryMsgRateLimit().apply(config)).isEqualTo(prefix + "-tele-msg");
        assertThat(type.getTelemetryDataPointsRateLimit().apply(config)).isEqualTo(prefix + "-tele-dp");
    }

    @ParameterizedTest
    @EnumSource(EntityLevel.class)
    void profileUpdateReachesEntityTrackedDuringFirstCheck(EntityLevel level) {
        DeviceId entity = new DeviceId(UUID.randomUUID());
        when(tenantProfileCache.get(tenant)).thenReturn(profileWithRegularMsgLimit(level, "100:600"));
        DefaultTransportRateLimitService service = new DefaultTransportRateLimitService(tenantProfileCache);

        // First check resolves the (permissive) limit and must register the entity into the per-tenant
        // tracking set via the onMiss callback - otherwise a later update(tenantId) can't reach it.
        assertThat(level.check(service, tenant, entity))
                .as("permissive limit should allow the first %s check", level).isNull();

        // Tighten the limit to a single message and push a profile update for this tenant.
        service.update(new TenantProfileUpdateResult(profileWithRegularMsgLimit(level, "1:600"), Set.of(tenant)));

        // The freshly merged "1:600" bucket allows exactly one message...
        assertThat(level.check(service, tenant, entity)).isNull();
        // ...and blocks the next one. This only happens if update(tenantId) reached the tracked entity.
        assertThat(level.check(service, tenant, entity))
                .as("update(tenantId) must reach the tracked %s so the tightened limit applies", level).isNotNull();
    }

    private TenantProfile tenantProfile() {
        return profileWith(new DefaultTenantProfileConfiguration());
    }

    private TenantProfile profileWithRegularMsgLimit(EntityLevel level, String regularMsgRateLimit) {
        DefaultTenantProfileConfiguration config = new DefaultTenantProfileConfiguration();
        level.setRegularMsgRateLimit(config, regularMsgRateLimit);
        return profileWith(config);
    }

    private TenantProfile profileWith(DefaultTenantProfileConfiguration config) {
        TenantProfile profile = new TenantProfile(new TenantProfileId(UUID.randomUUID()));
        profile.setName("test-profile");
        TenantProfileData profileData = new TenantProfileData();
        profileData.setConfiguration(config);
        profile.setProfileData(profileData);
        return profile;
    }

    private enum EntityLevel {
        DEVICE {
            @Override
            void setRegularMsgRateLimit(DefaultTenantProfileConfiguration config, String value) {
                config.setTransportDeviceMsgRateLimit(value);
            }

            @Override
            Object check(DefaultTransportRateLimitService service, TenantId tenantId, DeviceId entityId) {
                return service.checkLimits(tenantId, null, entityId, 0, false);
            }
        },
        GATEWAY {
            @Override
            void setRegularMsgRateLimit(DefaultTenantProfileConfiguration config, String value) {
                config.setTransportGatewayMsgRateLimit(value);
            }

            @Override
            Object check(DefaultTransportRateLimitService service, TenantId tenantId, DeviceId entityId) {
                return service.checkLimits(tenantId, entityId, null, 0, false);
            }
        },
        GATEWAY_DEVICE {
            @Override
            void setRegularMsgRateLimit(DefaultTenantProfileConfiguration config, String value) {
                config.setTransportGatewayDeviceMsgRateLimit(value);
            }

            @Override
            Object check(DefaultTransportRateLimitService service, TenantId tenantId, DeviceId entityId) {
                return service.checkLimits(tenantId, null, entityId, 0, true);
            }
        };

        abstract void setRegularMsgRateLimit(DefaultTenantProfileConfiguration config, String value);

        abstract Object check(DefaultTransportRateLimitService service, TenantId tenantId, DeviceId entityId);
    }

}
