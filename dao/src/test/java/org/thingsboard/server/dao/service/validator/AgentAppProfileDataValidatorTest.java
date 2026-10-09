// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.service.validator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.agent.AgentAppProfile;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.agent.config.AgentAppArgument;
import org.thingsboard.server.common.data.agent.config.AgentAppArgumentSource;
import org.thingsboard.server.common.data.agent.config.AgentAppArgumentValueType;
import org.thingsboard.server.common.data.agent.config.DockerComposeConfig;
import org.thingsboard.server.common.data.id.AgentAppProfileId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.agent.AgentAppArgumentReferenceValidator;
import org.thingsboard.server.dao.agent.AgentAppArgumentSourceResolver;
import org.thingsboard.server.dao.agent.AgentAppProfileDao;
import org.thingsboard.server.dao.entity.EntityDaoService;
import org.thingsboard.server.dao.entity.EntityServiceRegistry;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.exception.DataValidationException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentAppProfileDataValidatorTest {

    @Mock
    private AgentAppProfileDao profileDao;
    @Mock
    private TenantService tenantService;
    @Mock
    private AgentAppArgumentSourceResolver sourceEntityResolver;
    @Mock
    private EntityServiceRegistry entityServiceRegistry;
    @Mock
    private EntityDaoService entityDaoService;

    private AgentAppProfileDataValidator validator;

    private static final TenantId TENANT_ID = TenantId.fromUUID(UUID.randomUUID());

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        AgentAppArgumentReferenceValidator argumentReferenceValidator =
                new AgentAppArgumentReferenceValidator(sourceEntityResolver, entityServiceRegistry);
        validator = new AgentAppProfileDataValidator(profileDao, tenantService, argumentReferenceValidator);
    }

    @Test
    void validateUpdate_nonExisting_throws() {
        AgentAppProfileId id = new AgentAppProfileId(UUID.randomUUID());
        AgentAppProfile profile = validProfile(AgentApplicationType.EDGE);
        profile.setId(id);
        when(tenantService.tenantExists(TENANT_ID)).thenReturn(true);
        when(profileDao.findById(any(), eq(id.getId()))).thenReturn(null);

        assertThatThrownBy(() -> validator.validate(profile, AgentAppProfile::getTenantId))
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("Can't update non existing agent application profile");
    }

    @Test
    void validateUpdate_appTypeChanged_throws() {
        AgentAppProfileId id = new AgentAppProfileId(UUID.randomUUID());
        AgentAppProfile profile = validProfile(AgentApplicationType.EDGE);
        profile.setId(id);
        AgentAppProfile old = validProfile(AgentApplicationType.GATEWAY);
        old.setId(id);
        when(tenantService.tenantExists(TENANT_ID)).thenReturn(true);
        when(profileDao.findById(any(), eq(id.getId()))).thenReturn(old);

        assertThatThrownBy(() -> validator.validate(profile, AgentAppProfile::getTenantId))
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("type cannot be changed");
    }

    @Test
    void validateUpdate_sameAppType_passes() {
        AgentAppProfileId id = new AgentAppProfileId(UUID.randomUUID());
        AgentAppProfile profile = validProfile(AgentApplicationType.EDGE);
        profile.setId(id);
        AgentAppProfile old = validProfile(AgentApplicationType.EDGE);
        old.setId(id);
        when(tenantService.tenantExists(TENANT_ID)).thenReturn(true);
        when(profileDao.findById(any(), eq(id.getId()))).thenReturn(old);

        assertDoesNotThrow(() -> validator.validate(profile, AgentAppProfile::getTenantId));
    }

    @Test
    void validateDataImpl_blankName_throws() {
        AgentAppProfile profile = validProfile(AgentApplicationType.EDGE);
        profile.setName("  ");

        assertThatThrownBy(() -> validator.validate(profile, AgentAppProfile::getTenantId))
                .isInstanceOf(DataValidationException.class)
                .hasMessage("Agent application profile name should be specified!");
    }

    @Test
    void validateDataImpl_nameWith0x00_throws() {
        AgentAppProfile profile = validProfile(AgentApplicationType.EDGE);
        profile.setName("bad\u0000name");

        assertThatThrownBy(() -> validator.validate(profile, AgentAppProfile::getTenantId))
                .isInstanceOf(DataValidationException.class)
                .hasMessage("Agent application profile name should not contain 0x00 symbol!");
    }

    @Test
    void validateDataImpl_nullAppType_throws() {
        AgentAppProfile profile = validProfile(AgentApplicationType.EDGE);
        profile.setAppType(null);

        assertThatThrownBy(() -> validator.validate(profile, AgentAppProfile::getTenantId))
                .isInstanceOf(DataValidationException.class)
                .hasMessage("Agent application profile app type must not be null!");
    }

    @Test
    void validateDataImpl_nullTemplateVersion_throws() {
        AgentAppProfile profile = validProfile(AgentApplicationType.EDGE);
        profile.setTemplateVersion(null);

        assertThatThrownBy(() -> validator.validate(profile, AgentAppProfile::getTenantId))
                .isInstanceOf(DataValidationException.class)
                .hasMessage("Agent application profile template version must not be null!");
    }

    @Test
    void validateDataImpl_nullTenantId_throws() {
        AgentAppProfile profile = validProfile(AgentApplicationType.EDGE);
        profile.setTenantId(null);

        assertThatThrownBy(() -> validator.validate(profile, AgentAppProfile::getTenantId))
                .isInstanceOf(DataValidationException.class)
                .hasMessage("Agent application profile should be assigned to tenant!");
    }

    @Test
    void validateDataImpl_nullConfig_throws() {
        AgentAppProfile profile = validProfile(AgentApplicationType.EDGE);
        profile.setConfig(null);

        assertThatThrownBy(() -> validator.validate(profile, AgentAppProfile::getTenantId))
                .isInstanceOf(DataValidationException.class)
                .hasMessage("Agent application config must not be null!");
    }

    @Test
    void validateDataImpl_nonExistentTenant_throws() {
        AgentAppProfile profile = validProfile(AgentApplicationType.EDGE);
        when(tenantService.tenantExists(TENANT_ID)).thenReturn(false);

        assertThatThrownBy(() -> validator.validate(profile, AgentAppProfile::getTenantId))
                .isInstanceOf(DataValidationException.class)
                .hasMessage("Agent application profile is referencing to non-existent tenant!");
    }

    @Test
    void validateDataImpl_concreteEntityArgument_invokesReferenceValidator() {
        DeviceId deviceId = new DeviceId(UUID.randomUUID());
        DockerComposeConfig config = mock(DockerComposeConfig.class);
        AgentAppArgument argument = new AgentAppArgument();
        argument.setName("dev");
        argument.setSourceType(AgentAppArgumentSource.DEVICE);
        argument.setSourceEntityId(deviceId);
        argument.setValueType(AgentAppArgumentValueType.ATTRIBUTE);
        argument.setScope(AttributeScope.SERVER_SCOPE);
        argument.setKey("k");
        when(config.getArguments()).thenReturn(List.of(argument));

        AgentAppProfile profile = validProfile(AgentApplicationType.EDGE);
        profile.setConfig(config);

        when(entityServiceRegistry.getServiceByEntityType(EntityType.DEVICE)).thenReturn(entityDaoService);
        when(entityDaoService.findEntity(eq(TENANT_ID), any())).thenReturn(Optional.of(new Device(deviceId)));
        when(tenantService.tenantExists(TENANT_ID)).thenReturn(true);

        assertDoesNotThrow(() -> validator.validate(profile, AgentAppProfile::getTenantId));
        verify(entityServiceRegistry).getServiceByEntityType(EntityType.DEVICE);
        verify(entityDaoService).findEntity(eq(TENANT_ID), eq(deviceId));
    }

    private AgentAppProfile validProfile(AgentApplicationType appType) {
        AgentAppProfile profile = new AgentAppProfile();
        profile.setName("Test App Profile");
        profile.setTenantId(TENANT_ID);
        profile.setAppType(appType);
        profile.setTemplateVersion("1.0.0");
        profile.setConfig(mock(DockerComposeConfig.class));
        return profile;
    }
}
