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
package org.thingsboard.server.common.transport.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.ApiUsageState;
import org.thingsboard.server.common.data.ApiUsageStateValue;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.id.ApiUsageStateId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.TenantProfileId;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.common.transport.limits.TransportRateLimitService;
import org.thingsboard.server.common.util.ProtoUtils;
import org.thingsboard.server.gen.transport.TransportProtos.GetEntityProfileRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.GetEntityProfileResponseMsg;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultTransportTenantProfileCacheTest {

    private DefaultTransportTenantProfileCache cache;
    private TransportService transportService;
    private TransportRateLimitService rateLimitService;
    private ExecutorService executor;

    private final TenantId tenantA = TenantId.fromUUID(UUID.randomUUID());
    private final TenantId tenantB = TenantId.fromUUID(UUID.randomUUID());

    @BeforeEach
    void setUp() {
        cache = new DefaultTransportTenantProfileCache();
        transportService = mock(TransportService.class);
        rateLimitService = mock(TransportRateLimitService.class);
        doNothing().when(rateLimitService).update(any(TenantId.class), anyBoolean());
        cache.setTransportService(transportService);
        cache.setRateLimitService(rateLimitService);
        executor = Executors.newCachedThreadPool();
    }

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    @Test
    void fetchForOneTenantDoesNotBlockResolutionOfAnotherTenant() throws Exception {
        CountDownLatch tenantAFetchStarted = new CountDownLatch(1);
        CountDownLatch releaseTenantA = new CountDownLatch(1);

        GetEntityProfileResponseMsg responseA = responseFor(tenantA);
        GetEntityProfileResponseMsg responseB = responseFor(tenantB);

        when(transportService.getEntityProfile(any())).thenAnswer(invocation -> {
            GetEntityProfileRequestMsg msg = invocation.getArgument(0);
            TenantId requested = TenantId.fromUUID(new UUID(msg.getEntityIdMSB(), msg.getEntityIdLSB()));
            if (requested.equals(tenantA)) {
                tenantAFetchStarted.countDown();
                releaseTenantA.await(5, TimeUnit.SECONDS);
                return responseA;
            }
            return responseB;
        });

        // T1 starts fetching tenantA's profile and blocks inside the cross-service round-trip.
        Future<TenantProfile> tenantAResult = executor.submit(() -> cache.get(tenantA));
        assertThat(tenantAFetchStarted.await(5, TimeUnit.SECONDS))
                .as("tenantA fetch should have started").isTrue();

        // T2 resolves a different tenant - it must NOT wait for tenantA's in-flight fetch.
        // Fails today (single global lock); passes once locking is per-tenant.
        TenantProfile tenantBProfile = CompletableFuture
                .supplyAsync(() -> cache.get(tenantB), executor)
                .get(2, TimeUnit.SECONDS);
        assertThat(tenantBProfile).isNotNull();

        releaseTenantA.countDown();
        assertThat(tenantAResult.get(5, TimeUnit.SECONDS)).isNotNull();
    }

    private GetEntityProfileResponseMsg responseFor(TenantId tenantId) {
        TenantProfile profile = new TenantProfile(new TenantProfileId(UUID.randomUUID()));
        profile.setName("profile-" + tenantId.getId());
        return GetEntityProfileResponseMsg.newBuilder()
                .setEntityType(EntityType.TENANT.name())
                .setTenantProfile(ProtoUtils.toProto(profile))
                .setApiState(ProtoUtils.toProto(enabledApiUsageState(tenantId)))
                .build();
    }

    private ApiUsageState enabledApiUsageState(TenantId tenantId) {
        ApiUsageState state = new ApiUsageState(new ApiUsageStateId(UUID.randomUUID()));
        state.setTenantId(tenantId);
        state.setEntityId(tenantId);
        state.setTransportState(ApiUsageStateValue.ENABLED);
        state.setDbStorageState(ApiUsageStateValue.ENABLED);
        state.setReExecState(ApiUsageStateValue.ENABLED);
        state.setJsExecState(ApiUsageStateValue.ENABLED);
        state.setTbelExecState(ApiUsageStateValue.ENABLED);
        state.setEmailExecState(ApiUsageStateValue.ENABLED);
        state.setSmsExecState(ApiUsageStateValue.ENABLED);
        state.setAlarmExecState(ApiUsageStateValue.ENABLED);
        state.setVersion(1L);
        return state;
    }

}
