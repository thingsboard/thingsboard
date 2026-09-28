// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.service.validator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.thingsboard.server.common.data.agent.Agent;
import org.thingsboard.server.common.data.agent.AgentAppProfile;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.AgentApplicationInfo;
import org.thingsboard.server.common.data.agent.AgentApplicationOrigin;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.agent.config.AgentAppArgument;
import org.thingsboard.server.common.data.agent.config.AgentAppArgumentSource;
import org.thingsboard.server.common.data.agent.config.AgentAppArgumentValueType;
import org.thingsboard.server.common.data.agent.config.DockerComposeConfig;
import org.thingsboard.server.common.data.agent.template.AgentAppTemplate;
import org.thingsboard.server.common.data.id.AgentAppProfileId;
import org.thingsboard.server.common.data.id.AgentApplicationId;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.dao.agent.AgentAppArgumentReferenceValidator;
import org.thingsboard.server.dao.agent.AgentAppArgumentSourceResolver;
import org.thingsboard.server.dao.agent.AgentAppProfileService;
import org.thingsboard.server.dao.agent.AgentAppRelationService;
import org.thingsboard.server.dao.sql.agent.AppTemplateRegistry;
import org.thingsboard.server.dao.agent.AgentApplicationDao;
import org.thingsboard.server.dao.agent.AgentService;
import org.thingsboard.server.dao.entity.EntityDaoService;
import org.thingsboard.server.dao.entity.EntityServiceRegistry;
import org.thingsboard.server.dao.owner.OwnerService;
import org.thingsboard.server.dao.usagerecord.ApiLimitService;
import org.thingsboard.server.exception.DataValidationException;
import org.thingsboard.server.exception.EntitiesLimitExceededException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@SpringBootTest(classes = {
        AgentApplicationDataValidator.class,
        AgentAppArgumentReferenceValidator.class,
        AgentAppArgumentSourceResolver.class
})
class AgentApplicationDataValidatorTest {

    @MockitoBean
    AgentService agentService;
    @MockitoBean
    AgentApplicationDao agentApplicationDao;
    @MockitoBean
    AppTemplateRegistry templateRegistry;
    @MockitoBean
    AgentAppProfileService agentAppProfileService;
    @MockitoBean
    EntityServiceRegistry entityServiceRegistry;
    @MockitoBean
    OwnerService ownerService;
    @MockitoBean
    AgentAppRelationService agentAppRelationService;
    @MockitoBean(name = "agentArgDeviceDaoService")
    EntityDaoService deviceDaoService;
    @MockitoBean
    ApiLimitService apiLimitService;
    @Autowired
    AgentApplicationDataValidator validator;

    TenantId tenantId = TenantId.fromUUID(UUID.fromString("9ef79cdf-37a8-4119-b682-2e7ed4e018da"));
    AgentId agentId = new AgentId(UUID.fromString("8ef79cdf-37a8-4119-b682-2e7ed4e018da"));
    AgentApplicationId applicationId = new AgentApplicationId(UUID.fromString("7ef79cdf-37a8-4119-b682-2e7ed4e018da"));
    String currentVersion = "1.0";
    String nextVersion = "2.0";

    @BeforeEach
    void setUp() {
        Agent agent = new Agent();
        agent.setId(agentId);
        agent.setTenantId(tenantId);
        agent.setName("Test Agent");
        willReturn(agent).given(agentService).findAgentById(eq(tenantId), eq(agentId));

        AgentAppTemplate template = new AgentAppTemplate();
        template.setCurrentVersion(currentVersion);
        template.setNextVersion(nextVersion);
        willReturn(template).given(templateRegistry).get(eq(AgentApplicationType.EDGE), any(), eq(currentVersion));
        willReturn(nextVersion).given(templateRegistry).next(eq(AgentApplicationType.EDGE), any(), eq(currentVersion));
    }

    // ==================== Per-tenant limit (validateCreate) ====================

    @Test
    void testValidateCreate_unlimited_thenOKWithoutCounting() {
        givenMaxAgentApplications(0);

        assertDoesNotThrow(() -> validator.validateCreate(tenantId, new AgentApplication()));
        verify(agentApplicationDao, never()).countByTenantId(any());
    }

    @Test
    void testValidateCreate_underLimit_thenOK() {
        givenMaxAgentApplications(5);
        willReturn(4L).given(agentApplicationDao).countByTenantId(tenantId);

        assertDoesNotThrow(() -> validator.validateCreate(tenantId, new AgentApplication()));
    }

    @Test
    void testValidateCreate_limitReached_thenEntitiesLimitExceeded() {
        long limit = 5;
        givenMaxAgentApplications(limit);
        willReturn(limit).given(agentApplicationDao).countByTenantId(tenantId);

        EntitiesLimitExceededException exception = assertThrows(EntitiesLimitExceededException.class,
                () -> validator.validateCreate(tenantId, new AgentApplication()));
        assertThat(exception.getTenantId()).isEqualTo(tenantId);
        assertThat(exception.getEntityType()).isEqualTo(EntityType.AGENT_APPLICATION);
        assertThat(exception.getLimit()).isEqualTo(limit);
    }

    private void givenMaxAgentApplications(long limit) {
        DefaultTenantProfileConfiguration configuration = DefaultTenantProfileConfiguration.builder()
                .maxAgentApplications(limit)
                .build();
        willAnswer(invocation -> invocation.<Function<DefaultTenantProfileConfiguration, Number>>getArgument(1).apply(configuration))
                .given(apiLimitService).getLimit(eq(tenantId), any());
    }

    // ==================== Basic data validation (validateDataImpl) ====================

