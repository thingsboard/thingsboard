// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.service.validator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.asset.AssetProfileDao;
import org.thingsboard.server.dao.asset.AssetProfileService;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.queue.QueueService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.tenant.TenantService;

import java.util.UUID;

import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.verify;

@SpringBootTest(classes = AssetProfileDataValidator.class)
class AssetProfileDataValidatorTest {

    @MockitoBean
    AssetProfileDao assetProfileDao;
    @MockitoBean
    AssetProfileService assetProfileService;
    @MockitoBean
    TenantService tenantService;
    @MockitoBean
    QueueService queueService;
    @MockitoBean
    RuleChainService ruleChainService;
    @MockitoBean
    DashboardService dashboardService;
    @MockitoSpyBean
    AssetProfileDataValidator validator;
    TenantId tenantId = TenantId.fromUUID(UUID.fromString("9ef79cdf-37a8-4119-b682-2e7ed4e018da"));

    @BeforeEach
    void setUp() {
        willReturn(true).given(tenantService).tenantExists(tenantId);
    }

    @Test
    void testValidateNameInvocation() {
        AssetProfile assetProfile = new AssetProfile();
        assetProfile.setName("prod");
        assetProfile.setTenantId(tenantId);

        validator.validateDataImpl(tenantId, assetProfile);
        verify(validator).validateString("Asset profile name", assetProfile.getName());
    }

}