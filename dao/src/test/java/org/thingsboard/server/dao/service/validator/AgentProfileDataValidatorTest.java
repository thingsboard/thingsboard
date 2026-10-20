// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.service.validator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.server.common.data.agent.AgentProfile;
import org.thingsboard.server.common.data.id.AgentProfileId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.agent.AgentProfileDao;
import org.thingsboard.server.dao.agent.AgentProfileService;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.exception.DataValidationException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentProfileDataValidatorTest {

    @Mock
    private AgentProfileDao agentProfileDao;
    @Mock
    private AgentProfileService agentProfileService;
    @Mock
    private TenantService tenantService;

    @InjectMocks
    private AgentProfileDataValidator validator;

    private static final TenantId TENANT_ID = TenantId.fromUUID(UUID.randomUUID());

    @Test
    void validate_nullTenant_throws() {
        AgentProfile profile = validProfile();
        profile.setTenantId(null);

        assertThatThrownBy(() -> validator.validate(profile, AgentProfile::getTenantId))
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("assigned to tenant");
    }

    @Test
    void validate_nonExistentTenant_throws() {
        AgentProfile profile = validProfile();
        when(tenantService.tenantExists(TENANT_ID)).thenReturn(false);

        assertThatThrownBy(() -> validator.validate(profile, AgentProfile::getTenantId))
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("non-existent tenant");
    }

    @Test
    void validate_anotherDefaultProfilePresent_throws() {
        AgentProfile profile = validProfile();
        profile.setDefault(true);
        when(tenantService.tenantExists(TENANT_ID)).thenReturn(true);
        AgentProfile existingDefault = validProfile();
        existingDefault.setId(new AgentProfileId(UUID.randomUUID()));
        when(agentProfileService.findDefaultAgentProfile(TENANT_ID)).thenReturn(existingDefault);

        assertThatThrownBy(() -> validator.validate(profile, AgentProfile::getTenantId))
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("Another default agent profile");
    }

    @Test
    void validate_defaultProfile_sameId_passes() {
        AgentProfileId id = new AgentProfileId(UUID.randomUUID());
        AgentProfile profile = validProfile();
        profile.setId(id);
        profile.setDefault(true);
        when(tenantService.tenantExists(TENANT_ID)).thenReturn(true);
        AgentProfile existingDefault = validProfile();
        existingDefault.setId(id);
        when(agentProfileService.findDefaultAgentProfile(TENANT_ID)).thenReturn(existingDefault);
        when(agentProfileDao.findById(any(), eq(id.getId()))).thenReturn(profile);

        assertDoesNotThrow(() -> validator.validate(profile, AgentProfile::getTenantId));
    }

    @Test
    void validate_nonDefaultProfile_passes() {
        AgentProfile profile = validProfile();
        when(tenantService.tenantExists(TENANT_ID)).thenReturn(true);

        assertDoesNotThrow(() -> validator.validate(profile, AgentProfile::getTenantId));
    }

    @Test
    void validateUpdate_nonExisting_throws() {
        AgentProfileId id = new AgentProfileId(UUID.randomUUID());
        AgentProfile profile = validProfile();
        profile.setId(id);
        when(tenantService.tenantExists(TENANT_ID)).thenReturn(true);
        when(agentProfileDao.findById(any(), eq(id.getId()))).thenReturn(null);

        assertThatThrownBy(() -> validator.validate(profile, AgentProfile::getTenantId))
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("Can't update non existing agent profile");
    }

    private AgentProfile validProfile() {
        AgentProfile profile = new AgentProfile();
        profile.setName("Test Agent Profile");
        profile.setTenantId(TENANT_ID);
        return profile;
    }
}