    @Test
    void testValidateDataImpl_nullAgentId_thenException() {
        AgentApplication app = new AgentApplication();

        DataValidationException exception = assertThrows(DataValidationException.class,
                () -> validator.validateDataImpl(tenantId, app));
        assertThat(exception.getMessage()).containsIgnoringCase("assigned to agent");
    }

    @Test
    void testValidateDataImpl_nullTemplateId_thenException() {
        AgentApplication app = new AgentApplication();
        app.setAgentId(agentId);
        app.setAppType(AgentApplicationType.EDGE);
        app.setOrigin(AgentApplicationOrigin.INSTALLED);
        app.setProjectName("test-project");

        DataValidationException exception = assertThrows(DataValidationException.class,
                () -> validator.validateDataImpl(tenantId, app));
        assertThat(exception.getMessage()).containsIgnoringCase("assigned to a template version");
    }

    @Test
    void testValidateDataImpl_nullAppType_thenException() {
        AgentApplication app = new AgentApplication();
        app.setAgentId(agentId);

        DataValidationException exception = assertThrows(DataValidationException.class,
                () -> validator.validateDataImpl(tenantId, app));
        assertThat(exception.getMessage()).containsIgnoringCase("type must not be null");
    }

    @Test
    void testValidateDataImpl_nullOrigin_thenException() {
        AgentApplication app = createValidApplication();
        app.setOrigin(null);

        DataValidationException exception = assertThrows(DataValidationException.class,
                () -> validator.validateDataImpl(tenantId, app));
        assertThat(exception.getMessage()).containsIgnoringCase("origin must not be null");
    }

    @Test
    void testValidateDataImpl_nullProjectName_thenException() {
        AgentApplication app = createValidApplication();
        app.setProjectName(null);

        DataValidationException exception = assertThrows(DataValidationException.class,
                () -> validator.validateDataImpl(tenantId, app));
        assertThat(exception.getMessage()).containsIgnoringCase("project name must not be null");
    }

    @Test
    void testValidateDataImpl_nullTemplateId_forAnyType_thenException() {
        AgentApplication app = createValidApplication();
        app.setTemplateVersion(null);

        DataValidationException exception = assertThrows(DataValidationException.class,
                () -> validator.validateDataImpl(tenantId, app));
        assertThat(exception.getMessage()).containsIgnoringCase("assigned to a template version");
    }

    @Test
    void testValidateDataImpl_nonExistentAgent_thenException() {
        AgentId nonExistentAgentId = new AgentId(UUID.randomUUID());
        willReturn(null).given(agentService).findAgentById(eq(tenantId), eq(nonExistentAgentId));

        AgentApplication app = createValidApplication();
        app.setAgentId(nonExistentAgentId);

        DataValidationException exception = assertThrows(DataValidationException.class,
                () -> validator.validateDataImpl(tenantId, app));
        assertThat(exception.getMessage()).contains("non-existent agent");
    }

    @Test
    void testValidateDataImpl_agentFromDifferentTenant_thenException() {
        TenantId differentTenantId = TenantId.fromUUID(UUID.randomUUID());
        Agent agentFromDifferentTenant = new Agent();
        agentFromDifferentTenant.setId(agentId);
        agentFromDifferentTenant.setTenantId(differentTenantId);
        willReturn(agentFromDifferentTenant).given(agentService).findAgentById(eq(tenantId), eq(agentId));

        AgentApplication app = createValidApplication();

        DataValidationException exception = assertThrows(DataValidationException.class,
                () -> validator.validateDataImpl(tenantId, app));
        assertThat(exception.getMessage()).contains("different tenant");
    }

    @Test
    void testValidateDataImpl_nonExistentTemplate_thenException() {
        AgentApplication app = createValidApplication();
        app.setTemplateVersion("9.9.9");

        DataValidationException exception = assertThrows(DataValidationException.class,
                () -> validator.validateDataImpl(tenantId, app));
        assertThat(exception.getMessage()).contains("non-existent template");
    }

    @Test
    void testValidateDataImpl_appTypeWithDefaultVersion_otherVersion_thenException() {
        AgentAppTemplate genericTemplate = new AgentAppTemplate();
        genericTemplate.setCurrentVersion("1.0");
        willReturn(genericTemplate).given(templateRegistry).get(eq(AgentApplicationType.GENERIC), any(), eq("1.0"));

        AgentApplication app = createValidApplication();
        app.setAppType(AgentApplicationType.GENERIC);
        app.setTemplateVersion("1.0");

        DataValidationException exception = assertThrows(DataValidationException.class,
                () -> validator.validateDataImpl(tenantId, app));
        assertThat(exception.getMessage()).isEqualTo("Template version does not match the application's current version");
    }

    @Test
    void testValidateDataImpl_appTypeWithDefaultVersion_defaultVersion_thenOK() {
        String defaultVersion = AgentApplicationType.GENERIC.getDefaultVersion();
        AgentAppTemplate genericTemplate = new AgentAppTemplate();
        genericTemplate.setCurrentVersion(defaultVersion);
        willReturn(genericTemplate).given(templateRegistry).get(eq(AgentApplicationType.GENERIC), any(), eq(defaultVersion));

        AgentApplication app = createValidApplication();
        app.setAppType(AgentApplicationType.GENERIC);
        app.setTemplateVersion(defaultVersion);

        assertDoesNotThrow(() -> validator.validateDataImpl(tenantId, app));
    }

