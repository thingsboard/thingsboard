// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.service.validator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.common.data.tenant.profile.TenantProfileData;
import org.thingsboard.server.dao.tenant.TenantProfileDao;
import org.thingsboard.server.dao.tenant.TenantProfileService;
import org.thingsboard.server.exception.DataValidationException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

    @ParameterizedTest
    @ValueSource(ints = {-1, -100, Integer.MIN_VALUE})
    void minAllowedScheduledUpdateIntervalInSecForCF_shouldRejectNegativeValues(int value) {
        // GIVEN
        var config = new DefaultTenantProfileConfiguration();
        config.setMinAllowedScheduledUpdateIntervalInSecForCF(value);

        var tenantProfileData = new TenantProfileData();
        tenantProfileData.setConfiguration(config);

        var tenantProfile = new TenantProfile();
        tenantProfile.setName("Test");
        tenantProfile.setProfileData(tenantProfileData);

        // WHEN/THEN
        assertThatThrownBy(() -> validator.validate(tenantProfile, __ -> TenantId.SYS_TENANT_ID))
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("minAllowedScheduledUpdateIntervalInSecForCF")
                .hasMessageContaining("must be greater than or equal to 0");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 60, Integer.MAX_VALUE})
    void minAllowedScheduledUpdateIntervalInSecForCF_shouldAcceptValidValues(int value) {
        // GIVEN
        var config = new DefaultTenantProfileConfiguration();
        config.setMinAllowedScheduledUpdateIntervalInSecForCF(value);

        var tenantProfileData = new TenantProfileData();
        tenantProfileData.setConfiguration(config);

        var tenantProfile = new TenantProfile();
        tenantProfile.setName("Test");
        tenantProfile.setProfileData(tenantProfileData);

        // WHEN/THEN
        assertThatCode(() -> validator.validate(tenantProfile, __ -> TenantId.SYS_TENANT_ID))
                .doesNotThrowAnyException();
    }

}
