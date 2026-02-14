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