    @Test
    void testValidateDataImpl_nameTooLong_thenException() {
        AgentApplication app = createValidApplication();
        app.setName("a".repeat(256));

        DataValidationException exception = assertThrows(DataValidationException.class,
                () -> validator.validateDataImpl(tenantId, app));
        assertThat(exception.getMessage()).contains("name length");
    }

    @Test
    void testValidateDataImpl_valid_thenOK() {
        AgentApplication app = createValidApplication();

        assertDoesNotThrow(() -> validator.validateDataImpl(tenantId, app));
    }

    @Test
    void testValidateDataImpl_nullConfigWithProfile_thenOK() {
        AgentApplication app = createValidApplication();
        app.setConfig(null);
        app.setApplicationProfileId(new AgentAppProfileId(UUID.randomUUID()));

        assertDoesNotThrow(() -> validator.validateDataImpl(tenantId, app));
    }

    @Test
    void testValidateDataImpl_nullConfigWithoutProfile_thenException() {
        AgentApplication app = createValidApplication();
        app.setConfig(null);

        DataValidationException exception = assertThrows(DataValidationException.class,
                () -> validator.validateDataImpl(tenantId, app));
        assertThat(exception.getMessage()).containsIgnoringCase("config must not be null");
    }

    @Test
    void testValidateDataImpl_dockerComposeConfig_nullCompose_thenException() {
        AgentApplication app = createValidApplication();
        DockerComposeConfig config = new DockerComposeConfig();
        app.setConfig(config);

        DataValidationException exception = assertThrows(DataValidationException.class,
                () -> validator.validateDataImpl(tenantId, app));
        assertThat(exception.getMessage()).contains("compose content");
    }

    @Test
    void testValidateDataImpl_dockerComposeConfig_valid_thenOK() {
        AgentApplication app = createValidApplication();

        assertDoesNotThrow(() -> validator.validateDataImpl(tenantId, app));
    }

    @Test
    void testValidateDataImpl_concreteEntityArgument_invokesReferenceValidator() {
        DeviceId deviceId = new DeviceId(UUID.randomUUID());
        willReturn(deviceDaoService).given(entityServiceRegistry).getServiceByEntityType(EntityType.DEVICE);
        willReturn(Optional.of(new Device(deviceId))).given(deviceDaoService).findEntity(eq(tenantId), any());

        AgentApplication app = createValidApplication();
        app.setConfig(configWithDeviceArgument(deviceId));

        assertDoesNotThrow(() -> validator.validateDataImpl(tenantId, app));
        verify(entityServiceRegistry).getServiceByEntityType(EntityType.DEVICE);
        verify(deviceDaoService).findEntity(eq(tenantId), eq(deviceId));
    }

    @Test
    void testValidateDataImpl_concreteEntityArgument_nonExistentEntity_thenException() {
        DeviceId deviceId = new DeviceId(UUID.randomUUID());
        willReturn(deviceDaoService).given(entityServiceRegistry).getServiceByEntityType(EntityType.DEVICE);
        willReturn(Optional.empty()).given(deviceDaoService).findEntity(eq(tenantId), any());

        AgentApplication app = createValidApplication();
        app.setConfig(configWithDeviceArgument(deviceId));

        DataValidationException exception = assertThrows(DataValidationException.class,
                () -> validator.validateDataImpl(tenantId, app));
        assertThat(exception.getMessage()).containsIgnoringCase("non-existent");
    }

    // ==================== Update — preconditions ====================

    @Test
    void testValidateUpdate_nonExistentApplication_thenException() {
        willReturn(null).given(agentApplicationDao).findInfoById(eq(tenantId), eq(applicationId.getId()));

        AgentApplication app = createValidApplication();
        app.setId(applicationId);

        DataValidationException exception = assertThrows(DataValidationException.class,
                () -> validator.validateUpdate(tenantId, app));
        assertThat(exception.getMessage()).contains("non existing agent application");
    }

    @Test
    void testValidateUpdate_pendingDeletion_thenException() {
        AgentApplication oldApp = createValidApplication();
        oldApp.setId(applicationId);
        oldApp.setPendingDeletion(true);
        willReturn(asInfo(oldApp)).given(agentApplicationDao).findInfoById(eq(tenantId), eq(applicationId.getId()));

        AgentApplication newApp = createValidApplication();
        newApp.setId(applicationId);
        newApp.setPendingDeletion(true);

        DataValidationException exception = assertThrows(DataValidationException.class,
                () -> validator.validateUpdate(tenantId, newApp));
        assertThat(exception.getMessage()).contains("pending for removal");
    }

    @Test
    void testValidateUpdate_originChanged_thenException() {
        AgentApplication oldApp = createValidApplication();
        oldApp.setId(applicationId);
        oldApp.setOrigin(AgentApplicationOrigin.DISCOVERED);
        willReturn(asInfo(oldApp)).given(agentApplicationDao).findInfoById(eq(tenantId), eq(applicationId.getId()));

        AgentApplication newApp = createValidApplication();
        newApp.setId(applicationId);
        newApp.setOrigin(AgentApplicationOrigin.INSTALLED);

        DataValidationException exception = assertThrows(DataValidationException.class,
                () -> validator.validateUpdate(tenantId, newApp));
        assertThat(exception.getMessage()).isEqualTo("Application origin does not match");
    }

    @Test
    void testValidateUpdate_appTypeChanged_thenException() {
        AgentApplication oldApp = createValidApplication();
        oldApp.setId(applicationId);
        willReturn(asInfo(oldApp)).given(agentApplicationDao).findInfoById(eq(tenantId), eq(applicationId.getId()));

        AgentApplication newApp = createValidApplication();
        newApp.setId(applicationId);
        newApp.setAppType(AgentApplicationType.GATEWAY);

        DataValidationException exception = assertThrows(DataValidationException.class,
                () -> validator.validateUpdate(tenantId, newApp));
        assertThat(exception.getMessage()).isEqualTo("Application type does not match");
    }

