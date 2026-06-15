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
package org.thingsboard.server.dao.service.validator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.dao.usagerecord.ApiLimitService;
import org.thingsboard.server.exception.EntitiesLimitExceededException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.verify;

@SpringBootTest(classes = DashboardDataValidator.class)
class DashboardDataValidatorTest {

    @MockBean
    TenantService tenantService;
    @MockBean
    ApiLimitService apiLimitService;
    @SpyBean
    DashboardDataValidator validator;
    TenantId tenantId = TenantId.fromUUID(UUID.fromString("9ef79cdf-37a8-4119-b682-2e7ed4e018da"));

    @BeforeEach
    void setUp() {
        willReturn(true).given(tenantService).tenantExists(tenantId);
    }

    @Test
    void testValidateNameInvocation() {
        Dashboard dashboard = new Dashboard();
        dashboard.setTitle("flight control");
        dashboard.setTenantId(tenantId);

        validator.validateDataImpl(tenantId, dashboard);
        verify(validator).validateString("Dashboard title", dashboard.getTitle());
    }

    @Test
    void validateMaxDashboardsPerTenant_doesNotThrow_whenLimitNotReached() {
        willReturn(true).given(apiLimitService).checkEntitiesLimit(tenantId, EntityType.DASHBOARD);

        assertThatNoException().isThrownBy(() -> validator.validateMaxDashboardsPerTenant(tenantId));
    }

    @Test
    void validateMaxDashboardsPerTenant_throwsEntitiesLimitExceeded_whenLimitReached() {
        long limit = 5;
        willReturn(false).given(apiLimitService).checkEntitiesLimit(tenantId, EntityType.DASHBOARD);
        willReturn(limit).given(apiLimitService).getLimit(eq(tenantId), any());

        assertThatThrownBy(() -> validator.validateMaxDashboardsPerTenant(tenantId))
                .isInstanceOfSatisfying(EntitiesLimitExceededException.class, ex -> {
                    assertThat(ex.getTenantId()).isEqualTo(tenantId);
                    assertThat(ex.getEntityType()).isEqualTo(EntityType.DASHBOARD);
                    assertThat(ex.getLimit()).isEqualTo(limit);
                });
    }

}
