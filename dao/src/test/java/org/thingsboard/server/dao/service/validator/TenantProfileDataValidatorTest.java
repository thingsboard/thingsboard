// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.service.validator;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.common.data.tenant.profile.TenantProfileData;
import org.thingsboard.server.dao.tenant.TenantProfileDao;
import org.thingsboard.server.dao.tenant.TenantProfileService;

import java.util.UUID;

import static org.mockito.Mockito.verify;

@SpringBootTest(classes = TenantProfileDataValidator.class)
class TenantProfileDataValidatorTest {

    @MockitoBean
    TenantProfileDao tenantProfileDao;
    @MockitoBean
    TenantProfileService tenantProfileService;
    @MockitoSpyBean
    TenantProfileDataValidator validator;
    TenantId tenantId = TenantId.fromUUID(UUID.fromString("9ef79cdf-37a8-4119-b682-2e7ed4e018da"));

    @Test
    void testValidateNameInvocation() {
        TenantProfile tenantProfile = new TenantProfile();
        tenantProfile.setName("Sandbox");
        TenantProfileData tenantProfileData = new TenantProfileData();
        tenantProfileData.setConfiguration(new DefaultTenantProfileConfiguration());
        tenantProfile.setProfileData(tenantProfileData);

        validator.validateDataImpl(tenantId, tenantProfile);
        verify(validator).validateString("Tenant profile name", tenantProfile.getName());
    }

}