    @Test
    void testValidateUpdate_projectNameChanged_thenException() {
        AgentApplication oldApp = createValidApplication();
        oldApp.setId(applicationId);
        willReturn(asInfo(oldApp)).given(agentApplicationDao).findInfoById(eq(tenantId), eq(applicationId.getId()));

        AgentApplication newApp = createValidApplication();
        newApp.setId(applicationId);
        newApp.setProjectName("other-project");

        DataValidationException exception = assertThrows(DataValidationException.class,
                () -> validator.validateUpdate(tenantId, newApp));
        assertThat(exception.getMessage()).isEqualTo("Application project name does not match");
    }

    @Test
    void testValidateUpdate_profileNotFound_thenException() {
        AgentAppProfileId profileId = new AgentAppProfileId(UUID.randomUUID());
        stubExistingApplication(null, createEdgeComposeConfig("rk-1", "tb.cloud"));
        willReturn(null).given(agentAppProfileService).findProfileById(eq(tenantId), eq(profileId));

        AgentApplication newApp = createProfileManagedApplication(profileId);

        DataValidationException exception = assertThrows(DataValidationException.class,
                () -> validator.validateUpdate(tenantId, newApp));
        assertThat(exception.getMessage()).isEqualTo("Application profile not found");
    }

    // ==================== Update — same profile, no real config change (early return) ====================

    @Test
    void testValidateUpdate_sameProfile_configUnchanged_thenOK() {
        AgentAppProfileId profileId = new AgentAppProfileId(UUID.randomUUID());
        stubExistingApplication(profileId, createEdgeComposeConfig("rk-1", "tb.cloud"));

        AgentApplication newApp = createProfileManagedApplication(profileId);
        newApp.setConfig(createEdgeComposeConfig("rk-1", "tb.cloud"));

        assertDoesNotThrow(() -> validator.validateUpdate(tenantId, newApp));
    }

    @Test
    void testValidateUpdate_sameProfile_credentialsRotated_thenOK() {
        // Cred-only diffs are accepted on a profile-managed app — users can rotate
        // routing keys / tokens without going through a profile re-sync.
        AgentAppProfileId profileId = new AgentAppProfileId(UUID.randomUUID());
        stubExistingApplication(profileId, createEdgeComposeConfig("rk-1", "tb.cloud"));

        AgentApplication newApp = createProfileManagedApplication(profileId);
        newApp.setConfig(createEdgeComposeConfig("rotated-rk", "tb.cloud"));

        assertDoesNotThrow(() -> validator.validateUpdate(tenantId, newApp));
    }

    // ==================== Update — same profile, config refetched from profile ====================
    // Triggered when the profile's config drifts and the client pushes the profile's new config back into the app.

    @Test
    void testValidateUpdate_sameProfile_configReplacedWithProfileConfig_thenOK() {
        AgentAppProfileId profileId = new AgentAppProfileId(UUID.randomUUID());
        stubExistingApplication(profileId, createEdgeComposeConfig("rk-1", "tb.cloud"));

        DockerComposeConfig profileConfig = createEdgeComposeConfig("rk-1", "other.cloud");
        stubProfile(profileId, currentVersion, profileConfig);

        AgentApplication newApp = createProfileManagedApplication(profileId);
        newApp.setConfig(createEdgeComposeConfig("rk-1", "other.cloud"));

        assertDoesNotThrow(() -> validator.validateUpdate(tenantId, newApp));
    }

    @Test
    void testValidateUpdate_sameProfile_configReplaced_butProfileTemplateAheadOfApp_thenException() {
        AgentAppProfileId profileId = new AgentAppProfileId(UUID.randomUUID());
        stubExistingApplication(profileId, createEdgeComposeConfig("rk-1", "tb.cloud"));

        stubProfile(profileId, "5.0", createEdgeComposeConfig("rk-1", "other.cloud"));

        AgentApplication newApp = createProfileManagedApplication(profileId);
        newApp.setConfig(createEdgeComposeConfig("rk-1", "other.cloud"));

        DataValidationException exception = assertThrows(DataValidationException.class,
                () -> validator.validateUpdate(tenantId, newApp));
        assertThat(exception.getMessage()).contains("must match the assigned profile's template version");
    }

    @Test
    void testValidateUpdate_sameProfile_credsRotated_profileTemplateAheadOfApp_thenOK() {
        AgentAppProfileId profileId = new AgentAppProfileId(UUID.randomUUID());
        stubExistingApplication(profileId, createEdgeComposeConfig("rk-1", "tb.cloud"));

        stubProfile(profileId, "5.0", createEdgeComposeConfig("rk-1", "other.cloud"));

        AgentApplication newApp = createProfileManagedApplication(profileId);
        newApp.setConfig(createEdgeComposeConfig("rotated-rk", "tb.cloud"));

        assertDoesNotThrow(() -> validator.validateUpdate(tenantId, newApp));
    }

    @Test
    void testValidateUpdate_sameProfile_templateVersionMutated_thenException() {
        AgentAppProfileId profileId = new AgentAppProfileId(UUID.randomUUID());
        stubExistingApplication(profileId, createEdgeComposeConfig("rk-1", "tb.cloud"));

        stubProfile(profileId, currentVersion, createEdgeComposeConfig("rk-1", "tb.cloud"));

        AgentApplication newApp = createProfileManagedApplication(profileId);
        newApp.setTemplateVersion("5.0");
        newApp.setConfig(createEdgeComposeConfig("rk-1", "tb.cloud"));

        DataValidationException exception = assertThrows(DataValidationException.class,
                () -> validator.validateUpdate(tenantId, newApp));
        assertThat(exception.getMessage()).contains("Cannot change template version");
    }

