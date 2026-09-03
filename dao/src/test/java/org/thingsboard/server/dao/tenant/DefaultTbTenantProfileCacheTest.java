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
package org.thingsboard.server.dao.tenant;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.NestedTestConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.TenantProfileId;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.willReturn;

@SpringBootTest(classes = DefaultTbTenantProfileCache.class)
@TestPropertySource(properties = "security.restricted_tenant_profiles=Free,Trial")
class DefaultTbTenantProfileCacheTest {

    @MockitoBean
    private TenantProfileService tenantProfileService;
    @MockitoBean
    private TenantService tenantService;

    @Autowired
    private DefaultTbTenantProfileCache tenantProfileCache;

    // DefaultTbTenantProfileCache caches into ConcurrentHashMaps that are never evicted, and the
    // Spring context is shared across methods, so every test must use its own tenant and profile id.
    private TenantId givenTenantWithProfileName(String name) {
        TenantId tenantId = TenantId.fromUUID(UUID.randomUUID());
        TenantProfileId tenantProfileId = new TenantProfileId(UUID.randomUUID());

        Tenant tenant = new Tenant();
        tenant.setId(tenantId);
        tenant.setTenantProfileId(tenantProfileId);
        willReturn(tenant).given(tenantService).findTenantById(tenantId);

        TenantProfile profile = new TenantProfile(tenantProfileId);
        profile.setName(name);
        willReturn(profile).given(tenantProfileService).findTenantProfileById(TenantId.SYS_TENANT_ID, tenantProfileId);
        return tenantId;
    }

    @Test
    void shouldBeRestrictedWhenProfileNameIsConfigured() {
        assertThat(tenantProfileCache.isRestricted(givenTenantWithProfileName("Free"))).isTrue();
    }

    @Test
    void shouldBeRestrictedForAnyConfiguredName() {
        assertThat(tenantProfileCache.isRestricted(givenTenantWithProfileName("Trial"))).isTrue();
    }

    @Test
    void shouldNotBeRestrictedWhenProfileNameIsNotConfigured() {
        assertThat(tenantProfileCache.isRestricted(givenTenantWithProfileName("Maximum"))).isFalse();
    }

    @Test
    void shouldMatchProfileNameCaseSensitively() {
        assertThat(tenantProfileCache.isRestricted(givenTenantWithProfileName("free"))).isFalse();
    }

    @Test
    void shouldNotBeRestrictedForSysTenant() {
        // Stub the sys tenant onto a restricted profile so the isSysTenantId() guard is load-bearing:
        // without it, this would resolve the profile below and return true.
        assertThat(tenantProfileCache.isRestricted(givenSysTenantWithProfileName("Free"))).isFalse();
    }

    @Test
    void shouldNotBeRestrictedForUnknownTenant() {
        TenantId unknown = TenantId.fromUUID(UUID.randomUUID());
        willReturn(null).given(tenantService).findTenantById(unknown);
        assertThat(tenantProfileCache.isRestricted(unknown)).isFalse();
    }

    @Test
    void shouldNotBeRestrictedForNullTenant() {
        assertThat(tenantProfileCache.isRestricted(null)).isFalse();
    }

    private TenantId givenSysTenantWithProfileName(String name) {
        TenantProfileId tenantProfileId = new TenantProfileId(UUID.randomUUID());

        Tenant tenant = new Tenant();
        tenant.setId(TenantId.SYS_TENANT_ID);
        tenant.setTenantProfileId(tenantProfileId);
        willReturn(tenant).given(tenantService).findTenantById(TenantId.SYS_TENANT_ID);

        TenantProfile profile = new TenantProfile(tenantProfileId);
        profile.setName(name);
        willReturn(profile).given(tenantProfileService).findTenantProfileById(TenantId.SYS_TENANT_ID, tenantProfileId);
        return TenantId.SYS_TENANT_ID;
    }

    // Own context configuration (not inherited) so the empty property overrides the outer class's value.
    @Nested
    @NestedTestConfiguration(NestedTestConfiguration.EnclosingConfiguration.OVERRIDE)
    @SpringBootTest(classes = DefaultTbTenantProfileCache.class)
    @TestPropertySource(properties = "security.restricted_tenant_profiles=")
    class WhenConfigurationIsEmpty {

        @MockitoBean
        private TenantProfileService tenantProfileService;
        @MockitoBean
        private TenantService tenantService;

        @Autowired
        private DefaultTbTenantProfileCache tenantProfileCache;

        @Test
        void shouldNotBeRestrictedEvenOnConfiguredProfileName() {
            TenantId tenantId = TenantId.fromUUID(UUID.randomUUID());
            TenantProfileId tenantProfileId = new TenantProfileId(UUID.randomUUID());

            Tenant tenant = new Tenant();
            tenant.setId(tenantId);
            tenant.setTenantProfileId(tenantProfileId);
            willReturn(tenant).given(tenantService).findTenantById(tenantId);

            TenantProfile profile = new TenantProfile(tenantProfileId);
            profile.setName("Free");
            willReturn(profile).given(tenantProfileService).findTenantProfileById(TenantId.SYS_TENANT_ID, tenantProfileId);

            assertThat(tenantProfileCache.isRestricted(tenantId)).isFalse();
        }

    }

}
