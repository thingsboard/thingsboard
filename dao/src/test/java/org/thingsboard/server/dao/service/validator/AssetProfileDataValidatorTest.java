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

    @MockBean
    AssetProfileDao assetProfileDao;
    @MockBean
    AssetProfileService assetProfileService;
    @MockBean
    TenantService tenantService;
    @MockBean
    QueueService queueService;
    @MockBean
    RuleChainService ruleChainService;
    @MockBean
    DashboardService dashboardService;
    @SpyBean
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