    @Test
    void testValidateUpdate_sameProfile_configReplaced_butDoesNotMatchProfile_thenException() {
        AgentAppProfileId profileId = new AgentAppProfileId(UUID.randomUUID());
        stubExistingApplication(profileId, createEdgeComposeConfig("rk-1", "tb.cloud"));

        stubProfile(profileId, currentVersion, createEdgeComposeConfig("rk-1", "profile.cloud"));

        AgentApplication newApp = createProfileManagedApplication(profileId);
        newApp.setConfig(createEdgeComposeConfig("rk-1", "client-supplied.cloud"));

        DataValidationException exception = assertThrows(DataValidationException.class,
                () -> validator.validateUpdate(tenantId, newApp));
        assertThat(exception.getMessage()).contains("does not match the profile's config");
    }

    // ==================== Update — switching to a different profile ====================

    @Test
    void testValidateUpdate_profileSwitched_configMatchesNewProfile_thenOK() {
        AgentAppProfileId oldProfileId = new AgentAppProfileId(UUID.randomUUID());
        AgentAppProfileId newProfileId = new AgentAppProfileId(UUID.randomUUID());
        DockerComposeConfig profileConfig = createEdgeComposeConfig("rk-9", "new.cloud");

        stubExistingApplication(oldProfileId, createEdgeComposeConfig("rk-1", "tb.cloud"));
        stubProfile(newProfileId, currentVersion, profileConfig);

        AgentApplication newApp = createProfileManagedApplication(newProfileId);
        newApp.setConfig(createEdgeComposeConfig("rk-9", "new.cloud"));

        assertDoesNotThrow(() -> validator.validateUpdate(tenantId, newApp));
    }

    @Test
    void testValidateUpdate_profileSwitched_differentTemplateVersion_appVersionFlipped_thenOK() {
        AgentAppProfileId oldProfileId = new AgentAppProfileId(UUID.randomUUID());
        AgentAppProfileId newProfileId = new AgentAppProfileId(UUID.randomUUID());

        stubExistingApplication(oldProfileId, createEdgeComposeConfig("rk-1", "tb.cloud"));
        stubProfile(newProfileId, "5.0", createEdgeComposeConfig("rk-9", "new.cloud"));

        AgentApplication newApp = createProfileManagedApplication(newProfileId);
        newApp.setTemplateVersion("5.0");
        newApp.setConfig(createEdgeComposeConfig("rk-9", "new.cloud"));

        assertDoesNotThrow(() -> validator.validateUpdate(tenantId, newApp));
    }

    @Test
    void testValidateUpdate_profileSwitched_appVersionNotMatchingNewProfile_thenException() {
        AgentAppProfileId oldProfileId = new AgentAppProfileId(UUID.randomUUID());
        AgentAppProfileId newProfileId = new AgentAppProfileId(UUID.randomUUID());

        stubExistingApplication(oldProfileId, createEdgeComposeConfig("rk-1", "tb.cloud"));
        stubProfile(newProfileId, "2.0", createEdgeComposeConfig("rk-9", "new.cloud"));

        AgentApplication newApp = createProfileManagedApplication(newProfileId);
        newApp.setConfig(createEdgeComposeConfig("rk-9", "new.cloud"));

        DataValidationException exception = assertThrows(DataValidationException.class,
                () -> validator.validateUpdate(tenantId, newApp));
        assertThat(exception.getMessage()).contains("must match the assigned profile's template version");
    }

    @Test
    void testValidateUpdate_profileSwitched_configDoesNotMatchNewProfile_thenException() {
        AgentAppProfileId oldProfileId = new AgentAppProfileId(UUID.randomUUID());
        AgentAppProfileId newProfileId = new AgentAppProfileId(UUID.randomUUID());

        stubExistingApplication(oldProfileId, createEdgeComposeConfig("rk-1", "tb.cloud"));
        stubProfile(newProfileId, currentVersion, createEdgeComposeConfig("rk-9", "profile.cloud"));

        AgentApplication newApp = createProfileManagedApplication(newProfileId);
        newApp.setConfig(createEdgeComposeConfig("rk-9", "wrong.cloud"));

        DataValidationException exception = assertThrows(DataValidationException.class,
                () -> validator.validateUpdate(tenantId, newApp));
        assertThat(exception.getMessage()).contains("does not match the profile's config");
    }

    // ==================== Update — adding a profile to an unprofiled application ====================

    @Test
    void testValidateUpdate_profileAdded_configMatchesProfile_thenOK() {
        AgentAppProfileId profileId = new AgentAppProfileId(UUID.randomUUID());
        DockerComposeConfig profileConfig = createEdgeComposeConfig("rk-9", "new.cloud");

        AgentApplication oldApp = createValidApplication();
        oldApp.setId(applicationId);
        willReturn(asInfo(oldApp)).given(agentApplicationDao).findInfoById(eq(tenantId), eq(applicationId.getId()));

        stubProfile(profileId, currentVersion, profileConfig);

        AgentApplication newApp = createProfileManagedApplication(profileId);
        newApp.setConfig(createEdgeComposeConfig("rk-9", "new.cloud"));

        assertDoesNotThrow(() -> validator.validateUpdate(tenantId, newApp));
    }

