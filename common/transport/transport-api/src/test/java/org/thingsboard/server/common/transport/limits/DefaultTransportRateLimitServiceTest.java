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
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.TenantProfileId;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.common.data.tenant.profile.TenantProfileData;
import org.thingsboard.server.common.transport.TransportTenantProfileCache;

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

    private TenantProfile tenantProfile() {
        TenantProfile profile = new TenantProfile(new TenantProfileId(UUID.randomUUID()));
        profile.setName("test-profile");
        TenantProfileData profileData = new TenantProfileData();
        profileData.setConfiguration(new DefaultTenantProfileConfiguration());
        profile.setProfileData(profileData);
        return profile;
    }

}
