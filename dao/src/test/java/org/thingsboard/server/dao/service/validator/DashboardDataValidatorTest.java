// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.service.validator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.tenant.TenantService;

import java.util.UUID;

import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.verify;

@SpringBootTest(classes = DashboardDataValidator.class)
class DashboardDataValidatorTest {

    @MockitoBean
    TenantService tenantService;
    @MockitoSpyBean
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

}