    @Test
    void testValidateUpdate_profileAdded_differentTemplateVersion_appVersionFlipped_thenOK() {
        AgentAppProfileId profileId = new AgentAppProfileId(UUID.randomUUID());

        AgentApplication oldApp = createValidApplication();
        oldApp.setId(applicationId);
        willReturn(asInfo(oldApp)).given(agentApplicationDao).findInfoById(eq(tenantId), eq(applicationId.getId()));

        stubProfile(profileId, "0.5", createEdgeComposeConfig("rk-9", "new.cloud"));

        AgentApplication newApp = createProfileManagedApplication(profileId);
        newApp.setTemplateVersion("0.5");
        newApp.setConfig(createEdgeComposeConfig("rk-9", "new.cloud"));

        assertDoesNotThrow(() -> validator.validateUpdate(tenantId, newApp));
    }

    @Test
    void testValidateUpdate_profileAdded_appVersionNotMatchingProfile_thenException() {
        AgentAppProfileId profileId = new AgentAppProfileId(UUID.randomUUID());

        AgentApplication oldApp = createValidApplication();
        oldApp.setId(applicationId);
        willReturn(asInfo(oldApp)).given(agentApplicationDao).findInfoById(eq(tenantId), eq(applicationId.getId()));

        stubProfile(profileId, "2.0", createEdgeComposeConfig("rk-9", "new.cloud"));

        AgentApplication newApp = createProfileManagedApplication(profileId);
        newApp.setConfig(createEdgeComposeConfig("rk-9", "new.cloud"));

        DataValidationException exception = assertThrows(DataValidationException.class,
                () -> validator.validateUpdate(tenantId, newApp));
        assertThat(exception.getMessage()).contains("must match the assigned profile's template version");
    }

    @Test
    void testValidateUpdate_profileAdded_configDoesNotMatchProfile_thenException() {
        AgentAppProfileId profileId = new AgentAppProfileId(UUID.randomUUID());

        AgentApplication oldApp = createValidApplication();
        oldApp.setId(applicationId);
        willReturn(asInfo(oldApp)).given(agentApplicationDao).findInfoById(eq(tenantId), eq(applicationId.getId()));

        stubProfile(profileId, currentVersion, createEdgeComposeConfig("rk-9", "profile.cloud"));

        AgentApplication newApp = createProfileManagedApplication(profileId);
        newApp.setConfig(createEdgeComposeConfig("rk-9", "wrong.cloud"));

        DataValidationException exception = assertThrows(DataValidationException.class,
                () -> validator.validateUpdate(tenantId, newApp));
        assertThat(exception.getMessage()).contains("does not match the profile's config");
    }

    // ==================== Update — no profile on the new state (drop or never had one) ====================

    @Test
    void testValidateUpdate_profileDropped_anyConfigAccepted_thenOK() {
        AgentAppProfileId oldProfileId = new AgentAppProfileId(UUID.randomUUID());
        stubExistingApplication(oldProfileId, createEdgeComposeConfig("rk-1", "tb.cloud"));

        AgentApplication newApp = createValidApplication();
        newApp.setId(applicationId);
        newApp.setConfig(createEdgeComposeConfig("rk-anything", "anywhere.cloud"));

        assertDoesNotThrow(() -> validator.validateUpdate(tenantId, newApp));
    }

    @Test
    void testValidateUpdate_neverHadProfile_thenOK() {
        AgentApplication oldApp = createValidApplication();
        oldApp.setId(applicationId);
        AgentApplicationInfo oldInfo = asInfo(oldApp);
        willReturn(oldInfo).given(agentApplicationDao).findInfoById(eq(tenantId), eq(applicationId.getId()));

        AgentApplication newApp = createValidApplication();
        newApp.setId(applicationId);

        AgentApplication result = validator.validateUpdate(tenantId, newApp);
        assertThat(result).isEqualTo(oldInfo);
    }

    // ==================== Upgrade — without a profile (upgrade-chain check only) ====================

    @Test
    void testValidateUpdate_upgrade_noProfile_validVersionChain_thenOK() {
        AgentApplication oldApp = createValidApplication();
        oldApp.setId(applicationId);
        AgentApplicationInfo oldInfo = asInfo(oldApp);
        willReturn(oldInfo).given(agentApplicationDao).findInfoById(eq(tenantId), eq(applicationId.getId()));

        stubTemplate(nextVersion);

        AgentApplication newApp = createValidApplication();
        newApp.setId(applicationId);
        newApp.setDesiredTemplateVersion(nextVersion);

        AgentApplication result = validator.validateUpdate(tenantId, newApp);
        assertThat(result).isEqualTo(oldInfo);
    }

    @Test
    void testValidateUpdate_upgrade_noProfile_currentTemplateHasNoNextVersion_thenException() {
        AgentApplication oldApp = createValidApplication();
        oldApp.setId(applicationId);
        willReturn(asInfo(oldApp)).given(agentApplicationDao).findInfoById(eq(tenantId), eq(applicationId.getId()));

        willReturn(null).given(templateRegistry).next(eq(AgentApplicationType.EDGE), any(), eq(currentVersion));

        AgentApplication newApp = createValidApplication();
        newApp.setId(applicationId);
        newApp.setDesiredTemplateVersion(nextVersion);

        DataValidationException exception = assertThrows(DataValidationException.class,
                () -> validator.validateUpdate(tenantId, newApp));
        assertThat(exception.getMessage()).contains("No next version");
    }

    @Test
    void testValidateUpdate_upgrade_noProfile_desiredTemplateNotFound_thenException() {
        AgentApplication oldApp = createValidApplication();
        oldApp.setId(applicationId);
        willReturn(asInfo(oldApp)).given(agentApplicationDao).findInfoById(eq(tenantId), eq(applicationId.getId()));

        AgentApplication newApp = createValidApplication();
        newApp.setId(applicationId);
        newApp.setDesiredTemplateVersion(nextVersion);

        DataValidationException exception = assertThrows(DataValidationException.class,
                () -> validator.validateUpdate(tenantId, newApp));
        assertThat(exception.getMessage()).contains("Desired template not found");
    }

    @Test
    void testValidateUpdate_upgrade_noProfile_desiredVersionNotEqualToNextVersion_thenException() {
        AgentApplication oldApp = createValidApplication();
        oldApp.setId(applicationId);
        willReturn(asInfo(oldApp)).given(agentApplicationDao).findInfoById(eq(tenantId), eq(applicationId.getId()));

        stubTemplate("3.0");

        AgentApplication newApp = createValidApplication();
        newApp.setId(applicationId);
        newApp.setDesiredTemplateVersion("3.0");

        DataValidationException exception = assertThrows(DataValidationException.class,
                () -> validator.validateUpdate(tenantId, newApp));
        assertThat(exception.getMessage()).contains("does not match the next available version");
    }

    // ==================== Upgrade — same profile ====================

    @Test
    void testValidateUpdate_upgrade_sameProfile_alignedWithProfileTemplate_thenOK() {
        AgentAppProfileId profileId = new AgentAppProfileId(UUID.randomUUID());
        DockerComposeConfig profileConfig = createEdgeComposeConfig("rk-1", "tb.cloud");

        stubExistingApplication(profileId, createEdgeComposeConfig("rk-1", "tb.cloud"));
        stubTemplate(nextVersion);
        stubProfile(profileId, nextVersion, profileConfig);

        AgentApplication newApp = createProfileManagedApplication(profileId);
        newApp.setDesiredTemplateVersion(nextVersion);
        newApp.setConfig(createEdgeComposeConfig("rotated-rk", "tb.cloud"));

        assertDoesNotThrow(() -> validator.validateUpdate(tenantId, newApp));
    }

    @Test
    void testValidateUpdate_upgrade_sameProfile_desiredTemplateNotProfileTemplate_thenException() {
        AgentAppProfileId profileId = new AgentAppProfileId(UUID.randomUUID());

        stubExistingApplication(profileId, createEdgeComposeConfig("rk-1", "tb.cloud"));
        stubTemplate(nextVersion);
        stubProfile(profileId, "9.0", createEdgeComposeConfig("rk-1", "tb.cloud"));

        AgentApplication newApp = createProfileManagedApplication(profileId);
        newApp.setDesiredTemplateVersion(nextVersion);
        newApp.setConfig(createEdgeComposeConfig("rk-1", "tb.cloud"));

        DataValidationException exception = assertThrows(DataValidationException.class,
                () -> validator.validateUpdate(tenantId, newApp));
        assertThat(exception.getMessage()).contains("Desired template does not match the profile's template");
    }

    @Test
    void testValidateUpdate_upgrade_sameProfile_configDoesNotMatchProfile_thenException() {
        AgentAppProfileId profileId = new AgentAppProfileId(UUID.randomUUID());

        stubExistingApplication(profileId, createEdgeComposeConfig("rk-1", "tb.cloud"));
        stubTemplate(nextVersion);
        stubProfile(profileId, nextVersion, createEdgeComposeConfig("rk-1", "profile.cloud"));

        AgentApplication newApp = createProfileManagedApplication(profileId);
        newApp.setDesiredTemplateVersion(nextVersion);
        newApp.setConfig(createEdgeComposeConfig("rk-1", "client.cloud"));

        DataValidationException exception = assertThrows(DataValidationException.class,
                () -> validator.validateUpdate(tenantId, newApp));
        assertThat(exception.getMessage()).contains("does not match the profile's config");
    }

    // ==================== Upgrade — switching to a different profile ====================

    @Test
    void testValidateUpdate_upgrade_profileSwitched_alignedWithNewProfileTemplate_thenOK() {
        AgentAppProfileId oldProfileId = new AgentAppProfileId(UUID.randomUUID());
        AgentAppProfileId newProfileId = new AgentAppProfileId(UUID.randomUUID());
        DockerComposeConfig profileConfig = createEdgeComposeConfig("rk-9", "new.cloud");

        stubExistingApplication(oldProfileId, createEdgeComposeConfig("rk-1", "tb.cloud"));
        stubTemplate(nextVersion);
        stubProfile(newProfileId, nextVersion, profileConfig);

        AgentApplication newApp = createProfileManagedApplication(newProfileId);
        newApp.setDesiredTemplateVersion(nextVersion);
        newApp.setConfig(createEdgeComposeConfig("rk-9", "new.cloud"));

        assertDoesNotThrow(() -> validator.validateUpdate(tenantId, newApp));
    }

    @Test
    void testValidateUpdate_upgrade_profileSwitched_desiredTemplateNotNewProfileTemplate_thenException() {
        AgentAppProfileId oldProfileId = new AgentAppProfileId(UUID.randomUUID());
        AgentAppProfileId newProfileId = new AgentAppProfileId(UUID.randomUUID());

        stubExistingApplication(oldProfileId, createEdgeComposeConfig("rk-1", "tb.cloud"));
        stubTemplate(nextVersion);
        stubProfile(newProfileId, "9.0", createEdgeComposeConfig("rk-9", "new.cloud"));

        AgentApplication newApp = createProfileManagedApplication(newProfileId);
        newApp.setDesiredTemplateVersion(nextVersion);
        newApp.setConfig(createEdgeComposeConfig("rk-9", "new.cloud"));

        DataValidationException exception = assertThrows(DataValidationException.class,
                () -> validator.validateUpdate(tenantId, newApp));
        assertThat(exception.getMessage()).contains("Desired template does not match the profile's template");
    }

    // ==================== Upgrade — adding a profile to an unprofiled application ====================

    @Test
    void testValidateUpdate_upgrade_profileAdded_alignedWithProfileTemplate_thenOK() {
        AgentAppProfileId profileId = new AgentAppProfileId(UUID.randomUUID());
        DockerComposeConfig profileConfig = createEdgeComposeConfig("rk-9", "new.cloud");

        AgentApplication oldApp = createValidApplication();
        oldApp.setId(applicationId);
        willReturn(asInfo(oldApp)).given(agentApplicationDao).findInfoById(eq(tenantId), eq(applicationId.getId()));

        stubTemplate(nextVersion);
        stubProfile(profileId, nextVersion, profileConfig);

        AgentApplication newApp = createProfileManagedApplication(profileId);
        newApp.setDesiredTemplateVersion(nextVersion);
        newApp.setConfig(createEdgeComposeConfig("rk-9", "new.cloud"));

        assertDoesNotThrow(() -> validator.validateUpdate(tenantId, newApp));
    }

    @Test
    void testValidateUpdate_upgrade_profileAdded_desiredTemplateNotProfileTemplate_thenException() {
        AgentAppProfileId profileId = new AgentAppProfileId(UUID.randomUUID());

        AgentApplication oldApp = createValidApplication();
        oldApp.setId(applicationId);
        willReturn(asInfo(oldApp)).given(agentApplicationDao).findInfoById(eq(tenantId), eq(applicationId.getId()));

        stubTemplate(nextVersion);
        stubProfile(profileId, "9.0", createEdgeComposeConfig("rk-9", "new.cloud"));

        AgentApplication newApp = createProfileManagedApplication(profileId);
        newApp.setDesiredTemplateVersion(nextVersion);
        newApp.setConfig(createEdgeComposeConfig("rk-9", "new.cloud"));

        DataValidationException exception = assertThrows(DataValidationException.class,
                () -> validator.validateUpdate(tenantId, newApp));
        assertThat(exception.getMessage()).contains("Desired template does not match the profile's template");
    }

    // ==================== Helpers ====================

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private AgentApplication createValidApplication() {
        AgentApplication app = new AgentApplication();
        app.setAgentId(agentId);
        app.setAppType(AgentApplicationType.EDGE);
        app.setOrigin(AgentApplicationOrigin.INSTALLED);
        app.setProjectName("test-project");
        app.setTemplateVersion(currentVersion);
        app.setConfig(createEdgeComposeConfig("rk-valid", "tb.cloud"));
        return app;
    }

    private AgentApplication createProfileManagedApplication(AgentAppProfileId profileId) {
        AgentApplication app = createValidApplication();
        app.setId(applicationId);
        app.setApplicationProfileId(profileId);
        return app;
    }

    private void stubExistingApplication(AgentAppProfileId profileId, DockerComposeConfig config) {
        AgentApplication oldApp = createProfileManagedApplication(profileId);
        oldApp.setConfig(config);
        willReturn(asInfo(oldApp)).given(agentApplicationDao).findInfoById(eq(tenantId), eq(applicationId.getId()));
    }

    private void stubProfile(AgentAppProfileId id, String templateVersion, DockerComposeConfig config) {
        AgentAppProfile profile = new AgentAppProfile(id);
        profile.setTemplateVersion(templateVersion);
        profile.setConfig(config);
        willReturn(profile).given(agentAppProfileService).findProfileById(eq(tenantId), eq(id));
    }

    private void stubTemplate(String version) {
        AgentAppTemplate template = new AgentAppTemplate();
        template.setCurrentVersion(version);
        willReturn(template).given(templateRegistry).get(eq(AgentApplicationType.EDGE), any(), eq(version));
    }

    private AgentApplicationInfo asInfo(AgentApplication app) {
        return new AgentApplicationInfo(app, "1.0", "2.0");
    }

    private DockerComposeConfig configWithDeviceArgument(DeviceId deviceId) {
        DockerComposeConfig config = createEdgeComposeConfig("rk-ref", "tb.cloud");
        AgentAppArgument argument = new AgentAppArgument();
        argument.setName("dev");
        argument.setSourceType(AgentAppArgumentSource.DEVICE);
        argument.setSourceEntityId(deviceId);
        argument.setValueType(AgentAppArgumentValueType.ATTRIBUTE);
        argument.setScope(AttributeScope.SERVER_SCOPE);
        argument.setKey("k");
        config.setArguments(List.of(argument));
        return config;
    }

    private DockerComposeConfig createEdgeComposeConfig(String routingKey, String rpcHost) {
        ObjectNode env = MAPPER.createObjectNode();
        env.put("CLOUD_ROUTING_KEY", routingKey);
        env.put("CLOUD_ROUTING_SECRET", "secret");
        env.put("CLOUD_RPC_HOST", rpcHost);

        ObjectNode service = MAPPER.createObjectNode();
        service.put("image", "thingsboard/tb-edge-pe:3.8.0");
        service.set("environment", env);

        ObjectNode services = MAPPER.createObjectNode();
        services.set("mytbedge", service);

        ObjectNode compose = MAPPER.createObjectNode();
        compose.set("services", services);

        DockerComposeConfig config = new DockerComposeConfig();
        config.setCompose(compose);
        return config;
    }
}
